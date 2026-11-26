package ru.vadimka.assistant.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AssistantVadimkaClient implements ClientModInitializer {
public static final String MOD_ID = "assistant_vadimka";

public enum VadimkaForm {
PLAYER("Player (Игрок)", 0xFF38BDF8, 0xFFFDE68A),
ALLAY("Allay (Спешик)", 0xFF22D3EE, 0xFFA5F3FC),
BREEZE("Breeze (Вихрь)", 0xFF818CF8, 0xFFC7D2FE),
WARDEN("Warden (Хранитель)", 0xFF0F766E, 0xFF2DD4BF),
AXOLOTL("Blue Axolotl (Аксолотль)", 0xFF60A5FA, 0xFFF472B6),
RAT("Rat (Крыска)", 0xFF94A3B8, 0xFFF9A8D4);

public final String title;
public final int primaryColor;
public final int accentColor;

VadimkaForm(String title, int primaryColor, int accentColor) {
this.title = title;
this.primaryColor = primaryColor;
this.accentColor = accentColor;
}
}

public enum MinionRole {
COLLECTOR("Сборщик"),
BUILDER("Строитель-подручный"),
SCOUT("Разведчик / Факельщик"),
SUPPLIER("Снабженец");

public final String title;

MinionRole(String title) {
this.title = title;
}
}

public enum MenuTab {
VADIMKA("1. Спутник Вадимка"),
AUTOPILOT("2. Автопилот"),
AUTO_BUILD("3. Авто-постройка"),
LITEMATICA_HUD("4. Litematica HUD"),
VISUAL_RADAR("5. Визуал и Радар"),
CHESTS_VILLAGERS("6. Ларчики и Жители"),
COMBAT_PVP("7. Бой и PvP"),
MUSIC_AFK("8. Музыка и AFK"),
CUSTOM_VISUAL("9. Кастом-визуал");

public final String title;

MenuTab(String title) {
this.title = title;
}
}

public enum MenuTheme {
CYBER_EMERALD("Cyber Emerald", 0xE60B1218, 0xFF131E29, 0xFF10B981, 0xFF38BDF8),
NETHER_CRIMSON("Nether Crimson", 0xE6160B0F, 0xFF261219, 0xFFF43F5E, 0xFFFBBF24),
AMETHYST_VOID("Amethyst Void", 0xE6110C1D, 0xFF1E1533, 0xFFA855F7, 0xFFF472B6),
CLASSIC_DARK("Classic Dark", 0xE6121316, 0xFF1E2025, 0xFFE2E8F0, 0xFF94A3B8);

public final String title;
public final int bgMain;
public final int bgCard;
public final int accent;
public final int secondary;

MenuTheme(String title, int bgMain, int bgCard, int accent, int secondary) {
this.title = title;
this.bgMain = bgMain;
this.bgCard = bgCard;
this.accent = accent;
this.secondary = secondary;
}
}

// Состояние Вадимки и миньонов
public static VadimkaForm currentForm = VadimkaForm.RAT;
public static boolean quietMode = false;
public static boolean randomChaosMode = false;
public static boolean voiceCommentator = true;
public static final Map<MinionRole, Boolean> activeMinions = new EnumMap<>(MinionRole.class);

// Состояние модулей
public static boolean styleProfiler = true;
public static boolean safeAutopilot = false;
public static boolean litematicaAutoBuild = false;
public static boolean smartRestoration = true;
public static boolean hotbarAutoRefill = true;
public static boolean shulkerScanner = true;
public static boolean layerSliceMode = false;

// Радар и визуал
public static boolean radarHudEnabled = true;
public static boolean soundRadarEnabled = true;
public static boolean ariadneThread = true;
public static boolean sixthSense = true;
public static boolean chestMemory = true;
public static boolean smartSorter = false;
public static boolean trajectoriesEnabled = true;
public static boolean totemScanner = true;
public static boolean spotifyHud = true;
public static boolean afkPanicExit = true;

// Кастомизация персонажа и меню
public static MenuTheme currentTheme = MenuTheme.CYBER_EMERALD;
public static final List<String> WINGS_OPTIONS = List.of("Выкл", "Ангельские", "Демонические", "Неон-элитры");
public static final List<String> HORNS_OPTIONS = List.of("Выкл", "Дьявольские", "Оленьи", "Кибер-рожки");
public static final List<String> HATS_OPTIONS = List.of("Выкл", "Корона", "Ушки Крыски", "Цилиндр");
public static final List<String> OUTFIT_OPTIONS = List.of("Стандарт", "Кибер-плащ", "Тень Ассасина", "Изумрудный сет");
public static final List<String> CROSSHAIR_OPTIONS = List.of("Точка + Крест", "Неон-круг", "Динамический", "Ванильный");

public static int wingsIndex = 1;
public static int hornsIndex = 1;
public static int hatsIndex = 1;
public static int outfitIndex = 1;
public static int crosshairIndex = 0;

private static KeyMapping openMenuKey;

@Override
public void onInitializeClient() {
for (MinionRole role : MinionRole.values()) {
activeMinions.put(role, true);
}

openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
"key.assistant_vadimka.menu",
GLFW.GLFW_KEY_P,
KeyMapping.Category.MISC
));

