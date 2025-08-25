package net.gompk.moreslots.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientLoginNetworking;
import net.gompk.moreslots.net.NetworkHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.PacketByteBuf;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import io.netty.channel.ChannelFutureListener;

/**
 * Client-side entrypoint for the MoreSlots mod.
 */
public class ExtraInventoryClient implements ClientModInitializer {

    public static boolean SERVER_SUPPORTS_EXTRA = false;

    @Override
    public void onInitializeClient() {
        System.out.println("[MoreSlots] Client initializing...");

        ClientLoginNetworking.registerGlobalReceiver(
                NetworkHandler.HAS_CLIENT_QUERY,
                (MinecraftClient client,
                 ClientLoginNetworkHandler handler,
                 PacketByteBuf buf,
                 Consumer<ChannelFutureListener> callbacks) -> {

                    // Schedule any changes to game state on the main thread
                    client.execute(() -> {
                        SERVER_SUPPORTS_EXTRA = true;
                        System.out.println("[MoreSlots] Server handshake detected; SERVER_SUPPORTS_EXTRA = true");
                    });

                    // Create reply packet: "true" = client has mod
                    PacketByteBuf reply = NetworkHandler.boolBuf(true);

                    // Return CompletableFuture for Fabric to send to server
                    return CompletableFuture.completedFuture(reply);
                }
        );
    }
}
