package com.termi.notenoughrevives;

import com.mojang.blaze3d.platform.InputConstants;
import com.termi.notenoughrevives.client.NERClientNetworking;
import com.termi.notenoughrevives.client.NERClientState;
import com.termi.notenoughrevives.client.NERReviveHandler;
import com.termi.notenoughrevives.client.hud.NERHudRenderer;
import com.termi.notenoughrevives.client.render.NERWorldRenderer;
import com.termi.notenoughrevives.data.ClientKnockedInfo;
import com.termi.notenoughrevives.network.payload.BleedOutPayload;
import com.termi.notenoughrevives.network.payload.CallForHelpPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.network.PacketDistributor;

@EventBusSubscriber(modid="not_enough_revives", bus=EventBusSubscriber.Bus.MOD, value={Dist.CLIENT})
public class NotEnoughRevivesClient {
    public static KeyMapping b;
    private static boolean c;

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent registerKeyMappingsEvent) {
        b = new KeyMapping("key.not_enough_revives.bleed_out", InputConstants.Type.KEYSYM, 66, "key.category.not_enough_revives");
        registerKeyMappingsEvent.register(b);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent fMLClientSetupEvent) {
        NERReviveHandler.b();
        NERWorldRenderer.register();
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.client.event.MovementInputUpdateEvent event) -> {
            if (NERClientState.a() && com.termi.notenoughrevives.client.NERPresentation.config.disableJumpWhileDowned) event.getInput().jumping = false;
        });
        NeoForge.EVENT_BUS.addListener(NERHudRenderer::onRenderHud);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> NotEnoughRevivesClient.a(event));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> NotEnoughRevivesClient.a(event));
    }

    public static void a(ClientTickEvent.Post post) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !NERClientState.a()) {
            c = false;
            return;
        }
        while (b.consumeClick()) {
            if (!NERClientState.b()) continue;
            PacketDistributor.sendToServer((CustomPacketPayload)new BleedOutPayload(), (CustomPacketPayload[])new CustomPacketPayload[0]);
            NERClientState.a(false);
        }
        if (minecraft.screen != null) { c = minecraft.options.keyUse.isDown(); return; }
        boolean pressed = minecraft.options.keyUse.isDown();
        if (pressed && !c) {
            ClientKnockedInfo info = NERClientState.c(minecraft.player.getUUID());
            if (info != null && info.h <= 0) {
                PacketDistributor.sendToServer(new CallForHelpPayload());
            }
        }
        c = pressed;
    }

    public static void a(ClientPlayerNetworkEvent.LoggingOut loggingOut) {
        NERClientState.g();
        NERClientNetworking.a();
    }

    static {
        c = false;
    }
}

