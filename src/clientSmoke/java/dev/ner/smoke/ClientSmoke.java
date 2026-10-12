package dev.ner.smoke;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.TitleScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.slf4j.LoggerFactory;

@Mod("ner_client_smoke")
@EventBusSubscriber(modid = "ner_client_smoke", value = Dist.CLIENT)
public final class ClientSmoke {
    private static int renderedFrames;
    @SubscribeEvent public static void render(net.neoforged.neoforge.client.event.ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof TitleScreen) {
            com.termi.notenoughrevives.client.hud.DownedVignette.render(event.getGuiGraphics(), Math.max(0, 100 - guiTicks * 3), 100);
            renderedFrames++;
        }
    }
    private static int ticks;
    private static int guiTicks;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        Minecraft game = Minecraft.getInstance();
        ticks++;
        if (game.screen instanceof TitleScreen && ++guiTicks > 40 && renderedFrames > 0) {
            LoggerFactory.getLogger("ner_client_smoke").info("CLIENT_SMOKE_SUCCESS: client setup and circular vignette texture rendering passed");
            com.termi.notenoughrevives.client.hud.DownedVignette.release();
            game.stop();
        } else if (ticks > 300) {
            LoggerFactory.getLogger("ner_client_smoke").error("CLIENT_SMOKE_FAILED: expected title screen did not initialize");
            com.termi.notenoughrevives.client.hud.DownedVignette.release();
            game.stop();
        }
    }
}
