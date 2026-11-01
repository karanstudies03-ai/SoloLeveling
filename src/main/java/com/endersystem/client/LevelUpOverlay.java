package com.endersystem.client;

import com.endersystem.SystemData;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** The "LEVEL UP!" hologram notification, drawn on the HUD like the anime. */
public class LevelUpOverlay {
    private static final long DURATION_MS = 4500;
    private static long startMs = 0;
    private static int level = 1;
    private static int points = 0;

    public static void init() {
        HudRenderCallback.EVENT.register(LevelUpOverlay::render);
    }

    public static void trigger(int newLevel, int pointsGained) {
        startMs = System.currentTimeMillis();
        level = newLevel;
        points = pointsGained;
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        if (startMs == 0) return;
        long age = System.currentTimeMillis() - startMs;
        if (age > DURATION_MS) return;

        float fadeIn = Math.min(1f, age / 300f);
        float fadeOut = Math.min(1f, (DURATION_MS - age) / 700f);
        float a = Math.max(0f, Math.min(fadeIn, fadeOut));

        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int cx = sw / 2;
        int cy = sh / 3;

        // Big banner: LEVEL UP!
        Holo.panel(ctx, cx - 90, cy, 180, 34, a);
        Holo.centered(ctx, tr, "LEVEL UP!", cx, cy + 13, Holo.BRIGHT, a, true);

        // Notification box (left, above the banner)
        int nx = cx - 120;
        int ny = cy - 62;
        Holo.panel(ctx, nx, ny, 150, 52, a);
        Holo.bold(ctx, tr, "(!) NOTIFICATION", nx + 8, ny + 6, Holo.CYAN, a);
        Holo.centered(ctx, tr, "Leveled up!", nx + 75, ny + 22, Holo.BRIGHT, a, true);
        Holo.centered(ctx, tr, "Level " + level + "  |  Rank " + SystemData.rankFor(level),
                nx + 75, ny + 36, Holo.BRIGHT, a, false);

        // Small notification (right)
        int sx = cx + 40;
        int sy = cy - 40;
        Holo.panel(ctx, sx, sy, 100, 30, a);
        Holo.bold(ctx, tr, "(!) NOTIFICATION", sx + 6, sy + 5, Holo.CYAN, a);
        Holo.centered(ctx, tr, "+" + points + " stat points", sx + 50, sy + 18, Holo.GOLD, a, false);
    }
}
