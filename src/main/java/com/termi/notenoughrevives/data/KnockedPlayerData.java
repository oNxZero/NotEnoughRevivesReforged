package com.termi.notenoughrevives.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.effect.MobEffectInstance;

public class KnockedPlayerData {
    public final UUID a;
    public int b;
    public int totalDownedTicks;
    public int c;
    public UUID d;
    public int e;
    public int reviveHeartbeatTick;
    public final java.util.Map<UUID, Integer> revivers = new java.util.LinkedHashMap<>();
    public boolean f;
    public int g;
    public String h = "WHITE";
    public double j = Double.NaN;
    public double k = Double.NaN;
    public double l = Double.NaN;
    public boolean p = false;
    public List<MobEffectInstance> q = new ArrayList<MobEffectInstance>();
    public int r = 0;
    public int effectElapsedTicks = 0;
    public CompoundTag s;

    public KnockedPlayerData(UUID uUID, int n) {
        this.a = uUID;
        this.b = n * 20;
        this.totalDownedTicks = this.b;
    }

    public boolean a() {
        return this.d != null;
    }
}

