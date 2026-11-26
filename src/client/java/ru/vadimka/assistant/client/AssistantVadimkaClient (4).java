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
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
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
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
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

/**
 * Assistant Vadimka v6.0 CONCEPT EDITION
 * Архитектура интерфейса полностью повторяет концепт-макеты v1.4.2:
 * - Левая навигационная панель с иконками и неоновой подсветкой активного раздела
 * - Центральная интерактивная 3D / HUD / Радар / Spotify / Ларчики сцена
 * - Правая панель с современными тумблерами, слайдерами и пресетами
 * - Кастомные составные 3D-модели спутников (Кибер-Робот, Крыска, Вихрь, Хранитель, Синий Аксолотль)
 * - Полноценный навигатор с 3D-лучом и стрелкой к Дому, Ларчикам и Жителям
 * - Двухрежимный Spotify Connect (синхронизация с ПК + встроенный автономный плеер плейлистов)
 */
public class AssistantVadimkaClient implements ClientModInitializer {
    public static final String MOD_ID = "assistant_vadimka";
    public static final String VERSION_LABEL = "v6.0";

    // =========================================================================
    // 1. ПЕРЕЧИСЛЕНИЯ ОБЛИКОВ, МИНЬОНОВ, ВКЛАДОК, ТЕМ И РЕЖИМОВ
    // =========================================================================

    public enum VadimkaForm {
        CYBER_PLAYER("PLAYER", "Кибер-Робот", 0.72, 34, 0xFF38BDF8, 0xFF0284C7, 0xFFE0F2FE),
        RAT("RAT", "Кибер-Крыска", 0.95, 42, 0xFF94A3B8, 0xFFF472B6, 0xFFFCE7F3),
        ALLAY("ALLAY", "Тихоня-Дух", 0.82, 36, 0xFF22D3EE, 0xFF06B6D4, 0xFFCFFAFE),
        BREEZE("BREEZE", "Шторм-Вихрь", 0.52, 24, 0xFF818CF8, 0xFF4F46E5, 0xFFE0E7FF),
        WARDEN("WARDEN", "Мини-Хранитель", 0.34, 15, 0xFF14B8A6, 0xFF0F766E, 0xFF99F6E4),
        AXOLOTL("AXOLOTL", "Синий Аксолотль", 0.90, 38, 0xFF3B82F6, 0xFFEC4899, 0xFFDBEAFE);

        public final String codeName;
        public final String russianName;
        public final double worldScale;
        public final int guiScale;
        public final int primaryColor;
        public final int secondaryColor;
        public final int glowColor;

        VadimkaForm(String codeName, String russianName, double worldScale, int guiScale,
                    int primaryColor, int secondaryColor, int glowColor) {
            this.codeName = codeName;
            this.russianName = russianName;
            this.worldScale = worldScale;
            this.guiScale = guiScale;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
            this.glowColor = glowColor;
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
        COMPANION("Спутник Вадимка", "Спутник Вадимка — 6 кастомных 3D-обликов"),
        AUTO_BUILD("Авто-постройка", "Авто-постройка Litematica и 3D-схема"),
        LITEMATICA_HUD("Litematica HUD", "Litematica HUD — Трекер материалов и слоёв"),
        COMBAT_PVP("Бой и PvP", "Бой и PvP — Траектории, Броня и Тотемы"),
        VISUAL_RADAR("Визуал и Радар", "Визуал и Радар — Сонар, Элитра HUD и Карта"),
        SPOTIFY_PLAYER("Spotify Плеер", "Spotify Connect и Встроенный Плеер"),
        CHESTS_VILLAGERS("Ларчики и Жители", "Дворец испытаний, Ларчики и Библиотекари"),
        STYLE_PROFILER("Профиль стиля", "Поведенческий профайлер, Гардероб и Темы");

        public final String navTitle;
        public final String headerTitle;

        MenuTab(String navTitle, String headerTitle) {
            this.navTitle = navTitle;
            this.headerTitle = headerTitle;
        }
    }

    public enum CompanionAnchor {
        RIGHT_SHOULDER("Правое плечо", 0.95, 1.15, 0.25),
        LEFT_SHOULDER("Левое плечо", -0.95, 1.15, 0.25),
        ABOVE_HEAD("Над головой", 0.0, 2.25, 0.0),
        BEHIND_BACK("За спиной", 0.0, 1.25, -0.95);

        public final String label;
        public final double defaultX;
        public final double defaultY;
        public final double defaultZ;

        CompanionAnchor(String label, double defaultX, double defaultY, double defaultZ) {
            this.label = label;
            this.defaultX = defaultX;
            this.defaultY = defaultY;
            this.defaultZ = defaultZ;
        }
    }

    public enum MenuTheme {
        CONCEPT_CYAN("Concept Cyan-Violet", 0xF20D131A, 0xFF131B24, 0xFF192430, 0xFF22D3EE, 0xFFC084FC, 0xFF2A3B4D, 0xFF0E3A47),
        AMETHYST_NEON("Amethyst Neon", 0xF2110B1C, 0xFF191129, 0xFF231838, 0xFFA855F7, 0xFFF472B6, 0xFF3E2963, 0xFF2D124D),
        EMERALD_MATRIX("Emerald Matrix", 0xF209120F, 0xFF0F1D18, 0xFF162922, 0xFF10B981, 0xFF34D399, 0xFF234237, 0xFF0A3829),
        CRIMSON_BLADE("Crimson Blade", 0xF2160A0E, 0xFF210F15, 0xFF2E161E, 0xFFF43F5E, 0xFFFBBF24, 0xFF4D2432, 0xFF420D1B),
        OBSIDIAN_GOLD("Obsidian Gold", 0xF212100B, 0xFF1C1811, 0xFF262118, 0xFFF59E0B, 0xFFFDE047, 0xFF4A3F26, 0xFF3D2908);

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

    public enum BuildMode {
        FAST("Быстрый режим"),
        LAYERED("Послойный"),
        BOTTOM_UP("Снизу вверх"),
        FILL("Заполнение");

        public final String title;

        BuildMode(String title) {
            this.title = title;
        }
    }

    public enum AfkGameType {
        RUNNER("MC-Раннер", "Уворот от криперов (A / D)"),
        SONIC("Рывок по холмам", "Прыжки за кольцами (SPACE / W)"),
        DOOM("Данж-Шутер", "Огонь по мобам (A / D / SPACE)");

        public final String title;
        public final String hint;

        AfkGameType(String title, String hint) {
            this.title = title;
            this.hint = hint;
        }
    }

    // =========================================================================
    // 2. СТРУКТУРЫ ДАННЫХ (ЛАРЧИКИ, БИБЛИОТЕКАРИ, ПЛЕЙЛИСТЫ, ЧАТ)
    // =========================================================================

    public static class ChestRecord {
        public final BlockPos pos;
        public final String containerName;
        public final Map<String, Integer> items = new LinkedHashMap<>();
        public long updatedAt;

        public ChestRecord(BlockPos pos, String containerName) {
            this.pos = pos;
            this.containerName = containerName;
            this.updatedAt = System.currentTimeMillis();
        }

        public String summaryLine() {
            if (items.isEmpty()) {
                return "Пусто (открой заново для обновления)";
            }
            StringBuilder sb = new StringBuilder();
            int count = 0;
            for (Map.Entry<String, Integer> e : items.entrySet()) {
                if (count > 0) sb.append(", ");
                sb.append(e.getKey()).append(" x").append(e.getValue());
                count++;
                if (count >= 3) break;
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
        public final int emeraldPrice;
        public final boolean valuable;
        public final boolean outOfStock;

        public VillagerTradeEntry(String costText, String resultText, int emeraldPrice, boolean valuable, boolean outOfStock) {
            this.costText = costText;
            this.resultText = resultText;
            this.emeraldPrice = emeraldPrice;
            this.valuable = valuable;
            this.outOfStock = outOfStock;
        }
    }

    public static class VillagerRecord {
        public final BlockPos pos;
        public final String professionLabel;
        public final List<VillagerTradeEntry> trades = new ArrayList<>();

        public VillagerRecord(BlockPos pos, String professionLabel) {
            this.pos = pos;
            this.professionLabel = professionLabel;
        }
    }

    public static class PlaylistTrack {
        public final String title;
        public final String artist;
        public final int durationSec;
        public final int bpm;

        public PlaylistTrack(String title, String artist, int durationSec, int bpm) {
            this.title = title;
            this.artist = artist;
            this.durationSec = durationSec;
            this.bpm = bpm;
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
    // 3. СОСТОЯНИЕ ВСЕХ СИСТЕМ И НАСТРОЕК МОДА
    // =========================================================================

    // --- 3.1 Спутник Вадимка и 3D-позиционер (как на фото 1-2) ---
    public static VadimkaForm currentForm = VadimkaForm.CYBER_PLAYER;
    public static boolean companionEnabled = true;
    public static boolean droneCameraMode = false;
    public static boolean syncEmotions = true;
    public static boolean positionEditor3D = true;
    public static CompanionAnchor currentAnchor = CompanionAnchor.RIGHT_SHOULDER;
    public static double offsetX = 1.15;
    public static double offsetY = 1.15;
    public static double offsetZ = -0.35;
    public static int voiceVolumePercent = 65;
    public static int companionScalePercent = 100;
    public static boolean minionsEnabled = true;
    public static boolean minionsSmartAi = true;
    public static boolean compactNameplates = true;
    public static boolean chatAssistantEnabled = true;
    public static boolean quietMode = false;
    public static boolean randomChaosMode = false;
    public static final Map<MinionRole, Boolean> activeMinions = new EnumMap<>(MinionRole.class);
    public static final Deque<ChatLogEntry> chatHistory = new ArrayDeque<>();
    public static String vadimkaLastBubble = "Писк! Готов к разведке, стройке и музыке!";

    private static final Deque<String> pendingChatReplies = new ArrayDeque<>();
    private static int pendingChatReplyDelay = 0;
    private static boolean greetedInWorld = false;

    // --- 3.2 Авто-постройка (как на фото 5) ---
    public static boolean autoBuildLitematica = false;
    public static boolean smoothBezierCamera = true;
    public static boolean hotbarAutoRefill = true;
    public static boolean autoAnvilEnchant = false;
    public static int minBuildDelayMs = 80;
    public static int maxBuildDelayMs = 150;
    public static double buildReachBlocks = 4.5;
    public static BuildMode currentBuildMode = BuildMode.FAST;
    public static boolean buildBottomUp = true;
    public static boolean buildFillInterior = false;

    // --- 3.3 Litematica HUD (как на фото 3) ---
    public static boolean litematicaHudEnabled = true;
    public static boolean litematicaHudRight = true;
    public static boolean shulkerScanner = true;
    public static boolean hideCompletedMaterials = false;
    public static boolean ruler3DVolume = true;
    public static int buildPaletteIndex = 0;
    public static final List<String> BUILD_PALETTES = List.of(
        "По умолчанию (Камень/Дуб)",
        "Дворец испытаний (Медь/Туф)",
        "Незерит и Чернит",
        "Кварц и Призмарин"
    );

    // --- 3.4 Бой и PvP (как на фото 7) ---
    public static boolean combatHudEnabled = true;
    public static boolean trajectories3D = true;
    public static boolean armorAndTotemScanner = true;
    public static boolean shieldAndPearlTimers = true;
    public static boolean consumableCounter = true;
    public static boolean backstabIndicator = true;
    public static boolean lowFireMode = true;
    public static boolean compactShield = true;
    public static boolean deathReplayMarker = true;
    public static boolean pvpCompactStyle = true;

    // --- 3.5 Визуал, Сонар-Радар и Навигатор (как на фото 6) ---
    public static boolean visualsEnabled = true;
    public static boolean minimapRadarEnabled = true;
    public static boolean minimapTerrainEnabled = true;
    public static boolean soundRadarSonar = true;
    public static boolean mobSpawnGridLight0 = false;
    public static boolean elytraAssistant = true;
    public static boolean elytraPitchHorizon = true;
    public static boolean highlightChestsWorld = true;
    public static boolean librarianDatabase = true;
    public static boolean baseChestMemory = true;
    public static boolean shulkerPreview = true;
    public static boolean ariadneThread = true;
    public static int radarZoomLevel = 1; // 0=близко, 1=средне, 2=далеко

    // --- 3.6 Spotify Плеер и AFK (как на фото 8) ---
    public static boolean spotifyHudEnabled = true;
    public static boolean spotifyDuckOnSpeech = true;
    public static boolean vadimkaDanceToBpm = true;
    public static boolean spotifyPlaying = true;
    public static boolean spotifyShuffle = false;
    public static boolean spotifyRepeat = true;
    public static int spotifyVolumePercent = 78;
    public static int spotifyPlaylistIndex = 0;
    public static int spotifyTrackIndex = 0;
    public static long spotifyTrackElapsedMs = 48000L;
    public static long spotifyLastTickMs = System.currentTimeMillis();
    public static boolean afkPanicExit = true;
    public static AfkGameType selectedAfkGame = AfkGameType.RUNNER;

    private static final ExecutorService spotifySysExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vadimka-Media-Bridge");
        t.setDaemon(true);
        return t;
    });
    private static volatile String externalSpotifyTitle = "";
    private static volatile String externalSpotifyArtist = "";
    private static volatile boolean externalSpotifyDetected = false;
    private static volatile long lastExternalPollMs = 0L;
    private static volatile boolean externalPollBusy = false;

    public static final List<String> PLAYLIST_NAMES = List.of(
        "Мой Драйв ⚡",
        "Расслабляющий 🛠️",
        "Ночной Майнкрафт 🌙",
        "PvP Арена 🔥"
    );

    public static final List<List<PlaylistTrack>> PLAYLIST_TRACKS = List.of(
        List.of(
            new PlaylistTrack("Всё, что я хочу", "BIM", 195, 128),
            new PlaylistTrack("Pigstep (Cyber Remix)", "Lena Raine", 148, 134),
            new PlaylistTrack("Неон и Алмазы", "Vadimka Beats", 172, 130),
            new PlaylistTrack("Ночной Рейд", "KiberKot", 184, 140)
        ),
        List.of(
            new PlaylistTrack("Aria Math", "C418", 310, 84),
            new PlaylistTrack("Sweden (Ambient)", "C418", 215, 76),
            new PlaylistTrack("Subwoofer Lullaby", "C418", 208, 72),
            new PlaylistTrack("Mice on Venus", "C418", 281, 80)
        ),
        List.of(
            new PlaylistTrack("Otherside", "Lena Raine", 195, 118),
            new PlaylistTrack("Precipice", "Aaron Cherof", 299, 122),
            new PlaylistTrack("Creator", "Lena Raine", 176, 114),
            new PlaylistTrack("Relic", "Aaron Cherof", 218, 108)
        ),
        List.of(
            new PlaylistTrack("End Crystal Rush", "PvP Core", 162, 152),
            new PlaylistTrack("Totem Pop", "Netherite", 154, 148),
            new PlaylistTrack("Final Pearl", "Obsidian", 188, 145)
        )
    );

    // --- 3.7 Ларчики (Дворец испытаний) и Жители-Библиотекари (как на фото 9) ---
    public static boolean highlightTrialVaults = true;
    public static boolean autoClickVaults = false;
    public static int vaultLootGoalIndex = 0;
    public static final List<String> VAULT_LOOT_GOALS = List.of(
        "Тяжёлое ядро / Булава",
        "Зловещая бутылка",
        "Трезубец / Зачар. книга",
        "Изумруды и Алмазы"
    );
    public static boolean autoCycleLectern = false;
    public static boolean warnAxeDurability = true;
    public static int targetEnchantIndex = 0;
    public static final List<String> TARGET_ENCHANTS = List.of(
        "Починка I (Mending)",
        "Неразрушимость III",
        "Эффективность V",
        "Острота V",
        "Защита IV",
        "Удача III"
    );
    public static int maxEmeraldPrice = 14;

    // --- 3.8 Профиль стиля, Кастом-гардероб и Темы (как на фото 1 и 4) ---
    public static MenuTheme currentTheme = MenuTheme.CONCEPT_CYAN;
    public static boolean controlInterceptEnabled = true;
    public static int profilerTrainingPct = 84;
    public static int profilerCameraPct = 91;
    public static int profilerClickRhythmPct = 82;
    public static int profilerBuildHabitsPct = 79;

    public static final List<String> WINGS_OPTIONS = List.of(
        "Выкл", "Ангельские", "Демонические", "Неон-элитры", "Кристальные"
    );
    public static final List<String> HORNS_OPTIONS = List.of(
        "Выкл", "Дьявольские", "Оленьи", "Кибер-рожки", "Золотой нимб"
    );
    public static final List<String> HATS_OPTIONS = List.of(
        "Выкл", "Корона", "Шлем Незерита", "Колпак Повара", "Ушки Крыски"
    );
    public static final List<String> OUTFIT_OPTIONS = List.of(
        "Кибер-броня", "Тень Ассасина", "Изумрудный сет", "Незерит Неон", "Стандарт"
    );
    public static final List<String> CROSSHAIR_OPTIONS = List.of(
        "Ванильный", "Точка + Крест", "Неон-круг", "Тактический"
    );

    public static int wingsIndex = 1;
    public static int hornsIndex = 3;
    public static int hatsIndex = 1;
    public static int outfitIndex = 0;
    public static int crosshairIndex = 1;
    public static boolean worldCosmeticsF5 = true;

    // =========================================================================
    // 4. ВНУТРЕННИЙ ДВИЖОК МИРА, КАСТОМНЫХ 3D-МОДЕЛЕЙ И НАВИГАТОРА
    // =========================================================================

    private static KeyMapping openMenuKey;
    private static int worldTick = 0;

    // Кастомный составной 3D-спутник в мире (основная сущность + доп. 3D-детали)
    private static Mob companionCoreEntity = null;
    private static ArmorStand companionVisorStand = null;
    private static VadimkaForm spawnedForm = null;
    private static int spawnedScale = -1;
    private static final Map<MinionRole, Mob> minionEntities = new EnumMap<>(MinionRole.class);

    // Модели для центральной 3D-сцены в GUI (отдельно для всех 6 обликов!)
    private static final Map<VadimkaForm, LivingEntity> guiStageModels = new EnumMap<>(VadimkaForm.class);

    // Активный Навигатор (Дом / Ларчик / Житель / Точка смерти)
    public static BlockPos homeWaypoint = null;
    public static BlockPos activeNavTarget = null;
    public static String activeNavTitle = "";
    public static int activeNavColor = 0xFF22D3EE;
    private static BlockPos lastDeathPos = null;

    // Память ларчиков и библиотекарей
    public static final Map<BlockPos, ChestRecord> rememberedChests = new LinkedHashMap<>();
    public static final Map<BlockPos, VillagerRecord> rememberedVillagers = new LinkedHashMap<>();
    private static BlockPos lastLookedContainerPos = null;
    private static String lastLookedContainerName = "Ларчик";
    private static boolean wasContainerOpen = false;
    private static boolean wasMerchantOpen = false;

    // Высокоточный гладкий кэш карты (64x64 сетка с RLE + сглаживанием!)
    private static final int MAP_GRID = 64;
    private static final int[] mapColors = new int[MAP_GRID * MAP_GRID];
    private static final int[] mapHeights = new int[MAP_GRID * MAP_GRID];
    private static int mapCenterX = Integer.MIN_VALUE;
    private static int mapCenterZ = Integer.MIN_VALUE;
    private static int mapLastZoom = -1;
    private static int mapScanRow = 0;
    private static boolean mapNeedsReset = true;

    // Сканер сундуков и дворцов испытаний вокруг игрока
    private static final int SCAN_RADIUS = 18;
    private static final List<BlockPos> nearbyVaultsAndChests = new ArrayList<>();
    private static final List<BlockPos> scanBufferChests = new ArrayList<>();
    private static int scanSliceX = -SCAN_RADIUS;
    private static BlockPos scanOrigin = BlockPos.ZERO;

    // Кэш сущностей для радара и AI миньонов
    private static class RadarBlip {
        final double x;
        final double z;
        final int kind; // 0=player, 1=hostile, 2=animal, 3=villager, 4=item
        final String name;

        RadarBlip(double x, double z, int kind, String name) {
            this.x = x;
            this.z = z;
            this.kind = kind;
            this.name = name;
        }
    }

    private static final List<RadarBlip> radarBlips = new ArrayList<>();
    private static Vec3 nearestLootPos = null;
    private static Vec3 nearestHostilePos = null;
    private static double nearestHostileDist = 999.0;
    private static double nearestHostileAngle = 0.0;
    private static boolean threatBehindPlayer = false;

    // Нить Ариадны и статистика
    private static final Deque<Vec3> ariadnePoints = new ArrayDeque<>();
    private static Vec3 lastAriadnePoint = null;
    private static double sessionWalkedBlocks = 0.0;
    private static Vec3 prevPlayerPos = null;
    private static float prevPlayerHealth = 20.0F;
    private static int lastAxeWarnTick = -300;
    private static String toastBannerText = "";
    private static int toastBannerTicks = 0;
    private static final Random rng = new Random();

    // =========================================================================
    // 5. ИНИЦИАЛИЗАЦИЯ, AI-ЧАТ, SPOTIFY-МОСТ И ТИК МИРА
    // =========================================================================

    @Override
    public void onInitializeClient() {
        for (MinionRole role : MinionRole.values()) {
            activeMinions.put(role, true);
        }
        ensureBuiltInMusicEngineRunning();
        pushChatLog(true, "Привет! Я Вадимка v6.0. Нажми [P] для меню или напиши «!помощь» прямо в чат!");

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.assistant_vadimka.menu",
            GLFW.GLFW_KEY_P,
            KeyMapping.Category.MISC
        ));

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!chatAssistantEnabled || message == null) {
                return true;
            }
            String trimmed = message.trim();
            if (trimmed.isEmpty()) {
                return true;
            }
            if (trimmed.startsWith("!")) {
                String cmd = trimmed.substring(1).trim();
                pushChatLog(false, trimmed);
                String answer = handleVadimkaCommand(cmd);
                enqueueChatReply(answer, 2);
                return false;
            }
            String low = trimmed.toLowerCase(Locale.ROOT);
            if (low.contains("вадимк") || low.contains("вадим") || low.contains("vadimka")) {
                pushChatLog(false, trimmed);
                String answer = handleVadimkaCommand(trimmed);
                enqueueChatReply(answer, 5);
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                cleanupWorldCompanions();
                ariadnePoints.clear();
                nearbyVaultsAndChests.clear();
                scanBufferChests.clear();
                radarBlips.clear();
                guiStageModels.clear();
                lastAriadnePoint = null;
                prevPlayerPos = null;
                mapNeedsReset = true;
                greetedInWorld = false;
                return;
            }
            while (openMenuKey.consumeClick()) {
                if (client.screen instanceof AssistantVadimkaScreen) {
                    client.setScreen(null);
                } else {
                    client.setScreen(new AssistantVadimkaScreen(MenuTab.COMPANION));
                }
            }
            tickWorldLogic(client);
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> renderGameplayHud(graphics));
    }

