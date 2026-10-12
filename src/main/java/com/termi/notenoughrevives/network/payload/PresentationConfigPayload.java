package com.termi.notenoughrevives.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record PresentationConfigPayload(String json) implements CustomPacketPayload {
    public static final Type<PresentationConfigPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("not_enough_revives", "presentation_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PresentationConfigPayload> STREAM_CODEC = StreamCodec.of(
        (buf, value) -> buf.writeUtf(value.json, 32767), buf -> new PresentationConfigPayload(buf.readUtf(32767)));
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
