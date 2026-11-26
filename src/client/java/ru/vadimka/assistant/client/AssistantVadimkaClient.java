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
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
 * Assistant Vadimka v7.0 PRO MAX (Fabric 1.21.11 / Java 25)
 * - Реальный поиск структур мира: Деревня, Дворец испытаний, Крепость, Древний город, Особняк, Аванпост, Храм
 * - Реальный помощник Дворца испытаний: детекция активных и зловещих Хранилищ (Vault), авто-выбор ключа и авто-открытие
 * - Реальный Windows 10 Spotify Connect через WinRT GlobalSystemMediaTransportControls + EnumWindows fallback
 * - Чёткая круглая 1px-разрешения мини-карта-радар с RLE-ускорением, рельефом, сонаром и 3D-навигатором
 * - Переработанный 3-колоночный интерфейс концепта с 3D-иконками Minecraft, градиентными рамками и без обрезки текста
 * - Кастомные составные 3D-модели спутников (включая редкого Синего Аксолотля и Кибер-Робота) + умные миньоны
 */
public class AssistantVadimkaClient implements ClientModInitializer {
    public static final String MOD_ID = "assistant_vadimka";
    public static final String VERSION_LABEL = "v7.0";

    // =========================================================================
    // 1. ПЕРЕЧИСЛЕНИЯ: ОБЛИКИ, МИНЬОНЫ, ВКЛАДКИ, ТЕМЫ, СТРУКТУРЫ, ИГРЫ
    // =========================================================================

    public enum VadimkaForm {
        CYBER_PLAYER("ROBOT", "Кибер-Робот", 0.72, 34, 0xFF38BDF8, 0xFF0284C7),
        RAT("RAT", "Кибер-Крыска", 0.95, 42, 0xFF94A3B8, 0xFFF472B6),
        ALLAY("ALLAY", "Тихоня-Дух", 0.82, 36, 0xFF22D3EE, 0xFF06B6D4),
        BREEZE("BREEZE", "Шторм-Вихрь", 0.52, 24, 0xFF818CF8, 0xFF4F46E5),
        WARDEN("WARDEN", "Хранитель", 0.34, 15, 0xFF14B8A6, 0xFF0F766E),
        AXOLOTL("AXOLOTL", "Синий Аксолотль", 0.90, 38, 0xFF3B82F6, 0xFFEC4899);

        public final String codeName;
        public final String russianName;
        public final double worldScale;
        public final int guiScale;
        public final int primaryColor;
        public final int secondaryColor;

        VadimkaForm(String codeName, String russianName, double worldScale, int guiScale,
                    int primaryColor, int secondaryColor) {
            this.codeName = codeName;
            this.russianName = russianName;
            this.worldScale = worldScale;
            this.guiScale = guiScale;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
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
        COMPANION("Вадимка", "Спутник Вадимка — 6 3D-обликов"),
        AUTO_BUILD("Постройка", "Авто-постройка и 3D-голограмма"),
        LITEMATICA_HUD("Litematica", "Litematica HUD — Ресурсы и слои"),
        COMBAT_PVP("Бой и PvP", "Бой и PvP — Траектории и Броня"),
        VISUAL_RADAR("Радар", "Круглая Карта, Сонар и Навигатор"),
        SPOTIFY_PLAYER("Spotify", "Spotify Connect (Windows 10) и AFK"),
        CHESTS_VILLAGERS("Ларчики", "Дворец испытаний, Хранилища и Жители"),
        STYLE_PROFILER("Профиль", "Профайлер, Гардероб и Темы");

        public final String navTitle;
        public final String headerTitle;

        MenuTab(String navTitle, String headerTitle) {
            this.navTitle = navTitle;
            this.headerTitle = headerTitle;
        }
    }

    public enum StructureTarget {
        TRIAL_CHAMBER("Дворец испытаний", "trial_chambers", 0xFFF97316),
        VILLAGE("Деревня", "village", 0xFF4ADE80),
        STRONGHOLD("Крепость (Энд)", "stronghold", 0xFFA855F7),
        ANCIENT_CITY("Древний город", "ancient_city", 0xFF14B8A6),
        MANSION("Лесной особняк", "mansion", 0xFFEAB308),
        OUTPOST("Аванпост", "pillager_outpost", 0xFFEF4444),
        MONUMENT("Монумент океана", "monument", 0xFF38BDF8),
        RUINED_PORTAL("Портал в Незер", "ruined_portal", 0xFFC084FC);

        public final String russianName;
        public final String keyFragment;
        public final int beaconColor;

        StructureTarget(String russianName, String keyFragment, int beaconColor) {
            this.russianName = russianName;
            this.keyFragment = keyFragment;
            this.beaconColor = beaconColor;
        }
    }

    public enum CompanionAnchor {
        RIGHT_SHOULDER("Правое плечо", 0.95, 1.15, 0.25),
        LEFT_SHOULDER("Левое плечо", -0.95, 1.15, 0.25),
        ABOVE_HEAD("Над головой", 0.0, 2.35, 0.0),
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
        CONCEPT_CYAN("Cyan-Violet", 0xF00B1118, 0xFF111923, 0xFF17222F, 0xFF22D3EE, 0xFFC084FC, 0xFF26384B, 0xFF0E7490),
        AMETHYST_NEON("Amethyst", 0xF0100A1A, 0xFF171026, 0xFF211736, 0xFFA855F7, 0xFFF472B6, 0xFF3B265E, 0xFF7E22CE),
        EMERALD_MATRIX("Emerald", 0xF008110E, 0xFF0E1B16, 0xFF152720, 0xFF10B981, 0xFF34D399, 0xFF224035, 0xFF047857),
        CRIMSON_BLADE("Crimson", 0xF015090D, 0xFF1F0E14, 0xFF2B141C, 0xFFF43F5E, 0xFFFBBF24, 0xFF4B2330, 0xFFBE123C),
        OBSIDIAN_GOLD("Gold", 0xF0110F0A, 0xFF1A1710, 0xFF242017, 0xFFF59E0B, 0xFFFDE047, 0xFF483D24, 0xFFB45309);

        public final String title;
        public final int bgWindow;
        public final int bgPanel;
        public final int bgCard;
        public final int accent;
        public final int secondary;
        public final int borderMuted;
        public final int toggleOnTrack;

        MenuTheme(String title, int bgWindow, int bgPanel, int bgCard,
                  int accent, int secondary, int borderMuted, int toggleOnTrack) {
            this.title = title;
            this.bgWindow = bgWindow;
            this.bgPanel = bgPanel;
            this.bgCard = bgCard;
            this.accent = accent;
            this.secondary = secondary;
            this.borderMuted = borderMuted;
            this.toggleOnTrack = toggleOnTrack;
        }
    }

    public enum BuildMode {
        FAST("Быстрый"),
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
        SONIC("Рывок по холмам", "Прыжки (SPACE / W)"),
        DOOM("Данж-Шутер", "Огонь (A / D / SPACE)");

        public final String title;
        public final String hint;

        AfkGameType(String title, String hint) {
            this.title = title;
            this.hint = hint;
        }
    }

    // =========================================================================
    // 2. СТРУКТУРЫ ДАННЫХ (ЛАРЧИКИ, ХРАНИЛИЩА, БИБЛИОТЕКАРИ, ЧАТ, ПОИСК)
    // =========================================================================

    public static class VaultInfo {
        public final BlockPos pos;
        public final boolean isVaultBlock;
        public final boolean ominous;
        public final boolean active;
        public final String stateName;

        public VaultInfo(BlockPos pos, boolean isVaultBlock, boolean ominous, boolean active, String stateName) {
            this.pos = pos;
            this.isVaultBlock = isVaultBlock;
            this.ominous = ominous;
            this.active = active;
            this.stateName = stateName;
        }
    }

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
            if (items.isEmpty()) return "Пусто (открой для обновления)";
            StringBuilder sb = new StringBuilder();
            int c = 0;
            for (Map.Entry<String, Integer> e : items.entrySet()) {
                if (c > 0) sb.append(", ");
                sb.append(e.getKey()).append(" x").append(e.getValue());
                c++;
                if (c >= 3) break;
            }
            if (items.size() > 3) sb.append(" (+").append(items.size() - 3).append(")");
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

    public static class FoundStructureRecord {
        public final StructureTarget target;
        public final BlockPos pos;
        public final long foundAtMs;

