package net.gompk.moreslots;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerLoginNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import java.util.HashMap;
import net.gompk.moreslots.net.HasClientQueryPayload;
import java.util.Map;
import java.util.UUID;

import net.gompk.moreslots.net.NetworkHandler;

/**
 * Main server/common entrypoint for the "More Slots" mod.
 *
 * Responsibilities:
 *  - Initialize the mod on the logical server side.
 *  - Register login networking handlers to detect whether clients
 *    have the mod installed (handshake system).
 *  - Store whether each connected client supports the extra slots feature.
 */
public class ExtraInventoryMod implements ModInitializer {

    // Mod ID used in fabric.mod.json
    public static final String MOD_ID = "moreslots";
    public static final int EXTRA_SLOTS = 27; // Adjust this number as needed

    /**
     * Map to track client capability.
     * Key: Player UUID (profile ID)
     * Value: Whether that client has the mod installed (true) or not (false).
     */
    public static final Map<UUID, Boolean> CLIENT_SUPPORT = new HashMap<>();

    @Override
    public void onInitialize() {
        System.out.println("[MoreSlots] Server-side mod initializing...");

        /**
         * Register a login-phase handshake receiver.
         *
         * - This detects if a connecting client has the mod installed.
         * - Fabric automatically handles the network channel association.
         * - The lambda now matches Fabric's six-parameter functional interface:
         *     (server, handler, understood, buf, synchronizer, responseSender)
         */
        ServerLoginNetworking.registerGlobalReceiver(
                NetworkHandler.HAS_CLIENT_QUERY,
                (MinecraftServer server,
                 ServerLoginNetworkHandler handler,
                 boolean understood,
                 PacketByteBuf buf,
                 ServerLoginNetworking.LoginSynchronizer synchronizer,
                 PacketSender responseSender) -> {

                    // If the client didn't understand our query, disconnect gracefully
                    if (!understood) {
                        handler.disconnect(Text.literal("Client does not support the MoreSlots mod!"));
                        return;
                    }

                    // Read the boolean sent by the client
                    boolean supports = buf.readBoolean();

                    // Track client support using a temporary key until we can get the actual UUID
                    // We'll use the connection's hash as a temporary identifier
                    String connectionKey = handler.toString(); // Use handler reference as key temporarily

                    // For now, we'll store with a placeholder UUID and update later when we have the real UUID
                    UUID tempId = UUID.nameUUIDFromBytes(connectionKey.getBytes());
                    CLIENT_SUPPORT.put(tempId, supports);

                    System.out.println("[MoreSlots] Client connection supports mod: " + supports + " (temp ID: " + tempId + ")");

                    // Reply to the client to complete handshake
                    HasClientQueryPayload replyPayload = new HasClientQueryPayload(true);
                    responseSender.sendPacket(replyPayload, null);

                    // Note: In a real implementation, you might want to move this tracking
                    // to when the player actually joins the game and you have their real UUID
                }
        );
    }
}