# Not Enough Revives Reforged

Use `build/libs/NotEnoughRevivesReforged-neoforge-1.21.1-1.0.2-rc.5.jar` on both server and clients. Replace the old JAR; never install both. The locally supplied original JAR remains unchanged and is excluded from Git. Network protocol 3 requires matching updated clients.

## Documentation

- [Installation and controls](docs/INSTALLATION.md)
- [Complete configuration reference](docs/CONFIGURATION.md)
- [Build, tests and troubleshooting](docs/BUILDING.md)
- [Architecture and source map](docs/ARCHITECTURE.md)
- [Change history](CHANGELOG.md)
- [Attribution and distribution status](NOTICE.md)

Minecraft **1.21.1**, NeoForge **21.1.166**, Java **21**, release candidate **1.0.2-rc.5**. This repository contains source and build tooling; obtain the original build dependency separately as described in the build guide.

## Config and defaults

The complete default config is [config-defaults.json](config-defaults.json). New installations receive these defaults. Existing choices are preserved; copy that file to `config/not_enough_revives.json` while the server is stopped if you want to apply the new defaults to an existing installation. Restart after edits. Server settings and GUI text synchronize to clients on login.

Requested defaults include player finishing enabled, interaction/jump blocking enabled, three cooperative helpers, 300-second downed/help timers, void rescue disabled, vignette strength 2.0, amethyst revival sound at volume 0.8, and mining disabled.

Valid old configs automatically lose obsolete custom-sound upload/cooldown, player sound-pitch, help-sound distance and self-revive fields. Other unknown keys survive saves. Invalid files are backed up and left untouched; safe defaults apply in memory until the file is corrected and the server restarted.

## Gameplay and customization

- Downed players glow immediately until revival/death. Help is a single Use Item press, chat-only, server-validated and rate limited. The help/bleed-out prompts sit above the health bar and hotbar.
- The server interaction lock blocks mining, placing, block/entity interaction, attacks, use, containers, drops/swaps and charged weapon release. Client guards also suppress predicted mining/item use. Disable the lock to use the legacy individual interaction settings.
- Player finishing respects PvP/team permissions and recognizes player-owned projectiles. Environmental protection from the original mod is retained. Game mode is preserved.
- Downed state survives disconnect/restart; offline timers pause. Revive sessions restart after disconnect/restart. Registry IDs and existing vanilla player data are preserved.
- Cooperative revival adds one unit of progress per validated helper each tick, capped by `maxCooperativeRevivers` (1–16). Helpers need range, line of sight, live keepalives and any configured cost item. One cost item is consumed from the first remaining helper on completion. Progress resets if everyone stops.
- `revivalSound`, `revivalSoundVolume` and `revivalSoundPitch` select a registered Minecraft/mod sound event. Empty/unregistered IDs are silent. Default downing and player-finishing audio uses the normal hurt sound.
- `enableDownedImpactEffect`, `downedImpactDurationSeconds` (0.2–5, default 1.2) and `downedImpactStrength` (0–0.8, default 0.35) control a brief red pulse on entering the downed state. It fades smoothly, does not repeat on state updates, and clears on revival/death/disconnect.
- `enableDownedVignette`, `downedVignetteStrength` (0–4) and `downedBloodTint` (0–1) control the inward darkening. It uses each player's saved original timer and smoothly interpolates between server updates. Higher strength darkens sooner. HUD text remains readable; the vanilla death screen stays intact.
- GUI text fields: `downedTitle`, `bleedTimerText`, `revivalProgressText`, `reviverProgressText`, `reviveCancelText`, `helpPromptText`, `helpCooldownText`, `bleedOutPromptText`. Supported placeholders: `{seconds}`, `{key}`.
- Chat text fields: `helpChatMessage`, `bleedOutChatMessage`, `timedOutChatMessage`, `revivedChatMessage`. Supported placeholders: `{player}`, `{x}`, `{y}`, `{z}`. Revival also supports `{revivers}` (all helpers) and `{reviver}` (first helper). Default: `{player} was revived by {revivers}!`. `showReviveMessage` controls this completion broadcast. Text is literal, not executable commands or JSON components.
- Empty GUI text hides that line; empty help/revival chat text suppresses that broadcast. Minecraft's unrelated UI and normal damage-source death messages retain vanilla/localization behavior.

