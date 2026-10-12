package com.termi.notenoughrevives.mixin;
import com.termi.notenoughrevives.config.NERConfig;
import com.termi.notenoughrevives.manager.KnockedManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class DownedJumpMixin {
    @Inject(method="jumpFromGround", at=@At("HEAD"), cancellable=true)
    private void ner$jump(CallbackInfo ci) {
        if ((Object)this instanceof ServerPlayer player && NERConfig.get().disableJumpWhileDowned && KnockedManager.a(player.getUUID())) ci.cancel();
    }
}
