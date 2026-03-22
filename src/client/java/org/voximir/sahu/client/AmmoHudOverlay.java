package org.voximir.sahu.client;

import org.voximir.sahu.FireMode;
import org.voximir.sahu.ModDataComponents;
import static org.voximir.sahu.Sahu.MOD_ID;
import org.voximir.sahu.items.GunItem;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Renders ammo counter, fire mode, and reload state in the bottom-right corner
 * of the HUD when the player is holding a gun.
 */
public class AmmoHudOverlay implements HudElement {

    private static final Identifier ID = Identifier.of(MOD_ID, "ammo_hud");

    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.currentScreen != null) return;

        ItemStack stack = client.player.getMainHandStack();
        if (!(stack.getItem() instanceof GunItem gun)) return;

        TextRenderer textRenderer = client.textRenderer;
        int screenWidth = context.getScaledWindowWidth();
        int screenHeight = context.getScaledWindowHeight();

        boolean reloading = stack.getOrDefault(ModDataComponents.RELOADING, false);

        // ── Ammo / Reloading line (bottom-right) ─────────────────────────
        if (reloading) {
            String text = "RELOADING";
            int textWidth = textRenderer.getWidth(text);
            int x = screenWidth - textWidth - 9;
            int y = screenHeight - 16;
            context.drawTextWithShadow(textRenderer, text, x, y, 0xFF808080);
        } else {
            int ammo = stack.getOrDefault(ModDataComponents.AMMO, gun.getMaxAmmo());
            int maxAmmo = gun.getMaxAmmo();

            int color;
            if (ammo == 0) {
                color = 0xFFCC3333;
            } else if (ammo <= maxAmmo * 0.25f) {
                color = 0xFFFF5555;
            } else if (ammo <= maxAmmo * 0.5f) {
                color = 0xFFFFFFAA;
            } else {
                color = 0xFFFFFFFF;
            }

            String text = ammo + " / " + maxAmmo;
            int textWidth = textRenderer.getWidth(text);
            int x = screenWidth - textWidth - 9;
            int y = screenHeight - 16;
            context.drawTextWithShadow(textRenderer, text, x, y, color);
        }

        // ── Fire mode indicator (above ammo line) ────────────────────────
        FireMode fireMode = stack.getOrDefault(ModDataComponents.FIRE_MODE, FireMode.FULL_AUTO);
        String modeText = (fireMode == FireMode.SINGLE) ? "SINGLE" : "AUTO";
        int modeColor = (fireMode == FireMode.SINGLE) ? 0xFFAAAAFF : 0xFFFFAA55;
        int modeWidth = textRenderer.getWidth(modeText);
        int modeX = screenWidth - modeWidth - 9;
        int modeY = screenHeight - 26;

        context.drawTextWithShadow(textRenderer, modeText, modeX, modeY, modeColor);
    }

    public static void initialize() {
        HudElementRegistry.addLast(ID, new AmmoHudOverlay());
    }
}
