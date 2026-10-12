package dev.ner.tests;

import com.mojang.authlib.GameProfile;
import com.termi.notenoughrevives.config.NERConfig;
import com.termi.notenoughrevives.manager.KnockedManager;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("ner_stability_tests")
@PrefixGameTestTemplate(false)
public final class DownedGameTests {
    @GameTest(template = "empty", timeoutTicks = 160)
    public static void downReviveReconnectAndFinish(GameTestHelper helper) throws Exception {
        MinecraftServer server = helper.getLevel().getServer();
        NERConfig config = NERConfig.get();
        boolean oldFinish = config.allowPlayersToKillDownedPlayers;
        boolean oldLock = config.disableInteractionsWhileDowned;
        boolean oldAttack = config.allowAttackWhenKnocked;
        int oldRevive = config.reviveDurationSeconds;
        boolean oldPvp = server.isPvpAllowed();
        config.allowPlayersToKillDownedPlayers = false;
        config.disableInteractionsWhileDowned = true;
        config.allowAttackWhenKnocked = true;
        config.reviveDurationSeconds = 1;
        server.setPvpAllowed(true);
        ServerPlayer target = player(helper, "NER_Downed", new BlockPos(1, 2, 1));
        ServerPlayer reviver = player(helper, "NER_Reviver", new BlockPos(2, 2, 1));
        ServerPlayer second = player(helper, "NER_Second", new BlockPos(1, 2, 2));
        target.setGameMode(GameType.CREATIVE);
        Runnable cleanup = () -> {
            KnockedManager.clearOnDeath(target); KnockedManager.clearOnDeath(reviver);
            remove(server, target); remove(server, reviver); remove(server, second);
            helper.getLevel().removePlayerImmediately(second, Entity.RemovalReason.DISCARDED);
            helper.getLevel().removePlayerImmediately(target, Entity.RemovalReason.DISCARDED);
            helper.getLevel().removePlayerImmediately(reviver, Entity.RemovalReason.DISCARDED);
            config.allowPlayersToKillDownedPlayers = oldFinish; config.disableInteractionsWhileDowned = oldLock;
            config.allowAttackWhenKnocked = oldAttack; config.reviveDurationSeconds = oldRevive;
            server.setPvpAllowed(oldPvp);
        };
        try {
            target.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1));
            helper.assertTrue(KnockedManager.a(target, target.damageSources().playerAttack(reviver)), "Could not enter downed state");
            Capture initialChat = (Capture)target.connection.getConnection();
            helper.assertTrue(initialChat.messages.stream().anyMatch(message -> message.equals("NER_Downed was downed by NER_Reviver!")), "Missing attacker downed announcement");
            int chatCount = initialChat.messages.size();
            helper.assertTrue(!KnockedManager.a(target), "Already downed player was downed twice");
            helper.assertTrue(initialChat.messages.size() == chatCount, "Duplicate downed announcement");
            initialChat.messages.clear();
            helper.assertTrue(target.isCurrentlyGlowing(), "No immediate downed glow");
            helper.assertTrue(target.isCreative(), "Downing changed creative game mode");
            target.setDeltaMovement(0, 0, 0); target.jumpFromGround();
            helper.assertTrue(target.getDeltaMovement().y == 0, "Downed server jump was allowed");
            target.hurt(target.damageSources().playerAttack(reviver), 2);
            helper.assertTrue(target.isAlive() && KnockedManager.a(target.getUUID()), "Finishing disabled but player died");
            KnockedManager.a(server, target, target.getUUID());
            helper.assertTrue(KnockedManager.d(target.getUUID()).d == null, "Self-revival packet accepted");
            KnockedManager.callForHelp(target);
            Capture connection = (Capture)target.connection.getConnection();
            helper.assertTrue(connection.messages.size() == 1 && connection.messages.getFirst().contains("is downed and needs help! X:"), "No coordinate help message");
            KnockedManager.callForHelp(target);
            helper.assertTrue(connection.messages.size() == 1, "Help cooldown failed");
            BlockPos block = helper.absolutePos(new BlockPos(1, 0, 2));
            helper.getLevel().setBlockAndUpdate(block, Blocks.STONE.defaultBlockState());
            helper.assertTrue(!target.gameMode.destroyBlock(block), "Downed player broke a block");
            target.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BOW));
            helper.assertTrue(target.gameMode.useItem(target, helper.getLevel(), target.getMainHandItem(), InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "Downed item use allowed");
            target.startUsingItem(InteractionHand.MAIN_HAND);
            helper.assertTrue(!target.isUsingItem(), "Downed bow charging allowed");
            float health = reviver.getHealth(); target.attack(reviver);
            helper.assertTrue(reviver.getHealth() == health, "Downed melee attack allowed");
            KnockedManager.a(server, reviver, target.getUUID());
            helper.assertTrue(KnockedManager.d(target.getUUID()).d != null, "Revive start rejected: sight=" + reviver.hasLineOfSight(target)
                    + " distance=" + reviver.position().distanceTo(target.position()) + " reviverAlive=" + reviver.isAlive());
            helper.onEachTick(() -> {
                if (target.isAlive() && KnockedManager.a(target.getUUID()) && config.reviveDurationSeconds == 1) {
                    KnockedManager.a(server, reviver, target.getUUID());
                    KnockedManager.a(server, second, target.getUUID());
                }
            });
            helper.runAfterDelay(6, () -> {
                helper.assertTrue(KnockedManager.d(target.getUUID()).revivers.size() == 2, "Two helpers did not join revival");
                helper.assertTrue(KnockedManager.d(target.getUUID()).e >= 8, "Cooperative revival did not speed up");
            });
            helper.runAfterDelay(15, () -> {
                try {
                    helper.assertTrue(!KnockedManager.a(target.getUUID()) && !target.isCurrentlyGlowing(), "Teammate revival did not clear downed state/glow: " + (KnockedManager.d(target.getUUID()) == null ? "state cleared, glow=" + target.isCurrentlyGlowing() : "remaining=" + KnockedManager.d(target.getUUID()).b + " progress=" + KnockedManager.d(target.getUUID()).e + " reviver=" + KnockedManager.d(target.getUUID()).d + " sight=" + reviver.hasLineOfSight(target) + " sameConfig=" + (NERConfig.get() == config)));
                    helper.assertTrue(target.isCreative(), "Revival changed creative game mode");
                    helper.assertTrue(connection.messages.stream().anyMatch(message -> message.contains("was revived by") && message.contains("NER_Reviver") && message.contains("NER_Second")), "Revival chat omitted helper names");
                    helper.assertTrue(target.getEffect(MobEffects.NIGHT_VISION) != null && target.getEffect(MobEffects.NIGHT_VISION).isInfiniteDuration(), "Infinite effect lost on revival");
                    config.reviveDurationSeconds = 5;
                    helper.assertTrue(KnockedManager.a(target), "Could not down revived player");
                    int remaining = KnockedManager.d(target.getUUID()).b;
                    int elapsed = KnockedManager.d(target.getUUID()).effectElapsedTicks;
                    KnockedManager.f(target);
                    remove(server, target);
                    helper.runAfterDelay(20, () -> {
                        try {
                            helper.assertTrue(KnockedManager.d(target.getUUID()).b == remaining
                                    && KnockedManager.d(target.getUUID()).effectElapsedTicks == elapsed, "Offline downed/effect timers advanced");
                            KnockedManager.saveState(server);
                            KnockedManager.loadState(server);
                            helper.assertTrue(KnockedManager.d(target.getUUID()).b == remaining, "Save/load lost remaining downed time");
                            add(server, target);
                            KnockedManager.a(server, target);
                            helper.assertTrue(target.isCurrentlyGlowing() && KnockedManager.a(target.getUUID()), "Reconnection escaped downing");
                            config.allowPlayersToKillDownedPlayers = true;
                            target.hurt(target.damageSources().playerAttack(reviver), 2);
                            helper.assertTrue(connection.messages.stream().anyMatch(message -> message.contains("NER_Downed was slain by NER_Reviver")), "Finishing death did not credit the current attacker");
                            helper.assertTrue(!target.isAlive() && !KnockedManager.a(target.getUUID()) && !target.isCurrentlyGlowing(), "Allowed finishing failed or left downed state/glow");
                            helper.assertTrue(target.isCreative(), "Finishing changed creative game mode");
                            KnockedManager.saveState(server); KnockedManager.loadState(server);
                            helper.assertTrue(!KnockedManager.a(target.getUUID()), "Finished player became downed after save/load");
                            cleanup.run(); helper.succeed();
                        } catch (Throwable error) { error.printStackTrace(); cleanup.run(); helper.fail(error.getMessage()); }
                    });
                } catch (Throwable error) { error.printStackTrace(); cleanup.run(); helper.fail(error.getMessage()); }
            });
        } catch (Throwable error) { error.printStackTrace(); cleanup.run(); helper.fail(error.getMessage()); }
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void finalDamageRespectsDefenses(GameTestHelper helper) throws Exception {
        ServerPlayer target=player(helper,"NER_Armor",new BlockPos(1,2,1));
        try {
            // Explicit full-netherite defense values: test gear defaults are not populated by this harness.
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR).setBaseValue(20);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS).setBaseValue(12);
            target.hurt(target.damageSources().mobAttack(target),25);
            helper.assertTrue(target.getHealth()>1 && !KnockedManager.a(target.getUUID()),"Armor-reduced hit incorrectly downed player: health="+target.getHealth()+", armor="+target.getArmorValue());
            target.invulnerableTime=0; target.setHealth(20);
            target.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,100,3));
            target.hurt(target.damageSources().magic(),40);
            helper.assertTrue(target.getHealth()>1 && !KnockedManager.a(target.getUUID()),"Resistance-reduced hit incorrectly downed player");
            target.removeAllEffects();
            target.invulnerableTime=0; target.setHealth(20);
            target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_ABSORPTION).setBaseValue(40);
            target.setAbsorptionAmount(40);
            target.hurt(target.damageSources().magic(),25);
            helper.assertTrue(target.getHealth()==20 && !KnockedManager.a(target.getUUID()),"Absorption ignored by lethal check");
            target.invulnerableTime=0; target.setAbsorptionAmount(0);
            target.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(Items.TOTEM_OF_UNDYING));
            target.hurt(target.damageSources().magic(),40);
            helper.assertTrue(!KnockedManager.a(target.getUUID()) && target.isAlive() && target.getOffhandItem().isEmpty(),"Totem failed to take priority");
            target.invulnerableTime=0; target.removeAllEffects(); target.setHealth(20); target.setAbsorptionAmount(0);
            target.hurt(target.damageSources().magic(),40);
            helper.assertTrue(KnockedManager.a(target.getUUID()) && target.getHealth()==1,"Actual lethal hit did not down player");
            helper.succeed();
        } finally {
            KnockedManager.clearOnDeath(target); remove(helper.getLevel().getServer(),target);
            helper.getLevel().removePlayerImmediately(target,Entity.RemovalReason.DISCARDED);
        }
    }

    private static ServerPlayer player(GameTestHelper helper, String name, BlockPos relative) throws Exception {
        MinecraftServer server = helper.getLevel().getServer();
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), new GameProfile(UUID.randomUUID(), name), ClientInformation.createDefault());
        player.connection = new CaptureListener(server, new Capture(), player);
        player.gameMode.changeGameModeForPlayer(GameType.SURVIVAL);
        var spawnField = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime"); spawnField.setAccessible(true); spawnField.setInt(player, 0);
        BlockPos absolute = helper.absolutePos(relative);
        helper.getLevel().setBlockAndUpdate(absolute.below(), Blocks.STONE.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(absolute, Blocks.AIR.defaultBlockState());
        helper.getLevel().setBlockAndUpdate(absolute.above(), Blocks.AIR.defaultBlockState());
        player.moveTo(absolute.getX() + 0.5, absolute.getY(), absolute.getZ() + 0.5);
        add(server, player); helper.getLevel().addNewPlayer(player);
        return player;
    }
    @SuppressWarnings("unchecked") private static Map<UUID, ServerPlayer> playerMap(MinecraftServer server) {
        try {
            var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID"); field.setAccessible(true);
            return (Map<UUID, ServerPlayer>)field.get(server.getPlayerList());
        } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }
    @SuppressWarnings("unchecked") private static List<ServerPlayer> mutablePlayers(MinecraftServer server) {
        try {
            var field = net.minecraft.server.players.PlayerList.class.getDeclaredField("players"); field.setAccessible(true);
            return (List<ServerPlayer>)field.get(server.getPlayerList());
        } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
    }
    private static void add(MinecraftServer server, ServerPlayer player) { mutablePlayers(server).add(player); playerMap(server).put(player.getUUID(), player); }
    private static void remove(MinecraftServer server, ServerPlayer player) { mutablePlayers(server).remove(player); playerMap(server).remove(player.getUUID()); }

    private static final class CaptureListener extends ServerGamePacketListenerImpl {
        private final Capture capture;
        CaptureListener(MinecraftServer server, Capture capture, ServerPlayer player) {
            super(server, capture, player, CommonListenerCookie.createInitial(player.getGameProfile(), false));
            this.capture = capture;
        }
        @Override public void send(Packet<?> packet) { capture.record(packet); }
        @Override public void send(Packet<?> packet, PacketSendListener listener) { capture.record(packet); }
    }

    private static final class Capture extends Connection {
        final List<String> messages = new ArrayList<>();
        Capture() {
            super(PacketFlow.SERVERBOUND);
            try {
                var field = Connection.class.getDeclaredField("channel"); field.setAccessible(true);
                field.set(this, new io.netty.channel.embedded.EmbeddedChannel());
            } catch (ReflectiveOperationException error) { throw new RuntimeException(error); }
        }
        @Override public boolean isConnected() { return true; }
        @Override public SocketAddress getRemoteAddress() { return new InetSocketAddress("127.0.0.1", 0); }
        @Override public void tick() {}
        @Override public void send(Packet<?> packet) { record(packet); }
        @Override public void send(Packet<?> packet, PacketSendListener listener) { record(packet); }
        @Override public void send(Packet<?> packet, PacketSendListener listener, boolean flush) { record(packet); }
        private void record(Packet<?> packet) { if (packet instanceof ClientboundSystemChatPacket chat && !chat.overlay()) messages.add(chat.content().getString()); }
    }
}
