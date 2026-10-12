package com.termi.notenoughrevives.mixin;

import com.termi.notenoughrevives.config.NERConfig;
import com.termi.notenoughrevives.manager.KnockedManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class DownedServerInputMixin {
    @Shadow public ServerPlayer player;

    // Includes dropping/swapping items, releasing charged weapons, and container actions.
    // Minecraft calls these handlers again on the server thread after PacketUtils dispatch.
    @Inject(method = {"handlePlayerAction", "handleContainerClick", "handleContainerButtonClick"},
            at = @At("HEAD"), cancellable = true)
    private void ner$lock(CallbackInfo ci) {
        if (!player.getServer().isSameThread()) return;
        if (NERConfig.get().disableInteractionsWhileDowned && KnockedManager.a(player.getUUID())) {
            player.stopUsingItem();
            player.closeContainer();
            player.inventoryMenu.sendAllDataToRemote();
            ci.cancel();
        }
    }
}
