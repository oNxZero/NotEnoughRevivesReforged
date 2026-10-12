package com.termi.notenoughrevives.network.payload;

import java.util.UUID;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SyncKnockedPayload(UUID a, boolean b, int c, boolean d, int e, int f, boolean g, String h, int i, boolean j, int totalTicks, java.util.List<UUID> helpers) implements CustomPacketPayload
{
    public SyncKnockedPayload(UUID a, boolean b, int c, boolean d, int e, int f, boolean g, String h, int i, boolean j) {
        this(a,b,c,d,e,f,g,h,i,j,Math.max(1,c),java.util.List.of());
    }
    public static final CustomPacketPayload.Type<SyncKnockedPayload> TYPE = new CustomPacketPayload.Type(ResourceLocation.fromNamespaceAndPath((String)"not_enough_revives", (String)"sync_knocked"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncKnockedPayload> STREAM_CODEC = StreamCodec.of((registryFriendlyByteBuf, syncKnockedPayload) -> {
        registryFriendlyByteBuf.writeUUID(syncKnockedPayload.a());
        registryFriendlyByteBuf.writeBoolean(syncKnockedPayload.b());
        registryFriendlyByteBuf.writeInt(syncKnockedPayload.c());
        registryFriendlyByteBuf.writeBoolean(syncKnockedPayload.d());
        registryFriendlyByteBuf.writeInt(syncKnockedPayload.e());
        registryFriendlyByteBuf.writeInt(syncKnockedPayload.f());
        registryFriendlyByteBuf.writeBoolean(syncKnockedPayload.g());
        registryFriendlyByteBuf.writeUtf(syncKnockedPayload.h());
        registryFriendlyByteBuf.writeInt(syncKnockedPayload.i());
        registryFriendlyByteBuf.writeBoolean(syncKnockedPayload.j());
        registryFriendlyByteBuf.writeInt(syncKnockedPayload.totalTicks());
        registryFriendlyByteBuf.writeCollection(syncKnockedPayload.helpers(), (buf, id) -> buf.writeUUID(id));
    }, registryFriendlyByteBuf -> new SyncKnockedPayload(registryFriendlyByteBuf.readUUID(), registryFriendlyByteBuf.readBoolean(), registryFriendlyByteBuf.readInt(), registryFriendlyByteBuf.readBoolean(), registryFriendlyByteBuf.readInt(), registryFriendlyByteBuf.readInt(), registryFriendlyByteBuf.readBoolean(), registryFriendlyByteBuf.readUtf(), registryFriendlyByteBuf.readInt(), registryFriendlyByteBuf.readBoolean(), registryFriendlyByteBuf.readInt(), registryFriendlyByteBuf.readCollection(size -> { if (size > 16) throw new io.netty.handler.codec.DecoderException("Too many revival participants"); return new java.util.ArrayList<UUID>(size); }, buf -> buf.readUUID())));

    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

