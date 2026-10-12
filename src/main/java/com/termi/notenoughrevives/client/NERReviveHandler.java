package com.termi.notenoughrevives.client;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.phys.EntityHitResult;
import com.termi.notenoughrevives.network.payload.StartRevivePayload;
import com.termi.notenoughrevives.network.payload.StopRevivePayload;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
public final class NERReviveHandler {
    private static UUID target;
    public static UUID a() { return target; }
    public static void b() { NeoForge.EVENT_BUS.addListener(NERReviveHandler::tick); }
    private static void tick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { target = null; return; }
        AbstractClientPlayer candidate = null;
        if (!NERClientState.a() && mc.screen == null && mc.options.keyUse.isDown()) {
            if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof AbstractClientPlayer player) candidate = player;
            if (candidate == null && !(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult)) {
                double best = NERPresentation.config.reviverMaxDistance * NERPresentation.config.reviverMaxDistance;
                for (AbstractClientPlayer player : mc.level.players()) {
                    double distance = mc.player.distanceToSqr(player);
                    if (player != mc.player && NERClientState.b(player.getUUID()) && distance <= best && mc.player.hasLineOfSight(player)) { best = distance; candidate = player; }
                }
            }
            if (candidate != null && (!NERClientState.b(candidate.getUUID()) || !mc.player.hasLineOfSight(candidate)
                || mc.player.distanceToSqr(candidate) > NERPresentation.config.reviverMaxDistance * NERPresentation.config.reviverMaxDistance)) candidate = null;
        }
        UUID next = candidate == null ? null : candidate.getUUID();
        if (target != null && !target.equals(next)) PacketDistributor.sendToServer(new StopRevivePayload(target));
        target = next;
        if (target != null) PacketDistributor.sendToServer(new StartRevivePayload(target));
    }
}