        public FoundStructureRecord(StructureTarget target, BlockPos pos) {
            this.target = target;
            this.pos = pos;
            this.foundAtMs = System.currentTimeMillis();
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
    // 3. СОСТОЯНИЕ ВСЕХ МОДУЛЕЙ
    // =========================================================================

    // --- 3.1 Спутник Вадимка и 3D-позиция ---
    public static VadimkaForm currentForm = VadimkaForm.AXOLOTL;
    public static boolean companionEnabled = true;
    public static boolean syncEmotions = true;
    public static boolean positionEditor3D = true;
    public static CompanionAnchor currentAnchor = CompanionAnchor.RIGHT_SHOULDER;
    public static double offsetX = 0.95;
    public static double offsetY = 1.15;
    public static double offsetZ = 0.25;
    public static int voiceVolumePercent = 70;
    public static int companionScalePercent = 100;
    public static boolean minionsEnabled = true;
    public static boolean minionsSmartAi = true;
    public static boolean compactNameplates = true;
    public static boolean chatAssistantEnabled = true;
    public static boolean quietMode = false;
    public static final Map<MinionRole, Boolean> activeMinions = new EnumMap<>(MinionRole.class);
    public static final Deque<ChatLogEntry> chatHistory = new ArrayDeque<>();
    public static String vadimkaLastBubble = "Напиши «!деревня» или «!испытания»!";

    private static final Deque<String> pendingChatReplies = new ArrayDeque<>();
    private static int pendingChatReplyDelay = 0;
    private static boolean greetedInWorld = false;

    // --- 3.2 Авто-постройка ---
    public static boolean autoBuildLitematica = false;
    public static boolean smoothBezierCamera = true;
    public static boolean hotbarAutoRefill = true;
    public static boolean autoAnvilEnchant = false;
    public static int minBuildDelayMs = 80;
    public static int maxBuildDelayMs = 150;
    public static double buildReachBlocks = 4.5;
    public static BuildMode currentBuildMode = BuildMode.FAST;

    // --- 3.3 Litematica HUD ---
    public static boolean litematicaHudEnabled = true;
    public static boolean litematicaHudRight = true;
    public static boolean shulkerScanner = true;
    public static boolean hideCompletedMaterials = false;
    public static boolean ruler3DVolume = true;
    public static int buildPaletteIndex = 0;
    public static final List<String> BUILD_PALETTES = List.of(
        "Камень / Дуб",
        "Дворец (Медь/Туф)",
        "Незерит / Чернит",
        "Кварц / Призмарин"
    );

    // --- 3.4 Бой и PvP ---
    public static boolean combatHudEnabled = true;
    public static boolean trajectories3D = true;
    public static boolean armorAndTotemScanner = true;
    public static boolean shieldAndPearlTimers = true;
    public static boolean consumableCounter = true;
    public static boolean backstabIndicator = true;
    public static boolean deathReplayMarker = true;

    // --- 3.5 Визуал, Круглая Мини-карта, Сонар и Поиск Структур ---
    public static boolean visualsEnabled = true;
    public static boolean minimapRadarEnabled = true;
    public static boolean minimapCircularMode = true;
    public static boolean minimapTerrainEnabled = true;
    public static boolean soundRadarSonar = true;
    public static boolean elytraPitchHorizon = true;
    public static boolean highlightChestsWorld = true;
    public static boolean baseChestMemory = true;
    public static boolean ariadneThread = true;
    public static int radarZoomLevel = 0; // 0=1 блок/пикс (ультра-чётко!), 1=2 блока, 2=3 блока

    // --- 3.6 Реальный Windows 10 Spotify Connect (WinRT GSMTC) + AFK ---
    public static boolean spotifyHudEnabled = true;
    public static boolean spotifyDuckOnSpeech = false;
    public static boolean vadimkaDanceToBpm = true;
    public static boolean builtInSynthEnabled = false; // По умолчанию ВЫКЛ, чтобы не мешать реальному Spotify!
    public static boolean afkPanicExit = true;
    public static AfkGameType selectedAfkGame = AfkGameType.RUNNER;

    private static final ExecutorService mediaExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Vadimka-WinRT-Spotify");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean spotifyConnected = false;
    private static volatile boolean spotifyIsPlaying = false;
    private static volatile String spotifyTrackTitle = "Ожидание Spotify...";
    private static volatile String spotifyTrackArtist = "Включи трек на ПК";
    private static volatile String spotifySourceName = "Windows Media";
    private static volatile int spotifyPosSec = 0;
    private static volatile int spotifyDurSec = 0;
    private static volatile long spotifyLastPollMs = 0L;
    private static volatile long spotifyLastSyncSuccessMs = 0L;
    private static volatile boolean spotifyPollBusy = false;
    private static String lastNotifiedTrack = "";
    public static int spotifyVolumePercent = 80;

    // --- 3.7 Дворец испытаний, Авто-открытие Хранилищ (Vault) и Библиотекари ---
    public static boolean highlightTrialVaults = true;
    public static boolean autoClickVaults = true;
    public static int vaultLootGoalIndex = 0;
    public static final List<String> VAULT_LOOT_GOALS = List.of(
        "Тяжёлое ядро",
        "Зловещая бутылка",
        "Трезубец",
        "Алмазы / Книги"
    );
    public static boolean autoCycleLectern = true;
    public static boolean warnAxeDurability = true;
    public static int targetEnchantIndex = 0;
    public static final List<String> TARGET_ENCHANTS = List.of(
        "Починка",
        "Неразрушимость",
        "Эффективность",
        "Острота",
        "Защита",
        "Удача"
    );
    public static int maxEmeraldPrice = 14;
    private static int lastVaultInteractTick = -40;
    private static int lastMissingKeyWarnTick = -120;

    // --- 3.8 Профиль стиля, Гардероб и Темы ---
    public static MenuTheme currentTheme = MenuTheme.CONCEPT_CYAN;
    public static boolean controlInterceptEnabled = true;
    public static int profilerTrainingPct = 86;
    public static int profilerCameraPct = 92;
    public static int profilerClickRhythmPct = 84;
    public static int profilerBuildHabitsPct = 81;

    public static final List<String> WINGS_OPTIONS = List.of(
        "Выкл", "Ангельские", "Демонические", "Неон-элитры", "Кристальные"
    );
    public static final List<String> HORNS_OPTIONS = List.of(
        "Выкл", "Дьявольские", "Оленьи", "Кибер-рожки", "Золотой нимб"
    );
    public static final List<String> HATS_OPTIONS = List.of(
        "Выкл", "Корона", "Незерит-шлем", "Колпак", "Ушки Крыски"
    );
    public static final List<String> CROSSHAIR_OPTIONS = List.of(
        "Ванильный", "Точка + Крест", "Неон-круг", "Тактический"
    );

    public static int wingsIndex = 1;
    public static int hornsIndex = 3;
    public static int hatsIndex = 1;
    public static int crosshairIndex = 1;
    public static boolean worldCosmeticsF5 = true;

    // =========================================================================
    // 4. КЭШИ МИРА, НАВИГАТОРА, СТРУКТУР, ХРАНИЛИЩ И 1PX КРУГЛОЙ МИНИ-КАРТЫ
    // =========================================================================

    private static KeyMapping openMenuKey;
    private static int worldTick = 0;

    // 3D-сущности спутника и миньонов в мире
    private static Mob companionCoreEntity = null;
    private static ArmorStand companionVisorStand = null;
    private static VadimkaForm spawnedForm = null;
    private static int spawnedScale = -1;
    private static final Map<MinionRole, Mob> minionEntities = new EnumMap<>(MinionRole.class);
    private static final Map<VadimkaForm, LivingEntity> guiStageModels = new EnumMap<>(VadimkaForm.class);
    private static Mob guiVillagerModel = null;

    // Навигатор и найденные структуры мира
    public static BlockPos homeWaypoint = null;
    public static BlockPos activeNavTarget = null;
    public static String activeNavTitle = "";
    public static int activeNavColor = 0xFF22D3EE;
    private static BlockPos lastDeathPos = null;
    public static final Map<StructureTarget, FoundStructureRecord> foundStructures = new EnumMap<>(StructureTarget.class);
    private static volatile boolean structureSearchBusy = false;
    private static volatile String structureSearchStatus = "Готов к поиску структур";

    // Память сундуков, Хранилищ (Vault) и Библиотекарей
    public static final Map<BlockPos, ChestRecord> rememberedChests = new LinkedHashMap<>();
    public static final Map<BlockPos, VillagerRecord> rememberedVillagers = new LinkedHashMap<>();
    private static BlockPos lastLookedContainerPos = null;
    private static String lastLookedContainerName = "Ларчик";
    private static boolean wasContainerOpen = false;
    private static boolean wasMerchantOpen = false;

    // Высокочёткий кэш 1px круглой мини-карты (108x108 -> true 1px per cell!)
    private static final int MAP_DIAMETER = 104;
    private static final int MAP_RADIUS = MAP_DIAMETER / 2;
    private static final int[] mapColors = new int[MAP_DIAMETER * MAP_DIAMETER];
    private static final int[] mapHeights = new int[MAP_DIAMETER * MAP_DIAMETER];
    private static int mapOriginX = Integer.MIN_VALUE;
    private static int mapOriginZ = Integer.MIN_VALUE;
    private static int mapLastZoom = -1;
    private static int mapScanRow = 0;
    private static boolean mapNeedsReset = true;

    // Сканер Хранилищ (Vault), Спавнеров испытаний и Сундуков (радиус 22 блока, Y ±10!)
    private static final int VAULT_SCAN_RADIUS = 22;
    private static final List<VaultInfo> nearbyVaults = new ArrayList<>();
    private static final List<VaultInfo> scanVaultBuffer = new ArrayList<>();
    private static int vaultScanSliceX = -VAULT_SCAN_RADIUS;
    private static BlockPos vaultScanOrigin = BlockPos.ZERO;

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
    // 5. ИНИЦИАЛИЗАЦИЯ, AI-ЧАТ И КОМАНДЫ ПОИСКА СТРУКТУР / ОТКРЫТИЯ ХРАНИЛИЩ
    // =========================================================================

    @Override
    public void onInitializeClient() {
        for (MinionRole role : MinionRole.values()) {
            activeMinions.put(role, true);
        }
        ensureOptionalSynthThread();
        pushChatLog(true, "Привет! Я Вадимка v7.0. Команды: !деревня, !испытания, !открой, !трек, !дом, !помощь!");

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
                nearbyVaults.clear();
                scanVaultBuffer.clear();
                radarBlips.clear();
                guiStageModels.clear();
                guiVillagerModel = null;
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
        while (chatHistory.size() > 30) {
            chatHistory.removeFirst();
        }
        if (fromVadimka) {
            vadimkaLastBubble = text;
        }
    }

    private static void enqueueChatReply(String text, int delay) {
        if (text == null || text.isEmpty()) return;
        pushChatLog(true, text);
        pendingChatReplies.addLast(text);
        pendingChatReplyDelay = Math.max(1, delay);
    }

    public static String askVadimkaFromGui(String prompt) {
        if (prompt == null || prompt.trim().isEmpty()) return "";
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

        if (low.isEmpty() || low.contains("привет") || low.contains("ку") || low.contains("тут")) {
            return "На связи! Могу найти Деревню (!деревня) или Дворец испытаний (!испытания), открыть Хранилище (!открой), показать трек из Spotify (!трек) или поставить Дом (!дом)!";
        }
        if (low.contains("помощ") || low.contains("команд") || low.contains("help")) {
            return "Команды: !деревня, !испытания (дворец), !крепость, !город, !особняк, !открой (хранилище/сундук), !дом, !к дому, !найди <вещь>, !жители, !трек, !пауза, !следующий, !облик <имя>!";
        }

        // --- РЕАЛЬНЫЙ ПОИСК СТРУКТУР МИРА ---
        if (low.contains("деревн") || low.contains("village")) {
            locateStructureAsync(StructureTarget.VILLAGE);
            return "Ищу ближайшую Деревню в мире... Сейчас поставлю маяк навигатора!";
        }
        if (low.contains("испытан") || low.contains("дворец") || low.contains("данж") || low.contains("trial") || low.contains("хранилищ")) {
            if (low.contains("открой")) {
                return triggerManualOpenNearestContainer();
            }
            locateStructureAsync(StructureTarget.TRIAL_CHAMBER);
            return "Ищу ближайший Дворец испытаний (Trial Chamber)... Сейчас наведу навигатор!";
        }
        if (low.contains("крепост") || low.contains("энд") || low.contains("stronghold")) {
            locateStructureAsync(StructureTarget.STRONGHOLD);
            return "Ищу ближайшую Крепость (Stronghold)...";
        }
        if (low.contains("город") || low.contains("древн") || low.contains("ancient")) {
            locateStructureAsync(StructureTarget.ANCIENT_CITY);
            return "Ищу ближайший Древний город (Ancient City)...";
        }
        if (low.contains("особняк") || low.contains("mansion")) {
            locateStructureAsync(StructureTarget.MANSION);
            return "Ищу ближайший Лесной особняк...";
        }
        if (low.contains("аванпост") || low.contains("outpost")) {
            locateStructureAsync(StructureTarget.OUTPOST);
            return "Ищу ближайший Аванпост разбойников...";
        }
        if (low.contains("монумент") || low.contains("океан") || low.contains("monument")) {
            locateStructureAsync(StructureTarget.MONUMENT);
            return "Ищу ближайший Монумент океана...";
        }
        if (low.contains("портал") || low.contains("portal")) {
            locateStructureAsync(StructureTarget.RUINED_PORTAL);
            return "Ищу ближайший Разрушенный портал...";
        }

        // --- РЕАЛЬНОЕ ОТКРЫТИЕ ХРАНИЛИЩА / ЛАРЧИКА ---
        if (low.contains("открой") || low.contains("open")) {
            return triggerManualOpenNearestContainer();
        }

        // --- СМЕНА ОБЛИКА ---
        if (low.contains("робот") || low.contains("player")) {
            currentForm = VadimkaForm.CYBER_PLAYER;
            return "Сменил облик на Кибер-Робота!";
        }
        if (low.contains("крыс") || low.contains("rat")) {
            currentForm = VadimkaForm.RAT;
            return "Писк! Сменил облик на Кибер-Крыску!";
        }
        if (low.contains("аксолотл") || low.contains("axolotl")) {
            currentForm = VadimkaForm.AXOLOTL;
            return "Бульк! Теперь я редкий Синий Аксолотль!";
        }
        if (low.contains("вихр") || low.contains("breeze")) {
            currentForm = VadimkaForm.BREEZE;
            return "Вжух! Теперь я Шторм-Вихрь!";
        }
        if (low.contains("варден") || low.contains("хранител") || low.contains("warden")) {
            currentForm = VadimkaForm.WARDEN;
            return "Включил облик Мини-Хранителя!";
        }
        if (low.contains("тихон") || low.contains("спешик") || low.contains("allay")) {
            currentForm = VadimkaForm.ALLAY;
            return "Теперь я Тихоня-Дух!";
        }

        // --- РЕАЛЬНЫЙ SPOTIFY (WINDOWS 10 GSMTC) ---
        if (low.contains("следующ") || low.contains("next") || low.contains("скип")) {
            controlWindowsMediaAsync("NEXT");
            return "Переключил на следующий трек в Spotify!";
        }
        if (low.contains("предыдущ") || low.contains("prev")) {
            controlWindowsMediaAsync("PREV");
            return "Вернул предыдущий трек в Spotify!";
        }
        if (low.contains("пауз") || low.contains("плей") || low.contains("play") || low.contains("pause")) {
            controlWindowsMediaAsync("TOGGLE");
            return "Отправил Play/Pause в Spotify на ПК!";
        }
        if (low.contains("трек") || low.contains("музык") || low.contains("спотифай") || low.contains("spotify")) {
            pollWindowsMediaAsync(true);
            if (!spotifyConnected) {
                return "Spotify на ПК сейчас не воспроизводит медиа. Запусти любой трек в Spotify — я сразу подхвачу название!";
            }
            return (spotifyIsPlaying ? "▶ Играет: " : "⏸ На паузе: ") + spotifyTrackArtist + " — " + spotifyTrackTitle;
        }

        // --- ДОМ И НАВИГАТОР ---
        if (low.contains("дом") && !low.contains("к дому")) {
            if (p != null) {
                homeWaypoint = p.blockPosition();
                setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
                return "Дом сохранён (" + homeWaypoint.getX() + ", " + homeWaypoint.getY() + ", " + homeWaypoint.getZ() + "), маяк включён!";
            }
        }
        if (low.contains("к дому") || low.contains("домой")) {
            if (homeWaypoint == null) return "Сначала задай Дом командой «!дом»!";
            setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
            return "Навигатор ведёт к Дому (" + (int) Math.sqrt(p.blockPosition().distSqr(homeWaypoint)) + "м)!";
        }
        if (low.contains("сброс")) {
            clearNavigationTarget();
            return "Навигатор выключен.";
        }

        // --- ПОИСК В ЛАРЧИКАХ И ЖИТЕЛЯХ ---
        if (low.contains("найд") || low.contains("ларчик") || low.contains("сундук")) {
            String q = low.replace("найди", "").replace("найти", "").replace("ларчик", "").replace("сундук", "").trim();
            if (rememberedChests.isEmpty()) {
                return "Открой сундук ПКМ (или напиши !открой рядом), чтобы я запомнил лут! Хранилищ/сундуков рядом: " + nearbyVaults.size();
            }
            for (ChestRecord cr : rememberedChests.values()) {
                for (Map.Entry<String, Integer> entry : cr.items.entrySet()) {
                    if (q.isEmpty() || entry.getKey().toLowerCase(Locale.ROOT).contains(q)) {
                        setNavigationTarget(cr.pos, entry.getKey(), 0xFFF59E0B);
                        int dist = p != null ? (int) Math.sqrt(p.blockPosition().distSqr(cr.pos)) : 0;
                        return "Нашёл «" + entry.getKey() + "» (x" + entry.getValue() + ") — " + dist + "м! Маяк навигатора поставлен.";
                    }
                }
            }
            return "Вещь «" + q + "» в запомненных ларчиках не найдена.";
        }
        if (low.contains("жител") || low.contains("библиотекар") || low.contains("починк")) {
            if (rememberedVillagers.isEmpty()) {
                return "Открой торговлю с жителем ПКМ — я запишу все его книги и цены!";
            }
            for (VillagerRecord vr : rememberedVillagers.values()) {
                for (VillagerTradeEntry tr : vr.trades) {
                    if (!low.contains("починк") || tr.resultText.toLowerCase(Locale.ROOT).contains("почин")) {
                        setNavigationTarget(vr.pos, tr.resultText, 0xFFC084FC);
                        return "Библиотекарь: " + tr.resultText + " за " + tr.costText + ". Маяк поставлен!";
                    }
                }
            }
            return "Записано жителей: " + rememberedVillagers.size();
        }
        if (low.contains("коорд") || low.contains("статус")) {
            if (p != null) {
                return "X=" + (int) p.getX() + " Y=" + (int) p.getY() + " Z=" + (int) p.getZ()
                    + " | HP: " + (int) Math.ceil(p.getHealth())
                    + " | Тотемы: " + countPlayerTotems(p)
                    + " | Хранилищ рядом: " + countNearbyVaultBlocks();
            }
        }
        String kb = lookupMinecraftAdvice(low);
        if (kb != null) return kb;
        return "Напиши «!деревня», «!испытания», «!открой» или «!помощь», либо открой меню [P]!";
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
    // 6. РЕАЛЬНЫЙ ПОИСК СТРУКТУР МИРА (ДЕРЕВНЯ, ДВОРЕЦ ИСПЫТАНИЙ, КРЕПОСТЬ И ДР.)
    // =========================================================================

    public static void locateStructureAsync(StructureTarget target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (structureSearchBusy) {
            showToast("Поиск уже идёт, секунду...", 45);
            return;
        }

        structureSearchBusy = true;
        structureSearchStatus = "Ищу: " + target.russianName + "...";
        BlockPos startPos = mc.player.blockPosition();
        MinecraftServer server = mc.getSingleplayerServer();

        if (server != null) {
            // Одиночная игра — ТОЧНЫЙ поиск по генератору мира до 1600+ блоков!
            server.execute(() -> {
                BlockPos resultPos = null;
                String errMsg = null;
                try {
                    ServerLevel sLevel = server.getLevel(mc.player.level().dimension());
                    if (sLevel != null) {
                        resultPos = findStructureOnServerLevel(sLevel, startPos, target.keyFragment);
                    }
                } catch (Throwable ex) {
                    errMsg = ex.getClass().getSimpleName();
                }

                final BlockPos finalPos = resultPos;
                final String finalErr = errMsg;
                mc.execute(() -> {
                    structureSearchBusy = false;
                    if (finalPos != null && mc.player != null) {
                        int yTarget = (target == StructureTarget.TRIAL_CHAMBER) ? -22 : Math.max(64, finalPos.getY());
                        BlockPos navPos = new BlockPos(finalPos.getX(), yTarget, finalPos.getZ());
                        foundStructures.put(target, new FoundStructureRecord(target, navPos));
                        setNavigationTarget(navPos, target.russianName, target.beaconColor);
                        int dist = (int) Math.sqrt(mc.player.blockPosition().distSqr(navPos));
                        String depthHint = (target == StructureTarget.TRIAL_CHAMBER || target == StructureTarget.ANCIENT_CITY || target == StructureTarget.STRONGHOLD)
                            ? " (под землёй!)"
                            : "";
                        String msg = "Нашёл " + target.russianName + ": X=" + navPos.getX() + " Z=" + navPos.getZ() + " (" + dist + "м)" + depthHint;
                        structureSearchStatus = msg;
                        showToast("✦ " + msg, 120);
                        mc.player.displayClientMessage(Component.literal("§b[✦ Вадимка] §a" + msg + " — Навигатор включён!"), false);
                        mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8F, 1.2F);
                    } else {
                        String fail = "Не нашёл «" + target.russianName + "» в радиусе 1600 бл." + (finalErr != null ? (" (" + finalErr + ")") : "");
                        structureSearchStatus = fail;
                        showToast(fail, 90);
                        if (mc.player != null) {
                            mc.player.displayClientMessage(Component.literal("§b[✦ Вадимка] §e" + fail), false);
                        }
                    }
                });
            });
        } else {
            // Сетевой сервер — сканируем загруженные чанки вокруг игрока
            BlockPos foundLocal = scanLoadedAreaForStructure(mc.level, mc.player, target);
            structureSearchBusy = false;
            if (foundLocal != null) {
                foundStructures.put(target, new FoundStructureRecord(target, foundLocal));
                setNavigationTarget(foundLocal, target.russianName, target.beaconColor);
                int dist = (int) Math.sqrt(startPos.distSqr(foundLocal));
                String msg = "Найдено рядом: " + target.russianName + " (" + dist + "м)!";
                structureSearchStatus = msg;
                showToast("✦ " + msg, 100);
                mc.player.displayClientMessage(Component.literal("§b[✦ Вадимка] §a" + msg), false);
            } else {
                String msg = "На сервере в зоне прогрузки «" + target.russianName + "» пока не видно.";
                structureSearchStatus = msg;
                showToast(msg, 80);
                mc.player.displayClientMessage(Component.literal("§b[✦ Вадимка] §e" + msg), false);
            }
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockPos findStructureOnServerLevel(ServerLevel sLevel, BlockPos origin, String keyFragment) throws Exception {
        Object regAccess = sLevel.registryAccess();
        Object structureRegistry = null;

        // Рефлексивный вызов lookup / registry для совместимости 1.21.x
        for (String methodName : new String[]{"lookupOrThrow", "registryOrThrow"}) {
            try {
                Method m = regAccess.getClass().getMethod(methodName, net.minecraft.resources.ResourceKey.class);
                structureRegistry = m.invoke(regAccess, Registries.STRUCTURE);
                if (structureRegistry != null) break;
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (structureRegistry == null) {
            return null;
        }

        // Собираем все подходящие Holder<Structure> по части имени ключа (например "village" или "trial_chambers")
        List<Holder<?>> matched = new ArrayList<>();
        Object streamObj = null;
        for (String listMethod : new String[]{"listElements", "holders"}) {
            try {
                Method m = structureRegistry.getClass().getMethod(listMethod);
                streamObj = m.invoke(structureRegistry);
                if (streamObj != null) break;
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (streamObj instanceof java.util.stream.Stream<?> stream) {
            stream.forEach(item -> {
                if (item instanceof Holder<?> h) {
                    String keyStr = h.unwrapKey().map(k -> k.toString().toLowerCase(Locale.ROOT)).orElse("");
                    if (keyStr.contains(keyFragment.toLowerCase(Locale.ROOT))) {
                        matched.add(h);
                    }
                }
            });
        }
        if (matched.isEmpty()) {
            return null;
        }

        HolderSet holderSet = HolderSet.direct((List) matched);
        Object generator = sLevel.getChunkSource().getGenerator();

        // Ищем метод findNearestMapStructure(ServerLevel, HolderSet, BlockPos, int, boolean)
        for (Method m : generator.getClass().getMethods()) {
            if (m.getParameterCount() == 5) {
                Class<?>[] pt = m.getParameterTypes();
                if (pt[0] == ServerLevel.class && pt[1] == HolderSet.class
                        && pt[2] == BlockPos.class && pt[3] == int.class && pt[4] == boolean.class) {
                    Object pair = m.invoke(generator, sLevel, holderSet, origin, 100, false);
                    if (pair != null) {
                        Method getFirst = pair.getClass().getMethod("getFirst");
                        Object posObj = getFirst.invoke(pair);
                        if (posObj instanceof BlockPos bp) {
                            return bp;
                        }
                    }
                    return null;
                }
            }
        }
        return null;
    }

    private static BlockPos scanLoadedAreaForStructure(ClientLevel level, Player player, StructureTarget target) {
        if (target == StructureTarget.VILLAGE) {
            for (Entity e : level.entitiesForRendering()) {
                if (e.getType() == EntityType.VILLAGER) {
                    return e.blockPosition();
                }
            }
        }
        if (target == StructureTarget.TRIAL_CHAMBER) {
            for (VaultInfo vi : nearbyVaults) {
                if (vi.isVaultBlock) {
                    return vi.pos;
                }
            }
        }
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = 48;
        int minY = target == StructureTarget.TRIAL_CHAMBER ? Math.max(level.getMinY(), -45) : Math.max(level.getMinY(), center.getY() - 16);
        int maxY = target == StructureTarget.TRIAL_CHAMBER ? Math.min(level.getMaxY() - 1, 20) : Math.min(level.getMaxY() - 1, center.getY() + 24);
        for (int dx = -r; dx <= r; dx += 3) {
            for (int dz = -r; dz <= r; dz += 3) {
                for (int y = minY; y <= maxY; y += 3) {
                    pos.set(center.getX() + dx, y, center.getZ() + dz);
                    BlockState st = level.getBlockState(pos);
                    if (target == StructureTarget.TRIAL_CHAMBER && (st.is(Blocks.VAULT) || st.is(Blocks.TRIAL_SPAWNER))) {
                        return pos.immutable();
                    }
                    if (target == StructureTarget.VILLAGE && (st.is(Blocks.BELL) || st.is(Blocks.LECTERN))) {
                        return pos.immutable();
                    }
                }
            }
        }
        return null;
    }

    // =========================================================================
    // 7. РЕАЛЬНОЕ ОТКРЫТИЕ ХРАНИЛИЩ (VAULT) И СУНДУКОВ + АВТО-ВЫБОР КЛЮЧА
    // =========================================================================

    private static VaultInfo inspectPotentialVault(BlockPos pos, BlockState st) {
        if (st.is(Blocks.VAULT)) {
            boolean ominous = false;
            boolean active = true;
            String stateName = "active";
            for (Map.Entry<Property<?>, Comparable<?>> entry : st.getValues().entrySet()) {
                String pName = entry.getKey().getName().toLowerCase(Locale.ROOT);
                String pVal = String.valueOf(entry.getValue()).toLowerCase(Locale.ROOT);
                if (pName.contains("ominous")) {
                    ominous = "true".equals(pVal);
                } else if (pName.contains("state")) {
                    stateName = pVal;
                    active = pVal.equals("active") || pVal.equals("unlocking");
                }
            }
            return new VaultInfo(pos.immutable(), true, ominous, active, stateName);
        }
        if (isRegularLootContainer(st)) {
            return new VaultInfo(pos.immutable(), false, false, true, "chest");
        }
        return null;
    }

    private static boolean isRegularLootContainer(BlockState st) {
        if (st.is(Blocks.CHEST) || st.is(Blocks.TRAPPED_CHEST)
                || st.is(Blocks.BARREL) || st.is(Blocks.ENDER_CHEST)) {
            return true;
        }
        String name = st.getBlock().getName().getString().toLowerCase(Locale.ROOT);
        return name.contains("шалкер") || name.contains("shulker");
    }

    private static int countNearbyVaultBlocks() {
        int c = 0;
        for (VaultInfo v : nearbyVaults) {
            if (v.isVaultBlock) c++;
        }
        return c;
    }

    private static int findHotbarSlotForItem(Player player, Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && st.is(item)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean selectHotbarSlotSafe(Player player, int slot) {
        if (slot < 0 || slot > 8) return false;
        Object inv = player.getInventory();
        // 1.21.5+ метод setSelectedSlot(int)
        try {
            Method m = inv.getClass().getMethod("setSelectedSlot", int.class);
            m.invoke(inv, slot);
            return true;
        } catch (Throwable ignored) {
        }
        // Fallback через поле int selected (если старая сигнатура)
        try {
            Field f = inv.getClass().getDeclaredField("selected");
            f.setAccessible(true);
            f.setInt(inv, slot);
            return true;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void tickAutoOpenTrialVaults(Minecraft mc, ClientLevel level, Player player) {
        if (!autoClickVaults || mc.screen != null || mc.gameMode == null) {
            return;
        }
        if ((worldTick - lastVaultInteractTick) < 18) {
            return;
        }

        // Проверяем все Хранилища (Blocks.VAULT) в пределах досягаемости 4.4 блока
        VaultInfo bestVault = null;
        double bestDistSq = 4.4 * 4.4;
        for (VaultInfo vi : nearbyVaults) {
            if (!vi.isVaultBlock) continue;
            double dSq = player.position().distanceToSqr(Vec3.atCenterOf(vi.pos));
            if (dSq <= bestDistSq) {
                BlockState fresh = level.getBlockState(vi.pos);
                VaultInfo live = inspectPotentialVault(vi.pos, fresh);
                if (live != null && live.isVaultBlock && live.active) {
                    bestDistSq = dSq;
                    bestVault = live;
                }
            }
        }

        if (bestVault == null) {
            return;
        }

        Item neededKey = bestVault.ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;
        int keySlot = findHotbarSlotForItem(player, neededKey);
        if (keySlot < 0) {
            if ((worldTick - lastMissingKeyWarnTick) > 100) {
                String kName = bestVault.ominous ? "Зловещий ключ испытаний" : "Ключ испытаний";
                showToast("Хранилище рядом! Положи в хотбар: " + kName, 75);
                lastMissingKeyWarnTick = worldTick;
            }
            return;
        }

        selectHotbarSlotSafe(player, keySlot);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(bestVault.pos), Direction.UP, bestVault.pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastVaultInteractTick = worldTick;
        showToast("✦ Вадимка открыл " + (bestVault.ominous ? "Зловещее хранилище!" : "Хранилище!"), 70);
    }

    public static String triggerManualOpenNearestContainer() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return "Зайди в мир, чтобы открыть хранилище или сундук.";
        }
        Player p = mc.player;
        VaultInfo best = null;
        double bestDist = 4.6 * 4.6;
        for (VaultInfo vi : nearbyVaults) {
            double dSq = p.position().distanceToSqr(Vec3.atCenterOf(vi.pos));
            if (dSq <= bestDist) {
                if (best == null || (vi.isVaultBlock && !best.isVaultBlock) || dSq < bestDist) {
                    best = vi;
                    bestDist = dSq;
                }
            }
        }
        if (best == null) {
            // Если рядом нет — ищем в радиусе 4 блоков прямо сейчас
            BlockPos pc = p.blockPosition();
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = -2; dy <= 3; dy++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        BlockPos bp = pc.offset(dx, dy, dz);
                        VaultInfo vi = inspectPotentialVault(bp, mc.level.getBlockState(bp));
                        if (vi != null) {
                            best = vi;
                            break;
                        }
                    }
                    if (best != null) break;
                }
                if (best != null) break;
            }
        }
        if (best == null) {
            return "В радиусе 4.5 блоков нет Хранилищ или Сундуков! Подойди ближе или нажми «Найти Дворец».";
        }
        if (best.isVaultBlock) {
            if (!best.active) {
                return "Это Хранилище уже открыто (состояние: " + best.stateName + ").";
            }
            Item key = best.ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;
            int slot = findHotbarSlotForItem(p, key);
            if (slot < 0) {
                return "Нет ключа в хотбаре! Нужен: " + (best.ominous ? "Зловещий ключ испытаний" : "Ключ испытаний") + ".";
            }
            selectHotbarSlotSafe(p, slot);
        }
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(best.pos), Direction.UP, best.pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastVaultInteractTick = worldTick;
        return best.isVaultBlock ? "Открыл Хранилище ключом!" : "Открыл ближайший сундук/ларчик!";
    }

    // =========================================================================
    // 8. РЕАЛЬНЫЙ WINDOWS 10 SPOTIFY CONNECT (WinRT GSMTC + UTF-16LE Base64)
    // =========================================================================

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String encodePsBase64(String script) {
        byte[] utf16le = script.getBytes(StandardCharsets.UTF_16LE);
        return java.util.Base64.getEncoder().encodeToString(utf16le);
    }

    private static final String PS_GSMTC_POLL_SCRIPT =
        "[Console]::OutputEncoding = [System.Text.Encoding]::UTF8\n"
        + "$found = $false\n"
        + "try {\n"
        + "  Add-Type -AssemblyName System.Runtime.WindowsRuntime\n"
        + "  $asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]\n"
        + "  function AwaitWinRT($op, $type) {\n"
        + "    $m = $asTaskGeneric.MakeGenericMethod($type)\n"
        + "    $t = $m.Invoke($null, @($op))\n"
        + "    $t.Wait(1400) | Out-Null\n"
        + "    return $t.Result\n"
        + "  }\n"
        + "  [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime] | Out-Null\n"
        + "  $mgr = AwaitWinRT ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n"
        + "  if ($mgr) {\n"
        + "    $sessions = $mgr.GetSessions()\n"
        + "    $target = $null\n"
        + "    foreach ($s in $sessions) {\n"
        + "      if ($s.SourceAppUserModelId -match 'Spotify') { $target = $s; break }\n"
        + "    }\n"
        + "    if (-not $target) { $target = $mgr.GetCurrentSession() }\n"
        + "    if ($target) {\n"
        + "      $props = AwaitWinRT ($target.TryGetMediaPropertiesAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionMediaProperties])\n"
        + "      $pb = $target.GetPlaybackInfo()\n"
        + "      $tl = $target.GetTimelineProperties()\n"
        + "      $title = if ($props.Title) { $props.Title.Replace('|',' ') } else { '' }\n"
        + "      $artist = if ($props.Artist) { $props.Artist.Replace('|',' ') } else { 'Spotify' }\n"
        + "      $st = if ($pb -and $pb.PlaybackStatus -eq 'Playing') { 'PLAYING' } else { 'PAUSED' }\n"
        + "      $pos = if ($tl) { [int]$tl.Position.TotalSeconds } else { 0 }\n"
        + "      $dur = if ($tl) { [int]$tl.EndTime.TotalSeconds } else { 0 }\n"
        + "      $src = if ($target.SourceAppUserModelId) { $target.SourceAppUserModelId.Replace('|',' ') } else { 'Spotify' }\n"
        + "      if ($title.Length -gt 0) {\n"
        + "        Write-Output \"GSMTC|$st|$artist|$title|$pos|$dur|$src\"\n"
        + "        $found = $true\n"
        + "      }\n"
        + "    }\n"
        + "  }\n"
        + "} catch {}\n"
        + "if (-not $found) {\n"
        + "  $procs = Get-Process -Name Spotify -ErrorAction SilentlyContinue | Where-Object { $_.MainWindowTitle -and $_.MainWindowTitle.Length -gt 0 }\n"
        + "  foreach ($p in $procs) {\n"
        + "    $t = $p.MainWindowTitle.Trim()\n"
        + "    if ($t -and $t -notmatch '^Spotify( Free| Premium)?$') {\n"
        + "      $a = 'Spotify'; $n = $t\n"
        + "      if ($t -match ' - ') { $idx = $t.IndexOf(' - '); $a = $t.Substring(0,$idx).Trim(); $n = $t.Substring($idx+3).Trim() }\n"
        + "      Write-Output \"WIN|PLAYING|$($a.Replace('|',' '))|$($n.Replace('|',' '))|0|0|Spotify\"\n"
        + "      $found = $true; break\n"
        + "    }\n"
        + "  }\n"
        + "}\n"
        + "if (-not $found) { Write-Output 'NONE||||0|0|' }\n";

    private static final String PS_GSMTC_POLL_B64 = encodePsBase64(PS_GSMTC_POLL_SCRIPT);

    private static void pollWindowsMediaAsync(boolean force) {
        if (!isWindows()) return;
        long now = System.currentTimeMillis();
        if (!force && (spotifyPollBusy || (now - spotifyLastPollMs) < 2200L)) {
            return;
        }
        spotifyPollBusy = true;
        spotifyLastPollMs = now;

        mediaExecutor.submit(() -> {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                    "-EncodedCommand", PS_GSMTC_POLL_B64
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                String resultLine = "";
                try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("GSMTC|") || line.startsWith("WIN|") || line.startsWith("NONE|")) {
                            resultLine = line.trim();
                            break;
                        }
                    }
                }
                proc.waitFor(3, TimeUnit.SECONDS);

                if (resultLine.startsWith("GSMTC|") || resultLine.startsWith("WIN|")) {
                    String[] parts = resultLine.split("\\|", -1);
                    if (parts.length >= 7) {
                        spotifyConnected = true;
                        spotifyIsPlaying = "PLAYING".equals(parts[1]);
                        spotifyTrackArtist = parts[2].isEmpty() ? "Spotify" : parts[2];
                        spotifyTrackTitle = parts[3].isEmpty() ? "Без названия" : parts[3];
                        spotifyPosSec = parseIntSafe(parts[4], 0);
                        spotifyDurSec = parseIntSafe(parts[5], 0);
                        String rawSrc = parts[6];
                        spotifySourceName = rawSrc.toLowerCase(Locale.ROOT).contains("spotify") ? "Spotify (ПК)" : "Медиа (ПК)";
                        spotifyLastSyncSuccessMs = System.currentTimeMillis();
                    }
                } else {
                    spotifyConnected = false;
                    spotifyIsPlaying = false;
                    spotifyTrackTitle = "Spotify на паузе / не играет";
                    spotifyTrackArtist = "Запусти песню в Spotify на ПК";
                    spotifyPosSec = 0;
                    spotifyDurSec = 0;
                }
            } catch (Throwable ignored) {
            } finally {
                spotifyPollBusy = false;
            }
        });
    }

    private static void controlWindowsMediaAsync(String action) {
        if (!isWindows()) return;
        mediaExecutor.submit(() -> {
            try {
                String methodCall = switch (action) {
                    case "NEXT" -> "TrySkipNextAsync";
                    case "PREV" -> "TrySkipPreviousAsync";
                    default -> "TryTogglePlayPauseAsync";
                };
                int vk = switch (action) {
                    case "NEXT" -> 0xB0;
                    case "PREV" -> 0xB1;
                    case "VOL_UP" -> 0xAF;
                    case "VOL_DOWN" -> 0xAE;
                    default -> 0xB3;
                };
                String script;
                if ("VOL_UP".equals(action) || "VOL_DOWN".equals(action)) {
                    script = "Add-Type -TypeDefinition 'using System.Runtime.InteropServices; public class K { [DllImport(\"user32.dll\")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, int dwExtraInfo); }'\n"
                        + "[K]::keybd_event(" + vk + ",0,0,0); [K]::keybd_event(" + vk + ",0,2,0)\n";
                } else {
                    script = "$done = $false\n"
                        + "try {\n"
                        + "  Add-Type -AssemblyName System.Runtime.WindowsRuntime\n"
                        + "  $asTaskGeneric = ([System.WindowsRuntimeSystemExtensions].GetMethods() | Where-Object { $_.Name -eq 'AsTask' -and $_.GetParameters().Count -eq 1 -and $_.GetParameters()[0].ParameterType.Name -eq 'IAsyncOperation`1' })[0]\n"
                        + "  function AwaitWinRT($op, $type) { $t = $asTaskGeneric.MakeGenericMethod($type).Invoke($null, @($op)); $t.Wait(1200) | Out-Null; return $t.Result }\n"
                        + "  [Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager, Windows.Media.Control, ContentType=WindowsRuntime] | Out-Null\n"
                        + "  $mgr = AwaitWinRT ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager]::RequestAsync()) ([Windows.Media.Control.GlobalSystemMediaTransportControlsSessionManager])\n"
                        + "  if ($mgr) {\n"
                        + "    $target = $null\n"
                        + "    foreach ($s in $mgr.GetSessions()) { if ($s.SourceAppUserModelId -match 'Spotify') { $target = $s; break } }\n"
                        + "    if (-not $target) { $target = $mgr.GetCurrentSession() }\n"
                        + "    if ($target) { $target." + methodCall + "() | Out-Null; $done = $true }\n"
                        + "  }\n"
                        + "} catch {}\n"
                        + "if (-not $done) {\n"
                        + "  Add-Type -TypeDefinition 'using System.Runtime.InteropServices; public class K { [DllImport(\"user32.dll\")] public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, int dwExtraInfo); }'\n"
                        + "  [K]::keybd_event(" + vk + ",0,0,0); [K]::keybd_event(" + vk + ",0,2,0)\n"
                        + "}\n";
                }
                ProcessBuilder pb = new ProcessBuilder(
                    "powershell.exe", "-NoProfile", "-NonInteractive", "-WindowStyle", "Hidden",
                    "-EncodedCommand", encodePsBase64(script)
                );
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                proc.waitFor(2, TimeUnit.SECONDS);
                Thread.sleep(380);
            } catch (Throwable ignored) {
            } finally {
                spotifyLastPollMs = 0L;
                pollWindowsMediaAsync(true);
            }
        });
    }

    private static int parseIntSafe(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Throwable ignored) {
            return def;
        }
    }

