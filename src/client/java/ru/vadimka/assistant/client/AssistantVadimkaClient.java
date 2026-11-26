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
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
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
PLAYER("Player (Мини-игрок)", "Player", EntityType.ALLAY, 0xFF38BDF8, 0xFFFDE68A),
ALLAY("Allay (Спешик)", "Allay", EntityType.ALLAY, 0xFF22D3EE, 0xFFA5F3FC),
BREEZE("Breeze (Вихрь)", "Breeze", EntityType.BREEZE, 0xFF818CF8, 0xFFC7D2FE),
WARDEN("Warden (Хранитель)", "Warden", EntityType.WARDEN, 0xFF0D9488, 0xFF2DD4BF),
AXOLOTL("Blue Axolotl (Аксолотль)", "Axolotl", EntityType.AXOLOTL, 0xFF60A5FA, 0xFFF472B6),
RAT("Rat (Крыска)", "Крыска", EntityType.SILVERFISH, 0xFF94A3B8, 0xFFF9A8D4);

public final String title;
public final String shortName;
public final EntityType<? extends Mob> entityType;
public final int primaryColor;
public final int accentColor;

VadimkaForm(String title, String shortName, EntityType<? extends Mob> entityType, int primaryColor, int accentColor) {
this.title = title;
this.shortName = shortName;
this.entityType = entityType;
this.primaryColor = primaryColor;
this.accentColor = accentColor;
}
}

public enum MinionRole {
COLLECTOR("Сборщик", "§a[Сборщик]", 0xFF4ADE80),
BUILDER("Строитель-подручный", "§e[Строитель]", 0xFFFACC15),
SCOUT("Разведчик / Факельщик", "§b[Разведчик]", 0xFF38BDF8),
SUPPLIER("Снабженец", "§d[Снабженец]", 0xFFF472B6);

public final String title;
public final String tag;
public final int color;

MinionRole(String title, String tag, int color) {
this.title = title;
this.tag = tag;
this.color = color;
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
CYBER_EMERALD("Cyber Emerald", 0xEB0A1016, 0xFF111B24, 0xFF10B981, 0xFF38BDF8),
NETHER_CRIMSON("Nether Crimson", 0xEB150A0E, 0xFF231017, 0xFFF43F5E, 0xFFFBBF24),
AMETHYST_VOID("Amethyst Void", 0xEB100B1C, 0xFF1C1330, 0xFFA855F7, 0xFFF472B6),
CLASSIC_DARK("Classic Dark", 0xEB101216, 0xFF1B1E24, 0xFFE2E8F0, 0xFF94A3B8);

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

// Состояние Вадимки и 3D-сущностей в мире
public static VadimkaForm currentForm = VadimkaForm.RAT;
public static boolean companionVisibleInWorld = true;
public static boolean quietMode = false;
public static boolean randomChaosMode = false;
public static boolean voiceCommentator = true;
public static final Map<MinionRole, Boolean> activeMinions = new EnumMap<>(MinionRole.class);

private static Mob companionEntity = null;
private static VadimkaForm spawnedForm = null;
private static final Map<MinionRole, Mob> minionEntities = new EnumMap<>(MinionRole.class);
private static int worldAnimTick = 0;

// Настройки остальных модулей
public static boolean styleProfiler = true;
public static boolean safeAutopilot = false;
public static boolean litematicaAutoBuild = false;
public static boolean smartRestoration = true;
public static boolean hotbarAutoRefill = true;
public static boolean shulkerScanner = true;
public static boolean layerSliceMode = false;

// Радар и визуал
public static boolean radarHudEnabled = true;
public static boolean radarRightSide = false;
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
public static int hatsIndex = 2;
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
if (client.player == null || client.level == null) {
cleanupWorldEntities();
return;
}
while (openMenuKey.consumeClick()) {
if (client.screen instanceof AssistantVadimkaScreen) {
client.setScreen(null);
} else {
client.setScreen(new AssistantVadimkaScreen(MenuTab.VADIMKA));
}
}
updateCompanionAndMinionsInWorld(client);
});

HudRenderCallback.EVENT.register((graphics, tickCounter) -> renderRadarHud(graphics));
}

private static void cleanupWorldEntities() {
if (companionEntity != null) {
companionEntity.discard();
companionEntity = null;
spawnedForm = null;
}
for (Mob minion : minionEntities.values()) {
if (minion != null) {
minion.discard();
}
}
minionEntities.clear();
}

