package net.gompk.moreslots.net;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

/**
 * Utility class for network identifiers and helper methods.
 * Handles the handshake between client and server for mod detection.
 */
public class NetworkHandler {

    /**
     * Identifier for the handshake query channel.
     * Used for detecting whether the client has the mod installed.
     */
    public static final Identifier HAS_CLIENT_QUERY = Identifier.of("moreslots", "has_client_query");

    /**
     * Helper method to create a PacketByteBuf containing a single boolean value.
     *
     * @param value Boolean to write to the buffer
     * @return PacketByteBuf containing the boolean
     */
    public static PacketByteBuf boolBuf(boolean value) {
        PacketByteBuf buf = new PacketByteBuf(Unpooled.buffer());
        buf.writeBoolean(value);
        return buf;
    }
}
