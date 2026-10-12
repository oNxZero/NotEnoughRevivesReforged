package com.termi.notenoughrevives.mixin;

import com.termi.notenoughrevives.config.NERConfig;
import com.termi.notenoughrevives.manager.KnockedManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.world.InteractionResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class DownedInteractionMixin {
    @Shadow protected ServerPlayer player;

    private boolean ner$locked() {
        return NERConfig.get().disableInteractionsWhileDowned && KnockedManager.a(player.getUUID());
    }

    @Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
    private void ner$break(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (ner$locked()) cir.setReturnValue(false);
    }

    @Inject(method = {"useItem", "useItemOn"}, at = @At("HEAD"), cancellable = true)
    private void ner$use(CallbackInfoReturnable<InteractionResult> cir) {
        if (ner$locked()) cir.setReturnValue(InteractionResult.FAIL);
    }
}