Self-revival, its recipe/creative entry and HUD are removed. `not_enough_revives:cross_of_revival` remains an inert registered item for existing saves.

The custom sound subsystem is removed from source and the packaged JAR: upload/download handlers and payloads, custom audio engine/decoder, GUI/textures, bundled audio and MP3 library. No custom sound config is read or written. Old user files are left untouched on disk. Only inert legacy sound registry IDs remain registered to avoid unnecessary world compatibility changes.

## Build and validation

This project selectively reconstructs changed classes over the supplied original JAR. Keep that JAR at the project root. Java 21, Gradle 8.8, ModDevGradle and NeoForge 21.1.166 are pinned; first builds download development dependencies.

```sh
./gradlew build --no-daemon
./gradlew runGameTestServer --no-daemon
./gradlew runSmokeClient --no-daemon
```

The build validates documented defaults, config migration/preservation and saved-state handling. Archive verification checks unchanged original entries, replacement classes, removed sound/upload entries, recipe removal, duplicates and mixin targets; common classes resolve with client classes forbidden.

A running dedicated GameTest server verifies glow, help cooldown, interaction/jump guards, cooperative revival, named completion chat, infinite effects, reconnect persistence, player finishing and creative-mode preservation. Test player packet listeners are captured fixtures, not two external network clients. A graphical client smoke test reaches the title screen. Test mods/fixtures are excluded from the release JAR.

## Manual multiplayer checks

Use a dedicated server and two or three matching clients, with a backed-up test world:

1. Down a player: immediate glow, no client block disappearance, no attacks/items/containers/jumping, help still available.
2. Tap/hold help: one coordinate message, no sound, no repeats during cooldown. Check negative coordinates and another dimension.
3. Revive with one, two and three helpers; compare speed, release/move out of range, check all helper names in chat and configured costs.
4. Edit GUI/chat templates and revival sound, restart, then check a second client's text/audio. Check empty text and `showReviveMessage=false`.
5. Test finishing with config true/false, melee/projectiles, PvP-disabled and protected teams. Check hurt audio on downing/finishing.
6. Check creative/adventure preservation through downing, revival, death/respawn, reconnect and restart.
7. Use a short timer to inspect vignette progression at several resolutions; verify elevated prompts clear vanilla health/hotbar elements.
8. Open an existing world containing crosses, verify item compatibility and no self-revival, then check logs for mixin/network errors.

Third-party damage sources without an owning player and mods with custom interaction packets need compatibility testing. Automated tests do not replace real multiplayer visual/audio testing.

## Public release

The original mod declares **All rights reserved**. Obtain redistribution permission from its author before publishing the combined JAR or reconstructed source, preserve attribution, and agree on fork branding. Modrinth's redistribution rules: https://modrinth.com/legal/rules . This is a private tested release candidate, not an assertion of public-upload permission.

### Damage handling fix

Downing is decided at the final player health write, after armor, Protection, Resistance, absorption and damage-event adjustments. High raw damage no longer incorrectly knocks down an armored player. Held totems retain priority; real lethal hits still enter the downed state.

### Downed chat announcements

Entering the downed state broadcasts `{player} was downed{cause}!` to chat. When an attacker is known, `{cause}` includes their name and held weapon. Environmental damage and command downing use a plain downed message. Reconnecting and repeated hits while downed do not repeat the announcement.

Set `showDownedMessage` to `false` to disable it, or customize `downedChatMessage` in `config/not_enough_revives.json`. Supported placeholders: `{player}`, `{cause}`, `{x}`, `{y}`, `{z}`. Existing revival and Call for Help messages remain separate.

### Vanilla finishing-hit messages

Finishing a downed player records the final hit in vanilla's combat tracker before death. Vanilla generates the resulting death message, including melee/projectile causes, attacker credit, custom weapon names and hover details, localization, and its normal death-message visibility rules. Bleed-out and timeout deaths retain their configured messages. Downed announcements preserve player/entity name and named-weapon components; only custom-named weapons are included by default, matching vanilla. The optional `{deathMessage}` placeholder inserts the damage source's full vanilla localized death message (including its death wording), useful in a custom template.
