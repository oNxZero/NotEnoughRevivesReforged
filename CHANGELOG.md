# 1.0.2-rc.5

- Replace rectangular vignette bands with an aspect-aware circular gradient, smooth falloff and filtered cached texture.
- Preserve timer progression, blood tint, strength and HUD readability.

# 1.0.2-rc.4

- Add a brief configurable red screen pulse when first downed, fading over 1.2 seconds.
- Keep the existing timer vignette and readable HUD; state updates do not replay the pulse.

# 1.0.2-rc.3

- Apply requested defaults: finishing enabled, three helpers, five-minute downed/help timers, vignette strength 2.0, sound volume 0.8, no void rescue/mining.
- Broadcast configurable completion chat naming all contributing revivers.
- Remove custom sound upload/download handlers, payloads, GUI, audio engine/decoder, audio assets and bundled MP3 decoder from the release JAR.
- Remove obsolete sound/self-revive config fields, migrating valid existing configs while preserving unrelated keys.
- Keep only inert legacy sound/item registry entries for world compatibility.
- Add documented-default, config-migration, archive-removal and named-revival integration checks.
- Protocol 3 requires matching updated server and clients.

# 1.0.2-rc.2

- Stop client-predicted mining, attacks and item use while locked downed.
- Preserve game mode instead of forcing survival when finished.
- Make help chat-only; disable the old custom-sound hotkey and uploads.
- Configure vanilla revival sound ID, volume and pitch; play normal hurt audio on downing and player finishing.
- Prevent jumping while downed, with a server config switch.
- Add validated cooperative revival, helper limits and synchronized participant-aware HUD timing.
- Customize all mod downed HUD and help/bleed-out chat text from server config.
- Simplify the revival HUD and remove out-of-range/first-frame hold prompts.
- Add a configurable inward darkening/blood tint effect scaled to each saved downed timer.
- Version network protocol: update both server and clients together.

# 1.0.2-rc.1

- Always glow while downed; single-press help with server-wide coordinates and cooldown.
- Add server-authoritative interaction locking and configurable player finishing.
- Disable self-revival and normal cross obtainability while retaining its registry ID.
- Persist downed state through reconnects and server restarts.
- Validate and atomically save configs; preserve malformed files and unknown keys.
- Bound custom sound transfers, caches and audio decoding; validate sender identity and rate limit uploads.
- Validate revive sessions and preserve infinite effects on revival.
- Use the official NeoForge development build, correct resource-pack metadata, and reproducible JAR packaging.
- Add archive, regression, server gameplay and graphical client smoke checks.

Public release requires original-author redistribution permission and the multiplayer compatibility checks in README.md.
