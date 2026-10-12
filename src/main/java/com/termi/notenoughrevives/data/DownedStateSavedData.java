package com.termi.notenoughrevives.data;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.LoggerFactory;

/** Additive world data; no registry changes or edits to vanilla player files. */
public final class DownedStateSavedData extends SavedData {
    public static final String NAME = "not_enough_revives_downed";
    public static final Factory<DownedStateSavedData> FACTORY = new Factory<>(DownedStateSavedData::new, DownedStateSavedData::load);
    private final Map<UUID, KnockedPlayerData> players = new HashMap<>();

    public Map<UUID, KnockedPlayerData> players() { return new HashMap<>(players); }

    public void update(Map<UUID, KnockedPlayerData> current) {
        players.clear();
        players.putAll(current);
        setDirty();
    }

    public static DownedStateSavedData load(CompoundTag root, HolderLookup.Provider registries) {
        DownedStateSavedData saved = new DownedStateSavedData();
        ListTag entries = root.getList("Players", Tag.TAG_COMPOUND);
        for (int index = 0; index < entries.size(); index++) {
            try {
                CompoundTag tag = entries.getCompound(index);
                UUID id = tag.getUUID("UUID");
                KnockedPlayerData data = new KnockedPlayerData(id, 1);
                data.b = Math.clamp(tag.getInt("RemainingTicks"), 0, 86400 * 20);
                data.totalDownedTicks = tag.contains("TotalTicks") ? Math.clamp(tag.getInt("TotalTicks"), Math.max(1, data.b), 86400 * 20) : Math.max(1, data.b + tag.getInt("EffectElapsedTicks"));
                data.c = Math.clamp(tag.getInt("HelpCooldownTicks"), 0, 3600 * 20);
                data.effectElapsedTicks = Math.clamp(tag.getInt("EffectElapsedTicks"), 0, 86400 * 20);
                data.f = true;
                data.h = tag.getString("GlowColour");
                if (data.h.isEmpty()) data.h = "WHITE";
                data.j = finiteOrNaN(tag.getDouble("SafeX"));
                data.k = finiteOrNaN(tag.getDouble("SafeY"));
                data.l = finiteOrNaN(tag.getDouble("SafeZ"));
                ListTag effects = tag.getList("Effects", Tag.TAG_COMPOUND);
                for (int effectIndex = 0; effectIndex < effects.size(); effectIndex++) {
                    MobEffectInstance effect = MobEffectInstance.load(effects.getCompound(effectIndex));
                    if (effect != null) data.q.add(effect);
                }
                if (tag.contains("BodyDamage", Tag.TAG_COMPOUND)) data.s = tag.getCompound("BodyDamage").copy();
                saved.players.put(id, data);
            } catch (RuntimeException error) {
                LoggerFactory.getLogger("not_enough_revives").error("Skipping invalid downed-state entry {}", index, error);
            }
        }
        return saved;
    }

    @Override public CompoundTag save(CompoundTag root, HolderLookup.Provider registries) {
        root.putInt("Version", 1);
        ListTag entries = new ListTag();
        for (var entry : players.entrySet()) {
            KnockedPlayerData data = entry.getValue();
            CompoundTag tag = new CompoundTag();
            tag.putUUID("UUID", entry.getKey());
            tag.putInt("RemainingTicks", data.b);
            tag.putInt("TotalTicks", data.totalDownedTicks);
            tag.putInt("HelpCooldownTicks", data.c);
            tag.putInt("EffectElapsedTicks", data.effectElapsedTicks);
            tag.putString("GlowColour", data.h == null ? "WHITE" : data.h);
            tag.putDouble("SafeX", data.j);
            tag.putDouble("SafeY", data.k);
            tag.putDouble("SafeZ", data.l);
            ListTag effects = new ListTag();
            for (MobEffectInstance effect : data.q) effects.add(effect.save());
            tag.put("Effects", effects);
            if (data.s != null) tag.put("BodyDamage", data.s.copy());
            entries.add(tag);
        }
        root.put("Players", entries);
        return root;
    }

    private static double finiteOrNaN(double value) { return Double.isFinite(value) ? value : Double.NaN; }
}