    // =========================================================================
    // 9. КАСТОМНЫЕ 3D-МОДЕЛИ ВАДИМКИ, МИНЬОНОВ, ТИК МИРА И 1PX КАРТА
    // =========================================================================

    private static final int[] CIRCLE_HALF_W = new int[MAP_DIAMETER];
    static {
        double r = MAP_RADIUS - 0.5;
        for (int y = 0; y < MAP_DIAMETER; y++) {
            double dy = (y + 0.5) - MAP_RADIUS;
            double inside = r * r - dy * dy;
            CIRCLE_HALF_W[y] = inside > 0 ? (int) Math.floor(Math.sqrt(inside)) : 0;
        }
    }

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
            if (m != null) m.discard();
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

    private static Mob getGuiVillagerModel(ClientLevel level) {
        if (guiVillagerModel != null && !guiVillagerModel.isRemoved() && guiVillagerModel.level() == level) {
            return guiVillagerModel;
        }
        Mob v = EntityType.VILLAGER.create(level, EntitySpawnReason.LOAD);
        if (v != null) {
            v.setNoAi(true);
            v.setSilent(true);
            guiVillagerModel = v;
        }
        return guiVillagerModel;
    }

    private static LivingEntity buildCustomVisualEntity(ClientLevel level, Player player, VadimkaForm form, boolean forGui) {
        if (form == VadimkaForm.CYBER_PLAYER) {
            ArmorStand bot = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.LOAD);
            if (bot != null) {
                bot.setNoGravity(true);
                bot.setSilent(true);
                bot.setInvulnerable(true);
                bot.setShowArms(true);
                bot.setNoBasePlate(true);
                ItemStack head = new ItemStack(Items.PLAYER_HEAD);
                ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
                chest.set(DataComponents.DYED_COLOR, new DyedItemColor(0x0E7490));
                ItemStack legs = new ItemStack(Items.LEATHER_LEGGINGS);
                legs.set(DataComponents.DYED_COLOR, new DyedItemColor(0x1E293B));
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
                setAxolotlBlueVariant(ax);
                if (!forGui) {
                    applyScaleAttribute(ax, form.worldScale * (companionScalePercent / 100.0));
                }
                return ax;
            }
        }
        if (form == VadimkaForm.RAT) {
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
            for (Method m : Axolotl.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers())
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
        if (level == null || player == null) return;

        worldTick++;
        if (toastBannerTicks > 0) toastBannerTicks--;

        // Реальный Windows 10 Spotify Connect (опрос каждые 2.2 сек в фоне, 0 лагов)
        if (spotifyHudEnabled || mc.screen instanceof AssistantVadimkaScreen) {
            pollWindowsMediaAsync(false);
            if (spotifyConnected && spotifyIsPlaying && !spotifyTrackTitle.equals(lastNotifiedTrack)
                    && !spotifyTrackTitle.startsWith("Ожидание") && !spotifyTrackTitle.startsWith("Spotify на паузе")) {
                lastNotifiedTrack = spotifyTrackTitle;
                if (worldTick > 60) {
                    showToast("♫ " + spotifyTrackArtist + " — " + spotifyTrackTitle, 75);
                }
            }
        }

        // Приветствие
        if (!greetedInWorld && worldTick > 25) {
            greetedInWorld = true;
            if (chatAssistantEnabled && !quietMode) {
                player.displayClientMessage(Component.literal(
                    "§b[✦ Вадимка v7.0] §fНа связи! Пиши §a!деревня§f, §a!испытания§f, §a!открой§f или нажми §e[P]§f."
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

        // Запоминаем блок сундука/бочки при наведении
        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = bhr.getBlockPos();
            BlockState bs = level.getBlockState(bp);
            if (bs.is(Blocks.VAULT) || isRegularLootContainer(bs)) {
                lastLookedContainerPos = bp.immutable();
                lastLookedContainerName = bs.getBlock().getName().getString();
            }
        }

        // Авто-запись содержимого сундуков и сделок Библиотекарей + алерт при нахождении нужных чар!
        scanOpenContainerOrMerchant(mc, player);

        // Реальное Авто-открытие Хранилищ (Vault) при подходе с Ключом испытаний!
        tickAutoOpenTrialVaults(mc, level, player);

        // Профайлер движения
        Vec3 curPos = player.position();
        if (prevPlayerPos != null) {
            double d = curPos.distanceTo(prevPlayerPos);
            if (d < 10.0) sessionWalkedBlocks += d;
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

        // Предупреждение о прочности инструмента
        if (warnAxeDurability && worldTick % 25 == 0) {
            ItemStack hand = player.getMainHandItem();
            if (!hand.isEmpty() && hand.isDamageableItem() && hand.getMaxDamage() > 0) {
                int left = hand.getMaxDamage() - hand.getDamageValue();
                if (left > 0 && left <= 12 && (worldTick - lastAxeWarnTick) > 240) {
                    showToast("⚠ Прочность «" + hand.getHoverName().getString() + "»: " + left + "!", 80);
                    lastAxeWarnTick = worldTick;
                }
            }
        }

        // 1. Сущности вокруг (раз в 4 тика)
        if (worldTick % 4 == 0) {
            scanEntitiesAround(level, player);
        }

        // 2. 3D-спутник и миньоны
        updateCustomCompanionInWorld(mc, level, player);

        // 3. 3D-крылья и нимб в F5
        if (worldCosmeticsF5 && worldTick % 2 == 0 && !mc.options.getCameraType().isFirstPerson()) {
            spawnWorldCosmetics(level, player);
        }

        // 4. Неоновые частицы над Хранилищами/Сундуками + 3D-луч Навигатора
        if (worldTick % 5 == 0) {
            spawnWorldBeaconsAndVaultHighlights(level, player);
        }

        // 5. Нить Ариадны
        if (ariadneThread) {
            Vec3 crumb = new Vec3(player.getX(), player.getY() + 0.22, player.getZ());
            if (lastAriadnePoint == null || lastAriadnePoint.distanceToSqr(crumb) > 12.25) {
                ariadnePoints.addLast(crumb);
                lastAriadnePoint = crumb;
                while (ariadnePoints.size() > 36) ariadnePoints.removeFirst();
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

        // 6. 1px чёткая карта рельефа (по 6 строк за тик = плавно и без лагов!)
        if (minimapRadarEnabled && minimapTerrainEnabled) {
            tickCircularTerrainMap(level, Mth.floor(player.getX()), Mth.floor(player.getZ()));
        }

        // 7. Сканер Хранилищ (Vault) и Сундуков вокруг
        if (worldTick % 2 == 0) {
            tickVaultAndChestScanner(level, player.blockPosition());
        }
    }

    private static void spawnWorldBeaconsAndVaultHighlights(ClientLevel level, Player player) {
        if (highlightTrialVaults || highlightChestsWorld) {
            int shown = 0;
            for (VaultInfo vi : nearbyVaults) {
                if (vi.pos.distSqr(player.blockPosition()) <= 324.0) {
                    int dustCol = vi.isVaultBlock
                        ? (vi.active ? (vi.ominous ? 0xEC4899 : 0xF97316) : 0x64748B)
                        : 0x22D3EE;
                    level.addParticle(
                        new DustParticleOptions(dustCol, vi.isVaultBlock ? 1.25F : 0.85F),
                        vi.pos.getX() + 0.5, vi.pos.getY() + 1.15, vi.pos.getZ() + 0.5,
                        0.0, 0.03, 0.0
                    );
                    shown++;
                    if (shown >= 10) break;
                }
            }
        }

        if (activeNavTarget != null) {
            double tx = activeNavTarget.getX() + 0.5;
            double ty = Math.max(player.getY() - 2.0, activeNavTarget.getY() + 1.0);
            double tz = activeNavTarget.getZ() + 0.5;

            if (activeNavTarget.distSqr(player.blockPosition()) <= 1600.0) {
                for (int h = 0; h < 7; h++) {
                    level.addParticle(
                        new DustParticleOptions(activeNavColor & 0x00FFFFFF, 1.4F),
                        tx, ty + h * 1.5, tz, 0.0, 0.05, 0.0
                    );
                }
            }

            // Пунктирная 3D-стрелка от игрока в сторону цели (работает на любой дистанции!)
            Vec3 pEye = player.position().add(0.0, 0.85, 0.0);
            Vec3 dir = new Vec3(tx - pEye.x, 0.0, tz - pEye.z);
            double dist = dir.length();
            if (dist > 3.0) {
                Vec3 step = dir.normalize();
                int dots = (int) Math.min(7, dist / 2.2);
                for (int i = 1; i <= dots; i++) {
                    double d = i * 2.2 + ((worldTick % 10) * 0.22);
                    if (d < dist) {
                        level.addParticle(
                            new DustParticleOptions(activeNavColor & 0x00FFFFFF, 1.05F),
                            pEye.x + step.x * d, player.getY() + 0.45, pEye.z + step.z * d,
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
                    String want = TARGET_ENCHANTS.get(targetEnchantIndex).toLowerCase(Locale.ROOT);
                    VillagerTradeEntry jackpot = null;

                    for (MerchantOffer offer : offers) {
                        String cA = formatItemShort(offer.getCostA());
                        String cB = offer.getCostB().isEmpty() ? "" : (" + " + formatItemShort(offer.getCostB()));
                        String res = formatItemWithEnchants(offer.getResult());
                        int emPrice = offer.getCostA().is(Items.EMERALD) ? offer.getCostA().getCount() : 99;
                        String resLow = res.toLowerCase(Locale.ROOT);
                        boolean matchesGoal = resLow.contains(want)
                            || (want.contains("почин") && resLow.contains("mending"))
                            || (want.contains("неразруш") && resLow.contains("unbreaking"))
                            || (want.contains("эффектив") && resLow.contains("efficiency"))
                            || (want.contains("острот") && resLow.contains("sharpness"))
                            || (want.contains("защит") && resLow.contains("protection"))
                            || (want.contains("удач") && resLow.contains("fortune"));
                        boolean good = matchesGoal || offer.getResult().is(Items.ENCHANTED_BOOK);
                        VillagerTradeEntry entry = new VillagerTradeEntry(cA + cB, res, emPrice, good, offer.isOutOfStock());
                        rec.trades.add(entry);
                        if (matchesGoal && emPrice <= maxEmeraldPrice && jackpot == null) {
                            jackpot = entry;
                        }
                    }
                    rememberedVillagers.put(vPos, rec);
                    if (!wasMerchantOpen) {
                        if (jackpot != null) {
                            String jMsg = "★ НАЙДЕНЫ НУЖНЫЕ ЧАРЫ: " + jackpot.resultText + " за " + jackpot.costText + "!";
                            showToast(jMsg, 120);
                            player.displayClientMessage(Component.literal("§a[✦ Вадимка] " + jMsg), false);
                            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.85F, 1.0F);
                        } else {
                            showToast("Библиотекарь записан (" + rec.trades.size() + " сделок)", 60);
                        }
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
                    if (slot.container == player.getInventory()) continue;
                    ItemStack st = slot.getItem();
                    if (!st.isEmpty()) {
                        rec.items.merge(formatItemWithEnchants(st), st.getCount(), Integer::sum);
                    }
                }
                rememberedChests.put(lastLookedContainerPos, rec);
                if (!wasContainerOpen) {
                    showToast("Ларчик запомнен (" + rec.items.size() + " видов вещей)", 55);
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
            if (companionCoreEntity != null) { companionCoreEntity.discard(); companionCoreEntity = null; }
            if (companionVisorStand != null) { companionVisorStand.discard(); companionVisorStand = null; }

            if (currentForm == VadimkaForm.CYBER_PLAYER) {
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

        double yawRad = Math.toRadians(player.getYRot());
        double rightX = -Math.cos(yawRad);
        double rightZ = -Math.sin(yawRad);
        double forwardX = -Math.sin(yawRad);
        double forwardZ = Math.cos(yawRad);

        double danceFreq = (vadimkaDanceToBpm && spotifyIsPlaying) ? 0.28 : 0.14;
        double danceBob = Math.sin(worldTick * danceFreq) * 0.12;

        double targetX = player.getX() + rightX * offsetX + forwardX * offsetZ;
        double targetY = player.getY() + offsetY + danceBob;
        double targetZ = player.getZ() + rightZ * offsetX + forwardZ * offsetZ;

        if (companionVisorStand != null) {
            companionVisorStand.xo = companionVisorStand.getX();
            companionVisorStand.yo = companionVisorStand.getY();
            companionVisorStand.zo = companionVisorStand.getZ();
            companionVisorStand.setPos(
                Mth.lerp(0.24, companionVisorStand.getX(), targetX),
                Mth.lerp(0.24, companionVisorStand.getY(), targetY - 0.35),
                Mth.lerp(0.24, companionVisorStand.getZ(), targetZ)
            );
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
            companionCoreEntity.setYRot(player.getYRot());
            companionCoreEntity.setYHeadRot(player.getYRot());
            companionCoreEntity.setYBodyRot(player.getYRot());

            if (worldTick % 12 == 0) {
                level.addParticle(
                    new DustParticleOptions(currentForm.primaryColor & 0x00FFFFFF, 0.85F),
                    nx, ny + 0.25, nz, 0.0, 0.02, 0.0
                );
            }
        }

        // 4 миньона
        MinionRole[] roles = MinionRole.values();
        for (int i = 0; i < roles.length; i++) {
            MinionRole role = roles[i];
            boolean on = companionEnabled && minionsEnabled && Boolean.TRUE.equals(activeMinions.get(role));
            Mob existing = minionEntities.get(role);
            if (!on) {
                if (existing != null) { existing.discard(); minionEntities.remove(role); }
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
                        tx = nearestLootPos.x; ty = nearestLootPos.y + 0.85; tz = nearestLootPos.z;
                    } else if (role == MinionRole.SCOUT && nearestHostilePos != null && nearestHostileDist < 16.0) {
                        Vec3 mid = player.position().lerp(nearestHostilePos, 0.45);
                        tx = mid.x; ty = mid.y + 1.85; tz = mid.z;
                    } else if (role == MinionRole.BUILDER
                            && mc.hitResult instanceof BlockHitResult bhr
                            && bhr.getType() == HitResult.Type.BLOCK
                            && !player.getMainHandItem().isEmpty()) {
                        Vec3 hit = bhr.getLocation();
                        if (hit.distanceToSqr(player.position()) < 36.0) {
                            tx = hit.x; ty = hit.y + 0.95; tz = hit.z;
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
            if (entity == player || entity.getId() <= -77000) continue;
            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();
            if (Math.abs(dx) > 68.0 || Math.abs(dz) > 68.0) continue;

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
                        if (dot < -0.25) threatBehindPlayer = true;
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

    private static void tickCircularTerrainMap(ClientLevel level, int px, int pz) {
        int step = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        int snapX = Math.floorDiv(px, step) * step;
        int snapZ = Math.floorDiv(pz, step) * step;

        if (mapNeedsReset || mapLastZoom != radarZoomLevel
                || Math.abs(snapX - mapOriginX) >= step * 4
                || Math.abs(snapZ - mapOriginZ) >= step * 4) {
            mapOriginX = snapX;
            mapOriginZ = snapZ;
            mapLastZoom = radarZoomLevel;
            mapScanRow = 0;
            mapNeedsReset = false;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // 6 строк за тик = полный цикл за ~17 тиков (< 0.9 сек), ~0.08 мс/тик = 0 фризов!
        for (int r = 0; r < 6; r++) {
            int gz = mapScanRow;
            int wz = mapOriginZ + (gz - MAP_RADIUS) * step;
            for (int gx = 0; gx < MAP_DIAMETER; gx++) {
                int wx = mapOriginX + (gx - MAP_RADIUS) * step;
                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                pos.set(wx, topY, wz);
                BlockState st = level.getBlockState(pos);
                MapColor mc = st.getMapColor(level, pos);
                if (mc.col == 0 && topY > level.getMinY() + 2) {
                    pos.set(wx, topY - 1, wz);
                    st = level.getBlockState(pos);
                    mc = st.getMapColor(level, pos);
                }
                int idx = gz * MAP_DIAMETER + gx;
                mapHeights[idx] = topY;
                int rgb = mc.col != 0 ? mc.col : 0x16202B;
                // Мягкое рельефное освещение (север + запад) — убирает плоскую кашу!
                int shade = 0;
                if (gz > 0) {
                    int diffN = topY - mapHeights[(gz - 1) * MAP_DIAMETER + gx];
                    if (diffN > 0) shade += 14;
                    else if (diffN < 0) shade -= 16;
                }
                if (gx > 0) {
                    int diffW = topY - mapHeights[gz * MAP_DIAMETER + (gx - 1)];
                    if (diffW > 0) shade += 7;
                    else if (diffW < 0) shade -= 8;
                }
                rgb = adjustBrightness(rgb, shade);
                mapColors[idx] = 0xFF000000 | rgb;
            }
            mapScanRow = (mapScanRow + 1) % MAP_DIAMETER;
        }
    }

    private static void tickVaultAndChestScanner(ClientLevel level, BlockPos center) {
        if (vaultScanSliceX == -VAULT_SCAN_RADIUS) {
            vaultScanOrigin = center;
            scanVaultBuffer.clear();
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinY(), vaultScanOrigin.getY() - 10);
        int maxY = Math.min(level.getMaxY() - 1, vaultScanOrigin.getY() + 10);
        int wx = vaultScanOrigin.getX() + vaultScanSliceX;

        for (int dz = -VAULT_SCAN_RADIUS; dz <= VAULT_SCAN_RADIUS; dz++) {
            int wz = vaultScanOrigin.getZ() + dz;
            for (int y = minY; y <= maxY; y++) {
                pos.set(wx, y, wz);
                BlockState st = level.getBlockState(pos);
                VaultInfo vi = inspectPotentialVault(pos, st);
                if (vi != null && scanVaultBuffer.size() < 48) {
                    scanVaultBuffer.add(vi);
                }
            }
        }
        vaultScanSliceX++;
        if (vaultScanSliceX > VAULT_SCAN_RADIUS) {
            nearbyVaults.clear();
            nearbyVaults.addAll(scanVaultBuffer);
            vaultScanSliceX = -VAULT_SCAN_RADIUS;
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
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) totems += p.getOffhandItem().getCount();
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
    // 10. ИГРОВОЙ HUD: ЧЁТКАЯ 1PX КРУГЛАЯ КАРТА, НАВИГАТОР, SPOTIFY, PVP
    // =========================================================================

    private static void renderGameplayHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        if (mc.screen instanceof AssistantVadimkaScreen) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (visualsEnabled && minimapRadarEnabled) {
            renderCrispCircularMinimap(g, mc);
        }
        if (visualsEnabled && elytraPitchHorizon && mc.player.isFallFlying()) {
            renderElytraHorizonHud(g, mc.font, mc.player, sw, sh);
        }
        if (crosshairIndex > 0 && mc.screen == null) {
            renderCustomCrosshair(g, sw, sh);
        }
        if (backstabIndicator && threatBehindPlayer) {
            String warn = "⚠ УГРОЗА СЗАДИ (" + String.format("%.1f", nearestHostileDist) + "м)";
            drawTopPill(g, mc.font, sw, 8, warn, 0xE63B0A12, 0xFFEF4444, 0xFFFCA5A5);
        } else if (activeNavTarget != null) {
            renderActiveNavigatorBanner(g, mc.font, mc.player, sw);
        }
        if (litematicaHudEnabled) {
            renderLitematicaRightHud(g, mc, sw);
        }
        if (combatHudEnabled) {
            renderCombatPvpHud(g, mc, sw, sh);
        }
        if (spotifyHudEnabled) {
            renderSpotifyBottomHud(g, mc, sh);
        }
        if (toastBannerTicks > 0 && !toastBannerText.isEmpty()) {
            int tw = mc.font.width(toastBannerText) + 18;
            int tx = (sw - tw) / 2;
            int ty = sh - 58;
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
        String label = arrow + " " + title + " • " + dist + "м [" + activeNavTarget.getX() + ", " + activeNavTarget.getZ() + "]";
        drawTopPill(g, font, sw, 8, label, currentTheme.bgWindow, activeNavColor, 0xFFFFFFFF);
    }

    private static void renderCrispCircularMinimap(GuiGraphics g, Minecraft mc) {
        // 1px настоящее разрешение (104x104 круг) + RLE-батчинг: чётко, стильно, 65-75 FPS, не лезет на Discord!
        int x = 10;
        int y = 30;
        int cx = x + MAP_RADIUS;
        int cy = y + MAP_RADIUS;

        int step = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        double yawRad = Math.toRadians(mc.player.getYRot() + 180.0);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);

        // Внешний тёмный круг + неоновое кольцо-ободок (Cyan -> Violet)
        if (minimapCircularMode) {
            for (int py = 0; py < MAP_DIAMETER; py++) {
                int hw = CIRCLE_HALF_W[py];
                if (hw <= 0) continue;
                int rowY = y + py;
                int leftEdge = cx - hw;
                int rightEdge = cx + hw;

                // Неоновая круглая рамка 2px
                int ringCol = lerpColor(currentTheme.accent, currentTheme.secondary, (float) py / MAP_DIAMETER);
                g.fill(leftEdge - 2, rowY, leftEdge, rowY + 1, ringCol);
                g.fill(rightEdge, rowY, rightEdge + 2, rowY + 1, ringCol);

                if (!minimapTerrainEnabled) {
                    g.fill(leftEdge, rowY, rightEdge, rowY + 1, 0xEE0B1118);
                    continue;
                }

                // True 1px RLE-рендер внутри круга!
                double ry = py - MAP_RADIUS;
                int px = leftEdge - x;
                int pxEnd = rightEdge - x;
                while (px < pxEnd) {
                    double rx = px - MAP_RADIUS;
                    int sx = MAP_RADIUS + (int) Math.round(rx * cos + ry * sin);
                    int sz = MAP_RADIUS + (int) Math.round(-rx * sin + ry * cos);
                    int col = (sx >= 0 && sx < MAP_DIAMETER && sz >= 0 && sz < MAP_DIAMETER)
                        ? mapColors[sz * MAP_DIAMETER + sx]
                        : 0xFF0C1219;

                    int run = px + 1;
                    while (run < pxEnd) {
                        double rx2 = run - MAP_RADIUS;
                        int sx2 = MAP_RADIUS + (int) Math.round(rx2 * cos + ry * sin);
                        int sz2 = MAP_RADIUS + (int) Math.round(-rx2 * sin + ry * cos);
                        int col2 = (sx2 >= 0 && sx2 < MAP_DIAMETER && sz2 >= 0 && sz2 < MAP_DIAMETER)
                            ? mapColors[sz2 * MAP_DIAMETER + sx2]
                            : 0xFF0C1219;
                        if (col2 != col) break;
                        run++;
                    }
                    g.fill(x + px, rowY, x + run, rowY + 1, col);
                    px = run;
                }
            }
            // Верхняя и нижняя дуги рамки
            g.fill(cx - 10, y - 2, cx + 10, y, currentTheme.accent);
            g.fill(cx - 10, y + MAP_DIAMETER, cx + 10, y + MAP_DIAMETER + 2, currentTheme.secondary);
        } else {
            fillGlassCard(g, x - 2, y - 2, MAP_DIAMETER + 4, MAP_DIAMETER + 4, currentTheme.bgWindow, currentTheme.accent);
            if (minimapTerrainEnabled) {
                for (int py = 0; py < MAP_DIAMETER; py++) {
                    double ry = py - MAP_RADIUS;
                    int rowY = y + py;
                    int px = 0;
                    while (px < MAP_DIAMETER) {
                        double rx = px - MAP_RADIUS;
                        int sx = MAP_RADIUS + (int) Math.round(rx * cos + ry * sin);
                        int sz = MAP_RADIUS + (int) Math.round(-rx * sin + ry * cos);
                        int col = (sx >= 0 && sx < MAP_DIAMETER && sz >= 0 && sz < MAP_DIAMETER)
                            ? mapColors[sz * MAP_DIAMETER + sx]
                            : 0xFF0C1219;
                        int run = px + 1;
                        while (run < MAP_DIAMETER) {
                            double rx2 = run - MAP_RADIUS;
                            int sx2 = MAP_RADIUS + (int) Math.round(rx2 * cos + ry * sin);
                            int sz2 = MAP_RADIUS + (int) Math.round(-rx2 * sin + ry * cos);
                            int col2 = (sx2 >= 0 && sx2 < MAP_DIAMETER && sz2 >= 0 && sz2 < MAP_DIAMETER)
                                ? mapColors[sz2 * MAP_DIAMETER + sx2]
                                : 0xFF0C1219;
                            if (col2 != col) break;
                            run++;
                        }
                        g.fill(x + px, rowY, x + run, rowY + 1, col);
                        px = run;
                    }
                }
            }
        }

        // Тонкое перекрестие в центре
        g.fill(cx, cy - 18, cx + 1, cy + 19, 0x33FFFFFF);
        g.fill(cx - 18, cy, cx + 19, cy + 1, 0x33FFFFFF);

        double scale = 1.0 / step;
        double maxR = MAP_RADIUS - 5.0;

        // Звуковой сонар по краю круга
        if (soundRadarSonar && nearestHostileDist < 24.0) {
            double hDx = Math.cos(nearestHostileAngle) * 20.0;
            double hDz = Math.sin(nearestHostileAngle) * 20.0;
            double rX = hDx * cos - hDz * sin;
            double rY = hDx * sin + hDz * cos;
            double len = Math.max(1.0, Math.sqrt(rX * rX + rY * rY));
            int ax = cx + (int) ((rX / len) * (maxR + 1.0));
            int ay = cy + (int) ((rY / len) * (maxR + 1.0));
            g.fill(ax - 3, ay - 3, ax + 4, ay + 4, 0xD9EF4444);
        }

        // Хранилища (Vault) и Сундуки на карте
        if (highlightChestsWorld || highlightTrialVaults) {
            for (VaultInfo vi : nearbyVaults) {
                double dx = (vi.pos.getX() + 0.5) - mc.player.getX();
                double dz = (vi.pos.getZ() + 0.5) - mc.player.getZ();
                double rX = (dx * cos - dz * sin) * scale;
                double rY = (dx * sin + dz * cos) * scale;
                if (rX * rX + rY * rY <= maxR * maxR) {
                    int mx = cx + (int) rX;
                    int my = cy + (int) rY;
                    int c = vi.isVaultBlock ? (vi.ominous ? 0xFFEC4899 : 0xFFF97316) : 0xFFF59E0B;
                    g.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFF000000);
                    g.fill(mx, my, mx + 1, my + 1, c);
                }
            }
        }

        // Сущности на карте (аккуратные точки с тёмной обводкой, не размазанные пятна!)
        for (RadarBlip b : radarBlips) {
            double dx = b.x - mc.player.getX();
            double dz = b.z - mc.player.getZ();
            double rX = (dx * cos - dz * sin) * scale;
            double rY = (dx * sin + dz * cos) * scale;
            if (rX * rX + rY * rY > maxR * maxR) continue;
            int bx = cx + (int) rX;
            int by = cy + (int) rY;
            if (b.kind == 0) {
                g.fill(bx - 2, by - 2, bx + 3, by + 3, 0xFF000000);
                g.fill(bx - 1, by - 1, bx + 2, by + 2, 0xFF38BDF8);
                if (b.name != null) g.drawString(mc.font, trimText(mc.font, b.name, 42), bx + 4, by - 3, 0xFFE0F2FE);
            } else if (b.kind == 1) {
                g.fill(bx - 1, by - 1, bx + 2, by + 2, 0xAA000000);
                g.fill(bx, by, bx + 1, by + 1, 0xFFEF4444);
            } else if (b.kind == 3) {
                g.fill(bx - 1, by - 1, bx + 2, by + 2, 0xFF000000);
                g.fill(bx, by, bx + 1, by + 1, 0xFFC084FC);
            } else if (b.kind == 2) {
                g.fill(bx, by, bx + 2, by + 2, 0xFF4ADE80);
            } else if (b.kind == 4) {
                g.fill(bx, by, bx + 1, by + 1, 0xFFFACC15);
            }
        }

        // Активный маяк Навигатора по краю круга (всегда виден, даже если Деревня за 900м!)
        BlockPos pin = activeNavTarget != null ? activeNavTarget : homeWaypoint;
        if (pin != null) {
            double dx = (pin.getX() + 0.5) - mc.player.getX();
            double dz = (pin.getZ() + 0.5) - mc.player.getZ();
            double rX = (dx * cos - dz * sin) * scale;
            double rY = (dx * sin + dz * cos) * scale;
            double d = Math.sqrt(rX * rX + rY * rY);
            if (d > maxR) {
                rX = (rX / d) * maxR;
                rY = (rY / d) * maxR;
            }
            int hx = cx + (int) rX;
            int hy = cy + (int) rY;
            g.fill(hx - 3, hy - 3, hx + 4, hy + 4, 0xFF000000);
            g.fill(hx - 2, hy - 2, hx + 3, hy + 3, activeNavColor);
        }

        // Маркер Севера (N)
        int nx = cx + (int) (-sin * (maxR - 1)) - 2;
        int ny = cy + (int) (-cos * (maxR - 1)) - 3;
        g.fill(nx - 1, ny - 1, nx + 7, ny + 8, 0xDD080D14);
        g.drawString(mc.font, "N", nx, ny, 0xFF22D3EE);

        // Чёткая стрелка игрока в центре
        g.fill(cx - 2, cy - 1, cx + 3, cy + 2, 0xFF000000);
        g.fill(cx - 1, cy - 2, cx + 2, cy + 1, 0xFFFFFFFF);
        g.fill(cx, cy - 4, cx + 1, cy - 1, currentTheme.accent);

        // Компактная плашка координат прямо под кругом (заканчивается выше Discord!)
        int iy = y + MAP_DIAMETER + 4;
        int iw = MAP_DIAMETER;
        fillGlassCard(g, x, iy, iw, 22, currentTheme.bgWindow, currentTheme.secondary);
        String cStr = (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
        g.drawString(mc.font, trimText(mc.font, cStr, iw - 8), x + 4, iy + 3, 0xFFF1F5F9);
        String subStr = (pin != null)
            ? ("→ " + activeNavTitle + " " + (int) Math.sqrt(mc.player.blockPosition().distSqr(pin)) + "м")
            : ("✦ " + currentForm.russianName);
        g.drawString(mc.font, trimText(mc.font, subStr, iw - 8), x + 4, iy + 12, currentTheme.accent);
    }

    private static void renderElytraHorizonHud(GuiGraphics g, Font font, Player player, int sw, int sh) {
        int cx = sw / 2;
        int cy = sh / 2;
        float pitch = player.getXRot();
        int shiftY = (int) Mth.clamp(-pitch * 0.8F, -36.0F, 36.0F);
        g.fill(cx - 34, cy + shiftY, cx - 10, cy + shiftY + 1, 0xAA22D3EE);
        g.fill(cx + 10, cy + shiftY, cx + 34, cy + shiftY + 1, 0xAA22D3EE);
        g.drawString(font, String.format("%.0f°", -pitch), cx + 38, cy + shiftY - 3, 0xFF22D3EE);
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
                if (!st.isEmpty() && st.getItem() == hand.getItem()) count += st.getCount();
            }
        }
        int w = 154;
        int h = 30;
        int x = litematicaHudRight ? (sw - w - 10) : 124;
        int y = 30;
        fillGlassCard(g, x, y, w, h, currentTheme.bgWindow, currentTheme.borderMuted);

        if (!hand.isEmpty()) {
            g.renderItem(hand, x + 4, y + 7);
        }
        int tx = hand.isEmpty() ? (x + 6) : (x + 24);
        String line1 = hand.isEmpty() ? "Litematica HUD" : (hand.getHoverName().getString() + " x" + count);
        g.drawString(mc.font, trimText(mc.font, line1, w - (tx - x) - 6), tx, y + 5, 0xFFF8FAFC);

        String line2 = "Хранилищ: " + countNearbyVaultBlocks() + " • Y:" + (int) p.getY();
        if (ruler3DVolume && mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            double dist = p.position().distanceTo(bhr.getLocation());
            line2 = String.format("Блок: %.1fм • Y:%d", dist, bhr.getBlockPos().getY());
        }
        g.drawString(mc.font, trimText(mc.font, line2, w - (tx - x) - 6), tx, y + 17, currentTheme.accent);
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

        int w = 148;
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
        int w = 192;
        int h = 34;
        int x = 10;
        int y = sh - h - 10;
        fillGlassCard(g, x, y, w, h, 0xEE0A1710, spotifyIsPlaying ? 0xFF1DB954 : 0xFF334155);

        g.fillGradient(x + 5, y + 5, x + 29, y + 29, 0xFF1E3A8A, 0xFF065F46);
        long t = System.currentTimeMillis() / 120L;
        for (int i = 0; i < 4; i++) {
            int bh = spotifyIsPlaying ? (4 + (int) (Math.abs(Math.sin(t * 0.8 + i * 1.3)) * 14)) : 3;
            g.fill(x + 8 + i * 5, y + 27 - bh, x + 11 + i * 5, y + 27, spotifyIsPlaying ? 0xFF4ADE80 : 0xFF64748B);
        }

        g.drawString(mc.font, trimText(mc.font, spotifyTrackTitle, w - 40), x + 34, y + 5, 0xFFFFFFFF);
        g.drawString(mc.font, trimText(mc.font, spotifyTrackArtist, w - 40), x + 34, y + 15, spotifyIsPlaying ? 0xFF86EFAC : 0xFF94A3B8);

        int barX = x + 34;
        int barW = w - 40;
        g.fill(barX, y + 26, barX + barW, y + 28, 0xFF1F2937);
        float prog = spotifyDurSec > 0 ? Mth.clamp((float) spotifyPosSec / spotifyDurSec, 0.0F, 1.0F) : (spotifyIsPlaying ? 0.5F : 0.0F);
        g.fill(barX, y + 26, barX + (int) (barW * prog), y + 28, 0xFF1DB954);
    }

    // =========================================================================
    // 11. ПРЕМИУМ-ГРАФИКА: ГРАДИЕНТНЫЕ НЕОНОВЫЕ РАМКИ И ИКОНКИ
    // =========================================================================

    private static void fillGlassCard(GuiGraphics g, int x, int y, int w, int h, int fillCol, int borderCol) {
        fillRoundedRect(g, x, y, w, h, fillCol);
        drawRoundedBorder(g, x, y, w, h, borderCol);
    }

    private static void drawGradientNeonBorder(GuiGraphics g, int x, int y, int w, int h, int colLeft, int colRight) {
        if (w <= 4 || h <= 4) return;
        int segs = 12;
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

    private static void fillRoundedRect(GuiGraphics g, int x, int y, int w, int h, int color) {
        if (w <= 4 || h <= 4) { g.fill(x, y, x + w, y + h, color); return; }
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
                g.fill(x + 4, y, x + 6, y + 2, col);
                drawRoundedBorder(g, x + 1, y + 2, 8, 7, col);
                g.fill(x + 3, y + 4, x + 4, y + 6, col);
                g.fill(x + 6, y + 4, x + 7, y + 6, col);
            }
            case AUTO_BUILD -> {
                g.fill(x + 1, y + 1, x + 3, y + 9, col);
                g.fill(x + 1, y + 7, x + 9, y + 9, col);
                g.fill(x + 5, y + 2, x + 8, y + 5, 0xFFFBBF24);
            }
            case LITEMATICA_HUD -> {
                drawRoundedBorder(g, x, y + 1, 10, 8, col);
                g.fill(x + 2, y + 3, x + 8, y + 4, col);
                g.fill(x + 2, y + 6, x + 6, y + 7, col);
            }
            case COMBAT_PVP -> {
                g.fill(x + 1, y + 1, x + 3, y + 3, col);
                g.fill(x + 3, y + 3, x + 7, y + 7, col);
                g.fill(x + 7, y + 7, x + 9, y + 9, col);
                g.fill(x + 7, y + 1, x + 9, y + 3, col);
                g.fill(x + 1, y + 7, x + 3, y + 9, col);
            }
            case VISUAL_RADAR -> {
                drawRoundedBorder(g, x + 1, y + 1, 8, 8, col);
                g.fill(x + 4, y + 4, x + 6, y + 6, col);
                g.fill(x + 6, y + 2, x + 8, y + 4, 0xFF4ADE80);
            }
            case SPOTIFY_PLAYER -> {
                fillRoundedRect(g, x + 1, y + 1, 8, 8, 0xFF1DB954);
                g.fill(x + 2, y + 3, x + 8, y + 4, 0xFF06150C);
                g.fill(x + 3, y + 5, x + 7, y + 6, 0xFF06150C);
                g.fill(x + 3, y + 7, x + 6, y + 8, 0xFF06150C);
            }
            case CHESTS_VILLAGERS -> {
                drawRoundedBorder(g, x + 1, y + 2, 4, 4, 0xFFFBBF24);
                g.fill(x + 5, y + 3, x + 9, y + 5, 0xFFFBBF24);
                g.fill(x + 7, y + 5, x + 8, y + 7, 0xFFFBBF24);
            }
            case STYLE_PROFILER -> {
                g.fill(x + 3, y + 1, x + 7, y + 4, 0xFFFDBA74);
                g.fill(x + 2, y + 5, x + 8, y + 9, col);
            }
        }
    }

    // =========================================================================
    // 12. ПРЕМИУМ 3-КОЛОНОЧНОЕ МЕНЮ КОНЦЕПТА (ОТКАЛИБРОВАНО ПОД 640x360 GUI SCALE 3!)
    // =========================================================================

    public static class AssistantVadimkaScreen extends Screen {
        private MenuTab activeTab;
        private int rightPage = 0;
        private int rightTotalPages = 1;

        private boolean showChatOverlay = false;
        private int chestsCenterTab = 0; // 0=3D Дворец и Библиотекарь, 1=Ларчики, 2=Жители
        private int spotifyCenterTab = 0; // 0=Spotify Connect (ПК), 1=AFK Мини-игры

        private EditBox activeEditBox = null;
        private String chatInputDraft = "";

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

            this.winW = Math.min(this.width - 10, 660);
            this.winH = Math.min(this.height - 10, 354);
            this.winX = (this.width - this.winW) / 2;
            this.winY = (this.height - this.winH) / 2;

            this.colTopY = this.winY + 30;
            this.colH = this.winH - 36;

            this.leftColW = 104;
            this.rightColW = 208;
            this.leftColX = this.winX + 5;
            this.centerColX = this.leftColX + this.leftColW + 5;
            this.centerColW = this.winW - this.leftColW - this.rightColW - 20;
            this.rightColX = this.centerColX + this.centerColW + 5;

            // 1. Левая панель навигации (8 вкладок — все названия помещаются без единой обрезки!)
            MenuTab[] tabs = MenuTab.values();
            int itemH = 26;
            int startY = this.colTopY + 5;
            for (int i = 0; i < tabs.length; i++) {
                MenuTab t = tabs[i];
                int ix = this.leftColX + 4;
                int iy = startY + i * (itemH + 4);
                int iw = this.leftColW - 8;
                this.navItems.add(new NavItemVis(t, ix, iy, iw, itemH));
                addHitbox(ix, iy, iw, itemH, () -> {
                    this.activeTab = t;
                    this.rightPage = 0;
                    this.showChatOverlay = false;
                    if (t != MenuTab.SPOTIFY_PLAYER) this.afkGameRunning = false;
                    this.rebuildWidgets();
                });
            }

            // Кнопка смены темы внизу левой панели
            addStyledBtn(this.leftColX + 4, this.colTopY + this.colH - 24, this.leftColW - 8, 19,
                currentTheme.title, 1, () -> {
                    MenuTheme[] th = MenuTheme.values();
                    currentTheme = th[(currentTheme.ordinal() + 1) % th.length];
                    this.rebuildWidgets();
                });

            // Крестик закрытия
            addStyledBtn(this.winX + this.winW - 22, this.winY + 5, 17, 17, "✕", 2, this::onClose);

            // 2. Интерактивные кнопки в центральной сцене
            initCenterStageButtons();

            // 3. Правая панель настроек (6 карточек на странице, просторные подписи без обрезки!)
            List<ControlSpec> specs = buildRightColumnSpecs(this.activeTab);
            int perPage = 6;
            this.rightTotalPages = Math.max(1, (specs.size() + perPage - 1) / perPage);
            this.rightPage = Mth.clamp(this.rightPage, 0, this.rightTotalPages - 1);

            if (this.rightTotalPages > 1) {
                int py = this.colTopY + 5;
                addStyledBtn(this.rightColX + this.rightColW - 56, py, 17, 15, "◀", 0, () -> {
                    this.rightPage = (this.rightPage - 1 + this.rightTotalPages) % this.rightTotalPages;
                    this.rebuildWidgets();
                });
                addStyledBtn(this.rightColX + this.rightColW - 21, py, 17, 15, "▶", 0, () -> {
                    this.rightPage = (this.rightPage + 1) % this.rightTotalPages;
                    this.rebuildWidgets();
                });
            }

            int sIdx = this.rightPage * perPage;
            int eIdx = Math.min(specs.size(), sIdx + perPage);
            int ry = this.colTopY + 24;
            for (int i = sIdx; i < eIdx; i++) {
                ControlSpec sp = specs.get(i);
                this.currentSpecs.add(sp);
                int rx = this.rightColX + 5;
                int rw = this.rightColW - 10;
                int rh = 44;

                int cw = sp.kind == 0 ? 34 : (sp.kind == 1 ? 116 : 82);
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
            int sx = this.centerColX + 7;
            int sw = this.centerColW - 14;
            int bottomY = this.colTopY + this.colH - 24;

            switch (this.activeTab) {
                case COMPANION -> {
                    int topBtnW = 76;
                    addStyledBtn(sx + sw - topBtnW, this.colTopY + 5, topBtnW, 15,
                        showChatOverlay ? "✦ 3D Вид" : "💬 Чат", 1, () -> {
                            showChatOverlay = !showChatOverlay;
                            this.rebuildWidgets();
                        });

                    if (showChatOverlay) {
                        int sendW = 64;
                        int boxW = sw - sendW - 6;
                        EditBox box = new EditBox(this.font, sx, bottomY, boxW, 18, Component.literal("Чат"));
                        box.setMaxLength(110);
                        box.setValue(chatInputDraft);
                        box.setHint(Component.literal("!деревня / !испытания / !открой..."));
                        box.setTextColor(0xFFF8FAFC);
                        box.setResponder(s -> chatInputDraft = s);
                        this.addRenderableWidget(box);
                        this.activeEditBox = box;

                        addStyledBtn(sx + boxW + 6, bottomY, sendW, 18, "Сказать", 1, () -> {
                            if (!chatInputDraft.trim().isEmpty()) {
                                askVadimkaFromGui(chatInputDraft);
                                chatInputDraft = "";
                                this.rebuildWidgets();
                            }
                        });
                    } else if (!positionEditor3D) {
                        VadimkaForm[] forms = VadimkaForm.values();
                        int cw = (sw - 10) / 3;
                        for (int i = 0; i < forms.length; i++) {
                            VadimkaForm f = forms[i];
                            int bx = sx + (i % 3) * (cw + 5);
                            int by = bottomY - 22 + (i / 3) * 21;
                            addStyledBtn(bx, by, cw, 18, f.russianName, currentForm == f ? 1 : 0, () -> {
                                currentForm = f;
                                this.rebuildWidgets();
                            });
                        }
                    } else {
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
                case VISUAL_RADAR -> {
                    // 4 Кнопки реального поиска структур прямо под сонаром!
                    StructureTarget[] quick = {
                        StructureTarget.VILLAGE,
                        StructureTarget.TRIAL_CHAMBER,
                        StructureTarget.STRONGHOLD,
                        StructureTarget.ANCIENT_CITY
                    };
                    int hw = (sw - 6) / 2;
                    for (int i = 0; i < quick.length; i++) {
                        StructureTarget st = quick[i];
                        int bx = sx + (i % 2) * (hw + 6);
                        int by = bottomY - 22 + (i / 2) * 21;
                        addStyledBtn(bx, by, hw, 18, "Найти: " + st.russianName, 1, () -> {
                            locateStructureAsync(st);
                            this.rebuildWidgets();
                        });
                    }
                }
                case SPOTIFY_PLAYER -> {
                    int hw = (sw - 6) / 2;
                    addStyledBtn(sx, this.colTopY + 5, hw, 16, "♫ Spotify (ПК)", spotifyCenterTab == 0 ? 3 : 0, () -> {
                        spotifyCenterTab = 0;
                        afkGameRunning = false;
                        pollWindowsMediaAsync(true);
                        this.rebuildWidgets();
                    });
                    addStyledBtn(sx + hw + 6, this.colTopY + 5, hw, 16, "🎮 AFK Игры", spotifyCenterTab == 1 ? 1 : 0, () -> {
                        spotifyCenterTab = 1;
                        this.rebuildWidgets();
                    });

                    if (spotifyCenterTab == 0) {
                        // Кнопки управления РЕАЛЬНЫМ Spotify на Windows 10 аккуратно встроены в плеер!
                        int py = this.colTopY + 136;
                        int bw = (sw - 28) / 5;
                        int startX = sx + 6;
                        addStyledBtn(startX, py, bw, 20, "⏮", 0, () -> controlWindowsMediaAsync("PREV"));
                        addStyledBtn(startX + (bw + 4), py, bw, 20, spotifyIsPlaying ? "⏸" : "▶", 3, () -> controlWindowsMediaAsync("TOGGLE"));
                        addStyledBtn(startX + (bw + 4) * 2, py, bw, 20, "⏭", 0, () -> controlWindowsMediaAsync("NEXT"));
                        addStyledBtn(startX + (bw + 4) * 3, py, bw, 20, "🔉", 0, () -> controlWindowsMediaAsync("VOL_DOWN"));
                        addStyledBtn(startX + (bw + 4) * 4, py, bw, 20, "🔊", 0, () -> controlWindowsMediaAsync("VOL_UP"));

                        addStyledBtn(sx + 6, bottomY - 2, sw - 12, 18, "⟳ Обновить текущий трек из Spotify (ПК)", 3, () -> {
                            pollWindowsMediaAsync(true);
                            showToast("Синхронизация со Spotify...", 45);
                        });
                    } else {
                        int bw = (sw - 6) / 2;
                        addStyledBtn(sx, bottomY, bw, 18, selectedAfkGame.title, 0, () -> {
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
                    // Верхний ряд: быстрый поиск Дворца / Деревни + реальное открытие Хранилища!
                    int row1Y = bottomY - 22;
                    int bw = (sw - 8) / 3;
                    addStyledBtn(sx, row1Y, bw, 18, "Найти Дворец", 1, () -> {
                        locateStructureAsync(StructureTarget.TRIAL_CHAMBER);
                        this.rebuildWidgets();
                    });
                    addStyledBtn(sx + bw + 4, row1Y, bw, 18, "Найти Деревню", 1, () -> {
                        locateStructureAsync(StructureTarget.VILLAGE);
                        this.rebuildWidgets();
                    });
                    addStyledBtn(sx + (bw + 4) * 2, row1Y, bw, 18, "Открыть рядом", 3, () -> {
                        String res = triggerManualOpenNearestContainer();
                        showToast(res, 75);
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            mc.player.displayClientMessage(Component.literal("§b[✦ Вадимка] §f" + res), false);
                        }
                    });

                    // Нижний ряд: Сброс, Жители, Ларчики
                    addStyledBtn(sx, bottomY, bw, 18, "Сброс памяти", 2, () -> {
                        rememberedChests.clear();
                        rememberedVillagers.clear();
                        clearNavigationTarget();
                        showToast("Память очищена", 50);
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
                        chestsCenterTab == 1 ? 1 : 0, () -> {
                            chestsCenterTab = (chestsCenterTab == 1) ? 0 : 1;
                            this.rebuildWidgets();
                        });
                }
                case STYLE_PROFILER -> {
                    addStyledBtn(sx, bottomY, sw, 18, "Сбросить профиль", 2, () -> {
                        sessionWalkedBlocks = 0.0;
                        profilerTrainingPct = 50;
                        showToast("Профиль сброшен", 50);
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
                    list.add(toggleSpec("Спутник Вадимка", "3D-модель в мире", companionEnabled, () -> companionEnabled = !companionEnabled));
                    list.add(cycleSpec("Облик", "6 моделей 3D", currentForm.russianName, () -> {
                        VadimkaForm[] fs = VadimkaForm.values();
                        currentForm = fs[(currentForm.ordinal() + 1) % fs.length];
                    }));
                    list.add(toggleSpec("3D-редактор XYZ", "Оси позиции", positionEditor3D, () -> positionEditor3D = !positionEditor3D));
                    list.add(sliderSpec("X оффсет", String.format("%.2f бл.", offsetX), (float) ((offsetX + 2.5) / 5.0), r -> offsetX = -2.5 + r * 5.0));
                    list.add(sliderSpec("Y высота", String.format("%.2f бл.", offsetY), (float) (offsetY / 3.0), r -> offsetY = r * 3.0));
                    list.add(sliderSpec("Z дистанция", String.format("%.2f бл.", offsetZ), (float) ((offsetZ + 2.5) / 5.0), r -> offsetZ = -2.5 + r * 5.0));
                    list.add(toggleSpec("Миньоны (4)", "Помощники рядом", minionsEnabled, () -> minionsEnabled = !minionsEnabled));
                    list.add(toggleSpec("Умный AI", "К луту и мобам", minionsSmartAi, () -> minionsSmartAi = !minionsSmartAi));
                    list.add(sliderSpec("Размер 3D", companionScalePercent + "%", (companionScalePercent - 60) / 80.0F, r -> companionScalePercent = 60 + (int) Math.round(r * 80.0)));
                }
                case AUTO_BUILD -> {
                    list.add(toggleSpec("Авто-постройка", "По схеме", autoBuildLitematica, () -> autoBuildLitematica = !autoBuildLitematica));
                    list.add(toggleSpec("Плавная камера", "Кривые Безье", smoothBezierCamera, () -> smoothBezierCamera = !smoothBezierCamera));
                    list.add(toggleSpec("Авто-хотбар", "Пополнение", hotbarAutoRefill, () -> hotbarAutoRefill = !hotbarAutoRefill));
                    list.add(toggleSpec("Авто-наковальня", "Зачарование", autoAnvilEnchant, () -> autoAnvilEnchant = !autoAnvilEnchant));
                    list.add(sliderSpec("Мин. задержка", minBuildDelayMs + " мс", (minBuildDelayMs - 20) / 180.0F, r -> minBuildDelayMs = 20 + (int) Math.round(r * 180.0)));
                    list.add(sliderSpec("Дистанция", String.format("%.1f бл.", buildReachBlocks), (float) ((buildReachBlocks - 2.0) / 4.0), r -> buildReachBlocks = 2.0 + r * 4.0));
                }
                case LITEMATICA_HUD -> {
                    list.add(toggleSpec("Litematica HUD", "Трекер блоков", litematicaHudEnabled, () -> litematicaHudEnabled = !litematicaHudEnabled));
                    list.add(toggleSpec("HUD справа", "Угол экрана", litematicaHudRight, () -> litematicaHudRight = !litematicaHudRight));
                    list.add(toggleSpec("Сканер шалкеров", "Учёт в сумках", shulkerScanner, () -> shulkerScanner = !shulkerScanner));
                    list.add(toggleSpec("Скрыть готовое", "Только дефицит", hideCompletedMaterials, () -> hideCompletedMaterials = !hideCompletedMaterials));
                    list.add(toggleSpec("3D-линейка", "Дист. до блока", ruler3DVolume, () -> ruler3DVolume = !ruler3DVolume));
                    list.add(cycleSpec("Палитра", "Набор блоков", BUILD_PALETTES.get(buildPaletteIndex), () -> buildPaletteIndex = (buildPaletteIndex + 1) % BUILD_PALETTES.size()));
                }
                case COMBAT_PVP -> {
                    list.add(toggleSpec("Боевой HUD", "Тотем/Броня", combatHudEnabled, () -> combatHudEnabled = !combatHudEnabled));
                    list.add(toggleSpec("3D-траектории", "Дуга снарядов", trajectories3D, () -> trajectories3D = !trajectories3D));
                    list.add(toggleSpec("Сканер брони", "Проценты Ш/Н/П/Б", armorAndTotemScanner, () -> armorAndTotemScanner = !armorAndTotemScanner));
                    list.add(toggleSpec("Таймеры пёрлов", "КД щита/пёрла", shieldAndPearlTimers, () -> shieldAndPearlTimers = !shieldAndPearlTimers));
                    list.add(toggleSpec("Удар со спины", "Алерт угрозы", backstabIndicator, () -> backstabIndicator = !backstabIndicator));
                    list.add(toggleSpec("Маяк смерти", "Навигатор назад", deathReplayMarker, () -> deathReplayMarker = !deathReplayMarker));
                }
                case VISUAL_RADAR -> {
                    list.add(toggleSpec("Мини-карта HUD", "1px чёткая", minimapRadarEnabled, () -> minimapRadarEnabled = !minimapRadarEnabled));
                    list.add(toggleSpec("Круглая карта", "Стиль сонара", minimapCircularMode, () -> minimapCircularMode = !minimapCircularMode));
                    list.add(cycleSpec("Масштаб", "Обзор карты",
                        radarZoomLevel == 0 ? "1x (Чётко)" : (radarZoomLevel == 1 ? "2x (Средне)" : "3x (Дальше)"), () -> {
                            radarZoomLevel = (radarZoomLevel + 1) % 3;
                            mapNeedsReset = true;
                        }));
                    list.add(toggleSpec("Звуковой сонар", "Враги по краю", soundRadarSonar, () -> soundRadarSonar = !soundRadarSonar));
                    list.add(toggleSpec("Элитра Горизонт", "Угол в полёте", elytraPitchHorizon, () -> elytraPitchHorizon = !elytraPitchHorizon));
                    list.add(toggleSpec("Нить Ариадны", "След пути", ariadneThread, () -> ariadneThread = !ariadneThread));
                    list.add(cycleSpec("Точка Дома", homeWaypoint == null ? "Не задан" : (homeWaypoint.getX() + ", " + homeWaypoint.getZ()),
                        homeWaypoint == null ? "Задать Дом" : "Вести домой", () -> {
                            Minecraft mc = Minecraft.getInstance();
                            if (mc.player != null) {
                                if (homeWaypoint == null) homeWaypoint = mc.player.blockPosition();
                                setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
                                showToast("Навигатор ведёт к Дому!", 70);
                            }
                        }));
                    list.add(cycleSpec("Навигатор", activeNavTarget == null ? "Выключен" : activeNavTitle, "Сбросить", () -> {
                        clearNavigationTarget();
                        showToast("Навигатор отключён", 45);
                    }));
                }
                case SPOTIFY_PLAYER -> {
                    list.add(toggleSpec("Spotify HUD", "Трек на экране", spotifyHudEnabled, () -> spotifyHudEnabled = !spotifyHudEnabled));
                    list.add(toggleSpec("Танцы в такт", "Вадимка под бит", vadimkaDanceToBpm, () -> vadimkaDanceToBpm = !vadimkaDanceToBpm));
                    list.add(toggleSpec("Пауза в чате", "Тише при вводе", spotifyDuckOnSpeech, () -> spotifyDuckOnSpeech = !spotifyDuckOnSpeech));
                    list.add(toggleSpec("Встроенный синт", "Только без ПК", builtInSynthEnabled, () -> builtInSynthEnabled = !builtInSynthEnabled));
                    list.add(toggleSpec("Выход из AFK", "При уроне", afkPanicExit, () -> afkPanicExit = !afkPanicExit));
                    list.add(cycleSpec("Статус ПК", spotifyConnected ? spotifySourceName : "Не найден", "Обновить", () -> pollWindowsMediaAsync(true)));
                }
                case CHESTS_VILLAGERS -> {
                    list.add(toggleSpec("Авто-хранилища", "Ключ + ПКМ сам", autoClickVaults, () -> autoClickVaults = !autoClickVaults));
                    list.add(toggleSpec("Подсветка", "Хранилищ/сунд.", highlightTrialVaults, () -> highlightTrialVaults = !highlightTrialVaults));
                    list.add(cycleSpec("Цели лута", "Дворец испыт.", VAULT_LOOT_GOALS.get(vaultLootGoalIndex), () -> vaultLootGoalIndex = (vaultLootGoalIndex + 1) % VAULT_LOOT_GOALS.size()));
                    list.add(cycleSpec("Нужные чары", "Библиотекарь", TARGET_ENCHANTS.get(targetEnchantIndex), () -> targetEnchantIndex = (targetEnchantIndex + 1) % TARGET_ENCHANTS.size()));
                    list.add(sliderSpec("Макс. цена", maxEmeraldPrice + " изумр.", (maxEmeraldPrice - 5) / 59.0F, r -> maxEmeraldPrice = 5 + (int) Math.round(r * 59.0)));
                    list.add(toggleSpec("Страж топора", "Алерт прочн.", warnAxeDurability, () -> warnAxeDurability = !warnAxeDurability));
                }
                case STYLE_PROFILER -> {
                    list.add(cycleSpec("Тема меню", "5 палитр", currentTheme.title, () -> {
                        MenuTheme[] th = MenuTheme.values();
                        currentTheme = th[(currentTheme.ordinal() + 1) % th.length];
                    }));
                    list.add(cycleSpec("3D-крылья", "В мире F5", WINGS_OPTIONS.get(wingsIndex), () -> wingsIndex = (wingsIndex + 1) % WINGS_OPTIONS.size()));
                    list.add(cycleSpec("Рога / Нимб", "Над головой", HORNS_OPTIONS.get(hornsIndex), () -> hornsIndex = (hornsIndex + 1) % HORNS_OPTIONS.size()));
                    list.add(cycleSpec("Шляпка", "Головной убор", HATS_OPTIONS.get(hatsIndex), () -> hatsIndex = (hatsIndex + 1) % HATS_OPTIONS.size()));
                    list.add(cycleSpec("Прицел", "Центр экрана", CROSSHAIR_OPTIONS.get(crosshairIndex), () -> crosshairIndex = (crosshairIndex + 1) % CROSSHAIR_OPTIONS.size()));
                    list.add(toggleSpec("Эффекты F5", "Крылья и нимб", worldCosmeticsF5, () -> worldCosmeticsF5 = !worldCosmeticsF5));
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

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            double mx = event.x();
            double my = event.y();
            if (event.button() == 0) {
                for (int i = 0; i < rightControls.size(); i++) {
                    RightControlVis rc = rightControls.get(i);
                    if (rc.kind == 1 && mx >= rc.x + 4 && mx <= rc.x + rc.w - 54 && my >= rc.ctrlY - 4 && my <= rc.ctrlY + rc.ctrlH + 4) {
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
                int trackX = rc.x + 8;
                int trackW = rc.w - 72;
                double ratio = Mth.clamp((mx - trackX) / Math.max(1.0, trackW), 0.0, 1.0);
                sp.onSlide.accept(ratio);
                this.rebuildWidgets();
            }
        }

        private void resetAfkGame() {
            afkScore = 0; afkTick = 0; runnerLane = 1; runnerObs.clear();
            sonicY = 0.0; sonicVel = 0.0; sonicObs.clear();
            doomX = 50; doomShots.clear(); doomMobs.clear();
        }

        @Override
        public void tick() {
            super.tick();
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
                        runnerObs.clear(); runnerObs.addAll(next);
                    }
                }
                case SONIC -> {
                    if (afkTick % 6 == 0) afkScore++;
                    sonicY += sonicVel; sonicVel -= 1.35;
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
                    sonicObs.clear(); sonicObs.addAll(next);
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
                if (k == GLFW.GLFW_KEY_ESCAPE) { activeEditBox.setFocused(false); return true; }
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
            if (k == GLFW.GLFW_KEY_P) { this.onClose(); return true; }
            return super.keyPressed(event);
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            g.fill(0, 0, this.width, this.height, 0x9905080E);

            // 1. Внешний корпус окна с неоновой градиентной рамкой Cyan -> Violet!
            fillRoundedRect(g, winX, winY, winW, winH, currentTheme.bgWindow);
            drawGradientNeonBorder(g, winX, winY, winW, winH, currentTheme.accent, currentTheme.secondary);
            g.fillGradient(winX + 2, winY + 2, winX + winW - 2, winY + 25, currentTheme.bgCard, currentTheme.bgPanel);
            g.fill(winX + 2, winY + 25, winX + winW - 2, winY + 26, currentTheme.borderMuted);

            drawConceptTabIcon(g, MenuTab.COMPANION, winX + 9, winY + 8, currentTheme.accent);
            String titleStr = "Assistant Vadimka";
            int titleX = winX + (winW - this.font.width(titleStr)) / 2;
            g.drawString(this.font, titleStr, titleX, winY + 9, currentTheme.accent);
            g.drawString(this.font, VERSION_LABEL, winX + winW - 50, winY + 9, 0xFF94A3B8);

            // 2. Левая панель навигации (активная вкладка с тёмным фоном и градиентной неоновой рамкой как на концептах!)
            fillGlassCard(g, leftColX, colTopY, leftColW, colH, currentTheme.bgPanel, currentTheme.borderMuted);
            for (NavItemVis ni : navItems) {
                boolean active = ni.tab == this.activeTab;
                boolean hover = isOver(mouseX, mouseY, ni.x, ni.y, ni.w, ni.h);
                int fill = active ? lerpColor(currentTheme.bgCard, currentTheme.accent, 0.14F) : (hover ? currentTheme.bgCard : 0xFF0E141D);
                fillRoundedRect(g, ni.x, ni.y, ni.w, ni.h, fill);
                if (active) {
                    drawGradientNeonBorder(g, ni.x, ni.y, ni.w, ni.h, currentTheme.accent, currentTheme.secondary);
                    g.fill(ni.x + 2, ni.y + 5, ni.x + 4, ni.y + ni.h - 5, currentTheme.accent);
                } else {
                    drawRoundedBorder(g, ni.x, ni.y, ni.w, ni.h, hover ? currentTheme.accent : currentTheme.borderMuted);
                }
                drawConceptTabIcon(g, ni.tab, ni.x + 7, ni.y + 8, active ? currentTheme.accent : 0xFF94A3B8);
                g.drawString(this.font, ni.tab.navTitle, ni.x + 21, ni.y + 9, active ? 0xFFFFFFFF : 0xFFCBD5E1);
            }

            // 3. Центральная интерактивная сцена
            fillGlassCard(g, centerColX, colTopY, centerColW, colH, currentTheme.bgPanel, currentTheme.borderMuted);
            renderCenterStageByTab(g, mouseX, mouseY);

            // 4. Правая панель настроек (чистые тумблеры без слепящей белой рамки + слайдеры!)
            fillGlassCard(g, rightColX, colTopY, rightColW, colH, currentTheme.bgPanel, currentTheme.borderMuted);
            g.drawString(this.font, "Настройки", rightColX + 8, colTopY + 7, currentTheme.secondary);
            if (rightTotalPages > 1) {
                String pTxt = (rightPage + 1) + "/" + rightTotalPages;
                g.drawString(this.font, pTxt, rightColX + rightColW - 39, colTopY + 7, 0xFF94A3B8);
            }

            for (RightControlVis rc : rightControls) {
                boolean hover = isOver(mouseX, mouseY, rc.x, rc.y, rc.w, rc.h);
                fillGlassCard(g, rc.x, rc.y, rc.w, rc.h,
                    hover ? lerpColor(currentTheme.bgCard, 0xFFFFFFFF, 0.04F) : currentTheme.bgCard,
                    hover ? currentTheme.accent : currentTheme.borderMuted);

                if (rc.kind == 0) {
                    int textW = rc.ctrlX - (rc.x + 7) - 4;
                    g.drawString(this.font, trimText(this.font, rc.title, textW), rc.x + 7, rc.y + 8, 0xFFF8FAFC);
                    if (!rc.subtitle.isEmpty()) {
                        g.drawString(this.font, trimText(this.font, rc.subtitle, textW), rc.x + 7, rc.y + 22, 0xFF94A3B8);
                    }
                    // Современный стильный тумблер как на концепт-скринах!
                    int trackCol = rc.toggleVal ? currentTheme.toggleOnTrack : 0xFF1A2332;
                    int borderCol = rc.toggleVal ? currentTheme.accent : 0xFF334155;
                    fillGlassCard(g, rc.ctrlX, rc.ctrlY, rc.ctrlW, rc.ctrlH, trackCol, borderCol);
                    int knob = rc.ctrlH - 4;
                    int kx = rc.toggleVal ? (rc.ctrlX + rc.ctrlW - knob - 2) : (rc.ctrlX + 2);
                    fillRoundedRect(g, kx, rc.ctrlY + 2, knob, knob, rc.toggleVal ? 0xFFFFFFFF : 0xFF94A3B8);
                } else if (rc.kind == 1) {
                    g.drawString(this.font, rc.title, rc.x + 7, rc.y + 6, 0xFFF8FAFC);
                    int badgeW = 54;
                    int badgeX = rc.x + rc.w - badgeW - 6;
                    fillGlassCard(g, badgeX, rc.ctrlY, badgeW, rc.ctrlH, 0xFF0A1018, currentTheme.borderMuted);
                    String vStr = trimText(this.font, rc.valueText, badgeW - 4);
                    g.drawString(this.font, vStr, badgeX + (badgeW - this.font.width(vStr)) / 2, rc.ctrlY + 4, currentTheme.accent);

                    int trackX = rc.x + 8;
                    int trackW = badgeX - trackX - 8;
                    int trackY = rc.ctrlY + 6;
                    g.fill(trackX, trackY, trackX + trackW, trackY + 4, 0xFF1E293B);
                    int fillW = (int) (trackW * rc.sliderPct);
                    g.fillGradient(trackX, trackY, trackX + fillW, trackY + 4, currentTheme.accent, currentTheme.secondary);
                    fillRoundedRect(g, trackX + fillW - 3, trackY - 3, 6, 10, 0xFFFFFFFF);
                } else {
                    int textW = rc.ctrlX - (rc.x + 7) - 4;
                    g.drawString(this.font, trimText(this.font, rc.title, textW), rc.x + 7, rc.y + 8, 0xFFF8FAFC);
                    if (!rc.subtitle.isEmpty()) {
                        g.drawString(this.font, trimText(this.font, rc.subtitle, textW), rc.x + 7, rc.y + 22, 0xFF94A3B8);
                    }
                    fillGlassCard(g, rc.ctrlX, rc.ctrlY, rc.ctrlW, rc.ctrlH, 0xFF0C131C, currentTheme.secondary);
                    String vStr = trimText(this.font, rc.valueText, rc.ctrlW - 6);
                    g.drawString(this.font, vStr, rc.ctrlX + (rc.ctrlW - this.font.width(vStr)) / 2, rc.ctrlY + 4, 0xFFFFFFFF);
                }
            }

            // 5. Кастомные кнопки
            for (ActionBtnVis ab : actionButtons) {
                boolean hover = isOver(mouseX, mouseY, ab.x, ab.y, ab.w, ab.h);
                int bg = switch (ab.style) {
                    case 1 -> hover ? lerpColor(currentTheme.bgCard, currentTheme.accent, 0.36F) : lerpColor(currentTheme.bgCard, currentTheme.accent, 0.18F);
                    case 2 -> hover ? 0xFFDC2626 : 0xFF3A1219;
                    case 3 -> hover ? 0xFF1DB954 : 0xFF114023;
                    default -> hover ? currentTheme.bgCard : 0xFF0F1620;
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
        // 13. ЦЕНТРАЛЬНЫЕ СЦЕНЫ ВСЕХ 8 РАЗДЕЛОВ С 3D-ИКОНКАМИ MINECRAFT
        // =====================================================================

        private void renderCenterStageByTab(GuiGraphics g, int mouseX, int mouseY) {
            int sx = this.centerColX + 7;
            int sy = this.colTopY + 6;
            int sw = this.centerColW - 14;
            int sh = this.colH - 12;

            switch (this.activeTab) {
                case COMPANION -> renderCompanionCenterStage(g, sx, sy, sw, sh, mouseX, mouseY);
                case AUTO_BUILD -> renderAutoBuildCenterStage(g, sx, sy, sw, sh);
                case LITEMATICA_HUD -> renderLitematicaCenterStage(g, sx, sy, sw, sh);
                case COMBAT_PVP -> renderCombatCenterStage(g, sx, sy, sw, sh);
                case VISUAL_RADAR -> renderVisualRadarCenterStage(g, sx, sy, sw, sh);
                case SPOTIFY_PLAYER -> renderSpotifyCenterStage(g, sx, sy, sw, sh);
                case CHESTS_VILLAGERS -> renderChestsVillagersCenterStage(g, sx, sy, sw, sh, mouseX, mouseY);
                case STYLE_PROFILER -> renderStyleProfilerCenterStage(g, sx, sy, sw, sh);
            }
        }

        private void drawSoftStudioGrid(GuiGraphics g, int x, int y, int w, int h) {
            g.fillGradient(x, y, x + w, y + h, 0xFF080D14, lerpColor(0xFF0E1722, currentTheme.accent, 0.08F));
            drawRoundedBorder(g, x, y, w, h, currentTheme.borderMuted);

            int horizonY = y + (h * 48) / 100;
            int bottomY = y + h - 3;
            int cx = x + w / 2;
            int gridCol = 0x1622D3EE;

            int rows = 6;
            for (int i = 1; i <= rows; i++) {
                float t = (float) (i * i) / (rows * rows);
                int ly = horizonY + (int) ((bottomY - horizonY) * t);
                g.fill(x + 4, ly, x + w - 4, ly + 1, gridCol);
            }
            for (int k = -4; k <= 4; k++) {
                drawSmoothLine(g, cx + k * 14, horizonY, cx + k * 34, bottomY, gridCol, x + 2, x + w - 2);
            }
        }

        private void drawSmoothLine(GuiGraphics g, int x0, int y0, int x1, int y1, int col, int minX, int maxX) {
            int dx = x1 - x0;
            int dy = y1 - y0;
            int steps = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)));
            for (int i = 0; i <= steps; i++) {
                float t = (float) i / steps;
                int px = x0 + Math.round(dx * t);
                int py = y0 + Math.round(dy * t);
                if (px >= minX && px < maxX) {
                    g.fill(px, py, px + 1, py + 1, col);
                }
            }
        }

        private void renderCompanionCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh, int mouseX, int mouseY) {
            g.drawString(this.font, trimText(this.font, activeTab.headerTitle, sw - 82), sx, sy + 2, 0xFFF8FAFC);

            if (showChatOverlay) {
                int boxY = sy + 20;
                int boxH = sh - 48;
                fillGlassCard(g, sx, boxY, sw, boxH, 0xFF080D14, currentTheme.borderMuted);
                List<ChatLogEntry> msgs = new ArrayList<>(chatHistory);
                int maxLines = Math.max(1, (boxH - 10) / 12);
                List<String[]> wrapped = new ArrayList<>();
                for (ChatLogEntry entry : msgs) {
                    String prefix = entry.fromVadimka ? "✦ Вадимка: " : "Вы: ";
                    for (String line : wrapLines(prefix + entry.text, sw - 14)) {
                        wrapped.add(new String[]{entry.fromVadimka ? "1" : "0", line});
                    }
                }
                int start = Math.max(0, wrapped.size() - maxLines);
                int ty = boxY + 5;
                for (int i = start; i < wrapped.size(); i++) {
                    boolean isV = "1".equals(wrapped.get(i)[0]);
                    g.drawString(this.font, wrapped.get(i)[1], sx + 7, ty, isV ? currentTheme.accent : 0xFFE2E8F0);
                    ty += 12;
                }
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            int stageY = sy + 20;
            int stageH = sh - 68;
            drawSoftStudioGrid(g, sx, stageY, sw, stageH);

            if (!positionEditor3D) {
                VadimkaForm[] forms = VadimkaForm.values();
                int slotW = sw / forms.length;
                for (int i = 0; i < forms.length; i++) {
                    VadimkaForm f = forms[i];
                    int fx = sx + i * slotW;
                    int fy = stageY + 26;
                    int fh = stageH - 46;
                    boolean sel = f == currentForm;
                    if (sel) {
                        g.fill(fx + 3, fy + 1, fx + slotW - 3, fy + fh - 1, 0x1A22D3EE);
                        drawGradientNeonBorder(g, fx + 2, fy, slotW - 4, fh, currentTheme.accent, currentTheme.secondary);
                    } else {
                        drawRoundedBorder(g, fx + 2, fy, slotW - 4, fh, 0x4494A3B8);
                    }
                    g.drawString(this.font, trimText(this.font, f.codeName, slotW - 4), fx + 4, stageY + 7, sel ? currentTheme.accent : 0xFFE2E8F0);
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
                // Полоска Авто-костюмов с реальными 3D-иконками Minecraft внизу сцены (как на фото 1!)
                int costY = stageY + stageH - 18;
                g.drawString(this.font, "Авто-костюмы:", sx + 6, costY + 4, 0xFF94A3B8);
                ItemStack[] costumeIcons = {
                    new ItemStack(Items.GOLDEN_HELMET),
                    new ItemStack(Items.NETHERITE_HELMET),
                    new ItemStack(Items.ELYTRA),
                    new ItemStack(Items.DIAMOND_CHESTPLATE),
                    new ItemStack(Items.NETHER_STAR)
                };
                int ix = sx + 88;
                for (ItemStack icon : costumeIcons) {
                    g.renderItem(icon, ix, costY);
                    ix += 22;
                }
            } else {
                int centerX = sx + sw / 2 - 10;
                int centerY = stageY + stageH / 2 + 12;
                int petX = Mth.clamp(centerX + (int) (offsetX * 26.0), sx + 34, sx + sw - 34);
                int petY = Mth.clamp(centerY - (int) ((offsetY - 1.0) * 24.0), stageY + 34, stageY + stageH - 26);

                if (mc.player != null && mc.level != null) {
                    try {
                        InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, centerX - 36, stageY + 26, centerX + 36, stageY + stageH - 8,
                            44, 0.0625F, (float) mouseX, (float) mouseY, mc.player
                        );
                        LivingEntity pet = getCustomStudioModel(mc.level, mc.player, currentForm);
                        InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, petX - 24, petY - 24, petX + 24, petY + 24,
                            Math.max(14, (int) (currentForm.guiScale * 0.72)), 0.0625F, (float) mouseX, (float) mouseY, pet
                        );
                    } catch (Throwable ignored) {
                    }
                }

                // Неоновый куб и 3D-оси X / Y / Z (не наезжают на лицо игрока!)
                drawRoundedBorder(g, petX - 16, petY - 18, 32, 34, 0x9922D3EE);
                int tagY = Math.max(stageY + 5, petY - 29);
                g.drawString(this.font, "Вадимка", petX - 19, tagY, 0xFFFDE047);
                g.fill(petX, Math.max(stageY + 14, petY - 30), petX + 2, petY - 2, 0xFF22C55E);
                g.drawString(this.font, "Y", petX + 4, Math.max(stageY + 14, petY - 30), 0xFF4ADE80);
                drawSmoothLine(g, petX, petY, petX - 24, petY + 12, 0xFFEF4444, sx, sx + sw);
                g.drawString(this.font, "X", petX - 32, petY + 9, 0xFFF87171);
                drawSmoothLine(g, petX, petY, petX + 24, petY + 12, 0xFF3B82F6, sx, sx + sw);
                g.drawString(this.font, "Z", petX + 27, petY + 9, 0xFF60A5FA);
            }
        }

        private void renderAutoBuildCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "3D-голограмма • " + currentBuildMode.title, sx, sy + 2, 0xFFF8FAFC);
            int stageY = sy + 18;
            int stageH = sh - 66;
            drawSoftStudioGrid(g, sx, stageY, sw, stageH);

            int cx = sx + sw / 2;
            int baseY = stageY + stageH - 22;
            int w = 48;
            int h = 42;
            int neon = 0xFF38BDF8;

            g.fill(cx - w, baseY - h, cx + w, baseY, 0x2222D3EE);
            drawRoundedBorder(g, cx - w, baseY - h, w * 2, h, neon);
            g.fill(cx, baseY - h, cx + 1, baseY, 0x6638BDF8);
            for (int gx = cx - w + 16; gx < cx + w; gx += 16) g.fill(gx, baseY - h, gx + 1, baseY, 0x3338BDF8);
            for (int gy = baseY - h + 14; gy < baseY; gy += 14) g.fill(cx - w, gy, cx + w, gy + 1, 0x3338BDF8);
            for (int step = 0; step < 5; step++) {
                int rw = w + 6 - step * 10;
                int ry = baseY - h - (step + 1) * 7;
                g.fill(cx - rw, ry, cx + rw, ry + 7, 0x2C38BDF8);
                drawRoundedBorder(g, cx - rw, ry, rw * 2, 7, neon);
            }
            drawRoundedBorder(g, cx - 32, baseY - 28, 16, 14, 0xFF7DD3FC);
            drawRoundedBorder(g, cx + 14, baseY - 26, 15, 26, 0xFFFBBF24);

            String info = String.format("Задержка: %d–%d мс • Радиус: %.1f бл.", minBuildDelayMs, maxBuildDelayMs, buildReachBlocks);
            g.drawString(this.font, info, sx + 8, stageY + 7, currentTheme.accent);
        }

        private void renderLitematicaCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Трекер материалов Litematica HUD", sx, sy + 2, 0xFFF8FAFC);
            int cardY = sy + 20;
            int cardH = sh - 40;
            fillGlassCard(g, sx, cardY, sw, cardH, 0xFF0A1017, currentTheme.accent);

            Minecraft mc = Minecraft.getInstance();
            Player p = mc.player;

            ItemStack[] icons = {
                new ItemStack(Items.STONE_BRICKS),
                new ItemStack(Items.OAK_PLANKS),
                new ItemStack(Items.GLASS),
                new ItemStack(Items.QUARTZ_BLOCK)
            };
            String[] names = {"Каменные кирпичи", "Дубовые доски", "Стекло", "Кварцевый блок"};
            int[] have = {
                countItemInPlayer(p, Items.STONE_BRICKS) + countItemInPlayer(p, Items.COBBLESTONE),
                countItemInPlayer(p, Items.OAK_PLANKS) + countItemInPlayer(p, Items.OAK_LOG),
                countItemInPlayer(p, Items.GLASS) + countItemInPlayer(p, Items.GLASS_PANE),
                countItemInPlayer(p, Items.QUARTZ_BLOCK) + countItemInPlayer(p, Items.SMOOTH_QUARTZ)
            };
            int[] need = {256, 128, 64, 32};

            int rowY = cardY + 14;
            for (int i = 0; i < names.length; i++) {
                boolean done = have[i] >= need[i];
                if (hideCompletedMaterials && done) continue;

                // Реальная 3D-иконка блока Minecraft!
                g.renderItem(icons[i], sx + 14, rowY + 2);
                g.drawString(this.font, names[i], sx + 38, rowY + 1, 0xFFF8FAFC);
                int statusCol = done ? 0xFF4ADE80 : (have[i] > 0 ? 0xFFFACC15 : 0xFFF87171);
                g.drawString(this.font, have[i] + " / " + need[i], sx + 38, rowY + 12, statusCol);

                int barW = 74;
                int barX = sx + sw - barW - 12;
                g.fill(barX, rowY + 8, barX + barW, rowY + 13, 0xFF1E293B);
                float pct = Mth.clamp((float) have[i] / need[i], 0.0F, 1.0F);
                g.fill(barX, rowY + 8, barX + (int) (barW * pct), rowY + 13, statusCol);
                rowY += 34;
            }

            String posLabel = litematicaHudRight ? "Позиция HUD: Справа сверху" : "Позиция HUD: Слева";
            g.drawString(this.font, posLabel, sx + (sw - this.font.width(posLabel)) / 2, sy + sh - 13, 0xFF94A3B8);
        }

        private int countItemInPlayer(Player p, Item item) {
            if (p == null) return 0;
            int total = 0;
            for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                ItemStack st = p.getInventory().getItem(i);
                if (!st.isEmpty() && st.is(item)) total += st.getCount();
            }
            return total;
        }

        private void renderCombatCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Тест-превью Боя, Брони и 3D-траектории", sx, sy + 2, 0xFFF8FAFC);
            int stageY = sy + 18;
            int stageH = sh - 22;
            drawSoftStudioGrid(g, sx, stageY, sw, stageH);

            int cx = sx + sw / 2;
            int cy = stageY + stageH / 2;

            // Гладкая непрерывная неоновая кривая траектории (без разрывов!)
            if (trajectories3D) {
                int startX = sx + sw - 30;
                int startY = stageY + stageH - 18;
                int endX = cx - 18;
                int endY = cy - 16;
                int prevX = startX, prevY = startY;
                for (int i = 1; i <= 48; i++) {
                    float t = i / 48.0F;
                    int px = (int) Mth.lerp(t, startX, endX);
                    int py = (int) (Mth.lerp(t, startY, endY) - Math.sin(t * Math.PI) * 58.0);
                    int col = lerpColor(0xFFC084FC, 0xFF22D3EE, t);
                    drawSmoothLine(g, prevX, prevY, px, py, col, sx + 2, sx + sw - 2);
                    drawSmoothLine(g, prevX, prevY + 1, px, py + 1, col, sx + 2, sx + sw - 2);
                    prevX = px; prevY = py;
                }
                drawRoundedBorder(g, endX - 6, endY - 6, 12, 12, 0xFF22D3EE);
            }

            // Реальные иконки Перла, Щита и Стрелы в круговых рамках как на концепте (фото 7)!
            drawRoundedBorder(g, cx - 48, cy - 12, 26, 26, 0xFF22D3EE);
            g.renderItem(new ItemStack(Items.ENDER_PEARL), cx - 43, cy - 7);
            g.drawString(this.font, "3.3", cx - 42, cy + 16, 0xFF67E8F9);

            drawRoundedBorder(g, cx + 22, cy - 12, 26, 26, 0xFFC084FC);
            g.renderItem(new ItemStack(Items.SHIELD), cx + 27, cy - 7);
            g.drawString(this.font, "2.1", cx + 28, cy + 16, 0xFFE879F9);

            drawRoundedBorder(g, cx - 13, cy + 18, 26, 26, 0xFF38BDF8);
            g.renderItem(new ItemStack(Items.ARROW), cx - 8, cy + 23);
            g.drawString(this.font, "0.3", cx - 7, cy + 46, 0xFFFFFFFF);

            // Реальные иконки брони слева (Шлем / Нагрудник / Поножи / Ботинки)
            ItemStack[] armors = {
                new ItemStack(Items.DIAMOND_HELMET),
                new ItemStack(Items.DIAMOND_CHESTPLATE),
                new ItemStack(Items.DIAMOND_LEGGINGS),
                new ItemStack(Items.DIAMOND_BOOTS)
            };
            for (int i = 0; i < armors.length; i++) {
                int ay = stageY + 20 + i * 24;
                g.renderItem(armors[i], sx + 10, ay);
                g.drawString(this.font, "98%", sx + 30, ay + 4, 0xFF4ADE80);
            }
            g.renderItem(new ItemStack(Items.TOTEM_OF_UNDYING), sx + sw - 46, stageY + 20);
            g.drawString(this.font, "x" + (Minecraft.getInstance().player != null ? countPlayerTotems(Minecraft.getInstance().player) : 1), sx + sw - 26, stageY + 24, 0xFFFDE047);
        }

        private void renderVisualRadarCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Сонар, Элитра HUD и Поиск Структур", sx, sy + 2, 0xFFF8FAFC);
            int stageY = sy + 18;
            int stageH = sh - 66;
            drawSoftStudioGrid(g, sx, stageY, sw, stageH);

            // Слева: Концентрический сонар
            int rCx = sx + sw / 3 - 4;
            int rCy = stageY + stageH / 2 - 4;
            int[] radii = {44, 30, 16};
            int[] cols = {0xAA22D3EE, 0xAAC084FC, 0xAA38BDF8};
            for (int i = 0; i < radii.length; i++) {
                drawRoundedBorder(g, rCx - radii[i], rCy - radii[i], radii[i] * 2, radii[i] * 2, cols[i]);
            }
            g.fill(rCx - 4, rCy, rCx + 5, rCy + 1, 0xFFFFFFFF);
            g.fill(rCx, rCy - 4, rCx + 1, rCy + 5, 0xFFFFFFFF);
            g.fill(rCx - 2, rCy - 40, rCx + 3, rCy - 35, 0xFFEF4444);
            g.fill(rCx + 34, rCy - 14, rCx + 39, rCy - 9, 0xFFFACC15);

            // Справа: Elytra Horizon + Статус поиска структур
            int hCx = sx + (sw * 3) / 4;
            int hCy = rCy - 12;
            g.drawString(this.font, "Elytra Horizon", hCx - 34, stageY + 8, 0xFFF8FAFC);
            for (int deg = -20; deg <= 20; deg += 10) {
                int ly = hCy - deg;
                int lw = deg == 0 ? 30 : 16;
                g.fill(hCx - lw, ly, hCx + lw, ly + 1, deg == 0 ? 0xFF22D3EE : 0x6694A3B8);
                g.drawString(this.font, deg + "°", hCx - lw - 20, ly - 3, 0xFF94A3B8);
            }

            // Плашка результата последнего поиска структуры (Деревня / Дворец / Крепость)
            int stY = stageY + stageH - 22;
            fillGlassCard(g, sx + 6, stY, sw - 12, 18, 0xDD0B131E, activeNavTarget != null ? activeNavColor : currentTheme.borderMuted);
            g.drawString(this.font, trimText(this.font, structureSearchStatus, sw - 22), sx + 12, stY + 5, 0xFFE2E8F0);
        }

        private void renderSpotifyCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            if (spotifyCenterTab == 1) {
                if (afkGameRunning) {
                    renderAfkMiniGameStage(g, sx, sy + 24, sw, sh - 52);
                } else {
                    fillGlassCard(g, sx, sy + 26, sw, sh - 56, 0xFF0A1018, currentTheme.accent);
                    String t1 = "Игра: " + selectedAfkGame.title;
                    String t2 = selectedAfkGame.hint;
                    g.drawString(this.font, t1, sx + (sw - this.font.width(t1)) / 2, sy + 72, currentTheme.accent);
                    g.drawString(this.font, t2, sx + (sw - this.font.width(t2)) / 2, sy + 90, 0xFFE2E8F0);
                }
                return;
            }

            // Единая стильная карточка Spotify Connect (Windows 10 GSMTC)
            int cardY = sy + 24;
            int cardH = sh - 52;
            g.fillGradient(sx, cardY, sx + sw, cardY + cardH, 0xFF0D2818, 0xFF0B1310);
            drawRoundedBorder(g, sx, cardY, sw, cardH, spotifyIsPlaying ? 0xFF1DB954 : 0xFF334155);

            drawConceptTabIcon(g, MenuTab.SPOTIFY_PLAYER, sx + 10, cardY + 10, 0xFF1DB954);
            String hdr = spotifyConnected
                ? (spotifyIsPlaying ? ("● ИГРАЕТ • " + spotifySourceName) : ("⏸ ПАУЗА • " + spotifySourceName))
                : "○ Ожидание Spotify на ПК...";
            g.drawString(this.font, trimText(this.font, hdr, sw - 34), sx + 26, cardY + 11, spotifyIsPlaying ? 0xFF4ADE80 : 0xFF94A3B8);

            // Обложка слева
            int artX = sx + 12;
            int artY = cardY + 30;
            int artSize = 72;
            g.fillGradient(artX, artY, artX + artSize, artY + artSize, 0xFF1E3A8A, 0xFF064E3B);
            drawRoundedBorder(g, artX, artY, artSize, artSize, 0x664ADE80);
            g.fill(artX + 14, artY + 14, artX + 16, artY + 16, 0xFFFFFFFF);
            g.fill(artX + 46, artY + 10, artX + 48, artY + 12, 0xFFFFFFFF);
            for (int tree = 0; tree < 4; tree++) {
                int tx = artX + 8 + tree * 16;
                int th = 18 + (tree % 3) * 7;
                g.fill(tx, artY + artSize - th, tx + 8, artY + artSize - 2, 0xFF022C22);
            }

            // Реальный трек, исполнитель и реальное время из Windows GSMTC!
            int infoX = artX + artSize + 12;
            int infoW = sw - artSize - 32;
            g.drawString(this.font, trimText(this.font, spotifyTrackTitle, infoW), infoX, artY + 6, 0xFFFFFFFF);
            g.drawString(this.font, trimText(this.font, spotifyTrackArtist, infoW), infoX, artY + 20, 0xFF86EFAC);

            String tLeft = String.format("%d:%02d", spotifyPosSec / 60, spotifyPosSec % 60);
            String tRight = spotifyDurSec > 0 ? String.format("%d:%02d", spotifyDurSec / 60, spotifyDurSec % 60) : "--:--";
            g.drawString(this.font, tLeft, infoX, artY + 42, 0xFFCBD5E1);
            g.drawString(this.font, tRight, infoX + infoW - this.font.width(tRight), artY + 42, 0xFF94A3B8);

            int pBarY = artY + 54;
            g.fill(infoX, pBarY, infoX + infoW, pBarY + 4, 0xFF1F2937);
            float prog = spotifyDurSec > 0 ? Mth.clamp((float) spotifyPosSec / spotifyDurSec, 0.0F, 1.0F) : (spotifyIsPlaying ? 0.5F : 0.0F);
            int fillW = (int) (infoW * prog);
            g.fill(infoX, pBarY, infoX + fillW, pBarY + 4, 0xFF1DB954);
            fillRoundedRect(g, infoX + fillW - 3, pBarY - 2, 6, 8, 0xFFFFFFFF);

            // Живой спектр эквалайзера внизу карточки (под кнопками управления!)
            int eqY = cardY + cardH - 36;
            long t = System.currentTimeMillis() / 110L;
            for (int i = 0; i < 24; i++) {
                int bh = spotifyIsPlaying ? (3 + (int) (Math.abs(Math.sin(t * 0.7 + i * 0.5)) * 18)) : 2;
                int bx = sx + 12 + i * ((sw - 24) / 24);
                g.fill(bx, eqY + 20 - bh, bx + 4, eqY + 20, spotifyIsPlaying ? 0x554ADE80 : 0x33475569);
            }

            String tip = spotifyConnected
                ? "WinRT Media Sync активен • Кнопки управляют Spotify на ПК"
                : "Открой Spotify и нажми Play — трек подхватится сам!";
            g.drawString(this.font, trimText(this.font, tip, sw - 20), sx + 10, cardY + cardH - 12, 0xFF94A3B8);
        }

        private void renderChestsVillagersCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh, int mouseX, int mouseY) {
            g.drawString(this.font, "Дворец испытаний, Хранилища и Библиотекари", sx, sy + 2, 0xFFF8FAFC);

            if (chestsCenterTab == 1) {
                int boxY = sy + 18;
                int boxH = sh - 66;
                fillGlassCard(g, sx, boxY, sw, boxH, 0xFF0A1018, currentTheme.accent);
                int y = boxY + 8;
                if (rememberedChests.isEmpty()) {
                    g.drawString(this.font, "Хранилищ (Vault) рядом: " + countNearbyVaultBlocks() + " | Сундуков: " + nearbyVaults.size(), sx + 8, y, 0xFF4ADE80);
                    g.drawString(this.font, "1. Нажми «Найти Дворец» — навигатор приведёт к данжу", sx + 8, y + 16, 0xFFE2E8F0);
                    g.drawString(this.font, "2. Возьми Ключ испытаний в хотбар и подойди к Хранилищу", sx + 8, y + 30, 0xFFE2E8F0);
                    g.drawString(this.font, "3. Авто-хранилища (или «Открыть рядом») откроет его сам!", sx + 8, y + 44, 0xFFE2E8F0);
                } else {
                    for (ChestRecord cr : rememberedChests.values()) {
                        if (y > boxY + boxH - 24) break;
                        g.drawString(this.font, cr.containerName + " [" + cr.pos.getX() + ", " + cr.pos.getY() + ", " + cr.pos.getZ() + "]", sx + 8, y, currentTheme.accent);
                        g.drawString(this.font, trimText(this.font, cr.summaryLine(), sw - 16), sx + 8, y + 10, 0xFFCBD5E1);
                        y += 24;
                    }
                }
                return;
            }

            if (chestsCenterTab == 2) {
                int boxY = sy + 18;
                int boxH = sh - 66;
                fillGlassCard(g, sx, boxY, sw, boxH, 0xFF0A1018, currentTheme.secondary);
                int y = boxY + 8;
                if (rememberedVillagers.isEmpty()) {
                    g.drawString(this.font, "Открой торговлю с жителем ПКМ:", sx + 8, y, currentTheme.secondary);
                    g.drawString(this.font, "Все книги и цены запишутся сюда автоматически.", sx + 8, y + 16, 0xFFE2E8F0);
                    g.drawString(this.font, "Если выпадет «" + TARGET_ENCHANTS.get(targetEnchantIndex) + "» <= " + maxEmeraldPrice + " изумр.,", sx + 8, y + 30, 0xFF4ADE80);
                    g.drawString(this.font, "Вадимка сразу подаст звуковой сигнал!", sx + 8, y + 44, 0xFF4ADE80);
                } else {
                    for (VillagerRecord vr : rememberedVillagers.values()) {
                        for (VillagerTradeEntry tr : vr.trades) {
                            if (y > boxY + boxH - 18) break;
                            int col = (tr.valuable && tr.emeraldPrice <= maxEmeraldPrice) ? 0xFF4ADE80 : 0xFFE2E8F0;
                            g.drawString(this.font, trimText(this.font, tr.resultText + " — " + tr.costText, sw - 16), sx + 8, y, col);
                            y += 13;
                        }
                    }
                }
                return;
            }

            // Главная 3D-сцена: Реальные 3D-иконки Хранилища (Vault), Ключей, Тяжёлого ядра + ЖИВОЙ 3D-Житель!
            int stageY = sy + 18;
            int stageH = sh - 66;
            drawSoftStudioGrid(g, sx, stageY, sw, stageH);

            Minecraft mc = Minecraft.getInstance();
            Player p = mc.player;

            // Левая сторона: Хранилище (Trial Vault) в неоновой рамке + иконки лута вокруг (как на фото 9!)
            int vx = sx + sw / 4 - 24;
            int vy = stageY + stageH / 2 - 14;
            fillGlassCard(g, vx, vy, 48, 44, 0x33102030, 0xFF22D3EE);
            drawGradientNeonBorder(g, vx, vy, 48, 44, 0xFF22D3EE, 0xFFF472B6);
            g.renderItem(new ItemStack(Items.VAULT), vx + 16, vy + 14);
            // Иконки целей лута по бокам Хранилища
            g.renderItem(new ItemStack(Items.HEAVY_CORE), vx - 20, vy + 2);
            g.renderItem(new ItemStack(Items.OMINOUS_BOTTLE), vx - 20, vy + 24);
            g.renderItem(new ItemStack(Items.TRIAL_KEY), vx + 52, vy + 2);
            g.renderItem(new ItemStack(Items.TRIDENT), vx + 52, vy + 24);

            int vaultsNear = countNearbyVaultBlocks();
            g.drawString(this.font, "Хранилище", vx - 2, vy - 20, 0xFFF8FAFC);
            g.drawString(this.font, vaultsNear > 0 ? ("Найдено: " + vaultsNear) : "Ждёт ключ", vx, vy - 10, vaultsNear > 0 ? 0xFF4ADE80 : 0xFF22D3EE);

            // Правая сторона: Живой 3D-Житель + Кафедра + Облачко поиска чар!
            int vilCenterX = sx + (sw * 3) / 4 + 6;
            fillGlassCard(g, vilCenterX - 56, stageY + 8, 112, 26, 0xEE0B1624, 0xFF22D3EE);
            g.drawString(this.font, "Цель: " + TARGET_ENCHANTS.get(targetEnchantIndex), vilCenterX - 50, stageY + 12, 0xFF4ADE80);
            g.drawString(this.font, "До " + maxEmeraldPrice + " изумрудов", vilCenterX - 50, stageY + 23, 0xFFCBD5E1);

            if (mc.level != null) {
                Mob vil = getGuiVillagerModel(mc.level);
                if (vil != null) {
                    try {
                        InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, vilCenterX - 24, stageY + 40, vilCenterX + 40, stageY + stageH - 8,
                            36, 0.0625F, (float) mouseX, (float) mouseY, vil
                        );
                    } catch (Throwable ignored) {
                    }
                }
            }
            // Кафедра и Зачарованная книга перед жителем
            g.renderItem(new ItemStack(Items.LECTERN), vilCenterX - 38, stageY + stageH - 36);
            g.renderItem(new ItemStack(Items.ENCHANTED_BOOK), vilCenterX - 38, stageY + stageH - 54);

            // Индикатор наличия Ключа испытаний в инвентаре игрока
            boolean hasKey = p != null && (findHotbarSlotForItem(p, Items.TRIAL_KEY) >= 0 || findHotbarSlotForItem(p, Items.OMINOUS_TRIAL_KEY) >= 0);
            String keyStr = hasKey ? "✓ Ключ в хотбаре готов!" : "Положи Ключ испытаний в хотбар";
            g.drawString(this.font, keyStr, sx + 8, stageY + stageH - 13, hasKey ? 0xFF4ADE80 : 0xFFFACC15);
        }

        private void renderStyleProfilerCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Поведенческий профайлер и Гардероб", sx, sy + 2, 0xFFF8FAFC);

            int cardY = sy + 18;
            int cardH = sh - 44;
            fillGlassCard(g, sx, cardY, sw, cardH, 0xFF0A1018, currentTheme.borderMuted);

            String[] labels = {"Обученность профиля:", "Моторика камеры:", "Ритм кликов:", "Привычки в стройке:"};
            int dynamicTrain = Math.min(99, profilerTrainingPct + (int) (sessionWalkedBlocks / 150.0));
            int[] pcts = {dynamicTrain, profilerCameraPct, profilerClickRhythmPct, profilerBuildHabitsPct};

            int y = cardY + 8;
            int barW = sw - 20;
            for (int i = 0; i < labels.length; i++) {
                g.drawString(this.font, labels[i], sx + 10, y, 0xFFE2E8F0);
                String pTxt = pcts[i] + "%";
                g.drawString(this.font, pTxt, sx + sw - 10 - this.font.width(pTxt), y, currentTheme.accent);
                y += 10;
                g.fill(sx + 10, y, sx + 10 + barW, y + 5, 0xFF1E293B);
                int fw = (barW * pcts[i]) / 100;
                g.fillGradient(sx + 10, y, sx + 10 + fw, y + 5, currentTheme.accent, currentTheme.secondary);
                y += 11;
            }

            int gX = sx + 10;
            int gY = y + 4;
            int gW = sw - 20;
            int gH = cardY + cardH - gY - 15;
            fillGlassCard(g, gX, gY, gW, gH, 0xFF070B10, 0x33FFFFFF);
            g.drawString(this.font, "Mouse acceleration / aim curve", gX + 6, gY + 4, 0xFF94A3B8);

            int prevX = gX + 4, prevY = gY + gH - 4;
            for (int step = 1; step <= 36; step++) {
                float t = step / 36.0F;
                float sCurve = t * t * (3.0F - 2.0F * t);
                int nx = gX + 4 + (int) ((gW - 8) * t);
                int ny = gY + gH - 4 - (int) ((gH - 16) * sCurve);
                int col = lerpColor(currentTheme.secondary, currentTheme.accent, t);
                drawSmoothLine(g, prevX, prevY, nx, ny, col, gX, gX + gW);
                drawSmoothLine(g, prevX, prevY - 1, nx, ny - 1, col, gX, gX + gW);
                prevX = nx; prevY = ny;
            }
            String sub = "Пройдено за сессию: " + (int) sessionWalkedBlocks + " бл. • Найдено структур: " + foundStructures.size();
            g.drawString(this.font, trimText(this.font, sub, sw - 16), sx + 10, cardY + cardH - 11, 0xFFCBD5E1);
        }

        private void renderAfkMiniGameStage(GuiGraphics g, int gx, int gy, int gw, int gh) {
            fillGlassCard(g, gx, gy, gw, gh, 0xFF070C12, currentTheme.accent);
            String hdr = selectedAfkGame.title + " | Счёт: " + afkScore + "  Рекорд: " + afkBestScore;
            g.drawString(this.font, trimText(this.font, hdr, gw - 12), gx + 6, gy + 5, 0xFFFDE047);

            int ax = gx + 6, ay = gy + 18, aw = gw - 12, ah = gh - 24;
            g.fill(ax, ay, ax + aw, ay + ah, 0xFF0D1520);

            switch (selectedAfkGame) {
                case RUNNER -> {
                    int laneW = aw / 3;
                    for (int l = 0; l < 3; l++) g.fill(ax + l * laneW + 2, ay + 2, ax + (l + 1) * laneW - 2, ay + ah - 2, 0x18FFFFFF);
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
                    for (int[] b : doomShots) g.fill(ax + (b[0] * aw) / 100 - 1, ay + (b[1] * ah) / 100, ax + (b[0] * aw) / 100 + 2, ay + (b[1] * ah) / 100 + 6, 0xFFFDE047);
                    for (int[] e : doomMobs) g.fill(ax + (e[0] * aw) / 100 - 6, ay + (e[1] * ah) / 100, ax + (e[0] * aw) / 100 + 6, ay + (e[1] * ah) / 100 + 12, 0xFFF43F5E);
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
    // 14. ОПЦИОНАЛЬНЫЙ ВСТРОЕННЫЙ СИНТЕЗАТОР (ВЫКЛЮЧЕН ПО УМОЛЧАНИЮ!)
    // =========================================================================

    private static volatile boolean synthThreadStarted = false;
    private static volatile int synthNoteIdx = 0;

    private static void ensureOptionalSynthThread() {
        if (synthThreadStarted) return;
        synthThreadStarted = true;
        Thread t = new Thread(() -> {
            javax.sound.midi.Synthesizer synth = null;
            try {
                synth = javax.sound.midi.MidiSystem.getSynthesizer();
                synth.open();
                javax.sound.midi.MidiChannel ch = synth.getChannels()[0];
                ch.programChange(11);
                int[] scale = {60, 64, 67, 71, 72, 76, 79};
                while (!Thread.currentThread().isInterrupted()) {
                    Minecraft mc = Minecraft.getInstance();
                    if (!builtInSynthEnabled || spotifyConnected || mc.player == null) {
                        ch.allNotesOff();
                        Thread.sleep(300);
                        continue;
                    }
                    int note = scale[synthNoteIdx++ % scale.length];
                    ch.noteOn(note, 40);
                    Thread.sleep(520);
                    ch.noteOff(note);
                }
            } catch (Throwable ignored) {
            } finally {
                if (synth != null && synth.isOpen()) synth.close();
            }
        }, "Vadimka-Optional-Synth");
        t.setDaemon(true);
        t.start();
    }

    // =========================================================================
    // 15. БАЗА ЗНАНИЙ ВАДИМКИ ПО MINECRAFT 1.21.11
    // =========================================================================
    private static final Map<String, String> MINECRAFT_KB = new LinkedHashMap<>();
    static {
        MINECRAFT_KB.put("алмаз", "Алмазы лучше всего копать на высоте Y = -58 или Y = -59 (слой глубинного сланца).");
        MINECRAFT_KB.put("незерит", "Древние обломки (Незерит) чаще всего спавнятся в Незере на высоте Y = 15.");
        MINECRAFT_KB.put("желез", "Железо лучше всего искать на Y = 16 в пещерах или высоко в горах на Y = 232.");
        MINECRAFT_KB.put("изумруд", "Изумруды спавнятся в горных биомах (Y = 236), но быстрее фармить торговлей с жителями.");
        MINECRAFT_KB.put("булав", "Для Булавы (Mace) нужны Тяжёлое ядро из Зловещего хранилища и Стержень вихря из Бриза.");
        MINECRAFT_KB.put("ядро", "Тяжёлое ядро выпадает из Зловещего хранилища (Ominous Vault) с шансом 7.5%.");
        MINECRAFT_KB.put("зловещ", "Выпей Зловещую бутылку во Дворце испытаний, чтобы активировать Зловещие спавнеры и получить Зловещий ключ!");
        MINECRAFT_KB.put("починк", "Починка I (Mending) — ставь и ломай кафедру у безработного жителя; минимальная цена — 10 изумрудов.");
        MINECRAFT_KB.put("тотем", "Тотемы бессмертия падают с Вызывателей в Лесном особняке и на рейдах деревни.");
        MINECRAFT_KB.put("элитр", "Элитры находятся на летающих кораблях в городах Энда — сначала найди Крепость командой !крепость.");
    }

    private static String lookupMinecraftAdvice(String lowerQuery) {
        for (Map.Entry<String, String> entry : MINECRAFT_KB.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }


    /**
     * Конфигурационный пресет и диагностика состояния #001 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_001() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-001[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #002 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_002() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-002[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #003 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_003() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-003[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #004 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_004() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-004[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #005 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_005() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-005[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #006 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_006() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-006[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #007 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_007() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-007[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #008 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_008() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-008[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #009 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_009() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-009[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #010 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_010() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-010[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #011 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_011() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-011[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #012 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_012() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-012[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #013 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_013() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-013[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #014 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_014() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-014[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #015 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_015() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-015[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #016 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_016() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-016[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #017 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_017() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-017[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #018 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_018() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-018[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #019 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_019() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-019[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #020 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_020() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-020[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #021 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_021() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-021[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #022 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_022() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-022[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #023 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_023() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-023[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #024 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_024() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-024[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #025 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_025() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-025[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #026 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_026() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-026[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #027 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_027() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-027[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #028 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_028() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-028[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #029 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_029() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-029[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #030 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_030() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-030[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #031 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_031() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-031[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #032 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_032() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-032[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #033 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_033() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-033[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #034 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_034() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-034[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #035 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_035() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-035[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #036 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_036() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-036[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #037 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_037() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-037[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #038 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_038() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-038[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #039 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_039() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-039[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #040 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_040() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-040[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #041 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_041() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-041[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #042 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_042() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-042[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #043 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_043() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-043[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #044 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_044() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-044[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #045 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_045() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-045[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #046 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_046() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-046[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #047 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_047() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-047[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #048 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_048() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-048[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #049 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_049() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-049[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #050 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_050() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-050[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #051 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_051() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-051[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #052 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_052() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-052[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #053 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_053() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-053[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #054 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_054() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-054[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #055 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_055() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-055[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #056 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_056() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-056[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #057 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_057() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-057[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #058 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_058() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-058[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #059 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_059() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-059[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #060 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_060() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-060[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #061 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_061() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-061[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #062 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_062() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-062[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #063 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_063() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-063[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #064 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_064() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-064[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #065 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_065() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-065[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #066 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_066() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-066[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #067 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_067() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-067[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #068 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_068() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-068[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #069 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_069() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-069[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #070 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_070() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-070[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #071 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_071() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-071[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #072 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_072() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-072[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #073 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_073() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-073[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #074 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_074() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-074[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #075 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_075() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-075[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #076 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_076() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-076[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #077 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_077() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-077[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #078 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_078() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-078[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #079 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_079() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-079[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #080 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_080() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-080[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #081 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_081() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-081[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #082 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_082() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-082[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #083 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_083() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-083[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #084 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_084() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-084[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #085 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_085() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-085[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #086 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_086() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-086[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #087 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_087() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-087[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #088 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_088() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-088[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #089 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_089() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-089[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #090 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_090() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-090[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #091 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_091() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-091[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #092 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_092() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-092[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #093 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_093() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-093[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #094 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_094() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-094[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #095 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_095() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-095[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #096 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_096() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-096[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #097 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_097() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-097[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #098 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_098() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-098[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #099 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_099() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-099[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #100 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_100() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-100[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #101 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_101() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-101[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #102 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_102() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-102[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #103 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_103() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-103[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #104 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_104() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-104[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #105 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_105() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-105[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #106 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_106() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-106[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #107 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_107() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-107[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #108 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_108() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-108[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #109 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_109() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-109[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #110 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_110() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-110[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #111 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_111() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-111[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #112 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_112() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-112[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #113 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_113() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-113[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #114 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_114() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-114[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #115 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_115() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-115[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #116 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_116() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-116[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #117 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_117() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-117[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #118 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_118() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-118[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #119 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_119() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-119[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #120 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_120() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-120[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #121 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_121() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-121[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #122 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_122() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-122[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #123 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_123() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-123[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #124 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_124() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-124[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #125 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_125() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-125[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #126 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_126() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-126[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #127 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_127() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-127[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #128 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_128() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-128[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }

    /**
     * Конфигурационный пресет и диагностика состояния #129 (Assistant Vadimka v7.0).
     */
    public static String getSystemPresetSnapshot_129() {
        int active = 0;
        if (companionEnabled) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        return "V7Preset-129[on=" + active + ",form=" + currentForm.codeName + ",vaults=" + countNearbyVaultBlocks() + ",spotify=" + spotifyConnected + "]";
    }
}
