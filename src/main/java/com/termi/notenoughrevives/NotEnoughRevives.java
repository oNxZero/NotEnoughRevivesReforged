package com.termi.notenoughrevives;

import com.termi.notenoughrevives.client.NERClientNetworking;
import com.termi.notenoughrevives.config.NERConfig;
import com.termi.notenoughrevives.gamerule.NERGameRules;
import com.termi.notenoughrevives.item.NERItems;
import com.termi.notenoughrevives.manager.KnockedManager;
import com.termi.notenoughrevives.network.payload.BleedOutPayload;
import com.termi.notenoughrevives.network.payload.CallForHelpPayload;
import com.termi.notenoughrevives.network.payload.ShowKnockedGuiPayload;
import com.termi.notenoughrevives.network.payload.StartRevivePayload;
import com.termi.notenoughrevives.network.payload.StopRevivePayload;
import com.termi.notenoughrevives.network.payload.SyncConfigPayload;
import com.termi.notenoughrevives.network.payload.SyncKnockedPayload;
import java.util.function.Supplier;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(value="not_enough_revives")
public class NotEnoughRevives {
    public static final Logger a = LoggerFactory.getLogger((String)"not_enough_revives");
    // Inert legacy registry entries protect existing saves; custom audio is removed.
    public static final DeferredRegister<SoundEvent> b = DeferredRegister.create((Registry)BuiltInRegistries.SOUND_EVENT, (String)"not_enough_revives");
    public static final ResourceLocation c = ResourceLocation.fromNamespaceAndPath((String)"not_enough_revives", (String)"helpcall");
    public static final ResourceLocation d = ResourceLocation.fromNamespaceAndPath((String)"not_enough_revives", (String)"revival");
    public static final Supplier<SoundEvent> e = b.register("helpcall", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)c));
    public static final Supplier<SoundEvent> f = b.register("revival", () -> SoundEvent.createVariableRangeEvent((ResourceLocation)d));

    public NotEnoughRevives(IEventBus iEventBus) {
        b.register(iEventBus);
        NERItems.ITEMS.register(iEventBus);
        NERGameRules.a();
        NERConfig.load();
        iEventBus.addListener((RegisterPayloadHandlersEvent event) -> this.a(event));
        NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> this.a(event));
        NeoForge.EVENT_BUS.addListener((ServerStoppingEvent event) -> this.a(event));
        NeoForge.EVENT_BUS.addListener((ServerTickEvent.Post event) -> this.a(event));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> this.a(event));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedOutEvent event) -> this.a(event));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> this.a(event));
        NeoForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> this.a(event));
        a.info("Not Enough Revives Reforged initialised.");
    }

    private void a(ServerStartedEvent serverStartedEvent) {
        NERConfig.load();
        if (!NERConfig.get().reviveCostItem.isEmpty()) {
            ResourceLocation cost = ResourceLocation.parse(NERConfig.get().reviveCostItem);
            if (!BuiltInRegistries.ITEM.containsKey(cost)) {
                a.warn("Configured reviveCostItem {} is not registered; clearing invalid cost setting", cost);
                NERConfig.get().reviveCostItem = "";
                NERConfig.save();
            }
        }
        KnockedManager.loadState(serverStartedEvent.getServer());
    }

    private void a(ServerStoppingEvent serverStoppingEvent) {
        for (ServerPlayer player : serverStoppingEvent.getServer().getPlayerList().getPlayers()) {
            if (KnockedManager.a(player.getUUID())) {
                player.setGlowingTag(false);
                player.setInvulnerable(false);
            }
        }
        KnockedManager.saveState(serverStoppingEvent.getServer());
        KnockedManager.a();
    }

    private void a(ServerTickEvent.Post event) { KnockedManager.a(event.getServer()); }

    private void a(PlayerEvent.PlayerRespawnEvent playerRespawnEvent) {
        Player player = playerRespawnEvent.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            if (playerRespawnEvent.isEndConquered()) KnockedManager.g(serverPlayer);
            else KnockedManager.i(serverPlayer.getUUID());
        }
    }

    private void a(PlayerEvent.PlayerLoggedOutEvent playerLoggedOutEvent) {
        Player player = playerLoggedOutEvent.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            KnockedManager.f(serverPlayer);
        }
    }

    private void a(PlayerEvent.PlayerChangedDimensionEvent playerChangedDimensionEvent) {
        Player player = playerChangedDimensionEvent.getEntity();
        if (player instanceof ServerPlayer) {
            ServerPlayer serverPlayer = (ServerPlayer)player;
            KnockedManager.g(serverPlayer);
        }
    }

    private void a(PlayerEvent.PlayerLoggedInEvent playerLoggedInEvent) {
        Player player = playerLoggedInEvent.getEntity();
        if (!(player instanceof ServerPlayer)) {
            return;
        }
        ServerPlayer serverPlayer = (ServerPlayer)player;
        MinecraftServer server = serverPlayer.getServer();
        if (server == null) {
            return;
        }
        server.execute(() -> this.b(serverPlayer, server));
    }

    private void a(RegisterPayloadHandlersEvent registerPayloadHandlersEvent) {
        PayloadRegistrar payloadRegistrar = registerPayloadHandlersEvent.registrar("3");
        payloadRegistrar.playToClient(com.termi.notenoughrevives.network.payload.PresentationConfigPayload.TYPE,
            com.termi.notenoughrevives.network.payload.PresentationConfigPayload.STREAM_CODEC,
            (payload, context) -> NERClientNetworking.presentation(payload, context));
        payloadRegistrar.playToClient(SyncKnockedPayload.TYPE, SyncKnockedPayload.STREAM_CODEC, (syncKnockedPayload, iPayloadContext) -> NERClientNetworking.a(syncKnockedPayload, iPayloadContext));
        payloadRegistrar.playToClient(ShowKnockedGuiPayload.TYPE, ShowKnockedGuiPayload.STREAM_CODEC, (showKnockedGuiPayload, iPayloadContext) -> NERClientNetworking.a(showKnockedGuiPayload, iPayloadContext));
        payloadRegistrar.playToClient(SyncConfigPayload.TYPE, SyncConfigPayload.STREAM_CODEC, (syncConfigPayload, iPayloadContext) -> NERClientNetworking.a(syncConfigPayload, iPayloadContext));
        payloadRegistrar.playToServer(CallForHelpPayload.TYPE, CallForHelpPayload.STREAM_CODEC, this::a);
        payloadRegistrar.playToServer(BleedOutPayload.TYPE, BleedOutPayload.STREAM_CODEC, this::a);
        payloadRegistrar.playToServer(StartRevivePayload.TYPE, StartRevivePayload.STREAM_CODEC, this::a);
        payloadRegistrar.playToServer(StopRevivePayload.TYPE, StopRevivePayload.STREAM_CODEC, this::a);
    }

    private void a(CallForHelpPayload callForHelpPayload, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer)iPayloadContext.player();
            KnockedManager.callForHelp(serverPlayer);
        });
    }

    private void a(BleedOutPayload bleedOutPayload, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer)iPayloadContext.player();
            if (NERConfig.get().allowBleedOut) KnockedManager.c(serverPlayer);
        });
    }

    private void a(StartRevivePayload startRevivePayload, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer)iPayloadContext.player();
            KnockedManager.a(serverPlayer.getServer(), serverPlayer, startRevivePayload.a());
        });
    }

    private void a(StopRevivePayload stopRevivePayload, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            ServerPlayer serverPlayer = (ServerPlayer)iPayloadContext.player();
            KnockedManager.a(serverPlayer, stopRevivePayload.a());
        });
    }

    private void a(ServerPlayer serverPlayer, MinecraftServer minecraftServer) {
        KnockedManager.a(minecraftServer, serverPlayer);
        KnockedManager.syncAllTo(serverPlayer);
        NERConfig nERConfig = NERConfig.get();
        PacketDistributor.sendToPlayer(serverPlayer, new com.termi.notenoughrevives.network.payload.PresentationConfigPayload(new com.google.gson.Gson().toJson(nERConfig)));
        PacketDistributor.sendToPlayer((ServerPlayer)serverPlayer, (CustomPacketPayload)new SyncConfigPayload(!nERConfig.disableInteractionsWhileDowned && nERConfig.allowMiningWhenKnocked, !nERConfig.disableInteractionsWhileDowned && nERConfig.allowAttackWhenKnocked, !nERConfig.disableInteractionsWhileDowned && nERConfig.allowItemUseWhenKnocked, nERConfig.miningSpeedMultiplier, "", nERConfig.reviveCostItem), (CustomPacketPayload[])new CustomPacketPayload[0]);
    }

    private /* synthetic */ void b(ServerPlayer serverPlayer, MinecraftServer minecraftServer) {
        this.a(serverPlayer, minecraftServer);
    }
}

