package net.gompk.moreslots.net;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Custom payload for the handshake query between client and server.
 */
public record HasClientQueryPayload(boolean hasClient) implements CustomPayload {

    public static final Id<HasClientQueryPayload> ID = new Id<>(NetworkHandler.HAS_CLIENT_QUERY);

    public static final PacketCodec<PacketByteBuf, HasClientQueryPayload> CODEC =
            PacketCodec.of(HasClientQueryPayload::write, HasClientQueryPayload::new);

    public HasClientQueryPayload(PacketByteBuf buf) {
        this(buf.readBoolean());
    }

    public void write(PacketByteBuf buf) {
        buf.writeBoolean(hasClient);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }
}