ClientTickEvents.END_CLIENT_TICK.register(client -> {
if (client.player == null) {
return;
}
while (openMenuKey.consumeClick()) {
if (client.screen instanceof AssistantVadimkaScreen) {
client.setScreen(null);
} else {
client.setScreen(new AssistantVadimkaScreen(MenuTab.VADIMKA));
}
}
});

HudRenderCallback.EVENT.register((graphics, tickCounter) -> renderRadarHud(graphics));
}

private static void renderRadarHud(GuiGraphics graphics) {
if (!radarHudEnabled) {
return;
}
Minecraft mc = Minecraft.getInstance();
if (mc.player == null || mc.level == null || mc.options.hideGui) {
return;
}

int x = 10;
int y = 10;
int size = 94;
int centerX = x + size / 2;
int centerY = y + size / 2;

// Фон и рамка радара
graphics.fill(x, y, x + size, y + size, currentTheme.bgMain);
drawBorder(graphics, x, y, size, size, currentTheme.accent);

// Сетка радара
graphics.fill(centerX, y + 4, centerX + 1, y + size - 4, 0x33FFFFFF);
graphics.fill(x + 4, centerY, x + size - 4, centerY + 1, 0x33FFFFFF);

double yawRad = Math.toRadians(mc.player.getYRot() + 180.0);
double cos = Math.cos(yawRad);
double sin = Math.sin(yawRad);
double scale = 0.75;
int maxOffset = (size / 2) - 6;

// Индикатор Севера (N)
int northX = centerX + (int) (-sin * (maxOffset - 2));
int northY = centerY + (int) (-cos * (maxOffset - 2));
graphics.drawString(mc.font, "N", northX - 2, northY - 3, currentTheme.secondary);

// Отрисовка сущностей вокруг
for (Entity entity : mc.level.entitiesForRendering()) {
if (entity == mc.player) {
continue;
}
double dx = entity.getX() - mc.player.getX();
double dz = entity.getZ() - mc.player.getZ();
if (Math.abs(dx) > 60 || Math.abs(dz) > 60) {
continue;
}

double rotX = (dx * cos - dz * sin) * scale;
double rotY = (dx * sin + dz * cos) * scale;
int dotX = centerX + (int) Math.max(-maxOffset, Math.min(maxOffset, rotX));
int dotY = centerY + (int) Math.max(-maxOffset, Math.min(maxOffset, rotY));

if (entity instanceof Player otherPlayer) {
graphics.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, 0xFF38BDF8);
graphics.drawString(mc.font, otherPlayer.getName().getString(), dotX + 3, dotY - 3, 0xFFE0F2FE);
} else if (entity instanceof Monster) {
graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFFEF4444);
} else if (entity instanceof Animal) {
graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFF4ADE80);
}
}

// Маркер игрока в центре
graphics.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xFFFFFFFF);

// Плашка координат и статуса Вадимки под радаром
int infoY = y + size + 3;
graphics.fill(x, infoY, x + size, infoY + 24, currentTheme.bgMain);
drawBorder(graphics, x, infoY, size, 24, currentTheme.secondary);

String coords = (int) mc.player.getX() + " / " + (int) mc.player.getY() + " / " + (int) mc.player.getZ();
graphics.drawString(mc.font, coords, x + 4, infoY + 3, 0xFFE2E8F0);
String vadimkaShort = currentForm == VadimkaForm.RAT ? "Вадимка: Крыска" : "Вадимка: " + currentForm.name();
graphics.drawString(mc.font, vadimkaShort, x + 4, infoY + 13, currentTheme.accent);
}

