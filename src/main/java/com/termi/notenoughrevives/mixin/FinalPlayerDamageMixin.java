package com.termi.notenoughrevives.mixin;

import com.termi.notenoughrevives.NotEnoughRevives;
import com.termi.notenoughrevives.manager.KnockedManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Decide downing at the final health write, after defenses and absorption. */
@Mixin(Player.class)
public abstract class FinalPlayerDamageMixin {
    @Redirect(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;setHealth(F)V"))
    private void nerWriteFinalHealth(Player receiver, float nextHealth, DamageSource source, float incomingDamage) {
        receiver.setHealth(nerFinalHealth(nextHealth, source));
    }

    private float nerFinalHealth(float nextHealth, DamageSource source) {
        if (!((Object)this instanceof ServerPlayer player) || nextHealth > 0 || player.getHealth() <= 0
                || source.is(DamageTypes.GENERIC_KILL) || source.is(DamageTypes.FELL_OUT_OF_WORLD)) return nextHealth;
        var id = player.getUUID();
        if (KnockedManager.a(id) || KnockedManager.b(id) || KnockedManager.c(id)) return nextHealth;
        // Let vanilla try a held totem before entering the downed state.
        for (InteractionHand hand : InteractionHand.values())
            if (player.getItemInHand(hand).is(Items.TOTEM_OF_UNDYING)) return nextHealth;
        try {
            if (KnockedManager.a(player, source)) return player.getHealth();
        } catch (Exception error) {
            NotEnoughRevives.a.error("NER: final damage intercept failed, falling through to vanilla", error);
        }
        return nextHealth;
    }
}
