package com.endersystem.client;

import com.endersystem.ModPackets;
import com.endersystem.ShopData;
import com.endersystem.mixin.client.HandledScreenAccessor;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

/**
 * Right-click an item in your inventory -> small hologram popup with its sell price
 * and Sell buttons. Only items that have a price in ShopData can be sold.
 */
public final class SellMenu {
    private static final int PW = 180;
    private static final int PH = 62;

    private static boolean open = false;
    private static int slotIndex;
    private static ItemStack stack = ItemStack.EMPTY;
    private static int price;
    private static int px, py;

    private SellMenu() {}

    public static void attach(HandledScreen<?> screen) {
        ScreenMouseEvents.allowMouseClick(screen).register((s, click) ->
                onClick((HandledScreen<?>) s, click.x(), click.y(), click.button()));
        ScreenEvents.afterRender(screen).register((s, ctx, mouseX, mouseY, delta) -> render(ctx));
        ScreenEvents.remove(screen).register(s -> open = false);
    }

    private static boolean hit(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    /** Returns false to cancel the normal click. */
    private static boolean onClick(HandledScreen<?> screen, double mx, double my, int button) {
        if (open) {
            boolean inside = hit(mx, my, px, py, PW, PH);
            if (button == 0 && inside) {
                if (hit(mx, my, px + 8, py + 38, 80, 16)) {
                    ClientPlayNetworking.send(new ModPackets.SellPayload(slotIndex, false));
                    open = false;
                } else if (stack.getCount() > 1 && hit(mx, my, px + 92, py + 38, 80, 16)) {
                    ClientPlayNetworking.send(new ModPackets.SellPayload(slotIndex, true));
                    open = false;
                }
                return false;
            }
            open = false;
            if (inside) return false;
        }

        if (button == 1) {
            if (!screen.getScreenHandler().getCursorStack().isEmpty()) return true;
            Slot slot = ((HandledScreenAccessor) screen).endersystem$getFocusedSlot();
            if (slot == null || !(slot.inventory instanceof PlayerInventory) || !slot.hasStack()) return true;
            int idx = slot.getIndex();
            if (idx < 0 || idx > 35) return true;
            ItemStack s = slot.getStack();
            int p = ShopData.sellPrice(s.getItem());
            if (p <= 0) return true;

            open = true;
            slotIndex = idx;
            stack = s.copy();
            price = p;
            px = Math.max(2, Math.min((int) mx, screen.width - PW - 2));
            py = Math.max(2, Math.min((int) my, screen.height - PH - 2));
            return false;
        }
        return true;
    }

    private static void render(DrawContext ctx) {
        if (!open) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        double scale = mc.getWindow().getScaleFactor();
        double mx = mc.mouse.getX() / scale;
        double my = mc.mouse.getY() / scale;

        Holo.panel(ctx, px, py, PW, PH, 1f);
        ctx.drawItem(stack, px + 8, py + 6);
        String name = tr.trimToWidth(stack.getName().getString(), PW - 40);
        Holo.bold(ctx, tr, name, px + 30, py + 6, Holo.BRIGHT, 1f);
        Holo.text(ctx, tr, "Sell price: " + price + " gold each", px + 30, py + 18, Holo.GOLD, 1f);

        drawButton(ctx, tr, px + 8, py + 38, 80, 16, "Sell 1  (" + price + "G)", hit(mx, my, px + 8, py + 38, 80, 16));
        if (stack.getCount() > 1) {
            drawButton(ctx, tr, px + 92, py + 38, 80, 16, "Sell all (" + (price * stack.getCount()) + "G)",
                    hit(mx, my, px + 92, py + 38, 80, 16));
        }
    }

    private static void drawButton(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, boolean hover) {
        ctx.fill(x, y, x + w, y + h, Holo.argb(hover ? 0x1E6FA8 : 0x0B3558, 0.9f));
        Holo.frame(ctx, x, y, w, h, Holo.argb(Holo.CYAN, hover ? 1f : 0.7f));
        Holo.centered(ctx, tr, label, x + w / 2, y + 4, Holo.BRIGHT, 1f, false);
    }
}
