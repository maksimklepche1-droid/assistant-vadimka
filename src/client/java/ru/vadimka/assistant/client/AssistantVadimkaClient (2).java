package ru.vadimka.assistant.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AssistantVadimkaClient implements ClientModInitializer {
    public static final String MOD_ID = "assistant_vadimka";
    public static final String VERSION_LABEL = "v5.0 ULTIMATE";

    // =========================================================================
    // ПЕРЕЧИСЛЕНИЯ: 6 ОБЛИКОВ, 4 МИНЬОНА, 9 ВКЛАДОК, 5 ТЕМ, СТУДИЯ, AFK-ИГРЫ
    // =========================================================================

    public enum VadimkaForm {
        RAT("Rat (Крыска)", "Крыска", EntityType.SILVERFISH, 1.05, 44, 0xFF94A3B8, 0xFFF472B6),
        ALLAY("Allay (Спешик)", "Спешик", EntityType.ALLAY, 0.85, 38, 0xFF22D3EE, 0xFFA5F3FC),
        BREEZE("Breeze (Вихрь)", "Вихрь", EntityType.BREEZE, 0.54, 24, 0xFF818CF8, 0xFFC7D2FE),
        WARDEN("Warden (Хранитель)", "Warden", EntityType.WARDEN, 0.34, 15, 0xFF0D9488, 0xFF2DD4BF),
        AXOLOTL("Blue Axolotl (Аксолотль)", "Аксолотль", EntityType.AXOLOTL, 0.90, 36, 0xFF60A5FA, 0xFFF472B6),
        PLAYER("Player (Мини-игрок)", "Игрок", EntityType.ALLAY, 0.78, 30, 0xFF38BDF8, 0xFFFDE68A);

        public final String title;
        public final String shortName;
        public final EntityType<? extends Mob> entityType;
        public final double baseWorldScale;
        public final int previewGuiSize;
        public final int primaryColor;
        public final int accentColor;

        VadimkaForm(String title, String shortName, EntityType<? extends Mob> entityType,
                    double baseWorldScale, int previewGuiSize, int primaryColor, int accentColor) {
            this.title = title;
            this.shortName = shortName;
            this.entityType = entityType;
            this.baseWorldScale = baseWorldScale;
            this.previewGuiSize = previewGuiSize;
            this.primaryColor = primaryColor;
            this.accentColor = accentColor;
        }
    }

    public enum MinionRole {
        COLLECTOR("Сборщик", "§a✦ Сборщик", "§a[С]", 0xFF4ADE80),
        BUILDER("Строитель", "§e✦ Строитель", "§e[Б]", 0xFFFACC15),
        SCOUT("Разведчик", "§b✦ Разведчик", "§b[Р]", 0xFF38BDF8),
        SUPPLIER("Снабженец", "§d✦ Снабженец", "§d[П]", 0xFFF472B6);

        public final String title;
        public final String fullTag;
        public final String compactTag;
        public final int color;

        MinionRole(String title, String fullTag, String compactTag, int color) {
            this.title = title;
            this.fullTag = fullTag;
            this.compactTag = compactTag;
            this.color = color;
        }
    }

    public enum MenuTab {
        VADIMKA("1. Вадимка", "1. Спутник Вадимка, AI-Чат и Миньоны"),
        AUTOPILOT("2. Автопилот", "2. Автопилот, Спринт и Профайлер стиля"),
        AUTO_BUILD("3. Постройка", "3. Авто-постройка, Линейка и Редстоун"),
        LITEMATICA_HUD("4. Litematica", "4. Litematica HUD, Слои и Материалы"),
        VISUAL_RADAR("5. Радар", "5. Визуал, Мини-карта и Разведка"),
        CHESTS_VILLAGERS("6. Ларчики/Жители", "6. Ларчики (Сундуки) и Торговля с жителями"),
        COMBAT_PVP("7. Бой и PvP", "7. Бой, PvP HUD, Тотемы и Броня"),
        MUSIC_AFK("8. Музыка/AFK", "8. Spotify Connect (ПК) и 3 MC мини-игры"),
        CUSTOM_VISUAL("9. Кастом", "9. Кастом-визуал, 3D-Гардероб и Темы");

        public final String shortTitle;
        public final String fullTitle;

        MenuTab(String shortTitle, String fullTitle) {
            this.shortTitle = shortTitle;
            this.fullTitle = fullTitle;
        }
    }

    public enum MenuTheme {
        CYBER_EMERALD("Cyber Emerald", 0xF5070C11, 0xFF0C141D, 0xFF121F2D, 0xFF10B981, 0xFF38BDF8, 0xFF1D3347, 0xFF04261B),
        AMETHYST_VOID("Amethyst Void", 0xF50D0816, 0xFF150E24, 0xFF1F1635, 0xFF9333EA, 0xFFF472B6, 0xFF3B2461, 0xFF230B3B),
        NETHER_CRIMSON("Nether Crimson", 0xF513080C, 0xFF1D0D13, 0xFF29131B, 0xFFE11D48, 0xFFFBBF24, 0xFF4A1D2C, 0xFF380713),
        ARCTIC_FROST("Arctic Frost", 0xF5070F18, 0xFF0E1A29, 0xFF16273B, 0xFF0284C7, 0xFF7DD3FC, 0xFF254161, 0xFF062133),
        OBSIDIAN_GOLD("Obsidian Gold", 0xF50D0B08, 0xFF17140F, 0xFF241F16, 0xFFD97706, 0xFFFDE047, 0xFF4A3D23, 0xFF2E1A03);

        public final String title;
        public final int bgWindow;
        public final int bgPanel;
        public final int bgCard;
        public final int accent;
        public final int secondary;
        public final int borderMuted;
        public final int toggleOnBg;

        MenuTheme(String title, int bgWindow, int bgPanel, int bgCard,
                  int accent, int secondary, int borderMuted, int toggleOnBg) {
            this.title = title;
            this.bgWindow = bgWindow;
            this.bgPanel = bgPanel;
            this.bgCard = bgCard;
            this.accent = accent;
            this.secondary = secondary;
            this.borderMuted = borderMuted;
            this.toggleOnBg = toggleOnBg;
        }
    }

    public enum PreviewMode {
        DUO("Дуэт"),
        PLAYER_ONLY("Игрок"),
        PET_ONLY("Вадимка");

        public final String label;

        PreviewMode(String label) {
            this.label = label;
        }
    }

    public enum AfkGameType {
        RUNNER("MC-Раннер", "Уворот от криперов (A / D)"),
        SONIC("Рывок по холмам", "Прыжки за золотом (SPACE / W)"),
        DOOM("Данж-Шутер", "Стрельба по мобам (A / D / SPACE)");

        public final String title;
        public final String hint;

        AfkGameType(String title, String hint) {
            this.title = title;
            this.hint = hint;
        }
    }

    // =========================================================================
    // СТРУКТУРЫ ДАННЫХ ДЛЯ ЛАРЧИКОВ, ЖИТЕЛЕЙ И ЧАТА С ВАДИМКОЙ
    // =========================================================================

    public static class ChestRecord {
        public final BlockPos pos;
        public final String containerName;
        public final Map<String, Integer> items = new LinkedHashMap<>();
        public long lastUpdatedMs;

        public ChestRecord(BlockPos pos, String containerName) {
            this.pos = pos;
            this.containerName = containerName;
            this.lastUpdatedMs = System.currentTimeMillis();
        }

        public int totalStackCount() {
            int sum = 0;
            for (int v : items.values()) {
                sum += v;
            }
            return sum;
        }

        public String summaryLine() {
            if (items.isEmpty()) {
                return "Пустой ларчик";
            }
            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (Map.Entry<String, Integer> e : items.entrySet()) {
                if (shown > 0) sb.append(", ");
                sb.append(e.getKey()).append(" x").append(e.getValue());
                shown++;
                if (shown >= 3) break;
            }
            if (items.size() > 3) {
                sb.append(" (+").append(items.size() - 3).append(")");
            }
            return sb.toString();
        }
    }

    public static class VillagerTradeEntry {
        public final String costText;
        public final String resultText;
        public final boolean highlighted;
        public final boolean outOfStock;

        public VillagerTradeEntry(String costText, String resultText, boolean highlighted, boolean outOfStock) {
            this.costText = costText;
            this.resultText = resultText;
            this.highlighted = highlighted;
            this.outOfStock = outOfStock;
        }
    }

    public static class VillagerRecord {
        public final BlockPos pos;
        public final String title;
        public final List<VillagerTradeEntry> trades = new ArrayList<>();
        public long lastUpdatedMs;

        public VillagerRecord(BlockPos pos, String title) {
            this.pos = pos;
            this.title = title;
            this.lastUpdatedMs = System.currentTimeMillis();
        }

        public String bestOfferPreview() {
            if (trades.isEmpty()) {
                return "Нет записанных торгов";
            }
            for (VillagerTradeEntry t : trades) {
                if (t.highlighted) {
                    return t.costText + " -> " + t.resultText;
                }
            }
            VillagerTradeEntry first = trades.get(0);
            return first.costText + " -> " + first.resultText;
        }
    }

    public static class ChatLogEntry {
        public final boolean fromVadimka;
        public final String text;

        public ChatLogEntry(boolean fromVadimka, String text) {
            this.fromVadimka = fromVadimka;
            this.text = text;
        }
    }

    // =========================================================================
    // СОСТОЯНИЕ ВСЕХ 9 МОДУЛЕЙ МОДА
    // =========================================================================

    // 1. Вадимка, AI-чат и миньоны
    public static VadimkaForm currentForm = VadimkaForm.RAT;
    public static boolean companionVisibleInWorld = true;
    public static boolean minionsVisibleInWorld = true;
    public static boolean minionsSmartAi = true;
    public static boolean compactNameplates = true;
    public static boolean chatAssistantEnabled = true;
    public static int companionScalePercent = 100; // 60..140
    public static boolean quietMode = false;
    public static boolean randomChaosMode = false;
    public static boolean voiceCommentator = true;
    public static final Map<MinionRole, Boolean> activeMinions = new EnumMap<>(MinionRole.class);
    public static final Deque<ChatLogEntry> vadimkaChatHistory = new ArrayDeque<>();
    public static String vadimkaLastBubble = "Писк! Напиши «Вадимка, помощь» в чат!";

    // Отложенные ответы Вадимки (чтобы в чате ответ шёл строго после сообщения игрока)
    private static final Deque<String> pendingChatReplies = new ArrayDeque<>();
    private static int pendingChatReplyDelay = 0;
    private static boolean greetedInCurrentWorld = false;

    // 2. Автопилот и профайлер
    public static boolean styleProfiler = true;
    public static boolean sprintAssist = false;
    public static boolean safeAutopilot = false;
    public static boolean autoReturnControl = true;
    public static boolean parkourEdgeGuard = true;
    public static boolean fallDangerAlert = true;
    private static boolean autopilotWasHoldingForward = false;

    // 3. Постройка
    public static boolean litematicaAutoBuild = false;
    public static boolean smartRestoration = true;
    public static boolean hotbarAutoRefill = true;
    public static boolean rulerOverlay = true;
    public static boolean redstoneInspector = true;
    public static boolean toolDurabilityAlert = true;

    // 4. Litematica HUD
    public static boolean litematicaHudEnabled = true;
    public static boolean shulkerScanner = true;
    public static boolean layerSliceMode = false;
    public static boolean deficitHighlight = true;
    public static int targetBuildLayerY = 64;

    // 5. Радар и мини-карта (RLE-движок без просадок FPS)
    public static boolean radarHudEnabled = true;
    public static boolean radarTerrainMode = true;
    public static boolean radarRotateMap = false;
    public static boolean radarRightSide = false;
    public static int radarZoomLevel = 1; // 0 = близко, 1 = средне, 2 = далеко
    public static boolean soundRadarEnabled = true;
    public static boolean ariadneThread = true;
    public static boolean sixthSense = true;
    public static boolean fullBrightnessBoost = false;

    // 6. Ларчики (Сундуки) и Торговля с жителями
    public static boolean chestMemory = true;
    public static boolean radarShowChests = true;
    public static boolean radarShowDrops = true;
    public static boolean villagerTracker = true;
    public static boolean autoRecordTrades = true;
    public static boolean smartSorter = false;
    public static BlockPos homeWaypoint = null;
    public static BlockPos activeNavTarget = null;
    public static String activeNavLabel = "";

    public static final Map<BlockPos, ChestRecord> rememberedChests = new LinkedHashMap<>();
    public static final Map<BlockPos, VillagerRecord> rememberedVillagers = new LinkedHashMap<>();
    private static BlockPos lastInteractedBlockPos = null;
    private static String lastInteractedBlockName = "Ларчик";
    private static boolean wasContainerOpen = false;
    private static boolean wasMerchantOpen = false;

    // 7. Бой и PvP
    public static boolean combatHudEnabled = true;
    public static boolean armorDurabilityHud = true;
    public static boolean totemScanner = true;
    public static boolean trajectoriesEnabled = true;
    public static boolean deathReplayMarker = true;

    // 8. РЕАЛЬНЫЙ Spotify Connect (Windows 10) и AFK мини-игры
    public static boolean spotifyHudEnabled = true;
    public static boolean spotifyLiveSync = true;
    public static boolean afkPanicExit = true;
    public static AfkGameType selectedAfkGame = AfkGameType.RUNNER;

    private static final ExecutorService spotifyExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vadimka-Spotify-Sync");
        t.setDaemon(true);
        return t;
    });
    private static volatile String spotifyCurrentArtist = "Spotify";
    private static volatile String spotifyCurrentTrack = "Подключение к Spotify...";
    private static volatile boolean spotifyIsPlaying = false;
    private static volatile boolean spotifyAppRunning = false;
    private static volatile long lastSpotifyPollMs = 0L;
    private static volatile boolean spotifyPollInProgress = false;
    private static String lastAnnouncedTrack = "";

    // 9. Кастом-визуал и 3D-гардероб
    public static MenuTheme currentTheme = MenuTheme.AMETHYST_VOID;
    public static PreviewMode previewMode = PreviewMode.DUO;
    public static final List<String> WINGS_OPTIONS = List.of(
        "Выкл", "Ангельские", "Демонические", "Неон-элитры", "Кристальные"
    );
    public static final List<String> HORNS_OPTIONS = List.of(
        "Выкл", "Дьявольские", "Оленьи", "Кибер-рожки", "Золотой нимб"
    );
    public static final List<String> HATS_OPTIONS = List.of(
        "Выкл", "Корона", "Ушки Крыски", "Цилиндр", "Наушники"
    );
    public static final List<String> OUTFIT_OPTIONS = List.of(
        "Стандарт", "Кибер-плащ", "Тень Ассасина", "Изумрудный сет", "Незерит Неон"
    );
    public static final List<String> CROSSHAIR_OPTIONS = List.of(
        "Ванильный", "Точка + Крест", "Неон-круг", "Тактический"
    );

    public static int wingsIndex = 1;
    public static int hornsIndex = 1;
    public static int hatsIndex = 2;
    public static int outfitIndex = 1;
    public static int crosshairIndex = 1;
    public static boolean worldCosmeticsParticles = true;

    // =========================================================================
    // ВНУТРЕННИЕ КЭШИ ПРОИЗВОДИТЕЛЬНОСТИ (75+ FPS БЕЗ ЛАГОВ)
    // =========================================================================

    private static KeyMapping openMenuKey;
    private static Mob companionEntity = null;
    private static VadimkaForm spawnedForm = null;
    private static int spawnedScalePercent = -1;
    private static boolean spawnedCompactTags = true;
    private static final Map<MinionRole, Mob> minionEntities = new EnumMap<>(MinionRole.class);
    private static int worldTick = 0;

    private static Mob studioPreviewMob = null;
    private static VadimkaForm studioPreviewForm = null;

    // Инкрементальный кэш карты (32x32, 2 строки за тик)
    private static final int MAP_GRID = 32;
    private static final int[] terrainColors = new int[MAP_GRID * MAP_GRID];
    private static final int[] terrainHeights = new int[MAP_GRID * MAP_GRID];
    private static final int[] rowRenderBuf = new int[MAP_GRID];
    private static int mapCenterX = Integer.MIN_VALUE;
    private static int mapCenterZ = Integer.MIN_VALUE;
    private static int mapLastZoom = -1;
    private static int mapScanRow = 0;
    private static boolean mapNeedsFullReset = true;

    // Инкрементальный сканер сундуков
    private static final int CHEST_RADIUS = 14;
    private static final List<BlockPos> confirmedChests = new ArrayList<>();
    private static final List<BlockPos> scanningChests = new ArrayList<>();
    private static int chestScanSliceX = -CHEST_RADIUS;
    private static BlockPos chestScanOrigin = BlockPos.ZERO;

    private static class RadarBlip {
        final double x;
        final double z;
        final int type; // 0=player, 1=hostile, 2=animal, 3=villager, 4=item
        final String label;

        RadarBlip(double x, double z, int type, String label) {
            this.x = x;
            this.z = z;
            this.type = type;
            this.label = label;
        }
    }

    private static final List<RadarBlip> cachedRadarBlips = new ArrayList<>();
    private static Vec3 nearestLootPos = null;
    private static Vec3 nearestHostilePos = null;

    // Нить Ариадны
    private static final Deque<Vec3> breadcrumbs = new ArrayDeque<>();
    private static Vec3 lastBreadcrumb = null;

    // Профайлер и уведомления
    private static double sessionDistanceWalked = 0.0;
    private static Vec3 lastPlayerPos = null;
    private static int fallDropBlocks = 0;
    private static boolean edgeDangerNow = false;
    private static BlockPos lastDeathPos = null;
    private static boolean threatBehindPlayer = false;
    private static double nearestHostileDist = 999.0;
    private static double nearestHostileAngle = 0.0;
    private static String vadimkaToastText = "";
    private static int vadimkaToastTicks = 0;
    private static float lastPlayerHealth = 20.0F;
    private static int lastLowToolWarnTick = -200;
    private static final Random worldRandom = new Random();

    private static final List<String> CHAOS_QUOTES = List.of(
        "Писк! Пора копать алмазы на Y = -58!",
        "Миньоны работают: Сборщик ищет лут, Разведчик палит мобов!",
        "Криперы любят тишину за спиной — но у меня Шестое чувство!",
        "Проверь 6-ю вкладку «Ларчики и Жители» — я запоминаю твои сундуки и торги!",
        "Врубай Spotify на компе — я покажу трек прямо на экране и могу листать его!"
    );

    private static final List<String> VADIMKA_JOKES = List.of(
        "Заходит крипер в бар... и больше бара нет. Писк!",
        "Почему эндермены не любят воду? Потому что у них нет зонтика из незерита!",
        "Житель продаёт 1 пшеницу за 64 изумруда и называет это «выгодной сделкой».",
        "Когда я в облике Крыски, я на 200% быстрее нахожу сыр и алмазы!",
        "Главное правило Майнкрафта: никогда не копай под себя... но все всё равно копают."
    );

    // =========================================================================
    // ИНИЦИАЛИЗАЦИЯ МОДА И ПОДПИСКА НА ЧАТ / ТИКИ / HUD
    // =========================================================================

    @Override
    public void onInitializeClient() {
        for (MinionRole role : MinionRole.values()) {
            activeMinions.put(role, true);
        }
        addChatHistory(true, "Привет! Я Вадимка. Нажми [P] или пиши мне прямо в чат (например: «Вадимка, помощь» или «!дом»).");

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.assistant_vadimka.menu",
            GLFW.GLFW_KEY_P,
            KeyMapping.Category.MISC
        ));

        // Перехват чата Minecraft — Вадимка реально читает, отвечает и выполняет команды!
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!chatAssistantEnabled || message == null) {
                return true;
            }
            String trimmed = message.trim();
            if (trimmed.isEmpty()) {
                return true;
            }
            // Приватные команды через "!" не улетают на сервер, а обрабатываются лично Вадимкой
            if (trimmed.startsWith("!")) {
                String cleanCmd = trimmed.substring(1).trim();
                addChatHistory(false, trimmed);
                String reply = processVadimkaChatCommand(cleanCmd);
                scheduleVadimkaReply(reply, 2);
                return false;
            }
            // Обычное обращение в чате по имени "Вадимка" / "Vadimka"
            String lower = trimmed.toLowerCase(Locale.ROOT);
            if (lower.contains("вадимк") || lower.contains("вадим") || lower.contains("vadimka") || lower.contains("крыск")) {
                addChatHistory(false, trimmed);
                String reply = processVadimkaChatCommand(trimmed);
                scheduleVadimkaReply(reply, 6);
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                if (autopilotWasHoldingForward) {
                    client.options.keyUp.setDown(false);
                    client.options.keyJump.setDown(false);
                    autopilotWasHoldingForward = false;
                }
                cleanupWorldEntities();
                breadcrumbs.clear();
                confirmedChests.clear();
                scanningChests.clear();
                cachedRadarBlips.clear();
                lastBreadcrumb = null;
                lastPlayerPos = null;
                mapNeedsFullReset = true;
                greetedInCurrentWorld = false;
                return;
            }

            while (openMenuKey.consumeClick()) {
                if (client.screen instanceof AssistantVadimkaScreen) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new AssistantVadimkaScreen(MenuTab.VADIMKA));
                }
            }
            tickWorldSystems(client);
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> renderAllHudWidgets(graphics));
    }

    // =========================================================================
    // УМНЫЙ AI-ЧАТ ВАДИМКИ (РЕАЛЬНЫЕ ОТВЕТЫ И КОМАНДЫ В ЧАТЕ И В МЕНЮ)
    // =========================================================================

    private static void addChatHistory(boolean fromVadimka, String text) {
        vadimkaChatHistory.addLast(new ChatLogEntry(fromVadimka, text));
        while (vadimkaChatHistory.size() > 24) {
            vadimkaChatHistory.removeFirst();
        }
        if (fromVadimka) {
            vadimkaLastBubble = text;
        }
    }

    private static void scheduleVadimkaReply(String reply, int delayTicks) {
        if (reply == null || reply.isEmpty()) {
            return;
        }
        addChatHistory(true, reply);
        pendingChatReplies.addLast(reply);
        pendingChatReplyDelay = Math.max(1, delayTicks);
    }

    public static String sendPromptToVadimkaFromMenu(String input) {
        if (input == null || input.trim().isEmpty()) {
            return "";
        }
        String trimmed = input.trim();
        addChatHistory(false, trimmed);
        String reply = processVadimkaChatCommand(trimmed);
        addChatHistory(true, reply);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§d[✦ Вадимка] §f" + reply), false);
        }
        return reply;
    }

    private static String processVadimkaChatCommand(String rawText) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        String lower = rawText.toLowerCase(Locale.ROOT)
            .replace("вадимка", "")
            .replace("вадим", "")
            .replace("vadimka", "")
            .replace(",", " ")
            .trim();

        if (lower.isEmpty() || lower.contains("привет") || lower.contains("здаров") || lower.contains("хай") || lower.contains("ку") || lower.equals("тут") || lower.contains("ты тут")) {
            return "Привет, босс! Я рядом в облике «" + currentForm.shortName + "». Могу найти лут в ларчиках, показать торги жителей, полистать Spotify, сменить облик или провести домой!";
        }
        if (lower.contains("помощ") || lower.contains("команд") || lower.contains("что умеешь") || lower.contains("help")) {
            return "Пиши в чат или через «!»: «!дом» (задать дом), «!к дому» (автопилот), «!найди <предмет>» (поиск в ларчиках), «!жители» (торги), «!облик крыска/warden/вихрь», «!трек» / «!следующий» / «!пауза» (Spotify), «!координаты», «!шутка»!";
        }
        if (lower.contains("как дела") || lower.contains("как ты")) {
            int activeM = countActiveMinions();
            return "Отлично! Миньоны (" + activeM + "/4) в строю, ларчиков в памяти: " + rememberedChests.size() + ", жителей: " + rememberedVillagers.size() + ".";
        }
        if (lower.contains("шутк") || lower.contains("анекдот") || lower.contains("рассмеши")) {
            return VADIMKA_JOKES.get(worldRandom.nextInt(VADIMKA_JOKES.size()));
        }

        // Команды смены облика Вадимки
        if (lower.contains("крыс")) {
            currentForm = VadimkaForm.RAT;
            return "Писк! Переключился в облик Крыски!";
        }
        if (lower.contains("варден") || lower.contains("хранител") || lower.contains("warden")) {
            currentForm = VadimkaForm.WARDEN;
            return "Готово! Теперь я мини-Хранитель (Warden).";
        }
        if (lower.contains("вихр") || lower.contains("бриз") || lower.contains("breeze")) {
            currentForm = VadimkaForm.BREEZE;
            return "Вжух! Сменил облик на Вихря (Breeze).";
        }
        if (lower.contains("спешик") || lower.contains("аллей") || lower.contains("allay")) {
            currentForm = VadimkaForm.ALLAY;
            return "Сменил облик на Спешика (Allay)!";
        }
        if (lower.contains("аксолотл") || lower.contains("axolotl")) {
            currentForm = VadimkaForm.AXOLOTL;
            return "Бульк! Теперь я Синий Аксолотль!";
        }
        if (lower.contains("игрок") || lower.contains("player")) {
            currentForm = VadimkaForm.PLAYER;
            return "Переключился в облик Мини-игрока!";
        }

        // Команды Spotify прямо из чата!
        if (lower.contains("следующ") || lower.contains("скип") || lower.contains("next")) {
            sendWindowsMediaKey(0xB0);
            return "Переключил на следующий трек в Spotify! Сейчас обновлю название...";
        }
        if (lower.contains("предыдущ") || lower.contains("назад трек") || lower.contains("prev")) {
            sendWindowsMediaKey(0xB1);
            return "Вернул предыдущий трек в Spotify!";
        }
        if (lower.contains("пауз") || lower.contains("стоп музык") || lower.contains("включи музык") || lower.contains("плей")) {
            sendWindowsMediaKey(0xB3);
            return "Нажал Play/Pause в Spotify!";
        }
        if (lower.contains("трек") || lower.contains("музык") || lower.contains("спотифай") || lower.contains("spotify") || lower.contains("что играет")) {
            pollSpotifyAsync(true);
            if (!spotifyAppRunning) {
                return "Spotify на ПК сейчас не запущен. Открой Spotify (или нажми кнопку в 8-й вкладке [P])!";
            }
            if (!spotifyIsPlaying) {
                return "Spotify открыт, но сейчас на паузе. Напиши «!пауза», чтобы включить!";
            }
            return "Сейчас в Spotify играет: " + spotifyCurrentArtist + " — " + spotifyCurrentTrack;
        }

        // Точка дома и Автопилот
        if (lower.contains("задай дом") || lower.equals("дом") || lower.contains("сохрани дом") || lower.contains("тут дом")) {
            if (p != null) {
                homeWaypoint = p.blockPosition();
                activeNavTarget = homeWaypoint;
                activeNavLabel = "Дом";
                return "Запомнил твой Дом на координатах " + homeWaypoint.getX() + " / " + homeWaypoint.getY() + " / " + homeWaypoint.getZ() + " и отметил на карте!";
            }
        }
        if (lower.contains("к дому") || lower.contains("домой") || lower.contains("автопилот")) {
            if (homeWaypoint == null) {
                return "Точка дома ещё не задана! Напиши «!дом» на базе, и я запомню координаты.";
            }
            activeNavTarget = homeWaypoint;
            activeNavLabel = "Дом";
            safeAutopilot = !lower.contains("стоп");
            if (safeAutopilot) {
                return "Включаю безопасный автопилот к Дому! Нажми S/A/D или Shift для мгновенного возврата управления.";
            }
            return "Автопилот выключен, управление у тебя.";
        }

        // Поиск по Ларчикам (Сундукам)
        if (lower.contains("найд") || lower.contains("где ") || lower.contains("сундук") || lower.contains("ларчик") || lower.contains("лут")) {
            String query = lower
                .replace("найди", "")
                .replace("найти", "")
                .replace("где", "")
                .replace("в сундуках", "")
                .replace("в ларчиках", "")
                .replace("сундуки", "")
                .replace("сундук", "")
                .replace("ларчики", "")
                .replace("ларчик", "")
                .replace("мой", "")
                .replace("мои", "")
                .trim();
            if (rememberedChests.isEmpty()) {
                return "Я пока не заглядывал в твои ларчики. Открой любой сундук или бочку — и я запомню всё содержимое!";
            }
            if (query.isEmpty() || query.length() < 2) {
                return "В памяти " + rememberedChests.size() + " ларчиков (рядом видно " + confirmedChests.size() + "). Напиши «!найди алмаз» или открой вкладку «6. Ларчики/Жители»!";
            }
            for (ChestRecord cr : rememberedChests.values()) {
                for (Map.Entry<String, Integer> item : cr.items.entrySet()) {
                    if (item.getKey().toLowerCase(Locale.ROOT).contains(query)) {
                        activeNavTarget = cr.pos;
                        activeNavLabel = item.getKey();
                        int dist = p != null ? (int) Math.sqrt(p.blockPosition().distSqr(cr.pos)) : 0;
                        return "Нашёл «" + item.getKey() + "» (x" + item.getValue() + ") в " + cr.containerName
                            + " на [" + cr.pos.getX() + ", " + cr.pos.getY() + ", " + cr.pos.getZ() + "] (" + dist + "м). Поставил метку на радар!";
                    }
                }
            }
            return "В " + rememberedChests.size() + " запомненных ларчиках «" + query + "» пока не нашёл. Открой остальные сундуки, чтобы я обновил память!";
        }

        // Жители и торговля
        if (lower.contains("жител") || lower.contains("торг") || lower.contains("починк") || lower.contains("книг")) {
            if (rememberedVillagers.isEmpty()) {
                return "Я ещё не записывал торги. Открой окно торговли с любым жителем — я сохраню все его книги и цены во вкладку «6. Ларчики/Жители»!";
            }
            String q = lower.contains("починк") ? "почин" : "";
            for (VillagerRecord vr : rememberedVillagers.values()) {
                for (VillagerTradeEntry tr : vr.trades) {
                    if (q.isEmpty() || tr.resultText.toLowerCase(Locale.ROOT).contains(q)) {
                        activeNavTarget = vr.pos;
                        activeNavLabel = "Житель";
                        return "Житель на [" + vr.pos.getX() + ", " + vr.pos.getY() + ", " + vr.pos.getZ() + "]: " + tr.costText + " -> " + tr.resultText + ". Поставил метку на карте!";
                    }
                }
            }
            return "Записано жителей: " + rememberedVillagers.size() + ". Открой вкладку «6. Ларчики/Жители» → [Жители], чтобы увидеть все сделки!";
        }

        // Координаты, здоровье, время, опасности
        if (lower.contains("коорд") || lower.contains("где я") || lower.contains("статус")) {
            if (p != null) {
                int totems = countPlayerTotems(p);
                return "Твои координаты: X=" + (int) p.getX() + " Y=" + (int) p.getY() + " Z=" + (int) p.getZ()
                    + " | HP: " + (int) Math.ceil(p.getHealth()) + " | Тотемы: " + totems
                    + (nearestHostileDist < 30.0 ? (" | Ближайший враг: " + String.format("%.1f", nearestHostileDist) + "м") : " | Врагов рядом нет.");
            }
        }
        if (lower.contains("миньон")) {
            minionsVisibleInWorld = !lower.contains("убер") && !lower.contains("скрой");
            return minionsVisibleInWorld
                ? "Команда из 4 миньонов на связи! Умный AI: " + (minionsSmartAi ? "ВКЛ" : "ВЫКЛ") + "."
                : "Спрятал миньонов, чтобы не мешали.";
        }
        if (lower.contains("тихо") || lower.contains("замолч")) {
            quietMode = true;
            return "Окей, перехожу в тихий режим. Позовёшь, когда понадоблюсь!";
        }
        if (lower.contains("говори") || lower.contains("проснись")) {
            quietMode = false;
            return "Я снова на связи и готов комментировать всё вокруг!";
        }
        if (lower.contains("спасиб") || lower.contains("красав") || lower.contains("молодец")) {
            return "Всегда пожалуйста, босс! Для тебя — хоть за незеритом в лаву!";
        }

        return "Слышу тебя! Напиши «Вадимка, помощь», чтобы увидеть все команды (ларчики, жители, Spotify, дом, облик), или нажми [P]!";
    }

    // =========================================================================
    // РЕАЛЬНАЯ ИНТЕГРАЦИЯ SPOTIFY DESKTOP (WINDOWS 10) — БЕЗ ФРИЗОВ!
    // =========================================================================

    private static boolean isWindowsOs() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static void pollSpotifyAsync(boolean force) {
        if (!spotifyLiveSync || !isWindowsOs()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && (spotifyPollInProgress || (now - lastSpotifyPollMs) < 2500L)) {
            return;
        }
        spotifyPollInProgress = true;
        lastSpotifyPollMs = now;

        spotifyExecutor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe",
                    "-NoProfile",
                    "-NonInteractive",
                    "-WindowStyle", "Hidden",
                    "-Command",
                    "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; "
                        + "$p = Get-Process -Name Spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -and $_.MainWindowTitle.Length -gt 0 } | Select-Object -First 1; "
                        + "if ($p) { Write-Output $p.MainWindowTitle } else { Write-Output '__NOT_RUNNING__' }"
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                String line = "";
                try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String read;
                    while ((read = br.readLine()) != null) {
                        if (!read.trim().isEmpty()) {
                            line = read.trim();
                            break;
                        }
                    }
                }
                proc.waitFor(2, TimeUnit.SECONDS);

                if (line.isEmpty() || "__NOT_RUNNING__".equals(line)) {
                    spotifyAppRunning = false;
                    spotifyIsPlaying = false;
                    spotifyCurrentArtist = "Spotify не запущен";
                    spotifyCurrentTrack = "Открой Spotify на ПК";
                } else if (line.equalsIgnoreCase("Spotify")
                        || line.equalsIgnoreCase("Spotify Free")
                        || line.equalsIgnoreCase("Spotify Premium")
                        || line.equalsIgnoreCase("Advertisement")) {
                    spotifyAppRunning = true;
                    spotifyIsPlaying = false;
                    spotifyCurrentArtist = line;
                    spotifyCurrentTrack = "На паузе (нажми ▶)";
                } else {
                    spotifyAppRunning = true;
                    spotifyIsPlaying = true;
                    int dash = line.indexOf(" - ");
                    if (dash > 0) {
                        spotifyCurrentArtist = line.substring(0, dash).trim();
                        spotifyCurrentTrack = line.substring(dash + 3).trim();
                    } else {
                        spotifyCurrentArtist = "Spotify";
                        spotifyCurrentTrack = line;
                    }
                }
            } catch (Throwable ignored) {
                spotifyAppRunning = false;
                spotifyIsPlaying = false;
                spotifyCurrentArtist = "Spotify";
                spotifyCurrentTrack = "Ожидание Spotify...";
            } finally {
                spotifyPollInProgress = false;
            }
        });
    }

    private static void sendWindowsMediaKey(int vkCode) {
        if (!isWindowsOs()) {
            return;
        }
        spotifyExecutor.submit(() -> {
            try {
                String ps = "Add-Type -TypeDefinition 'using System.Runtime.InteropServices; "
                    + "public class VMKey { [DllImport(\"user32.dll\")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, int dwExtraInfo); }'; "
                    + "[VMKey]::keybd_event(" + vkCode + ", 0, 0, 0); "
                    + "[VMKey]::keybd_event(" + vkCode + ", 0, 2, 0);";
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", ps
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                proc.waitFor(2, TimeUnit.SECONDS);
                Thread.sleep(420);
            } catch (Throwable ignored) {
            } finally {
                lastSpotifyPollMs = 0L;
                pollSpotifyAsync(true);
            }
        });
    }

    private static void launchSpotifyDesktop() {
        if (!isWindowsOs()) {
            return;
        }
        spotifyExecutor.submit(() -> {
            try {
                new ProcessBuilder("cmd.exe", "/c", "start", "", "spotify:").start();
            } catch (Throwable ignored) {
            }
        });
    }

    // =========================================================================
    // ВСПОМОГАТЕЛЬНЫЕ ФУНКЦИИ СОСТОЯНИЯ И ОЧИСТКИ
    // =========================================================================

    private static int countActiveMinions() {
        int c = 0;
        for (MinionRole r : MinionRole.values()) {
            if (Boolean.TRUE.equals(activeMinions.get(r))) {
                c++;
            }
        }
        return c;
    }

    private static int countPlayerTotems(Player p) {
        int totems = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(Items.TOTEM_OF_UNDYING)) {
                totems += st.getCount();
            }
        }
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            totems += p.getOffhandItem().getCount();
        }
        return totems;
    }

    private static int countPlayerEmeralds(Player p) {
        int emeralds = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(Items.EMERALD)) {
                emeralds += st.getCount();
            }
        }
        return emeralds;
    }

    private static void pushVadimkaToast(String text, int durationTicks) {
        if (quietMode) {
            return;
        }
        vadimkaToastText = text;
        vadimkaToastTicks = durationTicks;
        vadimkaLastBubble = text;
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
        studioPreviewMob = null;
        studioPreviewForm = null;
    }

    // =========================================================================
    // ИГРОВОЙ ТИК (СВЕРХЛЁГКИЙ, 75+ FPS, ЧАТ, ЛАРЧИКИ, ЖИТЕЛИ, АВТОПИЛОТ, SPOTIFY)
    // =========================================================================

    private static void tickWorldSystems(Minecraft mc) {
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            return;
        }
        worldTick++;
        if (vadimkaToastTicks > 0) {
            vadimkaToastTicks--;
        }

        // Приветствие при заходе в мир
        if (!greetedInCurrentWorld && worldTick > 30) {
            greetedInCurrentWorld = true;
            if (chatAssistantEnabled && !quietMode) {
                player.displayClientMessage(Component.literal(
                    "§d[✦ Вадимка] §fПривет! Я на месте (§b" + currentForm.shortName
                        + "§f). Меню на §e[P]§f, а в чат можешь писать мне напрямую: §a«Вадимка, помощь»§f или §a«!трек»§f!"
                ), false);
            }
        }

        // Отправка отложенных ответов Вадимки в игровой чат
        if (!pendingChatReplies.isEmpty()) {
            if (pendingChatReplyDelay > 0) {
                pendingChatReplyDelay--;
            } else {
                String msg = pendingChatReplies.pollFirst();
                if (msg != null) {
                    player.displayClientMessage(Component.literal("§d[✦ Вадимка] §f" + msg), false);
                }
                pendingChatReplyDelay = 4;
            }
        }

        // Реальный опрос Spotify в фоне (раз в ~2.5 секунды)
        if (spotifyLiveSync && (spotifyHudEnabled || mc.screen instanceof AssistantVadimkaScreen)) {
            pollSpotifyAsync(false);
            if (spotifyIsPlaying && !spotifyCurrentTrack.equals(lastAnnouncedTrack) && !spotifyCurrentTrack.contains("...")) {
                lastAnnouncedTrack = spotifyCurrentTrack;
                if (!quietMode && worldTick > 60) {
                    pushVadimkaToast("♫ Spotify: " + spotifyCurrentArtist + " — " + spotifyCurrentTrack, 75);
                }
            }
        }

        // Отслеживание блока под прицелом для Памяти Ларчиков и Жителей
        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = bhr.getBlockPos();
            BlockState bs = level.getBlockState(bp);
            if (bs.is(Blocks.CHEST) || bs.is(Blocks.TRAPPED_CHEST)
                    || bs.is(Blocks.BARREL) || bs.is(Blocks.ENDER_CHEST)
                    || bs.getBlock().getName().getString().toLowerCase(Locale.ROOT).contains("шалкер")
                    || bs.getBlock().getName().getString().toLowerCase(Locale.ROOT).contains("shulker")) {
                lastInteractedBlockPos = bp.immutable();
                lastInteractedBlockName = bs.getBlock().getName().getString();
            }
        }

        // Авто-запись содержимого открытых Ларчиков и торгов Жителей!
        inspectOpenMenusForChestsAndVillagers(mc, player);

        // Яркость (безопасный предел 1.0)
        if (fullBrightnessBoost && mc.options.gamma().get() < 1.0) {
            mc.options.gamma().set(1.0);
        }

        // Авто-спринт ассистент
        if (sprintAssist && mc.screen == null && mc.options.keyUp.isDown()
                && !player.isCrouching() && !player.isUsingItem()
                && player.getFoodData().getFoodLevel() > 6) {
            player.setSprinting(true);
        }

        // Безопасный Автопилот к активной цели (Дом / Ларчик / Житель)
        tickSafeAutopilot(mc, level, player);

        // Профайлер пройденной дистанции
        Vec3 curPos = player.position();
        if (styleProfiler && lastPlayerPos != null) {
            double moved = curPos.distanceTo(lastPlayerPos);
            if (moved < 10.0) {
                sessionDistanceWalked += moved;
            }
        }
        lastPlayerPos = curPos;

        // Здоровье, урон, точка смерти и тревога AFK
        float hp = player.getHealth();
        if (hp <= 0.0F && lastPlayerHealth > 0.0F && deathReplayMarker) {
            lastDeathPos = player.blockPosition();
            pushVadimkaToast("Вадимка: Точка смерти (" + lastDeathPos.getX() + ", " + lastDeathPos.getY() + ", " + lastDeathPos.getZ() + ")", 120);
        } else if (hp < lastPlayerHealth - 0.1F) {
            if (voiceCommentator) {
                pushVadimkaToast("Вадимка: Осторожно, урон! HP: " + (int) Math.ceil(hp), 65);
            }
            if (safeAutopilot && autoReturnControl) {
                safeAutopilot = false;
                releaseAutopilotKeys(mc);
                pushVadimkaToast("Автопилот отключён: получен урон!", 70);
            }
            if (afkPanicExit && mc.screen instanceof AssistantVadimkaScreen scr && scr.afkGameRunning) {
                scr.afkGameRunning = false;
                mc.setScreen(null);
                pushVadimkaToast("Вадимка: ЭКСТРЕННЫЙ ВЫХОД ИЗ AFK!", 90);
            }
        }
        lastPlayerHealth = hp;

        // Предупреждение о прочности инструмента
        if (worldTick % 20 == 0 && toolDurabilityAlert) {
            ItemStack hand = player.getMainHandItem();
            if (!hand.isEmpty() && hand.isDamageableItem() && hand.getMaxDamage() > 0) {
                int left = hand.getMaxDamage() - hand.getDamageValue();
                if (left > 0 && (left * 100) / hand.getMaxDamage() <= 12 && (worldTick - lastLowToolWarnTick) > 200) {
                    pushVadimkaToast("Снабженец: " + hand.getHoverName().getString() + " почти сломан (" + left + ")!", 75);
                    lastLowToolWarnTick = worldTick;
                }
            }
        }

        // Проверка обрыва
        if ((fallDangerAlert || parkourEdgeGuard || safeAutopilot) && worldTick % 4 == 0) {
            BlockPos below = player.blockPosition().below();
            boolean airBelow = level.getBlockState(below).isAir();
            fallDropBlocks = airBelow ? measureDropBelow(level, player.blockPosition()) : 0;
            edgeDangerNow = parkourEdgeGuard && fallDropBlocks >= 4;
        }

        // Режим рандомной дичи
        if (randomChaosMode && !quietMode && worldTick % 650 == 0) {
            String quote = CHAOS_QUOTES.get(worldRandom.nextInt(CHAOS_QUOTES.size()));
            pushVadimkaToast("Вадимка: " + quote, 90);
            if (chatAssistantEnabled) {
                player.displayClientMessage(Component.literal("§d[✦ Вадимка] §7" + quote), false);
            }
        }

        // 1. Сканирование сущностей (раз в 4 тика)
        if (worldTick % 4 == 0) {
            scanEntitiesForRadarAndAi(level, player);
            if (afkPanicExit && nearestHostileDist < 4.5 && mc.screen instanceof AssistantVadimkaScreen scr && scr.afkGameRunning) {
                scr.afkGameRunning = false;
                mc.setScreen(null);
                pushVadimkaToast("Вадимка: Враг рядом! AFK-игра закрыта!", 90);
            }
        }

        // 2. Обновление 3D-спутника Вадимки и 4 миньонов
        updateCompanionAndMinions(mc, level, player);

        // 3. 3D-крылья и нимб/рога (только в F5)
        if (worldCosmeticsParticles && worldTick % 2 == 0 && !mc.options.getCameraType().isFirstPerson()) {
            spawnPlayerCosmeticParticles(level, player);
        }

        // 4. Нить Ариадны
        if (ariadneThread) {
            Vec3 crumbPos = new Vec3(player.getX(), player.getY() + 0.22, player.getZ());
            if (lastBreadcrumb == null || lastBreadcrumb.distanceToSqr(crumbPos) > 12.25) {
                breadcrumbs.addLast(crumbPos);
                lastBreadcrumb = crumbPos;
                while (breadcrumbs.size() > 36) {
                    breadcrumbs.removeFirst();
                }
            }
            if (worldTick % 10 == 0) {
                int idx = 0;
                for (Vec3 c : breadcrumbs) {
                    if ((idx + worldTick / 10) % 4 == 0 && c.distanceToSqr(crumbPos) < 324.0) {
                        level.addParticle(ParticleTypes.ENCHANT, c.x, c.y + 0.15, c.z, 0.0, 0.015, 0.0);
                    }
                    idx++;
                }
            }
        }

        // 5. Инкрементальное обновление карты рельефа (2 строки за тик)
        if (radarHudEnabled && radarTerrainMode) {
            tickIncrementalTerrainMap(level, Mth.floor(player.getX()), Mth.floor(player.getZ()));
        }

        // 6. Инкрементальный сканер сундуков
        if (radarHudEnabled && (radarShowChests || chestMemory) && worldTick % 2 == 0) {
            tickIncrementalChestScanner(level, player.blockPosition());
        }
    }

    private static void inspectOpenMenusForChestsAndVillagers(Minecraft mc, Player player) {
        AbstractContainerMenu menu = player.containerMenu;
        boolean containerOpen = menu != null && menu != player.inventoryMenu && mc.screen != null && !(mc.screen instanceof AssistantVadimkaScreen);

        if (containerOpen && menu instanceof MerchantMenu merchantMenu) {
            if (autoRecordTrades && (!wasMerchantOpen || worldTick % 20 == 0)) {
                MerchantOffers offers = merchantMenu.getOffers();
                if (offers != null && !offers.isEmpty()) {
                    BlockPos vPos = player.blockPosition();
                    VillagerRecord rec = new VillagerRecord(vPos, "Житель [" + vPos.getX() + ", " + vPos.getZ() + "]");
                    for (MerchantOffer offer : offers) {
                        String costA = formatStackShort(offer.getCostA());
                        String costB = offer.getCostB().isEmpty() ? "" : (" + " + formatStackShort(offer.getCostB()));
                        String res = formatStackWithEnchants(offer.getResult());
                        boolean valuable = res.toLowerCase(Locale.ROOT).contains("почин")
                            || res.toLowerCase(Locale.ROOT).contains("mending")
                            || res.toLowerCase(Locale.ROOT).contains("неразруш")
                            || res.toLowerCase(Locale.ROOT).contains("эффектив")
                            || res.toLowerCase(Locale.ROOT).contains("острот")
                            || res.toLowerCase(Locale.ROOT).contains("защит")
                            || offer.getResult().is(Items.ENCHANTED_BOOK)
                            || offer.getResult().is(Items.DIAMOND_PICKAXE)
                            || offer.getResult().is(Items.DIAMOND_SWORD);
                        rec.trades.add(new VillagerTradeEntry(costA + costB, res, valuable, offer.isOutOfStock()));
                    }
                    rememberedVillagers.put(vPos, rec);
                    if (!wasMerchantOpen) {
                        pushVadimkaToast("Вадимка: Записал " + rec.trades.size() + " торгов жителя!", 65);
                    }
                }
            }
            wasMerchantOpen = true;
            wasContainerOpen = false;
            return;
        }
        wasMerchantOpen = false;

        if (containerOpen && chestMemory && lastInteractedBlockPos != null) {
            if (!wasContainerOpen || worldTick % 15 == 0) {
                ChestRecord rec = new ChestRecord(lastInteractedBlockPos, lastInteractedBlockName);
                for (Slot slot : menu.slots) {
                    if (slot.container == player.getInventory()) {
                        continue;
                    }
                    ItemStack st = slot.getItem();
                    if (!st.isEmpty()) {
                        String name = formatStackWithEnchants(st);
                        rec.items.merge(name, st.getCount(), Integer::sum);
                    }
                }
                if (!rec.items.isEmpty() || !rememberedChests.containsKey(lastInteractedBlockPos)) {
                    rememberedChests.put(lastInteractedBlockPos, rec);
                    if (!wasContainerOpen) {
                        pushVadimkaToast("Память ларчиков: сохранено " + rec.items.size() + " видов предметов", 55);
                    }
                }
            }
            wasContainerOpen = true;
        } else {
            wasContainerOpen = false;
        }
    }

    private static String formatStackShort(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return stack.getHoverName().getString() + (stack.getCount() > 1 ? (" x" + stack.getCount()) : "");
    }

    private static String formatStackWithEnchants(ItemStack stack) {
        if (stack.isEmpty()) return "";
        String base = stack.getHoverName().getString();
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        ItemEnchantments active = stack.get(DataComponents.ENCHANTMENTS);
        ItemEnchantments target = (stored != null && !stored.isEmpty()) ? stored : active;
        if (target != null && !target.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int cnt = 0;
            for (var entry : target.entrySet()) {
                if (cnt > 0) sb.append(", ");
                int lvl = entry.getValue();
                sb.append(Enchantment.getFullname(entry.getKey(), lvl).getString());
                cnt++;
                if (cnt >= 2) break;
            }
            if (stack.is(Items.ENCHANTED_BOOK)) {
                return "Книга: " + sb;
            }
            return base + " (" + sb + ")";
        }
        return base + (stack.getCount() > 1 ? (" x" + stack.getCount()) : "");
    }

    private static void releaseAutopilotKeys(Minecraft mc) {
        if (autopilotWasHoldingForward) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
            autopilotWasHoldingForward = false;
        }
    }

    private static void tickSafeAutopilot(Minecraft mc, ClientLevel level, Player player) {
        if (!safeAutopilot) {
            releaseAutopilotKeys(mc);
            return;
        }
        BlockPos goal = activeNavTarget != null ? activeNavTarget : homeWaypoint;
        if (goal == null || mc.screen != null) {
            releaseAutopilotKeys(mc);
            return;
        }
        // Мгновенный возврат управления игроку при нажатии S/A/D/Shift
        if (autoReturnControl && (mc.options.keyDown.isDown() || mc.options.keyLeft.isDown()
                || mc.options.keyRight.isDown() || mc.options.keyShift.isDown())) {
            safeAutopilot = false;
            releaseAutopilotKeys(mc);
            pushVadimkaToast("Автопилот: управление возвращено тебе!", 60);
            return;
        }
        // Защита от обрыва и врагов
        if (fallDropBlocks >= 4 || nearestHostileDist < 5.0) {
            safeAutopilot = false;
            releaseAutopilotKeys(mc);
            pushVadimkaToast("Автопилот остановлен: впереди опасность!", 75);
            return;
        }

        double dx = (goal.getX() + 0.5) - player.getX();
        double dz = (goal.getZ() + 0.5) - player.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        if (dist < 2.2) {
            safeAutopilot = false;
            releaseAutopilotKeys(mc);
            pushVadimkaToast("Автопилот: Ты прибыл к цели (" + (activeNavLabel.isEmpty() ? "Дом" : activeNavLabel) + ")!", 80);
            return;
        }

        float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float delta = Mth.wrapDegrees(desiredYaw - player.getYRot());
        player.setYRot(player.getYRot() + Mth.clamp(delta, -8.0F, 8.0F));
        mc.options.keyUp.setDown(true);
        mc.options.keyJump.setDown(player.horizontalCollision || player.isInWater());
        autopilotWasHoldingForward = true;
    }

    private static int measureDropBelow(ClientLevel level, BlockPos start) {
        int drop = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(start.getX(), start.getY() - 1, start.getZ());
        for (int i = 0; i < 16; i++) {
            BlockState st = level.getBlockState(pos);
            if (st.is(Blocks.WATER)) {
                return 0;
            }
            if (!st.isAir()) {
                break;
            }
            drop++;
            pos.setY(pos.getY() - 1);
        }
        return drop;
    }

    private static void scanEntitiesForRadarAndAi(ClientLevel level, Player player) {
        cachedRadarBlips.clear();
        threatBehindPlayer = false;
        nearestHostileDist = 999.0;
        nearestHostilePos = null;
        nearestLootPos = null;
        double nearestLootDistSq = 121.0;

        double lookX = -Math.sin(Math.toRadians(player.getYRot()));
        double lookZ = Math.cos(Math.toRadians(player.getYRot()));

        for (Entity entity : level.entitiesForRendering()) {
            if (entity == player || entity.getId() <= -77000) {
                continue;
            }
            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();
            if (Math.abs(dx) > 64.0 || Math.abs(dz) > 64.0) {
                continue;
            }

            if (entity instanceof Player otherPlayer) {
                cachedRadarBlips.add(new RadarBlip(entity.getX(), entity.getZ(), 0, otherPlayer.getName().getString()));
            } else if (entity instanceof Monster) {
                cachedRadarBlips.add(new RadarBlip(entity.getX(), entity.getZ(), 1, null));
                double dy = Math.abs(entity.getY() - player.getY());
                if (dy <= 8.0) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < nearestHostileDist) {
                        nearestHostileDist = dist;
                        nearestHostileAngle = Math.atan2(dz, dx);
                        nearestHostilePos = entity.position();
                    }
                    if (dist < 10.0 && dist > 0.1) {
                        double dot = (dx / dist) * lookX + (dz / dist) * lookZ;
                        if (dot < -0.25) {
                            threatBehindPlayer = true;
                        }
                    }
                }
            } else if (villagerTracker && entity.getType() == EntityType.VILLAGER) {
                cachedRadarBlips.add(new RadarBlip(entity.getX(), entity.getZ(), 3, null));
            } else if (entity instanceof Animal) {
                cachedRadarBlips.add(new RadarBlip(entity.getX(), entity.getZ(), 2, null));
            } else if (entity instanceof ItemEntity) {
                if (radarShowDrops) {
                    cachedRadarBlips.add(new RadarBlip(entity.getX(), entity.getZ(), 4, null));
                }
                double dSq = dx * dx + dz * dz;
                if (dSq < nearestLootDistSq && Math.abs(entity.getY() - player.getY()) <= 5.0) {
                    nearestLootDistSq = dSq;
                    nearestLootPos = entity.position();
                }
            }
        }
    }

    private static void tickIncrementalTerrainMap(ClientLevel level, int px, int pz) {
        int stepBlocks = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        int snapX = Math.floorDiv(px, stepBlocks) * stepBlocks;
        int snapZ = Math.floorDiv(pz, stepBlocks) * stepBlocks;

        if (mapNeedsFullReset || mapLastZoom != radarZoomLevel
                || Math.abs(snapX - mapCenterX) >= stepBlocks * 3
                || Math.abs(snapZ - mapCenterZ) >= stepBlocks * 3) {
            mapCenterX = snapX;
            mapCenterZ = snapZ;
            mapLastZoom = radarZoomLevel;
            mapScanRow = 0;
            mapNeedsFullReset = false;
        }

        int half = MAP_GRID / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int r = 0; r < 2; r++) {
            int gz = mapScanRow;
            int wz = mapCenterZ + (gz - half) * stepBlocks;
            for (int gx = 0; gx < MAP_GRID; gx++) {
                int wx = mapCenterX + (gx - half) * stepBlocks;
                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                pos.set(wx, topY, wz);
                BlockState state = level.getBlockState(pos);
                MapColor mapColor = state.getMapColor(level, pos);
                if (mapColor.col == 0 && topY > level.getMinY() + 2) {
                    pos.set(wx, topY - 1, wz);
                    state = level.getBlockState(pos);
                    mapColor = state.getMapColor(level, pos);
                }
                int idx = gz * MAP_GRID + gx;
                terrainHeights[idx] = topY;
                int rgb = mapColor.col != 0 ? mapColor.col : 0x16202B;
                if (gz > 0) {
                    int northY = terrainHeights[(gz - 1) * MAP_GRID + gx];
                    if (topY > northY) {
                        rgb = adjustBrightness(rgb, 18);
                    } else if (topY < northY) {
                        rgb = adjustBrightness(rgb, -20);
                    }
                }
                terrainColors[idx] = 0xFF000000 | rgb;
            }
            mapScanRow = (mapScanRow + 1) % MAP_GRID;
        }
    }

    private static void tickIncrementalChestScanner(ClientLevel level, BlockPos playerPos) {
        if (chestScanSliceX == -CHEST_RADIUS) {
            chestScanOrigin = playerPos;
            scanningChests.clear();
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinY(), chestScanOrigin.getY() - 4);
        int maxY = Math.min(level.getMaxY() - 1, chestScanOrigin.getY() + 4);
        int wx = chestScanOrigin.getX() + chestScanSliceX;

        for (int dz = -CHEST_RADIUS; dz <= CHEST_RADIUS; dz++) {
            int wz = chestScanOrigin.getZ() + dz;
            for (int y = minY; y <= maxY; y++) {
                pos.set(wx, y, wz);
                BlockState st = level.getBlockState(pos);
                if (st.is(Blocks.CHEST) || st.is(Blocks.TRAPPED_CHEST)
                        || st.is(Blocks.BARREL) || st.is(Blocks.ENDER_CHEST)) {
                    if (scanningChests.size() < 28) {
                        scanningChests.add(pos.immutable());
                    }
                }
            }
        }

        chestScanSliceX++;
        if (chestScanSliceX > CHEST_RADIUS) {
            confirmedChests.clear();
            confirmedChests.addAll(scanningChests);
            chestScanSliceX = -CHEST_RADIUS;
        }
    }

    private static void updateCompanionAndMinions(Minecraft mc, ClientLevel level, Player player) {
        if (!companionVisibleInWorld) {
            if (companionEntity != null) {
                companionEntity.discard();
                companionEntity = null;
                spawnedForm = null;
            }
        } else {
            boolean needsRespawn = companionEntity == null
                || companionEntity.isRemoved()
                || companionEntity.level() != level
                || spawnedForm != currentForm;

            if (needsRespawn) {
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
                    created.setCustomName(Component.literal(compactNameplates
                        ? ("§b✦ Вадимка")
                        : ("§b✦ Вадимка §7[" + currentForm.shortName + "]")));
                    created.setCustomNameVisible(true);
                    applyEntityScale(created, currentForm.baseWorldScale * (companionScalePercent / 100.0));
                    created.setPos(player.getX() + 1.1, player.getY() + 1.0, player.getZ() + 1.1);
                    level.addEntity(created);
                    companionEntity = created;
                    spawnedForm = currentForm;
                    spawnedScalePercent = companionScalePercent;
                    spawnedCompactTags = compactNameplates;
                }
            } else {
                if (spawnedScalePercent != companionScalePercent) {
                    applyEntityScale(companionEntity, currentForm.baseWorldScale * (companionScalePercent / 100.0));
                    spawnedScalePercent = companionScalePercent;
                }
                if (spawnedCompactTags != compactNameplates) {
                    companionEntity.setCustomName(Component.literal(compactNameplates
                        ? ("§b✦ Вадимка")
                        : ("§b✦ Вадимка §7[" + currentForm.shortName + "]")));
                    spawnedCompactTags = compactNameplates;
                }
            }

            if (companionEntity != null) {
                double yawRad = Math.toRadians(player.getYRot());
                double sideOffset = 1.15;
                double backOffset = 0.85;
                double targetX = player.getX() - Math.cos(yawRad) * sideOffset + Math.sin(yawRad) * backOffset;
                double targetZ = player.getZ() - Math.sin(yawRad) * sideOffset - Math.cos(yawRad) * backOffset;
                double baseHeight = (currentForm == VadimkaForm.RAT || currentForm == VadimkaForm.AXOLOTL) ? 0.14 : 1.08;
                double bob = (currentForm == VadimkaForm.RAT)
                    ? Math.abs(Math.sin(worldTick * 0.24)) * 0.08
                    : Math.sin(worldTick * 0.12) * 0.11;
                double targetY = player.getY() + baseHeight + bob;

                companionEntity.xo = companionEntity.getX();
                companionEntity.yo = companionEntity.getY();
                companionEntity.zo = companionEntity.getZ();

                double nextX = Mth.lerp(0.20, companionEntity.getX(), targetX);
                double nextY = Mth.lerp(0.20, companionEntity.getY(), targetY);
                double nextZ = Mth.lerp(0.20, companionEntity.getZ(), targetZ);
                companionEntity.setPos(nextX, nextY, nextZ);

                double lookTargetX = (nearestHostilePos != null && nearestHostileDist < 12.0) ? nearestHostilePos.x : player.getX();
                double lookTargetZ = (nearestHostilePos != null && nearestHostileDist < 12.0) ? nearestHostilePos.z : player.getZ();
                float lookYaw = (float) (Mth.atan2(lookTargetZ - nextZ, lookTargetX - nextX) * (180.0 / Math.PI)) - 90.0F;
                companionEntity.setYRot(lookYaw);
                companionEntity.setYHeadRot(lookYaw);
                companionEntity.setYBodyRot(lookYaw);

                if (worldTick % 16 == 0) {
                    level.addParticle(ParticleTypes.END_ROD, nextX, nextY + 0.35, nextZ, 0.0, 0.012, 0.0);
                }
            }
        }

        // 4 миньона с умным AI
        MinionRole[] roles = MinionRole.values();
        for (int i = 0; i < roles.length; i++) {
            MinionRole role = roles[i];
            boolean enabled = companionVisibleInWorld && minionsVisibleInWorld && Boolean.TRUE.equals(activeMinions.get(role));
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
                    minion.setCustomName(Component.literal(compactNameplates ? role.compactTag : role.fullTag));
                    minion.setCustomNameVisible(true);
                    applyEntityScale(minion, 0.56);
                    minion.setPos(player.getX(), player.getY() + 1.5, player.getZ());
                    level.addEntity(minion);
                    minionEntities.put(role, minion);
                    existing = minion;
                }
            } else {
                String expectedTag = compactNameplates ? role.compactTag : role.fullTag;
                if (existing.getCustomName() == null || !expectedTag.equals(existing.getCustomName().getString())) {
                    existing.setCustomName(Component.literal(expectedTag));
                }
            }

            if (existing != null) {
                double yawRad = Math.toRadians(player.getYRot());
                double slotAngle = yawRad + Math.PI + ((i - 1.5) * 0.72);
                double tx = player.getX() - Math.sin(slotAngle) * 2.1;
                double tz = player.getZ() + Math.cos(slotAngle) * 2.1;
                double ty = player.getY() + 1.55 + Math.sin(worldTick * 0.10 + i * 1.6) * 0.16;

                if (minionsSmartAi) {
                    if (role == MinionRole.COLLECTOR && nearestLootPos != null) {
                        tx = nearestLootPos.x;
                        ty = nearestLootPos.y + 0.85 + Math.sin(worldTick * 0.2) * 0.12;
                        tz = nearestLootPos.z;
                    } else if (role == MinionRole.SCOUT && nearestHostilePos != null && nearestHostileDist < 16.0) {
                        Vec3 mid = player.position().lerp(nearestHostilePos, 0.45);
                        tx = mid.x;
                        ty = mid.y + 1.85;
                        tz = mid.z;
                    } else if (role == MinionRole.BUILDER
                            && mc.hitResult instanceof BlockHitResult bhr
                            && bhr.getType() == HitResult.Type.BLOCK
                            && !player.getMainHandItem().isEmpty()) {
                        Vec3 hit = bhr.getLocation();
                        if (hit.distanceToSqr(player.position()) < 36.0) {
                            tx = hit.x;
                            ty = hit.y + 0.95;
                            tz = hit.z;
                        }
                    }
                }

                existing.xo = existing.getX();
                existing.yo = existing.getY();
                existing.zo = existing.getZ();
                existing.setPos(
                    Mth.lerp(0.14, existing.getX(), tx),
                    Mth.lerp(0.14, existing.getY(), ty),
                    Mth.lerp(0.14, existing.getZ(), tz)
                );

                float mYaw = (float) (Mth.atan2(player.getZ() - existing.getZ(), player.getX() - existing.getX()) * (180.0 / Math.PI)) - 90.0F;
                existing.setYRot(mYaw);
                existing.setYHeadRot(mYaw);
                existing.setYBodyRot(mYaw);
            }
        }
    }

    private static void applyEntityScale(Mob mob, double scale) {
        AttributeInstance attr = mob.getAttribute(Attributes.SCALE);
        if (attr != null) {
            attr.setBaseValue(Mth.clamp(scale, 0.25, 2.0));
        }
    }

    private static void spawnPlayerCosmeticParticles(ClientLevel level, Player player) {
        double yawRad = Math.toRadians(player.getYRot());
        double backX = Math.sin(yawRad);
        double backZ = -Math.cos(yawRad);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);

        if (wingsIndex > 0) {
            var wingParticle = switch (wingsIndex) {
                case 1 -> ParticleTypes.SNOWFLAKE;
                case 2 -> ParticleTypes.FLAME;
                case 3 -> ParticleTypes.SOUL_FIRE_FLAME;
                default -> ParticleTypes.ELECTRIC_SPARK;
            };
            double flap = Math.sin(worldTick * 0.20) * 0.14;
            for (int side = -1; side <= 1; side += 2) {
                for (int step = 1; step <= 3; step++) {
                    if ((worldTick + step) % 4 != 0) {
                        continue;
                    }
                    double spread = 0.28 * step;
                    double up = 1.06 + (step == 2 ? 0.28 : (step == 3 ? 0.14 : 0.05)) + flap * step;
                    double back = 0.34 + 0.12 * step;
                    level.addParticle(
                        wingParticle,
                        player.getX() + backX * back + rightX * (side * spread),
                        player.getY() + up,
                        player.getZ() + backZ * back + rightZ * (side * spread),
                        0.0, 0.0, 0.0
                    );
                }
            }
        }

        if (hornsIndex == 4 && worldTick % 4 == 0) {
            double ang = (worldTick * 0.35) % (Math.PI * 2.0);
            level.addParticle(
                ParticleTypes.END_ROD,
                player.getX() + Math.cos(ang) * 0.34,
                player.getY() + 2.16,
                player.getZ() + Math.sin(ang) * 0.34,
                0.0, 0.0, 0.0
            );
        } else if (hornsIndex > 0 && worldTick % 6 == 0) {
            var hornP = hornsIndex == 1 ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK;
            for (int side = -1; side <= 1; side += 2) {
                level.addParticle(
                    hornP,
                    player.getX() + rightX * (side * 0.24),
                    player.getY() + 2.02,
                    player.getZ() + rightZ * (side * 0.24),
                    0.0, 0.01, 0.0
                );
            }
        }
    }

    // =========================================================================
    // ИГРОВОЙ HUD (МИНИ-КАРТА, РЕАЛЬНЫЙ SPOTIFY, НАВИГАЦИЯ, PVP, LITEMATICA)
    // =========================================================================

    private static void renderAllHudWidgets(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        if (mc.screen instanceof AssistantVadimkaScreen) {
            return;
        }

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (radarHudEnabled) {
            renderMinimapRadarHud(graphics, mc, sw);
        }
        if (crosshairIndex > 0 && mc.screen == null) {
            renderCustomCrosshair(graphics, sw, sh);
        }
        if (combatHudEnabled) {
            renderCombatSurvivalHud(graphics, mc, sw, sh);
        }
        if (litematicaHudEnabled) {
            renderLitematicaMiniHud(graphics, mc, sw);
        }
        if (rulerOverlay && mc.screen == null) {
            renderRulerAndRedstoneHud(graphics, mc, sw, sh);
        }
        if (spotifyHudEnabled) {
            renderSpotifyMiniHud(graphics, mc, sw, sh);
        }

        // Верхняя строка статуса: Опасность / Обрыв / Активная цель (Дом, Ларчик, Житель)
        if (sixthSense && threatBehindPlayer) {
            String warn = "⚠ ОПАСНОСТЬ СЗАДИ (" + String.format("%.1f", nearestHostileDist) + "м)";
            drawTopBanner(graphics, mc.font, sw, warn, 0xE63B0A12, 0xFFEF4444, 0xFFFCA5A5);
        } else if (fallDangerAlert && fallDropBlocks >= 6 && mc.screen == null) {
            String warn = "⚠ ОБРЫВ: " + fallDropBlocks + " бл.";
            drawTopBanner(graphics, mc.font, sw, warn, 0xE63B2508, 0xFFF59E0B, 0xFFFDE68A);
        } else if (activeNavTarget != null) {
            int dist = (int) Math.sqrt(mc.player.blockPosition().distSqr(activeNavTarget));
            String nav = (safeAutopilot ? "▶ АВТОПИЛОТ → " : "✦ МЕТКА: ")
                + (activeNavLabel.isEmpty() ? "Цель" : activeNavLabel)
                + " (" + dist + "м)";
            drawTopBanner(graphics, mc.font, sw, nav, currentTheme.bgWindow, currentTheme.accent, 0xFFF8FAFC);
        } else if (edgeDangerNow && mc.screen == null) {
            graphics.drawString(mc.font, "▼ КРАЙ", sw / 2 - 18, sh / 2 + 12, 0xFFFACC15);
        }

        if (vadimkaToastTicks > 0 && !vadimkaToastText.isEmpty()) {
            int tw = mc.font.width(vadimkaToastText) + 18;
            int tx = (sw - tw) / 2;
            int ty = sh - 58;
            fillRoundedRect(graphics, tx, ty, tw, 18, currentTheme.bgWindow);
            drawRoundedBorder(graphics, tx, ty, tw, 18, currentTheme.accent);
            graphics.drawString(mc.font, vadimkaToastText, tx + 9, ty + 5, 0xFFF8FAFC);
        }
    }

    private static void drawTopBanner(GuiGraphics g, Font font, int sw, String text, int bg, int border, int fg) {
        int ww = font.width(text) + 18;
        int wx = (sw - ww) / 2;
        int wy = 10;
        fillRoundedRect(g, wx, wy, ww, 18, bg);
        drawRoundedBorder(g, wx, wy, ww, 18, border);
        g.drawString(font, text, wx + 9, wy + 5, fg);
    }

    private static void renderMinimapRadarHud(GuiGraphics graphics, Minecraft mc, int screenW) {
        int cell = 3;
        int mapSize = MAP_GRID * cell; // 96x96
        int pad = 8;
        int x = radarRightSide ? (screenW - mapSize - pad - 4) : pad;
        int y = 32;
        int cx = x + mapSize / 2;
        int cy = y + mapSize / 2;

        fillRoundedRect(graphics, x - 2, y - 2, mapSize + 4, mapSize + 4, currentTheme.bgWindow);
        drawRoundedBorder(graphics, x - 2, y - 2, mapSize + 4, mapSize + 4, currentTheme.accent);

        int stepBlocks = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        int halfGrid = MAP_GRID / 2;
        double yawRad = Math.toRadians(mc.player.getYRot() + 180.0);
        double cos = radarRotateMap ? Math.cos(yawRad) : 1.0;
        double sin = radarRotateMap ? Math.sin(yawRad) : 0.0;

        if (radarTerrainMode) {
            if (!radarRotateMap) {
                for (int gz = 0; gz < MAP_GRID; gz++) {
                    int rowBase = gz * MAP_GRID;
                    int py = y + gz * cell;
                    int gx = 0;
                    while (gx < MAP_GRID) {
                        int col = terrainColors[rowBase + gx];
                        int runEnd = gx + 1;
                        while (runEnd < MAP_GRID && terrainColors[rowBase + runEnd] == col) {
                            runEnd++;
                        }
                        graphics.fill(x + gx * cell, py, x + runEnd * cell, py + cell, col);
                        gx = runEnd;
                    }
                }
            } else {
                for (int gz = 0; gz < MAP_GRID; gz++) {
                    double relY = gz - halfGrid;
                    for (int gx = 0; gx < MAP_GRID; gx++) {
                        double relX = gx - halfGrid;
                        int sx = halfGrid + (int) Math.round(relX * cos + relY * sin);
                        int sz = halfGrid + (int) Math.round(-relX * sin + relY * cos);
                        rowRenderBuf[gx] = (sx >= 0 && sx < MAP_GRID && sz >= 0 && sz < MAP_GRID)
                            ? terrainColors[sz * MAP_GRID + sx]
                            : 0xFF0E1520;
                    }
                    int py = y + gz * cell;
                    int gx = 0;
                    while (gx < MAP_GRID) {
                        int col = rowRenderBuf[gx];
                        int runEnd = gx + 1;
                        while (runEnd < MAP_GRID && rowRenderBuf[runEnd] == col) {
                            runEnd++;
                        }
                        graphics.fill(x + gx * cell, py, x + runEnd * cell, py + cell, col);
                        gx = runEnd;
                    }
                }
            }
            graphics.fill(x, y, x + mapSize, y + mapSize, 0x1E080D14);
        } else {
            graphics.fill(x, y, x + mapSize, y + mapSize, 0xF0090E16);
        }

        graphics.fill(cx, y + 2, cx + 1, y + mapSize - 2, 0x24FFFFFF);
        graphics.fill(x + 2, cy, x + mapSize - 2, cy + 1, 0x24FFFFFF);

        double mapScale = (double) cell / stepBlocks;
        int maxOff = (mapSize / 2) - 4;

        // Звуковой радар
        if (soundRadarEnabled && nearestHostileDist < 22.0) {
            double hDx = Math.cos(nearestHostileAngle) * 20.0;
            double hDz = Math.sin(nearestHostileAngle) * 20.0;
            double rX = hDx * cos - hDz * sin;
            double rY = hDx * sin + hDz * cos;
            double len = Math.max(1.0, Math.sqrt(rX * rX + rY * rY));
            int ax = cx + (int) ((rX / len) * (maxOff + 1));
            int ay = cy + (int) ((rY / len) * (maxOff + 1));
            graphics.fill(ax - 3, ay - 3, ax + 4, ay + 4, 0xCCEF4444);
        }

        // Активная метка (Дом / Ларчик / Житель)
        BlockPos pin = activeNavTarget != null ? activeNavTarget : homeWaypoint;
        if (pin != null) {
            double dx = (pin.getX() + 0.5) - mc.player.getX();
            double dz = (pin.getZ() + 0.5) - mc.player.getZ();
            double rotX = (dx * cos - dz * sin) * mapScale;
            double rotY = (dx * sin + dz * cos) * mapScale;
            int hx = cx + (int) Mth.clamp(rotX, -maxOff, maxOff);
            int hy = cy + (int) Mth.clamp(rotY, -maxOff, maxOff);
            graphics.fill(hx - 2, hy - 2, hx + 3, hy + 3, 0xFF000000);
            graphics.fill(hx - 1, hy - 1, hx + 2, hy + 2, 0xFF22D3EE);
        }

        // Сундуки
        if (radarShowChests) {
            for (BlockPos chestPos : confirmedChests) {
                double dx = (chestPos.getX() + 0.5) - mc.player.getX();
                double dz = (chestPos.getZ() + 0.5) - mc.player.getZ();
                double rotX = (dx * cos - dz * sin) * mapScale;
                double rotY = (dx * sin + dz * cos) * mapScale;
                if (Math.abs(rotX) <= maxOff && Math.abs(rotY) <= maxOff) {
                    int mx = cx + (int) rotX;
                    int my = cy + (int) rotY;
                    graphics.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFFF59E0B);
                }
            }
        }

        // Сущности
        for (RadarBlip blip : cachedRadarBlips) {
            double dx = blip.x - mc.player.getX();
            double dz = blip.z - mc.player.getZ();
            double rotX = (dx * cos - dz * sin) * mapScale;
            double rotY = (dx * sin + dz * cos) * mapScale;
            if (Math.abs(rotX) > maxOff || Math.abs(rotY) > maxOff) {
                continue;
            }
            int dotX = cx + (int) rotX;
            int dotY = cy + (int) rotY;
            if (blip.type == 0) {
                graphics.fill(dotX - 2, dotY - 2, dotX + 3, dotY + 3, 0xFF000000);
                graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFF38BDF8);
                if (blip.label != null) {
                    graphics.drawString(mc.font, trimText(mc.font, blip.label, 44), dotX + 4, dotY - 3, 0xFFE0F2FE);
                }
            } else if (blip.type == 1) {
                graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFFEF4444);
            } else if (blip.type == 2) {
                graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFF4ADE80);
            } else if (blip.type == 3) {
                graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFFC084FC);
            } else if (blip.type == 4) {
                graphics.fill(dotX, dotY, dotX + 2, dotY + 2, 0xFFFACC15);
            }
        }

        // Север (N)
        if (!radarRotateMap) {
            graphics.fill(cx - 4, y + 1, cx + 4, y + 10, 0xCC090E14);
            graphics.drawString(mc.font, "N", cx - 2, y + 2, 0xFF38BDF8);
        } else {
            int nx = cx + (int) (-sin * (maxOff - 2)) - 2;
            int ny = cy + (int) (-cos * (maxOff - 2)) - 3;
            graphics.fill(nx - 1, ny - 1, nx + 7, ny + 8, 0xCC090E14);
            graphics.drawString(mc.font, "N", nx, ny, 0xFF38BDF8);
        }

        // Игрок в центре
        graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF000000);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        if (!radarRotateMap) {
            double lookRad = Math.toRadians(mc.player.getYRot());
            int tipX = cx + (int) Math.round(-Math.sin(lookRad) * 4.0);
            int tipY = cy + (int) Math.round(Math.cos(lookRad) * 4.0);
            graphics.fill(tipX - 1, tipY - 1, tipX + 1, tipY + 1, currentTheme.accent);
        } else {
            graphics.fill(cx, cy - 4, cx + 1, cy - 1, currentTheme.accent);
        }

        // Нижняя инфо-плашка
        int infoY = y + mapSize + 5;
        int infoW = mapSize + 4;
        int infoH = 29;
        fillRoundedRect(graphics, x - 2, infoY, infoW, infoH, currentTheme.bgWindow);
        drawRoundedBorder(graphics, x - 2, infoY, infoW, infoH, currentTheme.secondary);

        String coords = (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
        String dir = getCompassDirection(mc.player.getYRot());
        graphics.drawString(mc.font, trimText(mc.font, coords + " " + dir, infoW - 8), x + 2, infoY + 4, 0xFFF1F5F9);
        String line2 = (pin != null)
            ? ("⌂ " + (int) Math.sqrt(mc.player.blockPosition().distSqr(pin)) + "м • " + currentForm.shortName)
            : ("✦ " + currentForm.shortName + (quietMode ? " (Тих)" : ""));
        graphics.drawString(mc.font, trimText(mc.font, line2, infoW - 8), x + 2, infoY + 16, currentTheme.accent);
    }

    private static String getCompassDirection(float yaw) {
        float norm = ((yaw % 360.0F) + 360.0F) % 360.0F;
        if (norm >= 337.5 || norm < 22.5) return "S";
        if (norm < 67.5) return "SW";
        if (norm < 112.5) return "W";
        if (norm < 157.5) return "NW";
        if (norm < 202.5) return "N";
        if (norm < 247.5) return "NE";
        if (norm < 292.5) return "E";
        return "SE";
    }

    private static void renderCustomCrosshair(GuiGraphics g, int sw, int sh) {
        int cx = sw / 2;
        int cy = sh / 2;
        int col = currentTheme.accent;
        if (crosshairIndex == 1) {
            g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
            g.fill(cx - 6, cy, cx - 2, cy + 1, col);
            g.fill(cx + 2, cy, cx + 6, cy + 1, col);
            g.fill(cx, cy - 6, cx + 1, cy - 2, col);
            g.fill(cx, cy + 2, cx + 1, cy + 6, col);
        } else if (crosshairIndex == 2) {
            drawRoundedBorder(g, cx - 5, cy - 5, 11, 11, col);
            g.fill(cx, cy, cx + 1, cy + 1, 0xFFFFFFFF);
        } else if (crosshairIndex == 3) {
            g.fill(cx - 7, cy, cx - 3, cy + 1, currentTheme.secondary);
            g.fill(cx + 4, cy, cx + 8, cy + 1, currentTheme.secondary);
            g.fill(cx, cy + 3, cx + 1, cy + 7, currentTheme.secondary);
            g.fill(cx, cy, cx + 1, cy + 1, col);
        }
    }

    private static void renderRulerAndRedstoneHud(GuiGraphics g, Minecraft mc, int sw, int sh) {
        if (!(mc.hitResult instanceof BlockHitResult bhr) || bhr.getType() != HitResult.Type.BLOCK) {
            return;
        }
        if (mc.player == null || mc.level == null) {
            return;
        }
        BlockPos bp = bhr.getBlockPos();
        BlockState st = mc.level.getBlockState(bp);
        double dist = mc.player.position().distanceTo(bhr.getLocation());
        int power = redstoneInspector ? mc.level.getBestNeighborSignal(bp) : 0;

        String text = st.getBlock().getName().getString() + " • " + String.format("%.1fм", dist);
        if (redstoneInspector && power > 0) {
            text += " • RS:" + power;
        }
        int tw = mc.font.width(text) + 12;
        int x = (sw - tw) / 2;
        int y = sh / 2 + 24;
        fillRoundedRect(g, x, y, tw, 15, 0xCC090E14);
        drawRoundedBorder(g, x, y, tw, 15, power > 0 ? 0xFFEF4444 : currentTheme.borderMuted);
        g.drawString(mc.font, text, x + 6, y + 4, 0xFFE2E8F0);
    }

    private static void renderCombatSurvivalHud(GuiGraphics g, Minecraft mc, int sw, int sh) {
        Player p = mc.player;
        if (p == null) {
            return;
        }
        int totems = countPlayerTotems(p);
        boolean showDeath = deathReplayMarker && lastDeathPos != null;
        int w = 134;
        int h = (armorDurabilityHud ? 44 : 22) + (showDeath ? 12 : 0);
        int x = sw - w - 10;
        int y = sh - h - 10;
        fillRoundedRect(g, x, y, w, h, currentTheme.bgWindow);
        drawRoundedBorder(g, x, y, w, h, currentTheme.borderMuted);

        String tText = "Тотемы: " + totems + "  |  HP: " + (int) Math.ceil(p.getHealth());
        g.drawString(mc.font, trimText(mc.font, tText, w - 12), x + 6, y + 6, totems > 0 ? 0xFFFDE047 : 0xFFE2E8F0);

        if (armorDurabilityHud) {
            EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
            String[] labels = {"Ш", "Н", "П", "Б"};
            int bx = x + 6;
            int by = y + 20;
            for (int i = 0; i < slots.length; i++) {
                ItemStack armor = p.getItemBySlot(slots[i]);
                int pct = 0;
                if (!armor.isEmpty() && armor.isDamageableItem() && armor.getMaxDamage() > 0) {
                    pct = (int) Math.round(((armor.getMaxDamage() - armor.getDamageValue()) * 100.0) / armor.getMaxDamage());
                } else if (!armor.isEmpty()) {
                    pct = 100;
                }
                int barCol = pct > 55 ? 0xFF4ADE80 : (pct > 25 ? 0xFFFACC15 : (pct > 0 ? 0xFFEF4444 : 0xFF334155));
                g.drawString(mc.font, labels[i], bx + i * 30, by, 0xFF94A3B8);
                g.fill(bx + i * 30, by + 11, bx + i * 30 + 25, by + 15, 0xFF1E293B);
                if (pct > 0) {
                    int fillW = Math.max(2, (25 * pct) / 100);
                    g.fill(bx + i * 30, by + 11, bx + i * 30 + fillW, by + 15, barCol);
                }
            }
        }

        if (showDeath) {
            int dDist = (int) Math.sqrt(p.blockPosition().distSqr(lastDeathPos));
            g.drawString(mc.font, trimText(mc.font, "☠ Смерть: " + dDist + "м", w - 12), x + 6, y + h - 12, 0xFFFCA5A5);
        }

        if (trajectoriesEnabled && p.isUsingItem()) {
            ItemStack use = p.getUseItem();
            if (use.is(Items.BOW) || use.is(Items.CROSSBOW) || use.is(Items.TRIDENT)) {
                int charge = Math.min(20, p.getTicksUsingItem());
                int bw = 54;
                int bx = (sw - bw) / 2;
                int by = sh / 2 + 12;
                g.fill(bx, by, bx + bw, by + 4, 0xCC090E14);
                g.fill(bx, by, bx + (bw * charge) / 20, by + 4, charge >= 20 ? 0xFF4ADE80 : currentTheme.accent);
            }
        }
    }

    private static void renderLitematicaMiniHud(GuiGraphics g, Minecraft mc, int sw) {
        Player p = mc.player;
        if (p == null) {
            return;
        }
        ItemStack hand = p.getMainHandItem();
        if (hand.isEmpty() && !layerSliceMode) {
            return;
        }
        int totalInInv = 0;
        if (!hand.isEmpty()) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack st = p.getInventory().getItem(i);
                if (!st.isEmpty() && st.getItem() == hand.getItem()) {
                    totalInInv += st.getCount();
                }
            }
        }
        int w = 138;
        int h = 30;
        int x = radarRightSide ? 10 : (sw - w - 10);
        int y = 32;
        fillRoundedRect(g, x, y, w, h, currentTheme.bgWindow);
        drawRoundedBorder(g, x, y, w, h, currentTheme.borderMuted);

        String itemLabel = hand.isEmpty() ? "Нет блока в руке" : hand.getHoverName().getString() + " x" + totalInInv;
        int itemCol = (deficitHighlight && !hand.isEmpty() && totalInInv < 16) ? 0xFFFCA5A5 : 0xFFE2E8F0;
        g.drawString(mc.font, trimText(mc.font, itemLabel, w - 10), x + 6, y + 5, itemCol);
        String layerText = "Слой Y: " + (int) p.getY() + (layerSliceMode ? " [Цель: " + targetBuildLayerY + "]" : " [Все]");
        g.drawString(mc.font, trimText(mc.font, layerText, w - 10), x + 6, y + 17, currentTheme.secondary);
    }

    private static void renderSpotifyMiniHud(GuiGraphics g, Minecraft mc, int sw, int sh) {
        int w = 174;
        int h = 30;
        int x = 10;
        int y = sh - h - 10;
        fillRoundedRect(g, x, y, w, h, currentTheme.bgWindow);
        drawRoundedBorder(g, x, y, w, h, spotifyIsPlaying ? 0xFF1DB954 : 0xFF475569);

        // Анимированный эквалайзер Spotify (двигается когда реально играет музыка!)
        long t = System.currentTimeMillis() / 120L;
        for (int i = 0; i < 4; i++) {
            int bh = spotifyIsPlaying
                ? (4 + (int) (Math.abs(Math.sin(t * 0.75 + i * 1.4)) * 13))
                : 3;
            int barCol = spotifyIsPlaying ? 0xFF1DB954 : 0xFF64748B;
            g.fill(x + 7 + i * 4, y + h - 6 - bh, x + 9 + i * 4, y + h - 6, barCol);
        }
        g.drawString(mc.font, trimText(mc.font, spotifyCurrentTrack, w - 32), x + 26, y + 5, 0xFFF8FAFC);
        g.drawString(mc.font, trimText(mc.font, spotifyCurrentArtist, w - 32), x + 26, y + 16, spotifyIsPlaying ? 0xFF4ADE80 : 0xFF94A3B8);
    }

    // =========================================================================
    // GPU-УТИЛИТЫ И КАСТОМНЫЕ ИКОНКИ
    // =========================================================================

    private static void fillRoundedRect(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 4 || h <= 4) {
            g.fill(x, y, x + w, y + h, color);
            return;
        }
        g.fill(x + 2, y, x + w - 2, y + 1, color);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, color);
        g.fill(x, y + 2, x + w, y + h - 2, color);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, color);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
    }

    private static void drawRoundedBorder(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 4 || h <= 4) {
            return;
        }
        g.fill(x + 2, y, x + w - 2, y + 1, color);
        g.fill(x + 2, y + h - 1, x + w - 2, y + h, color);
        g.fill(x, y + 2, x + 1, y + h - 2, color);
        g.fill(x + w - 1, y + 2, x + w, y + h - 2, color);
        g.fill(x + 1, y + 1, x + 2, y + 2, color);
        g.fill(x + w - 2, y + 1, x + w - 1, y + 2, color);
        g.fill(x + 1, y + h - 2, x + 2, y + h - 1, color);
        g.fill(x + w - 2, y + h - 2, x + w - 1, y + h - 1, color);
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >>> 24) & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF;
        int g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int adjustBrightness(int rgb, int delta) {
        int r = Mth.clamp(((rgb >> 16) & 0xFF) + delta, 0, 255);
        int g = Mth.clamp(((rgb >> 8) & 0xFF) + delta, 0, 255);
        int b = Mth.clamp((rgb & 0xFF) + delta, 0, 255);
        return (r << 16) | (g << 8) | b;
    }

    private static String trimText(Font font, String text, int maxWidth) {
        if (text == null || maxWidth <= 8) {
            return "";
        }
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String dots = "..";
        int limit = maxWidth - font.width(dots);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            sb.append(text.charAt(i));
            if (font.width(sb.toString()) > limit) {
                sb.setLength(Math.max(0, sb.length() - 1));
                break;
            }
        }
        return sb + dots;
    }

    private static void drawTabIcon(GuiGraphics g, MenuTab tab, int x, int y, int col) {
        switch (tab) {
            case VADIMKA -> {
                g.fill(x + 1, y + 1, x + 4, y + 4, col);
                g.fill(x + 6, y + 1, x + 9, y + 4, col);
                g.fill(x + 2, y + 3, x + 8, y + 9, col);
                g.fill(x + 3, y + 5, x + 4, y + 6, 0xFF0A0F14);
                g.fill(x + 6, y + 5, x + 7, y + 6, 0xFF0A0F14);
            }
            case AUTOPILOT -> {
                g.fill(x + 4, y + 1, x + 6, y + 9, col);
                g.fill(x + 3, y + 2, x + 7, y + 4, col);
                g.fill(x + 2, y + 4, x + 8, y + 6, col);
            }
            case AUTO_BUILD -> {
                g.fill(x + 1, y + 2, x + 5, y + 5, col);
                g.fill(x + 6, y + 2, x + 9, y + 5, col);
                g.fill(x + 1, y + 6, x + 3, y + 9, col);
                g.fill(x + 4, y + 6, x + 9, y + 9, col);
            }
            case LITEMATICA_HUD -> {
                drawRoundedBorder(g, x + 1, y + 1, 8, 8, col);
                g.fill(x + 4, y + 1, x + 5, y + 9, col);
                g.fill(x + 1, y + 4, x + 9, y + 5, col);
            }
            case VISUAL_RADAR -> {
                drawRoundedBorder(g, x + 1, y + 1, 8, 8, col);
                g.fill(x + 4, y + 4, x + 6, y + 6, col);
                g.fill(x + 5, y + 2, x + 8, y + 3, col);
            }
            case CHESTS_VILLAGERS -> {
                g.fill(x + 1, y + 2, x + 9, y + 9, col);
                g.fill(x + 1, y + 4, x + 9, y + 5, 0xFF0A0F14);
                g.fill(x + 4, y + 4, x + 6, y + 7, 0xFFFDE047);
            }
            case COMBAT_PVP -> {
                g.fill(x + 7, y + 1, x + 9, y + 3, col);
                g.fill(x + 5, y + 3, x + 7, y + 5, col);
                g.fill(x + 3, y + 5, x + 5, y + 7, col);
                g.fill(x + 1, y + 7, x + 3, y + 9, col);
                g.fill(x + 2, y + 4, x + 6, y + 5, col);
            }
            case MUSIC_AFK -> {
                g.fill(x + 4, y + 1, x + 9, y + 3, col);
                g.fill(x + 4, y + 2, x + 5, y + 8, col);
                g.fill(x + 8, y + 2, x + 9, y + 7, col);
                g.fill(x + 2, y + 6, x + 5, y + 9, col);
                g.fill(x + 6, y + 5, x + 9, y + 8, col);
            }
            case CUSTOM_VISUAL -> {
                g.fill(x + 1, y + 3, x + 3, y + 8, col);
                g.fill(x + 4, y + 2, x + 6, y + 8, col);
                g.fill(x + 7, y + 3, x + 9, y + 8, col);
                g.fill(x + 1, y + 6, x + 9, y + 8, col);
            }
        }
    }

    private static LivingEntity getOrCreateStudioCompanion(ClientLevel level, Player player) {
        if (currentForm == VadimkaForm.PLAYER) {
            return player;
        }
        if (studioPreviewMob == null || studioPreviewForm != currentForm || studioPreviewMob.level() != level) {
            Mob m = currentForm.entityType.create(level, EntitySpawnReason.LOAD);
            if (m != null) {
                m.setNoAi(true);
                m.setSilent(true);
                m.setCustomNameVisible(false);
                studioPreviewMob = m;
                studioPreviewForm = currentForm;
            }
        }
        return studioPreviewMob;
    }

    // =========================================================================
    // ГЛАВНОЕ ПРЕМИУМ-МЕНЮ [P] (ЖИВОЙ ЧАТ, ЛАРЧИКИ, ЖИТЕЛИ, SPOTIFY, 3D-СТУДИЯ)
    // =========================================================================

    public static class AssistantVadimkaScreen extends Screen {
        private MenuTab activeTab;
        private int currentPage = 0;
        private int totalPages = 1;

        // Подвкладки для богатых разделов
        private int vadimkaSubTab = 0; // 0 = Настройки, 1 = AI-Чат с Вадимкой
        private int chestsSubTab = 0;  // 0 = Ларчики (поиск лута), 1 = Жители (торги), 2 = Настройки
        private int musicSubTab = 0;   // 0 = Spotify Connect (ПК), 1 = AFK Мини-игры

        private EditBox activeInputBox = null;
        private String chestSearchQuery = "";
        private String pendingChatDraft = "";

        private static class TabVisual {
            final MenuTab tab;
            final int x, y, w, h;

            TabVisual(MenuTab tab, int x, int y, int w, int h) {
                this.tab = tab;
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
            }
        }

        private static class SettingRowVisual {
            final int x, y, w, h;
            final int btnX, btnY, btnW, btnH;
            final String title;
            final String subtitle;
            final boolean isToggle;
            final boolean toggleState;
            final String valueLabel;

            SettingRowVisual(int x, int y, int w, int h,
                             int btnX, int btnY, int btnW, int btnH,
                             String title, String subtitle,
                             boolean isToggle, boolean toggleState, String valueLabel) {
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
                this.btnX = btnX;
                this.btnY = btnY;
                this.btnW = btnW;
                this.btnH = btnH;
                this.title = title;
                this.subtitle = subtitle;
                this.isToggle = isToggle;
                this.toggleState = toggleState;
                this.valueLabel = valueLabel;
            }
        }

        private static class SmallBtnVisual {
            final int x, y, w, h;
            final String label;
            final boolean danger;
            final boolean highlighted;
            final boolean spotifyGreen;

            SmallBtnVisual(int x, int y, int w, int h, String label, boolean danger, boolean highlighted, boolean spotifyGreen) {
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
                this.label = label;
                this.danger = danger;
                this.highlighted = highlighted;
                this.spotifyGreen = spotifyGreen;
            }
        }

        private static class RowSpec {
            final String title;
            final String subtitle;
            final boolean isToggle;
            final boolean state;
            final String valueText;
            final Runnable action;

            RowSpec(String title, String subtitle, boolean isToggle, boolean state, String valueText, Runnable action) {
                this.title = title;
                this.subtitle = subtitle;
                this.isToggle = isToggle;
                this.state = state;
                this.valueText = valueText;
                this.action = action;
            }
        }

        private final List<TabVisual> tabVisuals = new ArrayList<>();
        private final List<SettingRowVisual> rowVisuals = new ArrayList<>();
        private final List<SmallBtnVisual> smallBtnVisuals = new ArrayList<>();

        // Состояние 3 играбельных AFK мини-игр
        public boolean afkGameRunning = false;
        private int afkScore = 0;
        private int afkBestScore = 0;
        private int afkTick = 0;
        private int runnerLane = 1;
        private final List<int[]> runnerItems = new ArrayList<>();
        private double sonicY = 0.0;
        private double sonicVel = 0.0;
        private final List<int[]> sonicObstacles = new ArrayList<>();
        private int doomPlayerX = 50;
        private final List<int[]> doomBullets = new ArrayList<>();
        private final List<int[]> doomEnemies = new ArrayList<>();
        private final Random gameRand = new Random();

        private int winX, winY, winW, winH;
        private int sidebarW, previewW, contentX, contentW;

        public AssistantVadimkaScreen(MenuTab activeTab) {
            super(Component.literal("ASSISTANT VADIMKA"));
            this.activeTab = activeTab;
        }

        @Override
        protected void init() {
            this.tabVisuals.clear();
            this.rowVisuals.clear();
            this.smallBtnVisuals.clear();
            this.activeInputBox = null;

            this.winW = Math.min(this.width - 14, 650);
            this.winH = Math.min(this.height - 14, 356);
            this.winX = (this.width - this.winW) / 2;
            this.winY = (this.height - this.winH) / 2;

            this.sidebarW = 122;
            this.previewW = 178;
            this.contentX = this.winX + this.sidebarW + 8;
            this.contentW = this.winW - this.sidebarW - this.previewW - 22;

            // 1. Левая панель — все 9 вкладок
            MenuTab[] tabs = MenuTab.values();
            int tabH = 22;
            int tabStartY = this.winY + 38;
            for (int i = 0; i < tabs.length; i++) {
                MenuTab tab = tabs[i];
                int tx = this.winX + 7;
                int ty = tabStartY + i * (tabH + 4);
                int tw = this.sidebarW - 14;
                this.tabVisuals.add(new TabVisual(tab, tx, ty, tw, tabH));
                addInvisibleHitbox(tx, ty, tw, tabH, () -> {
                    this.activeTab = tab;
                    this.currentPage = 0;
                    if (tab != MenuTab.MUSIC_AFK) {
                        this.afkGameRunning = false;
                    }
                    this.rebuildWidgets();
                });
            }

            // Быстрая смена темы внизу сайдбара
            int themeBtnY = this.winY + this.winH - 44;
            addSmallButton(this.winX + 7, themeBtnY, this.sidebarW - 14, 18, "Тема: " + trimShortTheme(currentTheme), false, true, false, () -> {
                MenuTheme[] themes = MenuTheme.values();
                currentTheme = themes[(currentTheme.ordinal() + 1) % themes.length];
                this.rebuildWidgets();
            });

            // Закрытие окна
            addSmallButton(this.winX + this.winW - 26, this.winY + 7, 19, 18, "✕", true, false, false, this::onClose);

            // Кнопки режима 3D-студии справа
            int prevX = this.winX + this.winW - this.previewW - 6;
            int prevCtrlY = this.winY + 186;
            int btn3W = (this.previewW - 24) / 3;
            PreviewMode[] modes = PreviewMode.values();
            for (int i = 0; i < modes.length; i++) {
                PreviewMode m = modes[i];
                addSmallButton(prevX + 8 + i * (btn3W + 4), prevCtrlY, btn3W, 17, m.label, false, previewMode == m, false, () -> {
                    previewMode = m;
                    this.rebuildWidgets();
                });
            }

            // 2. Построение центральной области (с подвкладками для Вадимки, Ларчиков/Жителей и Spotify/AFK!)
            if (this.activeTab == MenuTab.VADIMKA) {
                initVadimkaTab();
            } else if (this.activeTab == MenuTab.CHESTS_VILLAGERS) {
                initChestsAndVillagersTab();
            } else if (this.activeTab == MenuTab.MUSIC_AFK) {
                initMusicAndAfkTab();
            } else {
                buildPagedRows(buildTabSpecs(this.activeTab), this.winY + 62, 7);
            }
        }

        private void initVadimkaTab() {
            int subY = this.winY + 58;
            int halfW = (this.contentW - 20) / 2;
            addSmallButton(this.contentX + 7, subY, halfW, 18, "Настройки и Миньоны", false, vadimkaSubTab == 0, false, () -> {
                vadimkaSubTab = 0;
                currentPage = 0;
                this.rebuildWidgets();
            });
            addSmallButton(this.contentX + 13 + halfW, subY, halfW, 18, "💬 Чат с Вадимкой", false, vadimkaSubTab == 1, false, () -> {
                vadimkaSubTab = 1;
                this.rebuildWidgets();
            });

            if (vadimkaSubTab == 0) {
                buildPagedRows(buildTabSpecs(MenuTab.VADIMKA), this.winY + 82, 6);
            } else {
                this.totalPages = 1;
                // Быстрые кнопки-команды в чате с Вадимкой
                int chipY = this.winY + 82;
                int cw = (this.contentW - 26) / 4;
                String[] chips = {"Помощь", "Статус", "Шутка", "Spotify"};
                String[] cmds = {"помощь", "статус", "шутка", "что играет в spotify"};
                for (int i = 0; i < chips.length; i++) {
                    String cmd = cmds[i];
                    addSmallButton(this.contentX + 7 + i * (cw + 4), chipY, cw, 17, chips[i], false, false, i == 3, () -> {
                        sendPromptToVadimkaFromMenu(cmd);
                        this.rebuildWidgets();
                    });
                }

                // Поле ввода сообщения Вадимке прямо внутри меню
                int inputY = this.winY + this.winH - 46;
                int sendBtnW = 68;
                int boxW = this.contentW - sendBtnW - 20;
                EditBox chatBox = new EditBox(this.font, this.contentX + 8, inputY, boxW, 18, Component.literal("Напиши Вадимке"));
                chatBox.setMaxLength(110);
                chatBox.setValue(pendingChatDraft);
                chatBox.setHint(Component.literal("Спроси Вадимку (Enter — отправить)..."));
                chatBox.setTextColor(0xFFF8FAFC);
                chatBox.setResponder(s -> pendingChatDraft = s);
                this.addRenderableWidget(chatBox);
                this.activeInputBox = chatBox;

                addSmallButton(this.contentX + 12 + boxW, inputY, sendBtnW, 18, "Отправить", false, true, false, () -> {
                    if (!pendingChatDraft.trim().isEmpty()) {
                        sendPromptToVadimkaFromMenu(pendingChatDraft);
                        pendingChatDraft = "";
                        this.rebuildWidgets();
                    }
                });
            }
        }

        private void initChestsAndVillagersTab() {
            int subY = this.winY + 58;
            int subW = (this.contentW - 22) / 3;
            addSmallButton(this.contentX + 7, subY, subW, 18, "Ларчики (" + rememberedChests.size() + ")", false, chestsSubTab == 0, false, () -> {
                chestsSubTab = 0;
                currentPage = 0;
                this.rebuildWidgets();
            });
            addSmallButton(this.contentX + 11 + subW, subY, subW, 18, "Жители (" + rememberedVillagers.size() + ")", false, chestsSubTab == 1, false, () -> {
                chestsSubTab = 1;
                currentPage = 0;
                this.rebuildWidgets();
            });
            addSmallButton(this.contentX + 15 + subW * 2, subY, subW, 18, "Настройки", false, chestsSubTab == 2, false, () -> {
                chestsSubTab = 2;
                currentPage = 0;
                this.rebuildWidgets();
            });

            if (chestsSubTab == 0) {
                // Поиск по содержимому Ларчиков
                int searchY = this.winY + 81;
                int findW = 48;
                int clearW = 48;
                int boxW = this.contentW - findW - clearW - 24;
                EditBox searchBox = new EditBox(this.font, this.contentX + 8, searchY, boxW, 17, Component.literal("Поиск по ларчикам"));
                searchBox.setMaxLength(48);
                searchBox.setValue(chestSearchQuery);
                searchBox.setHint(Component.literal("Поиск в ларчиках (напр. алмаз)..."));
                searchBox.setTextColor(0xFFF8FAFC);
                searchBox.setResponder(s -> {
                    if (!s.equals(chestSearchQuery)) {
                        chestSearchQuery = s;
                        currentPage = 0;
                    }
                });
                this.addRenderableWidget(searchBox);
                this.activeInputBox = searchBox;

                addSmallButton(this.contentX + 12 + boxW, searchY, findW, 17, "Найти", false, true, false, () -> {
                    currentPage = 0;
                    this.rebuildWidgets();
                });
                addSmallButton(this.contentX + 16 + boxW + findW, searchY, clearW, 17, "Сброс", false, false, false, () -> {
                    chestSearchQuery = "";
                    currentPage = 0;
                    this.rebuildWidgets();
                });

                List<RowSpec> chestRows = buildChestMemoryRows();
                buildPagedRows(chestRows, this.winY + 104, 5);
            } else if (chestsSubTab == 1) {
                List<RowSpec> villagerRows = buildVillagerTradeRows();
                buildPagedRows(villagerRows, this.winY + 82, 6);
            } else {
                buildPagedRows(buildTabSpecs(MenuTab.CHESTS_VILLAGERS), this.winY + 82, 6);
            }
        }

        private List<RowSpec> buildChestMemoryRows() {
            List<RowSpec> rows = new ArrayList<>();
            Minecraft mc = Minecraft.getInstance();
            Player p = mc.player;
            String q = chestSearchQuery == null ? "" : chestSearchQuery.trim().toLowerCase(Locale.ROOT);

            List<ChestRecord> sorted = new ArrayList<>(rememberedChests.values());
            if (p != null) {
                BlockPos pPos = p.blockPosition();
                sorted.sort(Comparator.comparingDouble(c -> c.pos.distSqr(pPos)));
            }

            for (ChestRecord cr : sorted) {
                String matchInfo = cr.summaryLine();
                if (!q.isEmpty()) {
                    String found = null;
                    for (Map.Entry<String, Integer> e : cr.items.entrySet()) {
                        if (e.getKey().toLowerCase(Locale.ROOT).contains(q)) {
                            found = e.getKey() + " x" + e.getValue();
                            break;
                        }
                    }
                    if (found == null && !cr.containerName.toLowerCase(Locale.ROOT).contains(q)) {
                        continue;
                    }
                    if (found != null) {
                        matchInfo = "Найдено: " + found;
                    }
                }
                int dist = p != null ? (int) Math.sqrt(p.blockPosition().distSqr(cr.pos)) : 0;
                String title = cr.containerName + " [" + cr.pos.getX() + ", " + cr.pos.getY() + ", " + cr.pos.getZ() + "] (" + dist + "м)";
                rows.add(cycleSpec(title, matchInfo, "✦ Метка", () -> {
                    activeNavTarget = cr.pos;
                    activeNavLabel = cr.containerName;
                    pushVadimkaToast("Метка на ларчик поставлена (" + dist + "м)!", 60);
                }));
            }

            if (rows.isEmpty()) {
                rows.add(cycleSpec(
                    rememberedChests.isEmpty() ? "Пока нет открытых ларчиков" : "Ничего не найдено по запросу",
                    "Открой любой сундук/бочку в мире — я запомню все вещи!",
                    "Понятно",
                    () -> pushVadimkaToast("Открой сундук ПКМ, чтобы я его просканировал!", 60)
                ));
            }
            return rows;
        }

        private List<RowSpec> buildVillagerTradeRows() {
            List<RowSpec> rows = new ArrayList<>();
            Minecraft mc = Minecraft.getInstance();
            Player p = mc.player;
            int emeralds = p != null ? countPlayerEmeralds(p) : 0;

            rows.add(cycleSpec(
                "Баланс изумрудов: " + emeralds + " шт.",
                "Открой окно торговли с жителем — я запишу все книги и цены!",
                "Очистить",
                () -> {
                    rememberedVillagers.clear();
                    pushVadimkaToast("База торгов жителей очищена", 50);
                }
            ));

            List<VillagerRecord> sorted = new ArrayList<>(rememberedVillagers.values());
            if (p != null) {
                BlockPos pPos = p.blockPosition();
                sorted.sort(Comparator.comparingDouble(v -> v.pos.distSqr(pPos)));
            }

            for (VillagerRecord vr : sorted) {
                int dist = p != null ? (int) Math.sqrt(p.blockPosition().distSqr(vr.pos)) : 0;
                for (VillagerTradeEntry tr : vr.trades) {
                    String prefix = tr.highlighted ? "★ " : "• ";
                    String status = tr.outOfStock ? " [Нет в наличии]" : (" (" + dist + "м)");
                    rows.add(cycleSpec(
                        prefix + tr.resultText,
                        "Цена: " + tr.costText + status,
                        "✦ К жителю",
                        () -> {
                            activeNavTarget = vr.pos;
                            activeNavLabel = "Житель";
                            pushVadimkaToast("Метка на жителя (" + dist + "м)!", 60);
                        }
                    ));
                }
            }
            return rows;
        }

        private void initMusicAndAfkTab() {
            int subY = this.winY + 58;
            int halfW = (this.contentW - 20) / 2;
            addSmallButton(this.contentX + 7, subY, halfW, 18, "♫ Spotify Connect (ПК)", false, musicSubTab == 0, musicSubTab == 0, () -> {
                musicSubTab = 0;
                afkGameRunning = false;
                pollSpotifyAsync(true);
                this.rebuildWidgets();
            });
            addSmallButton(this.contentX + 13 + halfW, subY, halfW, 18, "🎮 AFK Мини-игры (3)", false, musicSubTab == 1, false, () -> {
                musicSubTab = 1;
                this.rebuildWidgets();
            });

            if (musicSubTab == 0) {
                this.totalPages = 1;
                pollSpotifyAsync(false);
                // Реальные кнопки управления Spotify Desktop на Windows!
                int ctrlY = this.winY + 148;
                int bw = (this.contentW - 30) / 5;
                addSmallButton(this.contentX + 7, ctrlY, bw, 22, "⏮ Пред", false, false, false, () -> sendWindowsMediaKey(0xB1));
                addSmallButton(this.contentX + 11 + bw, ctrlY, bw, 22, spotifyIsPlaying ? "⏸ Пауза" : "▶ Плей", false, true, true, () -> sendWindowsMediaKey(0xB3));
                addSmallButton(this.contentX + 15 + bw * 2, ctrlY, bw, 22, "⏭ След", false, false, false, () -> sendWindowsMediaKey(0xB0));
                addSmallButton(this.contentX + 19 + bw * 3, ctrlY, bw, 22, "🔉 Тише", false, false, false, () -> sendWindowsMediaKey(0xAE));
                addSmallButton(this.contentX + 23 + bw * 4, ctrlY, bw, 22, "🔊 Громче", false, false, false, () -> sendWindowsMediaKey(0xAF));

                List<RowSpec> spRows = new ArrayList<>();
                spRows.add(toggleSpec("Spotify HUD на экране", "Показывает реальный трек и эквалайзер", spotifyHudEnabled, () -> spotifyHudEnabled = !spotifyHudEnabled));
                spRows.add(toggleSpec("Живая синхронизация ПК", "Читает трек напрямую из Spotify.exe", spotifyLiveSync, () -> {
                    spotifyLiveSync = !spotifyLiveSync;
                    if (spotifyLiveSync) pollSpotifyAsync(true);
                }));
                spRows.add(cycleSpec("Управление приложением", spotifyAppRunning ? "Spotify запущен на ПК" : "Нажми, чтобы открыть Spotify", "Открыть", AssistantVadimkaClient::launchSpotifyDesktop));
                buildPagedRows(spRows, this.winY + 180, 4);
            } else {
                List<RowSpec> afkRows = new ArrayList<>();
                afkRows.add(cycleSpec("Выбор MC мини-игры", selectedAfkGame.hint, selectedAfkGame.title, () -> {
                    AfkGameType[] gs = AfkGameType.values();
                    selectedAfkGame = gs[(selectedAfkGame.ordinal() + 1) % gs.length];
                    resetAfkGameState();
                }));
                afkRows.add(cycleSpec("Запуск AFK-игры", "Управление: A / D / SPACE / ESC", afkGameRunning ? "■ СТОП" : "▶ ИГРАТЬ", () -> {
                    afkGameRunning = !afkGameRunning;
                    if (afkGameRunning) {
                        resetAfkGameState();
                    }
                }));
                afkRows.add(toggleSpec("Экстренный выход при тревоге", "Закроет игру при уроне или мобе рядом", afkPanicExit, () -> afkPanicExit = !afkPanicExit));
                buildPagedRows(afkRows, this.winY + 82, 3);
            }
        }

        private void buildPagedRows(List<RowSpec> specs, int startY, int maxPerPage) {
            this.totalPages = Math.max(1, (specs.size() + maxPerPage - 1) / maxPerPage);
            this.currentPage = Mth.clamp(this.currentPage, 0, this.totalPages - 1);

            if (this.totalPages > 1) {
                int navY = this.winY + 39;
                addSmallButton(this.contentX + this.contentW - 68, navY, 20, 16, "◀", false, false, false, () -> {
                    this.currentPage = (this.currentPage - 1 + this.totalPages) % this.totalPages;
                    this.rebuildWidgets();
                });
                addSmallButton(this.contentX + this.contentW - 26, navY, 20, 16, "▶", false, false, false, () -> {
                    this.currentPage = (this.currentPage + 1) % this.totalPages;
                    this.rebuildWidgets();
                });
            }

            int startIdx = this.currentPage * maxPerPage;
            int endIdx = Math.min(specs.size(), startIdx + maxPerPage);
            int rowY = startY;

            for (int i = startIdx; i < endIdx; i++) {
                RowSpec spec = specs.get(i);
                int rx = this.contentX + 7;
                int rw = this.contentW - 14;
                int rh = 28;
                int btnW = spec.isToggle ? 54 : 88;
                int btnH = 18;
                int btnX = rx + rw - btnW - 6;
                int btnY = rowY + (rh - btnH) / 2;

                this.rowVisuals.add(new SettingRowVisual(
                    rx, rowY, rw, rh, btnX, btnY, btnW, btnH,
                    spec.title, spec.subtitle, spec.isToggle, spec.state, spec.valueText
                ));
                addInvisibleHitbox(btnX, btnY, btnW, btnH, () -> {
                    spec.action.run();
                    this.rebuildWidgets();
                });
                rowY += rh + 5;
            }
        }

        private String trimShortTheme(MenuTheme theme) {
            return switch (theme) {
                case CYBER_EMERALD -> "Emerald";
                case NETHER_CRIMSON -> "Crimson";
                case AMETHYST_VOID -> "Void";
                case ARCTIC_FROST -> "Frost";
                case OBSIDIAN_GOLD -> "Gold";
            };
        }

        // addWidget регистрирует клики БЕЗ отрисовки стандартных серых кнопок Minecraft!
        private void addInvisibleHitbox(int x, int y, int w, int h, Runnable action) {
            this.addWidget(Button.builder(Component.empty(), b -> action.run()).bounds(x, y, w, h).build());
        }

        private void addSmallButton(int x, int y, int w, int h, String label,
                                    boolean danger, boolean highlighted, boolean spotifyGreen, Runnable action) {
            this.smallBtnVisuals.add(new SmallBtnVisual(x, y, w, h, label, danger, highlighted, spotifyGreen));
            addInvisibleHitbox(x, y, w, h, action);
        }

        private List<RowSpec> buildTabSpecs(MenuTab tab) {
            List<RowSpec> list = new ArrayList<>();
            switch (tab) {
                case VADIMKA -> {
                    list.add(toggleSpec("3D-спутник в мире", "Спавн Вадимки рядом с игроком", companionVisibleInWorld, () -> companionVisibleInWorld = !companionVisibleInWorld));
                    list.add(cycleSpec("Облик Вадимки", "6 живых 3D-моделей", currentForm.shortName, () -> {
                        VadimkaForm[] f = VadimkaForm.values();
                        currentForm = f[(currentForm.ordinal() + 1) % f.length];
                    }));
                    list.add(cycleSpec("Размер в мире", "Масштаб 3D-модели Вадимки", companionScalePercent + "%", () -> {
                        companionScalePercent = companionScalePercent >= 140 ? 60 : companionScalePercent + 20;
                    }));
                    list.add(toggleSpec("Ответы Вадимки в чате", "Пиши «Вадимка ...» или «!помощь»", chatAssistantEnabled, () -> chatAssistantEnabled = !chatAssistantEnabled));
                    list.add(toggleSpec("Команда миньонов (4)", "Летающие помощники вокруг тебя", minionsVisibleInWorld, () -> minionsVisibleInWorld = !minionsVisibleInWorld));
                    list.add(toggleSpec("Умный AI миньонов", "Летят к луту, врагам и блокам", minionsSmartAi, () -> minionsSmartAi = !minionsSmartAi));
                    list.add(toggleSpec("Компактные теги имён", "Не загораживают экран в F5", compactNameplates, () -> compactNameplates = !compactNameplates));
                    list.add(toggleSpec("Тихий режим", "Убирает уведомления и шум", quietMode, () -> quietMode = !quietMode));
                    list.add(toggleSpec("Комментатор событий", "Реакции на урон и опасности", voiceCommentator, () -> voiceCommentator = !voiceCommentator));
                    list.add(toggleSpec("Режим рандомной дичи", "Шутки и пасхалки от Вадимки", randomChaosMode, () -> randomChaosMode = !randomChaosMode));
                    for (MinionRole role : MinionRole.values()) {
                        boolean on = Boolean.TRUE.equals(activeMinions.get(role));
                        list.add(toggleSpec("Миньон: " + role.title, "Роль в команде помощников", on, () -> {
                            activeMinions.put(role, !Boolean.TRUE.equals(activeMinions.get(role)));
                        }));
                    }
                }
                case AUTOPILOT -> {
                    String distStat = "Пройдено за сессию: " + (int) sessionDistanceWalked + " бл.";
                    list.add(toggleSpec("Профайлер стиля игры", distStat, styleProfiler, () -> styleProfiler = !styleProfiler));
                    list.add(toggleSpec("Безопасный автопилот", activeNavTarget != null ? ("Ведёт к: " + activeNavLabel) : "Сначала задай Дом или Метку", safeAutopilot, () -> {
                        if (!safeAutopilot && activeNavTarget == null && homeWaypoint == null) {
                            pushVadimkaToast("Сначала задай точку дома в 6-й вкладке или «!дом»!", 70);
                        } else {
                            safeAutopilot = !safeAutopilot;
                        }
                    }));
                    list.add(toggleSpec("Ассистент спринта", "Авто-бег при зажатой клавише W", sprintAssist, () -> sprintAssist = !sprintAssist));
                    list.add(toggleSpec("Мгновенный возврат", "Отключает автопилот по S/A/D/Shift", autoReturnControl, () -> autoReturnControl = !autoReturnControl));
                    list.add(toggleSpec("Паркур-страховка края", "Предупреждение на краю блока", parkourEdgeGuard, () -> parkourEdgeGuard = !parkourEdgeGuard));
                    list.add(toggleSpec("Сигнал обрыва (HUD)", "Тревога при обрыве >= 6 блоков", fallDangerAlert, () -> fallDangerAlert = !fallDangerAlert));
                }
                case AUTO_BUILD -> {
                    list.add(toggleSpec("3D-линейка замеров", "Блок и дистанция под прицелом", rulerOverlay, () -> rulerOverlay = !rulerOverlay));
                    list.add(toggleSpec("Инспектор редстоуна", "Показ силы сигнала RS на блоке", redstoneInspector, () -> redstoneInspector = !redstoneInspector));
                    list.add(toggleSpec("Страж прочности кирки", "Сигнал когда инструмент < 12%", toolDurabilityAlert, () -> toolDurabilityAlert = !toolDurabilityAlert));
                    list.add(toggleSpec("Авто-постройка по схеме", "Помощь по активной Litematica", litematicaAutoBuild, () -> litematicaAutoBuild = !litematicaAutoBuild));
                    list.add(toggleSpec("Умная реставрация", "Подсказка сломанных блоков", smartRestoration, () -> smartRestoration = !smartRestoration));
                    list.add(toggleSpec("Авто-пополнение хотбара", "Контроль заканчивающихся стаков", hotbarAutoRefill, () -> hotbarAutoRefill = !hotbarAutoRefill));
                }
                case LITEMATICA_HUD -> {
                    list.add(toggleSpec("Виджет материалов HUD", "Счётчик блоков в руке и сумке", litematicaHudEnabled, () -> litematicaHudEnabled = !litematicaHudEnabled));
                    list.add(toggleSpec("Сканер шалкеров", "Учёт ресурсов внутри шалкеров", shulkerScanner, () -> shulkerScanner = !shulkerScanner));
                    list.add(toggleSpec("Разрез по слоям (Y)", "Отслеживание выбранной высоты", layerSliceMode, () -> layerSliceMode = !layerSliceMode));
                    list.add(cycleSpec("Целевой слой (Y)", "Нажми, чтобы задать текущий Y", "Y = " + targetBuildLayerY, () -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            targetBuildLayerY = (int) mc.player.getY();
                        } else {
                            targetBuildLayerY = targetBuildLayerY >= 120 ? 40 : targetBuildLayerY + 8;
                        }
                    }));
                    list.add(toggleSpec("Подсветка дефицита", "Красный цвет при остатке < 16", deficitHighlight, () -> deficitHighlight = !deficitHighlight));
                }
                case VISUAL_RADAR -> {
                    list.add(toggleSpec("Мини-карта-радар (HUD)", "Сверхбыстрая RLE-карта (75+ FPS)", radarHudEnabled, () -> radarHudEnabled = !radarHudEnabled));
                    list.add(toggleSpec("Реальный рельеф мира", "Цветные блоки и тени высот", radarTerrainMode, () -> radarTerrainMode = !radarTerrainMode));
                    list.add(toggleSpec("Вращение карты за камерой", "ВЫКЛ = чёткий Север вверх (N)", radarRotateMap, () -> radarRotateMap = !radarRotateMap));
                    list.add(cycleSpec("Угол экрана радара", "Расположение под строкой FPS", radarRightSide ? "Справа" : "Слева", () -> radarRightSide = !radarRightSide));
                    list.add(cycleSpec("Масштаб карты", "Радиус обзора местности",
                        radarZoomLevel == 0 ? "Близко" : (radarZoomLevel == 1 ? "Средне" : "Далеко"), () -> {
                            radarZoomLevel = (radarZoomLevel + 1) % 3;
                            mapNeedsFullReset = true;
                        }));
                    list.add(toggleSpec("Звуковой радар угроз", "Красный маркер в сторону врага", soundRadarEnabled, () -> soundRadarEnabled = !soundRadarEnabled));
                    list.add(toggleSpec("Нить Ариадны", "След из частиц обратно домой", ariadneThread, () -> ariadneThread = !ariadneThread));
                    list.add(toggleSpec("Шестое чувство", "Тревога если моб за спиной", sixthSense, () -> sixthSense = !sixthSense));
                    list.add(toggleSpec("Макс. яркость (100%)", "Поднимает гамму для пещер", fullBrightnessBoost, () -> fullBrightnessBoost = !fullBrightnessBoost));
                }
                case CHESTS_VILLAGERS -> {
                    String homeSub = homeWaypoint == null
                        ? "Метка дома ещё не задана"
                        : ("Дом: " + homeWaypoint.getX() + " / " + homeWaypoint.getY() + " / " + homeWaypoint.getZ());
                    list.add(cycleSpec("Точка дома на карте", homeSub, homeWaypoint == null ? "Задать" : "Обновить", () -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            homeWaypoint = mc.player.blockPosition();
                            activeNavTarget = homeWaypoint;
                            activeNavLabel = "Дом";
                            pushVadimkaToast("Вадимка: Точка дома сохранена!", 60);
                        }
                    }));
                    list.add(toggleSpec("Авто-память ларчиков", "Запоминает предметы в открытых сундуках", chestMemory, () -> chestMemory = !chestMemory));
                    list.add(toggleSpec("Авто-запись торгов жителей", "Сохраняет книги и цены при торговле", autoRecordTrades, () -> autoRecordTrades = !autoRecordTrades));
                    list.add(toggleSpec("Метки сундуков на карте", "Оранжевые точки хранилищ рядом", radarShowChests, () -> radarShowChests = !radarShowChests));
                    list.add(toggleSpec("Метки дропа на радаре", "Золотые точки выпавшего лута", radarShowDrops, () -> radarShowDrops = !radarShowDrops));
                    list.add(toggleSpec("Трекер жителей на карте", "Фиолетовые метки на мини-карте", villagerTracker, () -> villagerTracker = !villagerTracker));
                    list.add(cycleSpec("Сбросить активную метку", "Убирает синий маяк с карты", "Сбросить", () -> {
                        activeNavTarget = null;
                        activeNavLabel = "";
                        safeAutopilot = false;
                    }));
                }
                case COMBAT_PVP -> {
                    list.add(toggleSpec("Боевой HUD (Тотемы/HP)", "Панель выживания справа внизу", combatHudEnabled, () -> combatHudEnabled = !combatHudEnabled));
                    list.add(toggleSpec("Индикатор брони", "Полоски прочности Ш/Н/П/Б", armorDurabilityHud, () -> armorDurabilityHud = !armorDurabilityHud));
                    list.add(toggleSpec("Сканер тотемов", "Контроль тотема в руке и сумке", totemScanner, () -> totemScanner = !totemScanner));
                    list.add(toggleSpec("Шкала натяжения лука", "Прогресс заряда под прицелом", trajectoriesEnabled, () -> trajectoriesEnabled = !trajectoriesEnabled));
                    list.add(toggleSpec("Метка точки смерти", "Дистанция до последней гибели", deathReplayMarker, () -> deathReplayMarker = !deathReplayMarker));
                }
                case CUSTOM_VISUAL -> {
                    list.add(cycleSpec("Тема меню [P]", "5 премиум-палитр интерфейса", currentTheme.title, () -> {
                        MenuTheme[] themes = MenuTheme.values();
                        currentTheme = themes[(currentTheme.ordinal() + 1) % themes.length];
                    }));
                    list.add(cycleSpec("Крылья персонажа", "Видны в 3D-студии и в F5", WINGS_OPTIONS.get(wingsIndex), () -> {
                        wingsIndex = (wingsIndex + 1) % WINGS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Рога / Нимб", "Аксессуар над головой", HORNS_OPTIONS.get(hornsIndex), () -> {
                        hornsIndex = (hornsIndex + 1) % HORNS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Шляпка / Ушки", "Головной убор в 3D-студии", HATS_OPTIONS.get(hatsIndex), () -> {
                        hatsIndex = (hatsIndex + 1) % HATS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Одежда и аура", "Стиль экипировки и подиума", OUTFIT_OPTIONS.get(outfitIndex), () -> {
                        outfitIndex = (outfitIndex + 1) % OUTFIT_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Кастомный прицел", "Неоновый прицел по центру", CROSSHAIR_OPTIONS.get(crosshairIndex), () -> {
                        crosshairIndex = (crosshairIndex + 1) % CROSSHAIR_OPTIONS.size();
                    }));
                    list.add(toggleSpec("3D-крылья и нимб в F5", "Авто-скрытие от 1-го лица", worldCosmeticsParticles, () -> worldCosmeticsParticles = !worldCosmeticsParticles));
                }
                default -> {
                }
            }
            return list;
        }

        private RowSpec toggleSpec(String title, String sub, boolean state, Runnable act) {
            return new RowSpec(title, sub, true, state, "", act);
        }

        private RowSpec cycleSpec(String title, String sub, String val, Runnable act) {
            return new RowSpec(title, sub, false, true, val, act);
        }

        private void resetAfkGameState() {
            afkScore = 0;
            afkTick = 0;
            runnerLane = 1;
            runnerItems.clear();
            sonicY = 0.0;
            sonicVel = 0.0;
            sonicObstacles.clear();
            doomPlayerX = 50;
            doomBullets.clear();
            doomEnemies.clear();
        }

        @Override
        public void tick() {
            super.tick();
            if (activeTab == MenuTab.MUSIC_AFK && musicSubTab == 0) {
                pollSpotifyAsync(false);
            }
            if (!afkGameRunning) {
                return;
            }
            afkTick++;
            switch (selectedAfkGame) {
                case RUNNER -> tickRunnerGame();
                case SONIC -> tickSonicGame();
                case DOOM -> tickDoomGame();
            }
            if (afkScore > afkBestScore) {
                afkBestScore = afkScore;
            }
        }

        private void tickRunnerGame() {
            if (afkTick % 8 == 0) {
                afkScore++;
                if (gameRand.nextInt(3) == 0) {
                    int type = gameRand.nextInt(5) == 0 ? 1 : 0;
                    runnerItems.add(new int[]{gameRand.nextInt(3), 0, type});
                }
            }
            if (afkTick % 2 == 0) {
                List<int[]> next = new ArrayList<>();
                for (int[] it : runnerItems) {
                    it[1] += 4;
                    if (it[1] >= 72 && it[1] <= 90 && it[0] == runnerLane) {
                        if (it[2] == 1) {
                            afkScore += 5;
                            continue;
                        } else {
                            resetAfkGameState();
                            return;
                        }
                    }
                    if (it[1] < 98) {
                        next.add(it);
                    }
                }
                runnerItems.clear();
                runnerItems.addAll(next);
            }
        }

        private void tickSonicGame() {
            if (afkTick % 6 == 0) {
                afkScore++;
            }
            sonicY += sonicVel;
            sonicVel -= 1.35;
            if (sonicY < 0.0) {
                sonicY = 0.0;
                sonicVel = 0.0;
            }
            if (afkTick % 24 == 0) {
                int type = gameRand.nextInt(3) == 0 ? 1 : 0;
                sonicObstacles.add(new int[]{100, type});
            }
            List<int[]> next = new ArrayList<>();
            for (int[] ob : sonicObstacles) {
                ob[0] -= 4;
                if (ob[0] >= 12 && ob[0] <= 24) {
                    if (ob[1] == 1 && sonicY >= 8) {
                        afkScore += 10;
                        continue;
                    } else if (ob[1] == 0 && sonicY < 10) {
                        resetAfkGameState();
                        return;
                    }
                }
                if (ob[0] > -6) {
                    next.add(ob);
                }
            }
            sonicObstacles.clear();
            sonicObstacles.addAll(next);
        }

        private void tickDoomGame() {
            if (afkTick % 26 == 0) {
                doomEnemies.add(new int[]{12 + gameRand.nextInt(76), 8, 1});
            }
            List<int[]> nextB = new ArrayList<>();
            for (int[] b : doomBullets) {
                b[1] -= 6;
                if (b[1] > 4) {
                    nextB.add(b);
                }
            }
            doomBullets.clear();
            doomBullets.addAll(nextB);

            List<int[]> nextE = new ArrayList<>();
            for (int[] e : doomEnemies) {
                if (afkTick % 3 == 0) {
                    e[1] += 2;
                }
                boolean hit = false;
                for (int i = doomBullets.size() - 1; i >= 0; i--) {
                    int[] b = doomBullets.get(i);
                    if (Math.abs(b[0] - e[0]) < 10 && Math.abs(b[1] - e[1]) < 10) {
                        doomBullets.remove(i);
                        hit = true;
                        afkScore += 5;
                        break;
                    }
                }
                if (hit) {
                    continue;
                }
                if (e[1] >= 84) {
                    resetAfkGameState();
                    return;
                }
                nextE.add(e);
            }
            doomEnemies.clear();
            doomEnemies.addAll(nextE);
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            int k = event.key();
            if (activeInputBox != null && activeInputBox.isFocused()) {
                if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) {
                    if (activeTab == MenuTab.VADIMKA && vadimkaSubTab == 1) {
                        if (!pendingChatDraft.trim().isEmpty()) {
                            sendPromptToVadimkaFromMenu(pendingChatDraft);
                            pendingChatDraft = "";
                            this.rebuildWidgets();
                        }
                        return true;
                    } else if (activeTab == MenuTab.CHESTS_VILLAGERS && chestsSubTab == 0) {
                        this.rebuildWidgets();
                        return true;
                    }
                }
                if (k == GLFW.GLFW_KEY_ESCAPE) {
                    activeInputBox.setFocused(false);
                    return true;
                }
                return super.keyPressed(event);
            }

            if (afkGameRunning) {
                if (k == GLFW.GLFW_KEY_ESCAPE) {
                    afkGameRunning = false;
                    this.rebuildWidgets();
                    return true;
                }
                if (selectedAfkGame == AfkGameType.RUNNER) {
                    if (k == GLFW.GLFW_KEY_A || k == GLFW.GLFW_KEY_LEFT) {
                        runnerLane = Math.max(0, runnerLane - 1);
                        return true;
                    }
                    if (k == GLFW.GLFW_KEY_D || k == GLFW.GLFW_KEY_RIGHT) {
                        runnerLane = Math.min(2, runnerLane + 1);
                        return true;
                    }
                } else if (selectedAfkGame == AfkGameType.SONIC) {
                    if ((k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_W || k == GLFW.GLFW_KEY_UP) && sonicY <= 0.5) {
                        sonicVel = 10.5;
                        return true;
                    }
                } else if (selectedAfkGame == AfkGameType.DOOM) {
                    if (k == GLFW.GLFW_KEY_A || k == GLFW.GLFW_KEY_LEFT) {
                        doomPlayerX = Math.max(10, doomPlayerX - 9);
                        return true;
                    }
                    if (k == GLFW.GLFW_KEY_D || k == GLFW.GLFW_KEY_RIGHT) {
                        doomPlayerX = Math.min(90, doomPlayerX + 9);
                        return true;
                    }
                    if (k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_W || k == GLFW.GLFW_KEY_UP) {
                        doomBullets.add(new int[]{doomPlayerX, 82});
                        return true;
                    }
                }
            }

            if (k == GLFW.GLFW_KEY_P) {
                this.onClose();
                return true;
            }
            return super.keyPressed(event);
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            // Пусто, чтобы super.render() в конце не затемнял кастомное окно поверх EditBox
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            // 1. Затемнённый фон и кастомный корпус рисуем ДО super.render, чтобы EditBox оставался поверх!
            g.fill(0, 0, this.width, this.height, 0xA805080D);

            fillRoundedRect(g, winX, winY, winW, winH, currentTheme.bgWindow);
            drawRoundedBorder(g, winX, winY, winW, winH, currentTheme.accent);

            // Шапка
            g.fillGradient(winX + 2, winY + 2, winX + winW - 2, winY + 31, currentTheme.bgCard, currentTheme.bgPanel);
            g.fill(winX + 2, winY + 31, winX + winW - 2, winY + 32, currentTheme.accent);

            g.fill(winX + 10, winY + 12, winX + 16, winY + 18, currentTheme.accent);
            g.drawString(this.font, "ASSISTANT VADIMKA", winX + 22, winY + 11, 0xFFFFFFFF);
            int verX = winX + 22 + this.font.width("ASSISTANT VADIMKA") + 8;
            fillRoundedRect(g, verX, winY + 9, 118, 13, 0xFF121C29);
            drawRoundedBorder(g, verX, winY + 9, 118, 13, currentTheme.secondary);
            g.drawString(this.font, VERSION_LABEL + " • 1.21.11", verX + 5, winY + 12, currentTheme.secondary);

            // 2. Левая боковая панель
            int bodyH = winH - 56;
            fillRoundedRect(g, winX + 4, winY + 35, sidebarW - 2, bodyH, currentTheme.bgPanel);
            drawRoundedBorder(g, winX + 4, winY + 35, sidebarW - 2, bodyH, currentTheme.borderMuted);

            for (TabVisual tv : tabVisuals) {
                boolean active = tv.tab == this.activeTab;
                boolean hover = isOver(mouseX, mouseY, tv.x, tv.y, tv.w, tv.h);
                int bg = active
                    ? lerpColor(currentTheme.bgCard, currentTheme.accent, 0.35F)
                    : (hover ? currentTheme.bgCard : 0xFF0C121B);
                fillRoundedRect(g, tv.x, tv.y, tv.w, tv.h, bg);
                if (active) {
                    g.fill(tv.x + 1, tv.y + 4, tv.x + 3, tv.y + tv.h - 4, currentTheme.accent);
                    drawRoundedBorder(g, tv.x, tv.y, tv.w, tv.h, currentTheme.accent);
                } else if (hover) {
                    drawRoundedBorder(g, tv.x, tv.y, tv.w, tv.h, currentTheme.secondary);
                }
                int iconCol = active ? currentTheme.secondary : (hover ? currentTheme.secondary : 0xFF64748B);
                drawTabIcon(g, tv.tab, tv.x + 6, tv.y + 6, iconCol);
                String tLabel = trimText(this.font, tv.tab.shortTitle, tv.w - 22);
                g.drawString(this.font, tLabel, tv.x + 19, tv.y + 7, active ? 0xFFFFFFFF : (hover ? 0xFFE2E8F0 : 0xFF94A3B8));
            }

            // 3. Центральная панель
            fillRoundedRect(g, contentX, winY + 35, contentW, bodyH, currentTheme.bgPanel);
            drawRoundedBorder(g, contentX, winY + 35, contentW, bodyH, currentTheme.borderMuted);

            int titleMaxW = totalPages > 1 ? (contentW - 84) : (contentW - 18);
            g.drawString(this.font, trimText(this.font, activeTab.fullTitle, titleMaxW), contentX + 10, winY + 43, currentTheme.secondary);
            if (totalPages > 1) {
                String pStr = (currentPage + 1) + "/" + totalPages;
                g.drawString(this.font, pStr, contentX + contentW - 44, winY + 43, 0xFF94A3B8);
            }
            g.fill(contentX + 8, winY + 54, contentX + contentW - 8, winY + 55, currentTheme.borderMuted);

            // Специальные панели для Чата с Вадимкой и Spotify Connect
            if (activeTab == MenuTab.VADIMKA && vadimkaSubTab == 1) {
                renderVadimkaChatPanel(g);
            } else if (activeTab == MenuTab.MUSIC_AFK && musicSubTab == 0) {
                renderSpotifyPlayerCard(g);
            }

            // Карточки настроек и списков
            for (SettingRowVisual rv : rowVisuals) {
                boolean cardHover = isOver(mouseX, mouseY, rv.x, rv.y, rv.w, rv.h);
                boolean btnHover = isOver(mouseX, mouseY, rv.btnX, rv.btnY, rv.btnW, rv.btnH);

                fillRoundedRect(g, rv.x, rv.y, rv.w, rv.h,
                    cardHover ? lerpColor(currentTheme.bgCard, 0xFFFFFFFF, 0.06F) : currentTheme.bgCard);
                drawRoundedBorder(g, rv.x, rv.y, rv.w, rv.h,
                    cardHover ? currentTheme.secondary : currentTheme.borderMuted);

                int stripCol = rv.isToggle ? (rv.toggleState ? currentTheme.accent : 0xFF475569) : currentTheme.secondary;
                g.fill(rv.x + 2, rv.y + 5, rv.x + 4, rv.y + rv.h - 5, stripCol);

                int textMaxW = (rv.btnX - (rv.x + 10)) - 6;
                g.drawString(this.font, trimText(this.font, rv.title, textMaxW), rv.x + 10, rv.y + 5, 0xFFF8FAFC);
                g.drawString(this.font, trimText(this.font, rv.subtitle, textMaxW), rv.x + 10, rv.y + 16, 0xFF94A3B8);

                if (rv.isToggle) {
                    // Высококонтрастный тумблер ВКЛ / ВЫКЛ (чётко читается на любой теме!)
                    int trackCol = rv.toggleState ? currentTheme.toggleOnBg : 0xFF151C28;
                    int borderCol = btnHover ? 0xFFFFFFFF : (rv.toggleState ? currentTheme.accent : 0xFF475569);
                    fillRoundedRect(g, rv.btnX, rv.btnY + 1, rv.btnW, rv.btnH - 2, trackCol);
                    drawRoundedBorder(g, rv.btnX, rv.btnY + 1, rv.btnW, rv.btnH - 2, borderCol);

                    int knobSize = rv.btnH - 6;
                    int knobX = rv.toggleState ? (rv.btnX + rv.btnW - knobSize - 3) : (rv.btnX + 3);
                    fillRoundedRect(g, knobX, rv.btnY + 3, knobSize, knobSize, rv.toggleState ? currentTheme.accent : 0xFF94A3B8);

                    String tag = rv.toggleState ? "ВКЛ" : "ВЫКЛ";
                    int tagX = rv.toggleState ? (rv.btnX + 6) : (rv.btnX + rv.btnW - this.font.width(tag) - 5);
                    g.drawString(this.font, tag, tagX, rv.btnY + 5, rv.toggleState ? 0xFFFFFFFF : 0xFF94A3B8);
                } else {
                    int selBg = btnHover ? lerpColor(currentTheme.bgPanel, currentTheme.accent, 0.38F) : 0xFF0A1018;
                    fillRoundedRect(g, rv.btnX, rv.btnY, rv.btnW, rv.btnH, selBg);
                    drawRoundedBorder(g, rv.btnX, rv.btnY, rv.btnW, rv.btnH,
                        btnHover ? currentTheme.accent : currentTheme.secondary);
                    String val = trimText(this.font, rv.valueLabel, rv.btnW - 8);
                    int vx = rv.btnX + (rv.btnW - this.font.width(val)) / 2;
                    g.drawString(this.font, val, vx, rv.btnY + 5, btnHover ? 0xFFFFFFFF : currentTheme.secondary);
                }
            }

            if (activeTab == MenuTab.MUSIC_AFK && musicSubTab == 1 && afkGameRunning) {
                renderAfkGameCanvas(g, contentX + 8, winY + 186, contentW - 16, bodyH - 156);
            }

            // 4. Правая 3D-студия (реальный GPU 3D-игрок + реальный 3D-Вадимка + живая косметика!)
            int prevX = winX + winW - previewW - 6;
            int prevY = winY + 35;
            fillRoundedRect(g, prevX, prevY, previewW, bodyH, currentTheme.bgPanel);
            drawRoundedBorder(g, prevX, prevY, previewW, bodyH, currentTheme.secondary);
            renderReal3DStudio(g, prevX, prevY, previewW, bodyH, mouseX, mouseY);

            // 5. Малые кнопки
            for (SmallBtnVisual sb : smallBtnVisuals) {
                boolean hover = isOver(mouseX, mouseY, sb.x, sb.y, sb.w, sb.h);
                int bg;
                int border;
                if (sb.danger) {
                    bg = hover ? 0xFFDC2626 : 0xFF331419;
                    border = hover ? 0xFFFCA5A5 : 0xFFEF4444;
                } else if (sb.spotifyGreen) {
                    bg = hover ? 0xFF1DB954 : 0xFF124726;
                    border = 0xFF1DB954;
                } else if (sb.highlighted) {
                    bg = hover
                        ? lerpColor(currentTheme.bgCard, currentTheme.accent, 0.50F)
                        : lerpColor(currentTheme.bgCard, currentTheme.accent, 0.28F);
                    border = currentTheme.accent;
                } else {
                    bg = hover ? currentTheme.bgCard : 0xFF0C121B;
                    border = hover ? currentTheme.secondary : currentTheme.borderMuted;
                }
                fillRoundedRect(g, sb.x, sb.y, sb.w, sb.h, bg);
                drawRoundedBorder(g, sb.x, sb.y, sb.w, sb.h, border);
                String lbl = trimText(this.font, sb.label, sb.w - 4);
                int lx = sb.x + (sb.w - this.font.width(lbl)) / 2;
                int ly = sb.y + (sb.h - 8) / 2;
                g.drawString(this.font, lbl, lx, ly, hover ? 0xFFFFFFFF : 0xFFE2E8F0);
            }

            // 6. Нижняя статус-панель всего окна
            int statusY = winY + winH - 18;
            g.fill(winX + 4, statusY, winX + winW - 4, winY + winH - 3, currentTheme.bgCard);
            g.fill(winX + 4, statusY, winX + winW - 4, statusY + 1, currentTheme.borderMuted);
            String spMini = spotifyIsPlaying ? ("♫ " + spotifyCurrentArtist + " - " + spotifyCurrentTrack) : "♫ Spotify: пауза/выкл";
            String statusLeft = "Чат: пиши «Вадимка ...»  |  Ларчиков: " + rememberedChests.size() + "  |  Жителей: " + rememberedVillagers.size();
            g.drawString(this.font, trimText(this.font, statusLeft, winW / 2 + 40), winX + 10, statusY + 4, 0xFF94A3B8);
            String spTrim = trimText(this.font, spMini, winW / 2 - 60);
            g.drawString(this.font, spTrim, winX + winW - this.font.width(spTrim) - 10, statusY + 4, spotifyIsPlaying ? 0xFF4ADE80 : 0xFF64748B);

            // 7. Рендер видимых виджетов (только EditBox!) поверх нашего интерфейса
            super.render(g, mouseX, mouseY, partialTick);
        }

        private void renderVadimkaChatPanel(GuiGraphics g) {
            int boxX = this.contentX + 8;
            int boxY = this.winY + 104;
            int boxW = this.contentW - 16;
            int boxH = this.winH - 156;
            fillRoundedRect(g, boxX, boxY, boxW, boxH, 0xFF080D14);
            drawRoundedBorder(g, boxX, boxY, boxW, boxH, currentTheme.borderMuted);

            List<ChatLogEntry> msgs = new ArrayList<>(vadimkaChatHistory);
            int maxLines = Math.max(1, (boxH - 12) / 13);
            List<String[]> wrapped = new ArrayList<>();
            int maxTextW = boxW - 18;

            for (ChatLogEntry entry : msgs) {
                String prefix = entry.fromVadimka ? "✦ Вадимка: " : "Вы: ";
                String full = prefix + entry.text;
                String[] parts = wrapToLines(full, maxTextW);
                for (String part : parts) {
                    wrapped.add(new String[]{entry.fromVadimka ? "1" : "0", part});
                }
            }

            int start = Math.max(0, wrapped.size() - maxLines);
            int y = boxY + 6;
            for (int i = start; i < wrapped.size(); i++) {
                boolean isV = "1".equals(wrapped.get(i)[0]);
                g.drawString(this.font, wrapped.get(i)[1], boxX + 8, y, isV ? currentTheme.secondary : 0xFFE2E8F0);
                y += 13;
            }
        }

        private String[] wrapToLines(String text, int maxW) {
            List<String> lines = new ArrayList<>();
            String[] words = text.split(" ");
            StringBuilder cur = new StringBuilder();
            for (String w : words) {
                String test = cur.isEmpty() ? w : (cur + " " + w);
                if (this.font.width(test) > maxW && !cur.isEmpty()) {
                    lines.add(cur.toString());
                    cur = new StringBuilder(w);
                } else {
                    cur = new StringBuilder(test);
                }
            }
            if (!cur.isEmpty()) {
                lines.add(cur.toString());
            }
            return lines.toArray(new String[0]);
        }

        private void renderSpotifyPlayerCard(GuiGraphics g) {
            int cx = this.contentX + 7;
            int cy = this.winY + 82;
            int cw = this.contentW - 14;
            int ch = 60;
            g.fillGradient(cx, cy, cx + cw, cy + ch, 0xFF0A1D12, 0xFF0B111A);
            drawRoundedBorder(g, cx, cy, cw, ch, spotifyIsPlaying ? 0xFF1DB954 : currentTheme.borderMuted);

            // Обложка-эквалайзер слева
            fillRoundedRect(g, cx + 8, cy + 8, 44, 44, 0xFF08140D);
            drawRoundedBorder(g, cx + 8, cy + 8, 44, 44, 0xFF1DB954);
            long t = System.currentTimeMillis() / 110L;
            for (int i = 0; i < 6; i++) {
                int bh = spotifyIsPlaying ? (6 + (int) (Math.abs(Math.sin(t * 0.7 + i * 1.1)) * 26)) : 5;
                g.fill(cx + 14 + i * 6, cy + 46 - bh, cx + 17 + i * 6, cy + 46, spotifyIsPlaying ? 0xFF1DB954 : 0xFF475569);
            }

            int tx = cx + 60;
            int tw = cw - 68;
            String stateBadge = spotifyIsPlaying ? "● ИГРАЕТ В SPOTIFY (ПК)" : (spotifyAppRunning ? "⏸ НА ПАУЗЕ" : "○ SPOTIFY НЕ ЗАПУЩЕН");
            g.drawString(this.font, stateBadge, tx, cy + 9, spotifyIsPlaying ? 0xFF4ADE80 : 0xFF94A3B8);
            g.drawString(this.font, trimText(this.font, spotifyCurrentTrack, tw), tx, cy + 23, 0xFFFFFFFF);
            g.drawString(this.font, trimText(this.font, spotifyCurrentArtist, tw), tx, cy + 36, 0xFFCBD5E1);
        }

        private boolean isOver(int mx, int my, int x, int y, int w, int h) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        // =====================================================================
        // НАСТОЯЩАЯ GPU 3D-СТУДИЯ С ЖИВЫМИ КОСМЕТИЧЕСКИМИ ЭФФЕКТАМИ И РЕПЛИКОЙ
        // =====================================================================

        private void renderReal3DStudio(GuiGraphics g, int px, int py, int pw, int ph, int mouseX, int mouseY) {
            g.drawString(this.font, "✦ 3D СТУДИЯ (GPU)", px + 8, py + 8, currentTheme.accent);

            int vpX = px + 8;
            int vpY = py + 22;
            int vpW = pw - 16;
            int vpH = 124;

            g.fillGradient(vpX, vpY, vpX + vpW, vpY + vpH, 0xFF060A11, lerpColor(0xFF0C1422, currentTheme.accent, 0.16F));
            drawRoundedBorder(g, vpX, vpY, vpW, vpH, currentTheme.borderMuted);

            // Цветная аура подиума под выбранный пресет одежды
            int auraCol = switch (outfitIndex) {
                case 1 -> 0xFF38BDF8;
                case 2 -> 0xFFF43F5E;
                case 3 -> 0xFF10B981;
                case 4 -> 0xFFC084FC;
                default -> currentTheme.accent;
            };
            int podY = vpY + vpH - 16;
            g.fill(vpX + 12, podY, vpX + vpW - 12, podY + 2, auraCol);
            g.fillGradient(vpX + 8, podY + 2, vpX + vpW - 8, vpY + vpH - 2, lerpColor(0xFF0A111C, auraCol, 0.28F), 0xFF060A11);

            // Рисуем крылья ЗА спиной 3D-игрока (видны сразу при смене в 9-й вкладке!)
            int playerCenterX = (previewMode == PreviewMode.DUO) ? (vpX + (vpW * 28) / 100) : (vpX + vpW / 2);
            int playerChestY = vpY + vpH / 2 + 2;
            if (previewMode != PreviewMode.PET_ONLY && wingsIndex > 0) {
                drawStudioCosmeticWings(g, playerCenterX, playerChestY);
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                LivingEntity pet = getOrCreateStudioCompanion(mc.level, mc.player);
                try {
                    if (previewMode == PreviewMode.DUO) {
                        int midX = vpX + (vpW * 55) / 100;
                        InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, vpX + 2, vpY + 8, midX, vpY + vpH - 10,
                            37, 0.0625F, (float) mouseX, (float) mouseY, mc.player
                        );
                        if (pet != null) {
                            InventoryScreen.renderEntityInInventoryFollowsMouse(
                                g, midX - 4, vpY + 14, vpX + vpW - 2, vpY + vpH - 10,
                                currentForm.previewGuiSize, 0.0625F, (float) mouseX, (float) mouseY, pet
                            );
                        }
                    } else if (previewMode == PreviewMode.PLAYER_ONLY) {
                        InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, vpX + 4, vpY + 6, vpX + vpW - 4, vpY + vpH - 8,
                            46, 0.0625F, (float) mouseX, (float) mouseY, mc.player
                        );
                    } else if (pet != null) {
                        int bigPetSize = Math.min(64, (int) (currentForm.previewGuiSize * 1.45));
                        InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, vpX + 4, vpY + 6, vpX + vpW - 4, vpY + vpH - 8,
                            bigPetSize, 0.0625F, (float) mouseX, (float) mouseY, pet
                        );
                    }
                } catch (Throwable ignored) {
                    g.drawString(this.font, "3D: " + currentForm.shortName, vpX + 10, vpY + vpH / 2 - 4, currentTheme.secondary);
                }
            }

            // Рога / Нимб / Шляпка над головой 3D-игрока в студии
            if (previewMode != PreviewMode.PET_ONLY) {
                int headTopY = (previewMode == PreviewMode.DUO) ? (vpY + 26) : (vpY + 16);
                drawStudioHeadCosmetics(g, playerCenterX, headTopY);
            }

            // Карточка сводки + облачко реплики Вадимки
            int infoY = py + 174;
            int infoH = ph - 180;
            fillRoundedRect(g, px + 8, infoY, pw - 16, infoH, currentTheme.bgCard);
            drawRoundedBorder(g, px + 8, infoY, pw - 16, infoH, currentTheme.borderMuted);

            int iy = infoY + 6;
            int maxW = pw - 26;
            g.drawString(this.font, trimText(this.font, "Вадимка: " + currentForm.shortName, maxW), px + 13, iy, currentTheme.accent);
            iy += 12;
            g.drawString(this.font, trimText(this.font, "Миньоны: " + countActiveMinions() + "/4 • " + (minionsSmartAi ? "AI" : "Орбита"), maxW), px + 13, iy, 0xFFCBD5E1);
            iy += 12;
            g.drawString(this.font, trimText(this.font, "Крылья: " + WINGS_OPTIONS.get(wingsIndex), maxW), px + 13, iy, 0xFFE2E8F0);
            iy += 11;
            g.drawString(this.font, trimText(this.font, "Рога: " + HORNS_OPTIONS.get(hornsIndex), maxW), px + 13, iy, 0xFFE2E8F0);
            iy += 11;
            g.drawString(this.font, trimText(this.font, "Шляпка: " + HATS_OPTIONS.get(hatsIndex), maxW), px + 13, iy, 0xFFE2E8F0);
            iy += 13;
            g.fill(px + 13, iy - 2, px + pw - 13, iy - 1, currentTheme.borderMuted);
            iy += 3;
            String[] bubbleLines = wrapToLines("«" + vadimkaLastBubble + "»", maxW);
            for (int i = 0; i < Math.min(3, bubbleLines.length); i++) {
                g.drawString(this.font, bubbleLines[i], px + 13, iy + i * 10, currentTheme.secondary);
            }
        }

        private void drawStudioCosmeticWings(GuiGraphics g, int cx, int cy) {
            int flap = (int) Math.round(Math.sin(System.currentTimeMillis() / 220.0) * 3.0);
            int wMain = switch (wingsIndex) {
                case 1 -> 0xBBF8FAFC;
                case 2 -> 0xBBDC2626;
                case 3 -> 0xBB06B6D4;
                default -> 0xBBA855F7;
            };
            int wTip = switch (wingsIndex) {
                case 1 -> 0xDD7DD3FC;
                case 2 -> 0xDD7F1D1D;
                case 3 -> 0xDD67E8F9;
                default -> 0xDDE879F9;
            };
            // Левое и правое стилизованные крылья со ступеньками перьев
            for (int side = -1; side <= 1; side += 2) {
                int x1 = cx + side * 12;
                int x2 = cx + side * 28;
                int x3 = cx + side * 35;
                g.fill(Math.min(x1, x2), cy - 16 + flap, Math.max(x1, x2), cy + 6 + flap, wMain);
                g.fill(Math.min(x2, x3), cy - 22 + flap, Math.max(x2, x3), cy - 2 + flap, wTip);
                g.fill(Math.min(x1, x2), cy + 6 + flap, Math.max(x1, x2) - (side > 0 ? 4 : 0), cy + 12 + flap, wTip);
            }
        }

        private void drawStudioHeadCosmetics(GuiGraphics g, int cx, int topY) {
            int bob = (int) Math.round(Math.sin(System.currentTimeMillis() / 280.0) * 1.5);
            if (hornsIndex == 4) {
                // Золотой нимб
                drawRoundedBorder(g, cx - 12, topY - 8 + bob, 24, 5, 0xFFFACC15);
            } else if (hornsIndex > 0) {
                int hCol = hornsIndex == 1 ? 0xFFEF4444 : (hornsIndex == 2 ? 0xFFD97706 : 0xFF38BDF8);
                g.fill(cx - 12, topY - 5, cx - 8, topY + 2, hCol);
                g.fill(cx - 14, topY - 9, cx - 10, topY - 4, hCol);
                g.fill(cx + 8, topY - 5, cx + 12, topY + 2, hCol);
                g.fill(cx + 10, topY - 9, cx + 14, topY - 4, hCol);
            }

            if (hatsIndex == 1) {
                // Корона
                g.fill(cx - 8, topY - 3, cx + 8, topY + 1, 0xFFFACC15);
                g.fill(cx - 8, topY - 6, cx - 5, topY - 3, 0xFFFACC15);
                g.fill(cx - 1, topY - 7, cx + 2, topY - 3, 0xFFFACC15);
                g.fill(cx + 5, topY - 6, cx + 8, topY - 3, 0xFFFACC15);
            } else if (hatsIndex == 2) {
                // Ушки Крыски
                g.fill(cx - 10, topY - 5, cx - 4, topY + 1, 0xFF94A3B8);
                g.fill(cx - 8, topY - 3, cx - 5, topY + 1, 0xFFF472B6);
                g.fill(cx + 4, topY - 5, cx + 10, topY + 1, 0xFF94A3B8);
                g.fill(cx + 5, topY - 3, cx + 8, topY + 1, 0xFFF472B6);
            } else if (hatsIndex == 3) {
                // Цилиндр
                g.fill(cx - 10, topY - 2, cx + 10, topY + 1, 0xFF0F172A);
                g.fill(cx - 6, topY - 11, cx + 6, topY - 2, 0xFF1E293B);
                g.fill(cx - 6, topY - 4, cx + 6, topY - 2, currentTheme.accent);
            } else if (hatsIndex == 4) {
                // Наушники
                g.fill(cx - 10, topY - 4, cx + 10, topY - 2, currentTheme.accent);
                g.fill(cx - 11, topY - 2, cx - 8, topY + 6, currentTheme.secondary);
                g.fill(cx + 8, topY - 2, cx + 11, topY + 6, currentTheme.secondary);
            }
        }

        // =====================================================================
        // 3 ИГРАБЕЛЬНЫЕ AFK-МИНИ-ИГРЫ
        // =====================================================================

        private void renderAfkGameCanvas(GuiGraphics g, int gx, int gy, int gw, int gh) {
            fillRoundedRect(g, gx, gy, gw, gh, 0xFF070C12);
            drawRoundedBorder(g, gx, gy, gw, gh, currentTheme.accent);
            String hdr = selectedAfkGame.title + " | Счёт: " + afkScore + "  Рекорд: " + afkBestScore;
            g.drawString(this.font, trimText(this.font, hdr, gw - 12), gx + 6, gy + 5, 0xFFFDE047);

            int ax = gx + 6;
            int ay = gy + 18;
            int aw = gw - 12;
            int ah = gh - 24;
            g.fill(ax, ay, ax + aw, ay + ah, 0xFF0D1520);

            switch (selectedAfkGame) {
                case RUNNER -> {
                    int laneW = aw / 3;
                    for (int l = 0; l < 3; l++) {
                        g.fill(ax + l * laneW + 2, ay + 2, ax + (l + 1) * laneW - 2, ay + ah - 2, 0x18FFFFFF);
                    }
                    for (int[] it : runnerItems) {
                        int ix = ax + it[0] * laneW + laneW / 2 - 6;
                        int iy = ay + (it[1] * (ah - 14)) / 100;
                        if (it[2] == 1) {
                            g.fill(ix + 2, iy, ix + 10, iy + 10, 0xFF10B981);
                        } else {
                            g.fill(ix, iy, ix + 12, iy + 12, 0xFF22C55E);
                            g.fill(ix + 2, iy + 3, ix + 4, iy + 5, 0xFF000000);
                            g.fill(ix + 8, iy + 3, ix + 10, iy + 5, 0xFF000000);
                        }
                    }
                    int plX = ax + runnerLane * laneW + laneW / 2 - 6;
                    int plY = ay + ah - 18;
                    g.fill(plX, plY, plX + 12, plY + 12, currentTheme.secondary);
                }
                case SONIC -> {
                    int groundY = ay + ah - 14;
                    g.fill(ax, groundY, ax + aw, ay + ah, 0xFF166534);
                    g.fill(ax, groundY, ax + aw, groundY + 3, 0xFF22C55E);
                    int plX = ax + (18 * aw) / 100;
                    int plY = groundY - 12 - (int) (sonicY * 1.4);
                    g.fill(plX, plY, plX + 12, plY + 12, 0xFF38BDF8);
                    for (int[] ob : sonicObstacles) {
                        int ox = ax + (ob[0] * aw) / 100;
                        if (ob[1] == 1) {
                            g.fill(ox, groundY - 28, ox + 8, groundY - 20, 0xFFFACC15);
                        } else {
                            g.fill(ox, groundY - 14, ox + 8, groundY, 0xFFEF4444);
                        }
                    }
                }
                case DOOM -> {
                    int plX = ax + (doomPlayerX * aw) / 100 - 6;
                    int plY = ay + ah - 16;
                    g.fill(plX, plY, plX + 12, plY + 10, currentTheme.secondary);
                    g.fill(plX + 4, plY - 4, plX + 8, plY, currentTheme.accent);
                    for (int[] b : doomBullets) {
                        int bx = ax + (b[0] * aw) / 100 - 1;
                        int by = ay + (b[1] * ah) / 100;
                        g.fill(bx, by, bx + 3, by + 6, 0xFFFDE047);
                    }
                    for (int[] e : doomEnemies) {
                        int ex = ax + (e[0] * aw) / 100 - 6;
                        int ey = ay + (e[1] * ah) / 100;
                        g.fill(ex, ey, ex + 12, ey + 12, 0xFFF43F5E);
                        g.fill(ex + 2, ey + 3, ex + 5, ey + 6, 0xFFFFFFFF);
                        g.fill(ex + 7, ey + 3, ex + 10, ey + 6, 0xFFFFFFFF);
                    }
                }
            }
        }
    }
}
