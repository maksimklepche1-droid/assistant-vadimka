package ru.vadimka.assistant.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Весь игровой HUD и общие графические примитивы Assistant Vadimka v9.0.
 */
public final class VadimkaHud {
    private VadimkaHud() {}

    public static void render(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        if (mc.screen instanceof VadimkaScreen) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // 1. Реалистичная GPU Мини-карта 128x128
        if (VadimkaConfig.visualsEnabled && VadimkaConfig.minimapRadarEnabled) {
            GpuMinimap.renderHud(g, mc);
        }

        // 2. Элитра Пич-Горизонт
        if (VadimkaConfig.visualsEnabled && VadimkaConfig.elytraPitchHorizon && mc.player.isFallFlying()) {
            int cx = sw / 2, cy = sh / 2;
            int shiftY = (int) Mth.clamp(-mc.player.getXRot() * 0.8F, -36.0F, 36.0F);
            g.fill(cx - 34, cy + shiftY, cx - 10, cy + shiftY + 1, 0xAA22D3EE);
            g.fill(cx + 10, cy + shiftY, cx + 34, cy + shiftY + 1, 0xAA22D3EE);
            g.drawString(mc.font, String.format("%.0f°", -mc.player.getXRot()), cx + 38, cy + shiftY - 3, 0xFF22D3EE);
        }

        // 3. Кастомный прицел
        if (VadimkaConfig.crosshairIndex > 0 && mc.screen == null) {
            renderCrosshair(g, sw, sh);
        }

        // 4. Верхние приоритетные статусы (Угроза / Активная задача / Навигатор / Библиотекарь)
        int topY = 8;
        if (VadimkaConfig.backstabIndicator && VadimkaConfig.threatBehindPlayer) {
            drawPill(g, mc.font, sw, topY, "⚠ УГРОЗА СЗАДИ (" + String.format("%.1f", VadimkaConfig.nearestHostileDist) + "м)", 0xE63B0A12, 0xFFEF4444, 0xFFFCA5A5);
            topY += 20;
        } else if (VadimkaConfig.fallDangerAlert && VadimkaConfig.fallDropBlocks >= 6 && mc.screen == null) {
            drawPill(g, mc.font, sw, topY, "⚠ ОБРЫВ: " + VadimkaConfig.fallDropBlocks + " бл.", 0xE63B2508, 0xFFF59E0B, 0xFFFDE68A);
            topY += 20;
        }

        if (ActionEngine.getCurrentTask() != ActionEngine.TaskType.NONE) {
            drawPill(g, mc.font, sw, topY, "⚡ " + ActionEngine.getActiveSummary() + " (Shift — стоп)", 0xE60B1D26, 0xFF22D3EE, 0xFFFFFFFF);
            topY += 20;
        }
        if (VillagerRoller.isRunning()) {
            drawPill(g, mc.font, sw, topY, "⚙ " + VillagerRoller.getStatusText() + " (Shift — стоп)", 0xE61A102B, 0xFFC084FC, 0xFFE9D5FF);
            topY += 20;
        }
        if (VaultHelper.isSniperArmed()) {
            int bd = VaultHelper.isMatchedRightNow() ? 0xFF4ADE80 : 0xFFF97316;
            drawPill(g, mc.font, sw, topY, "✦ ХРАНИЛИЩЕ: " + VaultHelper.getLiveDisplayStatus(), 0xE61A120A, bd, VaultHelper.isMatchedRightNow() ? 0xFF86EFAC : 0xFFFDE68A);
            topY += 20;
        }
        if (VadimkaConfig.activeNavTarget != null) {
            renderNavigatorPill(g, mc.font, mc.player, sw, topY);
        }

        // 5. Litematica HUD справа сверху
        if (VadimkaConfig.litematicaHudEnabled) {
            renderLitematicaHud(g, mc, sw);
        }

        // 6. Боевой PvP HUD справа снизу
        if (VadimkaConfig.combatHudEnabled) {
            renderCombatHud(g, mc, sw, sh);
        }

        // 7. Spotify Connect HUD слева снизу
        if (VadimkaConfig.spotifyHudEnabled) {
            renderSpotifyHud(g, mc, sh);
        }

        // 8. Всплывающее уведомление
        if (VadimkaConfig.toastTicks > 0 && !VadimkaConfig.toastText.isEmpty()) {
            int tw = mc.font.width(VadimkaConfig.toastText) + 18;
            int tx = (sw - tw) / 2;
            int ty = sh - 58;
            fillGlassCard(g, tx, ty, tw, 18, VadimkaConfig.currentTheme.bgWindow, VadimkaConfig.currentTheme.accent);
            g.drawString(mc.font, VadimkaConfig.toastText, tx + 9, ty + 5, 0xFFF8FAFC);
        }
    }

