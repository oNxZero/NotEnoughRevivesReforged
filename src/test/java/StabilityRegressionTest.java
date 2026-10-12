import com.google.gson.JsonParser;
import com.termi.notenoughrevives.config.JsonConfigStore;
import com.termi.notenoughrevives.config.NERConfig;
import com.termi.notenoughrevives.data.DownedStateSavedData;
import com.termi.notenoughrevives.data.KnockedPlayerData;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.sound.sampled.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public final class StabilityRegressionTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Files.createDirectories(Path.of(args[0])), "case-");
        config(directory);
        savedState();
        System.out.println("Passed " + checks + " stability regression assertions.");
    }

    private static JsonConfigStore<NERConfig> store(Path path) {
        return new JsonConfigStore<>(path, NERConfig.class, NERConfig::new, NERConfig::validate);
    }
    private static void config(Path directory) throws Exception {
        Path path = directory.resolve("config.json");
        NERConfig defaults = store(path).load();
        check(defaults.allowPlayersToKillDownedPlayers && defaults.disableInteractionsWhileDowned, "Unsafe new defaults");
        check(defaults.downedDurationSeconds == 300 && defaults.helpCallCooldownSeconds == 300 && defaults.maxCooperativeRevivers == 3, "Requested default timers/helper limit lost");
        check(defaults.downedVignetteStrength == 2f && defaults.revivalSoundVolume == 0.8f && !defaults.enableVoidRescue && !defaults.allowMiningWhenKnocked && defaults.miningSpeedMultiplier == 0f, "Requested display/world defaults lost");
        var defaultsJson = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        check(!defaultsJson.has("allowCustomSoundUploads") && !defaultsJson.has("allowPitchChange") && !defaultsJson.has("maxHelpSoundDistance"), "Legacy sound defaults remain");
        check(defaultsJson.equals(JsonParser.parseString(Files.readString(Path.of("config-defaults.json")))), "Complete documented defaults do not match generated server config");
        String original = "{\"downedDurationSeconds\": 120, \"unknownFutureSetting\": {\"keep\": true}, \"disableInteractionsWhileDowned\": false}";
        Files.writeString(path, original);
        NERConfig config = store(path).load();
        check(config.downedDurationSeconds == 120 && !config.disableInteractionsWhileDowned, "Existing values lost");
        check(JsonParser.parseString(Files.readString(path)).getAsJsonObject().has("unknownFutureSetting"), "Unknown config key erased");
        Files.writeString(path, "{\"allowCustomSoundUploads\":true,\"customSoundUploadCooldownSeconds\":10,\"allowPitchChange\":true,\"maxHelpSoundDistance\":50,\"selfReviveItem\":\"cross\",\"unknownFutureSetting\":42}");
        store(path).load();
        var migrated = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        check(!migrated.has("allowCustomSoundUploads") && !migrated.has("customSoundUploadCooldownSeconds") && !migrated.has("allowPitchChange") && !migrated.has("maxHelpSoundDistance") && !migrated.has("selfReviveItem") && migrated.has("unknownFutureSetting"), "Legacy config migration failed");
        String broken = "{ invalid config";
        Files.writeString(path, broken);
        JsonConfigStore<NERConfig> invalid = store(path);
        invalid.load(); invalid.save(new NERConfig());
        check(Files.readString(path).equals(broken), "Broken config overwritten");
        try (var files = Files.list(directory)) { check(files.anyMatch(file -> file.getFileName().toString().contains(".invalid-")), "No invalid-file backup"); }
        String wrongType = "{\"allowBleedOut\": \"false\"}";
        Files.writeString(path, wrongType); store(path).load();
        check(Files.readString(path).equals(wrongType), "Wrong-type config overwritten");
        config = new NERConfig();
        config.downedDurationSeconds = -4; config.reviveDurationSeconds = Integer.MAX_VALUE;
        config.helpCallCooldownSeconds = 0; config.reviveHealthPercent = Float.NaN;
        config.knockedSpeedPercent = 12; config.reviverMaxDistance = Double.POSITIVE_INFINITY;
        config.glowColour = null; config.reviveCostItem = "bad id!";
        config.validate();
        check(config.downedDurationSeconds == 1 && config.reviveDurationSeconds == 3600, "Timer bounds invalid");
        check(config.helpCallCooldownSeconds == 1 && Float.isFinite(config.reviveHealthPercent), "Unsafe cooldown/health");
        check(config.knockedSpeedPercent == 1 && Double.isFinite(config.reviverMaxDistance), "Invalid movement/range");
        check(config.voidRescueSearchRadius <= 16, "Unbounded rescue range");
        check(config.glowColour.equals("WHITE") && config.reviveCostItem.isEmpty(), "Invalid strings retained");
    }

    private static void savedState() {
        UUID owner = UUID.randomUUID();
        KnockedPlayerData data = new KnockedPlayerData(owner, 60);
        data.b = 812; data.c = 45; data.effectElapsedTicks = 388;
        data.d = UUID.randomUUID(); data.e = 71;
        data.s = new CompoundTag(); data.s.putInt("originalBodyDamage", 9);
        DownedStateSavedData saved = new DownedStateSavedData();
        saved.update(Map.of(owner, data));
        CompoundTag encoded = saved.save(new CompoundTag(), null);
        // A malformed entry must not discard another player's valid state.
        ListTag entries = encoded.getList("Players", 10); entries.add(new CompoundTag());
        KnockedPlayerData restored = DownedStateSavedData.load(encoded, null).players().get(owner);
        check(restored != null && restored.b == 812 && restored.c == 45, "Downed timers lost across save/load");
        check(restored.effectElapsedTicks == 388 && restored.s.getInt("originalBodyDamage") == 9, "Saved effect/compatibility state lost");
        check(restored.d == null && restored.e == 0 && !restored.p && restored.f, "Reviver or self-revival persisted unexpectedly");
        saved.update(Map.of());
        check(DownedStateSavedData.load(saved.save(new CompoundTag(), null), null).players().isEmpty(), "Revived/dead state reappears after restart");
    }

}
