package com.termi.notenoughrevives.config;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;

public class NERConfig {
    private static JsonConfigStore<NERConfig> store;
    private static NERConfig instance;
    // Player melee and player-owned projectiles may finish an already downed player.
    public boolean allowPlayersToKillDownedPlayers = true;
    // Blocks world interaction while downed; Call for Help and revival remain available.
    public boolean disableInteractionsWhileDowned = true;
    public String allowPlayersToKillDownedPlayersComment = "When true, player attacks and player-owned projectiles can finish downed players. Environmental protection is unchanged.";
    public String disableInteractionsWhileDownedComment = "When true, blocks world interaction while downed. Call for Help remains available. When false, legacy interaction settings apply.";
    public boolean disableJumpWhileDowned = true;
    public boolean cooperativeRevival = true;
    public int maxCooperativeRevivers = 3;
    public boolean enableDownedImpactEffect = true;
    public float downedImpactDurationSeconds = 1.2f;
    public float downedImpactStrength = 0.35f;
    public boolean enableDownedVignette = true;
    public float downedVignetteStrength = 2f;
    public float downedBloodTint = 0.25f;
    public String revivalSound = "minecraft:block.amethyst_block.chime";
    public float revivalSoundVolume = 0.8f;
    public float revivalSoundPitch = 1f;
    public String downedTitle = "You are downed!";
    public String bleedTimerText = "Bleeding out in {seconds}s";
    public String revivalProgressText = "Being revived : {seconds}s";
    public String reviverProgressText = "Reviving : {seconds}s";
    public String reviveCancelText = "Release to cancel";
    public String helpPromptText = "[{key}] Call for Help";
    public String helpCooldownText = "Call for Help ({seconds}s)";
    public String bleedOutPromptText = "[{key}] Bleed Out";
    public String helpChatMessage = "{player} is downed and needs help! X: {x} Y: {y} Z: {z}";
    public String bleedOutChatMessage = "{player} chose to bleed out";
    public boolean showDownedMessage = true;
    public String downedChatMessage = "{player} was downed{cause}!";
    public String revivedChatMessage = "{player} was revived by {revivers}!";
    public String timedOutChatMessage = "{player} could not be revived in time";
    public int downedDurationSeconds = 300;
    public int reviveDurationSeconds = 5;
    public int helpCallCooldownSeconds = 300;
    public boolean allowBleedOut = true;
    public String glowColour = "WHITE";
    public boolean showReviveMessage = true;
    public boolean mobsIgnoreKnocked = true;
    public boolean enableVoidRescue = false;
    public float reviveHealthPercent = 0.3f;
    public float knockedSpeedPercent = 0.3f;
    public float miningSpeedMultiplier = 0.0f;
    public boolean allowMiningWhenKnocked = false;
    public boolean allowAttackWhenKnocked = false;
    public boolean allowItemUseWhenKnocked = false;
    public double reviverMaxDistance = 3.5;
    public int voidRescueSearchRadius = 8;
    public int voidRescueCooldownTicks = 80;
    public int voidRescueDepthTrigger = 8;
    public String reviveCostItem = "";

    public static NERConfig get() {
        if (instance == null) {
            NERConfig.load();
        }
        return instance;
    }

    public static synchronized void load() {
        store = new JsonConfigStore<>(FMLPaths.CONFIGDIR.get().resolve("not_enough_revives.json"),
                NERConfig.class, NERConfig::new, NERConfig::validate);
        instance = store.load();
    }

    public static synchronized void save() {
        if (instance != null && store != null) store.save(instance);
    }

    public void validate() {
        downedImpactDurationSeconds = clamp(downedImpactDurationSeconds, 0.2f, 5, 1.2f);
        downedImpactStrength = clamp(downedImpactStrength, 0, 0.8f, 0.35f);
        maxCooperativeRevivers = clamp(maxCooperativeRevivers, 1, 16);
        downedVignetteStrength = clamp(downedVignetteStrength, 0, 4, 2);
        downedBloodTint = clamp(downedBloodTint, 0, 1, 0.25f);
        revivalSoundVolume = clamp(revivalSoundVolume, 0, 4, 0.8f);
        revivalSoundPitch = clamp(revivalSoundPitch, 0.5f, 2, 1);
        revivalSound = revivalSound == null ? "" : revivalSound.trim();
        if (!revivalSound.isEmpty() && ResourceLocation.tryParse(revivalSound) == null) revivalSound = "";
        for (var field : NERConfig.class.getFields()) {
            if (field.getType() == String.class && !java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                try {
                    String value = (String) field.get(this);
                    if (value == null) value = (String) field.get(new NERConfig());
                    field.set(this, value.length() > 1024 ? value.substring(0, 1024) : value);
                } catch (IllegalAccessException error) { throw new IllegalStateException(error); }
            }
        }
        downedDurationSeconds = clamp(downedDurationSeconds, 1, 86400);
        reviveDurationSeconds = clamp(reviveDurationSeconds, 1, 3600);
        helpCallCooldownSeconds = clamp(helpCallCooldownSeconds, 1, 3600);
        reviveHealthPercent = clamp(reviveHealthPercent, 0.01f, 1, 0.3f);
        knockedSpeedPercent = clamp(knockedSpeedPercent, 0, 1, 0.3f);
        miningSpeedMultiplier = clamp(miningSpeedMultiplier, 0, 1, 0.15f);
        reviverMaxDistance = Double.isFinite(reviverMaxDistance) ? Math.clamp(reviverMaxDistance, 0.5, 16) : 3.5;
        voidRescueSearchRadius = clamp(voidRescueSearchRadius, 0, 16);
        voidRescueCooldownTicks = clamp(voidRescueCooldownTicks, 20, 72000);
        voidRescueDepthTrigger = clamp(voidRescueDepthTrigger, 1, 128);
        glowColour = glowColour == null ? "WHITE" : glowColour.toUpperCase(Locale.ROOT);
        if (!java.util.Set.of("WHITE", "RED", "DARK_RED", "BLUE", "DARK_BLUE", "GREEN", "DARK_GREEN",
                "YELLOW", "GOLD", "ORANGE", "AQUA", "DARK_AQUA", "LIGHT_PURPLE", "PINK", "DARK_PURPLE", "GRAY", "DARK_GRAY", "BLACK").contains(glowColour)) glowColour = "WHITE";
        reviveCostItem = reviveCostItem == null ? "" : reviveCostItem.trim();
        if (!reviveCostItem.isEmpty() && ResourceLocation.tryParse(reviveCostItem) == null) reviveCostItem = "";
        allowPlayersToKillDownedPlayersComment = "When true, player attacks and player-owned projectiles can finish downed players. Environmental protection is unchanged.";
        disableInteractionsWhileDownedComment = "When true, blocks world interaction while downed. Call for Help remains available. When false, legacy interaction settings apply.";
    }

    private static int clamp(int value, int min, int max) { return Math.clamp(value, min, max); }
    private static float clamp(float value, float min, float max, float fallback) {
        return Float.isFinite(value) ? Math.clamp(value, min, max) : fallback;
    }
}
