package net.gompk.moreslots.mixin;

import net.gompk.moreslots.inventory.ExtraInventory;
import net.gompk.moreslots.util.PlayerExtraInventoryBridge;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a per-player ExtraInventory field, handles persistence to NBT, and drops on death
 * when keepInventory is false (mirrors vanilla semantics).
 */
@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements PlayerExtraInventoryBridge {
    @Unique private final ExtraInventory extrainv$extra = new ExtraInventory();
    @Unique private boolean extrainv$dirty = false;

    @Override
    public ExtraInventory extrainv$getExtraInventory() { return extrainv$extra; }

    @Override
    public void extrainv$markExtraDirty() { extrainv$dirty = true; }

    // Serialize into the player's custom data
    @Inject(method = "writeCustomDataToNbt(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)V", at = @At("TAIL"))
    private void extrainv$save(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfo ci) {
        if (extrainv$dirty || !extrainv$extra.isEmpty()) {
            nbt.put("ExtraInventory", extrainv$extra.writeNbt(registryLookup));
            extrainv$dirty = false;
        }
    }

    // Deserialize our inventory back
    @Inject(method = "readCustomDataFromNbt(Lnet/minecraft/nbt/NbtCompound;Lnet/minecraft/registry/RegistryWrapper$WrapperLookup;)V", at = @At("TAIL"))
    private void extrainv$load(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup, CallbackInfo ci) {
        if (nbt.contains("ExtraInventory")) {
            extrainv$extra.readNbt(nbt.getCompoundOrEmpty("ExtraInventory"), registryLookup);
        } else {
            extrainv$extra.clear();
        }
    }

    /**
     * On death: if keepInventory is false, drop our items exactly like vanilla does for the PlayerInventory.
     * This ensures graves/datapacks that collect drops will see these stacks as normal item entities.
     */
    @Inject(method = "dropInventory", at = @At("TAIL"))
    private void extrainv$dropExtra(CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        if (self.getWorld().isClient) return;
        ServerWorld world = (ServerWorld) self.getWorld();
        if (world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        for (int i = 0; i < extrainv$extra.size(); i++) {
            ItemStack stack = extrainv$extra.getStack(i);
            if (!stack.isEmpty()) {
                self.dropItem(stack, true, false);
                extrainv$extra.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    /**
     * On respawn with keepInventory = true, copy our stacks to the new player.
     * Mirrors how vanilla copies its PlayerInventory in keepInventory scenarios.
     */
    @Inject(method = "copyFrom(Lnet/minecraft/entity/player/PlayerEntity;Z)V", at = @At("TAIL"))
    private void extrainv$copyOnRespawn(PlayerEntity oldPlayer, boolean alive, CallbackInfo ci) {
        PlayerEntity self = (PlayerEntity)(Object)this;
        if (self.getWorld().isClient) return;
        ServerWorld world = (ServerWorld) self.getWorld();
        if (!world.getGameRules().getBoolean(GameRules.KEEP_INVENTORY)) return;

        ExtraInventory oldInv = ((PlayerExtraInventoryBridge) oldPlayer).extrainv$getExtraInventory();
        for (int i = 0; i < extrainv$extra.size(); i++) {
            extrainv$extra.setStack(i, oldInv.getStack(i).copy());
        }
    }
}