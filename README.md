# TITAN PNPL Web — POC Analysis API

Proof of concept of a REST API that exposes TITAN's analysis and validation capabilities as HTTP endpoints, decoupled from the Eclipse IDE.

## Context

TITAN is an Eclipse-based tool for the analysis of Petri Net Product Lines (PNPL). Originally, analyses and validations are triggered through the Eclipse UI. This POC demonstrates that the analysis and validation logic can be executed headlessly via a REST API, using the TITAN Eclipse plugins loaded dynamically from a `plugins/` directory.

## Requirements

- Java 22
- Maven 3.8+
- TITAN core JARs installed in local Maven repository (see [Dependencies](#dependencies))
- Analysis plugin JARs placed in the `plugins/` directory (see [Adding analyses](#adding-analyses))

## Dependencies

The following JARs must be exported from the [TITAN Eclipse project](https://github.com/TITAN-Petri-Nets-Product-Lines/TITAN) and installed manually in the local Maven repository:

| JAR | groupId | artifactId | version |
|-----|---------|------------|---------|
| `org.pnpl.analysis_1.0.0.jar` | org.pnpl | analysis-core | 1.0.0 |
| `org.pnpl.analysis.featureide_1.0.0.jar` | org.pnpl | analysis-featureide | 1.0.0 |
| `org.pnpl.solvers.sat_1.0.0.jar` | org.pnpl | solvers-sat | 1.0.0 |
| `org.pnpl.model.variability_1.0.0.jar` | org.pnpl | model-variability | 1.0.0 |
| `org.pnpl.model.variability.editor_1.0.0.jar` | org.pnpl | model-variability-editor | 1.0.0 |
| `org.pnpl.model.petrinet_1.0.0.jar` | org.pnpl | model-petrinet | 1.0.0 |

### Exporting JARs from Eclipse

For each plugin, open the TITAN project in Eclipse and go to:

**File → Export → Plug-in Development → Deployable plug-ins and fragments → select the plugin → Finish**

### Installing JARs in Maven

```bash
mvn install:install-file \
  -Dfile=<path-to-jar> \
  -DgroupId=org.pnpl \
  -DartifactId=<artifactId> \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

## Adding analyses

Analysis plugins are **not declared in `pom.xml`**. Instead, they are loaded dynamically at startup from the `plugins/` directory in the project root.

To add a new analysis:
1. Export the plugin JAR from Eclipse (same process as above)
2. Copy the JAR to the `plugins/` directory
3. Restart the API

The API will discover the new analysis automatically — no code changes required.

Current plugins in `plugins/`:

| JAR |
|-----|
| `org.pnpl.analysis.freechoice_1.0.0.jar` |
| `org.pnpl.analysis.extendedfreechoice_1.0.0.jar` |
| `org.pnpl.analysis.markedgraph_1.0.0.jar` |
| `org.pnpl.analysis.statemachine_1.0.0.jar` |

## Running the API

```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

## Endpoints

### GET /pnpl/analyses

Returns the list of all available analyses discovered from the `plugins/` directory.

**Response:**
```json
{
  "analyses": [
    { "name": "Free Choice", "type": "pnpl" },
    { "name": "Free Choice", "type": "products" },
    { "name": "Extended Free Choice", "type": "pnpl" },
    { "name": "Extended Free Choice", "type": "products" },
    { "name": "Marked Graphs", "type": "pnpl" },
    { "name": "Marked Graphs", "type": "products" },
    { "name": "State Machines", "type": "pnpl" },
    { "name": "State Machines", "type": "products" }
  ]
}
```

**Example with curl:**
```bash
curl http://localhost:8080/pnpl/analyses
```

---

### POST /pnpl/validate

Validates a `.vrb` file using the Xtext parser and validator in standalone mode (no Eclipse). Runs three layers of validation: syntax (ANTLR parser from grammar), cross-references (elements exist in `.petrinets`), and semantic checks (features exist in `model.xml`).

**Request body:**
```json
{
  "vrbPath": "/absolute/path/to/annotation.vrb"
}
```

- `vrbPath` — absolute path to the `.vrb` file. The `.petrinets` and `.xml` files referenced inside the `.vrb` must be in the same directory.

**Response (valid):**
```json
{
  "valid": true,
  "issues": []
}
```

**Response (invalid):**
```json
{
  "valid": false,
  "issues": [
    {
      "severity": "ERROR",
      "message": "no viable alternative at input 'AND'",
      "line": 7,
      "column": 39
    },
    {
      "severity": "ERROR",
      "message": "Feature 'AND' does not exist",
      "line": 7,
      "column": 39
    }
  ]
}
```

**Example with curl:**
```bash
curl -X POST http://localhost:8080/pnpl/validate \
  -H "Content-Type: application/json" \
  -d '{
    "vrbPath": "/path/to/examples/annotation.vrb"
  }'
```

---

### POST /pnpl/analyze

Runs the specified analysis on a given `.vrb` file.

**Request body:**
```json
{
  "vrbPath": "/absolute/path/to/model.vrb",
  "name": "Free Choice",
  "type": "pnpl"
}
```

- `vrbPath` — absolute path to the `.vrb` file. The `.petrinets` and `.xml` files must be in the same directory.
- `name` — name of the analysis to run, as returned by `GET /pnpl/analyses`.
- `type` — type of analysis: `pnpl` (analysis over the full product line) or `products` (analysis over each derived product).

**Response:**
```json
{
  "analysis": "Free Choice",
  "allProducts": true,
  "timeMs": 700,
  "message": "All products satisfy Free Choice"
}
```

**Example with curl:**
```bash
curl -X POST http://localhost:8080/pnpl/analyze \
  -H "Content-Type: application/json" \
  -d '{
    "vrbPath": "/path/to/examples/150mm.vrb",
    "name": "Free Choice",
    "type": "pnpl"
  }'
```

## Project structure

```
titan-api-poc/
├── plugins/                                      # Analysis plugin JARs (loaded at startup)
│   ├── org.pnpl.analysis.freechoice_1.0.0.jar
│   └── ...
└── src/main/java/poc/titan/api/
    ├── TitanApiApplication.java                  # Spring Boot entry point
    ├── analysis/
    │   ├── AnalysisDescriptor.java               # Represents a discovered analysis
    │   ├── PluginScanner.java                    # Scans plugins/ and registers analyses
    │   └── XtextInitializer.java                 # Registers Xtext language at startup
    ├── controller/
    │   ├── AnalysisController.java               # REST endpoints for analysis
    │   └── ValidationController.java             # REST endpoint for validation
    ├── dto/
    │   ├── AnalysesResponse.java                 # Response for GET /analyses
    │   ├── AnalysisRequest.java                  # Request for POST /analyze
    │   ├── AnalysisResponse.java                 # Response for POST /analyze
    │   ├── ValidationRequest.java                # Request for POST /validate
    │   └── ValidationResponse.java               # Response for POST /validate
    └── service/
        ├── AnalysisService.java                  # Analysis execution logic
        └── ValidationService.java                # Xtext validation pipeline
```

## Validation architecture

The validation endpoint uses the Xtext language infrastructure from the `model-variability-editor` dependency in standalone mode (no Eclipse/OSGi). At startup, `XtextInitializer` calls `PNPL_variabilityStandaloneSetup.createInjectorAndDoEMFRegistration()` to register the `.vrb` language and creates a Guice injector. Per request, `ValidationService` creates a fresh `XtextResourceSet`, pre-loads the `.petrinets` file for cross-reference resolution, loads the `.vrb` with the Xtext parser, and runs `IResourceValidator.validate()` which triggers:

1. **Syntax validation** — ANTLR parser checks the `.vrb` conforms to the `PNPL_variability.xtext` grammar
2. **Cross-reference validation** — verifies that element names in presence conditions (places, transitions, arcs) exist in the referenced `.petrinets` file
3. **Semantic validation** — `@Check checkValidFeature` in `PNPL_variabilityValidator` verifies that feature names used in presence condition expressions exist in the `model.xml` feature model

## Functional POC

This repository is part of a functional Proof of Concept (POC) of the TITAN refactoring.

### Version

```
poc-functional-v1
```

### Repositories

- Core: https://github.com/TITAN-PNPL-WEB/titan-core-fork
- API: https://github.com/TITAN-PNPL-WEB/titan-api-poc

Both repositories must be used with the same tag: `poc-functional-v1`

### What it includes

- Dynamic discovery of analysis plugins from `plugins/` directory
- `GET /pnpl/analyses` — list all available analyses
- `POST /pnpl/validate` — validate `.vrb` files using Xtext standalone
- `POST /pnpl/analyze` — run any analysis generically
- End-to-end execution decoupled from Eclipse

## Notes

- This is a POC — no authentication, no error handling, no tests.
- The `.vrb` path is resolved as an absolute path on the server.
- The formal API, frontend, and CI/CD pipeline are planned as separate repositories under [TITAN-PNPL-WEB](https://github.com/TITAN-PNPL-WEB).