# Maintenance

How to bring changes from the original TITAN repository into this API.

| Repository | Role |
|------------|------|
| [TITAN-Petri-Nets-Product-Lines/TITAN](https://github.com/TITAN-Petri-Nets-Product-Lines/TITAN) | Original (upstream) |
| [TITAN-PNPL-WEB/titan-core-fork](https://github.com/TITAN-PNPL-WEB/titan-core-fork) | Fork with the headless changes |
| [TITAN-PNPL-WEB/titan-api-poc](https://github.com/TITAN-PNPL-WEB/titan-api-poc) | This API. Uses JARs exported from the fork. |

Steps marked **(untested)** were not run end to end.

## Case 1: the original repository changed

### 1. Sync the fork

You need write access to `titan-core-fork`.

**Option A: GitHub web (untested)**

1. Open https://github.com/TITAN-PNPL-WEB/titan-core-fork.
2. Click **Sync fork** -> **Update branch**.
3. If GitHub reports conflicts, **do not click "Discard commits"**: it deletes the headless changes. Use option B.

**Option B: command line**

1. ```bash
   git clone https://github.com/TITAN-PNPL-WEB/titan-core-fork.git
   ```
2. ```bash
   cd titan-core-fork
   ```
3. ```bash
   git remote add upstream https://github.com/TITAN-Petri-Nets-Product-Lines/TITAN.git
   ```
4. ```bash
   git fetch upstream
   ```
5. ```bash
   git merge upstream/main
   ```
6. If there are conflicts, keep **both** sides. The fork only **adds** code (no deletions), in these 4 files:
   - `org.pnpl.analysis/.../analyzer/AbstractAnalysis.java`
   - `org.pnpl.analysis/.../helpers/VariabilityHelper.java`
   - `org.pnpl.analysis.featureide/.../helper/EMFHandler.java`
   - `org.pnpl.analysis.featureide/.../composer/FeatureModelProviderHeadless.java` (new file)
7. ```bash
   git push origin main
   ```

### 2. See which projects changed

```bash
git diff --stat <commit-before-sync> HEAD -- src/
```

Each folder in `src/` is an Eclipse project. What to do depends on the project:

| Changed project | Where it goes in the API | Go to |
|-----------------|--------------------------|-------|
| `org.pnpl.analysis`, `org.pnpl.analysis.featureide`, `org.pnpl.solvers.sat`, `org.pnpl.model.petrinet`, `org.pnpl.model.variability`, `org.pnpl.model.variability.editor` | `local-repo/` (core) | Step 4 |
| `org.pnpl.analysis.extendedfreechoice`, `.freechoice`, `.markedgraph`, `.statemachine` | `plugins/` (analysis) | Step 5 |
| Anything else (`*.ui`, `*.edit`, `export.*`, ...) | Not used by the API | Nothing to do |

### 3. Export the changed projects from Eclipse (untested here)

1. Import the fork into Eclipse: **File -> Import -> Existing Projects into Workspace**, select `titan-core-fork/src`.
2. **File -> Export -> Plug-in Development -> Deployable plug-ins and fragments**.
3. Select the changed projects, choose a destination folder (not inside `~/.m2`), **Finish**.
4. The JARs are in `<destination>/plugins/`.

### 4. Core project changed: update `local-repo/`

For each core JAR, from the root of `titan-api-poc`. Always use a **new version** (Maven caches the old one):

1. ```bash
   mvn deploy:deploy-file -Durl=file://$PWD/local-repo -DrepositoryId=project-local -Dfile=<path-to-jar> -DgroupId=org.pnpl -DartifactId=<artifactId> -Dversion=<new-version> -Dpackaging=jar -DgeneratePom=true
   ```
2. In `pom.xml`, set that dependency's `<version>` to `<new-version>`.

| Eclipse project | artifactId |
|-----------------|------------|
| `org.pnpl.analysis` | `analysis-core` |
| `org.pnpl.analysis.featureide` | `analysis-featureide` |
| `org.pnpl.solvers.sat` | `solvers-sat` |
| `org.pnpl.model.petrinet` | `model-petrinet` |
| `org.pnpl.model.variability` | `model-variability` |
| `org.pnpl.model.variability.editor` | `model-variability-editor` |

### 5. Analysis project changed: update `plugins/`

1. Delete the old JAR of that analysis from `plugins/`. If two JARs declare the same analysis, the API uses whichever it finds first, in no guaranteed order.
2. Copy the new JAR to `plugins/`.

### 6. Build, run, test

1. ```bash
   mvn clean package
   ```
2. ```bash
   java -jar target/titan-api-poc-1.0-SNAPSHOT.jar
   ```
3. Test as in [QUICKSTART.md, step 6](QUICKSTART.md#6-test-it).
4. Commit `plugins/`, `local-repo/` and `pom.xml` in `titan-api-poc`.

## Case 2: a new analysis was added

The API needs **no code change**. It finds analyses by reading the `plugin.xml` of each JAR in `plugins/` at startup.

1. **Sync the fork** (Case 1, step 1) so the new project is in `titan-core-fork`.
2. **Check `plugin.xml`** of the new project. It must register the class under this extension point:
   ```xml
   <extension point="org.pnpl.analysis.analyzer.pnpl">
      <client class="org.pnpl.analysis.<name>.analysis.AnalysisPNPL" name="<Analysis Name>"/>
   </extension>
   ```
   `name` is the exact name the API will show. `org.pnpl.analysis.analyzer.products` can also be registered, but `products` analyses do not work in the API (see [README, Known limitations](../README.md#known-limitations)).
3. **Check `META-INF/MANIFEST.MF`** (`Require-Bundle`, `Import-Package`) for dependencies:
   - The 6 core `org.pnpl.*` projects from step 4: nothing to do.
   - Any other `org.pnpl.*` project (for example `org.pnpl.analysis.invariant`): export it too and copy its JAR to `plugins/`.
   - A third-party library (for example `org.eclipse.collections`): add it to `pom.xml` from Maven Central. If it is not in Maven Central, add it to `local-repo/` (step 4).
4. **Export** the new project, plus any extra `org.pnpl.*` project from step 3 (Case 1, step 3).
5. **Copy** the JARs to `plugins/`.
6. **Build and run** (Case 1, step 6).
7. **Check it is listed:**
   ```bash
   curl http://localhost:8080/pnpl/analyses
   ```
   The new `name` must appear. If not: the JAR is not in `plugins/`, `plugin.xml` is missing from the JAR, or the extension point is misspelled.
8. **Run it:**
   ```bash
   curl -X POST http://localhost:8080/pnpl/analyze -H "Content-Type: application/json" -d '{"vrbPath":"/absolute/path/to/model/annotation.vrb","name":"<Analysis Name>","type":"pnpl"}'
   ```
   HTTP 500 with `java.lang.ClassNotFoundException: <class>` in the API log means a dependency is missing. Find which project or library contains `<class>`, add it (step 3), and repeat from step 5.
9. **Commit** `plugins/`, and `pom.xml` / `local-repo/` if they changed.

The analysis must not depend on the Eclipse workspace (`IFile`, `IProject`). Use `vh.getPetriNet()` and `vh.getFeatureModel()`, like the existing analyses.
