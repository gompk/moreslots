package net.gompk.moreslots.mixin;

import net.gompk.moreslots.inventory.ExtraInventory;
import net.gompk.moreslots.screen.ExtraPlayerScreenHandler;
import net.gompk.moreslots.util.PlayerExtraInventoryBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts the creation of the player's screen handler to use our extended version
 * when the client supports the extra inventory mod.
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {

    /**
     * Replace the default PlayerScreenHandler with our ExtraPlayerScreenHandler
     * when the player opens their inventory (E key).
     */
    @Inject(method = "createScreenHandler", at = @At("HEAD"), cancellable = true)
    private void createExtraScreenHandler(CallbackInfoReturnable<ScreenHandler> cir) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;

        // TODO: Check if client supports mod (you'll need to implement this check)
        // For now, always use the extra handler
        boolean clientSupportsExtra = true; // Replace with actual check

        if (clientSupportsExtra) {
            // Get the player's extra inventory
            ExtraInventory extraInv = ((PlayerExtraInventoryBridge) player).extrainv$getExtraInventory();

            // Create our extended screen handler instead of vanilla
            ExtraPlayerScreenHandler handler = new ExtraPlayerScreenHandler(
                    player.getInventory(),
                    false, // onServer = false for screen handler creation
                    player,
                    extraInv
            );

            cir.setReturnValue(handler);
        }
    }
}