package com.endersystem.client;

import com.endersystem.ModAttachments;
import com.endersystem.ModPackets;
import com.endersystem.SystemData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** The holographic STATUS window. The world stays visible behind it. Press Esc to close. */
public class StatusScreen extends Screen {
    private static final int W = 330;
    private static final int H = 200;
    private final ButtonWidget[] plus = new ButtonWidget[6];

    public StatusScreen() {
        super(Text.literal("System"));
    }

    @Override
    protected void init() {
        int left = (width - W) / 2;
        int top = (height - H) / 2;
        int rightX = left + 170;
        int panelY = top + 32;
        for (int i = 0; i < 6; i++) {
            final int stat = i;
            int by = panelY + 10 + i * 22;
            plus[i] = ButtonWidget.builder(Text.literal("+"),
                            b -> ClientPlayNetworking.send(new ModPackets.SpendPointPayload(stat)))
                    .dimensions(rightX + 160 - 30, by - 2, 18, 16)
                    .build();
            addDrawableChild(plus[i]);
        }
    }

    private SystemData data() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return SystemData.DEFAULT;
        return mc.player.getAttachedOrElse(ModAttachments.SYSTEM, SystemData.DEFAULT);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Intentionally empty: no dark/blur background, so it looks like a hologram.
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        SystemData d = data();

        int left = (width - W) / 2;
        int top = (height - H) / 2;
        int panelY = top + 32;
        int panelH = H - 32;

        // Title bar
        Holo.panel(ctx, left, top, W, 26, 1f);
        Holo.centered(ctx, textRenderer, "S T A T U S", left + W / 2, top + 9, Holo.BRIGHT, 1f, true);

        // Left panel: level, rank, bars
        int lx = left;
        int lw = 160;
        Holo.panel(ctx, lx, panelY, lw, panelH, 1f);
        Holo.bold(ctx, textRenderer, "LEVEL  " + d.level(), lx + 12, panelY + 12, Holo.BRIGHT, 1f);
        Holo.text(ctx, textRenderer, "RANK   " + d.rank(), lx + 12, panelY + 26, Holo.GOLD, 1f);
        Holo.text(ctx, textRenderer, "JOB    None", lx + 12, panelY + 40, Holo.CYAN, 1f);

        float hpFrac = mc.player.getMaxHealth() > 0 ? mc.player.getHealth() / mc.player.getMaxHealth() : 0;
        Holo.text(ctx, textRenderer, "HP", lx + 12, panelY + 66, Holo.CYAN, 1f);
        Holo.bar(ctx, lx + 34, panelY + 66, lw - 48, 8, hpFrac, 0xFF5252, 1f);
        Holo.text(ctx, textRenderer, (int) Math.ceil(mc.player.getHealth()) + " / " + (int) mc.player.getMaxHealth(),
                lx + 34, panelY + 78, Holo.BRIGHT, 1f);

        int need = SystemData.xpForNextLevel(d.level());
        float xpFrac = d.level() >= SystemData.MAX_LEVEL ? 1f : (float) d.xp() / need;
        Holo.text(ctx, textRenderer, "EXP", lx + 12, panelY + 104, Holo.CYAN, 1f);
        Holo.bar(ctx, lx + 34, panelY + 104, lw - 48, 8, xpFrac, Holo.CYAN, 1f);
        String xpText = d.level() >= SystemData.MAX_LEVEL ? "MAX LEVEL" : d.xp() + " / " + need;
        Holo.text(ctx, textRenderer, xpText, lx + 34, panelY + 116, Holo.BRIGHT, 1f);

        // Right panel: stats + upgrade buttons
        int rx = left + 170;
        int rw = 160;
        Holo.panel(ctx, rx, panelY, rw, panelH, 1f);
        for (int i = 0; i < 6; i++) {
            int ry = panelY + 10 + i * 22;
            Holo.text(ctx, textRenderer, SystemData.STAT_NAMES[i] + ":", rx + 12, ry + 2, Holo.CYAN, 1f);
            Holo.bold(ctx, textRenderer, String.valueOf(d.stat(i)), rx + 56, ry + 2, Holo.BRIGHT, 1f);
            plus[i].active = d.statPoints() > 0;
            plus[i].visible = d.statPoints() > 0;
        }
        int ptsColor = d.statPoints() > 0 ? Holo.GOLD : Holo.CYAN;
        Holo.text(ctx, textRenderer, "Available points: " + d.statPoints(),
                rx + 12, panelY + panelH - 16, ptsColor, 1f);

        super.render(ctx, mouseX, mouseY, delta); // draws the + buttons
    }
}
