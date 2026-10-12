package com.termi.notenoughrevives.mixin;
import com.termi.notenoughrevives.client.NERClientState;
import com.termi.notenoughrevives.client.NERPresentation;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Minecraft.class)
public abstract class DownedClientInputMixin {
    @Inject(method="continueAttack", at=@At("HEAD"), cancellable=true)
    private void ner$mine(boolean held, CallbackInfo ci) {
        if (NERClientState.a() && !NERClientState.c()) {
            Minecraft mc = (Minecraft)(Object)this;
            if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();
            ci.cancel();
        }
    }
    @Inject(method="startAttack", at=@At("HEAD"), cancellable=true)
    private void ner$attack(CallbackInfoReturnable<Boolean> ci) {
        if (NERClientState.a() && !NERClientState.d()) ci.setReturnValue(false);
    }
    @Inject(method="startUseItem", at=@At("HEAD"), cancellable=true)
    private void ner$use(CallbackInfo ci) {
        if (NERClientState.a() && (NERPresentation.config.disableInteractionsWhileDowned || !NERPresentation.config.allowItemUseWhenKnocked)) ci.cancel();
    }
}