    private static void drawPill(GuiGraphics g, Font font, int sw, int y, String text, int bg, int border, int fg) {
        int w = font.width(text) + 18;
        int x = (sw - w) / 2;
        fillGlassCard(g, x, y, w, 17, bg, border);
        g.drawString(font, text, x + 9, y + 5, fg);
    }

    private static void renderNavigatorPill(GuiGraphics g, Font font, Player player, int sw, int y) {
        double dx = (VadimkaConfig.activeNavTarget.getX() + 0.5) - player.getX();
        double dz = (VadimkaConfig.activeNavTarget.getZ() + 0.5) - player.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        double rel = Mth.wrapDegrees((Math.toDegrees(Math.atan2(dz, dx)) - 90.0) - player.getYRot());
        String arrow = Math.abs(rel) < 22.5 ? "⬆"
            : (rel >= 22.5 && rel < 67.5 ? "↗"
            : (rel >= 67.5 && rel < 112.5 ? "➡"
            : (rel >= 112.5 && rel < 157.5 ? "↘"
            : (rel <= -22.5 && rel > -67.5 ? "↖"
            : (rel <= -67.5 && rel > -112.5 ? "⬅"
            : (rel <= -112.5 && rel > -157.5 ? "↙" : "⬇"))))));
        boolean auto = PathfinderAStar.isActive();
        String title = VadimkaConfig.activeNavTitle.isEmpty() ? "Дом" : VadimkaConfig.activeNavTitle;
        String txt = (auto ? "▶ A* " : "") + arrow + " " + title + " • " + dist + "м" + (auto ? " (S/Shift — стоп)" : "");
        drawPill(g, font, sw, y, txt, VadimkaConfig.currentTheme.bgWindow, auto ? 0xFF4ADE80 : VadimkaConfig.activeNavColor, 0xFFFFFFFF);
    }

    private static void renderCrosshair(GuiGraphics g, int sw, int sh) {
        int cx = sw / 2, cy = sh / 2, col = VadimkaConfig.currentTheme.accent;
        if (VadimkaConfig.crosshairIndex == 1) {
            g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
            g.fill(cx - 6, cy, cx - 2, cy + 1, col); g.fill(cx + 2, cy, cx + 6, cy + 1, col);
            g.fill(cx, cy - 6, cx + 1, cy - 2, col); g.fill(cx, cy + 2, cx + 1, cy + 6, col);
        } else if (VadimkaConfig.crosshairIndex == 2) {
            drawRoundedBorder(g, cx - 5, cy - 5, 11, 11, col);
            g.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
        } else if (VadimkaConfig.crosshairIndex == 3) {
            g.fill(cx - 7, cy, cx - 3, cy + 1, VadimkaConfig.currentTheme.secondary);
            g.fill(cx + 4, cy, cx + 8, cy + 1, VadimkaConfig.currentTheme.secondary);
            g.fill(cx, cy + 3, cx + 1, cy + 7, VadimkaConfig.currentTheme.secondary);
            g.fill(cx, cy, cx + 1, cy + 1, col);
        }
    }

