package net.gompk.moreslots.util;

import net.gompk.moreslots.inventory.ExtraInventory;

/**
 * Interface to access the extra inventory from any PlayerEntity.
 * Implemented via mixin in PlayerEntityMixin.
 */
public interface PlayerExtraInventoryBridge {
    ExtraInventory extrainv$getExtraInventory();
    void extrainv$markExtraDirty();
}