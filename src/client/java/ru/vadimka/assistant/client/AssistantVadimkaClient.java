package ru.vadimka.assistant.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class AssistantVadimkaClient implements ClientModInitializer {
public static final String MOD_ID = "assistant_vadimka";

public enum VadimkaForm {
PLAYER("Player"),
ALLAY("Allay"),
BREEZE("Breeze"),
WARDEN("Warden"),
AXOLOTL("Blue Axolotl"),
RAT("Rat (Крыска)");

public final String title;

VadimkaForm(String title) {
this.title = title;
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
AUTOPILOT("2. Автопилот и обучение"),
AUTO_BUILD("3. Авто-постройка"),
LITEMATICA_HUD("4. Litematica HUD"),
VISUAL_RADAR("5. Визуал и Радар"),
CHESTS_VILLAGERS("6. Ларчики и Жители"),
COMBAT_PVP("7. Бой и PvP"),
MUSIC_AFK("8. Музыка и AFK-игры"),
CUSTOM_VISUAL("9. Кастом-визуал");

public final String title;

MenuTab(String title) {
this.title = title;
}
}

public static VadimkaForm currentForm = VadimkaForm.RAT;
public static boolean quietMode = false;
public static boolean randomChaosMode = false;
public static String menuFontStyle = "Minecraft Pixel";
public static String menuAccentPreset = "Emerald / Cyan";

public static final Map<MinionRole, Boolean> activeMinions = new EnumMap<>(MinionRole.class);
public static final List<String> playerWardrobeSlots = List.of(
"Крылья", "Рога", "Шляпки", "Одежда / Слои"
);
public static final List<String> minecraftAfkGames = List.of(
"Раннер по рельсам / тропе (MC)",
"Сверхзвуковой рывок по холмам (MC)",
"Коридорный шутер в данже / крепости (MC)",
"Быстрые MC-аркады (1–2 мин, мгновенный выход)"
);

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
client.setScreen(new AssistantVadimkaScreen(MenuTab.VADIMKA));
}
});
}

public static class AssistantVadimkaScreen extends Screen {
private MenuTab activeTab;

public AssistantVadimkaScreen(MenuTab activeTab) {
super(Component.literal("Assistant Vadimka — Меню [P]"));
this.activeTab = activeTab;
}

@Override
protected void init() {
int tabWidth = 116;
int tabHeight = 18;
int cols = 3;
int startX = (this.width - (cols * tabWidth + (cols - 1) * 4)) / 2;
int startY = 24;

MenuTab[] tabs = MenuTab.values();
for (int i = 0; i < tabs.length; i++) {
MenuTab tab = tabs[i];
int col = i % cols;
int row = i / cols;
int x = startX + col * (tabWidth + 4);
int y = startY + row * (tabHeight + 3);

Button btn = Button.builder(Component.literal(tab.title), b -> {
this.activeTab = tab;
this.rebuildWidgets();
}).bounds(x, y, tabWidth, tabHeight).build();
btn.active = (tab != this.activeTab);
this.addRenderableWidget(btn);
}

int actionY = this.height - 28;
this.addRenderableWidget(Button.builder(
Component.literal("Облик Вадимки: " + currentForm.title),
b -> {
VadimkaForm[] values = VadimkaForm.values();
currentForm = values[(currentForm.ordinal() + 1) % values.length];
this.rebuildWidgets();
}
).bounds(this.width / 2 - 155, actionY, 150, 20).build());

this.addRenderableWidget(Button.builder(
Component.literal("Закрыть [ESC / P]"),
b -> this.onClose()
).bounds(this.width / 2 + 5, actionY, 150, 20).build());
}

@Override
public boolean keyPressed(KeyEvent event) {
if (event.key() == GLFW.GLFW_KEY_P) {
this.onClose();
return true;
}
return super.keyPressed(event);
}

@Override
public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
graphics.fill(0, 0, this.width, this.height, 0xCC10141C);
graphics.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

int panelTop = 86;
int panelLeft = 24;
graphics.drawString(this.font, "Активная вкладка: " + activeTab.title, panelLeft, panelTop, 0x7CF3FF);
graphics.drawString(this.font, "Спутник Вадимка: " + currentForm.title + (quietMode ? " [Тихий режим]" : " [Активный]"), panelLeft, panelTop + 14, 0xA6FF9E);

int y = panelTop + 32;
switch (activeTab) {
case VADIMKA -> {
graphics.drawString(this.font, "6 обликов: PLAYER, ALLAY, BREEZE, WARDEN, AXOLOTL, RAT (Крыска)", panelLeft, y, 0xE2E8F0);
y += 14;
for (MinionRole role : MinionRole.values()) {
String state = Boolean.TRUE.equals(activeMinions.get(role)) ? "ON" : "OFF";
graphics.drawString(this.font, "• Миньон [" + role.title + "]: " + state, panelLeft, y, 0xCBD5E1);
y += 12;
}
}
case MUSIC_AFK -> {
graphics.drawString(this.font, "AFK-мини-игры в стиле Minecraft (мгновенный выход по клавише / тревоге):", panelLeft, y, 0xFDE68A);
y += 14;
for (String game : minecraftAfkGames) {
graphics.drawString(this.font, "• " + game, panelLeft, y, 0xE2E8F0);
y += 12;
}
}
case CUSTOM_VISUAL -> {
graphics.drawString(this.font, "Гардероб персонажа и превью в реальном времени:", panelLeft, y, 0xFBCFE8);
y += 14;
for (String slot : playerWardrobeSlots) {
graphics.drawString(this.font, "• Кастомизация: " + slot, panelLeft, y, 0xE2E8F0);
y += 12;
}
graphics.drawString(this.font, "• Меню [P]: шрифт [" + menuFontStyle + "], тема [" + menuAccentPreset + "]", panelLeft, y + 4, 0x93C5FD);
}
default -> {
graphics.drawString(this.font, "Модуль подключён в структуру Assistant Vadimka (Fabric 21.01.11+).", panelLeft, y, 0xE2E8F0);
graphics.drawString(this.font, "Готов к поэтапному наращиванию логики без нарушения правил серверов.", panelLeft, y + 14, 0x94A3B8);
}
}

super.render(graphics, mouseX, mouseY, partialTick);
}
}
}