private static void updateCompanionAndMinionsInWorld(Minecraft mc) {
ClientLevel level = mc.level;
Player player = mc.player;
if (level == null || player == null) {
return;
}
worldAnimTick++;

// 1. Обновляем 3D-спутника Вадимку в мире
if (!companionVisibleInWorld) {
if (companionEntity != null) {
companionEntity.discard();
companionEntity = null;
spawnedForm = null;
}
} else {
if (companionEntity == null || companionEntity.isRemoved() || companionEntity.level() != level || spawnedForm != currentForm) {
if (companionEntity != null) {
companionEntity.discard();
}
Mob created = currentForm.entityType.create(level, EntitySpawnReason.LOAD);
if (created != null) {
created.setId(-77001);
created.setNoGravity(true);
created.setNoAi(true);
created.setSilent(true);
created.setInvulnerable(true);
created.setCustomName(Component.literal("§b✦ Вадимка §7[" + currentForm.shortName + "]"));
created.setCustomNameVisible(true);
double startX = player.getX() + 1.2;
double startY = player.getY() + 1.1;
double startZ = player.getZ() + 1.2;
created.setPos(startX, startY, startZ);
level.addEntity(created);
companionEntity = created;
spawnedForm = currentForm;
}
}

if (companionEntity != null) {
double yawRad = Math.toRadians(player.getYRot());
double sideOffset = 1.15;
double backOffset = 0.65;
double targetX = player.getX() - Math.cos(yawRad) * sideOffset + Math.sin(yawRad) * backOffset;
double targetZ = player.getZ() - Math.sin(yawRad) * sideOffset - Math.cos(yawRad) * backOffset;
double baseHeight = (currentForm == VadimkaForm.RAT) ? 0.15 : 1.15;
double bob = Math.sin(worldAnimTick * 0.14) * 0.12;
double targetY = player.getY() + baseHeight + bob;

companionEntity.xo = companionEntity.getX();
companionEntity.yo = companionEntity.getY();
companionEntity.zo = companionEntity.getZ();

double nextX = Mth.lerp(0.22, companionEntity.getX(), targetX);
double nextY = Mth.lerp(0.22, companionEntity.getY(), targetY);
double nextZ = Mth.lerp(0.22, companionEntity.getZ(), targetZ);
companionEntity.setPos(nextX, nextY, nextZ);

float lookYaw = (float) (Mth.atan2(player.getZ() - nextZ, player.getX() - nextX) * (180.0 / Math.PI)) - 90.0F;
companionEntity.setYRot(lookYaw);
companionEntity.setYHeadRot(lookYaw);
companionEntity.setYBodyRot(lookYaw);

if (worldAnimTick % 12 == 0) {
level.addParticle(ParticleTypes.END_ROD, nextX, nextY + 0.35, nextZ, 0.0, 0.01, 0.0);
}
}
}

// 2. Обновляем 4 мини-миньонов вокруг игрока
MinionRole[] roles = MinionRole.values();
for (int i = 0; i < roles.length; i++) {
MinionRole role = roles[i];
boolean enabled = companionVisibleInWorld && Boolean.TRUE.equals(activeMinions.get(role));
Mob existing = minionEntities.get(role);

if (!enabled) {
if (existing != null) {
existing.discard();
minionEntities.remove(role);
}
continue;
}

if (existing == null || existing.isRemoved() || existing.level() != level) {
if (existing != null) {
existing.discard();
}
Mob minion = EntityType.ALLAY.create(level, EntitySpawnReason.LOAD);
if (minion != null) {
minion.setId(-77010 - i);
minion.setNoGravity(true);
minion.setNoAi(true);
minion.setSilent(true);
minion.setInvulnerable(true);
minion.setCustomName(Component.literal(role.tag));
minion.setCustomNameVisible(true);
minion.setPos(player.getX(), player.getY() + 1.5, player.getZ());
level.addEntity(minion);
minionEntities.put(role, minion);
existing = minion;
}
}

if (existing != null) {
double angle = (worldAnimTick * 0.04) + (i * (Math.PI / 2.0));
double radius = 2.05;
double tx = player.getX() + Math.cos(angle) * radius;
double tz = player.getZ() + Math.sin(angle) * radius;
double ty = player.getY() + 1.45 + Math.sin(worldAnimTick * 0.12 + i) * 0.16;

existing.xo = existing.getX();
existing.yo = existing.getY();
existing.zo = existing.getZ();
existing.setPos(Mth.lerp(0.2, existing.getX(), tx), Mth.lerp(0.2, existing.getY(), ty), Mth.lerp(0.2, existing.getZ(), tz));

float mYaw = (float) (Mth.atan2(player.getZ() - existing.getZ(), player.getX() - existing.getX()) * (180.0 / Math.PI)) - 90.0F;
existing.setYRot(mYaw);
existing.setYHeadRot(mYaw);
existing.setYBodyRot(mYaw);
}
}
}

