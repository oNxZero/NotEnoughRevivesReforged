# Source and runtime architecture

| Location | Responsibility |
| --- | --- |
| `src/main/java/com/termi/notenoughrevives/NotEnoughRevives.java` | Mod setup, config and network registration |
| `manager/KnockedManager.java` | Downing, validated cooperative sessions, help, finishing and revival |
| `data/` | Downed player state and world saved data; offline timers pause |
| `config/` | Config validation, migration and preservation of unknown fields |
| `util/AtomicFiles.java` | Atomic persistence helpers |
| `network/payload/` | Replacement help, state and presentation payloads |
| `mixin/` | Final-health damage interception, interaction and movement guards, finishing |
| `client/` | Input, revive targeting, network state and presentation |
| `client/hud/` | HUD prompts, progress, impact pulse and vignette |
| `src/test/` | Regression checks and packaged archive verification |
| `src/gameTests/` | Dedicated-server integration fixtures |
| `src/clientSmoke/` | Client startup fixture |
| `gradle/patch.gradle` | Overlay packaging and verification tasks |

Paths after the first row are relative to `src/main/java/com/termi/notenoughrevives/` unless otherwise shown. Some referenced original classes intentionally have no Java source here and come from the base archive.

The server decides damage, downed state, revive eligibility, costs and completion. Clients provide target/keepalive requests and render synchronized server settings. Final health interception accounts for armor, enchantments, effects, absorption and damage-event adjustments; held totems retain priority. Sessions require eligible helpers within range and line of sight. Losing all helpers resets progress. One configured cost item is consumed on completion.

Persistence retains downed players across disconnect/restart, while live revive sessions restart. Registry IDs for the removed self-revival item and legacy sounds remain for existing-world compatibility. Custom sound uploads, playback engine and bundled decoder are removed from the packaged artifact.

The original package and some short/decompiled member names are retained for binary compatibility with unchanged classes. Protocol 3 requires matching clients and server. Test fixtures are excluded from the release archive.
