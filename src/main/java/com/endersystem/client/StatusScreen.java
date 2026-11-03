package com.endersystem.client;

import com.endersystem.ModAttachments;
import com.endersystem.ModPackets;
import com.endersystem.QuestData;
import com.endersystem.ShopData;
import com.endersystem.SystemData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The holographic System window with tabs. The world stays visible behind it. Esc closes it.
 * All buttons are drawn in the hologram style by us; vanilla buttons are only used invisibly
 * to receive the clicks.
 */
public class StatusScreen extends Screen {

    private enum Tab {
        STATUS("Status", "S T A T U S"),
        UPGRADE("Upgrade", "U P G R A D E"),
        QUESTS("Daily Quests", "D A I L Y   Q U E S T S"),
        SHOP("Shop", "S H O P");

        final String label;
        final String title;

        Tab(String label, String title) {
            this.label = label;
            this.title = title;
        }
    }

    private static Tab lastTab = Tab.STATUS;

    private static final int TAB_W = 80;
    private static final int W = 320;
    private static final int H = 200;
    private static final String[] EFFECTS = {
            "attack damage", "move speed", "max health", "mana (soon)", "armor", "loot luck"};

    private Tab tab = lastTab;
    private final List<ButtonWidget> tabButtons = new ArrayList<>();
    private final List<ButtonWidget> upgradeButtons = new ArrayList<>();
    private final List<ButtonWidget> buyButtons = new ArrayList<>();

    public StatusScreen() {
        super(Text.literal("System"));
    }

    private int winX() { return (width - (TAB_W + W)) / 2 + TAB_W; }
    private int top() { return (height - H) / 2; }
    private int panelY() { return top() + 32; }
    private int panelH() { return H - 32; }

    /** An invisible vanilla button that only receives the click. */
    private ButtonWidget clickArea(int x, int y, int w, int h, Runnable action) {
        ButtonWidget b = ButtonWidget.builder(Text.empty(), btn -> action.run())
                .dimensions(x, y, w, h)
                .build();
        b.setAlpha(0f);
        addDrawableChild(b);
        return b;
    }

    @Override
    protected void init() {
        tabButtons.clear();
        upgradeButtons.clear();
        buyButtons.clear();

        int tabX = winX() - TAB_W;
        int i = 0;
        for (Tab t : Tab.values()) {
            tabButtons.add(clickArea(tabX, panelY() + i * 26, 74, 22, () -> {
                lastTab = t;
                tab = t;
                clearAndInit();
            }));
            i++;
        }

        if (tab == Tab.UPGRADE) {
            for (int s = 0; s < 6; s++) {
                final int stat = s;
                int ry = panelY() + 28 + s * 22;
                upgradeButtons.add(clickArea(winX() + W - 40, ry - 3, 28, 16,
                        () -> ClientPlayNetworking.send(new ModPackets.SpendPointPayload(stat))));
            }
        }

        if (tab == Tab.SHOP) {
            for (int s = 0; s < ShopData.BUY.size(); s++) {
                final int idx = s;
                int ry = panelY() + 10 + s * 28;
                buyButtons.add(clickArea(winX() + W - 56, ry + 3, 44, 16,
                        () -> ClientPlayNetworking.send(new ModPackets.BuyPayload(idx))));
            }
        }
    }

