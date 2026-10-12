package com.termi.notenoughrevives.mixin;

import com.termi.notenoughrevives.config.NERConfig;
import com.termi.notenoughrevives.manager.KnockedManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract class DownedFinishMixin {
    // ServerPlayer checks invulnerability before LivingEntity.hurt, so intercept here.
    @Inject(method = "hurt", at = @At("HEAD"), cancellable = true)
    private void ner$finish(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        ServerPlayer target = (ServerPlayer)(Object)this;
        if (!KnockedManager.a(target.getUUID()) || KnockedManager.b(target.getUUID())) return;
        Entity causing = source.getEntity();
        if (!(causing instanceof Player)) {
            Entity direct = source.getDirectEntity();
            causing = direct instanceof Projectile projectile ? projectile.getOwner() : direct;
        }
        if (!(causing instanceof Player attacker)) return;
        cir.setReturnValue(false);
        if (amount <= 0 || !NERConfig.get().allowPlayersToKillDownedPlayers
                || !target.canHarmPlayer(attacker)
                || (NERConfig.get().disableInteractionsWhileDowned && KnockedManager.a(attacker.getUUID()))) return;
        KnockedManager.finishByPlayer(target, source);
        cir.setReturnValue(true);
    }

    @Inject(method = "die", at = @At("TAIL"))
    private void ner$deathCleanup(DamageSource source, CallbackInfo ci) {
        ServerPlayer player = (ServerPlayer)(Object)this;
        if (KnockedManager.a(player.getUUID())) KnockedManager.clearOnDeath(player);
    }
}
