# CLAUDE.md

Context guide for working on `titan-api-poc`. Read it before touching any code.

## What this project is

A Spring Boot REST API that acts as a wrapper around the TITAN core (a tool for analyzing Petri nets with software product lines, PNPL). The core analyses are exported as JARs from Eclipse, placed in `plugins/`, and the API discovers and runs them dynamically through reflection. It also exposes endpoints to upload model files and to validate `.vrb` files with Xtext in standalone mode.

## Related repositories and how they connect

- **titan-front-end** (React): https://github.com/TITAN-PNPL-WEB/titan-front-end
  Visual editor (Petri net, feature model, presence conditions). It exports the three model files and calls this API. It is the only consumer of the endpoints.
- **titan-core-fork**: https://github.com/TITAN-PNPL-WEB/titan-core-fork
  Fork of the TITAN core with the headless path. Provides the `org.pnpl:*` artifacts (in `local-repo/`) and the analysis JARs (in `plugins/`).
- **titan-api-poc** (this one): glue between the two.

Flow: front -> `POST /pnpl/upload` (3 files) -> `vrbPath` -> `POST /pnpl/validate` -> `POST /pnpl/analyze` -> core (`AbstractAnalysis.run()`).

### Contract with the front-end
The front depends on these fields. Changing any of them is a breaking change and needs a matching change in the front:
- `/pnpl/upload` request parts: `vrb`, `petrinets`, `featureModel`. Response: `vrbPath`, `message`.
- `/pnpl/validate` response: `valid`, `issues[]` (`severity`, `message`, `line`, `column`).
- `/pnpl/analyze` request: `vrbPath`, `name`, `type`. Response: `analysis`, `allProducts`, `timeMs`, `message`.
- `/pnpl/analyses` response: list of `name` + `type`.
- CORS origins live in `CorsConfig` (see below).

### Where a change belongs
- Analysis behavior, headless loading, base classes (`AbstractAnalysis`, `VariabilityHelper`, ...): `titan-core-fork`, then publish the artifact in `local-repo/` or regenerate the JAR in `plugins/`.
- Endpoints, DTOs, validation, upload, CORS: this repo, and then check the front.
- UI, editing, export of the three files: `titan-front-end`.

If a task touches more than one repo, say it up front and list which repos need changes before editing anything. Do not edit another repo from here unless asked.

## Stack

- Java 22 + Spring Boot 3.4.2, built with Maven (`pom.xml`). There is no Maven wrapper (`mvnw`) in the repo. JDK 22 or newer is required to build and run (tested with 22 and 25).
- Dynamic class loading with `URLClassLoader`
- Core base classes (`AbstractAnalysis`, `VariabilityHelper`, `EMFHandler`, the Xtext language, etc.) come from `pom.xml`, as the `org.pnpl:*` artifacts (`analysis-core`, `analysis-featureide`, `solvers-sat`, `model-petrinet`, `model-variability`, `model-variability-editor`). They are not in any public repository.
- FeatureIDE (`de.ovgu.featureide:featureide-core` and `featureide-fm-core` 3.11.1) is not in Maven Central either.
- Those 8 artifacts are shipped in `local-repo/`, a file-based Maven repository declared in `pom.xml` (`<repository id="project-local">`, URL `${project.baseUri}local-repo`). A fresh clone builds with no manual install. To update one, use `mvn deploy:deploy-file` into `local-repo/` with a **new version** and bump it in `pom.xml` (see `docs/MAINTENANCE.md`), not `install:install-file` into `~/.m2`, or other machines will not get it. Maven caches these JARs in `~/.m2` and never re-reads a version it already has, so overwriting a JAR with the same version is silently ignored on machines that built before. The generated POMs declare no dependencies: any new library a core JAR needs must be added to `pom.xml`.
- Third-party dependencies from Maven Central: EMF/Xtext, Guice, Guava, SAT4J, Eclipse platform (`core.resources`, `core.runtime`, etc.)
- The JARs in `plugins/` only contain the concrete analysis classes (`AnalysisPNPL`, `AnalysisProducts`, `Activator`) and their `plugin.xml`.

## Package structure

