package com.termi.notenoughrevives.client.hud;

import com.termi.notenoughrevives.NotEnoughRevivesClient;
import com.termi.notenoughrevives.client.NERClientState;
import com.termi.notenoughrevives.client.NERPresentation;
import com.termi.notenoughrevives.client.NERReviveHandler;
import com.termi.notenoughrevives.data.ClientKnockedInfo;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

public final class NERHudRenderer {
    public static void onRenderHud(RenderGuiEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen instanceof net.minecraft.client.gui.screens.DeathScreen) return;
        GuiGraphics g = event.getGuiGraphics();
        var config = NERPresentation.config;
        ClientKnockedInfo own = NERClientState.c(mc.player.getUUID());
        int center = g.guiWidth() / 2;
        int top = Math.max(24, g.guiHeight() / 2 - 65);
        if (own != null) {
            var snapshot = NERPresentation.states.get(own.a);
            float ticksSinceSync = Math.min(20f, (System.nanoTime() - NERPresentation.received.getOrDefault(own.a, System.nanoTime())) / 50_000_000f);
            DownedVignette.render(g, Math.max(0, own.b - ticksSinceSync), snapshot == null ? config.downedDurationSeconds * 20 : snapshot.totalTicks());
            impact(g);
            line(g, config.downedTitle, center, top, 0xFF7777);
            if (own.c) {
                int seconds = Math.max(0, (own.e - own.d + 20 * helpers(own.a) - 1) / (20 * helpers(own.a)));
                line(g, text(config.revivalProgressText, seconds, ""), center, top + 18, 0x88FFAA);
                bar(g, own, center, top + 34);
            } else line(g, text(config.bleedTimerText, Math.max(0, (own.b + 19) / 20), ""), center, top + 18, 0xFFAAAA);
            int bottom = g.guiHeight() - 100;
            String key = mc.options.keyUse.getTranslatedKeyMessage().getString();
            line(g, text(own.h > 0 ? config.helpCooldownText : config.helpPromptText, (own.h + 19) / 20, key), center, bottom, own.h > 0 ? 0xAAAAAA : 0xFFFFAA);
            if (NERClientState.b()) line(g, text(config.bleedOutPromptText, 0, NotEnoughRevivesClient.b.getTranslatedKeyMessage().getString()), center, bottom + 14, 0xFF7777);
        } else {
            var target = NERReviveHandler.a();
            ClientKnockedInfo info = target == null ? null : NERClientState.c(target);
            if (info == null || !info.c || !isHelper(info.a, mc.player.getUUID())) return; // No misleading first-frame or out-of-range hold prompt.
            line(g, text(config.reviverProgressText, Math.max(0, (info.e - info.d + 20 * helpers(info.a) - 1) / (20 * helpers(info.a))), ""), center, top, 0x88FFAA);
            bar(g, info, center, top + 16);
            line(g, config.reviveCancelText, center, top + 30, 0xAAAAAA);
        }
    }
    private static void impact(GuiGraphics g) {
        var config = NERPresentation.config;
        if (!config.enableDownedImpactEffect || NERPresentation.downedImpactStarted == 0) return;
        float elapsed = (System.nanoTime() - NERPresentation.downedImpactStarted) / 1_000_000_000f;
        float progress = Math.clamp(elapsed / config.downedImpactDurationSeconds, 0f, 1f);
        float fade = (1 - progress) * (1 - progress);
        int alpha = (int)(255 * config.downedImpactStrength * fade);
        if (alpha > 0) g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha << 24 | 0x990D0D);
    }

    private static int helpers(java.util.UUID target) {
        var state = NERPresentation.states.get(target);
        return !NERPresentation.config.cooperativeRevival || state == null ? 1 : Math.max(1, state.helpers().size());
    }
    private static boolean isHelper(java.util.UUID target, java.util.UUID player) {
        var state = NERPresentation.states.get(target);
        return state != null && state.helpers().contains(player);
    }
    private static String text(String value, int seconds, String key) { return value.replace("{seconds}", Integer.toString(seconds)).replace("{key}", key); }
    private static void line(GuiGraphics g, String text, int x, int y, int color) {
        if (!text.isEmpty()) g.drawCenteredString(Minecraft.getInstance().font, text, x, y, color);
    }
    private static void bar(GuiGraphics g, ClientKnockedInfo info, int center, int y) {
        g.fill(center - 61, y - 1, center + 61, y + 6, 0xCC000000);
        g.fill(center - 60, y, center - 60 + (int)(120 * Math.clamp(info.a(), 0f, 1f)), y + 5, 0xFF55CC88);
    }
}
