# Validation record

On 2026-10-12, ran with JDK 21:

```sh
JAVA_HOME=/usr/lib/jvm/java-21-openjdk bash gradlew --offline build --no-daemon
```

Result: **BUILD SUCCESSFUL**. All 20 stability assertions passed. Archive verification preserved 25 original entries byte-for-byte, found no removed recipe or duplicate entries, verified new mixin target methods, and resolved common classes with client classes forbidden.

Malformed-config and invalid-saved-state error messages are expected fixtures in this regression run. The dedicated GameTest server and graphical smoke client were not rerun for this documentation/repository preparation. Historical test coverage and remaining real multiplayer checks are described in the README; common-class resolution alone is not a complete server launch.

Documentation preparation adds installation, complete configuration, build/troubleshooting, architecture and attribution guides. Generated build output, runtime logs, local credentials and the original mod archive are excluded from Git.
