package com.endersystem.client;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Drawing helpers for the blue hologram look. All colours are RGB; alpha is 0..1. */
public final class Holo {
    public static final int CYAN = 0x4FC3F7;
    public static final int BRIGHT = 0xBFEFFF;
    public static final int DIM = 0x7FB8D6;
    public static final int DARK = 0x061A33;
    public static final int GOLD = 0xFFD54F;
    public static final int GREEN = 0x66FF99;
    public static final int RED = 0xFF5252;

    private Holo() {}

    public static int argb(int rgb, float alpha) {
        int a = Math.max(0, Math.min(255, (int) (alpha * 255f)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static void frame(DrawContext c, int x, int y, int w, int h, int color) {
        c.fill(x, y, x + w, y + 1, color);
        c.fill(x, y + h - 1, x + w, y + h, color);
        c.fill(x, y, x + 1, y + h, color);
        c.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void panel(DrawContext c, int x, int y, int w, int h, float alpha) {
        panel(c, x, y, w, h, alpha, CYAN);
    }

    public static void panel(DrawContext c, int x, int y, int w, int h, float alpha, int borderRgb) {
        c.fill(x, y, x + w, y + h, argb(DARK, 0.72f * alpha));
        frame(c, x - 1, y - 1, w + 2, h + 2, argb(borderRgb, 0.25f * alpha));
        frame(c, x, y, w, h, argb(borderRgb, alpha));
    }

    /** A hologram-style button (drawn by us; the real click is handled by an invisible widget). */
    public static void button(DrawContext c, TextRenderer tr, int x, int y, int w, int h,
                              String label, boolean enabled, boolean selected, boolean hover) {
        int fill = selected ? 0x1E6FA8 : (hover && enabled ? 0x14507F : 0x0B2A4A);
        c.fill(x, y, x + w, y + h, argb(fill, selected ? 0.95f : 0.8f));
        if (selected || (hover && enabled)) {
            frame(c, x - 1, y - 1, w + 2, h + 2, argb(CYAN, 0.3f));
        }
        boolean lit = enabled || selected;
        frame(c, x, y, w, h, argb(lit ? CYAN : 0x3A5568, lit ? 1f : 0.6f));
        if (selected) {
            c.fill(x + 1, y + 1, x + 4, y + h - 1, argb(GOLD, 1f));
        }
        int textColor = selected ? 0xFFFFFF : (enabled ? BRIGHT : 0x6C8190);
        centered(c, tr, label, x + w / 2, y + (h - 8) / 2, textColor, 1f, selected);
    }

    public static void text(DrawContext c, TextRenderer tr, String s, int x, int y, int rgb, float alpha) {
        c.drawText(tr, Text.literal(s), x, y, argb(rgb, alpha), false);
    }

    public static void bold(DrawContext c, TextRenderer tr, String s, int x, int y, int rgb, float alpha) {
        c.drawText(tr, Text.literal(s).formatted(Formatting.BOLD), x, y, argb(rgb, alpha), false);
    }

    public static void right(DrawContext c, TextRenderer tr, String s, int xRight, int y, int rgb, float alpha) {
        c.drawText(tr, Text.literal(s), xRight - tr.getWidth(s), y, argb(rgb, alpha), false);
    }

    public static void centered(DrawContext c, TextRenderer tr, String s, int cx, int y, int rgb, float alpha, boolean bold) {
        Text t = bold ? Text.literal(s).formatted(Formatting.BOLD) : Text.literal(s);
        c.drawText(tr, t, cx - tr.getWidth(t) / 2, y, argb(rgb, alpha), false);
    }

    public static void bar(DrawContext c, int x, int y, int w, int h, float frac, int rgb, float alpha) {
        frac = Math.max(0f, Math.min(1f, frac));
        c.fill(x, y, x + w, y + h, argb(0x000000, 0.5f * alpha));
        c.fill(x, y, x + (int) (w * frac), y + h, argb(rgb, 0.85f * alpha));
        frame(c, x, y, w, h, argb(CYAN, 0.6f * alpha));
    }
}
