package net.gompk.moreslots.mixin.client;

import net.gompk.moreslots.ExtraInventoryMod;
import net.gompk.moreslots.screen.ExtraPlayerScreenHandler;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.recipebook.RecipeBookWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Extends the vanilla 'E' inventory screen ONLY when the underlying handler is our
 * ExtraPlayerScreenHandler. We increase background height and draw two extra rows
 * that look identical to vanilla slots by reusing the vanilla texture middle slice.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
    @Shadow private boolean mouseDown;
    @Shadow private float mouseX;
    @Shadow private float mouseY;

    // These fields are inherited from HandledScreen
    @Shadow protected int backgroundWidth;
    @Shadow protected int backgroundHeight;
    @Shadow protected int x;
    @Shadow protected int y;

    // These fields are inherited from Screen
    @Shadow public int width;
    @Shadow public int height;

    @Unique private boolean extrainv$isExtended() {
        ScreenHandler h = ((InventoryScreen)(Object)this).getScreenHandler();
        return (h instanceof ExtraPlayerScreenHandler);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void extrainv$extendInit(CallbackInfo ci) {
        if (!extrainv$isExtended()) return;
        // Vanilla background is 166 tall. Two extra rows = +36px.
        this.backgroundHeight += 18 * (ExtraInventoryMod.EXTRA_SLOTS / 9);
        // Recenter x/y using new height
        this.x = (this.width - this.backgroundWidth) / 2;
        this.y = (this.height - this.backgroundHeight) / 2;

        // Recipe book / other widgets read y; most auto-layout off backgroundHeight,
        // but if you have custom widgets you might want to nudge here.
    }

    @Inject(method = "drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V", at = @At("TAIL"))
    private void extrainv$drawExtra(DrawContext ctx, float delta, int mouseX, int mouseY, CallbackInfo ci) {
        if (!extrainv$isExtended()) return;

        // Draw two additional slot rows using the middle slice of the vanilla inventory texture.
        // Vanilla texture: /textures/gui/container/inventory.png
        // Middle grid area starts at V=84 (top of main inventory rows) and is 3*18 tall; we can tile its 18px strip.
        // We'll blit the 176x18 strip (U=0,V=166- (hotbar+margin)), but the simplest is: reuse the row texture from V=84.
        final int left = this.x;
        final int top = this.y;

        // Each row is 176x18 wide/tall
        int rows = ExtraInventoryMod.EXTRA_SLOTS / 9;
        for (int r = 0; r < rows; r++) {
            // Destination y: original (y + 84 + 18*3) + 18*r moves below vanilla main rows and above hotbar.
            int destY = top + 84 + 18 * (3 + r);
            // Source rect: U=0, V=84, W=176, H=18 from the vanilla inventory texture
            // Method signature: drawTexture(RenderPipeline, Identifier, x, y, u, v, width, height, regionWidth, regionHeight, textureWidth, textureHeight)
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, InventoryScreen.BACKGROUND_TEXTURE, left, destY, 0.0F, 84.0F, 176, 18, 176, 18, 256, 256);
        }

        // Finally, move the hotbar strip down so it sits *below* our extra rows.
        // Hotbar source is at V=166 (width 176, height 22). We re-draw it lower by rows*18.
        int hotbarY = top + 162 + (rows * 18);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, InventoryScreen.BACKGROUND_TEXTURE, left, hotbarY, 0.0F, 166.0F, 176, 22, 176, 22, 256, 256);
    }
}