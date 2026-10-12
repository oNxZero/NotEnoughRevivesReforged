package com.termi.notenoughrevives.manager;

import com.termi.notenoughrevives.NotEnoughRevives;
import com.termi.notenoughrevives.config.NERConfig;
import com.termi.notenoughrevives.data.KnockedPlayerData;
import com.termi.notenoughrevives.data.DownedStateSavedData;
import com.termi.notenoughrevives.gamerule.NERGameRules;
import com.termi.notenoughrevives.mixin.LivingEntityAccessor;
import com.termi.notenoughrevives.network.payload.ShowKnockedGuiPayload;
import com.termi.notenoughrevives.network.payload.SyncKnockedPayload;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

public final class KnockedManager {
    public static final ResourceKey<DamageType> a = ResourceKey.create((ResourceKey)Registries.DAMAGE_TYPE, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"not_enough_revives", (String)"ner_bleed_out"));
    public static final ResourceKey<DamageType> b = ResourceKey.create((ResourceKey)Registries.DAMAGE_TYPE, (ResourceLocation)ResourceLocation.fromNamespaceAndPath((String)"not_enough_revives", (String)"ner_not_revived"));
    private static final ResourceLocation c = ResourceLocation.fromNamespaceAndPath((String)"not_enough_revives", (String)"knocked_slow");
    private static final Map<UUID, KnockedPlayerData> d = new ConcurrentHashMap<UUID, KnockedPlayerData>();
    private static final Set<UUID> e = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> f = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Integer> g = new ConcurrentHashMap<UUID, Integer>();
    private static final Map<UUID, Component> h = new ConcurrentHashMap<UUID, Component>();
    private static final Map<UUID, Integer> k = new ConcurrentHashMap<UUID, Integer>();
    private static DownedStateSavedData savedState;
    private static int m = 0;
    private static boolean n = false;
    private static Supplier<?> o = null;

    public static void loadState(MinecraftServer server) {
        a();
        savedState = server.overworld().getDataStorage().computeIfAbsent(DownedStateSavedData.FACTORY, DownedStateSavedData.NAME);
        d.putAll(savedState.players());
    }

    private static void persistState() {
        if (savedState != null) savedState.update(d);
    }

    public static void saveState(MinecraftServer server) {
        persistState();
        server.overworld().getDataStorage().save();
    }

    public static void syncAllTo(ServerPlayer recipient) {
        NERConfig config = NERConfig.get();
        for (var entry : d.entrySet()) {
            if (recipient.getServer().getPlayerList().getPlayer(entry.getKey()) == null) continue;
            KnockedPlayerData data = entry.getValue();
            PacketDistributor.sendToPlayer(recipient, new SyncKnockedPayload(entry.getKey(), true, data.b, data.a(), data.e,
                    config.reviveDurationSeconds * 20, true, data.h, data.c, false, data.totalDownedTicks, java.util.List.copyOf(data.revivers.keySet())));
        }
    }

    private static double b() {
        return -(1.0 - (double)NERConfig.get().knockedSpeedPercent);
    }

    public static boolean a(UUID uUID) {
        return d.containsKey(uUID);
    }

    public static boolean b(UUID uUID) {
        return e.contains(uUID);
    }

    public static boolean c(UUID uUID) {
        return f.contains(uUID);
    }

    public static KnockedPlayerData d(UUID uUID) {
        return d.get(uUID);
    }

    public static Component e(UUID uUID) {
        return h.remove(uUID);
    }

    public static boolean h(UUID uUID) {
        return g.getOrDefault(uUID, 0) > 0;
    }

    public static void i(UUID uUID) {
        d.remove(uUID);
        persistState();
        f.remove(uUID);
        h.remove(uUID);
    }

    public static void j(UUID uUID) {
        d.remove(uUID);
        persistState();
        if (!e.contains(uUID)) {
            f.remove(uUID);
        }
    }

    public static void a(MinecraftServer minecraftServer, ServerPlayer serverPlayer) {
        UUID uUID = serverPlayer.getUUID();
        KnockedPlayerData knockedPlayerData = d.get(uUID);
        if (knockedPlayerData == null) {
            return;
        }
        serverPlayer.setInvulnerable(true);
        serverPlayer.setGlowingTag(true);
        if (NERConfig.get().disableInteractionsWhileDowned) {
            serverPlayer.stopUsingItem();
            serverPlayer.closeContainer();
        }
        serverPlayer.setHealth(1.0f);
        KnockedManager.h(serverPlayer);
        serverPlayer.setPose(Pose.SWIMMING);
        PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)new ShowKnockedGuiPayload(NERConfig.get().allowBleedOut), (CustomPacketPayload[])new CustomPacketPayload[0]);
        KnockedManager.a(minecraftServer, uUID);
    }

    public static boolean a(ServerPlayer serverPlayer) {
        return a(serverPlayer, (net.minecraft.world.damagesource.DamageSource)null);
    }

    public static boolean a(ServerPlayer serverPlayer, net.minecraft.world.damagesource.DamageSource source) {
        UUID uUID = serverPlayer.getUUID();
        if (e.contains(uUID)) {
            return false;
        }
        if (f.contains(uUID)) {
            return false;
        }
        if (KnockedManager.h(uUID)) {
            return false;
        }
        if (!serverPlayer.serverLevel().getGameRules().getBoolean(NERGameRules.DO_KNOCKED)) {
            return false;
        }
        if (KnockedManager.a(uUID)) {
            return false;
        }
        NERConfig nERConfig = NERConfig.get();
        KnockedPlayerData knockedPlayerData = new KnockedPlayerData(uUID, nERConfig.downedDurationSeconds);
        knockedPlayerData.f = true;
        knockedPlayerData.h = nERConfig.glowColour;
        d.put(uUID, knockedPlayerData);
        float originalHealth = serverPlayer.getHealth();
        try {
            serverPlayer.setInvulnerable(true);
            serverPlayer.setGlowingTag(true);
            if (NERConfig.get().disableInteractionsWhileDowned) {
                serverPlayer.stopUsingItem();
                serverPlayer.closeContainer();
            }
            serverPlayer.setHealth(1.0f);
            KnockedManager.h(serverPlayer);
            serverPlayer.setPose(Pose.SWIMMING);
            knockedPlayerData.q = new ArrayList<MobEffectInstance>();
            for (MobEffectInstance mobEffectInstance : serverPlayer.getActiveEffects()) {
                knockedPlayerData.q.add(new MobEffectInstance(mobEffectInstance));
            }
            knockedPlayerData.r = serverPlayer.getServer().getTickCount();
            knockedPlayerData.effectElapsedTicks = 0;
            serverPlayer.removeAllEffects();
            serverPlayer.clearFire();
            KnockedManager.a(serverPlayer, knockedPlayerData);
            persistState();
            playHurtSound(serverPlayer);
            PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)new ShowKnockedGuiPayload(nERConfig.allowBleedOut), (CustomPacketPayload[])new CustomPacketPayload[0]);
            KnockedManager.a(serverPlayer.getServer(), uUID);
            if (nERConfig.showDownedMessage && !nERConfig.downedChatMessage.isEmpty()) {
                MutableComponent cause = Component.empty();
                if (source != null) {
                    var attacker = source.getEntity() != null ? source.getEntity() : source.getDirectEntity();
                    if (attacker != null) {
                        cause.append(" by ").append(attacker.getDisplayName());
                        if (attacker instanceof net.minecraft.world.entity.LivingEntity living
                                && living.getMainHandItem().has(net.minecraft.core.component.DataComponents.CUSTOM_NAME)) {
                            cause.append(" using ").append(living.getMainHandItem().getDisplayName());
                        }
                    }
                }
                serverPlayer.getServer().getPlayerList().broadcastSystemMessage(
                        formatDownedMessage(nERConfig.downedChatMessage, serverPlayer, cause, source), false);
            }
            return true;
        }
        catch (Exception exception) {
            NotEnoughRevives.a.error("NER: tryKnock failed for {}, undoing", (Object)uUID, (Object)exception);
            d.remove(uUID);
            persistState();
            serverPlayer.setGlowingTag(false);
            serverPlayer.setHealth(originalHealth);
            for (MobEffectInstance effect : knockedPlayerData.q) serverPlayer.addEffect(new MobEffectInstance(effect));
            KnockedManager.b(serverPlayer, knockedPlayerData);
            try {
                serverPlayer.setInvulnerable(false);
            }
            catch (Exception exception2) {
                // empty catch block
            }
            try {
                KnockedManager.i(serverPlayer);
            }
            catch (Exception exception3) {
                // empty catch block
            }
            try {
                KnockedManager.j(serverPlayer);
            }
            catch (Exception exception4) {
                // empty catch block
            }
            return false;
        }
    }

    public static void b(ServerPlayer serverPlayer) {
        if (!KnockedManager.a(serverPlayer.getUUID())) {
            return;
        }
        KnockedPlayerData knockedPlayerData = d.remove(serverPlayer.getUUID());
        persistState();
        try {
            int n;
            serverPlayer.setInvulnerable(false);
            serverPlayer.setHealth(serverPlayer.getMaxHealth() * NERConfig.get().reviveHealthPercent);
            KnockedManager.i(serverPlayer);
            KnockedManager.j(serverPlayer);
            serverPlayer.setGlowingTag(false);
            if (knockedPlayerData != null && knockedPlayerData.q != null) {
                int n2 = knockedPlayerData.effectElapsedTicks;
                for (MobEffectInstance effect : knockedPlayerData.q) {
                    n = effect.isInfiniteDuration() ? -1 : effect.getDuration() - n2;
                    if (n <= 0 && !effect.isInfiniteDuration()) continue;
                    serverPlayer.addEffect(new MobEffectInstance(effect.getEffect(), n, effect.getAmplifier(), effect.isAmbient(), effect.isVisible(), effect.showIcon()));
                }
            }
            KnockedManager.b(serverPlayer, knockedPlayerData);
            if (knockedPlayerData != null && knockedPlayerData.d != null) {
                String cost = NERConfig.get().reviveCostItem;
                ServerPlayer reviver = serverPlayer.getServer().getPlayerList().getPlayer(knockedPlayerData.d);
                if (cost != null && !cost.isEmpty() && reviver != null) {
                    Item costItem = BuiltInRegistries.ITEM.get(ResourceLocation.parse(cost));
                    if (costItem != Items.AIR) KnockedManager.b(reviver, costItem);
                }
            }
            KnockedManager.a(serverPlayer.getServer(), serverPlayer.getUUID());
            if (NERConfig.get().showReviveMessage && !NERConfig.get().revivedChatMessage.isEmpty()) {
                java.util.List<String> names = new java.util.ArrayList<>();
                if (knockedPlayerData != null) for (UUID helperId : knockedPlayerData.revivers.keySet()) {
                    ServerPlayer helper = serverPlayer.getServer().getPlayerList().getPlayer(helperId);
                    if (helper != null) names.add(helper.getDisplayName().getString());
                }
                String revivers = names.isEmpty() ? "server" : String.join(", ", names);
                serverPlayer.getServer().getPlayerList().broadcastSystemMessage(Component.literal(formatPlayerText(NERConfig.get().revivedChatMessage, serverPlayer)
                    .replace("{revivers}", revivers).replace("{reviver}", names.isEmpty() ? "server" : names.getFirst())), false);
            }
            NERConfig soundConfig = NERConfig.get();
            ResourceLocation soundId = ResourceLocation.tryParse(soundConfig.revivalSound);
            if (soundId != null && BuiltInRegistries.SOUND_EVENT.containsKey(soundId)) {
                serverPlayer.serverLevel().playSound(null, serverPlayer.getX(), serverPlayer.getY(), serverPlayer.getZ(),
                    BuiltInRegistries.SOUND_EVENT.get(soundId), SoundSource.PLAYERS, soundConfig.revivalSoundVolume, soundConfig.revivalSoundPitch);
            }
        }
        catch (Exception exception) {
            NotEnoughRevives.a.error("NER: error during revive for {}", (Object)serverPlayer.getUUID(), (Object)exception);
        }
        finally {
            UUID uUID = serverPlayer.getUUID();
            if (d.containsKey(uUID)) {
                d.remove(uUID);
            }
            try {
                serverPlayer.setInvulnerable(false);
            }
            catch (Exception exception) {}
            try {
                KnockedManager.i(serverPlayer);
            }
            catch (Exception exception) {}
            try {
                KnockedManager.j(serverPlayer);
            }
            catch (Exception exception) {}
            try {
                serverPlayer.setGlowingTag(false);
            }
            catch (Exception exception) {}
        }
    }

    public static void clearOnDeath(ServerPlayer player) {
        j(player.getUUID());
        player.setGlowingTag(false);
        player.setInvulnerable(false);
        i(player);
        j(player);
        a(player.getServer(), player.getUUID());
    }

    public static void c(ServerPlayer serverPlayer) {
        KnockedManager.a(serverPlayer, true);
    }

    public static void d(ServerPlayer serverPlayer) {
        KnockedManager.a(serverPlayer, false);
    }

    public static void e(ServerPlayer serverPlayer) {
        KnockedManager.a(serverPlayer, false);
    }

    public static void finishByPlayer(ServerPlayer player, DamageSource source) {
        if (!NERConfig.get().allowPlayersToKillDownedPlayers) return;
        a(player, false, source);
    }

    private static void a(ServerPlayer serverPlayer, boolean bl) {
        a(serverPlayer, bl, null);
    }

    private static void a(ServerPlayer serverPlayer, boolean bl, DamageSource finishingSource) {
        if (!KnockedManager.a(serverPlayer.getUUID())) {
            return;
        }
        UUID uUID = serverPlayer.getUUID();
        d.remove(uUID);
        persistState();
        e.add(uUID);
        f.add(uUID);
        KnockedManager.i(serverPlayer);
        serverPlayer.setGlowingTag(false);
        if (finishingSource != null) playHurtSound(serverPlayer);
        MutableComponent mutableComponent = Component.literal(formatPlayerText(bl ? NERConfig.get().bleedOutChatMessage : NERConfig.get().timedOutChatMessage, serverPlayer));
        if (finishingSource == null) h.put(uUID, (Component)mutableComponent);
        try {
            for (InteractionHand interactionHand : InteractionHand.values()) {
                ItemStack itemStack = serverPlayer.getItemInHand(interactionHand);
                if (!itemStack.is(Items.TOTEM_OF_UNDYING)) continue;
                serverPlayer.setItemInHand(interactionHand, ItemStack.EMPTY);
                if (serverPlayer.getInventory().add(itemStack)) continue;
                serverPlayer.drop(itemStack, false);
            }
            serverPlayer.setInvulnerable(false);
            ((LivingEntityAccessor)serverPlayer).a(0);
            DamageSource damageSource = finishingSource != null ? finishingSource : KnockedManager.a(serverPlayer, bl ? a : b);
            // die() reads CombatTracker, not its DamageSource argument, for vanilla chat.
            // Record the actual finishing hit before health reaches zero.
            if (finishingSource != null) serverPlayer.getCombatTracker().recordDamage(finishingSource, serverPlayer.getHealth());
            serverPlayer.setHealth(0.0f);
            serverPlayer.die(damageSource);
            if (serverPlayer.isAlive()) {
                serverPlayer.kill();
            }
        }
        finally {
            e.remove(uUID);
            f.remove(uUID);
            h.remove(uUID);
            serverPlayer.setInvulnerable(false);
            KnockedManager.i(serverPlayer);
            KnockedManager.j(serverPlayer);
            serverPlayer.setGlowingTag(false);
            g.put(uUID, 40);
        }
        KnockedManager.a(serverPlayer.getServer(), uUID);
    }

    public static void a(MinecraftServer minecraftServer) {
        try {
            ServerPlayer serverPlayer;
            UUID uUID2;
            ++m;
            NERConfig nERConfig = NERConfig.get();
            int n2 = nERConfig.reviveDurationSeconds * 20;
            g.replaceAll((uUID, n) -> n - 1);
            g.values().removeIf(n -> n <= 0);
            k.replaceAll((uUID, n) -> n - 1);
            k.values().removeIf(n -> n <= 0);
            ArrayList<UUID> arrayList = new ArrayList<UUID>();
            ArrayList<UUID> arrayList2 = new ArrayList<UUID>();
            for (Map.Entry<UUID, KnockedPlayerData> object : d.entrySet()) {
                uUID2 = object.getKey();
                KnockedPlayerData knockedPlayerData = object.getValue();
                serverPlayer = minecraftServer.getPlayerList().getPlayer(uUID2);
                if (serverPlayer == null) {
                    continue; // Persist offline players; their remaining timer is paused.
                }
                try {
                    Vec3 vec3;
                    knockedPlayerData.effectElapsedTicks = Math.min(86400 * 20, knockedPlayerData.effectElapsedTicks + 1);
                    if (savedState != null) savedState.setDirty();
                    if (serverPlayer.getPose() != Pose.SWIMMING) {
                        serverPlayer.setPose(Pose.SWIMMING);
                    }
                    if (serverPlayer.getHealth() < 1.0f) {
                        serverPlayer.setHealth(1.0f);
                    }
                    if (knockedPlayerData.b > 0) {
                        --knockedPlayerData.b;
                    }
                    if (knockedPlayerData.c > 0) {
                        --knockedPlayerData.c;
                    }
                    serverPlayer.setGlowingTag(true);
                    if (nERConfig.disableInteractionsWhileDowned) {
                        serverPlayer.stopUsingItem();
                        if (serverPlayer.containerMenu != serverPlayer.inventoryMenu) serverPlayer.closeContainer();
                    }
                    if (serverPlayer.isInWater() || serverPlayer.isInLava()) {
                        vec3 = serverPlayer.getDeltaMovement();
                        if (vec3.y < 0.1) {
                            serverPlayer.setDeltaMovement(vec3.x, Math.min(vec3.y + 0.08, 0.15), vec3.z);
                            serverPlayer.hurtMarked = true;
                        }
                    }
                    if (serverPlayer.onGround() && serverPlayer.getY() > (double)serverPlayer.serverLevel().getMinBuildHeight()) {
                        knockedPlayerData.j = serverPlayer.getX();
                        knockedPlayerData.k = serverPlayer.getY();
                        knockedPlayerData.l = serverPlayer.getZ();
                    }
                    if (nERConfig.enableVoidRescue && serverPlayer.getY() < (double)(serverPlayer.serverLevel().getMinBuildHeight() - nERConfig.voidRescueDepthTrigger) && !k.containsKey(uUID2)) {
                        k.put(uUID2, nERConfig.voidRescueCooldownTicks);
                        KnockedManager.b(minecraftServer, serverPlayer);
                    }
                    knockedPlayerData.revivers.entrySet().removeIf(entry -> {
                        ServerPlayer helper = minecraftServer.getPlayerList().getPlayer(entry.getKey());
                        if (helper == null || helper.serverLevel() != minecraftServer.getPlayerList().getPlayer(knockedPlayerData.a).serverLevel()
                            || !helper.isAlive() || helper.isSpectator() || a(helper.getUUID())
                            || minecraftServer.getTickCount() - entry.getValue() > 20) return true;
                        ServerPlayer target = minecraftServer.getPlayerList().getPlayer(knockedPlayerData.a);
                        if (target == null || helper.distanceToSqr(target) > nERConfig.reviverMaxDistance * nERConfig.reviverMaxDistance || !helper.hasLineOfSight(target)) return true;
                        String cost = nERConfig.reviveCostItem;
                        return !cost.isEmpty() && !a(helper, BuiltInRegistries.ITEM.get(ResourceLocation.parse(cost)));
                    });
                    knockedPlayerData.d = knockedPlayerData.revivers.keySet().stream().findFirst().orElse(null);
                    if (knockedPlayerData.d == null) { knockedPlayerData.e = 0; knockedPlayerData.p = false; }
                    else {
                        knockedPlayerData.e += nERConfig.cooperativeRevival ? Math.min(nERConfig.maxCooperativeRevivers, knockedPlayerData.revivers.size()) : 1;
                        if (knockedPlayerData.e >= n2) { arrayList.add(uUID2); continue; }
                    }
                    if (knockedPlayerData.b > 0) continue;
                    arrayList2.add(uUID2);
                }
                catch (Exception exception) {
                    NotEnoughRevives.a.error("NER: tick error for knocked player {}", (Object)uUID2, (Object)exception);
                }
            }
            for (UUID uUID3 : arrayList) {
                ServerPlayer revived = minecraftServer.getPlayerList().getPlayer(uUID3);
                if (revived != null) {
                    KnockedManager.b(revived);
                    continue;
                }
                d.remove(uUID3);
            }
            for (UUID uUID4 : arrayList2) {
                ServerPlayer expired = minecraftServer.getPlayerList().getPlayer(uUID4);
                if (expired != null) {
                    KnockedManager.d(expired);
                    continue;
                }
                d.remove(uUID4);
            }
            if (m % 5 == 0) {
                for (ServerPlayer online : minecraftServer.getPlayerList().getPlayers()) {
                    if (d.containsKey(online.getUUID())) KnockedManager.a(minecraftServer, online.getUUID());
                }
            }
            if (m % 100 == 50) {
                for (ServerPlayer serverPlayer2 : minecraftServer.getPlayerList().getPlayers()) {
                    uUID2 = serverPlayer2.getUUID();
                    if (KnockedManager.a(uUID2) || KnockedManager.b(uUID2)) continue;
                    boolean bl2 = false;
                    AttributeInstance speed = serverPlayer2.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speed != null && speed.getModifier(c) != null) {
                        bl2 = true;
                    }
                    boolean bl = serverPlayer2.isInvulnerable();
                    if (!bl2) continue;
                    KnockedManager.i(serverPlayer2);
                    KnockedManager.j(serverPlayer2);
                    if (bl) {
                        serverPlayer2.setInvulnerable(false);
                    }
                    serverPlayer2.setGlowingTag(false);
                    KnockedManager.a(minecraftServer, uUID2);
                }
            }
        }
        catch (Exception exception) {
            NotEnoughRevives.a.error("NER: error in server tick", (Throwable)exception);
        }
    }

    public static void callForHelp(ServerPlayer serverPlayer) {
        MinecraftServer minecraftServer = serverPlayer.getServer();
        KnockedPlayerData data = d(serverPlayer.getUUID());
        if (data == null || !serverPlayer.isAlive() || data.c > 0) return;
        data.c = NERConfig.get().helpCallCooldownSeconds * 20;
        if (!NERConfig.get().helpChatMessage.isEmpty()) minecraftServer.getPlayerList().broadcastSystemMessage(Component.literal(formatPlayerText(NERConfig.get().helpChatMessage, serverPlayer)), false);
        a(minecraftServer, serverPlayer.getUUID());
    }

    private static Component formatDownedMessage(String template, ServerPlayer player, Component cause, DamageSource source) {
        MutableComponent result = Component.empty();
        var matcher = java.util.regex.Pattern.compile("\\{(player|cause|deathMessage|x|y|z)\\}").matcher(template);
        int previous = 0;
        while (matcher.find()) {
            result.append(template.substring(previous, matcher.start()));
            result.append(switch (matcher.group(1)) {
                case "player" -> player.getDisplayName();
                case "cause" -> cause;
                case "deathMessage" -> source != null ? source.getLocalizedDeathMessage(player) : player.getDisplayName();
                default -> Component.literal(formatPlayerText(matcher.group(), player));
            });
            previous = matcher.end();
        }
        return result.append(template.substring(previous));
    }

    public static String formatPlayerText(String template, ServerPlayer player) {
        return template.replace("{player}", player.getDisplayName().getString())
            .replace("{x}", Integer.toString(player.blockPosition().getX()))
            .replace("{y}", Integer.toString(player.blockPosition().getY()))
            .replace("{z}", Integer.toString(player.blockPosition().getZ()));
    }

    public static void playHurtSound(ServerPlayer player) {
        player.serverLevel().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.PLAYER_HURT, SoundSource.PLAYERS, 1f, 1f);
    }

    public static void a(MinecraftServer minecraftServer, ServerPlayer serverPlayer, UUID uUID) {
        KnockedPlayerData knockedPlayerData = KnockedManager.d(uUID);
        if (knockedPlayerData == null) {
            return;
        }
        if (knockedPlayerData.a() && !knockedPlayerData.d.equals(serverPlayer.getUUID()) && !NERConfig.get().cooperativeRevival) {
            return;
        }
        if (serverPlayer.getUUID().equals(uUID) || KnockedManager.a(serverPlayer.getUUID())
                || !serverPlayer.isAlive() || serverPlayer.isSpectator()) return;
        ServerPlayer target = minecraftServer.getPlayerList().getPlayer(uUID);
        if (target == null || target.serverLevel() != serverPlayer.serverLevel()
                || target.position().distanceTo(serverPlayer.position()) > NERConfig.get().reviverMaxDistance || !serverPlayer.hasLineOfSight(target)) return;
        String cost = NERConfig.get().reviveCostItem;
        if (cost != null && !cost.isEmpty()) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(cost));
            if (item != Items.AIR && !KnockedManager.a(serverPlayer, item)) return;
        }
        if (!knockedPlayerData.revivers.containsKey(serverPlayer.getUUID()) && knockedPlayerData.revivers.size() >= NERConfig.get().maxCooperativeRevivers) return;
        for (KnockedPlayerData other : d.values()) {
            if (other != knockedPlayerData) other.revivers.remove(serverPlayer.getUUID());
        }
        boolean started = knockedPlayerData.revivers.isEmpty();
        boolean joined = !knockedPlayerData.revivers.containsKey(serverPlayer.getUUID());
        knockedPlayerData.revivers.put(serverPlayer.getUUID(), minecraftServer.getTickCount());
        knockedPlayerData.reviveHeartbeatTick = minecraftServer.getTickCount();
        knockedPlayerData.d = knockedPlayerData.revivers.keySet().iterator().next();
        if (started) knockedPlayerData.e = 0;
        if (joined) a(minecraftServer, uUID);
    }

    public static void a(ServerPlayer serverPlayer, UUID uUID) {
        KnockedPlayerData data = d(uUID);
        if (data == null) return;
        data.revivers.remove(serverPlayer.getUUID());
        data.d = data.revivers.keySet().stream().findFirst().orElse(null);
        if (data.d == null) { data.e = 0; data.p = false; }
        a(serverPlayer.getServer(), uUID);
    }

    public static void f(ServerPlayer serverPlayer) {
        UUID uUID = serverPlayer.getUUID();
        if (d.containsKey(uUID)) serverPlayer.setGlowingTag(false);
        e.remove(uUID);
        f.remove(uUID);
        KnockedPlayerData knockedPlayerData = d.get(uUID);
        if (knockedPlayerData != null) {
            knockedPlayerData.revivers.clear();
            knockedPlayerData.d = null;
            knockedPlayerData.e = 0;
            persistState();
            try {
                serverPlayer.setInvulnerable(false);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        for (KnockedPlayerData knockedPlayerData2 : d.values()) {
            knockedPlayerData2.revivers.remove(uUID);
            knockedPlayerData2.d = knockedPlayerData2.revivers.keySet().stream().findFirst().orElse(null);
            if (knockedPlayerData2.d == null) knockedPlayerData2.e = 0;
        }
    }

    public static void a() {
        d.clear();
        e.clear();
        f.clear();
        g.clear();
        h.clear();
        k.clear();
        savedState = null;
        m = 0;
        n = false;
        o = null;
    }

    public static void g(ServerPlayer serverPlayer) {
        UUID uUID = serverPlayer.getUUID();
        KnockedPlayerData knockedPlayerData = d.get(uUID);
        if (knockedPlayerData == null) {
            return;
        }
        serverPlayer.setInvulnerable(true);
        serverPlayer.setGlowingTag(true);
        if (NERConfig.get().disableInteractionsWhileDowned) {
            serverPlayer.stopUsingItem();
            serverPlayer.closeContainer();
        }
        serverPlayer.setHealth(1.0f);
        KnockedManager.h(serverPlayer);
        serverPlayer.setPose(Pose.SWIMMING);
        serverPlayer.setGlowingTag(true);
        if (knockedPlayerData.a()) {
            knockedPlayerData.revivers.clear();
            knockedPlayerData.d = null;
            knockedPlayerData.e = 0;
            knockedPlayerData.p = false;
        }
        knockedPlayerData.j = serverPlayer.getX();
        knockedPlayerData.k = serverPlayer.getY();
        knockedPlayerData.l = serverPlayer.getZ();
        KnockedManager.a(serverPlayer.getServer(), uUID);
    }

    private static void h(ServerPlayer serverPlayer) {
        AttributeInstance attributeInstance = serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attributeInstance != null && attributeInstance.getModifier(c) == null) {
            attributeInstance.addTransientModifier(new AttributeModifier(c, KnockedManager.b(), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
    }

    private static void i(ServerPlayer serverPlayer) {
        AttributeInstance attributeInstance = serverPlayer.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attributeInstance != null) {
            attributeInstance.removeModifier(c);
        }
    }

    private static void j(ServerPlayer serverPlayer) {
        serverPlayer.setPose(Pose.STANDING);
    }

    private static boolean a(ServerPlayer serverPlayer, Item item) {
        for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); ++i) {
            if (!serverPlayer.getInventory().getItem(i).is(item)) continue;
            return true;
        }
        return false;
    }

    private static void b(ServerPlayer serverPlayer, Item item) {
        for (int i = 0; i < serverPlayer.getInventory().getContainerSize(); ++i) {
            ItemStack itemStack = serverPlayer.getInventory().getItem(i);
            if (!itemStack.is(item)) continue;
            itemStack.shrink(1);
            return;
        }
    }

    private static void c() {
        if (n) {
            return;
        }
        n = true;
        try {
            Class<?> clazz = Class.forName("sfiomn.legendarysurvivaloverhaul.common.attachments.ModAttachments");
            Field field = clazz.getField("BODY_DAMAGE");
            o = (Supplier)field.get(null);
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    private static void a(ServerPlayer serverPlayer, KnockedPlayerData knockedPlayerData) {
        KnockedManager.c();
        if (o == null) {
            return;
        }
        try {
            AttachmentType attachmentType = (AttachmentType)o.get();
            Object object = serverPlayer.getData(attachmentType);
            Method method = object.getClass().getMethod("serializeNBT", HolderLookup.Provider.class);
            knockedPlayerData.s = (CompoundTag)method.invoke(object, serverPlayer.registryAccess());
            Method method2 = object.getClass().getMethod("deserializeNBT", HolderLookup.Provider.class, CompoundTag.class);
            method2.invoke(object, serverPlayer.registryAccess(), new CompoundTag());
            try {
                Method method3 = object.getClass().getMethod("updateBrokenHearts", Player.class);
                method3.invoke(object, serverPlayer);
            }
            catch (NoSuchMethodException noSuchMethodException) {}
        }
        catch (Exception exception) {
            NotEnoughRevives.a.warn("NER: failed to save/reset LSO body damage", (Throwable)exception);
        }
    }

    private static void b(ServerPlayer serverPlayer, KnockedPlayerData knockedPlayerData) {
        if (knockedPlayerData == null || knockedPlayerData.s == null) {
            return;
        }
        KnockedManager.c();
        if (o == null) {
            return;
        }
        try {
            AttachmentType attachmentType = (AttachmentType)o.get();
            Object object = serverPlayer.getData(attachmentType);
            Method method = object.getClass().getMethod("deserializeNBT", HolderLookup.Provider.class, CompoundTag.class);
            method.invoke(object, serverPlayer.registryAccess(), knockedPlayerData.s);
            try {
                Method method2 = object.getClass().getMethod("updateBrokenHearts", Player.class);
                method2.invoke(object, serverPlayer);
            }
            catch (NoSuchMethodException noSuchMethodException) {}
        }
        catch (Exception exception) {
            NotEnoughRevives.a.warn("NER: failed to restore LSO body damage", (Throwable)exception);
        }
    }

    private static DamageSource a(ServerPlayer serverPlayer, ResourceKey<DamageType> resourceKey) {
        try {
            Registry registry = serverPlayer.serverLevel().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE);
            Optional optional = registry.getHolder(resourceKey);
            if (optional.isPresent()) {
                return new DamageSource((Holder)optional.get());
            }
        }
        catch (Exception exception) {
            NotEnoughRevives.a.error("NER dmg", (Throwable)exception);
        }
        return serverPlayer.damageSources().genericKill();
    }

    private static boolean a(BlockState blockState, ServerLevel serverLevel, BlockPos blockPos) {
        if (blockState.isAir()) {
            return false;
        }
        if (!blockState.getFluidState().isEmpty()) {
            return false;
        }
        if (!blockState.isSolidRender((BlockGetter)serverLevel, blockPos)) {
            return false;
        }
        return !blockState.is(Blocks.MAGMA_BLOCK);
    }

    private static boolean a(BlockState blockState) {
        return blockState.isAir() && blockState.getFluidState().isEmpty();
    }

    private static void b(MinecraftServer minecraftServer, ServerPlayer serverPlayer) {
        int n;
        if (!NERConfig.get().enableVoidRescue) {
            return;
        }
        ServerLevel serverLevel = serverPlayer.serverLevel();
        int n2 = (int)Math.floor(serverPlayer.getX());
        int n3 = (int)Math.floor(serverPlayer.getZ());
        serverPlayer.setDeltaMovement(0.0, 0.0, 0.0);
        serverPlayer.hurtMarked = true;
        double d = Double.MAX_VALUE;
        Integer n4 = null;
        Integer n5 = null;
        Integer n6 = null;
        int n7 = NERConfig.get().voidRescueSearchRadius;
        for (int i = 0; i <= n7; ++i) {
            for (n = -i; n <= i; ++n) {
                for (int j = -i; j <= i; ++j) {
                    if (Math.abs(n) != i && Math.abs(j) != i) continue;
                    int n8 = n2 + n;
                    int n9 = n3 + j;
                    for (int k = serverLevel.getMinBuildHeight(); k < serverLevel.getMaxBuildHeight() - 2; ++k) {
                        double d2;
                        double d3;
                        double d4;
                        double d5;
                        BlockPos blockPos = new BlockPos(n8, k, n9);
                        if (!KnockedManager.a(serverLevel.getBlockState(blockPos), serverLevel, blockPos) || !KnockedManager.a(serverLevel.getBlockState(blockPos.above())) || !KnockedManager.a(serverLevel.getBlockState(blockPos.above(2))) || !((d5 = (d4 = (double)n8 + 0.5 - serverPlayer.getX()) * d4 + (d3 = (double)(k + 1) - serverPlayer.getY()) * d3 + (d2 = (double)n9 + 0.5 - serverPlayer.getZ()) * d2) < d)) continue;
                        d = d5;
                        n4 = n8;
                        n5 = k + 1;
                        n6 = n9;
                    }
                }
            }
        }
        if (n5 == null) {
            KnockedPlayerData knockedPlayerData = KnockedManager.d(serverPlayer.getUUID());
            if (knockedPlayerData != null && !Double.isNaN(knockedPlayerData.k)) {
                n4 = (int)Math.floor(knockedPlayerData.j);
                n5 = (int)Math.floor(knockedPlayerData.k);
                n6 = (int)Math.floor(knockedPlayerData.l);
            } else {
                for (n = serverLevel.getMaxBuildHeight() - 3; n >= serverLevel.getMinBuildHeight(); --n) {
                    BlockPos blockPos = new BlockPos(n2, n, n3);
                    if (!KnockedManager.a(serverLevel.getBlockState(blockPos), serverLevel, blockPos) || !KnockedManager.a(serverLevel.getBlockState(blockPos.above())) || !KnockedManager.a(serverLevel.getBlockState(blockPos.above(2)))) continue;
                    n4 = n2;
                    n5 = n + 1;
                    n6 = n3;
                    break;
                }
                if (n5 == null) {
                    n4 = n2;
                    n5 = serverLevel.getSeaLevel() + 1;
                    n6 = n3;
                }
            }
        }
        double d6 = (double)n4.intValue() + 0.5;
        double d7 = (double)n6.intValue() + 0.5;
        serverLevel.sendParticles((ParticleOptions)new DustParticleOptions(new Vector3f(1.0f, 0.843f, 0.0f), 1.5f), serverPlayer.getX(), serverPlayer.getY() + 1.0, serverPlayer.getZ(), 32, 0.3, 0.5, 0.3, 0.1);
        serverPlayer.teleportTo(d6, (double)n5.intValue(), d7);
        serverPlayer.setDeltaMovement(0.0, 0.0, 0.0);
        serverPlayer.hurtMarked = true;
        serverLevel.playSound(null, new BlockPos((int)d6, n5.intValue(), (int)d7), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
        serverLevel.sendParticles((ParticleOptions)new DustParticleOptions(new Vector3f(1.0f, 0.843f, 0.0f), 1.5f), d6, (double)n5.intValue() + 1.0, d7, 64, 0.5, 1.0, 0.5, 0.3);
    }

    private static void a(MinecraftServer minecraftServer, UUID uUID) {
        KnockedPlayerData knockedPlayerData = d.get(uUID);
        NERConfig nERConfig = NERConfig.get();
        String string = knockedPlayerData != null && knockedPlayerData.h != null ? knockedPlayerData.h : nERConfig.glowColour;
        SyncKnockedPayload syncKnockedPayload = knockedPlayerData == null ? new SyncKnockedPayload(uUID, false, 0, false, 0, 0, false, "WHITE", 0, false) : new SyncKnockedPayload(uUID, true, knockedPlayerData.b, knockedPlayerData.a(), knockedPlayerData.e, nERConfig.reviveDurationSeconds * 20, knockedPlayerData.f, string, knockedPlayerData.c, knockedPlayerData.p, knockedPlayerData.totalDownedTicks, java.util.List.copyOf(knockedPlayerData.revivers.keySet()));
        for (ServerPlayer serverPlayer : minecraftServer.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)syncKnockedPayload, (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }

    public static void a(MinecraftServer minecraftServer, CustomPacketPayload customPacketPayload, ServerPlayer serverPlayer, double d) {
        ServerLevel serverLevel = serverPlayer.serverLevel();
        Vec3 vec3 = serverPlayer.position();
        for (ServerPlayer serverPlayer2 : minecraftServer.getPlayerList().getPlayers()) {
            if (serverPlayer2.serverLevel() != serverLevel || !(serverPlayer2.position().distanceToSqr(vec3) <= d * d)) continue;
            PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer2, (CustomPacketPayload)customPacketPayload, (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }

    public static void a(MinecraftServer minecraftServer, CustomPacketPayload customPacketPayload) {
        for (ServerPlayer serverPlayer : minecraftServer.getPlayerList().getPlayers()) {
            PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)customPacketPayload, (CustomPacketPayload[])new CustomPacketPayload[0]);
        }
    }

}

