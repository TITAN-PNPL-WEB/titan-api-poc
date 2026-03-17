# TITAN PNPL Web — POC Free Choice

Proof of concept of a REST API that exposes the Free Choice analysis from [TITAN](https://github.com/TITAN-Petri-Nets-Product-Lines/TITAN) as an HTTP endpoint, decoupled from the Eclipse IDE.

## Context

TITAN is an Eclipse-based tool for the analysis of Petri Net Product Lines (PNPL). Originally, analyses are triggered through the Eclipse UI. This POC demonstrates that the analysis logic can be executed headlessly via a REST API, using the TITAN Eclipse plugins exported as JARs.

## Requirements

- Java 22
- Maven 3.8+
- TITAN JARs installed in local Maven repository (see [Dependencies](#dependencies))

## Dependencies

The following JARs must be exported from the [TITAN Eclipse project](https://github.com/TITAN-Petri-Nets-Product-Lines/TITAN) and installed manually in the local Maven repository:

| JAR | groupId | artifactId | version |
|-----|---------|------------|---------|
| `org.pnpl.analysis_1.0.0.jar` | org.pnpl | analysis-core | 1.0.0 |
| `org.pnpl.analysis.freechoice_1.0.0.jar` | org.pnpl | analysis-freechoice | 1.0.0 |
| `org.pnpl.analysis.featureide_1.0.0.jar` | org.pnpl | analysis-featureide | 1.0.0 |
| `org.pnpl.solvers.sat_1.0.0.jar` | org.pnpl | solvers-sat | 1.0.0 |
| `org.pnpl.model.variability_1.0.0.jar` | org.pnpl | model-variability | 1.0.0 |
| `org.pnpl.model.variability.editor_1.0.0.jar` | org.pnpl | model-variability-editor | 1.0.0 |
| `org.pnpl.model.petrinet_1.0.0.jar` | org.pnpl | model-petrinet | 1.0.0 |

### Exporting JARs from Eclipse

For each plugin, open the TITAN project in Eclipse and go to:

**File → Export → Plug-in Development → Deployable plug-ins and fragments → select the plugin → Finish**

The exported JAR is the one to use as `-Dfile` in the `mvn install` command below.

### Installing JARs in Maven

Install each JAR with:

```bash
mvn install:install-file \
  -Dfile=<path-to-jar> \
  -DgroupId=org.pnpl \
  -DartifactId=<artifactId> \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

Example for `analysis-freechoice`:

```bash
mvn install:install-file \
  -Dfile=org.pnpl.analysis.freechoice_1.0.0.jar \
  -DgroupId=org.pnpl \
  -DartifactId=analysis-freechoice \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

## Running the API

```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

## Endpoint

### POST /pnpl/freechoice

Runs the Free Choice analysis on a given `.vrb` file.

**Request body:**
```json
{
  "vrbPath": "/absolute/path/to/model.vrb"
}
```

The `.vrb` file must reference the `.petrinets` and `.xml` files in the same directory.

**Response:**
```json
{
  "analysis": "Free Choice",
  "allProducts": true,
  "timeMs": 700,
  "message": "All products are Free Choice"
}
```

**Example with curl:**
```bash
curl -X POST http://localhost:8080/pnpl/freechoice \
  -H "Content-Type: application/json" \
  -d '{"vrbPath": "/path/to/examples/150mm.vrb"}'
```

## Project structure

```
src/main/java/pocfreechoice/
├── PocFreeChoiceApplication.java   # Spring Boot entry point
├── controller/
│   └── PnplController.java         # REST endpoint
├── service/
│   └── FreeChoiceService.java      # Analysis logic
└── dto/
    ├── AnalyzeRequest.java
    └── AnalyzeResponse.java
```

## Functional POC

This repository is part of a functional Proof of Concept (POC) of the TITAN refactoring.

### Version

The POC is versioned using:

`poc-functional-v1`

### Repositories

- Core: https://github.com/TITAN-PNPL-WEB/titan-core-fork
- API: https://github.com/TITAN-PNPL-WEB/titan-api-poc

Both repositories must be used with the same tag:
`poc-functional-v1`

### What it includes

- Free-choice Petri net analysis
- REST API integration
- End-to-end execution

## Notes

- This is a POC — no authentication, no error handling, no tests.
- The `.vrb` path is resolved as an absolute path on the server.
- The formal API, frontend, and CI/CD pipeline are planned as separate repositories under [TITAN-PNPL-WEB](https://github.com/TITAN-PNPL-WEB).