private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
graphics.fill(x, y, x + w, y + 1, color);
graphics.fill(x, y + h - 1, x + w, y + h, color);
graphics.fill(x, y, x + 1, y + h, color);
graphics.fill(x + w - 1, y, x + w, y + h, color);
}

public static class AssistantVadimkaScreen extends Screen {
private MenuTab activeTab;

// Мини-игра AFK (уворот от криперов в 3 полосах)
private boolean afkGameRunning = false;
private int afkPlayerLane = 1;
private int afkScore = 0;
private int afkBestScore = 0;
private int afkTickCounter = 0;
private final List<int[]> afkObstacles = new ArrayList<>();
private final Random random = new Random();

private int winX, winY, winW, winH;
private int sidebarW, previewW, contentX, contentW;

public AssistantVadimkaScreen(MenuTab activeTab) {
super(Component.literal("ASSISTANT VADIMKA"));
this.activeTab = activeTab;
}

@Override
protected void init() {
this.winW = Math.min(this.width - 24, 540);
this.winH = Math.min(this.height - 24, 300);
this.winX = (this.width - this.winW) / 2;
this.winY = (this.height - this.winH) / 2;

this.sidebarW = 128;
this.previewW = 148;
this.contentX = this.winX + this.sidebarW + 8;
this.contentW = this.winW - this.sidebarW - this.previewW - 22;

// Кнопки 9 вкладок в левой боковой панели
MenuTab[] tabs = MenuTab.values();
int tabBtnH = 20;
int tabStartY = this.winY + 34;
for (int i = 0; i < tabs.length; i++) {
MenuTab tab = tabs[i];
int by = tabStartY + i * (tabBtnH + 3);
Button btn = Button.builder(Component.literal(tab.title), b -> {
this.activeTab = tab;
if (tab != MenuTab.MUSIC_AFK) {
this.afkGameRunning = false;
}
this.rebuildWidgets();
}).bounds(this.winX + 6, by, this.sidebarW - 12, tabBtnH).build();
btn.active = (tab != this.activeTab);
this.addRenderableWidget(btn);
}

// Кнопки управления внутри активной вкладки
int ctrlX = this.contentX + 6;
int ctrlY = this.winY + 62;
int ctrlW = this.contentW - 12;

switch (this.activeTab) {
case VADIMKA -> {
addToggleButton(ctrlX, ctrlY, ctrlW, "Облик Вадимки: " + currentForm.title, () -> {
VadimkaForm[] forms = VadimkaForm.values();
currentForm = forms[(currentForm.ordinal() + 1) % forms.length];
});
addToggleButton(ctrlX, ctrlY + 24, ctrlW, "Тихий режим: " + boolText(quietMode), () -> quietMode = !quietMode);
addToggleButton(ctrlX, ctrlY + 48, ctrlW, "Рандомная дичь: " + boolText(randomChaosMode), () -> randomChaosMode = !randomChaosMode);
addToggleButton(ctrlX, ctrlY + 72, ctrlW, "Голос и комментатор: " + boolText(voiceCommentator), () -> voiceCommentator = !voiceCommentator);

int my = ctrlY + 102;
for (MinionRole role : MinionRole.values()) {
boolean state = Boolean.TRUE.equals(activeMinions.get(role));
addToggleButton(ctrlX, my, ctrlW, "Миньон [" + role.title + "]: " + boolText(state), () -> {
activeMinions.put(role, !Boolean.TRUE.equals(activeMinions.get(role)));
});
my += 22;
}
}
case AUTOPILOT -> {
addToggleButton(ctrlX, ctrlY, ctrlW, "Профайлер стиля игры: " + boolText(styleProfiler), () -> styleProfiler = !styleProfiler);
addToggleButton(ctrlX, ctrlY + 24, ctrlW, "Безопасный автопилот: " + boolText(safeAutopilot), () -> safeAutopilot = !safeAutopilot);
}
case AUTO_BUILD -> {
addToggleButton(ctrlX, ctrlY, ctrlW, "Авто-постройка по схеме: " + boolText(litematicaAutoBuild), () -> litematicaAutoBuild = !litematicaAutoBuild);
addToggleButton(ctrlX, ctrlY + 24, ctrlW, "Умная реставрация: " + boolText(smartRestoration), () -> smartRestoration = !smartRestoration);
addToggleButton(ctrlX, ctrlY + 48, ctrlW, "Авто-пополнение хотбара: " + boolText(hotbarAutoRefill), () -> hotbarAutoRefill = !hotbarAutoRefill);
}
case LITEMATICA_HUD -> {
addToggleButton(ctrlX, ctrlY, ctrlW, "Сканер шалкеров: " + boolText(shulkerScanner), () -> shulkerScanner = !shulkerScanner);
addToggleButton(ctrlX, ctrlY + 24, ctrlW, "Разрез по слоям: " + boolText(layerSliceMode), () -> layerSliceMode = !layerSliceMode);
}
case VISUAL_RADAR -> {
addToggleButton(ctrlX, ctrlY, ctrlW, "Мини-карта-радар (HUD): " + boolText(radarHudEnabled), () -> radarHudEnabled = !radarHudEnabled);
addToggleButton(ctrlX, ctrlY + 24, ctrlW, "Звуковой радар: " + boolText(soundRadarEnabled), () -> soundRadarEnabled = !soundRadarEnabled);
addToggleButton(ctrlX, ctrlY + 48, ctrlW, "Нить Ариадны: " + boolText(ariadneThread), () -> ariadneThread = !ariadneThread);
addToggleButton(ctrlX, ctrlY + 72, ctrlW, "Шестое чувство: " + boolText(sixthSense), () -> sixthSense = !sixthSense);
}
case CHESTS_VILLAGERS -> {
addToggleButton(ctrlX, ctrlY, ctrlW, "Память сундуков: " + boolText(chestMemory), () -> chestMemory = !chestMemory);
addToggleButton(ctrlX, ctrlY + 24, ctrlW, "Умный сортировщик: " + boolText(smartSorter), () -> smartSorter = !smartSorter);
}
case COMBAT_PVP -> {
addToggleButton(ctrlX, ctrlY, ctrlW, "Траектории снарядов: " + boolText(trajectoriesEnabled), () -> trajectoriesEnabled = !trajectoriesEnabled);
addToggleButton(ctrlX, ctrlY + 24, ctrlW, "Сканер тотемов и оружия: " + boolText(totemScanner), () -> totemScanner = !totemScanner);
}
case MUSIC_AFK -> {
addToggleButton(ctrlX, ctrlY, ctrlW, "Spotify Connect HUD: " + boolText(spotifyHud), () -> spotifyHud = !spotifyHud);
addToggleButton(ctrlX, ctrlY + 24, ctrlW, "Экстренный выход при тревоге: " + boolText(afkPanicExit), () -> afkPanicExit = !afkPanicExit);
addToggleButton(ctrlX, ctrlY + 52, ctrlW, afkGameRunning ? "Стоп AFK-игры [ESC]" : "Запустить MC-Раннер (A / D)", () -> {
afkGameRunning = !afkGameRunning;
if (afkGameRunning) {
afkScore = 0;
afkPlayerLane = 1;
afkObstacles.clear();
}
});
}
case CUSTOM_VISUAL -> {
addToggleButton(ctrlX, ctrlY, ctrlW, "Тема меню: " + currentTheme.title, () -> {
MenuTheme[] themes = MenuTheme.values();
currentTheme = themes[(currentTheme.ordinal() + 1) % themes.length];
});
addToggleButton(ctrlX, ctrlY + 24, ctrlW, "Крылья: " + WINGS_OPTIONS.get(wingsIndex), () -> {
wingsIndex = (wingsIndex + 1) % WINGS_OPTIONS.size();
});
addToggleButton(ctrlX, ctrlY + 48, ctrlW, "Рога: " + HORNS_OPTIONS.get(hornsIndex), () -> {
hornsIndex = (hornsIndex + 1) % HORNS_OPTIONS.size();
});
addToggleButton(ctrlX, ctrlY + 72, ctrlW, "Шляпка: " + HATS_OPTIONS.get(hatsIndex), () -> {
hatsIndex = (hatsIndex + 1) % HATS_OPTIONS.size();
});
addToggleButton(ctrlX, ctrlY + 96, ctrlW, "Одежда: " + OUTFIT_OPTIONS.get(outfitIndex), () -> {
outfitIndex = (outfitIndex + 1) % OUTFIT_OPTIONS.size();
});
addToggleButton(ctrlX, ctrlY + 120, ctrlW, "Прицел: " + CROSSHAIR_OPTIONS.get(crosshairIndex), () -> {
crosshairIndex = (crosshairIndex + 1) % CROSSHAIR_OPTIONS.size();
});
}
}

// Кнопка закрытия в правом верхнем углу окна
this.addRenderableWidget(Button.builder(Component.literal("X"), b -> this.onClose())
.bounds(this.winX + this.winW - 24, this.winY + 6, 18, 18).build());
}

private void addToggleButton(int x, int y, int w, String label, Runnable action) {
this.addRenderableWidget(Button.builder(Component.literal(label), b -> {
action.run();
this.rebuildWidgets();
}).bounds(x, y, w, 20).build());
}

private static String boolText(boolean value) {
return value ? "ВКЛ" : "ВЫКЛ";
}

@Override
public void tick() {
super.tick();
if (!afkGameRunning) {
return;
}
afkTickCounter++;
if (afkTickCounter % 8 == 0) {
afkScore++;
if (afkScore > afkBestScore) {
afkBestScore = afkScore;
}
if (random.nextInt(3) == 0) {
afkObstacles.add(new int[]{random.nextInt(3), 0});
}
}
if (afkTickCounter % 2 == 0) {
List<int[]> next = new ArrayList<>();
for (int[] obs : afkObstacles) {
obs[1] += 4;
if (obs[1] >= 74 && obs[1] <= 90 && obs[0] == afkPlayerLane) {
afkScore = 0;
afkObstacles.clear();
return;
}
if (obs[1] < 96) {
next.add(obs);
}
}
afkObstacles.clear();
afkObstacles.addAll(next);
}
}

@Override
public boolean keyPressed(KeyEvent event) {
if (afkGameRunning) {
if (event.key() == GLFW.GLFW_KEY_A || event.key() == GLFW.GLFW_KEY_LEFT) {
afkPlayerLane = Math.max(0, afkPlayerLane - 1);
return true;
}
if (event.key() == GLFW.GLFW_KEY_D || event.key() == GLFW.GLFW_KEY_RIGHT) {
afkPlayerLane = Math.min(2, afkPlayerLane + 1);
return true;
}
if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
afkGameRunning = false;
this.rebuildWidgets();
return true;
}
}
if (event.key() == GLFW.GLFW_KEY_P) {
this.onClose();
return true;
}
return super.keyPressed(event);
}

