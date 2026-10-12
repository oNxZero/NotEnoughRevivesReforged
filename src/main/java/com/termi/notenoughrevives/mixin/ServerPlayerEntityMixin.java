package com.termi.notenoughrevives.mixin;

import com.termi.notenoughrevives.NotEnoughRevives;
import com.termi.notenoughrevives.manager.KnockedManager;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={LivingEntity.class})
public abstract class ServerPlayerEntityMixin {
    @Inject(method={"hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"}, at={@At(value="HEAD")}, cancellable=true)
    private void nerInterceptDamage(DamageSource damageSource, float f, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
        try {
            LivingEntity livingEntity = (LivingEntity)(Object)this;
            if (!(livingEntity instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer serverPlayer = (ServerPlayer)livingEntity;
            UUID uUID = serverPlayer.getUUID();
            if (KnockedManager.b(uUID)) {
                return;
            }
            if (damageSource.is(DamageTypes.GENERIC_KILL) || damageSource.is(DamageTypes.FELL_OUT_OF_WORLD)) {
                if (KnockedManager.a(uUID)) {
                    callbackInfoReturnable.setReturnValue(false);
                    KnockedManager.e(serverPlayer);
                }
                return;
            }
            if (KnockedManager.a(uUID)) {
                callbackInfoReturnable.setReturnValue(false);
                return;
            }

        }
        catch (Exception exception) {
            NotEnoughRevives.a.error("NER: error in hurt() intercept, falling through to vanilla", (Throwable)exception);
        }
    }

    private static boolean a(ServerPlayer serverPlayer) {
        for (InteractionHand interactionHand : InteractionHand.values()) {
            if (!serverPlayer.getItemInHand(interactionHand).is(Items.TOTEM_OF_UNDYING)) continue;
            return true;
        }
        return false;
    }

    @Inject(method={"die(Lnet/minecraft/world/damagesource/DamageSource;)V"}, at={@At(value="HEAD")})
    private void nerClearOnRealDeath(DamageSource damageSource, CallbackInfo callbackInfo) {
        try {
            LivingEntity livingEntity = (LivingEntity)(Object)this;
            if (!(livingEntity instanceof ServerPlayer)) {
                return;
            }
            ServerPlayer serverPlayer = (ServerPlayer)livingEntity;
            KnockedManager.clearOnDeath(serverPlayer);
        }
        catch (Exception exception) {
            NotEnoughRevives.a.error("NER: error in die() cleanup", (Throwable)exception);
        }
    }
}

