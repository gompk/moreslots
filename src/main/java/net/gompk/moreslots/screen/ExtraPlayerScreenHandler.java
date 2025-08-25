package net.gompk.moreslots.screen;

import net.gompk.moreslots.ExtraInventoryMod;
import net.gompk.moreslots.inventory.ExtraInventory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.slot.Slot;

/**
 * Extends the vanilla player container with +2 rows (*18* slots) placed directly
 * below the vanilla 3 inventory rows. Hotbar remains at the bottom, moved down visually
 * by the client mixin that expands the InventoryScreen background height.
 *
 * IMPORTANT: Because we extend PlayerScreenHandler, we preserve all vanilla
 * crafting, armor, offhand, and recipe interactions. We only append Slots.
 */
public class ExtraPlayerScreenHandler extends PlayerScreenHandler {
    private final Inventory extra;

    // Index math mirrors vanilla: armor(4), crafting(5), offhand(1), main 3 rows(27), hotbar(9), then ours(18)
    // But PlayerScreenHandler registers slots first; we add ours after super(..) finishes.
    public final int extraStartIndex;
    public final int extraEndIndex;

    public ExtraPlayerScreenHandler(PlayerInventory playerInv, boolean onServer, PlayerEntity player, ExtraInventory extraInventory) {
        super(playerInv, onServer, player);
        this.extra = extraInventory;

        // 2 rows x 9 columns appended below the vanilla main inventory rows.
        // Slot positions: we fake Y offsets here; the client mixin will expand the background and
        // shift Widgets accordingly. The logical grid is still 9-wide, contiguous with main inventory.
        int leftX = 8; // same left padding as vanilla inventory screen
        int vanillaMainBottomY = 84; // vanilla base Y used by InventoryScreen for the main rows
        int rowHeight = 18;

        int firstRowY = vanillaMainBottomY + rowHeight * 3; // place below the 3 vanilla rows (visually)
        // BUT: The vanilla screen will move things; what matters is slot index spacing (x/y are just rendering).
        // We'll set our Y as vanillaMainBottomY + (rowHeight * 3) to appear directly above hotbar after the mixin lifts hotbar down.

        int slotIndex = 0;
        int added = 0;
        for (int row = 0; row < ExtraInventoryMod.EXTRA_SLOTS / 9; row++) {
            for (int col = 0; col < 9; col++) {
                int x = leftX + col * 18;
                int y = firstRowY + row * 18;
                this.addSlot(new Slot(extra, slotIndex++, x, y));
                added++;
            }
        }
        this.extraStartIndex = this.slots.size() - added;
        this.extraEndIndex = this.slots.size(); // exclusive
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        // identical to vanilla PlayerScreenHandler, but also validate extra inv
        return super.canUse(player) && this.extra.canPlayerUse(player);
    }

    /**
     * Shift-click behavior: we want it to behave exactly like vanilla — items in our
     * extra rows move into the player inventory/hotbar and vice versa.
     *
     * PlayerScreenHandler already implements intelligent merging across its slots for
     * main inventory/hotbar. We extend that by treating our extra area as an additional
     * source/target region contiguous with main inventory.
     */
    @Override
    public net.minecraft.item.ItemStack quickMove(PlayerEntity player, int index) {
        // Defer to PlayerScreenHandler's rules, but include our indices in the merge windows.
        // We'll copy the vanilla logic pattern while widening the "inventory area" to include [extraStartIndex, extraEndIndex)
        // NOTE: We intentionally keep armor/offhand/crafting behavior unchanged.
        var original = net.minecraft.item.ItemStack.EMPTY;
        var slot = this.slots.get(index);
        if (slot != null && slot.hasStack()) {
            var stack = slot.getStack();
            original = stack.copy();

            // Regions (by vanilla order):
            final int CRAFT_START = 0;                // 0..4 craft + result
            final int CRAFT_END = 5;
            final int ARMOR_START = 5;                // 5..9 armor
            final int ARMOR_END = ARMOR_START + 4;
            final int OFFHAND = ARMOR_END;            // 9 offhand (single)
            final int PLAYER_INV_START = OFFHAND + 1; // 10..36 (27 main)
            final int PLAYER_INV_END = PLAYER_INV_START + 27;
            final int HOTBAR_START = PLAYER_INV_END;  // 37..45 (9 hotbar)
            final int HOTBAR_END = HOTBAR_START + 9;

            // Our extra slots follow immediately after vanilla hotbar in this.slots,
            // but rendered above hotbar by our client mixin.
            final int EXTRA_START = this.extraStartIndex;
            final int EXTRA_END = this.extraEndIndex;

            // If clicked slot is in our extra rows: try to move to (player inv + hotbar)
            if (index >= EXTRA_START && index < EXTRA_END) {
                if (!this.insertItem(stack, PLAYER_INV_START, HOTBAR_END, false)) {
                    return net.minecraft.item.ItemStack.EMPTY;
                }
            }
            // If clicked slot is in main inventory/hotbar: try to move into extra rows first
            else if (index >= PLAYER_INV_START && index < HOTBAR_END) {
                if (!this.insertItem(stack, EXTRA_START, EXTRA_END, false)) {
                    // then apply vanilla behavior among main <-> hotbar
                    if (index < HOTBAR_START) {
                        if (!this.insertItem(stack, HOTBAR_START, HOTBAR_END, false)) {
                            return net.minecraft.item.ItemStack.EMPTY;
                        }
                    } else if (!this.insertItem(stack, PLAYER_INV_START, HOTBAR_START, false)) {
                        return net.minecraft.item.ItemStack.EMPTY;
                    }
                }
            }
            // Otherwise (armor/offhand/crafting) -> try player+hotbar+extra, like vanilla plus extra
            else {
                if (!this.insertItem(stack, PLAYER_INV_START, HOTBAR_END, false)
                        && !this.insertItem(stack, EXTRA_START, EXTRA_END, false)) {
                    return net.minecraft.item.ItemStack.EMPTY;
                }
            }

            if (stack.isEmpty()) slot.setStack(net.minecraft.item.ItemStack.EMPTY);
            else slot.markDirty();

            if (stack.getCount() == original.getCount()) return net.minecraft.item.ItemStack.EMPTY;
            slot.onTakeItem(player, stack);
        }

        return original;
    }
}
