package com.termi.notenoughrevives.client;

import com.termi.notenoughrevives.client.NERClientState;
import com.termi.notenoughrevives.data.ClientKnockedInfo;
import com.termi.notenoughrevives.network.payload.ShowKnockedGuiPayload;
import com.termi.notenoughrevives.network.payload.SyncConfigPayload;
import com.termi.notenoughrevives.network.payload.SyncKnockedPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class NERClientNetworking {

    public static void a(SyncKnockedPayload syncKnockedPayload, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> NERClientNetworking.a(syncKnockedPayload, Minecraft.getInstance()));
    }

    public static void presentation(com.termi.notenoughrevives.network.payload.PresentationConfigPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var config = new com.google.gson.Gson().fromJson(payload.json(), com.termi.notenoughrevives.config.NERConfig.class);
            if (config != null) { config.validate(); NERPresentation.config = config; }
        });
    }

    public static void a(ShowKnockedGuiPayload showKnockedGuiPayload, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> {
            NERClientState.b(showKnockedGuiPayload.a());
            NERClientState.a(true);
        });
    }

    public static void a(SyncConfigPayload syncConfigPayload, IPayloadContext iPayloadContext) {
        iPayloadContext.enqueueWork(() -> NERClientState.a(syncConfigPayload.a(), syncConfigPayload.b(), syncConfigPayload.c(), syncConfigPayload.d(), syncConfigPayload.e(), syncConfigPayload.f()));
    }

    public static void a() {
        com.termi.notenoughrevives.client.hud.DownedVignette.release();
        NERPresentation.reset();
    }

    private static void a(SyncKnockedPayload syncKnockedPayload, Minecraft minecraft) {
        boolean local = minecraft.player != null && minecraft.player.getUUID().equals(syncKnockedPayload.a());
        if (local && syncKnockedPayload.b() && !NERPresentation.states.containsKey(syncKnockedPayload.a())
                && syncKnockedPayload.c() >= syncKnockedPayload.totalTicks() - 2) {
            NERPresentation.downedImpactStarted = System.nanoTime();
        }
        NERPresentation.states.put(syncKnockedPayload.a(), syncKnockedPayload);
        NERPresentation.received.put(syncKnockedPayload.a(), System.nanoTime());
        if (!syncKnockedPayload.b()) {
            NERPresentation.states.remove(syncKnockedPayload.a());
            NERPresentation.received.remove(syncKnockedPayload.a());
            NERClientState.a(syncKnockedPayload.a());
            if (minecraft.player != null && minecraft.player.getUUID().equals(syncKnockedPayload.a())) {
                NERClientState.a(false);
                NERPresentation.downedImpactStarted = 0;
            }
            return;
        }
        NERClientState.a(new ClientKnockedInfo(syncKnockedPayload.a(), syncKnockedPayload.c(), syncKnockedPayload.d(), syncKnockedPayload.e(), syncKnockedPayload.f(), syncKnockedPayload.g(), syncKnockedPayload.h(), syncKnockedPayload.i(), syncKnockedPayload.j()));
        if (minecraft.player != null && minecraft.player.getUUID().equals(syncKnockedPayload.a())) {
            NERClientState.a(true);
        }
    }
}