@Override
public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
// Затемняем фон без размытия, чтобы интерфейс оставался чётким
graphics.fill(0, 0, this.width, this.height, 0x9906090E);
}

@Override
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
// 1. Главное окно и панели (рисуем ДО кнопок)
graphics.fill(winX, winY, winX + winW, winY + winH, currentTheme.bgMain);
drawBorder(graphics, winX, winY, winW, winH, currentTheme.accent);

// Верхняя шапка
graphics.fill(winX + 1, winY + 1, winX + winW - 1, winY + 28, currentTheme.bgCard);
graphics.fill(winX + 1, winY + 28, winX + winW - 1, winY + 29, currentTheme.accent);

// Левая панель вкладок
graphics.fill(winX + 2, winY + 30, winX + sidebarW, winY + winH - 2, currentTheme.bgCard);

// Центральная карточка настроек
graphics.fill(contentX, winY + 34, contentX + contentW, winY + winH - 8, currentTheme.bgCard);
drawBorder(graphics, contentX, winY + 34, contentW, winH - 42, 0x44FFFFFF);

// Правая панель живого превью
int prevX = winX + winW - previewW - 6;
int prevY = winY + 34;
int prevH = winH - 42;
graphics.fill(prevX, prevY, prevX + previewW, prevY + prevH, currentTheme.bgCard);
drawBorder(graphics, prevX, prevY, previewW, prevH, currentTheme.secondary);

