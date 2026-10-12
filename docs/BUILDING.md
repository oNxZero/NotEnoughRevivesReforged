# Building and testing

## Requirements

- Minecraft 1.21.1 and NeoForge 21.1.166 for development.
- JDK 21; Gradle 8.8 is downloaded by the included wrapper.
- ModDevGradle 2.0.148 (pinned in `build.gradle`).
- A lawfully obtained original `not-enough-revives-neoforge-1.21.1-1.0.1.jar`, placed at the repository root. This dependency is not committed.

Expected original JAR SHA-256:

```text
ffe28dab80d68aa181f17a6c0fbbcd334d34b9f0d21e7ed91957ca7b4ef44732
```

This is a selective reconstruction, not a standalone rewrite. `gradle/patch.gradle` overlays compiled replacement classes and resources onto the original archive. Unchanged classes/assets come from that archive; removals are listed in `gradle/removed-legacy-entries.txt`. A different base JAR may break compilation or verification.

## Commands

From the repository root on Linux/macOS:

```sh
export JAVA_HOME=/path/to/jdk-21
./gradlew build --no-daemon
./gradlew runGameTestServer --no-daemon
./gradlew runSmokeClient --no-daemon
```

The checked-in launcher is a Bash script. On Windows, use WSL with JDK 21 or an installed Gradle 8.8; no Windows wrapper launcher is currently supplied.

The first build needs network access to retrieve dependencies. Once cached, add `--offline`. Output: `build/libs/NotEnoughRevivesReforged-neoforge-1.21.1-1.0.2-rc.5.jar`.

`build` invokes `check`, including `stabilityTests` (configuration/defaults/migration and persistence) and `verifyPatch` (packaged entries, removed legacy content, replacements, mixins and dedicated-server class resolution). `runGameTestServer` adds server gameplay integration tests. `runSmokeClient` needs a graphical desktop and checks client startup. See the README multiplayer checklist for checks requiring actual players.

## Troubleshooting

- Missing original classes/JAR: supply the exact base archive above; do not use a reforged output as the base.
- Wrong Java: check `java -version` and `JAVA_HOME`; compilation requires 21.
- Offline dependency failure: run once without `--offline` on a connected machine.
- Client/server disconnect: install the same reforged version on all sides; this edition uses network protocol 3.
- Configuration edits ignored: stop the server, edit its config, and restart. Existing values are intentionally preserved.
- Invalid JSON: consult the server log and repair the original file. Safe defaults apply in memory; invalid files are backed up rather than overwritten.