private static void renderRadarHud(GuiGraphics graphics) {
if (!radarHudEnabled) {
return;
}
Minecraft mc = Minecraft.getInstance();
if (mc.player == null || mc.level == null || mc.options.hideGui) {
return;
}

int size = 86;
int x = radarRightSide ? (mc.getWindow().getGuiScaledWidth() - size - 10) : 10;
// Сдвинули вниз на y = 32, чтобы не перекрывать верхнюю строку FPS и XYZ!
int y = 32;
int centerX = x + size / 2;
int centerY = y + size / 2;

graphics.fill(x, y, x + size, y + size, currentTheme.bgMain);
drawBorder(graphics, x, y, size, size, currentTheme.accent);

graphics.fill(centerX, y + 4, centerX + 1, y + size - 4, 0x2AFFFFFF);
graphics.fill(x + 4, centerY, x + size - 4, centerY + 1, 0x2AFFFFFF);

double yawRad = Math.toRadians(mc.player.getYRot() + 180.0);
double cos = Math.cos(yawRad);
double sin = Math.sin(yawRad);
double scale = 0.7;
int maxOffset = (size / 2) - 6;

int northX = centerX + (int) (-sin * (maxOffset - 2));
int northY = centerY + (int) (-cos * (maxOffset - 2));
graphics.drawString(mc.font, "N", northX - 2, northY - 3, currentTheme.secondary);

for (Entity entity : mc.level.entitiesForRendering()) {
if (entity == mc.player || entity.getId() <= -77000) {
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

graphics.fill(centerX - 2, centerY - 2, centerX + 2, centerY + 2, 0xFFFFFFFF);

int infoY = y + size + 3;
graphics.fill(x, infoY, x + size, infoY + 23, currentTheme.bgMain);
drawBorder(graphics, x, infoY, size, 23, currentTheme.secondary);

String coords = (int) mc.player.getX() + " / " + (int) mc.player.getY() + " / " + (int) mc.player.getZ();
graphics.drawString(mc.font, coords, x + 4, infoY + 3, 0xFFE2E8F0);
graphics.drawString(mc.font, "Вадимка: " + currentForm.shortName, x + 4, infoY + 13, currentTheme.accent);
}

private static void drawBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
graphics.fill(x, y, x + w, y + 1, color);
graphics.fill(x, y + h - 1, x + w, y + h, color);
graphics.fill(x, y, x + 1, y + h, color);
graphics.fill(x + w - 1, y, x + w, y + h, color);
}

public static class AssistantVadimkaScreen extends Screen {
private MenuTab activeTab;

private static class SettingCard {
final int y;
final String title;
final String subtitle;
final boolean enabled;

SettingCard(int y, String title, String subtitle, boolean enabled) {
this.y = y;
this.title = title;
this.subtitle = subtitle;
this.enabled = enabled;
}
}

private final List<SettingCard> settingCards = new ArrayList<>();

// Мини-игра AFK
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
this.settingCards.clear();
this.winW = Math.min(this.width - 20, 560);
this.winH = Math.min(this.height - 20, 310);
this.winX = (this.width - this.winW) / 2;
this.winY = (this.height - this.winH) / 2;

this.sidebarW = 128;
this.previewW = 152;
this.contentX = this.winX + this.sidebarW + 8;
this.contentW = this.winW - this.sidebarW - this.previewW - 22;

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

int rowY = this.winY + 58;

switch (this.activeTab) {
case VADIMKA -> {
addCardRow(rowY, "Спавн 3D-помощника в мире", "Отображать Вадимку и миньонов", companionVisibleInWorld, companionVisibleInWorld ? "ВКЛ" : "ВЫКЛ", () -> companionVisibleInWorld = !companionVisibleInWorld);
rowY += 26;
addCardRow(rowY, "Облик Вадимки", currentForm.title, true, "Сменить", () -> {
VadimkaForm[] forms = VadimkaForm.values();
currentForm = forms[(currentForm.ordinal() + 1) % forms.length];
});
rowY += 26;
addCardRow(rowY, "Тихий / Активный режим", quietMode ? "Только помощь без шума" : "Живые эмоции и реакции", !quietMode, quietMode ? "ТИХИЙ" : "АКТИВ", () -> quietMode = !quietMode);
rowY += 26;
addCardRow(rowY, "Рандомная дичь и голос", "Шутки, комментарии и пасхалки", randomChaosMode, randomChaosMode ? "ВКЛ" : "ВЫКЛ", () -> randomChaosMode = !randomChaosMode);
rowY += 28;
for (MinionRole role : MinionRole.values()) {
boolean state = Boolean.TRUE.equals(activeMinions.get(role));
addCardRow(rowY, "Миньон: " + role.title, "Летающий помощник в мире", state, state ? "ВКЛ" : "ВЫКЛ", () -> {
activeMinions.put(role, !Boolean.TRUE.equals(activeMinions.get(role)));
});
rowY += 24;
}
}
case AUTOPILOT -> {
addCardRow(rowY, "Профайлер стиля игры", "Запоминает привычки и темп", styleProfiler, styleProfiler ? "ВКЛ" : "ВЫКЛ", () -> styleProfiler = !styleProfiler);
rowY += 28;
addCardRow(rowY, "Безопасный автопилот", "Перехват и возврат управления", safeAutopilot, safeAutopilot ? "ВКЛ" : "ВЫКЛ", () -> safeAutopilot = !safeAutopilot);
}
case AUTO_BUILD -> {
addCardRow(rowY, "Авто-постройка по схеме", "Помощь при укладке блоков", litematicaAutoBuild, litematicaAutoBuild ? "ВКЛ" : "ВЫКЛ", () -> litematicaAutoBuild = !litematicaAutoBuild);
rowY += 28;
addCardRow(rowY, "Умная реставрация", "Подсветка сломанных блоков", smartRestoration, smartRestoration ? "ВКЛ" : "ВЫКЛ", () -> smartRestoration = !smartRestoration);
rowY += 28;
addCardRow(rowY, "Авто-пополнение хотбара", "Подмена закончившихся стаков", hotbarAutoRefill, hotbarAutoRefill ? "ВКЛ" : "ВЫКЛ", () -> hotbarAutoRefill = !hotbarAutoRefill);
}
case LITEMATICA_HUD -> {
addCardRow(rowY, "Сканер шалкеров", "Подсчёт ресурсов в рюкзаке", shulkerScanner, shulkerScanner ? "ВКЛ" : "ВЫКЛ", () -> shulkerScanner = !shulkerScanner);
rowY += 28;
addCardRow(rowY, "Разрез по слоям (Y)", "Подсветка дефицита по уровням", layerSliceMode, layerSliceMode ? "ВКЛ" : "ВЫКЛ", () -> layerSliceMode = !layerSliceMode);
}
case VISUAL_RADAR -> {
addCardRow(rowY, "Мини-карта-радар (HUD)", "Игроки, мобы и стороны света", radarHudEnabled, radarHudEnabled ? "ВКЛ" : "ВЫКЛ", () -> radarHudEnabled = !radarHudEnabled);
rowY += 28;
addCardRow(rowY, "Позиция радара на экране", radarRightSide ? "Справа сверху" : "Слева сверху (под FPS)", true, radarRightSide ? "ПРАВО" : "ЛЕВО", () -> radarRightSide = !radarRightSide);
rowY += 28;
addCardRow(rowY, "Звуковой радар", "Направление шагов и взрывов", soundRadarEnabled, soundRadarEnabled ? "ВКЛ" : "ВЫКЛ", () -> soundRadarEnabled = !soundRadarEnabled);
rowY += 28;
addCardRow(rowY, "Нить Ариадны", "Маршрут обратно наверх", ariadneThread, ariadneThread ? "ВКЛ" : "ВЫКЛ", () -> ariadneThread = !ariadneThread);
rowY += 28;
addCardRow(rowY, "Шестое чувство", "Предупреждение об угрозах", sixthSense, sixthSense ? "ВКЛ" : "ВЫКЛ", () -> sixthSense = !sixthSense);
}
case CHESTS_VILLAGERS -> {
addCardRow(rowY, "Память сундуков", "Запоминает лут в контейнерах", chestMemory, chestMemory ? "ВКЛ" : "ВЫКЛ", () -> chestMemory = !chestMemory);
rowY += 28;
addCardRow(rowY, "Умный сортировщик", "Раскладка предметов по клику", smartSorter, smartSorter ? "ВКЛ" : "ВЫКЛ", () -> smartSorter = !smartSorter);
}
case COMBAT_PVP -> {
addCardRow(rowY, "Траектории снарядов", "Дуга лука, жемчуга и зелий", trajectoriesEnabled, trajectoriesEnabled ? "ВКЛ" : "ВЫКЛ", () -> trajectoriesEnabled = !trajectoriesEnabled);
rowY += 28;
addCardRow(rowY, "Сканер тотемов и оружия", "Индикатор прочности и тотема", totemScanner, totemScanner ? "ВКЛ" : "ВЫКЛ", () -> totemScanner = !totemScanner);
}
case MUSIC_AFK -> {
addCardRow(rowY, "Spotify Connect HUD", "Мини-плеер текущего трека", spotifyHud, spotifyHud ? "ВКЛ" : "ВЫКЛ", () -> spotifyHud = !spotifyHud);
rowY += 28;
addCardRow(rowY, "Экстренный выход при тревоге", "Закрытие игры при уроне/враге", afkPanicExit, afkPanicExit ? "ВКЛ" : "ВЫКЛ", () -> afkPanicExit = !afkPanicExit);
rowY += 28;
addCardRow(rowY, "MC-Раннер (A / D)", "Уворот от криперов в 3 полосах", afkGameRunning, afkGameRunning ? "СТОП" : "ИГРАТЬ", () -> {
afkGameRunning = !afkGameRunning;
if (afkGameRunning) {
afkScore = 0;
afkPlayerLane = 1;
afkObstacles.clear();
}
});
}
case CUSTOM_VISUAL -> {
addCardRow(rowY, "Тема интерфейса [P]", currentTheme.title, true, "Сменить", () -> {
MenuTheme[] themes = MenuTheme.values();
currentTheme = themes[(currentTheme.ordinal() + 1) % themes.length];
});
rowY += 26;
addCardRow(rowY, "Крылья персонажа", WINGS_OPTIONS.get(wingsIndex), wingsIndex > 0, WINGS_OPTIONS.get(wingsIndex), () -> {
wingsIndex = (wingsIndex + 1) % WINGS_OPTIONS.size();
});
rowY += 26;
addCardRow(rowY, "Рога персонажа", HORNS_OPTIONS.get(hornsIndex), hornsIndex > 0, HORNS_OPTIONS.get(hornsIndex), () -> {
hornsIndex = (hornsIndex + 1) % HORNS_OPTIONS.size();
});
rowY += 26;
addCardRow(rowY, "Шляпка / Ушки", HATS_OPTIONS.get(hatsIndex), hatsIndex > 0, HATS_OPTIONS.get(hatsIndex), () -> {
hatsIndex = (hatsIndex + 1) % HATS_OPTIONS.size();
});
rowY += 26;
addCardRow(rowY, "Одежда и плащ", OUTFIT_OPTIONS.get(outfitIndex), outfitIndex > 0, OUTFIT_OPTIONS.get(outfitIndex), () -> {
outfitIndex = (outfitIndex + 1) % OUTFIT_OPTIONS.size();
});
rowY += 26;
addCardRow(rowY, "Кастом-прицел", CROSSHAIR_OPTIONS.get(crosshairIndex), true, CROSSHAIR_OPTIONS.get(crosshairIndex), () -> {
crosshairIndex = (crosshairIndex + 1) % CROSSHAIR_OPTIONS.size();
});
}
}

this.addRenderableWidget(Button.builder(Component.literal("X"), b -> this.onClose())
.bounds(this.winX + this.winW - 24, this.winY + 6, 18, 18).build());
}

private void addCardRow(int y, String title, String subtitle, boolean enabled, String btnText, Runnable action) {
this.settingCards.add(new SettingCard(y, title, subtitle, enabled));
int btnW = 74;
int btnX = this.contentX + this.contentW - btnW - 8;
this.addRenderableWidget(Button.builder(Component.literal(btnText), b -> {
action.run();
this.rebuildWidgets();
}).bounds(btnX, y + 2, btnW, 18).build());
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
graphics.fill(0, 0, this.width, this.height, 0x9906090E);
}

@Override
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
// Главное окно
graphics.fill(winX, winY, winX + winW, winY + winH, currentTheme.bgMain);
drawBorder(graphics, winX, winY, winW, winH, currentTheme.accent);

// Шапка
graphics.fill(winX + 1, winY + 1, winX + winW - 1, winY + 28, currentTheme.bgCard);
graphics.fill(winX + 1, winY + 28, winX + winW - 1, winY + 29, currentTheme.accent);

// Левая панель
graphics.fill(winX + 2, winY + 30, winX + sidebarW, winY + winH - 2, currentTheme.bgCard);

// Центральная карточка
graphics.fill(contentX, winY + 34, contentX + contentW, winY + winH - 8, currentTheme.bgCard);
drawBorder(graphics, contentX, winY + 34, contentW, winH - 42, 0x33FFFFFF);

// Отрисовка стильных карточек настроек внутри центральной панели
for (SettingCard card : settingCards) {
int cx = contentX + 6;
int cw = contentW - 12;
int ch = 22;
graphics.fill(cx, card.y, cx + cw, card.y + ch, 0x880B1017);
int stripColor = card.enabled ? currentTheme.accent : 0xFF475569;
graphics.fill(cx, card.y, cx + 3, card.y + ch, stripColor);
drawBorder(graphics, cx, card.y, cw, ch, 0x22FFFFFF);

graphics.drawString(this.font, card.title, cx + 8, card.y + 3, 0xFFF1F5F9);
graphics.drawString(this.font, card.subtitle, cx + 8, card.y + 12, 0xFF94A3B8);
}

// Правая панель превью
int prevX = winX + winW - previewW - 6;
int prevY = winY + 34;
int prevH = winH - 42;
graphics.fill(prevX, prevY, prevX + previewW, prevY + prevH, currentTheme.bgCard);
drawBorder(graphics, prevX, prevY, previewW, prevH, currentTheme.secondary);

// Кнопки поверх карточек
super.render(graphics, mouseX, mouseY, partialTick);

// Заголовки
graphics.drawString(this.font, "ASSISTANT VADIMKA | Fabric 1.21.11 [P]", winX + 10, winY + 10, currentTheme.accent);
graphics.drawString(this.font, activeTab.title, contentX + 8, winY + 42, currentTheme.secondary);

if (activeTab == MenuTab.MUSIC_AFK && afkGameRunning) {
renderAfkMiniGameBox(graphics, contentX + 8, winY + 148, contentW - 16, 102);
}

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

for (int[] obs : afkObstacles) {
int ox = gx + 10 + obs[0] * laneW + laneW / 2 - 6;
int oy = gy + 16 + obs[1];
graphics.fill(ox, oy, ox + 12, oy + 12, 0xFF22C55E);
graphics.fill(ox + 2, oy + 3, ox + 4, oy + 5, 0xFF000000);
graphics.fill(ox + 8, oy + 3, ox + 10, oy + 5, 0xFF000000);
}

int px = gx + 10 + afkPlayerLane * laneW + laneW / 2 - 6;
int py = gy + gh - 20;
graphics.fill(px, py, px + 12, py + 12, currentTheme.secondary);
}

