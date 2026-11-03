package com.endersystem.client;

import com.endersystem.ModAttachments;
import com.endersystem.PenaltyData;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

/** Red countdown at the top of the screen while the Penalty Quest is active. */
public class PenaltyHud {

    public static void init() {
        HudRenderCallback.EVENT.register(PenaltyHud::render);
    }

    private static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        PenaltyData pd = mc.player.getAttachedOrElse(ModAttachments.PENALTY, PenaltyData.NONE);
        if (!pd.active()) return;

        TextRenderer tr = mc.textRenderer;
        int secs = (pd.ticksLeft() + 19) / 20;
        int cx = ctx.getScaledWindowWidth() / 2;
        int y = 8;

        Holo.panel(ctx, cx - 90, y, 180, 40, 1f, Holo.RED);
        Holo.centered(ctx, tr, "PENALTY ZONE", cx, y + 6, Holo.RED, 1f, true);
        Holo.centered(ctx, tr, "Survive:  " + (secs / 60) + ":" + String.format("%02d", secs % 60),
                cx, y + 18, Holo.BRIGHT, 1f, false);
        float frac = (float) pd.ticksLeft() / PenaltyData.DURATION_TICKS;
        Holo.bar(ctx, cx - 80, y + 30, 160, 5, frac, Holo.RED, 1f);
    }
}