```
poc.titan.api
├── TitanApiApplication.java        Main Spring Boot class
├── CorsConfig.java                 CORS for /pnpl/** (GET, POST) from the known front-end origins
├── analysis/
│   ├── AnalysisDescriptor.java     POJO: name, className, type ("pnpl" | "products")
│   ├── PluginScanner.java          @Component, @PostConstruct: scans plugins/
│   └── XtextInitializer.java       @Component, @PostConstruct: Xtext standalone setup + EMF registration
├── controller/
│   ├── AnalysisController.java     REST under /pnpl: /analyses, /analyze
│   ├── FileUploadController.java   REST under /pnpl: /upload (logic lives in the controller)
│   └── ValidationController.java   REST under /pnpl: /validate
├── dto/
│   ├── AnalysisRequest.java        vrbPath, name, type
│   ├── AnalysisResponse.java       analysis, allProducts, timeMs, message
│   ├── AnalysesResponse.java       list of AnalysisInfo (name + type)
│   ├── UploadResponse.java         vrbPath, message
│   ├── ValidationRequest.java      vrbPath
│   └── ValidationResponse.java     valid, list of ValidationIssue (severity, message, line, column)
└── service/
    ├── AnalysisService.java        Finds descriptor, reflection, runs run()
    └── ValidationService.java      Parses and validates a .vrb with the Xtext injector
```

## Endpoints

| Method | Path | Description |
|--------|------|-------------|
| GET | `/pnpl/analyses` | Lists the available analyses (name + type) |
| POST | `/pnpl/analyze` | Runs an analysis. Body: `{ "vrbPath": "...", "name": "Free Choice", "type": "pnpl" }` |
| POST | `/pnpl/upload` | Multipart with the parts `vrb`, `petrinets` and `featureModel`. Saves them in a new temp directory and returns `{ "vrbPath": "...", "message": "..." }` |
| POST | `/pnpl/validate` | Validates a `.vrb` (syntax, cross-references, `@Check` rules). Body: `{ "vrbPath": "..." }`. Returns `{ "valid": true, "issues": [...] }` |

`name` and `type` in `/pnpl/analyze` are matched exactly and are case-sensitive: `type` must be `"pnpl"` or `"products"` in lowercase (the comment in `AnalysisRequest` that mentions `"PNPL"`/`"Products"` is wrong).

Response from `/pnpl/analyze`:
```json
{ "analysis": "Free Choice", "allProducts": true, "timeMs": 123, "message": "..." }
```

`/pnpl/validate` always answers 200: a missing file or an internal failure comes back as `valid: false` with a single `ERROR` issue.

## How it works

### Plugin discovery (`PluginScanner`)
1. On startup (`@PostConstruct`) it walks the `.jar` files in `plugins/`. The path is relative to the working directory (in Docker, `/app/plugins`). If the folder does not exist it logs a message and continues with no analyses.
2. It reads the `plugin.xml` of each JAR.
3. It extracts the analyses registered under the extension points `org.pnpl.analysis.analyzer.pnpl` and `org.pnpl.analysis.analyzer.products`, taking `name` and `class` from each `<client>`.
4. It creates one `AnalysisDescriptor` per analysis and a shared `URLClassLoader` containing all the JARs. Its parent is the application classloader, so the core base classes (for example `AbstractAnalysis`) are resolved from the app classpath, which is what makes the cast in `AnalysisService` work.

Two descriptors are equal if they have the same `name` + `type`. If two JARs declare the same pair, the first one found wins, and `Files.list` does not guarantee any order.

### Xtext initialization (`XtextInitializer`)
On startup (`@PostConstruct`) it forces the registration of `PetriNetsPackage` and `VariabilityPackage`, creates the Guice injector with `PNPL_variabilityStandaloneSetup().createInjectorAndDoEMFRegistration()`, and registers the `petrinets` extension in the global Xtext `IResourceServiceProvider` registry and the global EMF `Resource.Factory` registry (XMI). `ValidationService` uses this injector; the analysis flow relies on the global EMF registration.

### Execution (`AnalysisService`)
1. The controller only checks that `vrbPath`, `name` and `type` are not blank.
2. Looks up the descriptor by `name` + `type`. If none matches, it throws `IllegalArgumentException`.
3. Loads the class with the shared `URLClassLoader`.
4. Instantiates it with the no-argument constructor and casts it to `AbstractAnalysis` (`org.pnpl.analysis.analyzer.AbstractAnalysis`).
5. Calls `loadVariabilityFile(Path)` with the absolute, normalized `.vrb` path. This creates `new VariabilityHelper(path)`, whose constructor calls `load(Path)`, so the model is parsed here.
6. Runs `run()` (returns `boolean`) and measures the time. `timeMs` only covers `run()`, not the model loading of the previous step.
7. Returns an `AnalysisResponse`. The `message` is built in the service: "All products satisfy X" or "Some products do not satisfy X".