// 2. Рендерим интерактивные кнопки поверх карточек
super.render(graphics, mouseX, mouseY, partialTick);

// 3. Рендерим заголовки, живое превью и мини-игру поверх всего
graphics.drawString(this.font, "ASSISTANT VADIMKA | Fabric 1.21.11 [P]", winX + 10, winY + 10, currentTheme.accent);
graphics.drawString(this.font, activeTab.title, contentX + 8, winY + 42, currentTheme.secondary);

// Если запущена AFK-мини-игра в 8-й вкладке
if (activeTab == MenuTab.MUSIC_AFK && afkGameRunning) {
renderAfkMiniGameBox(graphics, contentX + 8, winY + 142, contentW - 16, 102);
}

// Живое превью персонажа и спутника Вадимки справа
renderLivePreviewPanel(graphics, prevX, prevY, previewW, prevH);
}

private void renderAfkMiniGameBox(GuiGraphics graphics, int gx, int gy, int gw, int gh) {
graphics.fill(gx, gy, gx + gw, gy + gh, 0xFF0A0F14);
drawBorder(graphics, gx, gy, gw, gh, currentTheme.accent);
graphics.drawString(this.font, "MC-РАННЕР | Счёт: " + afkScore + " Рекорд: " + afkBestScore, gx + 6, gy + 4, 0xFFFDE68A);

int laneW = (gw - 20) / 3;
for (int lane = 0; lane < 3; lane++) {
int lx = gx + 10 + lane * laneW;
graphics.fill(lx + 2, gy + 16, lx + laneW - 2, gy + gh - 6, 0x22FFFFFF);
}

// Препятствия (Криперы)
for (int[] obs : afkObstacles) {
int ox = gx + 10 + obs[0] * laneW + laneW / 2 - 6;
int oy = gy + 16 + obs[1];
graphics.fill(ox, oy, ox + 12, oy + 12, 0xFF22C55E);
graphics.fill(ox + 2, oy + 3, ox + 4, oy + 5, 0xFF000000);
graphics.fill(ox + 8, oy + 3, ox + 10, oy + 5, 0xFF000000);
}

// Игрок
int px = gx + 10 + afkPlayerLane * laneW + laneW / 2 - 6;
int py = gy + gh - 20;
graphics.fill(px, py, px + 12, py + 12, currentTheme.secondary);
}

