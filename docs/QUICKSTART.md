# Quick start

Run `titan-api-poc` locally from a clean machine.

Steps marked **(untested)** were not run on a clean machine of that OS. Everything else was verified on macOS with an empty Maven repository.

## 1. Requirements

| Tool | Version | Notes |
|------|---------|-------|
| Java JDK | 22 or newer | `pom.xml` sets `java.version` 22. Tested with JDK 22 and JDK 25. JDK 21 or older fails. |
| Maven | 3.6.3 or newer | Tested with 3.8.4. |
| Git | any recent version | |
| curl | any | Only to test the API. |

## 2. Install from scratch

Install the tools, then open a **new** terminal so `PATH` is reloaded.

### Windows (untested)

Use PowerShell to install. Use **Git Bash** (installed with Git) for every later step.

1. Install the JDK:
   ```powershell
   winget install EclipseAdoptium.Temurin.25.JDK
   ```
2. Install Git:
   ```powershell
   winget install Git.Git
   ```
3. Install Maven: download `apache-maven-<version>-bin.zip` from https://maven.apache.org/download.cgi, unzip it to `C:\maven`, and add `C:\maven\bin` to `PATH`:
   ```powershell
   setx PATH "$env:PATH;C:\maven\bin"
   ```

### macOS (untested on a clean machine)