For `pnpl` analyses, `AbstractAnalysisPNPL.run()` extracts the presence conditions, calls the plugin's `buildCondition()`, converts the condition to CNF and solves it with SAT4J. For `products` analyses, `AbstractAnalysisProducts.run()` delegates to the plugin's `runProductAnalysis()`.

### Available plugins (`plugins/`)
Each JAR registers the same name under both types (`pnpl` and `products`), so there are 8 analyses in total:

| JAR | Registered name |
|-----|-----------------|
| `org.pnpl.analysis.extendedfreechoice` | `Extended Free Choice` |
| `org.pnpl.analysis.freechoice` | `Free Choice` |
| `org.pnpl.analysis.markedgraph` | `Marked Graphs` |
| `org.pnpl.analysis.statemachine` | `State Machines` |

## PNPL model (input)

A model is made of three files in the same directory:

- `*.petrinets`: Petri net in XMI/EMF (places, transitions, PTArc/TPArc arcs).
- `model.xml`: feature model in FeatureIDE XML format.
- `annotation.vrb`: variability file. It references the other two (`pn "x.petrinets"`, `fm "model.xml"`) and defines the Presence Conditions (PCs) that link net elements to features.

The `vrbPath` in the request points to the `.vrb`. The paths of the `.petrinets` and the `.xml` are resolved **relative to the parent directory of the `.vrb`**.

Headless flow in the core (`titan-core-fork`):
```
Path(.vrb) -> AbstractAnalysis.loadVariabilityFile(Path)
           -> new VariabilityHelper(Path) -> VariabilityHelper.load(Path)
           -> parses pn / fm / PCs
           -> loadVariabilityHeadless(pnPath, fmPath, pcs)
              -> EMFHandler.loadModel()  (PetriNet)
              -> FeatureModelProviderHeadless (feature model)
           -> readPresenceConditions()
           -> analysis.run()
```

Core classes touched for headless mode: `AbstractAnalysis`, `VariabilityHelper`, `FeatureModelProviderHeadless` (new), `EMFHandler`. The original methods using `IFile`/`IProject` are untouched for Eclipse use.

## How to regenerate the plugins

1. Make the changes in `titan-core-fork`.
2. In Eclipse: right click the plugin -> Export -> Deployable plugins and fragments (or JAR file).
3. Copy the resulting JAR into this repo's `plugins/` folder, replacing the old one.
4. Restart the API (scanning only happens at startup).

If a change in the core does not show up, it is almost always because the JAR in `plugins/` is outdated. If the change is in a base class (`AbstractAnalysis`, `VariabilityHelper`, etc.), the JAR to update is the `org.pnpl:*` artifact in `local-repo/`, not the one in `plugins/`: those classes are loaded from the app classpath. Publish it with a new version (see above). If you kept the same version while testing locally, delete the cached copy in `~/.m2/repository/org/pnpl/<artifactId>` so Maven picks up the new one.

## Commands

```bash
# Maven (no wrapper in the repo, use a local mvn)
mvn clean package
mvn spring-boot:run

# Docker (the Dockerfile copies target/titan-api-poc-1.0-SNAPSHOT.jar, so package first)
mvn clean package
docker build -t titan-api-poc .
docker run -p 8080:8080 titan-api-poc
```

Quick test:
```bash
curl http://localhost:8080/pnpl/analyses

curl -X POST http://localhost:8080/pnpl/upload \
  -F "vrb=@annotation.vrb" -F "petrinets=@net.petrinets" -F "featureModel=@model.xml"

curl -X POST http://localhost:8080/pnpl/validate \
  -H "Content-Type: application/json" \
  -d '{"vrbPath":"/absolute/path/annotation.vrb"}'

curl -X POST http://localhost:8080/pnpl/analyze \
  -H "Content-Type: application/json" \
  -d '{"vrbPath":"/absolute/path/annotation.vrb","name":"Free Choice","type":"pnpl"}'
```