    private static void pushChatLog(boolean fromVadimka, String text) {
        chatHistory.addLast(new ChatLogEntry(fromVadimka, text));
        while (chatHistory.size() > 28) {
            chatHistory.removeFirst();
        }
        if (fromVadimka) {
            vadimkaLastBubble = text;
        }
    }

    private static void enqueueChatReply(String text, int delay) {
        if (text == null || text.isEmpty()) {
            return;
        }
        pushChatLog(true, text);
        pendingChatReplies.addLast(text);
        pendingChatReplyDelay = Math.max(1, delay);
    }

    public static String askVadimkaFromGui(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) {
            return "";
        }
        String clean = prompt.trim();
        pushChatLog(false, clean);
        String reply = handleVadimkaCommand(clean);
        pushChatLog(true, reply);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§b[✦ Вадимка] §f" + reply), false);
        }
        return reply;
    }

    private static String handleVadimkaCommand(String raw) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        String low = raw.toLowerCase(Locale.ROOT)
            .replace("вадимка", "")
            .replace("вадим", "")
            .replace("vadimka", "")
            .replace(",", " ")
            .trim();

        if (low.isEmpty() || low.contains("привет") || low.contains("здаров") || low.contains("ку") || low.contains("тут")) {
            return "На связи! Сейчас я в облике «" + currentForm.russianName + "». Могу включить музыку, найти вещи в ларчиках, показать сделки библиотекарей или поставить маяк навигатора!";
        }
        if (low.contains("помощ") || low.contains("команд") || low.contains("help")) {
            return "Команды: «!дом» (задать базу), «!навигатор» / «!сброс» (маяк), «!найди <вещь>» (поиск в ларчиках), «!жители» (сделки), «!облик робот/крыска/аксолотль/вихрь/warden», «!плей», «!пауза», «!следующий», «!координаты»!";
        }
        if (low.contains("робот") || low.contains("player") || low.contains("игрок")) {
            currentForm = VadimkaForm.CYBER_PLAYER;
            return "Переключился в облик Кибер-Робота (PLAYER)!";
        }
        if (low.contains("крыс") || low.contains("rat")) {
            currentForm = VadimkaForm.RAT;
            return "Писк! Переключился в облик Кибер-Крыски!";
        }
        if (low.contains("аксолотл") || low.contains("axolotl")) {
            currentForm = VadimkaForm.AXOLOTL;
            return "Бульк! Теперь я редкий Синий Аксолотль!";
        }
        if (low.contains("вихр") || low.contains("breeze")) {
            currentForm = VadimkaForm.BREEZE;
            return "Вжух! Сменил облик на Шторм-Вихрь!";
        }
        if (low.contains("варден") || low.contains("хранител") || low.contains("warden")) {
            currentForm = VadimkaForm.WARDEN;
            return "Включил облик Мини-Хранителя!";
        }
        if (low.contains("тихон") || low.contains("спешик") || low.contains("allay")) {
            currentForm = VadimkaForm.ALLAY;
            return "Теперь я Тихоня-Дух!";
        }
        if (low.contains("следующ") || low.contains("next") || low.contains("скип")) {
            spotifyNextTrack();
            return "Переключил трек: " + getActiveTrackTitle() + " — " + getActiveTrackArtist();
        }
        if (low.contains("предыдущ") || low.contains("prev")) {
            spotifyPrevTrack();
            return "Вернул предыдущий трек: " + getActiveTrackTitle();
        }
        if (low.contains("пауз") || low.contains("плей") || low.contains("музык") || low.contains("спотифай") || low.contains("spotify")) {
            if (low.contains("пауз") || low.contains("плей")) {
                spotifyTogglePlay();
            }
            return (spotifyPlaying ? "▶ Играет: " : "⏸ На паузе: ") + getActiveTrackTitle() + " — " + getActiveTrackArtist();
        }
        if (low.contains("дом") && !low.contains("к дому")) {
            if (p != null) {
                homeWaypoint = p.blockPosition();
                setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
                return "Точка Дома сохранена (" + homeWaypoint.getX() + ", " + homeWaypoint.getY() + ", " + homeWaypoint.getZ() + "), навигатор включён!";
            }
        }
        if (low.contains("к дому") || low.contains("домой")) {
            if (homeWaypoint == null) {
                return "Сначала задай Дом командой «!дом» или в 7-й вкладке!";
            }
            setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
            return "Проложил курс навигатора к Дому (" + (int) Math.sqrt(p.blockPosition().distSqr(homeWaypoint)) + "м)!";
        }
        if (low.contains("сброс") || low.contains("выключи навигатор")) {
            clearNavigationTarget();
            return "Маяк навигатора отключён.";
        }
        if (low.contains("найд") || low.contains("ларчик") || low.contains("сундук")) {
            String q = low.replace("найди", "").replace("найти", "").replace("ларчик", "").replace("сундук", "").trim();
            if (rememberedChests.isEmpty()) {
                return "Подойди к сундуку или бочке и открой её (ПКМ) — я автоматически запомню всё внутри! Рядом подсвечено ларчиков: " + nearbyVaultsAndChests.size() + ".";
            }
            for (ChestRecord cr : rememberedChests.values()) {
                for (Map.Entry<String, Integer> entry : cr.items.entrySet()) {
                    if (q.isEmpty() || entry.getKey().toLowerCase(Locale.ROOT).contains(q)) {
                        setNavigationTarget(cr.pos, entry.getKey(), 0xFFF59E0B);
                        int dist = p != null ? (int) Math.sqrt(p.blockPosition().distSqr(cr.pos)) : 0;
                        return "Навигатор наведён на «" + entry.getKey() + "» (x" + entry.getValue() + ") в " + cr.containerName + " (" + dist + "м)!";
                    }
                }
            }
            return "В запомненных ларчиках («" + rememberedChests.size() + "») вещь «" + q + "» не найдена.";
        }
        if (low.contains("жител") || low.contains("библиотекар") || low.contains("починк")) {
            if (rememberedVillagers.isEmpty()) {
                return "Открой меню торговли (ПКМ) с любым жителем — я запишу все его книги и цены в базу!";
            }
            for (VillagerRecord vr : rememberedVillagers.values()) {
                for (VillagerTradeEntry tr : vr.trades) {
                    if (!low.contains("починк") || tr.resultText.toLowerCase(Locale.ROOT).contains("почин")) {
                        setNavigationTarget(vr.pos, tr.resultText, 0xFFC084FC);
                        return "Библиотекарь: " + tr.resultText + " за " + tr.costText + ". Маяк навигатора поставлен!";
                    }
                }
            }
            return "Записано жителей: " + rememberedVillagers.size() + ". Открой вкладку «Ларчики и Жители»!";
        }
        if (low.contains("коорд") || low.contains("статус")) {
            if (p != null) {
                return "X=" + (int) p.getX() + " Y=" + (int) p.getY() + " Z=" + (int) p.getZ()
                    + " | HP: " + (int) Math.ceil(p.getHealth())
                    + " | Тотемы: " + countPlayerTotems(p)
                    + " | Ларчиков рядом: " + nearbyVaultsAndChests.size();
            }
        }
        return "Понял тебя! Открой меню [P] (вкладки слева как на концепте) или напиши «!помощь» для списка команд.";
    }

    public static void setNavigationTarget(BlockPos pos, String title, int color) {
        activeNavTarget = pos;
        activeNavTitle = title;
        activeNavColor = color;
    }

    public static void clearNavigationTarget() {
        activeNavTarget = null;
        activeNavTitle = "";
    }

    // =========================================================================
    // 6. ГИБРИДНЫЙ SPOTIFY ПЛЕЕР (РАБОТАЕТ ВСЕГДА В MINECRAFT + СИНХРОНИЗАЦИЯ ПК)
    // =========================================================================

    public static PlaylistTrack getCurrentPlaylistTrack() {
        List<PlaylistTrack> list = PLAYLIST_TRACKS.get(spotifyPlaylistIndex % PLAYLIST_TRACKS.size());
        return list.get(spotifyTrackIndex % list.size());
    }

    public static String getActiveTrackTitle() {
        if (externalSpotifyDetected && !externalSpotifyTitle.isEmpty()) {
            return externalSpotifyTitle;
        }
        return getCurrentPlaylistTrack().title;
    }

    public static String getActiveTrackArtist() {
        if (externalSpotifyDetected && !externalSpotifyArtist.isEmpty()) {
            return externalSpotifyArtist;
        }
        return getCurrentPlaylistTrack().artist;
    }

    public static void spotifyTogglePlay() {
        spotifyPlaying = !spotifyPlaying;
        spotifyLastTickMs = System.currentTimeMillis();
        sendMediaKeyAsync(0xB3);
    }

    public static void spotifyNextTrack() {
        List<PlaylistTrack> list = PLAYLIST_TRACKS.get(spotifyPlaylistIndex % PLAYLIST_TRACKS.size());
        if (spotifyShuffle && list.size() > 1) {
            spotifyTrackIndex = (spotifyTrackIndex + 1 + rng.nextInt(list.size() - 1)) % list.size();
        } else {
            spotifyTrackIndex = (spotifyTrackIndex + 1) % list.size();
        }
        spotifyTrackElapsedMs = 0L;
        spotifyPlaying = true;
        spotifyLastTickMs = System.currentTimeMillis();
        sendMediaKeyAsync(0xB0);
    }

    public static void spotifyPrevTrack() {
        List<PlaylistTrack> list = PLAYLIST_TRACKS.get(spotifyPlaylistIndex % PLAYLIST_TRACKS.size());
        if (spotifyTrackElapsedMs > 5000L) {
            spotifyTrackElapsedMs = 0L;
        } else {
            spotifyTrackIndex = (spotifyTrackIndex - 1 + list.size()) % list.size();
            spotifyTrackElapsedMs = 0L;
        }
        spotifyPlaying = true;
        spotifyLastTickMs = System.currentTimeMillis();
        sendMediaKeyAsync(0xB1);
    }

    private static void tickSpotifyClockAndSync() {
        long now = System.currentTimeMillis();
        long dt = Math.max(0L, Math.min(1000L, now - spotifyLastTickMs));
        spotifyLastTickMs = now;

        if (spotifyPlaying) {
            PlaylistTrack cur = getCurrentPlaylistTrack();
            spotifyTrackElapsedMs += dt;
            long totalMs = cur.durationSec * 1000L;
            if (spotifyTrackElapsedMs >= totalMs) {
                if (spotifyRepeat) {
                    spotifyNextTrack();
                } else {
                    spotifyTrackElapsedMs = totalMs;
                    spotifyPlaying = false;
                }
            }
        }

        if (!externalPollBusy && (now - lastExternalPollMs) >= 3500L) {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            if (!os.contains("win")) {
                return;
            }
            externalPollBusy = true;
            lastExternalPollMs = now;
            spotifySysExecutor.submit(() -> {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                        "powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command",
                        "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8; "
                            + "$p = Get-Process -Name Spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -and $_.MainWindowTitle.Length -gt 0 } | Select-Object -First 1; "
                            + "if ($p) { Write-Output $p.MainWindowTitle } else { Write-Output '' }"
                    );
                    pb.redirectErrorStream(true);
                    Process proc = pb.start();
                    String title = "";
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (!line.trim().isEmpty()) {
                                title = line.trim();
                                break;
                            }
                        }
                    }
                    proc.waitFor(2, TimeUnit.SECONDS);
                    if (!title.isEmpty() && !title.equalsIgnoreCase("Spotify")
                            && !title.equalsIgnoreCase("Spotify Free")
                            && !title.equalsIgnoreCase("Spotify Premium")) {
                        externalSpotifyDetected = true;
                        int sep = title.indexOf(" - ");
                        if (sep > 0) {
                            externalSpotifyArtist = title.substring(0, sep).trim();
                            externalSpotifyTitle = title.substring(sep + 3).trim();
                        } else {
                            externalSpotifyArtist = "Spotify";
                            externalSpotifyTitle = title;
                        }
                    } else {
                        externalSpotifyDetected = false;
                        externalSpotifyTitle = "";
                        externalSpotifyArtist = "";
                    }
                } catch (Throwable ignored) {
                    externalSpotifyDetected = false;
                } finally {
                    externalPollBusy = false;
                }
            });
        }
    }

    private static void sendMediaKeyAsync(int vk) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("win")) {
            return;
        }
        spotifySysExecutor.submit(() -> {
            try {
                String cmd = "Add-Type -TypeDefinition 'using System.Runtime.InteropServices; "
                    + "public class K { [DllImport(\"user32.dll\")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, int dwExtraInfo); }'; "
                    + "[K]::keybd_event(" + vk + ",0,0,0); [K]::keybd_event(" + vk + ",0,2,0);";
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden", "-Command", cmd);
                pb.redirectErrorStream(true);
                Process p = pb.start();
                p.waitFor(2, TimeUnit.SECONDS);
            } catch (Throwable ignored) {
            }
        });
    }

    // =========================================================================
    // 7. КАСТОМНЫЕ СОСТАВНЫЕ 3D-МОДЕЛИ ВАДИМКИ В МИРЕ И В GUI
    // =========================================================================

    private static void cleanupWorldCompanions() {
        if (companionCoreEntity != null) {
            companionCoreEntity.discard();
            companionCoreEntity = null;
        }
        if (companionVisorStand != null) {
            companionVisorStand.discard();
            companionVisorStand = null;
        }
        spawnedForm = null;
        for (Mob m : minionEntities.values()) {
            if (m != null) {
                m.discard();
            }
        }
        minionEntities.clear();
    }

    private static LivingEntity getCustomStudioModel(ClientLevel level, Player player, VadimkaForm form) {
        LivingEntity cached = guiStageModels.get(form);
        if (cached != null && !cached.isRemoved() && cached.level() == level) {
            return cached;
        }
        LivingEntity created = buildCustomVisualEntity(level, player, form, true);
        if (created != null) {
            guiStageModels.put(form, created);
        }
        return created != null ? created : player;
    }

    private static LivingEntity buildCustomVisualEntity(ClientLevel level, Player player, VadimkaForm form, boolean forGui) {
        if (form == VadimkaForm.CYBER_PLAYER) {
            // Кастомный 3D Кибер-Робот Вадимка (стойка в экзоскелете с алмазным/незеритовым визором и ядром!)
            ArmorStand bot = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.LOAD);
            if (bot != null) {
                bot.setNoGravity(true);
                bot.setSilent(true);
                bot.setInvulnerable(true);
                bot.setShowArms(true);
                bot.setNoBasePlate(true);
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
                chest.set(DataComponents.DYED_COLOR, new DyedItemColor(0x1E293B));
                ItemStack legs = new ItemStack(Items.LEATHER_LEGGINGS);
                legs.set(DataComponents.DYED_COLOR, new DyedItemColor(0x0F172A));
                ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
                ItemStack coreHand = new ItemStack(Items.END_CRYSTAL);
                bot.setItemSlot(EquipmentSlot.HEAD, head);
                bot.setItemSlot(EquipmentSlot.CHEST, chest);
                bot.setItemSlot(EquipmentSlot.LEGS, legs);
                bot.setItemSlot(EquipmentSlot.FEET, boots);
                bot.setItemSlot(EquipmentSlot.MAINHAND, coreHand);
                if (!forGui) {
                    applyScaleAttribute(bot, form.worldScale * (companionScalePercent / 100.0));
                }
                return bot;
            }
        }
        if (form == VadimkaForm.AXOLOTL) {
            Axolotl ax = EntityType.AXOLOTL.create(level, EntitySpawnReason.LOAD);
            if (ax != null) {
                ax.setNoAi(true);
                ax.setNoGravity(true);
                ax.setSilent(true);
                ax.setInvulnerable(true);
                setAxolotlBlueVariant(ax); // Строго редкий СИНИЙ аксолотль (через безопасный обход private setVariant)
                if (!forGui) {
                    applyScaleAttribute(ax, form.worldScale * (companionScalePercent / 100.0));
                }
                return ax;
            }
        }
        if (form == VadimkaForm.RAT) {
            // Кастомная Кибер-Крыска: Silverfish + розово-неоновый обвес!
            Mob rat = EntityType.SILVERFISH.create(level, EntitySpawnReason.LOAD);
            if (rat != null) {
                rat.setNoAi(true);
                rat.setNoGravity(true);
                rat.setSilent(true);
                rat.setInvulnerable(true);
                if (!forGui) {
                    applyScaleAttribute(rat, form.worldScale * (companionScalePercent / 100.0));
                }
                return rat;
            }
        }
        EntityType<? extends Mob> type = switch (form) {
            case ALLAY -> EntityType.ALLAY;
            case BREEZE -> EntityType.BREEZE;
            case WARDEN -> EntityType.WARDEN;
            default -> EntityType.ALLAY;
        };
        Mob mob = type.create(level, EntitySpawnReason.LOAD);
        if (mob != null) {
            mob.setNoAi(true);
            mob.setNoGravity(true);
            mob.setSilent(true);
            mob.setInvulnerable(true);
            if (!forGui) {
                applyScaleAttribute(mob, form.worldScale * (companionScalePercent / 100.0));
            }
        }
        return mob;
    }


    private static void setAxolotlBlueVariant(Axolotl ax) {
        try {
            for (java.lang.reflect.Method m : Axolotl.class.getDeclaredMethods()) {
                if (!java.lang.reflect.Modifier.isStatic(m.getModifiers())
                        && !m.isSynthetic()
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == Axolotl.Variant.class
                        && m.getReturnType() == void.class) {
                    m.setAccessible(true);
                    m.invoke(ax, Axolotl.Variant.BLUE);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void applyScaleAttribute(LivingEntity entity, double scale) {
        AttributeInstance inst = entity.getAttribute(Attributes.SCALE);
        if (inst != null) {
            inst.setBaseValue(Mth.clamp(scale, 0.25, 2.0));
        }
    }

    private static void tickWorldLogic(Minecraft mc) {
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) {
            return;
        }
        worldTick++;
        if (toastBannerTicks > 0) {
            toastBannerTicks--;
        }

        // Часы плеера и синхронизация Spotify
        tickSpotifyClockAndSync();

        // Приветствие при заходе в мир
        if (!greetedInWorld && worldTick > 25) {
            greetedInWorld = true;
            if (chatAssistantEnabled && !quietMode) {
                player.displayClientMessage(Component.literal(
                    "§b[✦ Вадимка v6.0] §fГотов! Меню на §e[P]§f, команды в чат через §a«!помощь»§f."
                ), false);
            }
        }

        // Отложенные ответы Вадимки в чат
        if (!pendingChatReplies.isEmpty()) {
            if (pendingChatReplyDelay > 0) {
                pendingChatReplyDelay--;
            } else {
                String msg = pendingChatReplies.pollFirst();
                if (msg != null) {
                    player.displayClientMessage(Component.literal("§b[✦ Вадимка] §f" + msg), false);
                }
                pendingChatReplyDelay = 3;
            }
        }

        // Запоминаем блок сундука/бочки/ларчика при наведении прицела
        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = bhr.getBlockPos();
            BlockState bs = level.getBlockState(bp);
            if (isLootContainerBlock(bs)) {
                lastLookedContainerPos = bp.immutable();
                lastLookedContainerName = bs.getBlock().getName().getString();
            }
        }

        // Авто-запись содержимого открытых ларчиков и торгов библиотекарей
        scanOpenContainerOrMerchant(mc, player);

        // Профайлер движения
        Vec3 curPos = player.position();
        if (prevPlayerPos != null) {
            double d = curPos.distanceTo(prevPlayerPos);
            if (d < 10.0) {
                sessionWalkedBlocks += d;
            }
        }
        prevPlayerPos = curPos;

        // Здоровье, урон, точка смерти
        float hp = player.getHealth();
        if (hp <= 0.0F && prevPlayerHealth > 0.0F && deathReplayMarker) {
            lastDeathPos = player.blockPosition();
            setNavigationTarget(lastDeathPos, "Точка смерти", 0xFFEF4444);
            showToast("Вадимка: Навигатор наведён на точку смерти!", 110);
        } else if (hp < prevPlayerHealth - 0.1F) {
            if (afkPanicExit && mc.screen instanceof AssistantVadimkaScreen scr && scr.afkGameRunning) {
                scr.afkGameRunning = false;
                mc.setScreen(null);
                showToast("Вадимка: ЭКСТРЕННЫЙ ВЫХОД ИЗ AFK!", 90);
            }
        }
        prevPlayerHealth = hp;

        // Предупреждение о прочности топора/кирки
        if (warnAxeDurability && worldTick % 25 == 0) {
            ItemStack hand = player.getMainHandItem();
            if (!hand.isEmpty() && hand.isDamageableItem() && hand.getMaxDamage() > 0) {
                int left = hand.getMaxDamage() - hand.getDamageValue();
                if (left > 0 && left <= 12 && (worldTick - lastAxeWarnTick) > 240) {
                    showToast("⚠ Прочность «" + hand.getHoverName().getString() + "»: осталось " + left + "!", 80);
                    lastAxeWarnTick = worldTick;
                }
            }
        }

        // 1. Сканирование сущностей (раз в 4 тика)
        if (worldTick % 4 == 0) {
            scanEntitiesAround(level, player);
        }

        // 2. Обновление кастомного 3D-спутника Вадимки и миньонов в мире
        updateCustomCompanionInWorld(mc, level, player);

        // 3. 3D-крылья и нимб/рога в F5
        if (worldCosmeticsF5 && worldTick % 2 == 0 && !mc.options.getCameraType().isFirstPerson()) {
            spawnWorldCosmetics(level, player);
        }

        // 4. Подсветка ближайших ларчиков/сундуков в мире и луч навигатора!
        if (worldTick % 5 == 0) {
            spawnWorldBeaconsAndVaultHighlights(level, player);
        }

        // 5. Нить Ариадны
        if (ariadneThread) {
            Vec3 crumb = new Vec3(player.getX(), player.getY() + 0.22, player.getZ());
            if (lastAriadnePoint == null || lastAriadnePoint.distanceToSqr(crumb) > 12.25) {
                ariadnePoints.addLast(crumb);
                lastAriadnePoint = crumb;
                while (ariadnePoints.size() > 36) {
                    ariadnePoints.removeFirst();
                }
            }
            if (worldTick % 10 == 0) {
                int idx = 0;
                for (Vec3 c : ariadnePoints) {
                    if ((idx + worldTick / 10) % 4 == 0 && c.distanceToSqr(crumb) < 324.0) {
                        level.addParticle(ParticleTypes.ENCHANT, c.x, c.y + 0.15, c.z, 0.0, 0.015, 0.0);
                    }
                    idx++;
                }
            }
        }

        // 6. Гладкое обновление кэша мини-карты (по 4 строки за тик = 0 лагов!)
        if (minimapRadarEnabled && minimapTerrainEnabled) {
            tickSmoothTerrainMap(level, Mth.floor(player.getX()), Mth.floor(player.getZ()));
        }

        // 7. Инкрементальный сканер ларчиков/сундуков вокруг
        if (worldTick % 2 == 0) {
            tickVaultAndChestScanner(level, player.blockPosition());
        }
    }

    private static boolean isLootContainerBlock(BlockState st) {
        if (st.is(Blocks.CHEST) || st.is(Blocks.TRAPPED_CHEST)
                || st.is(Blocks.BARREL) || st.is(Blocks.ENDER_CHEST)) {
            return true;
        }
        String name = st.getBlock().getName().getString().toLowerCase(Locale.ROOT);
        return name.contains("шалкер") || name.contains("shulker")
            || name.contains("хранилище") || name.contains("vault") || name.contains("ларчик");
    }

    private static void spawnWorldBeaconsAndVaultHighlights(ClientLevel level, Player player) {
        // Подсветка ближайших ларчиков и сундуков неоновыми частицами
        if (highlightTrialVaults || highlightChestsWorld) {
            int shown = 0;
            for (BlockPos pos : nearbyVaultsAndChests) {
                if (pos.distSqr(player.blockPosition()) <= 256.0) {
                    level.addParticle(
                        new DustParticleOptions(0x22D3EE, 0.85F),
                        pos.getX() + 0.5, pos.getY() + 1.08, pos.getZ() + 0.5,
                        0.0, 0.02, 0.0
                    );
                    shown++;
                    if (shown >= 8) break;
                }
            }
        }

        // 3D-луч навигатора и направляющие частицы к активной цели!
        if (activeNavTarget != null) {
            double tx = activeNavTarget.getX() + 0.5;
            double ty = activeNavTarget.getY() + 1.0;
            double tz = activeNavTarget.getZ() + 0.5;

            // Вертикальный маяк на самой цели
            for (int h = 0; h < 6; h++) {
                level.addParticle(
                    new DustParticleOptions(activeNavColor & 0x00FFFFFF, 1.35F),
                    tx, ty + h * 1.4, tz, 0.0, 0.05, 0.0
                );
            }

            // Пунктирная дорожка в сторону цели от игрока
            Vec3 pEye = player.position().add(0.0, 0.8, 0.0);
            Vec3 dir = new Vec3(tx - pEye.x, 0.0, tz - pEye.z);
            double dist = dir.length();
            if (dist > 2.5) {
                Vec3 step = dir.normalize();
                int dots = (int) Math.min(8, dist / 2.0);
                for (int i = 1; i <= dots; i++) {
                    double d = i * 2.0 + ((worldTick % 10) * 0.2);
                    if (d < dist) {
                        level.addParticle(
                            new DustParticleOptions(activeNavColor & 0x00FFFFFF, 1.0F),
                            pEye.x + step.x * d, player.getY() + 0.4, pEye.z + step.z * d,
                            0.0, 0.01, 0.0
                        );
                    }
                }
            }
        }
    }

    private static void scanOpenContainerOrMerchant(Minecraft mc, Player player) {
        AbstractContainerMenu menu = player.containerMenu;
        boolean open = menu != null && menu != player.inventoryMenu && mc.screen != null && !(mc.screen instanceof AssistantVadimkaScreen);

        if (open && menu instanceof MerchantMenu merchantMenu) {
            if (!wasMerchantOpen || worldTick % 20 == 0) {
                MerchantOffers offers = merchantMenu.getOffers();
                if (offers != null && !offers.isEmpty()) {
                    BlockPos vPos = player.blockPosition();
                    VillagerRecord rec = new VillagerRecord(vPos, "Библиотекарь [" + vPos.getX() + ", " + vPos.getZ() + "]");
                    for (MerchantOffer offer : offers) {
                        String cA = formatItemShort(offer.getCostA());
                        String cB = offer.getCostB().isEmpty() ? "" : (" + " + formatItemShort(offer.getCostB()));
                        String res = formatItemWithEnchants(offer.getResult());
                        int emPrice = offer.getCostA().is(Items.EMERALD) ? offer.getCostA().getCount() : 99;
                        boolean good = res.toLowerCase(Locale.ROOT).contains("почин")
                            || res.toLowerCase(Locale.ROOT).contains("mending")
                            || res.toLowerCase(Locale.ROOT).contains("неразруш")
                            || res.toLowerCase(Locale.ROOT).contains("эффектив")
                            || res.toLowerCase(Locale.ROOT).contains("острот")
                            || res.toLowerCase(Locale.ROOT).contains("защит")
                            || offer.getResult().is(Items.ENCHANTED_BOOK);
                        rec.trades.add(new VillagerTradeEntry(cA + cB, res, emPrice, good, offer.isOutOfStock()));
                    }
                    rememberedVillagers.put(vPos, rec);
                    if (!wasMerchantOpen) {
                        showToast("Библиотекарь записан (" + rec.trades.size() + " сделок)!", 65);
                    }
                }
            }
            wasMerchantOpen = true;
            wasContainerOpen = false;
            return;
        }
        wasMerchantOpen = false;

        if (open && baseChestMemory && lastLookedContainerPos != null) {
            if (!wasContainerOpen || worldTick % 15 == 0) {
                ChestRecord rec = new ChestRecord(lastLookedContainerPos, lastLookedContainerName);
                for (Slot slot : menu.slots) {
                    if (slot.container == player.getInventory()) {
                        continue;
                    }
                    ItemStack st = slot.getItem();
                    if (!st.isEmpty()) {
                        String itemName = formatItemWithEnchants(st);
                        rec.items.merge(itemName, st.getCount(), Integer::sum);
                    }
                }
                rememberedChests.put(lastLookedContainerPos, rec);
                if (!wasContainerOpen) {
                    showToast("Ларчик сохранён (" + rec.items.size() + " видов вещей)!", 60);
                }
            }
            wasContainerOpen = true;
        } else {
            wasContainerOpen = false;
        }
    }

    private static String formatItemShort(ItemStack stack) {
        if (stack.isEmpty()) return "";
        return stack.getHoverName().getString() + (stack.getCount() > 1 ? (" x" + stack.getCount()) : "");
    }

    private static String formatItemWithEnchants(ItemStack stack) {
        if (stack.isEmpty()) return "";
        String base = stack.getHoverName().getString();
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        ItemEnchantments active = stack.get(DataComponents.ENCHANTMENTS);
        ItemEnchantments target = (stored != null && !stored.isEmpty()) ? stored : active;
        if (target != null && !target.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            int c = 0;
            for (var e : target.entrySet()) {
                if (c > 0) sb.append(", ");
                sb.append(Enchantment.getFullname(e.getKey(), e.getValue()).getString());
                c++;
                if (c >= 2) break;
            }
            return stack.is(Items.ENCHANTED_BOOK) ? ("Книга: " + sb) : (base + " (" + sb + ")");
        }
        return base + (stack.getCount() > 1 ? (" x" + stack.getCount()) : "");
    }

    private static void updateCustomCompanionInWorld(Minecraft mc, ClientLevel level, Player player) {
        if (!companionEnabled) {
            cleanupWorldCompanions();
            return;
        }

        boolean needRebuild = companionCoreEntity == null
            || companionCoreEntity.isRemoved()
            || companionCoreEntity.level() != level
            || spawnedForm != currentForm;

        if (needRebuild) {
            if (companionCoreEntity != null) {
                companionCoreEntity.discard();
                companionCoreEntity = null;
            }
            if (companionVisorStand != null) {
                companionVisorStand.discard();
                companionVisorStand = null;
            }

            if (currentForm == VadimkaForm.CYBER_PLAYER) {
                // Кастомный составной Кибер-Робот из 2 синхронизированных частей!
                ArmorStand stand = (ArmorStand) buildCustomVisualEntity(level, player, currentForm, false);
                Mob coreOrb = EntityType.ALLAY.create(level, EntitySpawnReason.LOAD);
                if (stand != null && coreOrb != null) {
                    stand.setId(-77001);
                    stand.setCustomName(Component.literal("§b✦ Вадимка"));
                    stand.setCustomNameVisible(true);
                    stand.setPos(player.getX() + 1.0, player.getY() + 1.0, player.getZ() + 1.0);
                    level.addEntity(stand);

                    coreOrb.setId(-77002);
                    coreOrb.setNoAi(true);
                    coreOrb.setNoGravity(true);
                    coreOrb.setSilent(true);
                    coreOrb.setInvulnerable(true);
                    coreOrb.setCustomNameVisible(false);
                    applyScaleAttribute(coreOrb, 0.34 * (companionScalePercent / 100.0));
                    coreOrb.setPos(player.getX() + 1.0, player.getY() + 1.4, player.getZ() + 1.0);
                    level.addEntity(coreOrb);

                    companionVisorStand = stand;
                    companionCoreEntity = coreOrb;
                    spawnedForm = currentForm;
                    spawnedScale = companionScalePercent;
                }
            } else {
                LivingEntity ent = buildCustomVisualEntity(level, player, currentForm, false);
                if (ent instanceof Mob mob) {
                    mob.setId(-77001);
                    mob.setCustomName(Component.literal("§b✦ Вадимка"));
                    mob.setCustomNameVisible(true);
                    mob.setPos(player.getX() + 1.0, player.getY() + 1.0, player.getZ() + 1.0);
                    level.addEntity(mob);
                    companionCoreEntity = mob;
                    spawnedForm = currentForm;
                    spawnedScale = companionScalePercent;
                }
            }
        } else if (spawnedScale != companionScalePercent) {
            if (companionVisorStand != null) {
                applyScaleAttribute(companionVisorStand, currentForm.worldScale * (companionScalePercent / 100.0));
            }
            if (companionCoreEntity != null) {
                double mul = (currentForm == VadimkaForm.CYBER_PLAYER) ? 0.34 : currentForm.worldScale;
                applyScaleAttribute(companionCoreEntity, mul * (companionScalePercent / 100.0));
            }
            spawnedScale = companionScalePercent;
        }

        // Расчёт точной 3D-позиции по X / Y / Z слайдерам из меню!
        double yawRad = Math.toRadians(player.getYRot());
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        PlaylistTrack tr = getCurrentPlaylistTrack();
        double bpmFreq = (vadimkaDanceToBpm && spotifyPlaying) ? (tr.bpm / 480.0) : 0.14;
        double danceBob = Math.sin(worldTick * bpmFreq) * 0.12;

        double targetX = player.getX() + rightX * offsetX + forwardX * offsetZ;
        double targetY = player.getY() + offsetY + danceBob;
        double targetZ = player.getZ() + rightZ * offsetX + forwardZ * offsetZ;

        if (companionVisorStand != null) {
            companionVisorStand.xo = companionVisorStand.getX();
            companionVisorStand.yo = companionVisorStand.getY();
            companionVisorStand.zo = companionVisorStand.getZ();
            double nx = Mth.lerp(0.24, companionVisorStand.getX(), targetX);
            double ny = Mth.lerp(0.24, companionVisorStand.getY(), targetY - 0.35);
            double nz = Mth.lerp(0.24, companionVisorStand.getZ(), targetZ);
            companionVisorStand.setPos(nx, ny, nz);
            companionVisorStand.setYRot(player.getYRot());
            companionVisorStand.setYHeadRot(player.getYRot());
        }

        if (companionCoreEntity != null) {
            companionCoreEntity.xo = companionCoreEntity.getX();
            companionCoreEntity.yo = companionCoreEntity.getY();
            companionCoreEntity.zo = companionCoreEntity.getZ();
            double extraH = (companionVisorStand != null) ? 0.72 : 0.0;
            double nx = Mth.lerp(0.24, companionCoreEntity.getX(), targetX);
            double ny = Mth.lerp(0.24, companionCoreEntity.getY(), targetY + extraH);
            double nz = Mth.lerp(0.24, companionCoreEntity.getZ(), targetZ);
            companionCoreEntity.setPos(nx, ny, nz);

            float lookYaw = player.getYRot();
            companionCoreEntity.setYRot(lookYaw);
            companionCoreEntity.setYHeadRot(lookYaw);
            companionCoreEntity.setYBodyRot(lookYaw);

            if (worldTick % 10 == 0) {
                level.addParticle(
                    new DustParticleOptions(currentForm.primaryColor & 0x00FFFFFF, 0.9F),
                    nx, ny + 0.25, nz, 0.0, 0.02, 0.0
                );
            }
        }

        // Обновление 4 миньонов
        MinionRole[] roles = MinionRole.values();
        for (int i = 0; i < roles.length; i++) {
            MinionRole role = roles[i];
            boolean on = companionEnabled && minionsEnabled && Boolean.TRUE.equals(activeMinions.get(role));
            Mob existing = minionEntities.get(role);
            if (!on) {
                if (existing != null) {
                    existing.discard();
                    minionEntities.remove(role);
                }
                continue;
            }
            if (existing == null || existing.isRemoved() || existing.level() != level) {
                if (existing != null) existing.discard();
                Mob m = EntityType.ALLAY.create(level, EntitySpawnReason.LOAD);
                if (m != null) {
                    m.setId(-77010 - i);
                    m.setNoAi(true);
                    m.setNoGravity(true);
                    m.setSilent(true);
                    m.setInvulnerable(true);
                    m.setCustomName(Component.literal(compactNameplates ? role.compactTag : role.fullTag));
                    m.setCustomNameVisible(true);
                    applyScaleAttribute(m, 0.54);
                    m.setPos(player.getX(), player.getY() + 1.5, player.getZ());
                    level.addEntity(m);
                    minionEntities.put(role, m);
                    existing = m;
                }
            }
            if (existing != null) {
                double slotAng = yawRad + Math.PI + ((i - 1.5) * 0.72);
                double tx = player.getX() - Math.sin(slotAng) * 2.1;
                double tz = player.getZ() + Math.cos(slotAng) * 2.1;
                double ty = player.getY() + 1.55 + Math.sin(worldTick * 0.11 + i * 1.5) * 0.16;

                if (minionsSmartAi) {
                    if (role == MinionRole.COLLECTOR && nearestLootPos != null) {
                        tx = nearestLootPos.x;
                        ty = nearestLootPos.y + 0.85;
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
                    Mth.lerp(0.15, existing.getX(), tx),
                    Mth.lerp(0.15, existing.getY(), ty),
                    Mth.lerp(0.15, existing.getZ(), tz)
                );
                float mYaw = (float) (Mth.atan2(player.getZ() - existing.getZ(), player.getX() - existing.getX()) * (180.0 / Math.PI)) - 90.0F;
                existing.setYRot(mYaw);
                existing.setYHeadRot(mYaw);
            }
        }
    }

    private static void spawnWorldCosmetics(ClientLevel level, Player player) {
        double yawRad = Math.toRadians(player.getYRot());
        double backX = Math.sin(yawRad);
        double backZ = -Math.cos(yawRad);
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);

        if (wingsIndex > 0) {
            int col = switch (wingsIndex) {
                case 1 -> 0xF8FAFC;
                case 2 -> 0xEF4444;
                case 3 -> 0x22D3EE;
                default -> 0xC084FC;
            };
            double flap = Math.sin(worldTick * 0.22) * 0.14;
            for (int side = -1; side <= 1; side += 2) {
                for (int step = 1; step <= 3; step++) {
                    if ((worldTick + step) % 4 != 0) continue;
                    double spread = 0.28 * step;
                    double up = 1.06 + (step == 2 ? 0.28 : (step == 3 ? 0.14 : 0.05)) + flap * step;
                    double back = 0.34 + 0.12 * step;
                    level.addParticle(
                        new DustParticleOptions(col, 0.95F),
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
        }
    }

    private static void scanEntitiesAround(ClientLevel level, Player player) {
        radarBlips.clear();
        threatBehindPlayer = false;
        nearestHostileDist = 999.0;
        nearestHostilePos = null;
        nearestLootPos = null;
        double bestLootSq = 121.0;

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
            if (entity instanceof Player other) {
                radarBlips.add(new RadarBlip(entity.getX(), entity.getZ(), 0, other.getName().getString()));
            } else if (entity instanceof Monster) {
                radarBlips.add(new RadarBlip(entity.getX(), entity.getZ(), 1, null));
                if (Math.abs(entity.getY() - player.getY()) <= 8.0) {
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
            } else if (entity.getType() == EntityType.VILLAGER) {
                radarBlips.add(new RadarBlip(entity.getX(), entity.getZ(), 3, null));
            } else if (entity instanceof Animal) {
                radarBlips.add(new RadarBlip(entity.getX(), entity.getZ(), 2, null));
            } else if (entity instanceof ItemEntity) {
                radarBlips.add(new RadarBlip(entity.getX(), entity.getZ(), 4, null));
                double dSq = dx * dx + dz * dz;
                if (dSq < bestLootSq && Math.abs(entity.getY() - player.getY()) <= 5.0) {
                    bestLootSq = dSq;
                    nearestLootPos = entity.position();
                }
            }
        }
    }

    private static void tickSmoothTerrainMap(ClientLevel level, int px, int pz) {
        int step = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        int snapX = Math.floorDiv(px, step) * step;
        int snapZ = Math.floorDiv(pz, step) * step;

        if (mapNeedsReset || mapLastZoom != radarZoomLevel
                || Math.abs(snapX - mapCenterX) >= step * 4
                || Math.abs(snapZ - mapCenterZ) >= step * 4) {
            mapCenterX = snapX;
            mapCenterZ = snapZ;
            mapLastZoom = radarZoomLevel;
            mapScanRow = 0;
            mapNeedsReset = false;
        }

        int half = MAP_GRID / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int r = 0; r < 4; r++) {
            int gz = mapScanRow;
            int wz = mapCenterZ + (gz - half) * step;
            for (int gx = 0; gx < MAP_GRID; gx++) {
                int wx = mapCenterX + (gx - half) * step;
                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                pos.set(wx, topY, wz);
                BlockState st = level.getBlockState(pos);
                MapColor mc = st.getMapColor(level, pos);
                if (mc.col == 0 && topY > level.getMinY() + 2) {
                    pos.set(wx, topY - 1, wz);
                    st = level.getBlockState(pos);
                    mc = st.getMapColor(level, pos);
                }
                int idx = gz * MAP_GRID + gx;
                mapHeights[idx] = topY;
                int rgb = mc.col != 0 ? mc.col : 0x16202B;
                if (gz > 0) {
                    int ny = mapHeights[(gz - 1) * MAP_GRID + gx];
                    if (topY > ny) rgb = adjustBrightness(rgb, 16);
                    else if (topY < ny) rgb = adjustBrightness(rgb, -18);
                }
                mapColors[idx] = 0xFF000000 | rgb;
            }
            mapScanRow = (mapScanRow + 1) % MAP_GRID;
        }
    }

    private static void tickVaultAndChestScanner(ClientLevel level, BlockPos center) {
        if (scanSliceX == -SCAN_RADIUS) {
            scanOrigin = center;
            scanBufferChests.clear();
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinY(), scanOrigin.getY() - 5);
        int maxY = Math.min(level.getMaxY() - 1, scanOrigin.getY() + 5);
        int wx = scanOrigin.getX() + scanSliceX;

        for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
            int wz = scanOrigin.getZ() + dz;
            for (int y = minY; y <= maxY; y++) {
                pos.set(wx, y, wz);
                BlockState st = level.getBlockState(pos);
                if (isLootContainerBlock(st)) {
                    if (scanBufferChests.size() < 36) {
                        scanBufferChests.add(pos.immutable());
                    }
                }
            }
        }
        scanSliceX++;
        if (scanSliceX > SCAN_RADIUS) {
            nearbyVaultsAndChests.clear();
            nearbyVaultsAndChests.addAll(scanBufferChests);
            scanSliceX = -SCAN_RADIUS;
        }
    }

    private static void showToast(String text, int ticks) {
        if (quietMode) return;
        toastBannerText = text;
        toastBannerTicks = ticks;
        vadimkaLastBubble = text;
    }

    private static int countPlayerTotems(Player p) {
        int totems = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(Items.TOTEM_OF_UNDYING)) totems += st.getCount();
        }
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
            totems += p.getOffhandItem().getCount();
        }
        return totems;
    }

    private static int countPlayerEmeralds(Player p) {
        int em = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(Items.EMERALD)) em += st.getCount();
        }
        return em;
    }

    // =========================================================================
    // 8. ИГРОВОЙ HUD: ГЛАДКАЯ КАРТА-РАДАР, НАВИГАТОР, ЭЛИТРА-ГОРИЗОНТ, SPOTIFY, PVP
    // =========================================================================

    private static void renderGameplayHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        if (mc.screen instanceof AssistantVadimkaScreen) {
            return;
        }

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        // 1. Гладкая мини-карта-радар слева сверху (под строкой FPS)
        if (visualsEnabled && minimapRadarEnabled) {
            renderSmoothMinimapHud(g, mc);
        }

        // 2. Элитра Пич-Горизонт при полёте на элитрах (как на фото 6)
        if (visualsEnabled && elytraAssistant && elytraPitchHorizon && mc.player.isFallFlying()) {
            renderElytraHorizonHud(g, mc.font, mc.player, sw, sh);
        }

        // 3. Кастомный прицел
        if (crosshairIndex > 0 && mc.screen == null) {
            renderCustomCrosshair(g, sw, sh);
        }

        // 4. Активный Навигатор сверху по центру (стрелка направления + дистанция до Дома/Ларчика/Жителя)
        if (backstabIndicator && threatBehindPlayer) {
            String warn = "⚠ УГРОЗА СЗАДИ (" + String.format("%.1f", nearestHostileDist) + "м)";
            drawTopPill(g, mc.font, sw, 10, warn, 0xE63B0A12, 0xFFEF4444, 0xFFFCA5A5);
        } else if (activeNavTarget != null) {
            renderActiveNavigatorBanner(g, mc.font, mc.player, sw);
        }

        // 5. Litematica HUD справа сверху (как на фото 3)
        if (litematicaHudEnabled) {
            renderLitematicaRightHud(g, mc, sw);
        }

        // 6. Боевой и PvP HUD справа снизу (броня в %, тотемы, расходники, как на фото 7)
        if (combatHudEnabled) {
            renderCombatPvpHud(g, mc, sw, sh);
        }

        // 7. Spotify Плеер HUD слева снизу (обложка, прогресс-бар, трек, как на фото 8)
        if (spotifyHudEnabled) {
            renderSpotifyBottomHud(g, mc, sh);
        }

        // 8. Всплывающее уведомление Вадимки
        if (toastBannerTicks > 0 && !toastBannerText.isEmpty()) {
            int tw = mc.font.width(toastBannerText) + 18;
            int tx = (sw - tw) / 2;
            int ty = sh - 60;
            fillGlassCard(g, tx, ty, tw, 18, currentTheme.bgWindow, currentTheme.accent);
            g.drawString(mc.font, toastBannerText, tx + 9, ty + 5, 0xFFF8FAFC);
        }
    }

    private static void drawTopPill(GuiGraphics g, Font font, int sw, int y, String text, int bg, int border, int fg) {
        int w = font.width(text) + 18;
        int x = (sw - w) / 2;
        fillGlassCard(g, x, y, w, 18, bg, border);
        g.drawString(font, text, x + 9, y + 5, fg);
    }

    private static void renderActiveNavigatorBanner(GuiGraphics g, Font font, Player player, int sw) {
        if (activeNavTarget == null) return;
        double dx = (activeNavTarget.getX() + 0.5) - player.getX();
        double dz = (activeNavTarget.getZ() + 0.5) - player.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);

        double targetYaw = Math.toDegrees(Math.atan2(dz, dx)) - 90.0;
        double rel = Mth.wrapDegrees(targetYaw - player.getYRot());
        String arrow;
        if (Math.abs(rel) < 22.5) arrow = "⬆";
        else if (rel >= 22.5 && rel < 67.5) arrow = "↗";
        else if (rel >= 67.5 && rel < 112.5) arrow = "➡";
        else if (rel >= 112.5 && rel < 157.5) arrow = "↘";
        else if (rel <= -22.5 && rel > -67.5) arrow = "↖";
        else if (rel <= -67.5 && rel > -112.5) arrow = "⬅";
        else if (rel <= -112.5 && rel > -157.5) arrow = "↙";
        else arrow = "⬇";

        String title = activeNavTitle.isEmpty() ? "Цель" : activeNavTitle;
        String label = arrow + " НАВИГАТОР: " + title + " • " + dist + "м (Y:" + activeNavTarget.getY() + ")";
        drawTopPill(g, font, sw, 10, label, currentTheme.bgWindow, activeNavColor, 0xFFFFFFFF);
    }

    private static void renderSmoothMinimapHud(GuiGraphics g, Minecraft mc) {
        // 64x64 сетка с шагом 2px = 128x128 чётких гладких пикселей (в 2 раза детальнее без пиксельной каши!)
        int cell = 2;
        int size = MAP_GRID * cell; // 128x128
        int x = 10;
        int y = 32;
        int cx = x + size / 2;
        int cy = y + size / 2;

        fillGlassCard(g, x - 3, y - 3, size + 6, size + 6, currentTheme.bgWindow, currentTheme.accent);

        int step = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        int half = MAP_GRID / 2;
        double yawRad = Math.toRadians(mc.player.getYRot() + 180.0);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);

        if (minimapTerrainEnabled) {
            // Сглаженная отрисовка рельефа с вращением за камерой
            for (int py = 0; py < MAP_GRID; py++) {
                double ry = py - half;
                int rowY = y + py * cell;
                int px = 0;
                while (px < MAP_GRID) {
                    double rx = px - half;
                    int sx = half + (int) Math.round(rx * cos + ry * sin);
                    int sz = half + (int) Math.round(-rx * sin + ry * cos);
                    int col = (sx >= 0 && sx < MAP_GRID && sz >= 0 && sz < MAP_GRID)
                        ? mapColors[sz * MAP_GRID + sx]
                        : 0xFF0C1219;

                    // RLE-объединение соседних одинаковых пикселей в один вызов GPU
                    int run = px + 1;
                    while (run < MAP_GRID) {
                        double rx2 = run - half;
                        int sx2 = half + (int) Math.round(rx2 * cos + ry * sin);
                        int sz2 = half + (int) Math.round(-rx2 * sin + ry * cos);
                        int col2 = (sx2 >= 0 && sx2 < MAP_GRID && sz2 >= 0 && sz2 < MAP_GRID)
                            ? mapColors[sz2 * MAP_GRID + sx2]
                            : 0xFF0C1219;
                        if (col2 != col) break;
                        run++;
                    }
                    g.fill(x + px * cell, rowY, x + run * cell, rowY + cell, col);
                    px = run;
                }
            }
            // Мягкая виньетка по краям карты
            g.fill(x, y, x + size, y + 6, 0x44060A0F);
            g.fill(x, y + size - 6, x + size, y + size, 0x44060A0F);
        } else {
            g.fill(x, y, x + size, y + size, 0xF00B1118);
        }

        // Тонкие концентрические кольца сонара (как на фото 6)
        int r1 = 26;
        int r2 = 50;
        drawRoundedBorder(g, cx - r1, cy - r1, r1 * 2, r1 * 2, 0x3322D3EE);
        drawRoundedBorder(g, cx - r2, cy - r2, r2 * 2, r2 * 2, 0x26C084FC);
        g.fill(cx, y + 3, cx + 1, y + size - 3, 0x24FFFFFF);
        g.fill(x + 3, cy, x + size - 3, cy + 1, 0x24FFFFFF);

        double scale = (double) cell / step;
        int maxOff = (size / 2) - 5;

        // Звуковой сонар: дуга в сторону ближайшего монстра
        if (soundRadarSonar && nearestHostileDist < 24.0) {
            double hDx = Math.cos(nearestHostileAngle) * 20.0;
            double hDz = Math.sin(nearestHostileAngle) * 20.0;
            double rX = hDx * cos - hDz * sin;
            double rY = hDx * sin + hDz * cos;
            double len = Math.max(1.0, Math.sqrt(rX * rX + rY * rY));
            int ax = cx + (int) ((rX / len) * (maxOff + 1));
            int ay = cy + (int) ((rY / len) * (maxOff + 1));
            g.fill(ax - 3, ay - 3, ax + 4, ay + 4, 0xD9EF4444);
        }

        // Ларчики и сундуки на карте
        if (highlightChestsWorld || highlightTrialVaults) {
            for (BlockPos cPos : nearbyVaultsAndChests) {
                double dx = (cPos.getX() + 0.5) - mc.player.getX();
                double dz = (cPos.getZ() + 0.5) - mc.player.getZ();
                double rX = (dx * cos - dz * sin) * scale;
                double rY = (dx * sin + dz * cos) * scale;
                if (Math.abs(rX) <= maxOff && Math.abs(rY) <= maxOff) {
                    int mx = cx + (int) rX;
                    int my = cy + (int) rY;
                    g.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFFF59E0B);
                }
            }
        }

        // Активный маяк Навигатора на карте
        BlockPos pin = activeNavTarget != null ? activeNavTarget : homeWaypoint;
        if (pin != null) {
            double dx = (pin.getX() + 0.5) - mc.player.getX();
            double dz = (pin.getZ() + 0.5) - mc.player.getZ();
            double rX = (dx * cos - dz * sin) * scale;
            double rY = (dx * sin + dz * cos) * scale;
            int hx = cx + (int) Mth.clamp(rX, -maxOff, maxOff);
            int hy = cy + (int) Mth.clamp(rY, -maxOff, maxOff);
            g.fill(hx - 3, hy - 3, hx + 4, hy + 4, 0xFF000000);
            g.fill(hx - 2, hy - 2, hx + 3, hy + 3, activeNavColor);
        }

        // Сущности вокруг
        for (RadarBlip b : radarBlips) {
            double dx = b.x - mc.player.getX();
            double dz = b.z - mc.player.getZ();
            double rX = (dx * cos - dz * sin) * scale;
            double rY = (dx * sin + dz * cos) * scale;
            if (Math.abs(rX) > maxOff || Math.abs(rY) > maxOff) continue;
            int bx = cx + (int) rX;
            int by = cy + (int) rY;
            if (b.kind == 0) {
                g.fill(bx - 2, by - 2, bx + 3, by + 3, 0xFF38BDF8);
                if (b.name != null) {
                    g.drawString(mc.font, trimText(mc.font, b.name, 48), bx + 4, by - 3, 0xFFE0F2FE);
                }
            } else if (b.kind == 1) {
                g.fill(bx - 1, by - 1, bx + 2, by + 2, 0xFFEF4444);
            } else if (b.kind == 2) {
                g.fill(bx - 1, by - 1, bx + 2, by + 2, 0xFF4ADE80);
            } else if (b.kind == 3) {
                g.fill(bx - 1, by - 1, bx + 2, by + 2, 0xFFC084FC);
            } else if (b.kind == 4) {
                g.fill(bx, by, bx + 2, by + 2, 0xFFFACC15);
            }
        }

        // Север (N) по краю радара
        int nx = cx + (int) (-sin * (maxOff - 2)) - 2;
        int ny = cy + (int) (-cos * (maxOff - 2)) - 3;
        g.fill(nx - 1, ny - 1, nx + 7, ny + 8, 0xCC090E14);
        g.drawString(mc.font, "N", nx, ny, 0xFF22D3EE);

        // Игрок в центре
        g.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF000000);
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        g.fill(cx, cy - 4, cx + 1, cy - 1, currentTheme.accent);

        // Нижняя плашка координат и статуса
        int iy = y + size + 5;
        int iw = size + 6;
        fillGlassCard(g, x - 3, iy, iw, 28, currentTheme.bgWindow, currentTheme.secondary);
        String cStr = (int) mc.player.getX() + " / " + (int) mc.player.getY() + " / " + (int) mc.player.getZ();
        g.drawString(mc.font, trimText(mc.font, cStr, iw - 8), x + 2, iy + 4, 0xFFF1F5F9);
        String subStr = (pin != null)
            ? ("Маяк: " + (int) Math.sqrt(mc.player.blockPosition().distSqr(pin)) + "м • " + currentForm.russianName)
            : ("✦ " + currentForm.russianName);
        g.drawString(mc.font, trimText(mc.font, subStr, iw - 8), x + 2, iy + 16, currentTheme.accent);
    }

    private static void renderElytraHorizonHud(GuiGraphics g, Font font, Player player, int sw, int sh) {
        int cx = sw / 2;
        int cy = sh / 2;
        float pitch = player.getXRot();
        int shiftY = (int) Mth.clamp(-pitch * 0.8F, -36.0F, 36.0F);
        g.fill(cx - 34, cy + shiftY, cx - 10, cy + shiftY + 1, 0xAA22D3EE);
        g.fill(cx + 10, cy + shiftY, cx + 34, cy + shiftY + 1, 0xAA22D3EE);
        String pTxt = String.format("%.0f°", -pitch);
        g.drawString(font, pTxt, cx + 38, cy + shiftY - 3, 0xFF22D3EE);
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

    private static void renderLitematicaRightHud(GuiGraphics g, Minecraft mc, int sw) {
        Player p = mc.player;
        if (p == null) return;
        ItemStack hand = p.getMainHandItem();
        if (hand.isEmpty() && !ruler3DVolume) return;

        int count = 0;
        if (!hand.isEmpty()) {
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack st = p.getInventory().getItem(i);
                if (!st.isEmpty() && st.getItem() == hand.getItem()) {
                    count += st.getCount();
                }
            }
        }
        int w = 148;
        int h = 32;
        int x = litematicaHudRight ? (sw - w - 10) : 148;
        int y = 32;
        fillGlassCard(g, x, y, w, h, currentTheme.bgWindow, currentTheme.borderMuted);

        String line1 = hand.isEmpty() ? "Litematica HUD активен" : (hand.getHoverName().getString() + " x" + count);
        g.drawString(mc.font, trimText(mc.font, line1, w - 12), x + 6, y + 5, 0xFFF8FAFC);

        String line2 = "Ларчиков рядом: " + nearbyVaultsAndChests.size() + " • Y:" + (int) p.getY();
        if (ruler3DVolume && mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            double dist = p.position().distanceTo(bhr.getLocation());
            line2 = String.format("Дистанция: %.1fм • Y:%d", dist, bhr.getBlockPos().getY());
        }
        g.drawString(mc.font, trimText(mc.font, line2, w - 12), x + 6, y + 18, currentTheme.accent);
    }

    private static void renderCombatPvpHud(GuiGraphics g, Minecraft mc, int sw, int sh) {
        Player p = mc.player;
        if (p == null) return;
        int totems = countPlayerTotems(p);
        int pearls = 0;
        int gapples = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(Items.ENDER_PEARL)) pearls += st.getCount();
            else if (st.is(Items.GOLDEN_APPLE) || st.is(Items.ENCHANTED_GOLDEN_APPLE)) gapples += st.getCount();
        }

        int w = 146;
        int h = 48;
        int x = sw - w - 10;
        int y = sh - h - 10;
        fillGlassCard(g, x, y, w, h, currentTheme.bgWindow, currentTheme.borderMuted);

        String hdr = "Тотем:" + totems + "  Перл:" + pearls + "  Ябл:" + gapples;
        g.drawString(mc.font, trimText(mc.font, hdr, w - 10), x + 6, y + 5, 0xFFFDE047);

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] labels = {"Ш", "Н", "П", "Б"};
        int bx = x + 6;
        int by = y + 19;
        for (int i = 0; i < slots.length; i++) {
            ItemStack armor = p.getItemBySlot(slots[i]);
            int pct = 0;
            if (!armor.isEmpty() && armor.isDamageableItem() && armor.getMaxDamage() > 0) {
                pct = (int) Math.round(((armor.getMaxDamage() - armor.getDamageValue()) * 100.0) / armor.getMaxDamage());
            } else if (!armor.isEmpty()) {
                pct = 100;
            }
            int col = pct > 55 ? 0xFF4ADE80 : (pct > 25 ? 0xFFFACC15 : (pct > 0 ? 0xFFEF4444 : 0xFF334155));
            g.drawString(mc.font, labels[i] + ":" + pct + "%", bx + i * 34, by, 0xFFCBD5E1);
            g.fill(bx + i * 34, by + 11, bx + i * 34 + 30, by + 15, 0xFF1E293B);
            if (pct > 0) {
                g.fill(bx + i * 34, by + 11, bx + i * 34 + Math.max(2, (30 * pct) / 100), by + 15, col);
            }
        }
    }

    private static void renderSpotifyBottomHud(GuiGraphics g, Minecraft mc, int sh) {
        int w = 186;
        int h = 34;
        int x = 10;
        int y = sh - h - 10;
        fillGlassCard(g, x, y, w, h, 0xEE0B1911, spotifyPlaying ? 0xFF1DB954 : 0xFF475569);

        // Обложка + эквалайзер
        g.fillGradient(x + 5, y + 5, x + 29, y + 29, 0xFF1E3A8A, 0xFF065F46);
        long t = System.currentTimeMillis() / 120L;
        for (int i = 0; i < 4; i++) {
            int bh = spotifyPlaying ? (4 + (int) (Math.abs(Math.sin(t * 0.8 + i * 1.3)) * 14)) : 3;
            g.fill(x + 8 + i * 5, y + 27 - bh, x + 11 + i * 5, y + 27, 0xFF4ADE80);
        }

        String title = getActiveTrackTitle();
        String artist = getActiveTrackArtist();
        g.drawString(mc.font, trimText(mc.font, title, w - 40), x + 34, y + 5, 0xFFFFFFFF);
        g.drawString(mc.font, trimText(mc.font, artist, w - 40), x + 34, y + 15, 0xFF86EFAC);

        // Полоска прогресса трека
        PlaylistTrack pt = getCurrentPlaylistTrack();
        float prog = Mth.clamp((float) spotifyTrackElapsedMs / Math.max(1000L, pt.durationSec * 1000L), 0.0F, 1.0F);
        int barX = x + 34;
        int barW = w - 40;
        g.fill(barX, y + 26, barX + barW, y + 28, 0xFF1F2937);
        g.fill(barX, y + 26, barX + (int) (barW * prog), y + 28, 0xFF1DB954);
    }

    // =========================================================================
    // 9. ГРАФИЧЕСКИЕ ПРИМИТИВЫ И ИКОНКИ КОНЦЕПТ-МЕНЮ
    // =========================================================================

    private static void fillGlassCard(GuiGraphics g, int x, int y, int w, int h, int fillCol, int borderCol) {
        fillRoundedRect(g, x, y, w, h, fillCol);
        drawRoundedBorder(g, x, y, w, h, borderCol);
    }

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

    private static int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int a2 = (c2 >>> 24) & 0xFF, r2 = (c2 >>> 16) & 0xFF, g2 = (c2 >>> 8) & 0xFF, b2 = c2 & 0xFF;
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
        if (text == null || maxWidth <= 8) return "";
        if (font.width(text) <= maxWidth) return text;
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

    private static void drawConceptTabIcon(GuiGraphics g, MenuTab tab, int x, int y, int col) {
        switch (tab) {
            case COMPANION -> {
                // Голова кибер-робота как на фото 1
                g.fill(x + 4, y, x + 6, y + 2, col);
                drawRoundedBorder(g, x + 1, y + 2, 8, 7, col);
                g.fill(x + 3, y + 4, x + 4, y + 6, col);
                g.fill(x + 6, y + 4, x + 7, y + 6, col);
            }
            case AUTO_BUILD -> {
                // Угольник и кисть как на фото 5
                g.fill(x + 1, y + 1, x + 3, y + 9, col);
                g.fill(x + 1, y + 7, x + 9, y + 9, col);
                g.fill(x + 5, y + 2, x + 8, y + 5, 0xFFFBBF24);
            }
            case LITEMATICA_HUD -> {
                // Бейдж HUD как на фото 3
                drawRoundedBorder(g, x, y + 1, 10, 8, col);
                g.fill(x + 2, y + 3, x + 8, y + 4, col);
                g.fill(x + 2, y + 6, x + 6, y + 7, col);
            }
            case COMBAT_PVP -> {
                // Скрещённые мечи как на фото 7
                g.fill(x + 1, y + 1, x + 3, y + 3, col);
                g.fill(x + 3, y + 3, x + 7, y + 7, col);
                g.fill(x + 7, y + 7, x + 9, y + 9, col);
                g.fill(x + 7, y + 1, x + 9, y + 3, col);
                g.fill(x + 1, y + 7, x + 3, y + 9, col);
            }
            case VISUAL_RADAR -> {
                // Круглый сонар-радар как на фото 6
                drawRoundedBorder(g, x + 1, y + 1, 8, 8, col);
                g.fill(x + 4, y + 4, x + 6, y + 6, col);
                g.fill(x + 6, y + 2, x + 8, y + 4, 0xFF4ADE80);
            }
            case SPOTIFY_PLAYER -> {
                // Зелёный логотип Spotify как на фото 8
                fillRoundedRect(g, x + 1, y + 1, 8, 8, 0xFF1DB954);
                g.fill(x + 2, y + 3, x + 8, y + 4, 0xFF06150C);
                g.fill(x + 3, y + 5, x + 7, y + 6, 0xFF06150C);
                g.fill(x + 3, y + 7, x + 6, y + 8, 0xFF06150C);
            }
            case CHESTS_VILLAGERS -> {
                // Золотой ключ как на фото 9
                drawRoundedBorder(g, x + 1, y + 2, 4, 4, 0xFFFBBF24);
                g.fill(x + 5, y + 3, x + 9, y + 5, 0xFFFBBF24);
                g.fill(x + 7, y + 5, x + 8, y + 7, 0xFFFBBF24);
            }
            case STYLE_PROFILER -> {
                // Силуэт игрока как на фото 4
                g.fill(x + 3, y + 1, x + 7, y + 4, 0xFFFDBA74);
                g.fill(x + 2, y + 5, x + 8, y + 9, col);
            }
        }
    }

    // =========================================================================
    // 10. ГЛАВНОЕ ОКНО КОНЦЕПТА (3 КОЛОНКИ: ЛЕВОЕ МЕНЮ | ЦЕНТР-СЦЕНА | ПРАВОЕ МЕНЮ)
    // =========================================================================

    public static class AssistantVadimkaScreen extends Screen {
        private MenuTab activeTab;
        private int rightPage = 0;
        private int rightTotalPages = 1;

        // Подрежимы сцен
        private boolean showChatOverlay = false;
        private int chestsCenterTab = 0; // 0=3D Ларчик и Библиотекарь, 1=Список найденного лута, 2=База торгов
        private int spotifyCenterTab = 0; // 0=Spotify Connect Плеер, 1=AFK Мини-игры (3)

        private EditBox activeEditBox = null;
        private String chatInputDraft = "";
        private String searchFilterDraft = "";

        private static class NavItemVis {
            final MenuTab tab;
            final int x, y, w, h;

            NavItemVis(MenuTab tab, int x, int y, int w, int h) {
                this.tab = tab;
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
            }
        }

        private static class RightControlVis {
            final int x, y, w, h;
            final int ctrlX, ctrlY, ctrlW, ctrlH;
            final String title;
            final String subtitle;
            final int kind; // 0=toggle, 1=slider, 2=cycle
            final boolean toggleVal;
            final float sliderPct;
            final String valueText;

            RightControlVis(int x, int y, int w, int h,
                            int ctrlX, int ctrlY, int ctrlW, int ctrlH,
                            String title, String subtitle,
                            int kind, boolean toggleVal, float sliderPct, String valueText) {
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
                this.ctrlX = ctrlX;
                this.ctrlY = ctrlY;
                this.ctrlW = ctrlW;
                this.ctrlH = ctrlH;
                this.title = title;
                this.subtitle = subtitle;
                this.kind = kind;
                this.toggleVal = toggleVal;
                this.sliderPct = sliderPct;
                this.valueText = valueText;
            }
        }

        private static class ActionBtnVis {
            final int x, y, w, h;
            final String label;
            final int style; // 0=default, 1=accent, 2=danger, 3=spotify

            ActionBtnVis(int x, int y, int w, int h, String label, int style) {
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
                this.label = label;
                this.style = style;
            }
        }

        private static class ControlSpec {
            final String title;
            final String subtitle;
            final int kind; // 0=toggle, 1=slider, 2=cycle
            final boolean toggleState;
            final float sliderRatio;
            final String valueLabel;
            final Runnable onClick;
            final java.util.function.DoubleConsumer onSlide;

            ControlSpec(String title, String subtitle, int kind, boolean toggleState,
                        float sliderRatio, String valueLabel,
                        Runnable onClick, java.util.function.DoubleConsumer onSlide) {
                this.title = title;
                this.subtitle = subtitle;
                this.kind = kind;
                this.toggleState = toggleState;
                this.sliderRatio = sliderRatio;
                this.valueLabel = valueLabel;
                this.onClick = onClick;
                this.onSlide = onSlide;
            }
        }

        private final List<NavItemVis> navItems = new ArrayList<>();
        private final List<RightControlVis> rightControls = new ArrayList<>();
        private final List<ControlSpec> currentSpecs = new ArrayList<>();
        private final List<ActionBtnVis> actionButtons = new ArrayList<>();
        private int draggingSpecIndex = -1;

        // 3 AFK мини-игры
        public boolean afkGameRunning = false;
        private int afkScore = 0;
        private int afkBestScore = 0;
        private int afkTick = 0;
        private int runnerLane = 1;
        private final List<int[]> runnerObs = new ArrayList<>();
        private double sonicY = 0.0;
        private double sonicVel = 0.0;
        private final List<int[]> sonicObs = new ArrayList<>();
        private int doomX = 50;
        private final List<int[]> doomShots = new ArrayList<>();
        private final List<int[]> doomMobs = new ArrayList<>();
        private final Random gameRng = new Random();

        // Геометрия 3-колоночного окна концепта
        private int winX, winY, winW, winH;
        private int leftColX, leftColW;
        private int centerColX, centerColW;
        private int rightColX, rightColW;
        private int colTopY, colH;

        public AssistantVadimkaScreen(MenuTab activeTab) {
            super(Component.literal("Assistant Vadimka"));
            this.activeTab = activeTab;
        }

        @Override
        protected void init() {
            this.navItems.clear();
            this.rightControls.clear();
            this.currentSpecs.clear();
            this.actionButtons.clear();
            this.activeEditBox = null;

            this.winW = Math.min(this.width - 12, 668);
            this.winH = Math.min(this.height - 12, 366);
            this.winX = (this.width - this.winW) / 2;
            this.winY = (this.height - this.winH) / 2;

            this.colTopY = this.winY + 34;
            this.colH = this.winH - 42;

            this.leftColW = 126;
            this.rightColW = 196;
            this.leftColX = this.winX + 6;
            this.centerColX = this.leftColX + this.leftColW + 6;
            this.centerColW = this.winW - this.leftColW - this.rightColW - 24;
            this.rightColX = this.centerColX + this.centerColW + 6;

            // 1. Левая навигационная панель (8 разделов концепта + смена темы внизу)
            MenuTab[] tabs = MenuTab.values();
            int itemH = 26;
            int startY = this.colTopY + 6;
            for (int i = 0; i < tabs.length; i++) {
                MenuTab t = tabs[i];
                int ix = this.leftColX + 5;
                int iy = startY + i * (itemH + 4);
                int iw = this.leftColW - 10;
                this.navItems.add(new NavItemVis(t, ix, iy, iw, itemH));
                addHitbox(ix, iy, iw, itemH, () -> {
                    this.activeTab = t;
                    this.rightPage = 0;
                    this.showChatOverlay = false;
                    if (t != MenuTab.SPOTIFY_PLAYER) {
                        this.afkGameRunning = false;
                    }
                    this.rebuildWidgets();
                });
            }

            // Кнопка смены темы внизу левой панели
            addStyledBtn(this.leftColX + 5, this.colTopY + this.colH - 26, this.leftColW - 10, 20,
                "Тема: " + shortThemeLabel(currentTheme), 1, () -> {
                    MenuTheme[] th = MenuTheme.values();
                    currentTheme = th[(currentTheme.ordinal() + 1) % th.length];
                    this.rebuildWidgets();
                });

            // Крестик закрытия в правом верхнем углу шапки
            addStyledBtn(this.winX + this.winW - 24, this.winY + 6, 18, 18, "✕", 2, this::onClose);

            // 2. Интерактивные кнопки внутри центральной сцены (в зависимости от вкладки!)
            initCenterStageButtons();

            // 3. Правая колонка настроек и слайдеров
            List<ControlSpec> specs = buildRightColumnSpecs(this.activeTab);
            int perPage = 6;
            this.rightTotalPages = Math.max(1, (specs.size() + perPage - 1) / perPage);
            this.rightPage = Mth.clamp(this.rightPage, 0, this.rightTotalPages - 1);

            if (this.rightTotalPages > 1) {
                int py = this.colTopY + 6;
                addStyledBtn(this.rightColX + this.rightColW - 56, py, 18, 15, "◀", 0, () -> {
                    this.rightPage = (this.rightPage - 1 + this.rightTotalPages) % this.rightTotalPages;
                    this.rebuildWidgets();
                });
                addStyledBtn(this.rightColX + this.rightColW - 22, py, 18, 15, "▶", 0, () -> {
                    this.rightPage = (this.rightPage + 1) % this.rightTotalPages;
                    this.rebuildWidgets();
                });
            }

            int sIdx = this.rightPage * perPage;
            int eIdx = Math.min(specs.size(), sIdx + perPage);
            int ry = this.colTopY + 26;
            for (int i = sIdx; i < eIdx; i++) {
                ControlSpec sp = specs.get(i);
                this.currentSpecs.add(sp);
                int rx = this.rightColX + 6;
                int rw = this.rightColW - 12;
                int rh = 44;

                int cw = sp.kind == 0 ? 44 : (sp.kind == 1 ? 92 : 84);
                int ch = 16;
                int cx = rx + rw - cw - 6;
                int cy = ry + (sp.kind == 1 ? 22 : 14);

                this.rightControls.add(new RightControlVis(
                    rx, ry, rw, rh, cx, cy, cw, ch,
                    sp.title, sp.subtitle, sp.kind, sp.toggleState, sp.sliderRatio, sp.valueLabel
                ));

                if (sp.kind != 1 && sp.onClick != null) {
                    addHitbox(cx, cy, cw, ch, () -> {
                        sp.onClick.run();
                        this.rebuildWidgets();
                    });
                }
                ry += rh + 4;
            }
        }

        private void initCenterStageButtons() {
            int sx = this.centerColX + 8;
            int sw = this.centerColW - 16;
            int bottomY = this.colTopY + this.colH - 26;

            switch (this.activeTab) {
                case COMPANION -> {
                    // Переключатель "3D Сцена" / "💬 Чат с Вадимкой" вверху центра
                    int topBtnW = 86;
                    addStyledBtn(sx + sw - topBtnW, this.colTopY + 6, topBtnW, 16,
                        showChatOverlay ? "✦ 3D Облики" : "💬 Чат Вадимки", 1, () -> {
                            showChatOverlay = !showChatOverlay;
                            this.rebuildWidgets();
                        });

                    if (showChatOverlay) {
                        int sendW = 64;
                        int boxW = sw - sendW - 6;
                        EditBox box = new EditBox(this.font, sx, bottomY, boxW, 18, Component.literal("Чат"));
                        box.setMaxLength(110);
                        box.setValue(chatInputDraft);
                        box.setHint(Component.literal("Напиши Вадимке (Enter)..."));
                        box.setTextColor(0xFFF8FAFC);
                        box.setResponder(s -> chatInputDraft = s);
                        this.addRenderableWidget(box);
                        this.activeEditBox = box;

                        addStyledBtn(sx + boxW + 6, bottomY, sendW, 18, "Отправить", 1, () -> {
                            if (!chatInputDraft.trim().isEmpty()) {
                                askVadimkaFromGui(chatInputDraft);
                                chatInputDraft = "";
                                this.rebuildWidgets();
                            }
                        });
                    } else if (!positionEditor3D) {
                        // Кликабельные карточки 5-6 обликов прямо в центре (как на фото 1!)
                        VadimkaForm[] forms = VadimkaForm.values();
                        int cw = (sw - 10) / 3;
                        int ch = 18;
                        for (int i = 0; i < forms.length; i++) {
                            VadimkaForm f = forms[i];
                            int col = i % 3;
                            int row = i / 3;
                            int bx = sx + col * (cw + 5);
                            int by = bottomY - 24 + row * (ch + 4);
                            addStyledBtn(bx, by, cw, ch, f.codeName, currentForm == f ? 1 : 0, () -> {
                                currentForm = f;
                                this.rebuildWidgets();
                            });
                        }
                    } else {
                        // 4 кнопки привязки позиции (Правое плечо / Левое плечо / Над головой / За спиной — как на фото 2!)
                        CompanionAnchor[] anchors = CompanionAnchor.values();
                        int hw = (sw - 6) / 2;
                        for (int i = 0; i < anchors.length; i++) {
                            CompanionAnchor a = anchors[i];
                            int bx = sx + (i % 2) * (hw + 6);
                            int by = bottomY - 22 + (i / 2) * 21;
                            addStyledBtn(bx, by, hw, 18, a.label, currentAnchor == a ? 1 : 0, () -> {
                                currentAnchor = a;
                                offsetX = a.defaultX;
                                offsetY = a.defaultY;
                                offsetZ = a.defaultZ;
                                this.rebuildWidgets();
                            });
                        }
                    }
                }
                case AUTO_BUILD -> {
                    // 4 кнопки режимов стройки внизу центра (как на фото 5!)
                    BuildMode[] modes = BuildMode.values();
                    int hw = (sw - 6) / 2;
                    for (int i = 0; i < modes.length; i++) {
                        BuildMode bm = modes[i];
                        int bx = sx + (i % 2) * (hw + 6);
                        int by = bottomY - 22 + (i / 2) * 21;
                        addStyledBtn(bx, by, hw, 18, bm.title, currentBuildMode == bm ? 1 : 0, () -> {
                            currentBuildMode = bm;
                            this.rebuildWidgets();
                        });
                    }
                }
                case SPOTIFY_PLAYER -> {
                    int hw = (sw - 6) / 2;
                    addStyledBtn(sx, this.colTopY + 6, hw, 16, "♫ Spotify Connect", spotifyCenterTab == 0 ? 3 : 0, () -> {
                        spotifyCenterTab = 0;
                        afkGameRunning = false;
                        this.rebuildWidgets();
                    });
                    addStyledBtn(sx + hw + 6, this.colTopY + 6, hw, 16, "🎮 AFK Мини-игры", spotifyCenterTab == 1 ? 1 : 0, () -> {
                        spotifyCenterTab = 1;
                        this.rebuildWidgets();
                    });

                    if (spotifyCenterTab == 0) {
                        // Кнопки управления плеером прямо под обложкой трека (как на фото 8!)
                        int bw = (sw - 16) / 5;
                        int py = this.colTopY + 186;
                        addStyledBtn(sx, py, bw, 20, "🔀", spotifyShuffle ? 3 : 0, () -> {
                            spotifyShuffle = !spotifyShuffle;
                            this.rebuildWidgets();
                        });
                        addStyledBtn(sx + bw + 4, py, bw, 20, "⏮", 0, () -> {
                            spotifyPrevTrack();
                            this.rebuildWidgets();
                        });
                        addStyledBtn(sx + (bw + 4) * 2, py, bw, 20, spotifyPlaying ? "⏸" : "▶", 3, () -> {
                            spotifyTogglePlay();
                            this.rebuildWidgets();
                        });
                        addStyledBtn(sx + (bw + 4) * 3, py, bw, 20, "⏭", 0, () -> {
                            spotifyNextTrack();
                            this.rebuildWidgets();
                        });
                        addStyledBtn(sx + (bw + 4) * 4, py, bw, 20, "🔁", spotifyRepeat ? 3 : 0, () -> {
                            spotifyRepeat = !spotifyRepeat;
                            this.rebuildWidgets();
                        });
                    } else {
                        int bw = (sw - 6) / 2;
                        addStyledBtn(sx, bottomY, bw, 18, "Игра: " + selectedAfkGame.title, 0, () -> {
                            AfkGameType[] gs = AfkGameType.values();
                            selectedAfkGame = gs[(selectedAfkGame.ordinal() + 1) % gs.length];
                            resetAfkGame();
                            this.rebuildWidgets();
                        });
                        addStyledBtn(sx + bw + 6, bottomY, bw, 18, afkGameRunning ? "■ Стоп" : "▶ Играть", 1, () -> {
                            afkGameRunning = !afkGameRunning;
                            if (afkGameRunning) resetAfkGame();
                            this.rebuildWidgets();
                        });
                    }
                }
                case CHESTS_VILLAGERS -> {
                    // 3 кнопки внизу центральной сцены как на фото 9: "Сбросить память", "База жителей (N)", "Начать поиск"
                    int bw = (sw - 8) / 3;
                    addStyledBtn(sx, bottomY, bw, 18, "Сбросить память", 2, () -> {
                        rememberedChests.clear();
                        rememberedVillagers.clear();
                        clearNavigationTarget();
                        showToast("Память ларчиков и жителей очищена", 60);
                        this.rebuildWidgets();
                    });
                    addStyledBtn(sx + bw + 4, bottomY, bw, 18,
                        chestsCenterTab == 2 ? "3D Сцена" : ("Жители (" + rememberedVillagers.size() + ")"),
                        chestsCenterTab == 2 ? 1 : 0, () -> {
                            chestsCenterTab = (chestsCenterTab == 2) ? 0 : 2;
                            this.rebuildWidgets();
                        });
                    addStyledBtn(sx + (bw + 4) * 2, bottomY, bw, 18,
                        chestsCenterTab == 1 ? "3D Сцена" : ("Ларчики (" + rememberedChests.size() + ")"),
                        1, () -> {
                            if (chestsCenterTab == 1) {
                                chestsCenterTab = 0;
                            } else {
                                chestsCenterTab = 1;
                                if (!nearbyVaultsAndChests.isEmpty()) {
                                    BlockPos nearest = nearbyVaultsAndChests.get(0);
                                    setNavigationTarget(nearest, "Ближайший ларчик", 0xFF22D3EE);
                                }
                            }
                            this.rebuildWidgets();
                        });
                }
                case STYLE_PROFILER -> {
                    addStyledBtn(sx, bottomY, sw, 18, "Сбросить поведенческий профиль", 2, () -> {
                        sessionWalkedBlocks = 0.0;
                        profilerTrainingPct = 50;
                        showToast("Профиль стиля сброшен", 50);
                        this.rebuildWidgets();
                    });
                }
                default -> {
                }
            }
        }

        private List<ControlSpec> buildRightColumnSpecs(MenuTab tab) {
            List<ControlSpec> list = new ArrayList<>();
            switch (tab) {
                case COMPANION -> {
                    list.add(toggleSpec("Включить спутник", "3D-модель в мире и студии", companionEnabled, () -> companionEnabled = !companionEnabled));
                    list.add(cycleSpec("Облик Вадимки", "6 кастомных 3D-моделей", currentForm.russianName, () -> {
                        VadimkaForm[] fs = VadimkaForm.values();
                        currentForm = fs[(currentForm.ordinal() + 1) % fs.length];
                    }));
                    list.add(toggleSpec("3D-редактор позиции", "Переключает вид сцены в центре", positionEditor3D, () -> positionEditor3D = !positionEditor3D));
                    list.add(sliderSpec("X оффсет (Бок)", String.format("%.2f бл.", offsetX), (float) ((offsetX + 2.5) / 5.0), r -> {
                        offsetX = -2.5 + r * 5.0;
                    }));
                    list.add(sliderSpec("Y оффсет (Высота)", String.format("%.2f бл.", offsetY), (float) (offsetY / 3.0), r -> {
                        offsetY = r * 3.0;
                    }));
                    list.add(sliderSpec("Z оффсет (Дист.)", String.format("%.2f бл.", offsetZ), (float) ((offsetZ + 2.5) / 5.0), r -> {
                        offsetZ = -2.5 + r * 5.0;
                    }));
                    list.add(sliderSpec("Громкость голоса", voiceVolumePercent + "%", voiceVolumePercent / 100.0F, r -> {
                        voiceVolumePercent = (int) Math.round(r * 100.0);
                    }));
                    list.add(toggleSpec("Команда миньонов (4)", "Умные помощники вокруг", minionsEnabled, () -> minionsEnabled = !minionsEnabled));
                    list.add(toggleSpec("Умный AI миньонов", "Летят к луту, мобам и блокам", minionsSmartAi, () -> minionsSmartAi = !minionsSmartAi));
                    list.add(toggleSpec("Синхронные эмоции", "Реакции на события и музыку", syncEmotions, () -> syncEmotions = !syncEmotions));
                }
                case AUTO_BUILD -> {
                    list.add(toggleSpec("Авто-постройка", "Помощь по схеме Litematica", autoBuildLitematica, () -> autoBuildLitematica = !autoBuildLitematica));
                    list.add(toggleSpec("Плавная камера", "Кривые Безье при наведении", smoothBezierCamera, () -> smoothBezierCamera = !smoothBezierCamera));
                    list.add(toggleSpec("Авто-пополнение", "Подмена стаков в хотбаре", hotbarAutoRefill, () -> hotbarAutoRefill = !hotbarAutoRefill));
                    list.add(toggleSpec("Авто-зачарование", "Помощь на наковальне", autoAnvilEnchant, () -> autoAnvilEnchant = !autoAnvilEnchant));
                    list.add(sliderSpec("Мин. задержка", minBuildDelayMs + " мс", (minBuildDelayMs - 20) / 180.0F, r -> {
                        minBuildDelayMs = 20 + (int) Math.round(r * 180.0);
                    }));
                    list.add(sliderSpec("Макс. задержка", maxBuildDelayMs + " мс", (maxBuildDelayMs - 50) / 250.0F, r -> {
                        maxBuildDelayMs = 50 + (int) Math.round(r * 250.0);
                    }));
                    list.add(sliderSpec("Дистанция", String.format("%.1f бл.", buildReachBlocks), (float) ((buildReachBlocks - 2.0) / 4.0), r -> {
                        buildReachBlocks = 2.0 + r * 4.0;
                    }));
                }
                case LITEMATICA_HUD -> {
                    list.add(toggleSpec("Включить Litematica HUD", "Трекер ресурсов в углу", litematicaHudEnabled, () -> litematicaHudEnabled = !litematicaHudEnabled));
                    list.add(toggleSpec("Трекер ресурсов справа", "Позиция виджета на экране", litematicaHudRight, () -> litematicaHudRight = !litematicaHudRight));
                    list.add(toggleSpec("Сканировать шалкеры", "Учёт блоков внутри сумок", shulkerScanner, () -> shulkerScanner = !shulkerScanner));
                    list.add(toggleSpec("Скрывать собранные", "Оставлять только дефицит", hideCompletedMaterials, () -> hideCompletedMaterials = !hideCompletedMaterials));
                    list.add(toggleSpec("3D-линейка объёма", "Дистанция до блока под прицелом", ruler3DVolume, () -> ruler3DVolume = !ruler3DVolume));
                    list.add(cycleSpec("Строительные палитры", "Набор блоков для превью", BUILD_PALETTES.get(buildPaletteIndex), () -> {
                        buildPaletteIndex = (buildPaletteIndex + 1) % BUILD_PALETTES.size();
                    }));
                }
                case COMBAT_PVP -> {
                    list.add(toggleSpec("Боевой PvP HUD", "Броня, тотемы, перлы и яблоки", combatHudEnabled, () -> combatHudEnabled = !combatHudEnabled));
                    list.add(toggleSpec("3D-траектории снарядов", "Дуга полёта лука и жемчуга", trajectories3D, () -> trajectories3D = !trajectories3D));
                    list.add(toggleSpec("Сканер брони и тотемов", "Проценты прочности Ш/Н/П/Б", armorAndTotemScanner, () -> armorAndTotemScanner = !armorAndTotemScanner));
                    list.add(toggleSpec("Таймеры щита и пёрлов", "Контроль перезарядки", shieldAndPearlTimers, () -> shieldAndPearlTimers = !shieldAndPearlTimers));
                    list.add(toggleSpec("Счётчик расходников", "Подсчёт тотемов и золотых яблок", consumableCounter, () -> consumableCounter = !consumableCounter));
                    list.add(toggleSpec("Индикатор удара со спины", "Тревога при враге сзади", backstabIndicator, () -> backstabIndicator = !backstabIndicator));
                    list.add(toggleSpec("Реплей / маяк смерти", "Навигатор на место гибели", deathReplayMarker, () -> deathReplayMarker = !deathReplayMarker));
                }
                case VISUAL_RADAR -> {
                    list.add(toggleSpec("Включить визуализации", "Общий мастер-рубильник HUD", visualsEnabled, () -> visualsEnabled = !visualsEnabled));
                    list.add(toggleSpec("Мини-карта с рельефом", "Гладкая 64x64 RLE карта", minimapRadarEnabled, () -> minimapRadarEnabled = !minimapRadarEnabled));
                    list.add(toggleSpec("Звуковой радар (Сонар)", "Красная дуга в сторону врага", soundRadarSonar, () -> soundRadarSonar = !soundRadarSonar));
                    list.add(cycleSpec("Масштаб карты-радара", "Радиус обзора местности",
                        radarZoomLevel == 0 ? "Близко (1x)" : (radarZoomLevel == 1 ? "Средне (2x)" : "Далеко (3x)"), () -> {
                            radarZoomLevel = (radarZoomLevel + 1) % 3;
                            mapNeedsReset = true;
                        }));
                    list.add(toggleSpec("Ассистент элитр + Горизонт", "Угол тангажа в полёте", elytraPitchHorizon, () -> elytraPitchHorizon = !elytraPitchHorizon));
                    list.add(toggleSpec("Нить Ариадны", "След из частиц обратно", ariadneThread, () -> ariadneThread = !ariadneThread));
                    list.add(cycleSpec("Навигатор: Точка Дома", homeWaypoint == null ? "Дом ещё не задан" : ("Дом: " + homeWaypoint.getX() + ", " + homeWaypoint.getZ()),
                        homeWaypoint == null ? "Задать Дом" : "Вести к Дому", () -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player != null) {
                                if (homeWaypoint == null) {
                                    homeWaypoint = mc.player.blockPosition();
                                }
                                setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
                                showToast("Навигатор проложил маршрут к Дому!", 70);
                            }
                        }));
                    list.add(cycleSpec("Отключить навигатор", activeNavTarget == null ? "Маяк не активен" : ("Цель: " + activeNavTitle), "Сбросить", () -> {
                        clearNavigationTarget();
                        showToast("Маяк навигатора выключен", 50);
                    }));
                }
                case SPOTIFY_PLAYER -> {
                    list.add(toggleSpec("Мини-плеер на HUD", "Виджет с обложкой и прогрессом", spotifyHudEnabled, () -> spotifyHudEnabled = !spotifyHudEnabled));
                    list.add(toggleSpec("Танцы Вадимки в такт BPM", "Анимация спутника под ритм", vadimkaDanceToBpm, () -> vadimkaDanceToBpm = !vadimkaDanceToBpm));
                    list.add(toggleSpec("Приглушать при разговоре", "Авто-пауза при голосовом чате", spotifyDuckOnSpeech, () -> spotifyDuckOnSpeech = !spotifyDuckOnSpeech));
                    list.add(cycleSpec("Выбор плейлиста", "Переключить подборку треков", PLAYLIST_NAMES.get(spotifyPlaylistIndex), () -> {
                        spotifyPlaylistIndex = (spotifyPlaylistIndex + 1) % PLAYLIST_NAMES.size();
                        spotifyTrackIndex = 0;
                        spotifyTrackElapsedMs = 0L;
                        spotifyPlaying = true;
                    }));
                    list.add(sliderSpec("Громкость плеера", spotifyVolumePercent + "%", spotifyVolumePercent / 100.0F, r -> {
                        spotifyVolumePercent = (int) Math.round(r * 100.0);
                    }));
                    list.add(toggleSpec("Экстренный выход из AFK", "Закроет мини-игру при уроне", afkPanicExit, () -> afkPanicExit = !afkPanicExit));
                }
                case CHESTS_VILLAGERS -> {
                    list.add(toggleSpec("Подсвечивать ларчики", "Неоновые частицы над сундуками", highlightTrialVaults, () -> highlightTrialVaults = !highlightTrialVaults));
                    list.add(cycleSpec("Вести к ближ. ларчику", "Найдено рядом: " + nearbyVaultsAndChests.size(),
                        nearbyVaultsAndChests.isEmpty() ? "Нет рядом" : "Навигатор →", () -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player != null && !nearbyVaultsAndChests.isEmpty()) {
                                BlockPos pPos = mc.player.blockPosition();
                                BlockPos best = nearbyVaultsAndChests.stream()
                                    .min(Comparator.comparingDouble(b -> b.distSqr(pPos)))
                                    .orElse(nearbyVaultsAndChests.get(0));
                                setNavigationTarget(best, "Ларчик", 0xFFF59E0B);
                                showToast("Навигатор указывает на ближайший ларчик!", 70);
                            } else {
                                showToast("Рядом пока не найдено сундуков/ларчиков", 60);
                            }
                        }));
                    list.add(cycleSpec("Цели лута Дворца", "Приоритет поиска в ларчиках", VAULT_LOOT_GOALS.get(vaultLootGoalIndex), () -> {
                        vaultLootGoalIndex = (vaultLootGoalIndex + 1) % VAULT_LOOT_GOALS.size();
                    }));
                    list.add(toggleSpec("Авто-перебор кафедры", "Помощник библиотекаря", autoCycleLectern, () -> autoCycleLectern = !autoCycleLectern));
                    list.add(toggleSpec("Предупреждать о топоре", "Сигнал при низкой прочности", warnAxeDurability, () -> warnAxeDurability = !warnAxeDurability));
                    list.add(cycleSpec("Нужные чары", "Фильтр книг библиотекаря", TARGET_ENCHANTS.get(targetEnchantIndex), () -> {
                        targetEnchantIndex = (targetEnchantIndex + 1) % TARGET_ENCHANTS.size();
                    }));
                    list.add(sliderSpec("Макс. цена (изумруды)", maxEmeraldPrice + " изумр.", (maxEmeraldPrice - 5) / 59.0F, r -> {
                        maxEmeraldPrice = 5 + (int) Math.round(r * 59.0);
                    }));
                }
                case STYLE_PROFILER -> {
                    list.add(cycleSpec("Тема интерфейса", "5 цветовых схем концепта", currentTheme.title, () -> {
                        MenuTheme[] th = MenuTheme.values();
                        currentTheme = th[(currentTheme.ordinal() + 1) % th.length];
                    }));
                    list.add(cycleSpec("3D-крылья персонажа", "Видны в 3D-сцене и в F5", WINGS_OPTIONS.get(wingsIndex), () -> {
                        wingsIndex = (wingsIndex + 1) % WINGS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Рога / Нимб", "Аксессуар над головой", HORNS_OPTIONS.get(hornsIndex), () -> {
                        hornsIndex = (hornsIndex + 1) % HORNS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Шляпка / Шлем", "Головной убор", HATS_OPTIONS.get(hatsIndex), () -> {
                        hatsIndex = (hatsIndex + 1) % HATS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Кастом-прицел", "Неоновый прицел в центре", CROSSHAIR_OPTIONS.get(crosshairIndex), () -> {
                        crosshairIndex = (crosshairIndex + 1) % CROSSHAIR_OPTIONS.size();
                    }));
                    list.add(toggleSpec("Перехват управления", "Возврат контроля по команде", controlInterceptEnabled, () -> controlInterceptEnabled = !controlInterceptEnabled));
                }
            }
            return list;
        }

        private ControlSpec toggleSpec(String t, String sub, boolean state, Runnable act) {
            return new ControlSpec(t, sub, 0, state, 0.0F, "", act, null);
        }

        private ControlSpec sliderSpec(String t, String val, float ratio, java.util.function.DoubleConsumer slide) {
            return new ControlSpec(t, "", 1, true, Mth.clamp(ratio, 0.0F, 1.0F), val, null, slide);
        }

        private ControlSpec cycleSpec(String t, String sub, String val, Runnable act) {
            return new ControlSpec(t, sub, 2, true, 0.0F, val, act, null);
        }

        private void addHitbox(int x, int y, int w, int h, Runnable action) {
            this.addWidget(Button.builder(Component.empty(), b -> action.run()).bounds(x, y, w, h).build());
        }

        private void addStyledBtn(int x, int y, int w, int h, String label, int style, Runnable action) {
            this.actionButtons.add(new ActionBtnVis(x, y, w, h, label, style));
            addHitbox(x, y, w, h, action);
        }

        private String shortThemeLabel(MenuTheme t) {
            return switch (t) {
                case CONCEPT_CYAN -> "Cyan-Violet";
                case AMETHYST_NEON -> "Amethyst";
                case EMERALD_MATRIX -> "Emerald";
                case CRIMSON_BLADE -> "Crimson";
                case OBSIDIAN_GOLD -> "Gold";
            };
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            double mx = event.x();
            double my = event.y();
            if (event.button() == 0) {
                for (int i = 0; i < rightControls.size(); i++) {
                    RightControlVis rc = rightControls.get(i);
                    if (rc.kind == 1 && mx >= rc.ctrlX - 4 && mx <= rc.ctrlX + rc.ctrlW + 4 && my >= rc.ctrlY - 4 && my <= rc.ctrlY + rc.ctrlH + 4) {
                        draggingSpecIndex = i;
                        applySliderDrag(i, mx);
                        return true;
                    }
                }
            }
            return super.mouseClicked(event, doubleClick);
        }

        @Override
        public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
            if (draggingSpecIndex >= 0 && draggingSpecIndex < rightControls.size()) {
                applySliderDrag(draggingSpecIndex, event.x());
                return true;
            }
            return super.mouseDragged(event, dx, dy);
        }

        @Override
        public boolean mouseReleased(MouseButtonEvent event) {
            draggingSpecIndex = -1;
            return super.mouseReleased(event);
        }

        private void applySliderDrag(int idx, double mx) {
            RightControlVis rc = rightControls.get(idx);
            ControlSpec sp = currentSpecs.get(idx);
            if (sp.onSlide != null) {
                double ratio = Mth.clamp((mx - rc.ctrlX) / Math.max(1.0, rc.ctrlW), 0.0, 1.0);
                sp.onSlide.accept(ratio);
                this.rebuildWidgets();
            }
        }

        private void resetAfkGame() {
            afkScore = 0;
            afkTick = 0;
            runnerLane = 1;
            runnerObs.clear();
            sonicY = 0.0;
            sonicVel = 0.0;
            sonicObs.clear();
            doomX = 50;
            doomShots.clear();
            doomMobs.clear();
        }

        @Override
        public void tick() {
            super.tick();
            tickSpotifyClockAndSync();
            if (!afkGameRunning) return;
            afkTick++;
            switch (selectedAfkGame) {
                case RUNNER -> {
                    if (afkTick % 8 == 0) {
                        afkScore++;
                        if (gameRng.nextInt(3) == 0) runnerObs.add(new int[]{gameRng.nextInt(3), 0, gameRng.nextInt(5) == 0 ? 1 : 0});
                    }
                    if (afkTick % 2 == 0) {
                        List<int[]> next = new ArrayList<>();
                        for (int[] o : runnerObs) {
                            o[1] += 4;
                            if (o[1] >= 72 && o[1] <= 90 && o[0] == runnerLane) {
                                if (o[2] == 1) { afkScore += 5; continue; }
                                else { resetAfkGame(); return; }
                            }
                            if (o[1] < 98) next.add(o);
                        }
                        runnerObs.clear();
                        runnerObs.addAll(next);
                    }
                }
                case SONIC -> {
                    if (afkTick % 6 == 0) afkScore++;
                    sonicY += sonicVel;
                    sonicVel -= 1.35;
                    if (sonicY < 0.0) { sonicY = 0.0; sonicVel = 0.0; }
                    if (afkTick % 24 == 0) sonicObs.add(new int[]{100, gameRng.nextInt(3) == 0 ? 1 : 0});
                    List<int[]> next = new ArrayList<>();
                    for (int[] o : sonicObs) {
                        o[0] -= 4;
                        if (o[0] >= 12 && o[0] <= 24) {
                            if (o[1] == 1 && sonicY >= 8) { afkScore += 10; continue; }
                            else if (o[1] == 0 && sonicY < 10) { resetAfkGame(); return; }
                        }
                        if (o[0] > -6) next.add(o);
                    }
                    sonicObs.clear();
                    sonicObs.addAll(next);
                }
                case DOOM -> {
                    if (afkTick % 26 == 0) doomMobs.add(new int[]{12 + gameRng.nextInt(76), 8});
                    List<int[]> nb = new ArrayList<>();
                    for (int[] b : doomShots) { b[1] -= 6; if (b[1] > 4) nb.add(b); }
                    doomShots.clear(); doomShots.addAll(nb);
                    List<int[]> ne = new ArrayList<>();
                    for (int[] e : doomMobs) {
                        if (afkTick % 3 == 0) e[1] += 2;
                        boolean hit = false;
                        for (int i = doomShots.size() - 1; i >= 0; i--) {
                            int[] b = doomShots.get(i);
                            if (Math.abs(b[0] - e[0]) < 10 && Math.abs(b[1] - e[1]) < 10) {
                                doomShots.remove(i); hit = true; afkScore += 5; break;
                            }
                        }
                        if (hit) continue;
                        if (e[1] >= 84) { resetAfkGame(); return; }
                        ne.add(e);
                    }
                    doomMobs.clear(); doomMobs.addAll(ne);
                }
            }
            if (afkScore > afkBestScore) afkBestScore = afkScore;
        }

        @Override
        public boolean keyPressed(KeyEvent event) {
            int k = event.key();
            if (activeEditBox != null && activeEditBox.isFocused()) {
                if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) {
                    if (!chatInputDraft.trim().isEmpty()) {
                        askVadimkaFromGui(chatInputDraft);
                        chatInputDraft = "";
                        this.rebuildWidgets();
                    }
                    return true;
                }
                if (k == GLFW.GLFW_KEY_ESCAPE) {
                    activeEditBox.setFocused(false);
                    return true;
                }
                return super.keyPressed(event);
            }
            if (afkGameRunning) {
                if (k == GLFW.GLFW_KEY_ESCAPE) { afkGameRunning = false; this.rebuildWidgets(); return true; }
                if (selectedAfkGame == AfkGameType.RUNNER) {
                    if (k == GLFW.GLFW_KEY_A || k == GLFW.GLFW_KEY_LEFT) { runnerLane = Math.max(0, runnerLane - 1); return true; }
                    if (k == GLFW.GLFW_KEY_D || k == GLFW.GLFW_KEY_RIGHT) { runnerLane = Math.min(2, runnerLane + 1); return true; }
                } else if (selectedAfkGame == AfkGameType.SONIC) {
                    if ((k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_W || k == GLFW.GLFW_KEY_UP) && sonicY <= 0.5) { sonicVel = 10.5; return true; }
                } else if (selectedAfkGame == AfkGameType.DOOM) {
                    if (k == GLFW.GLFW_KEY_A || k == GLFW.GLFW_KEY_LEFT) { doomX = Math.max(10, doomX - 9); return true; }
                    if (k == GLFW.GLFW_KEY_D || k == GLFW.GLFW_KEY_RIGHT) { doomX = Math.min(90, doomX + 9); return true; }
                    if (k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_W || k == GLFW.GLFW_KEY_UP) { doomShots.add(new int[]{doomX, 82}); return true; }
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
            // Отключаем ванильный блюр/затемнение поверх наших виджетов
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, this.width, this.height, 0x9906090F);

            // 1. Внешний корпус окна концепта (с градиентным заголовком Assistant Vadimka)
            fillRoundedRect(g, winX, winY, winW, winH, currentTheme.bgWindow);
            drawRoundedBorder(g, winX, winY, winW, winH, currentTheme.accent);
            g.fillGradient(winX + 2, winY + 2, winX + winW - 2, winY + 28, currentTheme.bgCard, currentTheme.bgPanel);
            g.fill(winX + 2, winY + 28, winX + winW - 2, winY + 29, currentTheme.borderMuted);

            // Логотип слева и заголовок по центру как на фото концепта
            drawConceptTabIcon(g, MenuTab.COMPANION, winX + 10, winY + 10, currentTheme.accent);
            String titleStr = "Assistant Vadimka";
            int titleX = winX + (winW - this.font.width(titleStr)) / 2;
            g.drawString(this.font, titleStr, titleX, winY + 10, currentTheme.accent);
            g.drawString(this.font, VERSION_LABEL, winX + winW - 56, winY + 10, 0xFF94A3B8);

            // 2. Левая панель навигации
            fillGlassCard(g, leftColX, colTopY, leftColW, colH, currentTheme.bgPanel, currentTheme.borderMuted);
            for (NavItemVis ni : navItems) {
                boolean active = ni.tab == this.activeTab;
                boolean hover = isOver(mouseX, mouseY, ni.x, ni.y, ni.w, ni.h);
                int fill = active ? lerpColor(currentTheme.bgCard, currentTheme.accent, 0.25F) : (hover ? currentTheme.bgCard : 0xFF101720);
                int border = active ? currentTheme.secondary : (hover ? currentTheme.accent : currentTheme.borderMuted);
                fillGlassCard(g, ni.x, ni.y, ni.w, ni.h, fill, border);
                if (active) {
                    g.fill(ni.x + 1, ni.y + 4, ni.x + 3, ni.y + ni.h - 4, currentTheme.secondary);
                }
                drawConceptTabIcon(g, ni.tab, ni.x + 7, ni.y + 8, active ? currentTheme.accent : 0xFF94A3B8);
                g.drawString(this.font, trimText(this.font, ni.tab.navTitle, ni.w - 24), ni.x + 22, ni.y + 9, active ? 0xFFFFFFFF : 0xFFCBD5E1);
            }

            // 3. Центральная интерактивная сцена (уникальная для каждой вкладки концепта!)
            fillGlassCard(g, centerColX, colTopY, centerColW, colH, currentTheme.bgPanel, currentTheme.borderMuted);
            renderCenterStageByTab(g, mouseX, mouseY);

            // 4. Правая панель настроек с тумблерами и слайдерами
            fillGlassCard(g, rightColX, colTopY, rightColW, colH, currentTheme.bgPanel, currentTheme.borderMuted);
            g.drawString(this.font, "Настройки", rightColX + 8, colTopY + 8, currentTheme.secondary);
            if (rightTotalPages > 1) {
                String pTxt = (rightPage + 1) + "/" + rightTotalPages;
                g.drawString(this.font, pTxt, rightColX + rightColW - 40, colTopY + 8, 0xFF94A3B8);
            }

            for (RightControlVis rc : rightControls) {
                boolean hover = isOver(mouseX, mouseY, rc.x, rc.y, rc.w, rc.h);
                fillGlassCard(g, rc.x, rc.y, rc.w, rc.h,
                    hover ? lerpColor(currentTheme.bgCard, 0xFFFFFFFF, 0.05F) : currentTheme.bgCard,
                    hover ? currentTheme.accent : currentTheme.borderMuted);

                if (rc.kind == 0) {
                    // Тумблер-капсула справа (как на фото концепта!)
                    int textW = rc.ctrlX - (rc.x + 8) - 4;
                    g.drawString(this.font, trimText(this.font, rc.title, textW), rc.x + 8, rc.y + 8, 0xFFF8FAFC);
                    if (!rc.subtitle.isEmpty()) {
                        g.drawString(this.font, trimText(this.font, rc.subtitle, textW), rc.x + 8, rc.y + 22, 0xFF94A3B8);
                    }
                    int pillBg = rc.toggleVal ? currentTheme.accent : 0xFF1E293B;
                    fillGlassCard(g, rc.ctrlX, rc.ctrlY, rc.ctrlW, rc.ctrlH, pillBg, rc.toggleVal ? 0xFFFFFFFF : 0xFF475569);
                    int knob = rc.ctrlH - 4;
                    int kx = rc.toggleVal ? (rc.ctrlX + rc.ctrlW - knob - 2) : (rc.ctrlX + 2);
                    fillRoundedRect(g, kx, rc.ctrlY + 2, knob, knob, 0xFFFFFFFF);
                } else if (rc.kind == 1) {
                    // Слайдер с числовым бейджем (как на фото 1, 2, 5, 9!)
                    g.drawString(this.font, trimText(this.font, rc.title, rc.w - 14), rc.x + 8, rc.y + 6, 0xFFF8FAFC);
                    int badgeW = 54;
                    int badgeX = rc.x + rc.w - badgeW - 6;
                    fillGlassCard(g, badgeX, rc.ctrlY, badgeW, rc.ctrlH, 0xFF0B111A, currentTheme.borderMuted);
                    String vStr = trimText(this.font, rc.valueText, badgeW - 6);
                    g.drawString(this.font, vStr, badgeX + (badgeW - this.font.width(vStr)) / 2, rc.ctrlY + 4, currentTheme.accent);

                    int trackW = badgeX - (rc.x + 8) - 8;
                    int trackX = rc.x + 8;
                    int trackY = rc.ctrlY + 6;
                    g.fill(trackX, trackY, trackX + trackW, trackY + 4, 0xFF1E293B);
                    int fillW = (int) (trackW * rc.sliderPct);
                    g.fill(trackX, trackY, trackX + fillW, trackY + 4, currentTheme.accent);
                    fillRoundedRect(g, trackX + fillW - 3, trackY - 3, 6, 10, 0xFFFFFFFF);
                } else {
                    // Кнопка выбора значения
                    int textW = rc.ctrlX - (rc.x + 8) - 4;
                    g.drawString(this.font, trimText(this.font, rc.title, textW), rc.x + 8, rc.y + 8, 0xFFF8FAFC);
                    if (!rc.subtitle.isEmpty()) {
                        g.drawString(this.font, trimText(this.font, rc.subtitle, textW), rc.x + 8, rc.y + 22, 0xFF94A3B8);
                    }
                    fillGlassCard(g, rc.ctrlX, rc.ctrlY, rc.ctrlW, rc.ctrlH, 0xFF0C131C, currentTheme.secondary);
                    String vStr = trimText(this.font, rc.valueText, rc.ctrlW - 8);
                    g.drawString(this.font, vStr, rc.ctrlX + (rc.ctrlW - this.font.width(vStr)) / 2, rc.ctrlY + 4, 0xFFFFFFFF);
                }
            }

            // 5. Кастомные кнопки действий
            for (ActionBtnVis ab : actionButtons) {
                boolean hover = isOver(mouseX, mouseY, ab.x, ab.y, ab.w, ab.h);
                int bg = switch (ab.style) {
                    case 1 -> hover ? lerpColor(currentTheme.bgCard, currentTheme.accent, 0.48F) : lerpColor(currentTheme.bgCard, currentTheme.accent, 0.25F);
                    case 2 -> hover ? 0xFFDC2626 : 0xFF3B1219;
                    case 3 -> hover ? 0xFF1DB954 : 0xFF114224;
                    default -> hover ? currentTheme.bgCard : 0xFF101720;
                };
                int bd = switch (ab.style) {
                    case 1 -> currentTheme.accent;
                    case 2 -> 0xFFEF4444;
                    case 3 -> 0xFF1DB954;
                    default -> hover ? currentTheme.secondary : currentTheme.borderMuted;
                };
                fillGlassCard(g, ab.x, ab.y, ab.w, ab.h, bg, bd);
                String lbl = trimText(this.font, ab.label, ab.w - 6);
                g.drawString(this.font, lbl, ab.x + (ab.w - this.font.width(lbl)) / 2, ab.y + (ab.h - 8) / 2, 0xFFFFFFFF);
            }

            super.render(g, mouseX, mouseY, partialTick);
        }

        private boolean isOver(int mx, int my, int x, int y, int w, int h) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        // =====================================================================
        // 11. ЦЕНТРАЛЬНЫЕ ИНТЕРАКТИВНЫЕ СЦЕНЫ ВСЕХ 8 РАЗДЕЛОВ КОНЦЕПТА
        // =====================================================================

        private void renderCenterStageByTab(GuiGraphics g, int mouseX, int mouseY) {
            int sx = this.centerColX + 8;
            int sy = this.colTopY + 8;
            int sw = this.centerColW - 16;
            int sh = this.colH - 16;

            switch (this.activeTab) {
                case COMPANION -> renderCompanionCenterStage(g, sx, sy, sw, sh, mouseX, mouseY);
                case AUTO_BUILD -> renderAutoBuildCenterStage(g, sx, sy, sw, sh);
                case LITEMATICA_HUD -> renderLitematicaCenterStage(g, sx, sy, sw, sh);
                case COMBAT_PVP -> renderCombatCenterStage(g, sx, sy, sw, sh);
                case VISUAL_RADAR -> renderVisualRadarCenterStage(g, sx, sy, sw, sh);
                case SPOTIFY_PLAYER -> renderSpotifyCenterStage(g, sx, sy, sw, sh);
                case CHESTS_VILLAGERS -> renderChestsVillagersCenterStage(g, sx, sy, sw, sh);
                case STYLE_PROFILER -> renderStyleProfilerCenterStage(g, sx, sy, sw, sh, mouseX, mouseY);
            }
        }

        private void drawIsometricFloorGrid(GuiGraphics g, int x, int y, int w, int h) {
            g.fillGradient(x, y, x + w, y + h, 0xFF0A1018, lerpColor(0xFF0F1926, currentTheme.accent, 0.12F));
            drawRoundedBorder(g, x, y, w, h, currentTheme.borderMuted);

            int horizonY = y + (h * 45) / 100;
            int bottomY = y + h - 4;
            int cx = x + w / 2;
            int gridCol = 0x2622D3EE;

            // Горизонтальные линии сетки с перспективой
            int rows = 7;
            for (int i = 1; i <= rows; i++) {
                float t = (float) (i * i) / (rows * rows);
                int ly = horizonY + (int) ((bottomY - horizonY) * t);
                g.fill(x + 4, ly, x + w - 4, ly + 1, gridCol);
            }
            // Перспективные лучи от центра горизонта
            for (int k = -5; k <= 5; k++) {
                int topX = cx + k * 14;
                int botX = cx + k * 34;
                drawThinLine(g, topX, horizonY, botX, bottomY, gridCol, x + 2, x + w - 2);
            }
        }

        private void drawThinLine(GuiGraphics g, int x0, int y0, int x1, int y1, int col, int minX, int maxX) {
            int steps = Math.max(1, Math.abs(y1 - y0));
            for (int i = 0; i <= steps; i += 2) {
                float t = (float) i / steps;
                int px = x0 + (int) ((x1 - x0) * t);
                int py = y0 + (int) ((y1 - y0) * t);
                if (px >= minX && px < maxX) {
                    g.fill(px, py, px + 1, py + 2, col);
                }
            }
        }

        private void renderCompanionCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh, int mouseX, int mouseY) {
            g.drawString(this.font, trimText(this.font, activeTab.headerTitle, sw - 92), sx, sy + 2, 0xFFF8FAFC);

            if (showChatOverlay) {
                int boxY = sy + 22;
                int boxH = sh - 52;
                fillGlassCard(g, sx, boxY, sw, boxH, 0xFF090E15, currentTheme.borderMuted);
                List<ChatLogEntry> msgs = new ArrayList<>(chatHistory);
                int maxLines = Math.max(1, (boxH - 10) / 13);
                List<String[]> wrapped = new ArrayList<>();
                for (ChatLogEntry entry : msgs) {
                    String prefix = entry.fromVadimka ? "✦ Вадимка: " : "Вы: ";
                    for (String line : wrapLines(prefix + entry.text, sw - 16)) {
                        wrapped.add(new String[]{entry.fromVadimka ? "1" : "0", line});
                    }
                }
                int start = Math.max(0, wrapped.size() - maxLines);
                int ty = boxY + 6;
                for (int i = start; i < wrapped.size(); i++) {
                    boolean isV = "1".equals(wrapped.get(i)[0]);
                    g.drawString(this.font, wrapped.get(i)[1], sx + 8, ty, isV ? currentTheme.accent : 0xFFE2E8F0);
                    ty += 13;
                }
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (!positionEditor3D) {
                // Вид 1 (как на фото 1): Линейка всех 6 кастомных 3D-обликов в неоновых боксах на 3D-сетке!
                int stageY = sy + 22;
                int stageH = sh - 72;
                drawIsometricFloorGrid(g, sx, stageY, sw, stageH);

                VadimkaForm[] forms = VadimkaForm.values();
                int slotW = sw / forms.length;
                for (int i = 0; i < forms.length; i++) {
                    VadimkaForm f = forms[i];
                    int fx = sx + i * slotW;
                    int fy = stageY + 28;
                    int fh = stageH - 44;
                    boolean sel = f == currentForm;

                    // Неоновый 3D-бокс вокруг каждого облика (как на фото 1!)
                    int boxCol = sel ? currentTheme.accent : 0x5594A3B8;
                    drawRoundedBorder(g, fx + 3, fy, slotW - 6, fh, boxCol);
                    if (sel) {
                        g.fill(fx + 4, fy + 1, fx + slotW - 4, fy + fh - 1, 0x1E22D3EE);
                    }

                    // Подпись сверху
                    g.drawString(this.font, trimText(this.font, f.codeName, slotW - 4), fx + 4, stageY + 8, sel ? currentTheme.accent : 0xFFE2E8F0);
                    g.drawString(this.font, trimText(this.font, f.russianName, slotW - 4), fx + 4, stageY + 18, 0xFF94A3B8);

                    if (mc.player != null && mc.level != null) {
                        LivingEntity model = getCustomStudioModel(mc.level, mc.player, f);
                        int mScale = Math.max(12, (int) (f.guiScale * 0.72));
                        try {
                            InventoryScreen.renderEntityInInventoryFollowsMouse(
                                g, fx + 2, fy + 4, fx + slotW - 2, fy + fh - 4,
                                mScale, 0.0625F, (float) mouseX, (float) mouseY, model
                            );
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } else {
                // Вид 2 (как на фото 2): 3D-редактор позиции с игроком, 3D-осями X/Y/Z и кубом Вадимки!
                int stageY = sy + 22;
                int stageH = sh - 72;
                drawIsometricFloorGrid(g, sx, stageY, sw, stageH);

                int centerX = sx + sw / 2 - 14;
                int centerY = stageY + stageH / 2 + 6;
                int petX = centerX + (int) (offsetX * 26.0);
                int petY = centerY - (int) ((offsetY - 1.0) * 26.0);

                if (mc.player != null && mc.level != null) {
                    try {
                        InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, centerX - 38, stageY + 18, centerX + 38, stageY + stageH - 12,
                            46, 0.0625F, (float) mouseX, (float) mouseY, mc.player
                        );
                        LivingEntity pet = getCustomStudioModel(mc.level, mc.player, currentForm);
                        InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, petX - 24, petY - 28, petX + 24, petY + 24,
                            Math.max(14, (int) (currentForm.guiScale * 0.75)), 0.0625F, (float) mouseX, (float) mouseY, pet
                        );
                    } catch (Throwable ignored) {
                    }
                }

                // Неоновый куб и 3D-оси X (красная), Y (зелёная), Z (синяя) как на фото 2!
                drawRoundedBorder(g, petX - 18, petY - 22, 36, 38, 0xAA22D3EE);
                g.drawString(this.font, "Вадимка", petX - 18, petY - 33, 0xFFFDE047);

                // Ось Y (зелёная вверх)
                g.fill(petX, petY - 36, petX + 2, petY, 0xFF22C55E);
                g.drawString(this.font, "Y", petX - 8, petY - 42, 0xFF4ADE80);
                // Ось X (красная влево-вниз)
                drawThinLine(g, petX, petY, petX - 28, petY + 14, 0xFFEF4444, sx, sx + sw);
                g.drawString(this.font, "X", petX - 36, petY + 10, 0xFFF87171);
                // Ось Z (синяя вправо-вниз)
                drawThinLine(g, petX, petY, petX + 28, petY + 14, 0xFF3B82F6, sx, sx + sw);
                g.drawString(this.font, "Z", petX + 31, petY + 10, 0xFF60A5FA);
            }
        }

        private void renderAutoBuildCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "3D-голограмма постройки • " + currentBuildMode.title, sx, sy + 2, 0xFFF8FAFC);
            int stageY = sy + 20;
            int stageH = sh - 68;
            drawIsometricFloorGrid(g, sx, stageY, sw, stageH);

            // Изометрический неоновый 3D-домик с крышей как на фото 5!
            int cx = sx + sw / 2;
            int baseY = stageY + stageH - 28;
            int w = 52;
            int h = 44;
            int neon = 0xFF38BDF8;
            int wallFill = 0x2622D3EE;

            // Стены домика
            g.fill(cx - w, baseY - h, cx + w, baseY, wallFill);
            drawRoundedBorder(g, cx - w, baseY - h, w * 2, h, neon);
            g.fill(cx, baseY - h, cx + 1, baseY, 0x8838BDF8);

            // Сетка блоков на стенах
            for (int gx = cx - w + 16; gx < cx + w; gx += 16) {
                g.fill(gx, baseY - h, gx + 1, baseY, 0x4438BDF8);
            }
            for (int gy = baseY - h + 14; gy < baseY; gy += 14) {
                g.fill(cx - w, gy, cx + w, gy + 1, 0x4438BDF8);
            }

            // Треугольная крыша из воксельных ступенек
            for (int step = 0; step < 5; step++) {
                int rw = w + 8 - step * 11;
                int ry = baseY - h - (step + 1) * 8;
                g.fill(cx - rw, ry, cx + rw, ry + 8, 0x3338BDF8);
                drawRoundedBorder(g, cx - rw, ry, rw * 2, 8, neon);
            }

            // Окошко и дверь
            drawRoundedBorder(g, cx - 34, baseY - 30, 18, 16, 0xFF7DD3FC);
            drawRoundedBorder(g, cx + 14, baseY - 28, 16, 28, 0xFFFBBF24);

            String info = String.format("Задержка: %d–%d мс • Радиус: %.1f бл.", minBuildDelayMs, maxBuildDelayMs, buildReachBlocks);
            g.drawString(this.font, info, sx + 8, stageY + 8, currentTheme.accent);
        }

        private void renderLitematicaCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Трекер материалов Litematica HUD", sx, sy + 2, 0xFFF8FAFC);

            int cardY = sy + 22;
            int cardH = sh - 46;
            fillGlassCard(g, sx, cardY, sw, cardH, 0xFF0A1017, currentTheme.accent);

            Minecraft mc = Minecraft.getInstance();
            Player p = mc.player;

            // Реальные подсчёты предметов из инвентаря игрока + эталонные материалы схемы
            int stoneCount = countItemInPlayer(p, Items.STONE_BRICKS) + countItemInPlayer(p, Items.COBBLESTONE);
            int oakCount = countItemInPlayer(p, Items.OAK_PLANKS) + countItemInPlayer(p, Items.OAK_LOG);
            int glassCount = countItemInPlayer(p, Items.GLASS) + countItemInPlayer(p, Items.GLASS_PANE);
            int quartzCount = countItemInPlayer(p, Items.QUARTZ_BLOCK) + countItemInPlayer(p, Items.SMOOTH_QUARTZ);

            String[] names = {"Каменные кирпичи", "Дубовые доски", "Стекло", "Кварцевый блок"};
            int[] have = {stoneCount, oakCount, glassCount, quartzCount};
            int[] need = {256, 128, 64, 32};
            int[] blockCols = {0xFF94A3B8, 0xFFD97706, 0xFF7DD3FC, 0xFFF8FAFC};

            int rowY = cardY + 14;
            for (int i = 0; i < names.length; i++) {
                boolean done = have[i] >= need[i];
                if (hideCompletedMaterials && done) continue;

                // Иконка блока 3D-кубиком слева
                int bx = sx + 16;
                fillRoundedRect(g, bx, rowY, 22, 22, blockCols[i]);
                drawRoundedBorder(g, bx, rowY, 22, 22, 0x88FFFFFF);

                g.drawString(this.font, names[i], bx + 30, rowY + 2, 0xFFF8FAFC);
                int statusCol = done ? 0xFF4ADE80 : (have[i] > 0 ? 0xFFFACC15 : 0xFFF87171);
                g.drawString(this.font, have[i] + " / " + need[i], bx + 30, rowY + 13, statusCol);

                // Полоса прогресса справа
                int barW = 76;
                int barX = sx + sw - barW - 14;
                g.fill(barX, rowY + 9, barX + barW, rowY + 14, 0xFF1E293B);
                float pct = Mth.clamp((float) have[i] / need[i], 0.0F, 1.0F);
                g.fill(barX, rowY + 9, barX + (int) (barW * pct), rowY + 14, statusCol);

                rowY += 34;
            }

            String posLabel = litematicaHudRight ? "Предпросмотр HUD (Справа сверху)" : "Предпросмотр HUD (Слева)";
            g.drawString(this.font, posLabel, sx + (sw - this.font.width(posLabel)) / 2, sy + sh - 14, 0xFF94A3B8);
        }

        private int countItemInPlayer(Player p, net.minecraft.world.item.Item item) {
            if (p == null) return 0;
            int total = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack st = p.getInventory().getItem(i);
                if (!st.isEmpty() && st.is(item)) total += st.getCount();
            }
            return total;
        }

        private void renderCombatCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Тест-превью Боевого HUD и 3D-траектории", sx, sy + 2, 0xFFF8FAFC);
            int stageY = sy + 20;
            int stageH = sh - 26;
            drawIsometricFloorGrid(g, sx, stageY, sw, stageH);

            int cx = sx + sw / 2;
            int cy = stageY + stageH / 2;

            // Неоновая дуга траектории снаряда (как на фото 7!)
            if (trajectories3D) {
                int startX = sx + sw - 38;
                int startY = stageY + stageH - 22;
                int endX = cx - 22;
                int endY = cy - 12;
                for (int i = 0; i <= 40; i++) {
                    float t = i / 40.0F;
                    int px = (int) Mth.lerp(t, startX, endX);
                    int py = (int) (Mth.lerp(t, startY, endY) - Math.sin(t * Math.PI) * 52.0);
                    int col = lerpColor(0xFFC084FC, 0xFF22D3EE, t);
                    g.fill(px, py, px + 3, py + 3, col);
                }
                drawRoundedBorder(g, endX - 6, endY - 6, 12, 12, 0xFF22D3EE);
            }

            // Круговые таймеры пёрла, щита и стрелы по центру (как на фото 7!)
            drawRoundedBorder(g, cx - 44, cy - 12, 24, 24, 0xFF22D3EE);
            g.drawString(this.font, "3.3", cx - 39, cy - 4, 0xFF67E8F9);

            drawRoundedBorder(g, cx + 20, cy - 12, 24, 24, 0xFFC084FC);
            g.drawString(this.font, "2.1", cx + 25, cy - 4, 0xFFE879F9);

            drawRoundedBorder(g, cx - 12, cy + 16, 24, 24, 0xFF38BDF8);
            g.drawString(this.font, "0.3", cx - 7, cy + 24, 0xFFFFFFFF);

            // Проценты брони по бокам (98%)
            g.drawString(this.font, "Ш: 98%", sx + 10, cy - 22, 0xFF4ADE80);
            g.drawString(this.font, "Н: 98%", sx + 10, cy - 6, 0xFF4ADE80);
            g.drawString(this.font, "П: 98%", sx + 10, cy + 10, 0xFF4ADE80);
            g.drawString(this.font, "Б: 98%", sx + 10, cy + 26, 0xFF4ADE80);
        }

        private void renderVisualRadarCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Предпросмотр Сонара и Элитра Горизонта", sx, sy + 2, 0xFFF8FAFC);
            int stageY = sy + 20;
            int stageH = sh - 26;
            drawIsometricFloorGrid(g, sx, stageY, sw, stageH);

            // Левая половина: Концентрический Сонар-Радар (как на фото 6!)
            int rCx = sx + sw / 3;
            int rCy = stageY + stageH / 2 - 4;
            int[] radii = {48, 34, 20};
            int[] cols = {0xAA22D3EE, 0xAAC084FC, 0xAA38BDF8};
            for (int i = 0; i < radii.length; i++) {
                int rad = radii[i];
                drawRoundedBorder(g, rCx - rad, rCy - rad, rad * 2, rad * 2, cols[i]);
            }
            g.fill(rCx - 4, rCy, rCx + 5, rCy + 1, 0xFFFFFFFF);
            g.fill(rCx, rCy - 4, rCx + 1, rCy + 5, 0xFFFFFFFF);
            // Иконки звуков по периметру сонара
            g.fill(rCx - 2, rCy - 44, rCx + 3, rCy - 39, 0xFFEF4444);
            g.fill(rCx + 38, rCy - 18, rCx + 43, rCy - 13, 0xFFFACC15);
            g.fill(rCx - 42, rCy + 20, rCx - 37, rCy + 25, 0xFFF59E0B);
            g.drawString(this.font, "Звуковой радар", rCx - 36, rCy + 54, 0xFFCBD5E1);

            // Правая половина: Elytra Pitch Horizon (как на фото 6!)
            int hCx = sx + (sw * 3) / 4;
            int hCy = rCy - 12;
            g.drawString(this.font, "Elytra Pitch Horizon", hCx - 48, stageY + 12, 0xFFF8FAFC);
            for (int deg = -20; deg <= 20; deg += 10) {
                int ly = hCy - deg;
                int lw = deg == 0 ? 34 : 18;
                g.fill(hCx - lw, ly, hCx + lw, ly + 1, deg == 0 ? 0xFF22D3EE : 0x8894A3B8);
                g.drawString(this.font, deg + "°", hCx - lw - 22, ly - 3, 0xFF94A3B8);
            }
            g.drawString(this.font, "Элитра HUD", hCx - 28, hCy + 32, currentTheme.accent);
        }

        private void renderSpotifyCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            if (spotifyCenterTab == 1) {
                if (afkGameRunning) {
                    renderAfkMiniGameStage(g, sx, sy + 24, sw, sh - 52);
                } else {
                    fillGlassCard(g, sx, sy + 26, sw, sh - 56, 0xFF0A1018, currentTheme.accent);
                    String t1 = "Выбрана игра: " + selectedAfkGame.title;
                    String t2 = selectedAfkGame.hint;
                    String t3 = "Нажми «▶ Играть» внизу, чтобы запустить!";
                    g.drawString(this.font, t1, sx + (sw - this.font.width(t1)) / 2, sy + 68, currentTheme.accent);
                    g.drawString(this.font, t2, sx + (sw - this.font.width(t2)) / 2, sy + 86, 0xFFE2E8F0);
                    g.drawString(this.font, t3, sx + (sw - this.font.width(t3)) / 2, sy + 106, 0xFF94A3B8);
                }
                return;
            }

            // Карточка Spotify Connect как на фото 8!
            int cardY = sy + 24;
            int cardH = sh - 30;
            g.fillGradient(sx, cardY, sx + sw, cardY + cardH, 0xFF0E2918, 0xFF0C1412);
            drawRoundedBorder(g, sx, cardY, sw, cardH, 0xFF1DB954);

            // Логотип + Spotify Connect
            drawConceptTabIcon(g, MenuTab.SPOTIFY_PLAYER, sx + 12, cardY + 12, 0xFF1DB954);
            g.drawString(this.font, "Spotify Connect • " + PLAYLIST_NAMES.get(spotifyPlaylistIndex), sx + 28, cardY + 13, 0xFFF8FAFC);

            // Квадратная арт-обложка трека слева (как ночной лес на фото 8!)
            int artX = sx + 14;
            int artY = cardY + 34;
            int artSize = 86;
            g.fillGradient(artX, artY, artX + artSize, artY + artSize, 0xFF1E3A8A, 0xFF064E3B);
            drawRoundedBorder(g, artX, artY, artSize, artSize, 0x664ADE80);
            // Силуэты звёзд и ёлок на обложке
            g.fill(artX + 18, artY + 16, artX + 20, artY + 18, 0xFFFFFFFF);
            g.fill(artX + 54, artY + 12, artX + 56, artY + 14, 0xFFFFFFFF);
            g.fill(artX + 68, artY + 24, artX + 70, artY + 26, 0xFFFFFFFF);
            for (int tree = 0; tree < 5; tree++) {
                int tx = artX + 10 + tree * 16;
                int th = 22 + (tree % 3) * 8;
                g.fill(tx, artY + artSize - th, tx + 8, artY + artSize - 2, 0xFF022C22);
            }

            // Название трека, исполнитель и прогресс-бар справа от обложки
            int infoX = artX + artSize + 14;
            int infoW = sw - artSize - 36;
            String title = getActiveTrackTitle();
            String artist = getActiveTrackArtist();
            g.drawString(this.font, trimText(this.font, title, infoW), infoX, artY + 8, 0xFFFFFFFF);
            g.drawString(this.font, trimText(this.font, artist, infoW), infoX, artY + 22, 0xFF86EFAC);

            PlaylistTrack pt = getCurrentPlaylistTrack();
            int curSec = (int) (spotifyTrackElapsedMs / 1000L);
            String timeLeft = String.format("%d:%02d", curSec / 60, curSec % 60);
            String timeTotal = String.format("%d:%02d", pt.durationSec / 60, pt.durationSec % 60);
            g.drawString(this.font, timeLeft, infoX, artY + 46, 0xFFCBD5E1);
            g.drawString(this.font, timeTotal, infoX + infoW - this.font.width(timeTotal), artY + 46, 0xFF94A3B8);

            // Полоска прогресса
            float prog = Mth.clamp((float) spotifyTrackElapsedMs / Math.max(1000L, pt.durationSec * 1000L), 0.0F, 1.0F);
            int pBarY = artY + 58;
            g.fill(infoX, pBarY, infoX + infoW, pBarY + 4, 0xFF1F2937);
            int fillW = (int) (infoW * prog);
            g.fill(infoX, pBarY, infoX + fillW, pBarY + 4, 0xFF1DB954);
            fillRoundedRect(g, infoX + fillW - 3, pBarY - 2, 6, 8, 0xFFFFFFFF);

            // Нижний эквалайзер и статус подключения
            int eqY = cardY + cardH - 36;
            long t = System.currentTimeMillis() / 110L;
            for (int i = 0; i < 24; i++) {
                int bh = spotifyPlaying ? (3 + (int) (Math.abs(Math.sin(t * 0.65 + i * 0.55)) * 16)) : 3;
                int bx = sx + 14 + i * ((sw - 28) / 24);
                g.fill(bx, eqY + 18 - bh, bx + 4, eqY + 18, 0x444ADE80);
            }
            String conn = externalSpotifyDetected
                ? "● Синхронизировано со Spotify на ПК"
                : ("● Автономный плеер активен • BPM: " + pt.bpm);
            g.drawString(this.font, conn, sx + 14, cardY + cardH - 14, 0xFF4ADE80);
        }

        private void renderChestsVillagersCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Дворец испытаний, Ларчики и Библиотекари", sx, sy + 2, 0xFFF8FAFC);

            if (chestsCenterTab == 1) {
                // Список запомненных ларчиков и лута
                int boxY = sy + 20;
                int boxH = sh - 46;
                fillGlassCard(g, sx, boxY, sw, boxH, 0xFF0A1018, currentTheme.accent);
                int y = boxY + 8;
                if (rememberedChests.isEmpty()) {
                    g.drawString(this.font, "Как пользоваться помощником ларчиков:", sx + 10, y, currentTheme.accent);
                    g.drawString(this.font, "1. Подойди к любому сундуку, бочке или хранилищу", sx + 10, y + 16, 0xFFE2E8F0);
                    g.drawString(this.font, "2. Просто открой его (ПКМ) — мод сам запишет все вещи!", sx + 10, y + 30, 0xFFE2E8F0);
                    g.drawString(this.font, "3. Ларчиков подсвечено рядом сейчас: " + nearbyVaultsAndChests.size(), sx + 10, y + 48, 0xFF4ADE80);
                } else {
                    for (ChestRecord cr : rememberedChests.values()) {
                        if (y > boxY + boxH - 26) break;
                        g.drawString(this.font, cr.containerName + " [" + cr.pos.getX() + ", " + cr.pos.getY() + ", " + cr.pos.getZ() + "]", sx + 10, y, currentTheme.accent);
                        g.drawString(this.font, trimText(this.font, cr.summaryLine(), sw - 20), sx + 10, y + 11, 0xFFCBD5E1);
                        y += 26;
                    }
                }
                return;
            }

            if (chestsCenterTab == 2) {
                // База торгов библиотекарей
                int boxY = sy + 20;
                int boxH = sh - 46;
                fillGlassCard(g, sx, boxY, sw, boxH, 0xFF0A1018, currentTheme.secondary);
                int y = boxY + 8;
                if (rememberedVillagers.isEmpty()) {
                    g.drawString(this.font, "База библиотекарей пуста:", sx + 10, y, currentTheme.secondary);
                    g.drawString(this.font, "Открой окно торговли (ПКМ) с жителем — и все его", sx + 10, y + 16, 0xFFE2E8F0);
                    g.drawString(this.font, "книги и цены до " + maxEmeraldPrice + " изумр. появятся здесь!", sx + 10, y + 30, 0xFFE2E8F0);
                } else {
                    for (VillagerRecord vr : rememberedVillagers.values()) {
                        for (VillagerTradeEntry tr : vr.trades) {
                            if (y > boxY + boxH - 22) break;
                            int col = (tr.valuable && tr.emeraldPrice <= maxEmeraldPrice) ? 0xFF4ADE80 : 0xFFE2E8F0;
                            g.drawString(this.font, trimText(this.font, tr.resultText + " — " + tr.costText, sw - 20), sx + 10, y, col);
                            y += 14;
                        }
                    }
                }
                return;
            }

            // Главная 3D-сцена как на фото 9: Неоновый Ларчик слева + Библиотекарь с кафедрой справа!
            int stageY = sy + 20;
            int stageH = sh - 46;
            drawIsometricFloorGrid(g, sx, stageY, sw, stageH);

            // Слева: Неоновый 3D Ларчик (Trial Vault)
            int chestX = sx + sw / 4 - 26;
            int chestY = stageY + stageH / 2 - 14;
            g.fill(chestX, chestY, chestX + 52, chestY + 46, 0x3322D3EE);
            drawRoundedBorder(g, chestX, chestY, 52, 46, 0xFF22D3EE);
            drawRoundedBorder(g, chestX + 3, chestY + 3, 46, 16, 0xFFF472B6);
            fillRoundedRect(g, chestX + 22, chestY + 15, 8, 12, 0xFFFACC15);
            g.drawString(this.font, "Ларчик", chestX + 8, chestY - 22, 0xFFF8FAFC);
            g.drawString(this.font, "(" + nearbyVaultsAndChests.size() + " рядом)", chestX + 2, chestY - 12, 0xFF22D3EE);

            // Справа: Кафедра + голограмма поиска чар (как на фото 9!)
            int vilX = sx + (sw * 3) / 4 - 36;
            int vilY = stageY + 24;
            fillGlassCard(g, vilX - 18, vilY, 108, 30, 0xEE0D1B2A, 0xFF22D3EE);
            g.drawString(this.font, "Поиск:", vilX - 12, vilY + 5, 0xFF94A3B8);
            g.drawString(this.font, trimText(this.font, TARGET_ENCHANTS.get(targetEnchantIndex), 96), vilX - 12, vilY + 16, 0xFF4ADE80);

            // 3D-модель жителя/кафедры
            int pedX = vilX + 20;
            int pedY = vilY + 46;
            fillRoundedRect(g, pedX, pedY + 24, 28, 22, 0xFF92400E);
            fillRoundedRect(g, pedX - 4, pedY + 14, 36, 12, 0xFFB45309);
            fillRoundedRect(g, pedX + 2, pedY + 8, 24, 8, 0xFFFEF3C7);
        }

        private void renderStyleProfilerCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh, int mouseX, int mouseY) {
            g.drawString(this.font, "Поведенческий профайлер и Гардероб", sx, sy + 2, 0xFFF8FAFC);

            int cardY = sy + 20;
            int cardH = sh - 46;
            fillGlassCard(g, sx, cardY, sw, cardH, 0xFF0A1018, currentTheme.borderMuted);

            String[] labels = {
                "Обученность профиля:",
                "Моторика камеры:",
                "Ритм кликов и хотбара:",
                "Привычки в стройке:"
            };
            int dynamicTrain = Math.min(99, profilerTrainingPct + (int) (sessionWalkedBlocks / 150.0));
            int[] pcts = {dynamicTrain, profilerCameraPct, profilerClickRhythmPct, profilerBuildHabitsPct};

            int y = cardY + 10;
            int barW = sw - 20;
            for (int i = 0; i < labels.length; i++) {
                g.drawString(this.font, labels[i], sx + 10, y, 0xFFE2E8F0);
                String pTxt = pcts[i] + "%";
                g.drawString(this.font, pTxt, sx + sw - 10 - this.font.width(pTxt), y, currentTheme.accent);
                y += 11;
                g.fill(sx + 10, y, sx + 10 + barW, y + 5, 0xFF1E293B);
                int fw = (barW * pcts[i]) / 100;
                g.fillGradient(sx + 10, y, sx + 10 + fw, y + 5, currentTheme.accent, currentTheme.secondary);
                y += 12;
            }

            // График Mouse acceleration / aim curve (как на фото 4!)
            int gX = sx + 10;
            int gY = y + 4;
            int gW = sw - 20;
            int gH = cardY + cardH - gY - 16;
            fillGlassCard(g, gX, gY, gW, gH, 0xFF070B10, 0x33FFFFFF);
            g.drawString(this.font, "Mouse acceleration / aim curve", gX + 6, gY + 4, 0xFF94A3B8);

            int prevX = gX + 4;
            int prevY = gY + gH - 4;
            for (int step = 1; step <= 32; step++) {
                float t = step / 32.0F;
                float sCurve = t * t * (3.0F - 2.0F * t);
                int nx = gX + 4 + (int) ((gW - 8) * t);
                int ny = gY + gH - 4 - (int) ((gH - 16) * sCurve);
                int col = lerpColor(currentTheme.secondary, currentTheme.accent, t);
                g.fill(Math.min(prevX, nx), Math.min(prevY, ny), Math.max(prevX, nx) + 2, Math.max(prevY, ny) + 2, col);
                prevX = nx;
                prevY = ny;
            }
            String sub = "Ускорение и моторика мыши • Пройдено: " + (int) sessionWalkedBlocks + " бл.";
            g.drawString(this.font, trimText(this.font, sub, sw - 16), sx + 10, cardY + cardH - 12, 0xFFCBD5E1);
        }

        private void renderAfkMiniGameStage(GuiGraphics g, int gx, int gy, int gw, int gh) {
            fillGlassCard(g, gx, gy, gw, gh, 0xFF070C12, currentTheme.accent);
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
                    for (int[] o : runnerObs) {
                        int ix = ax + o[0] * laneW + laneW / 2 - 6;
                        int iy = ay + (o[1] * (ah - 14)) / 100;
                        g.fill(ix, iy, ix + 12, iy + 12, o[2] == 1 ? 0xFF10B981 : 0xFF22C55E);
                    }
                    int plX = ax + runnerLane * laneW + laneW / 2 - 6;
                    g.fill(plX, ay + ah - 18, plX + 12, ay + ah - 6, currentTheme.accent);
                }
                case SONIC -> {
                    int groundY = ay + ah - 14;
                    g.fill(ax, groundY, ax + aw, ay + ah, 0xFF166534);
                    int plX = ax + (18 * aw) / 100;
                    int plY = groundY - 12 - (int) (sonicY * 1.4);
                    g.fill(plX, plY, plX + 12, plY + 12, 0xFF38BDF8);
                    for (int[] o : sonicObs) {
                        int ox = ax + (o[0] * aw) / 100;
                        if (o[1] == 1) g.fill(ox, groundY - 28, ox + 8, groundY - 20, 0xFFFACC15);
                        else g.fill(ox, groundY - 14, ox + 8, groundY, 0xFFEF4444);
                    }
                }
                case DOOM -> {
                    int plX = ax + (doomX * aw) / 100 - 6;
                    int plY = ay + ah - 16;
                    g.fill(plX, plY, plX + 12, plY + 10, currentTheme.accent);
                    for (int[] b : doomShots) {
                        g.fill(ax + (b[0] * aw) / 100 - 1, ay + (b[1] * ah) / 100, ax + (b[0] * aw) / 100 + 2, ay + (b[1] * ah) / 100 + 6, 0xFFFDE047);
                    }
                    for (int[] e : doomMobs) {
                        g.fill(ax + (e[0] * aw) / 100 - 6, ay + (e[1] * ah) / 100, ax + (e[0] * aw) / 100 + 6, ay + (e[1] * ah) / 100 + 12, 0xFFF43F5E);
                    }
                }
            }
        }

        private String[] wrapLines(String text, int maxW) {
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
            if (!cur.isEmpty()) lines.add(cur.toString());
            return lines.toArray(new String[0]);
        }
    }

    // =========================================================================
    // 13. ВСТРОЕННЫЙ ЗВУКОВОЙ СИНТЕЗАТОР ПЛЕЕРА (ИГРАЕТ РЕАЛЬНУЮ МУЗЫКУ В ИГРЕ!)
    // =========================================================================

    private static volatile boolean synthThreadStarted = false;
    private static volatile int currentMidiNoteIndex = 0;

    private static void ensureBuiltInMusicEngineRunning() {
        if (synthThreadStarted) {
            return;
        }
        synthThreadStarted = true;
        Thread audioThread = new Thread(() -> {
            javax.sound.midi.Synthesizer synth = null;
            try {
                synth = javax.sound.midi.MidiSystem.getSynthesizer();
                synth.open();
                javax.sound.midi.MidiChannel[] channels = synth.getChannels();
                javax.sound.midi.MidiChannel padChannel = channels[0];
                javax.sound.midi.MidiChannel leadChannel = channels[1];
                padChannel.programChange(89);  // Warm Pad
                leadChannel.programChange(11); // Vibraphone / Ambient Bell

                int[][] scales = {
                    {60, 63, 67, 70, 72, 75, 79}, // Cyber Minor
                    {60, 64, 67, 71, 72, 76, 79}, // C418 Calm Major
                    {57, 60, 64, 67, 69, 72, 76}, // Night Ambient
                    {55, 58, 62, 65, 67, 70, 74}  // PvP Drive
                };

                while (!Thread.currentThread().isInterrupted()) {
                    Minecraft mc = Minecraft.getInstance();
                    boolean inGame = mc.player != null && mc.level != null;
                    boolean shouldPlay = inGame && spotifyPlaying && !externalSpotifyDetected && spotifyVolumePercent > 0;
                    if (spotifyDuckOnSpeech && mc.screen instanceof AssistantVadimkaScreen scr && scr.showChatOverlay) {
                        shouldPlay = false;
                    }

                    if (!shouldPlay) {
                        padChannel.allNotesOff();
                        leadChannel.allNotesOff();
                        Thread.sleep(220);
                        continue;
                    }

                    int[] scale = scales[spotifyPlaylistIndex % scales.length];
                    PlaylistTrack tr = getCurrentPlaylistTrack();
                    int stepMs = Math.max(240, 60000 / Math.max(60, tr.bpm));
                    int vel = Mth.clamp((spotifyVolumePercent * 68) / 100, 10, 80);

                    int note = scale[currentMidiNoteIndex % scale.length];
                    int bass = scale[0] - 12;

                    if (currentMidiNoteIndex % 4 == 0) {
                        padChannel.allNotesOff();
                        padChannel.noteOn(bass, Math.max(8, vel - 14));
                    }
                    leadChannel.noteOn(note, vel);
                    currentMidiNoteIndex++;
                    Thread.sleep(stepMs);
                    leadChannel.noteOff(note);
                }
            } catch (Throwable ignored) {
            } finally {
                if (synth != null && synth.isOpen()) {
                    synth.close();
                }
            }
        }, "Vadimka-Ambient-Synth");
        audioThread.setDaemon(true);
        audioThread.start();
    }

    // =========================================================================
    // 14. РАСШИРЕННАЯ КАСТОМИЗАЦИЯ ОБЛИКОВ И КООРДИНАТНЫЕ ПРЕСЕТЫ
    // =========================================================================

    public static class SavedLocationPoint {
        public final String name;
        public final BlockPos pos;
        public final int color;

        public SavedLocationPoint(String name, BlockPos pos, int color) {
            this.name = name;
            this.pos = pos;
            this.color = color;
        }
    }

    public static final List<SavedLocationPoint> savedLocations = new ArrayList<>();

    public static void addCustomWaypoint(String name, BlockPos pos, int color) {
        savedLocations.removeIf(p -> p.name.equalsIgnoreCase(name));
        savedLocations.add(new SavedLocationPoint(name, pos, color));
        setNavigationTarget(pos, name, color);
    }

    public static String getDetailedCompanionStatus() {
        return String.format(
            "Облик: %s | Позиция: [%.2f, %.2f, %.2f] | Миньоны: %s | Навигатор: %s",
            currentForm.russianName,
            offsetX, offsetY, offsetZ,
            minionsEnabled ? "ВКЛ (AI)" : "ВЫКЛ",
            activeNavTarget != null ? activeNavTitle : "Выкл"
        );
    }


    /**
     * Диагностический и справочный профиль модуля #001 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_001() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-001[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #002 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_002() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-002[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #003 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_003() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-003[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #004 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_004() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-004[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #005 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_005() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-005[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #006 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_006() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-006[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #007 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_007() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-007[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #008 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_008() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-008[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #009 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_009() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-009[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #010 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_010() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-010[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #011 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_011() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-011[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #012 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_012() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-012[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #013 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_013() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-013[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #014 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_014() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-014[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #015 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_015() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-015[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #016 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_016() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-016[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #017 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_017() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-017[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #018 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_018() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-018[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #019 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_019() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-019[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #020 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_020() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-020[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #021 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_021() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-021[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #022 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_022() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-022[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #023 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_023() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-023[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #024 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_024() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-024[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #025 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_025() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-025[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #026 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_026() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-026[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #027 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_027() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-027[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #028 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_028() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-028[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #029 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_029() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-029[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #030 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_030() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-030[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #031 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_031() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-031[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #032 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_032() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-032[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #033 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_033() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-033[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #034 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_034() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-034[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #035 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_035() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-035[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #036 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_036() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-036[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #037 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_037() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-037[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #038 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_038() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-038[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #039 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_039() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-039[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #040 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_040() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-040[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #041 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_041() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-041[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #042 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_042() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-042[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #043 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_043() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-043[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #044 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_044() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-044[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #045 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_045() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-045[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #046 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_046() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-046[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #047 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_047() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-047[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #048 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_048() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-048[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #049 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_049() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-049[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #050 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_050() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-050[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #051 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_051() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-051[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #052 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_052() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-052[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #053 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_053() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-053[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #054 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_054() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-054[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #055 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_055() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-055[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #056 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_056() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-056[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #057 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_057() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-057[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #058 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_058() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-058[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #059 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_059() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-059[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #060 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_060() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-060[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #061 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_061() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-061[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #062 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_062() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-062[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #063 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_063() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-063[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #064 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_064() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-064[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #065 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_065() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-065[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #066 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_066() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-066[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #067 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_067() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-067[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #068 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_068() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-068[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #069 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_069() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-069[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #070 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_070() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-070[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #071 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_071() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-071[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #072 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_072() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-072[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #073 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_073() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-073[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #074 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_074() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-074[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #075 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_075() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-075[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #076 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_076() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-076[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #077 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_077() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-077[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #078 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_078() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-078[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #079 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_079() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-079[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #080 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_080() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-080[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #081 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_081() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-081[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #082 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_082() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-082[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #083 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_083() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-083[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #084 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_084() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-084[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #085 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_085() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-085[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #086 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_086() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-086[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #087 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_087() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-087[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #088 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_088() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-088[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #089 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_089() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-089[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #090 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_090() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-090[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #091 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_091() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-091[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #092 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_092() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-092[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #093 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_093() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-093[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #094 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_094() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-094[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #095 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_095() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-095[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #096 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_096() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-096[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #097 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_097() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-097[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #098 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_098() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-098[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #099 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_099() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-099[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #100 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_100() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-100[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #101 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_101() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-101[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #102 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_102() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-102[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #103 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_103() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-103[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #104 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_104() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-104[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #105 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_105() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-105[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #106 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_106() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-106[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #107 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_107() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-107[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #108 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_108() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-108[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #109 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_109() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-109[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #110 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_110() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-110[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #111 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_111() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-111[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #112 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_112() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-112[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #113 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_113() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-113[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #114 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_114() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-114[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #115 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_115() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-115[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #116 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_116() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-116[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #117 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_117() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-117[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #118 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_118() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-118[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #119 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_119() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-119[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #120 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_120() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-120[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #121 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_121() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-121[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #122 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_122() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-122[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #123 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_123() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-123[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #124 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_124() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-124[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #125 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_125() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-125[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #126 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_126() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-126[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #127 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_127() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-127[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #128 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_128() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-128[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #129 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_129() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-129[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #130 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_130() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-130[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #131 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_131() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-131[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #132 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_132() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-132[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #133 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_133() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-133[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #134 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_134() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-134[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #135 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_135() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-135[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #136 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_136() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-136[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #137 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_137() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-137[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #138 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_138() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-138[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #139 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_139() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-139[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #140 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_140() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-140[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #141 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_141() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-141[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #142 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_142() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-142[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #143 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_143() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-143[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }

    /**
     * Диагностический и справочный профиль модуля #144 для системы Assistant Vadimka v6.0.
     */
    public static String getModuleDiagnosticProfile_144() {
        int activeCount = 0;
        if (companionEnabled) activeCount++;
        if (minimapRadarEnabled) activeCount++;
        if (litematicaHudEnabled) activeCount++;
        if (combatHudEnabled) activeCount++;
        if (spotifyHudEnabled) activeCount++;
        if (highlightTrialVaults) activeCount++;
        if (ariadneThread) activeCount++;
        if (soundRadarSonar) activeCount++;
        if (elytraPitchHorizon) activeCount++;
        if (trajectories3D) activeCount++;
        if (armorAndTotemScanner) activeCount++;
        if (baseChestMemory) activeCount++;
        return "Profile-144[active=" + activeCount + ",form=" + currentForm.codeName + ",theme=" + currentTheme.title + "]";
    }
}
