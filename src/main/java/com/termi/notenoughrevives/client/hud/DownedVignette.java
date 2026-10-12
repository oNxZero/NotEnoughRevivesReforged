package com.termi.notenoughrevives.client.hud;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.termi.notenoughrevives.client.NERPresentation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

/** One cached radial texture; aspect-aware sampling keeps the clear area circular. */
public final class DownedVignette {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("not_enough_revives", "dynamic/downed_vignette");
    private static DynamicTexture texture;
    private static int screenWidth, screenHeight;
    private static int lastProgress = -1, lastTint = -1;

    public static void render(GuiGraphics graphics, float remaining, int total) {
        var config = NERPresentation.config;
        if (!config.enableDownedVignette) return;
        float elapsed = 1f - Math.clamp(remaining / Math.max(1, total), 0f, 1f);
        float progress = Math.clamp(elapsed * config.downedVignetteStrength, 0f, 1f);
        int step = Math.round(progress * 1024);
        int tint = Math.round(100 * config.downedBloodTint * (1 - elapsed));
        int width = graphics.guiWidth(), height = graphics.guiHeight();
        if (texture == null || width != screenWidth || height != screenHeight) {
            release();
            screenWidth = width; screenHeight = height;
            int imageHeight = 256;
            int imageWidth = Math.clamp(Math.round(imageHeight * (float)width / Math.max(1, height)), 1, 2048);
            texture = new DynamicTexture(new NativeImage(imageWidth, imageHeight, false));
            Minecraft.getInstance().getTextureManager().register(TEXTURE, texture);
            texture.setFilter(true, false);
        }
        NativeImage pixels = texture.getPixels();
        if (pixels == null) return;
        if (lastProgress != step || lastTint != tint) {
            float p = step / 1024f;
            float clearRadius = (1 - p) * 1.25f;
            float blackout = smooth((p - 0.65f) / 0.35f);
            float scale = Math.min(pixels.getWidth(), pixels.getHeight()) * 0.5f;
            for (int y = 0; y < pixels.getHeight(); y++) {
                float dy = (y + 0.5f - pixels.getHeight() * 0.5f) / scale;
                for (int x = 0; x < pixels.getWidth(); x++) {
                    float dx = (x + 0.5f - pixels.getWidth() * 0.5f) / scale;
                    float radius = (float)Math.sqrt(dx * dx + dy * dy);
                    float border = smooth((radius - clearRadius) / 0.8f) * p;
                    int alpha = Math.round(255 * (blackout + (1 - blackout) * border));
                    pixels.setPixelRGBA(x, y, alpha << 24 | tint);
                }
            }
            texture.upload();
            lastProgress = step; lastTint = tint;
        }
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        try {
            graphics.blit(TEXTURE, 0, 0, width, height, 0f, 0f,
                    pixels.getWidth(), pixels.getHeight(), pixels.getWidth(), pixels.getHeight());
            graphics.flush();
        } finally {
            RenderSystem.disableBlend();
        }
    }

    private static float smooth(float value) {
        float t = Math.clamp(value, 0f, 1f);
        return t * t * (3 - 2 * t);
    }

    public static void release() {
        if (texture != null) {
            Minecraft.getInstance().getTextureManager().release(TEXTURE);
            texture = null;
        }
        lastProgress = -1; lastTint = -1;
    }
}
