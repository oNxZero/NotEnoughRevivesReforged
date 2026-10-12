# Configuration reference

Server file: `config/not_enough_revives.json`. Stop the server before editing and restart afterward. Settings and presentation text synchronize to clients on login. [config-defaults.json](../config-defaults.json) contains the complete example. Existing values are preserved; replacing the file with the example intentionally resets those choices.

Numeric values are clamped during validation. Malformed files or incorrect types are backed up and preserved; defaults are used in memory until corrected and restarted. Unknown unrelated keys survive saves. Obsolete custom-sound and self-revival fields are removed from valid configs.

| Key | Default | Meaning / valid range |
| --- | --- | --- |
| `allowPlayersToKillDownedPlayers` | `true` | Player melee and player-owned projectiles can finish downed players; PvP and team restrictions apply. |
| `disableInteractionsWhileDowned` | `true` | Master lock for mining, placing, attacking, using items, containers, dropping and swapping. |
| `allowPlayersToKillDownedPlayersComment` | `"When true, player attacks and player-owned projectiles can finish downed players. Environmental protection is unchanged."` | Generated explanatory text; validation restores the built-in explanation. |
| `disableInteractionsWhileDownedComment` | `"When true, blocks world interaction while downed. Call for Help remains available. When false, legacy interaction settings apply."` | Generated explanatory text; validation restores the built-in explanation. |
| `disableJumpWhileDowned` | `true` | Block jumping while downed. |
| `cooperativeRevival` | `true` | Enable progress from multiple validated helpers. |
| `maxCooperativeRevivers` | `3` | Helper cap; 1–16. |
| `enableDownedVignette` | `true` | Enable timer-driven inward darkening. |
| `downedVignetteStrength` | `2.0` | Vignette intensity; 0–4. |
| `downedBloodTint` | `0.25` | Red tint blend; 0–1. |
| `revivalSound` | `"minecraft:block.amethyst_block.chime"` | Registered sound-event ID; empty or unregistered IDs are silent. |
| `revivalSoundVolume` | `0.8` | Sound volume; 0–4. |
| `revivalSoundPitch` | `1.0` | Sound pitch; 0.5–2. |
| `downedTitle` | `"You are downed!"` | Downed HUD heading. |
| `bleedTimerText` | `"Bleeding out in {seconds}s"` | Remaining bleed-out timer HUD text. |
| `revivalProgressText` | `"Being revived : {seconds}s"` | Progress shown to the downed player. |
| `reviverProgressText` | `"Reviving : {seconds}s"` | Progress shown to a helper. |
| `reviveCancelText` | `"Release to cancel"` | Helper cancel instruction. |
| `helpPromptText` | `"[{key}] Call for Help"` | Call for Help key prompt. |
| `helpCooldownText` | `"Call for Help ({seconds}s)"` | Call for Help cooldown text. |
| `bleedOutPromptText` | `"[{key}] Bleed Out"` | Bleed Out key prompt. |
| `helpChatMessage` | `"{player} is downed and needs help! X: {x} Y: {y} Z: {z}"` | Help broadcast including coordinates. |
| `bleedOutChatMessage` | `"{player} chose to bleed out"` | Voluntary bleed-out chat message. |
| `revivedChatMessage` | `"{player} was revived by {revivers}!"` | Revival completion broadcast. |
| `timedOutChatMessage` | `"{player} could not be revived in time"` | Message when the downed timer expires. |
| `downedDurationSeconds` | `300` | Downed lifetime in seconds; 1–86400. Offline time pauses. |
| `reviveDurationSeconds` | `5` | Base single-helper revival duration in seconds; 1–3600. |
| `helpCallCooldownSeconds` | `300` | Help cooldown in seconds; 1–3600. |
| `allowBleedOut` | `true` | Allow the player to choose to bleed out. |
| `glowColour` | `"WHITE"` | Glow color name; accepted names listed below. |
| `showReviveMessage` | `true` | Enable completion broadcast. |
| `mobsIgnoreKnocked` | `true` | Ask mobs to ignore downed targets. |
| `enableVoidRescue` | `false` | Enable the retained original void-rescue behavior. |
| `reviveHealthPercent` | `0.3` | Restored fraction of maximum health; 0.01–1. |
| `knockedSpeedPercent` | `0.3` | Downed movement fraction; 0–1. |
| `miningSpeedMultiplier` | `0.0` | Legacy downed mining speed multiplier; 0–1. |
| `allowMiningWhenKnocked` | `false` | Legacy mining permission when the master interaction lock is disabled. |
| `allowAttackWhenKnocked` | `false` | Legacy attack permission when the master interaction lock is disabled. |
| `allowItemUseWhenKnocked` | `false` | Legacy item-use permission when the master interaction lock is disabled. |
| `reviverMaxDistance` | `3.5` | Maximum helper distance in blocks; 0.5–16, with line of sight required. |
| `voidRescueSearchRadius` | `8` | Void-rescue search radius; 0–16. |
| `voidRescueCooldownTicks` | `80` | Void-rescue cooldown in game ticks; 20–72000. |
| `voidRescueDepthTrigger` | `8` | Void-rescue depth trigger; 1–128. |
| `reviveCostItem` | `""` | Registered item ID required from helpers; empty disables cost. One item is consumed from the first remaining helper on completion. |
| `enableDownedImpactEffect` | `true` | Play a brief red screen pulse on first entering the downed state. |
| `downedImpactDurationSeconds` | `1.2` | Pulse duration in seconds; 0.2–5. |
| `downedImpactStrength` | `0.35` | Pulse strength; 0–0.8. |
| `showDownedMessage` | `true` | Enable entry announcement; does not repeat on reconnect or repeated hits. |
| `downedChatMessage` | `"{player} was downed{cause}!"` | Downed announcement with optional attacker/weapon components. |

## Text placeholders

HUD templates support `{seconds}` and `{key}`. Chat templates support `{player}`, `{x}`, `{y}`, `{z}`. Revival additionally supports `{revivers}` (all helpers) and `{reviver}` (first helper). Downed announcements additionally support `{cause}` and `{deathMessage}`; `{cause}` includes the known attacker and custom-named weapon, while `{deathMessage}` inserts the full vanilla localized damage-source death wording.

Empty HUD text hides that line. Empty help/revival chat text suppresses that broadcast. Templates are literal text, not commands or executable JSON components. String values are limited to 1024 characters; null values revert to defaults. Vanilla finishing-hit messages are produced by the combat tracker, with vanilla localization and visibility rules.

## Glow colors

`WHITE`, `RED`, `DARK_RED`, `BLUE`, `DARK_BLUE`, `GREEN`, `DARK_GREEN`, `YELLOW`, `GOLD`, `ORANGE`, `AQUA`, `DARK_AQUA`, `LIGHT_PURPLE`, `PINK`, `DARK_PURPLE`, `GRAY`, `DARK_GRAY`, `BLACK`. Names normalize to uppercase; unsupported names revert to `WHITE`.

## Item and sound IDs

Use namespaced registry IDs such as `minecraft:golden_apple` or `minecraft:block.amethyst_block.chime`. Invalid IDs are cleared. An unregistered cost item is cleared during server setup. Cooperative cost eligibility is checked per helper; only one item is consumed for a completed revival. Custom audio uploads and self-revival settings are no longer supported.