    private static void renderLitematicaHud(GuiGraphics g, Minecraft mc, int sw) {
        Player p = mc.player;
        if (p == null) return;
        ItemStack hand = p.getMainHandItem();
        if (hand.isEmpty() && !VadimkaConfig.ruler3DVolume) return;
        int count = 0;
        if (!hand.isEmpty()) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack st = p.getInventory().getItem(i);
                if (!st.isEmpty() && st.getItem() == hand.getItem()) count += st.getCount();
            }
        }
        int w = 152, h = 30;
        int x = VadimkaConfig.litematicaHudRight ? (sw - w - 10) : 124;
        int y = 30;
        fillGlassCard(g, x, y, w, h, VadimkaConfig.currentTheme.bgWindow, VadimkaConfig.currentTheme.borderMuted);
        if (!hand.isEmpty()) g.renderItem(hand, x + 4, y + 7);
        int tx = hand.isEmpty() ? (x + 6) : (x + 24);
        String line1 = hand.isEmpty() ? "Litematica HUD" : (hand.getHoverName().getString() + " x" + count);
        g.drawString(mc.font, VadimkaConfig.trimText(mc.font, line1, w - (tx - x) - 6), tx, y + 5, 0xFFF8FAFC);

        String line2 = "Хранилищ: " + VaultHelper.countVaultBlocks() + " • Y:" + (int) p.getY();
        if (VadimkaConfig.ruler3DVolume && mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK && mc.level != null) {
            double dist = p.position().distanceTo(bhr.getLocation());
            int rs = VadimkaConfig.redstoneInspector ? mc.level.getBestNeighborSignal(bhr.getBlockPos()) : 0;
            line2 = String.format("%.1fм • Y:%d%s", dist, bhr.getBlockPos().getY(), rs > 0 ? (" RS:" + rs) : "");
        }
        g.drawString(mc.font, VadimkaConfig.trimText(mc.font, line2, w - (tx - x) - 6), tx, y + 17, VadimkaConfig.currentTheme.accent);
    }

    private static void renderCombatHud(GuiGraphics g, Minecraft mc, int sw, int sh) {
        Player p = mc.player;
        if (p == null) return;
        int totems = VadimkaConfig.countPlayerTotems(p);
        int pearls = 0, gapples = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(Items.ENDER_PEARL)) pearls += st.getCount();
            else if (st.is(Items.GOLDEN_APPLE) || st.is(Items.ENCHANTED_GOLDEN_APPLE)) gapples += st.getCount();
        }
        int w = 148, h = 48, x = sw - w - 10, y = sh - h - 10;
        fillGlassCard(g, x, y, w, h, VadimkaConfig.currentTheme.bgWindow, VadimkaConfig.currentTheme.borderMuted);
        g.drawString(mc.font, "Тотем:" + totems + " Перл:" + pearls + " Ябл:" + gapples, x + 6, y + 5, 0xFFFDE047);

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] lbl = {"Ш", "Н", "П", "Б"};
        int bx = x + 6, by = y + 19;
        for (int i = 0; i < slots.length; i++) {
            ItemStack armor = p.getItemBySlot(slots[i]);
            int pct = 0;
            if (!armor.isEmpty() && armor.isDamageableItem() && armor.getMaxDamage() > 0) {
                pct = (int) Math.round(((armor.getMaxDamage() - armor.getDamageValue()) * 100.0) / armor.getMaxDamage());
            } else if (!armor.isEmpty()) pct = 100;
            int col = pct > 55 ? 0xFF4ADE80 : (pct > 25 ? 0xFFFACC15 : (pct > 0 ? 0xFFEF4444 : 0xFF334155));
            g.drawString(mc.font, lbl[i] + ":" + pct + "%", bx + i * 34, by, 0xFFCBD5E1);
            g.fill(bx + i * 34, by + 11, bx + i * 34 + 30, by + 15, 0xFF1E293B);
            if (pct > 0) g.fill(bx + i * 34, by + 11, bx + i * 34 + Math.max(2, (30 * pct) / 100), by + 15, col);
        }
    }

    private static void renderSpotifyHud(GuiGraphics g, Minecraft mc, int sh) {
        int w = 188, h = 34, x = 10, y = sh - h - 10;
        boolean play = SpotifyBridge.isPlaying();
        fillGlassCard(g, x, y, w, h, 0xEE0A1710, play ? 0xFF1DB954 : 0xFF334155);
        g.blit(RenderPipelines.GUI_TEXTURED, VadimkaConfig.MenuTab.SPOTIFY_PLAYER.iconTex, x + 6, y + 6, 0.0F, 0.0F, 22, 22, 22, 22);
        g.drawString(mc.font, VadimkaConfig.trimText(mc.font, SpotifyBridge.getTitle(), w - 38), x + 32, y + 5, 0xFFFFFFFF);
        g.drawString(mc.font, VadimkaConfig.trimText(mc.font, SpotifyBridge.getArtist(), w - 38), x + 32, y + 15, play ? 0xFF86EFAC : 0xFF94A3B8);
        int bx = x + 32, bw = w - 38;
        g.fill(bx, y + 26, bx + bw, y + 28, 0xFF1F2937);
        int dur = SpotifyBridge.getDurSec();
        float prog = dur > 0 ? Mth.clamp((float) SpotifyBridge.getPosSec() / dur, 0.0F, 1.0F) : (play ? 0.5F : 0.0F);
        g.fill(bx, y + 26, bx + (int) (bw * prog), y + 28, 0xFF1DB954);
    }

    // =========================================================================
    // ОБЩИЕ ГРАФИЧЕСКИЕ ПРИМИТИВЫ
    // =========================================================================

    public static void fillGlassCard(GuiGraphics g, int x, int y, int w, int h, int fillCol, int borderCol) {
        fillRoundedRect(g, x, y, w, h, fillCol);
        drawRoundedBorder(g, x, y, w, h, borderCol);
    }

    public static void drawGradientNeonBorder(GuiGraphics g, int x, int y, int w, int h, int colLeft, int colRight) {
        if (w <= 4 || h <= 4) return;
        int segs = 10;
        for (int i = 0; i < segs; i++) {
            int x1 = x + 2 + ((w - 4) * i) / segs;
            int x2 = x + 2 + ((w - 4) * (i + 1)) / segs;
            int col = lerpColor(colLeft, colRight, (float) i / (segs - 1));
            g.fill(x1, y, x2, y + 1, col);
            g.fill(x1, y + h - 1, x2, y + h, col);
        }
        g.fill(x, y + 2, x + 1, y + h - 2, colLeft);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, colRight);
        g.fill(x + 1, y + 1, x + 2, y + 2, colLeft);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, colRight);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, colLeft);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, colRight);
    }

    public static void fillRoundedRect(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 4 || h <= 4) { g.fill(x, y, x + w, y + h, color); return; }
        g.fill(x + 2, y, x + w - 2, y + 1, color);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, color);
        g.fill(x, y + 2, x + w, y + h - 2, color);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, color);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
    }

    public static void drawRoundedBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 4 || h <= 4) return;
        g.fill(x + 2, y, x + w - 2, y + 1, color);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
        g.fill(x, y + 2, x + 1, y + h - 2, color);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
        g.fill(x + 1, y + 1, x + 2, y + 2, color);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
    }

    public static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
        return ((int) (a1 + (a2 - a1) * t) << 24) | ((int) (r1 + (r2 - r1) * t) << 16)
            | ((int) (g1 + (g2 - g1) * t) << 8) | (int) (b1 + (b2 - b1) * t);
    }
}
