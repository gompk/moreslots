package net.gompk.moreslots.inventory;

import net.gompk.moreslots.ExtraInventoryMod;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryWrapper;

/**
 * Server-authoritative storage for the extra item stacks.
 * Backed by SimpleInventory for convenience.
 */
public class ExtraInventory extends SimpleInventory {
    public ExtraInventory() {
        super(ExtraInventoryMod.EXTRA_SLOTS);
    }

    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        clear();
        if (nbt.contains("Items")) {
            // Use getListOrEmpty() or handle the Optional properly
            NbtList nbtList = nbt.getListOrEmpty("Items"); // This returns NbtList directly

            for (int i = 0; i < nbtList.size(); i++) {
                // Handle the Optional<NbtCompound> properly
                nbtList.getCompound(i).ifPresent(itemNbt -> {
                    if (itemNbt.contains("Slot")) {
                        int slot = itemNbt.getByte("Slot", (byte) 0) & 255;
                        if (slot >= 0 && slot < this.size()) {
                            // Use the codec to deserialize ItemStack from NBT
                            ItemStack.CODEC.parse(registryLookup.getOps(NbtOps.INSTANCE), itemNbt)
                                    .resultOrPartial(error -> {})
                                    .ifPresent(stack -> {
                                        if (!stack.isEmpty()) {
                                            this.setStack(slot, stack);
                                        }
                                    });
                        }
                    }
                });
            }
        }
    }

    public NbtCompound writeNbt(RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound out = new NbtCompound();
        NbtList nbtList = new NbtList();

        for (int i = 0; i < this.size(); i++) {
            ItemStack stack = this.getStack(i);
            if (!stack.isEmpty()) {
                final int slot = i; // Make a final copy for lambda capture
                // Use the codec to serialize ItemStack to NBT
                ItemStack.CODEC.encodeStart(registryLookup.getOps(NbtOps.INSTANCE), stack)
                        .resultOrPartial(error -> {})
                        .filter(nbtElement -> nbtElement instanceof NbtCompound)
                        .map(nbtElement -> (NbtCompound) nbtElement)
                        .ifPresent(stackNbt -> {
                            NbtCompound itemNbt = stackNbt.copy();
                            itemNbt.putByte("Slot", (byte) slot);
                            nbtList.add(itemNbt);
                        });
            }
        }

        out.put("Items", nbtList);
        return out;
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true; // same as vanilla PlayerInventory accessibility
    }
}