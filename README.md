# TITAN PNPL Web: POC Analysis API

Proof of concept of a REST API that exposes TITAN's analysis and validation capabilities as HTTP endpoints, decoupled from the Eclipse IDE.

## Context

TITAN is an Eclipse-based tool for the analysis of Petri Net Product Lines (PNPL). Originally, analyses and validations are triggered through the Eclipse UI. This POC runs the analysis and validation logic headlessly through a REST API, using the TITAN analysis plugins loaded dynamically from the `plugins/` directory.

## Documentation

| I want to... | Read |
|--------------|------|
| Install and run the API | [docs/QUICKSTART.md](docs/QUICKSTART.md) |
| Sync the core fork with the original TITAN, update core or analysis JARs, or add a new analysis | [docs/MAINTENANCE.md](docs/MAINTENANCE.md) |
| Use the API | [Endpoints](#endpoints) (below) |

## Available analyses

Loaded at startup from the JARs in `plugins/`:

| Analysis name | JAR |
|---------------|-----|
| Extended Free Choice | `org.pnpl.analysis.extendedfreechoice_1.0.0.202604041448.jar` |
| Free Choice | `org.pnpl.analysis.freechoice_1.0.0.202603162205.jar` |
| Marked Graphs | `org.pnpl.analysis.markedgraph_1.0.0.202604041427.jar` |
| State Machines | `org.pnpl.analysis.statemachine_1.0.0.202604041427.jar` |

Each one is registered as type `pnpl` and type `products`.

## CORS

`CorsConfig.java` allows `GET` and `POST` on `/pnpl/**` from:
- `http://localhost:5173` (Vite dev server)
- `https://titan-front-end-sigma.vercel.app`
- `https://titan-api-pyts.onrender.com`

## Endpoints

| Method | Path | Body | Response |
|--------|------|------|----------|
| POST | `/pnpl/upload` | multipart: `vrb`, `petrinets`, `featureModel` | `{"vrbPath":"/tmp/titan-123/annotation.vrb","message":"Files uploaded successfully"}` |
| POST | `/pnpl/validate` | `{"vrbPath":"..."}` | `{"valid":true,"issues":[]}` |
| GET | `/pnpl/analyses` | none | `{"analyses":[{"name":"Free Choice","type":"pnpl"}, ...]}` |
| POST | `/pnpl/analyze` | `{"vrbPath":"...","name":"Free Choice","type":"pnpl"}` | `{"analysis":"Free Choice","allProducts":true,"timeMs":13,"message":"All products satisfy Free Choice"}` |

- `vrbPath`: absolute path on the server to the `.vrb`. The `.petrinets` and `model.xml` files must be in the same directory, with the names used in the `pn "..."` and `fm "..."` lines of the `.vrb`.
- `name`, `type`: exactly as returned by `GET /pnpl/analyses` (case-sensitive). `type` is `pnpl` (whole product line) or `products` (each derived product, see [Known limitations](#known-limitations)).
- `/pnpl/upload` saves the three files in a new temporary directory and returns the `vrbPath` to use in the other calls.
- `/pnpl/validate` always returns HTTP 200. Invalid models return `valid: false` with a list of `{severity, message, line, column}` issues.

Examples with curl are in [docs/QUICKSTART.md](docs/QUICKSTART.md#6-test-it).

## Intended usage flow

```
1. POST /pnpl/upload       -> send the 3 files, get vrbPath
2. POST /pnpl/validate     -> validate the model
3. GET  /pnpl/analyses     -> list available analyses (if valid)
4. POST /pnpl/analyze      -> run the selected analysis
```

## Project structure

```
titan-api-poc/
├── docs/QUICKSTART.md                            # Installation and running guide
├── docs/MAINTENANCE.md                           # Updating TITAN JARs and adding analyses
├── local-repo/                                   # File-based Maven repo: TITAN core + FeatureIDE JARs (not in Maven Central)
├── plugins/                                      # Analysis plugin JARs (scanned at startup)
├── Dockerfile                                    # Runtime image (needs target/*.jar built first)
├── pom.xml
└── src/main/java/poc/titan/api/
    ├── TitanApiApplication.java                  # Spring Boot entry point
    ├── CorsConfig.java                           # CORS configuration for the front end
    ├── analysis/
    │   ├── AnalysisDescriptor.java               # A discovered analysis (name, class, type)
    │   ├── PluginScanner.java                    # Scans plugins/ and builds the plugin class loader
    │   └── XtextInitializer.java                 # Registers the Xtext language and EMF packages at startup
    ├── controller/
    │   ├── AnalysisController.java               # GET /analyses, POST /analyze
    │   ├── FileUploadController.java             # POST /upload
    │   └── ValidationController.java             # POST /validate
    ├── dto/                                      # Request and response bodies
    └── service/
        ├── AnalysisService.java                  # Loads and runs an analysis by reflection
        └── ValidationService.java                # Xtext validation pipeline
```

## Validation architecture

At startup, `XtextInitializer` calls `PNPL_variabilityStandaloneSetup.createInjectorAndDoEMFRegistration()` (from `model-variability-editor`) to register the `.vrb` language and create a Guice injector. Per request, `ValidationService` creates a fresh `XtextResourceSet`, pre-loads the `.petrinets` file, loads the `.vrb` and runs `IResourceValidator.validate()`, which checks:

1. **Syntax**: the `.vrb` conforms to the `PNPL_variability.xtext` grammar.
2. **Cross-references**: places, transitions and arcs used in presence conditions exist in the `.petrinets` file.
3. **Semantics**: `@Check checkValidFeature` verifies that the features used exist in `model.xml`.

## Functional POC

This repository is part of a functional Proof of Concept (POC) of the TITAN refactoring.

Version: `poc-functional-v1`

Repositories:
- Core: https://github.com/TITAN-PNPL-WEB/titan-core-fork
- API: https://github.com/TITAN-PNPL-WEB/titan-api-poc
- Frontend: https://github.com/TITAN-PNPL-WEB/titan-front-end

## Known limitations

- `type: "products"` analyses always return `allProducts: false` with `timeMs: 0`: they still depend on Eclipse (`IFile`) and do not run headless. Use `type: "pnpl"`.
- `/pnpl/analyze` does not detect a missing `.petrinets` or `model.xml`: it can return HTTP 200 with a wrong result. Call `/pnpl/validate` first.
- Errors (missing file, unknown analysis) return a generic HTTP 500. The cause is only in the server log.
- `timeMs` only measures the analysis itself, not model loading.
- No authentication, no tests.
- Uploaded files are stored in the OS temporary directory and are never cleaned up.
- The formal API, frontend and CI/CD pipeline are planned as separate repositories under [TITAN-PNPL-WEB](https://github.com/TITAN-PNPL-WEB).