    private SystemData data() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return SystemData.DEFAULT;
        return mc.player.getAttachedOrElse(ModAttachments.SYSTEM, SystemData.DEFAULT);
    }

    private QuestData quests() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return QuestData.DEFAULT;
        return mc.player.getAttachedOrElse(ModAttachments.QUESTS, QuestData.DEFAULT);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Intentionally empty: no dark/blur background, so it looks like a hologram.
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void drawBtn(DrawContext ctx, ButtonWidget b, String label, boolean selected, int mx, int my) {
        Holo.button(ctx, textRenderer, b.getX(), b.getY(), b.getWidth(), b.getHeight(),
                label, b.active, selected, b.isMouseOver(mx, my));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        SystemData d = data();

        int wx = winX();
        int top = top();

        // Title bar + gold (top right)
        Holo.panel(ctx, wx, top, W, 26, 1f);
        Holo.centered(ctx, textRenderer, tab.title, wx + W / 2, top + 9, Holo.BRIGHT, 1f, true);
        Holo.right(ctx, textRenderer, "GOLD  " + d.gold(), wx + W - 10, top + 9, Holo.GOLD, 1f);

        // Tab bar in the same theme
        Tab[] tabs = Tab.values();
        for (int i = 0; i < tabs.length && i < tabButtons.size(); i++) {
            drawBtn(ctx, tabButtons.get(i), tabs[i].label, tabs[i] == tab, mouseX, mouseY);
        }

        switch (tab) {
            case STATUS -> renderStatus(ctx, mc, d);
            case UPGRADE -> renderUpgrade(ctx, d, mouseX, mouseY);
            case QUESTS -> renderQuests(ctx);
            case SHOP -> renderShop(ctx, d, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta); // invisible click areas draw nothing
    }

    private void renderStatus(DrawContext ctx, MinecraftClient mc, SystemData d) {
        int wx = winX();
        int py = panelY();
        int ph = panelH();

        int lw = 156;
        Holo.panel(ctx, wx, py, lw, ph, 1f);
        Holo.bold(ctx, textRenderer, "LEVEL  " + d.level(), wx + 12, py + 12, Holo.BRIGHT, 1f);
        Holo.text(ctx, textRenderer, "RANK   " + d.rank(), wx + 12, py + 26, Holo.GOLD, 1f);
        Holo.text(ctx, textRenderer, "JOB    None", wx + 12, py + 40, Holo.CYAN, 1f);

        float hpFrac = mc.player.getMaxHealth() > 0 ? mc.player.getHealth() / mc.player.getMaxHealth() : 0;
        Holo.text(ctx, textRenderer, "HP", wx + 12, py + 66, Holo.CYAN, 1f);
        Holo.bar(ctx, wx + 34, py + 66, lw - 48, 8, hpFrac, 0xFF5252, 1f);
        Holo.text(ctx, textRenderer, (int) Math.ceil(mc.player.getHealth()) + " / " + (int) mc.player.getMaxHealth(),
                wx + 34, py + 78, Holo.BRIGHT, 1f);

        int need = SystemData.xpForNextLevel(d.level());
        float xpFrac = d.level() >= SystemData.MAX_LEVEL ? 1f : (float) d.xp() / need;
        Holo.text(ctx, textRenderer, "EXP", wx + 12, py + 104, Holo.CYAN, 1f);
        Holo.bar(ctx, wx + 34, py + 104, lw - 48, 8, xpFrac, Holo.CYAN, 1f);
        String xpText = d.level() >= SystemData.MAX_LEVEL ? "MAX LEVEL" : d.xp() + " / " + need;
        Holo.text(ctx, textRenderer, xpText, wx + 34, py + 116, Holo.BRIGHT, 1f);

        int rx = wx + 164;
        int rw = 156;
        Holo.panel(ctx, rx, py, rw, ph, 1f);
        for (int i = 0; i < 6; i++) {
            int ry = py + 10 + i * 22;
            Holo.text(ctx, textRenderer, SystemData.STAT_NAMES[i] + ":", rx + 12, ry + 2, Holo.CYAN, 1f);
            Holo.bold(ctx, textRenderer, String.valueOf(d.stat(i)), rx + 56, ry + 2, Holo.BRIGHT, 1f);
        }
        int ptsColor = d.statPoints() > 0 ? Holo.GOLD : Holo.CYAN;
        Holo.text(ctx, textRenderer, "Available points: " + d.statPoints(),
                rx + 12, py + ph - 16, ptsColor, 1f);
    }

    private void renderUpgrade(DrawContext ctx, SystemData d, int mx, int my) {
        int wx = winX();
        int py = panelY();
        int ph = panelH();
        Holo.panel(ctx, wx, py, W, ph, 1f);

        int ptsColor = d.statPoints() >= SystemData.UPGRADE_COST ? Holo.GOLD : Holo.CYAN;
        Holo.text(ctx, textRenderer, "Available points: " + d.statPoints(), wx + 14, py + 8, ptsColor, 1f);
        Holo.right(ctx, textRenderer, SystemData.UPGRADE_COST + " points = +1 stat", wx + W - 14, py + 8, Holo.DIM, 1f);

        for (int i = 0; i < 6; i++) {
            int ry = py + 28 + i * 22;
            Holo.text(ctx, textRenderer, SystemData.STAT_NAMES[i] + ":", wx + 14, ry + 2, Holo.CYAN, 1f);
            Holo.bold(ctx, textRenderer, String.valueOf(d.stat(i)), wx + 50, ry + 2, Holo.BRIGHT, 1f);
            Holo.text(ctx, textRenderer, EFFECTS[i], wx + 84, ry + 2, Holo.DIM, 1f);
            Holo.right(ctx, textRenderer, SystemData.UPGRADE_COST + " pts", wx + W - 46, ry + 2, Holo.GOLD, 1f);
            if (i < upgradeButtons.size()) {
                ButtonWidget b = upgradeButtons.get(i);
                b.active = d.statPoints() >= SystemData.UPGRADE_COST;
                drawBtn(ctx, b, "+", false, mx, my);
            }
        }
    }

    private static String untilMidnight() {
        LocalDateTime now = LocalDateTime.now();
        long s = Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay()).getSeconds();
        return String.format("%02d:%02d:%02d", s / 3600, (s % 3600) / 60, s % 60);
    }

    private void renderQuests(DrawContext ctx) {
        QuestData q = quests();
        int wx = winX();
        int py = panelY();
        int ph = panelH();
        Holo.panel(ctx, wx, py, W, ph, 1f);

        Holo.text(ctx, textRenderer, "Finish all three to earn " + QuestData.REWARD_POINTS + " stat points!",
                wx + 14, py + 8, Holo.GOLD, 1f);

        String[] names = {"Squats  (crouch)", "Jumps", "Sprint  (blocks)"};
        int[] cur = {q.crouches(), q.jumps(), q.sprintBlocks()};
        int[] goal = {QuestData.CROUCH_GOAL, QuestData.JUMP_GOAL, QuestData.SPRINT_BLOCKS_GOAL};

        for (int i = 0; i < 3; i++) {
            int y = py + 26 + i * 30;
            int shown = Math.min(cur[i], goal[i]);
            boolean done = cur[i] >= goal[i];
            Holo.text(ctx, textRenderer, names[i], wx + 14, y, done ? Holo.GREEN : Holo.BRIGHT, 1f);
            Holo.right(ctx, textRenderer, shown + " / " + goal[i], wx + W - 14, y, done ? Holo.GREEN : Holo.CYAN, 1f);
            Holo.bar(ctx, wx + 14, y + 11, W - 28, 8, (float) shown / goal[i], done ? Holo.GREEN : Holo.CYAN, 1f);
        }

        if (q.claimed()) {
            Holo.centered(ctx, textRenderer, "ALL QUESTS COMPLETE  -  REWARD CLAIMED",
                    wx + W / 2, py + 118, Holo.GOLD, 1f, true);
        } else {
            Holo.centered(ctx, textRenderer, "WARNING: Failure triggers a PENALTY QUEST!",
                    wx + W / 2, py + 118, Holo.RED, 1f, true);
        }
        Holo.centered(ctx, textRenderer, "Resets at midnight  -  in " + untilMidnight(),
                wx + W / 2, py + 138, Holo.DIM, 1f, false);
    }

    private void renderShop(DrawContext ctx, SystemData d, int mx, int my) {
        int wx = winX();
        int py = panelY();
        int ph = panelH();
        Holo.panel(ctx, wx, py, W, ph, 1f);

        for (int i = 0; i < ShopData.BUY.size(); i++) {
            ShopData.BuyEntry e = ShopData.BUY.get(i);
            int ry = py + 10 + i * 28;
            ctx.drawItem(new ItemStack(e.item()), wx + 12, ry + 3);
            Holo.bold(ctx, textRenderer, new ItemStack(e.item()).getName().getString(), wx + 36, ry + 2, Holo.BRIGHT, 1f);
            Holo.text(ctx, textRenderer, e.usage(), wx + 36, ry + 13, Holo.DIM, 1f);
            boolean afford = d.gold() >= e.price();
            Holo.right(ctx, textRenderer, e.price() + " G", wx + W - 62, ry + 7, afford ? Holo.GOLD : 0xFF6666, 1f);
            if (i < buyButtons.size()) {
                ButtonWidget b = buyButtons.get(i);
                b.active = afford;
                drawBtn(ctx, b, "Buy", false, mx, my);
            }
        }
        Holo.text(ctx, textRenderer, "Tip: right-click an item in your inventory to sell it.",
                wx + 14, py + ph - 16, Holo.DIM, 1f);
    }
}
