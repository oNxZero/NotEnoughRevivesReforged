package com.termi.notenoughrevives.network.payload;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
public record CallForHelpPayload() implements CustomPacketPayload {
    public static final Type<CallForHelpPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath("not_enough_revives", "call_for_help"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CallForHelpPayload> STREAM_CODEC = StreamCodec.unit(new CallForHelpPayload());
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