private void renderLivePreviewPanel(GuiGraphics graphics, int px, int py, int pw, int ph) {
graphics.drawString(this.font, "ЖИВОЕ ПРЕВЬЮ 3D", px + 8, py + 8, currentTheme.accent);

int stageX = px + 8;
int stageY = py + 22;
int stageW = pw - 16;
int stageH = 132;
graphics.fill(stageX, stageY, stageX + stageW, stageY + stageH, 0xFF070B10);
drawBorder(graphics, stageX, stageY, stageW, stageH, 0x33FFFFFF);

// Неоновый подиум с перспективой
int podY = stageY + stageH - 18;
graphics.fill(stageX + 12, podY, stageX + stageW - 12, podY + 4, currentTheme.accent);
graphics.fill(stageX + 18, podY + 4, stageX + stageW - 18, podY + 9, 0xFF1E293B);

// Плавная анимация дыхания и парения
double time = System.currentTimeMillis() / 280.0;
int breathOffset = (int) Math.round(Math.sin(time));
int floatOffset = (int) Math.round(Math.sin(time * 1.4) * 3.0);

int pX = stageX + 42;
int pY = podY;

// 1. Крылья с тенями и перьями
if (wingsIndex > 0) {
int wMain = wingsIndex == 1 ? 0xFFF8FAFC : (wingsIndex == 2 ? 0xFFDC2626 : 0xFF06B6D4);
int wEdge = wingsIndex == 1 ? 0xFFBAE6FD : (wingsIndex == 2 ? 0xFF7F1D1D : 0xFF67E8F9);
// Левое крыло
graphics.fill(pX - 28, pY - 62 + breathOffset, pX - 10, pY - 34 + breathOffset, wEdge);
graphics.fill(pX - 26, pY - 60 + breathOffset, pX - 11, pY - 38 + breathOffset, wMain);
graphics.fill(pX - 32, pY - 56 + breathOffset, pX - 26, pY - 44 + breathOffset, wEdge);
// Правое крыло
graphics.fill(pX + 10, pY - 62 + breathOffset, pX + 28, pY - 34 + breathOffset, wEdge);
graphics.fill(pX + 11, pY - 60 + breathOffset, pX + 26, pY - 38 + breathOffset, wMain);
graphics.fill(pX + 26, pY - 56 + breathOffset, pX + 32, pY - 44 + breathOffset, wEdge);
}

// 2. Ноги и ботинки персонажа
graphics.fill(pX - 8, pY - 28, pX - 1, pY - 4, 0xFF1E293B);
graphics.fill(pX + 1, pY - 28, pX + 8, pY - 4, 0xFF0F172A);
graphics.fill(pX - 8, pY - 4, pX - 1, pY, 0xFF475569);
graphics.fill(pX + 1, pY - 4, pX + 8, pY, 0xFF334155);

// 3. Торс и руки (с учётом выбранной одежды)
int bodyMain = switch (outfitIndex) {
case 1 -> 0xFF0284C7;
case 2 -> 0xFF1E293B;
case 3 -> 0xFF059669;
default -> 0xFF2563EB;
};
int bodyShade = switch (outfitIndex) {
case 1 -> 0xFF38BDF8;
case 2 -> 0xFFF43F5E;
case 3 -> 0xFF34D399;
default -> 0xFF60A5FA;
};
// Руки
graphics.fill(pX - 14, pY - 54 + breathOffset, pX - 9, pY - 28 + breathOffset, bodyMain);
graphics.fill(pX + 9, pY - 54 + breathOffset, pX + 14, pY - 28 + breathOffset, bodyMain);
// Торс + неоновая полоса
graphics.fill(pX - 9, pY - 55 + breathOffset, pX + 9, pY - 28, bodyMain);
graphics.fill(pX - 2, pY - 55 + breathOffset, pX + 2, pY - 28, bodyShade);

// 4. Голова персонажа (причёска, глаза, рот)
int headY = pY - 75 + breathOffset;
graphics.fill(pX - 10, headY, pX + 10, headY + 20, 0xFFFDBA74);
// Тёмные волосы как у скина игрока
graphics.fill(pX - 10, headY, pX + 10, headY + 6, 0xFF18181B);
graphics.fill(pX - 10, headY + 6, pX - 7, headY + 11, 0xFF18181B);
graphics.fill(pX + 7, headY + 6, pX + 10, headY + 11, 0xFF18181B);
// Глаза
graphics.fill(pX - 7, headY + 10, pX - 3, headY + 13, 0xFFFFFFFF);
graphics.fill(pX - 6, headY + 10, pX - 4, headY + 13, 0xFF7F1D1D);
graphics.fill(pX + 3, headY + 10, pX + 7, headY + 13, 0xFFFFFFFF);
graphics.fill(pX + 4, headY + 10, pX + 6, headY + 13, 0xFF7F1D1D);

// 5. Рога
if (hornsIndex > 0) {
int hColor = hornsIndex == 1 ? 0xFFEF4444 : (hornsIndex == 2 ? 0xFFD97706 : 0xFF38BDF8);
graphics.fill(pX - 12, headY - 7, pX - 8, headY, hColor);
graphics.fill(pX - 14, headY - 10, pX - 10, headY - 6, hColor);
graphics.fill(pX + 8, headY - 7, pX + 12, headY, hColor);
graphics.fill(pX + 10, headY - 10, pX + 14, headY - 6, hColor);
}

// 6. Шляпка / Ушки
if (hatsIndex == 1) {
graphics.fill(pX - 8, headY - 5, pX + 8, headY, 0xFFFACC15);
graphics.fill(pX - 8, headY - 8, pX - 5, headY - 5, 0xFFFACC15);
graphics.fill(pX - 1, headY - 9, pX + 2, headY - 5, 0xFFFACC15);
graphics.fill(pX + 5, headY - 8, pX + 8, headY - 5, 0xFFFACC15);
} else if (hatsIndex == 2) {
graphics.fill(pX - 11, headY - 6, pX - 4, headY, 0xFF94A3B8);
graphics.fill(pX - 9, headY - 4, pX - 6, headY, 0xFFF472B6);
graphics.fill(pX + 4, headY - 6, pX + 11, headY, 0xFF94A3B8);
graphics.fill(pX + 6, headY - 4, pX + 9, headY, 0xFFF472B6);
} else if (hatsIndex == 3) {
graphics.fill(pX - 11, headY - 3, pX + 11, headY, 0xFF0F172A);
graphics.fill(pX - 7, headY - 13, pX + 7, headY - 3, 0xFF1E293B);
graphics.fill(pX - 7, headY - 5, pX + 7, headY - 3, currentTheme.accent);
}

// --- Детализированный спутник Вадимка справа на сцене ---
int petX = stageX + 104;
int petY = podY - 16 + floatOffset;

if (currentForm == VadimkaForm.RAT) {
int ratY = podY - 4;
// Тельце Крыски, ушки, глазки, усики и длинный хвостик
graphics.fill(petX - 12, ratY - 12, petX + 10, ratY + 4, 0xFF94A3B8);
graphics.fill(petX - 14, ratY - 8, petX - 10, ratY + 2, 0xFFCBD5E1);
graphics.fill(petX - 10, ratY - 18, petX - 3, ratY - 12, 0xFFF472B6);
graphics.fill(petX + 1, ratY - 18, petX + 8, ratY - 12, 0xFFF472B6);
graphics.fill(petX - 11, ratY - 8, petX - 8, ratY - 5, 0xFF0F172A);
graphics.fill(petX - 15, ratY - 4, petX - 13, ratY - 2, 0xFFF472B6);
graphics.fill(petX + 10, ratY, petX + 18, ratY + 2, 0xFFF9A8D4);
graphics.fill(petX + 18, ratY - 3, petX + 23, ratY, 0xFFF9A8D4);
} else if (currentForm == VadimkaForm.WARDEN) {
graphics.fill(petX - 12, petY - 26, petX + 12, petY + 6, 0xFF0F766E);
graphics.fill(petX - 16, petY - 32, petX - 10, petY - 22, 0xFF2DD4BF);
graphics.fill(petX + 10, petY - 32, petX + 16, petY - 22, 0xFF2DD4BF);
graphics.fill(petX - 6, petY - 14, petX + 6, petY - 4, 0xFF115E59);
graphics.fill(petX - 4, petY - 12, petX + 4, petY - 6, 0xFF5EEAD4);
} else {
// Allay / Breeze / Axolotl / Player mini
graphics.fill(petX - 9, petY - 20, petX + 9, petY + 2, currentForm.primaryColor);
graphics.fill(petX - 16, petY - 16, petX - 9, petY - 6, currentForm.accentColor);
graphics.fill(petX + 9, petY - 16, petX + 16, petY - 6, currentForm.accentColor);
graphics.fill(petX - 5, petY - 14, petX - 2, petY - 10, 0xFFFFFFFF);
graphics.fill(petX + 2, petY - 14, petX + 5, petY - 10, 0xFFFFFFFF);
}

// Информация под сценой
int infoY = stageY + stageH + 6;
graphics.drawString(this.font, "В мире: " + (companionVisibleInWorld ? "АКТИВЕН" : "СКРЫТ"), px + 8, infoY, companionVisibleInWorld ? 0xFF4ADE80 : 0xFFF87171);
graphics.drawString(this.font, "Облик: " + currentForm.shortName, px + 8, infoY + 12, currentForm.accentColor);
graphics.drawString(this.font, "Крылья: " + WINGS_OPTIONS.get(wingsIndex), px + 8, infoY + 26, 0xFFE2E8F0);
graphics.drawString(this.font, "Рога: " + HORNS_OPTIONS.get(hornsIndex), px + 8, infoY + 37, 0xFFE2E8F0);
graphics.drawString(this.font, "Шляпка: " + HATS_OPTIONS.get(hatsIndex), px + 8, infoY + 48, 0xFFE2E8F0);
graphics.drawString(this.font, "Одежда: " + OUTFIT_OPTIONS.get(outfitIndex), px + 8, infoY + 59, 0xFFE2E8F0);
}
}
}