Requires Homebrew (https://brew.sh).

1. Install JDK, Maven and Git:
   ```bash
   brew install openjdk maven git
   ```
2. Make the JDK visible to the system:
   ```bash
   sudo ln -sfn "$(brew --prefix)/opt/openjdk/libexec/openjdk.jdk" /Library/Java/JavaVirtualMachines/openjdk.jdk
   ```

### Linux (untested)

Uses SDKMAN for Java and Maven, so it does not depend on the distribution's JDK version.

1. Install Git, curl and zip (Debian/Ubuntu):
   ```bash
   sudo apt update && sudo apt install -y git curl zip unzip
   ```
2. Install SDKMAN:
   ```bash
   curl -s "https://get.sdkman.io" | bash
   ```
3. Load SDKMAN in the current terminal:
   ```bash
   source "$HOME/.sdkman/bin/sdkman-init.sh"
   ```
4. Install the JDK:
   ```bash
   sdk install java 25-tem
   ```
5. Install Maven:
   ```bash
   sdk install maven
   ```

### Verify (all OS)

1. ```bash
   java -version
   ```
   Must show version 22 or newer.
2. ```bash
   mvn -version
   ```
   The `Java version:` line must show 22 or newer. Maven uses this JDK, not the one from `java -version`.
3. ```bash
   git --version
   ```

## 3. Get the code

1. ```bash
   git clone https://github.com/TITAN-PNPL-WEB/titan-api-poc.git
   ```
2. ```bash
   cd titan-api-poc
   ```

Run every following command from this folder.

## 4. Prepare dependencies

Nothing to do. The TITAN core and FeatureIDE JARs are not in Maven Central. They are included in the repo in `local-repo/`, and `pom.xml` points to it. Maven downloads everything else from Maven Central on the first build (internet required).

Check that the folder exists:
```bash
ls local-repo/org/pnpl
```
Expected: `analysis-core  analysis-featureide  model-petrinet  model-variability  model-variability-editor  solvers-sat`

## 5. Build and run

1. Build:
   ```bash
   mvn clean package
   ```
   Expected: `BUILD SUCCESS`.
2. Run:
   ```bash
   java -jar target/titan-api-poc-1.0-SNAPSHOT.jar
   ```
   Alternative to steps 1 and 2: `mvn spring-boot:run`.

The API listens on **http://localhost:8080**. It is ready when the log shows `Started TitanApiApplication`. The log must also show `[scanner] Found analyses: [...]` with 8 entries.

Stop it with `Ctrl+C`.

## 6. Test it

Open a second terminal. You need a PNPL model: a folder with `annotation.vrb`, its `.petrinets` file and `model.xml`. The repo does not include one. Replace `/absolute/path/to/model` with your folder.

1. List the analyses:
   ```bash
   curl http://localhost:8080/pnpl/analyses
   ```
   Expected (order may vary):
   ```json
   {"analyses":[{"name":"Free Choice","type":"pnpl"},{"name":"Free Choice","type":"products"},{"name":"State Machines","type":"pnpl"},{"name":"State Machines","type":"products"},{"name":"Extended Free Choice","type":"pnpl"},{"name":"Extended Free Choice","type":"products"},{"name":"Marked Graphs","type":"pnpl"},{"name":"Marked Graphs","type":"products"}]}
   ```
2. Validate the model (always do this first, see issues 9 and 10):
   ```bash
   curl -X POST http://localhost:8080/pnpl/validate -H "Content-Type: application/json" -d '{"vrbPath":"/absolute/path/to/model/annotation.vrb"}'
   ```
   Expected:
   ```json
   {"valid":true,"issues":[]}
   ```
3. Run an analysis:
   ```bash
   curl -X POST http://localhost:8080/pnpl/analyze -H "Content-Type: application/json" -d '{"vrbPath":"/absolute/path/to/model/annotation.vrb","name":"Free Choice","type":"pnpl"}'
   ```
   Expected (values depend on the model):
   ```json
   {"analysis":"Free Choice","allProducts":true,"timeMs":13,"message":"All products satisfy Free Choice"}
   ```

## 7. Issues and fixes

| # | Error message | Cause | Fix |
|---|---------------|-------|-----|
| 1 | `Could not resolve dependencies ... org.pnpl:analysis-core:jar:1.0.0 ... de.ovgu.featureide:featureide-fm-core:jar:3.11.1 ... in central` | `local-repo/` is missing (old clone), or a mirror in `~/.m2/settings.xml` uses `<mirrorOf>*</mirrorOf>` and hides `local-repo/`. | Update the clone: `git pull`. If you have a `*` mirror, change it to `<mirrorOf>*,!project-local</mirrorOf>`. |
| 2 | `mvn: command not found` / `'mvn' is not recognized` (same for `java`) | The tool is not in `PATH`. | Open a new terminal. If it persists, add the tool's `bin` folder to `PATH`. macOS/Linux: `export PATH="<folder>/bin:$PATH"`. Windows: `setx PATH "$env:PATH;<folder>\bin"`. |
| 3 | `Fatal error compiling: error: release version 22 not supported` | Maven runs with a JDK older than 22 (see `Java version:` in `mvn -version`). | Point `JAVA_HOME` to JDK 22+. macOS: `export JAVA_HOME=$(/usr/libexec/java_home -v 22+)`. Linux: `sdk use java 25-tem`. Windows: `setx JAVA_HOME "<JDK folder>"`. |
| 4 | `UnsupportedClassVersionError ... class file version 66.0` | `java -jar` runs with a JDK older than 22. | Use JDK 22+ (see issue 3), then check `java -version`. |
| 5 | `Web server failed to start. Port 8080 was already in use.` | Another process (often a previous run of the API) uses port 8080. | Stop it, or use another port: `java -jar target/titan-api-poc-1.0-SNAPSHOT.jar --server.port=8081`. Find the process: `lsof -i :8080` (macOS/Linux), `netstat -ano \| findstr :8080` (Windows). |
| 6 | `{"analyses":[]}` and log `[scanner] plugins/ directory not found, skipping.` | The API was started from another folder. `plugins/` is resolved from the current folder. | `cd titan-api-poc` and start it again. |
| 7 | An analysis is missing or a JAR change has no effect | `plugins/` is scanned only at startup. | Check the JAR is in `plugins/` and restart the API. |
| 8 | HTTP 500 on `/pnpl/analyze`, log `java.nio.file.NoSuchFileException: ...` | `vrbPath` does not exist. A relative path is resolved from the API's folder, not yours. | Use an absolute path to an existing `.vrb`. |
| 9 | HTTP 500 on `/pnpl/analyze`, log `Analysis not found: name='...' type='...'` | `name` or `type` do not match exactly. They are case-sensitive: `"PNPL"` fails. | Copy `name` and `type` from `GET /pnpl/analyses`. `type` is `pnpl` or `products`. |
| 10 | HTTP 200 on `/pnpl/analyze`, but the result is wrong (`timeMs` near 0) | The `.petrinets` or `model.xml` file is not in the `.vrb` folder, or its name differs from the `pn "..."` / `fm "..."` lines. These are resolved relative to the `.vrb` folder. The API does not report it. | Put the three files in the same folder. Run `/pnpl/validate`: errors like `Couldn't resolve reference` or `Imported resource could not be found` confirm it. |
| 11 | `"type":"products"` always returns `allProducts: false` with `timeMs: 0` | Known limitation: the `products` analyses still need Eclipse (`IFile`) and do not run headless. | Use `"type":"pnpl"`. |
| 12 | `curl` JSON errors on Windows | PowerShell quotes differently. | Run the `curl` commands in Git Bash. **(untested)** |