private void renderLivePreviewPanel(GuiGraphics graphics, int px, int py, int pw, int ph) {
graphics.drawString(this.font, "ЖИВОЕ ПРЕВЬЮ", px + 8, py + 8, currentTheme.accent);

int stageX = px + 8;
int stageY = py + 22;
int stageW = pw - 16;
int stageH = 126;
graphics.fill(stageX, stageY, stageX + stageW, stageY + stageH, 0xFF090E14);
drawBorder(graphics, stageX, stageY, stageW, stageH, 0x33FFFFFF);

// Платформа под персонажами
graphics.fill(stageX + 10, stageY + stageH - 16, stageX + stageW - 10, stageY + stageH - 10, 0xFF1E293B);

// --- Отрисовка стилизованного Игрока слева на сцене ---
int playerCenterX = stageX + 42;
int playerFeetY = stageY + stageH - 16;

// Крылья (если включены)
if (wingsIndex > 0) {
int wingColor = wingsIndex == 1 ? 0xFFE0F2FE : (wingsIndex == 2 ? 0xFFDC2626 : 0xFF22D3EE);
graphics.fill(playerCenterX - 24, playerFeetY - 56, playerCenterX - 8, playerFeetY - 30, wingColor);
graphics.fill(playerCenterX + 8, playerFeetY - 56, playerCenterX + 24, playerFeetY - 30, wingColor);
}

// Ноги и тело игрока (цвет зависит от выбранной одежды)
int outfitColor = switch (outfitIndex) {
case 1 -> 0xFF0284C7; // Кибер-плащ
case 2 -> 0xFF334155; // Тень Ассасина
case 3 -> 0xFF059669; // Изумрудный сет
default -> 0xFF3B82F6;
};
graphics.fill(playerCenterX - 7, playerFeetY - 26, playerCenterX - 1, playerFeetY, 0xFF1E3A8A);
graphics.fill(playerCenterX + 1, playerFeetY - 26, playerCenterX + 7, playerFeetY, 0xFF1E3A8A);
graphics.fill(playerCenterX - 9, playerFeetY - 54, playerCenterX + 9, playerFeetY - 26, outfitColor);

// Голова игрока
graphics.fill(playerCenterX - 9, playerFeetY - 72, playerCenterX + 9, playerFeetY - 54, 0xFFFDBA74);
graphics.fill(playerCenterX - 6, playerFeetY - 64, playerCenterX - 3, playerFeetY - 62, 0xFF0F172A);
graphics.fill(playerCenterX + 3, playerFeetY - 64, playerCenterX + 6, playerFeetY - 62, 0xFF0F172A);

// Рога (если включены)
if (hornsIndex > 0) {
int hornColor = hornsIndex == 1 ? 0xFFEF4444 : (hornsIndex == 2 ? 0xFFD97706 : 0xFF38BDF8);
graphics.fill(playerCenterX - 11, playerFeetY - 80, playerCenterX - 7, playerFeetY - 72, hornColor);
graphics.fill(playerCenterX + 7, playerFeetY - 80, playerCenterX + 11, playerFeetY - 72, hornColor);
}

// Шляпка (если включена)
if (hatsIndex == 1) { // Корона
graphics.fill(playerCenterX - 8, playerFeetY - 77, playerCenterX + 8, playerFeetY - 72, 0xFFFACC15);
} else if (hatsIndex == 2) { // Ушки Крыски
graphics.fill(playerCenterX - 10, playerFeetY - 78, playerCenterX - 4, playerFeetY - 72, 0xFFF472B6);
graphics.fill(playerCenterX + 4, playerFeetY - 78, playerCenterX + 10, playerFeetY - 72, 0xFFF472B6);
} else if (hatsIndex == 3) { // Цилиндр
graphics.fill(playerCenterX - 10, playerFeetY - 75, playerCenterX + 10, playerFeetY - 72, 0xFF0F172A);
graphics.fill(playerCenterX - 6, playerFeetY - 84, playerCenterX + 6, playerFeetY - 75, 0xFF1E293B);
}

// --- Отрисовка спутника Вадимки справа на сцене ---
int petX = stageX + 98;
int petY = playerFeetY - 14;
if (currentForm == VadimkaForm.RAT) {
// Крыска: серое тельце, розовые ушки, носик и хвостик
graphics.fill(petX - 10, petY - 12, petX + 10, petY + 4, currentForm.primaryColor);
graphics.fill(petX - 9, petY - 17, petX - 3, petY - 12, currentForm.accentColor);
graphics.fill(petX + 3, petY - 17, petX + 9, petY - 12, currentForm.accentColor);
graphics.fill(petX - 2, petY - 4, petX + 2, petY - 1, currentForm.accentColor);
graphics.fill(petX + 10, petY, petX + 20, petY + 2, currentForm.accentColor);
} else {
// Остальные облики Вадимки (Allay, Breeze, Warden, Axolotl, Player)
graphics.fill(petX - 9, petY - 22, petX + 9, petY + 2, currentForm.primaryColor);
graphics.fill(petX - 14, petY - 16, petX - 9, petY - 6, currentForm.accentColor);
graphics.fill(petX + 9, petY - 16, petX + 14, petY - 6, currentForm.accentColor);
graphics.fill(petX - 5, petY - 15, petX - 2, petY - 12, 0xFFFFFFFF);
graphics.fill(petX + 2, petY - 15, petX + 5, petY - 12, 0xFFFFFFFF);
}

// Подписи параметров под сценой превью
int infoY = stageY + stageH + 8;
graphics.drawString(this.font, "Вадимка:", px + 8, infoY, 0xFF94A3B8);
graphics.drawString(this.font, currentForm.title, px + 8, infoY + 11, currentForm.accentColor);

graphics.drawString(this.font, "Гардероб:", px + 8, infoY + 27, 0xFF94A3B8);
graphics.drawString(this.font, "Крылья: " + WINGS_OPTIONS.get(wingsIndex), px + 8, infoY + 38, 0xFFE2E8F0);
graphics.drawString(this.font, "Рога: " + HORNS_OPTIONS.get(hornsIndex), px + 8, infoY + 49, 0xFFE2E8F0);
graphics.drawString(this.font, "Шляпка: " + HATS_OPTIONS.get(hatsIndex), px + 8, infoY + 60, 0xFFE2E8F0);
graphics.drawString(this.font, "Одежда: " + OUTFIT_OPTIONS.get(outfitIndex), px + 8, infoY + 71, 0xFFE2E8F0);
}
}
}
