# Repository and contribution workflow

GitHub repository: [oNxZero/NotEnoughRevivesReforged](https://github.com/oNxZero/NotEnoughRevivesReforged).

The repository is private. It tracks replacement source, resources, test fixtures, Gradle tooling, default configuration and documentation. The original dependency and generated mod JARs are excluded. See [NOTICE.md](../NOTICE.md) for attribution and distribution status.

## Getting a working checkout

```sh
git clone https://github.com/oNxZero/NotEnoughRevivesReforged.git
cd NotEnoughRevivesReforged
```

Access requires permission to the private repository. Obtain the original base JAR separately, place it at the checkout root, and follow [BUILDING.md](BUILDING.md) for its expected checksum, JDK and build commands.

## Making changes

Create a branch, change the relevant source or documentation, and run the build checks:

```sh
git switch -c your-change
JAVA_HOME=/path/to/jdk-21 ./gradlew build --no-daemon
git diff --check
git status --short
git add <changed-files>
git commit -m "Describe the resulting change"
git push -u origin your-change
```

Open a pull request describing the resulting behavior and validation. For gameplay changes, also run the dedicated GameTest server; for client changes, run the graphical smoke client and applicable manual multiplayer checks from the README. Record what actually ran and any remaining checks in [VALIDATION.md](VALIDATION.md).

Update the configuration reference and default JSON when settings change. Keep the build version, output filename, mod metadata, README and changelog consistent for a version change. Preserve protocol compatibility or update the protocol and document the requirement for matching clients and server.

## Files kept out of Git

`.gitignore` excludes Gradle caches, build output, runtime worlds/logs, local mod JARs, IDE state and environment files. The Gradle wrapper JAR is intentionally tracked. Inspect staged changes before committing; do not commit credentials, personal runtime data or the original dependency. Automated builds need the separately supplied base JAR, so a fresh clone alone cannot build the mod.

## Release preparation

Build and validate the packaged artifact using [BUILDING.md](BUILDING.md), then complete the README multiplayer checklist with matching clients. Record the version, test results and known compatibility limits. Public source or binary distribution remains subject to the permissions described in [NOTICE.md](../NOTICE.md).