## Code conventions

- Java with packages under `poc.titan.api`. One responsibility per layer: no logic in the controller, logic in the service.
- DTOs kept separate from internal POJOs (`AnalysisDescriptor` never goes out through the API).
- Constructor injection, not field injection.
- Always load the plugin analysis classes (the `class` from `plugin.xml`) with the `URLClassLoader` from `PluginScanner`, never with `Class.forName` on the application classloader. The core base classes are on the app classpath through `pom.xml` and can be imported directly.
- When using reflection, set the thread's context classloader to the `URLClassLoader` during execution and restore it in a `finally` block (Xtext/EMF/Guice rely on it to resolve resources and services).
- Errors: return consistent HTTP codes (400 for an invalid request or an unknown analysis, 404 if the `.vrb` does not exist, 500 for internal failures) with a readable message. Do not return stack traces.
- Logging with SLF4J, no `System.out`.

## Commit conventions

- Commit messages: brief, concise, in English (for example `Add maintenance guide for fork sync and new analyses`).
- Never add `Co-Authored-By: Claude ...` or any other AI attribution to commits or pull requests.

## Things to keep in mind

- **No OSGi**: there is no extension registry, so anything the core used to resolve through extension points has to be registered by hand (already done for `DefaultFeatureModelFactory` and the EMF packages `PetriNetsPackage` and `VariabilityPackage`).
- **Global EMF/Xtext state**: package and factory registries are static. `ValidationService` creates a fresh `XtextResourceSet` per request, but `AnalysisService` has no synchronization. If there are concurrent requests, check thread safety before assuming it is safe.
- **Paths**: `vrbPath` is a path on the server's file system. `/pnpl/upload` stores the files in a new temp directory per request (`Files.createTempDirectory("titan-")`) and returns the `.vrb` path. Those directories are never cleaned up, and the file names come from `getOriginalFilename()` without validation. No endpoint validates `vrbPath` against path traversal yet; always do it when touching this code.
- **One shared classloader**: the analysis JARs share dependencies. If two plugins ship different versions of the same library, there can be conflicts.
- **`allProducts`**: it is the boolean result of `run()`. Its exact meaning depends on the analysis (for example, that all products of the line satisfy the property).
- **`products` analyses do not work headless**: `AbstractAnalysisProducts.run()` returns `false` right away when `VariabilityHelper.getPetriNetFile()` (an Eclipse `IFile`) is `null`, which is always the case in headless mode. All 4 return `allProducts: false` with `timeMs: 0`. The fix belongs in `titan-core-fork`.
- **Silent wrong results**: if the `.petrinets` or `model.xml` is missing next to the `.vrb`, `/pnpl/analyze` still returns 200 with a meaningless `allProducts` (verified both `true` and `false`). `/pnpl/validate` does detect it.
- **Working directory**: `plugins/` is resolved from the current directory. Starting the API from another folder gives `{"analyses":[]}`.
- **Maven mirrors**: a `settings.xml` mirror with `<mirrorOf>*</mirrorOf>` hides `local-repo/` and brings back the "Could not resolve dependencies ... org.pnpl" error. It must be `*,!project-local`.
- Installation and troubleshooting for users live in `docs/QUICKSTART.md`. Keep it in sync when changing build, port or dependencies.
- Syncing `titan-core-fork` with the original TITAN repo and adding new analyses are documented in `docs/MAINTENANCE.md`.
- **CORS**: only `http://localhost:5173`, `https://titan-front-end-sigma.vercel.app` and `https://titan-api-pyts.onrender.com` are allowed, for `GET` and `POST` on `/pnpl/**`. A new front-end origin has to be added in `CorsConfig`.
- **Known gaps with the conventions above** (the current code does not follow them yet):
  - `AnalysisService` does not set or restore the thread's context classloader.
  - There is no `@ControllerAdvice`/`@ExceptionHandler`: an `IllegalArgumentException` (missing field, unknown analysis) ends up as a 500, and a missing `.vrb` in `/pnpl/analyze` is not reported as a 404.
  - `PluginScanner`, `XtextInitializer` and `FileUploadController` log with `System.out` (the core does too).
  - `FileUploadController` has its logic in the controller instead of a service.
- This is a PoC: prioritize making it work and keeping it clear over overengineering.
