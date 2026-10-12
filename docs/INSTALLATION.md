# Installation and player controls

1. Back up the world and existing configuration.
2. Install Minecraft 1.21.1 with a compatible NeoForge loader (development/testing is pinned to 21.1.166).
3. Build the reforged JAR following [BUILDING.md](BUILDING.md).
4. Stop the server and clients. Replace the previous Not Enough Revives JAR in each `mods/` folder with the same reforged build.
5. Start the server to generate `config/not_enough_revives.json`. Stop it before editing; restart to apply settings.

Do not install both the original and reforged JAR: they share the `not_enough_revives` mod ID. A local base JAR is needed to build, but is not a second runtime mod.

## Controls and gameplay

- A lethal hit puts an eligible player into a timed downed state, with glow and a HUD timer.
- While downed, press Use Item (normally right mouse) once to request help in chat. The server applies the configured cooldown.
- Hold Use Item while targeting a nearby downed player to revive them. Helpers must remain within range and line of sight. Cooperative helpers accelerate progress up to the configured cap.
- The Bleed Out key defaults to **B** and can be changed in Minecraft Controls. It is available when `allowBleedOut` is enabled.
- Default settings block world interaction and jumping while downed. Players can finish downed players subject to PvP/team rules.
- Self-revival is removed. Old cross items retain their registry ID but are inert.

The server synchronizes presentation text and settings to clients on login. Use [CONFIGURATION.md](CONFIGURATION.md) for every supported option and the README for the multiplayer acceptance checklist.
