package com.endersystem.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** The hologram notification (LEVEL UP!, QUEST COMPLETE!...), drawn on the HUD like the anime. */
public class LevelUpOverlay {
    private static final long DURATION_MS = 4500;
    private static long startMs = 0;
    private static String banner = "";
    private static String line1 = "";
    private static String line2 = "";

    public static void init() {
        HudRenderCallback.EVENT.register(LevelUpOverlay::render);
    }

    public static void trigger(String bannerText, String first, String second) {
        startMs = System.currentTimeMillis();
        banner = bannerText;
        line1 = first;
        line2 = second;
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (startMs == 0) return;
        long age = System.currentTimeMillis() - startMs;
        if (age > DURATION_MS) return;

        float fadeIn = Math.min(1f, age / 300f);
        float fadeOut = Math.min(1f, (DURATION_MS - age) / 700f);
        float a = Math.max(0f, Math.min(fadeIn, fadeOut));

        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int cx = sw / 2;
        int cy = sh / 3;

        int bw = Math.max(180, tr.getWidth(banner) + 40);
        Holo.panel(ctx, cx - bw / 2, cy, bw, 34, a);
        Holo.centered(ctx, tr, banner, cx, cy + 13, Holo.BRIGHT, a, true);

        int nw = Math.max(150, tr.getWidth(line1) + 24);
        int nx = cx - bw / 2 - 30;
        int ny = cy - 56;
        Holo.panel(ctx, nx, ny, nw, 40, a);
        Holo.bold(ctx, tr, "(!) NOTIFICATION", nx + 8, ny + 6, Holo.CYAN, a);
        Holo.centered(ctx, tr, line1, nx + nw / 2, ny + 24, Holo.BRIGHT, a, true);

        int sw2 = Math.max(100, tr.getWidth(line2) + 20);
        int sx = cx + 20;
        int sy = cy - 38;
        Holo.panel(ctx, sx, sy, sw2, 30, a);
        Holo.bold(ctx, tr, "(!) NOTIFICATION", sx + 6, sy + 5, Holo.CYAN, a);
        Holo.centered(ctx, tr, line2, sx + sw2 / 2, sy + 18, Holo.GOLD, a, false);
    }
}
