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
import net.minecraft.world.inventory.ClickType;
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
import net.minecraft.world.level.block.entity.BlockEntity;
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
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
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
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;

/**
 * ============================================================================
 * Assistant Vadimka v8.0 TITAN EDITION (Fabric 1.21.11 / Java 25)
 * ============================================================================
 * Полный комбайн-помощник с ультра-оптимизацией (75+ FPS):
 * 1. GPU DynamicTexture 128x128 Реалистичная Мини-карта (1 вызов GPU за кадр!)
 *    с рельефным затенением склонов, глубиной воды, контурами высот и сонаром.
 * 2. Умный Безопасный Автопилот: реально ведёт игрока к Дому, Деревне, Дворцу
 *    испытаний, Ларчику или Жителю, сам перепрыгивает преграды и воду, обходит
 *    лаву/обрывы и мгновенно отдаёт управление по S/A/D/Shift.
 * 3. Снайпер Хранилищ Дворца испытаний (Vault Loot Sniper): считывает предмет,
 *    который прямо сейчас крутится внутри VaultBlockEntity, ЖДЁТ нужный лут
 *    (например «Вадимка выбей из хранилища трезубец» или Тяжёлое ядро), сам
 *    берёт нужный ключ (обычный/зловещий) и открывает в точный момент!
 * 4. Авто-перебор кафедры Библиотекаря (Auto-Cycle Lectern): реально ломает
 *    кафедру топором (с защитой от поломки топора <= 10), ставит обратно,
 *    открывает торги, проверяет чары и цену <= макс. изумрудов и сигналит!
 * 5. Реальный Windows 10 Spotify Connect (WinRT GSMTC) + управление треками.
 * 6. Поиск 8 структур мира, 6 кастомных 3D-обликов Вадимки, 4 умных миньона,
 *    Litematica HUD, Боевой PvP HUD, 3D-Гардероб и 3 AFK мини-игры.
 */
public class AssistantVadimkaClient implements ClientModInitializer {
    public static final String MOD_ID = "assistant_vadimka";
    public static final String VERSION_LABEL = "v8.0";

    // =========================================================================
    // 1. ПЕРЕЧИСЛЕНИЯ ВСЕХ ПОДСИСТЕМ
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
        COMPANION("Вадимка", "Спутник Вадимка, 3D-позиция и Чат"),
        AUTOPILOT("Автопилот", "Безопасный Автопилот и Профайлер"),
        AUTO_BUILD("Постройка", "Авто-постройка, Хотбар и Линейка"),
        LITEMATICA_HUD("Litematica", "Litematica HUD — Ресурсы и слои"),
        COMBAT_PVP("Бой и PvP", "Бой и PvP — Траектории и Броня"),
        VISUAL_RADAR("Радар", "GPU Карта, Сонар и Поиск Структур"),
        SPOTIFY_PLAYER("Spotify", "Spotify Connect (ПК) и AFK-игры"),
        CHESTS_VILLAGERS("Ларчики", "Снайпер Хранилищ и Перебор Жителей"),
        CUSTOM_VISUAL("Кастом", "3D-Гардероб, Крылья, Прицел и Темы");

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

    public enum LecternCycleState {
        IDLE("Выключен"),
        OPEN_VILLAGER("Открываю торги"),
        CHECK_TRADES("Проверяю книги"),
        BREAK_LECTERN("Ломаю кафедру"),
        WAIT_PICKUP("Подбираю кафедру"),
        PLACE_LECTERN("Ставлю кафедру"),
        WAIT_JOB("Жду профессию");

        public final String label;

        LecternCycleState(String label) {
            this.label = label;
        }
    }

    // =========================================================================
    // 2. СТРУКТУРЫ ДАННЫХ
    // =========================================================================

    public static class VaultInfo {
        public final BlockPos pos;
        public final boolean isVaultBlock;
        public final boolean ominous;
        public final boolean active;
        public final String stateName;
        public ItemStack displayItem;

        public VaultInfo(BlockPos pos, boolean isVaultBlock, boolean ominous, boolean active, String stateName, ItemStack displayItem) {
            this.pos = pos;
            this.isVaultBlock = isVaultBlock;
            this.ominous = ominous;
            this.active = active;
            this.stateName = stateName;
            this.displayItem = displayItem != null ? displayItem : ItemStack.EMPTY;
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

    public static class VaultLootPreset {
        public final String displayName;
        public final String[] matchKeywords;
        public final Item previewIcon;

        public VaultLootPreset(String displayName, Item previewIcon, String... matchKeywords) {
            this.displayName = displayName;
            this.previewIcon = previewIcon;
            this.matchKeywords = matchKeywords;
        }
    }

    // =========================================================================
    // 3. СОСТОЯНИЕ ВСЕХ 9 МОДУЛЕЙ
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
    public static boolean randomChaosMode = false;
    public static boolean voiceCommentator = true;
    public static final Map<MinionRole, Boolean> activeMinions = new EnumMap<>(MinionRole.class);
    public static final Deque<ChatLogEntry> chatHistory = new ArrayDeque<>();
    public static String vadimkaLastBubble = "Напиши «Вадимка выбей из хранилища трезубец» или «!к дому»!";

    private static final Deque<String> pendingChatReplies = new ArrayDeque<>();
    private static int pendingChatReplyDelay = 0;
    private static boolean greetedInWorld = false;

    // --- 3.2 Безопасный Автопилот и Профайлер стиля ---
    public static boolean safeAutopilot = false;
    public static boolean sprintAssist = true;
    public static boolean autoReturnControl = true;
    public static boolean parkourEdgeGuard = true;
    public static boolean fallDangerAlert = true;
    public static boolean autopilotAvoidLavaAndCliffs = true;
    private static boolean autopilotHoldingForward = false;
    private static int autopilotStuckTicks = 0;
    private static Vec3 autopilotLastCheckPos = null;
    private static int autopilotStrafeDir = 0;
    private static int autopilotStrafeTicks = 0;
    private static int fallDropBlocks = 0;
    private static boolean edgeDangerNow = false;

    public static int profilerTrainingPct = 88;
    public static int profilerCameraPct = 93;
    public static int profilerClickRhythmPct = 85;
    public static int profilerBuildHabitsPct = 82;

    // --- 3.3 Авто-постройка и Авто-пополнение хотбара ---
    public static boolean autoBuildLitematica = false;
    public static boolean smoothBezierCamera = true;
    public static boolean hotbarAutoRefill = true;
    public static boolean smartRestoration = true;
    public static boolean redstoneInspector = true;
    public static boolean ruler3DVolume = true;
    public static boolean autoAnvilEnchant = false;
    public static int minBuildDelayMs = 80;
    public static int maxBuildDelayMs = 150;
    public static double buildReachBlocks = 4.5;
    public static BuildMode currentBuildMode = BuildMode.FAST;
    private static Item lastMainHandItem = Items.AIR;
    private static int lastMainHandCount = 0;
    private static int lastRefillTick = -40;

    // --- 3.4 Litematica HUD ---
    public static boolean litematicaHudEnabled = true;
    public static boolean litematicaHudRight = true;
    public static boolean shulkerScanner = true;
    public static boolean hideCompletedMaterials = false;
    public static boolean layerSliceMode = false;
    public static boolean deficitHighlight = true;
    public static int targetBuildLayerY = 64;
    public static int buildPaletteIndex = 0;
    public static final List<String> BUILD_PALETTES = List.of(
        "Камень / Дуб",
        "Дворец (Медь/Туф)",
        "Незерит / Чернит",
        "Кварц / Призмарин"
    );

    // --- 3.5 Бой и PvP ---
    public static boolean combatHudEnabled = true;
    public static boolean trajectories3D = true;
    public static boolean armorAndTotemScanner = true;
    public static boolean shieldAndPearlTimers = true;
    public static boolean consumableCounter = true;
    public static boolean backstabIndicator = true;
    public static boolean deathReplayMarker = true;

    // --- 3.6 Реалистичная GPU-карта, Сонар и Поиск Структур ---
    public static boolean visualsEnabled = true;
    public static boolean minimapRadarEnabled = true;
    public static boolean minimapCircularMode = true;
    public static boolean minimapTerrainEnabled = true;
    public static boolean minimapContourLines = true;
    public static boolean soundRadarSonar = true;
    public static boolean elytraPitchHorizon = true;
    public static boolean highlightChestsWorld = true;
    public static boolean baseChestMemory = true;
    public static boolean ariadneThread = true;
    public static int radarZoomLevel = 0; // 0=1 блок/пикс, 1=2 блока, 2=3 блока

    // --- 3.7 Windows 10 Spotify Connect (WinRT GSMTC) + AFK ---
    public static boolean spotifyHudEnabled = true;
    public static boolean spotifyDuckOnSpeech = false;
    public static boolean vadimkaDanceToBpm = true;
    public static boolean builtInSynthEnabled = false;
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
    private static volatile boolean spotifyPollBusy = false;
    private static String lastNotifiedTrack = "";
    public static int spotifyVolumePercent = 80;

    // --- 3.8 СНАЙПЕР ХРАНИЛИЩ (VAULT LOOT SNIPER) И АВТО-ПЕРЕБОР БИБЛИОТЕКАРЯ ---
    public static boolean highlightTrialVaults = true;
    public static boolean autoClickVaults = false;     // Вкл -> ждёт нужный предмет внутри хранилища!
    public static boolean vaultSmartWaitLoot = true;   // Строго проверяет предмет внутри VaultBlockEntity перед кликом!
    public static int vaultLootGoalIndex = 0;
    public static String customVaultItemQuery = "";    // Сюда пишется произвольный предмет из чата («трезубец», «яблоко» и т.д.)
    public static String lastVaultSpinningLabel = "Подойди к Хранилищу";
    public static boolean lastVaultMatchedNow = false;
    private static int lastVaultInteractTick = -40;
    private static int lastMissingKeyWarnTick = -120;

    public static final List<VaultLootPreset> VAULT_LOOT_PRESETS = List.of(
        new VaultLootPreset("Трезубец", Items.TRIDENT, "трезуб", "trident"),
        new VaultLootPreset("Тяжёлое ядро", Items.HEAVY_CORE, "ядро", "heavy_core", "heavy core"),
        new VaultLootPreset("Зачарованная книга", Items.ENCHANTED_BOOK, "книг", "book", "enchanted_book"),
        new VaultLootPreset("Золотое яблоко", Items.ENCHANTED_GOLDEN_APPLE, "яблок", "apple", "golden_apple"),
        new VaultLootPreset("Зловещая бутылка", Items.OMINOUS_BOTTLE, "бутыл", "ominous_bottle", "bottle"),
        new VaultLootPreset("Алмаз / Блок", Items.DIAMOND, "алмаз", "diamond"),
        new VaultLootPreset("Ветровой заряд", Items.WIND_CHARGE, "ветер", "ветров", "wind_charge"),
        new VaultLootPreset("Любой лут (сразу)", Items.TRIAL_KEY, "*")
    );

    // Авто-перебор кафедры Библиотекаря (реальный state machine!)
    public static boolean autoCycleLectern = false;
    public static boolean warnAxeDurability = true;
    public static int targetEnchantIndex = 0;
    public static int maxEmeraldPrice = 14;
    public static final List<String> TARGET_ENCHANTS = List.of(
        "Починка",
        "Неразрушимость",
        "Эффективность",
        "Острота",
        "Защита",
        "Удача",
        "Добыча",
        "Шёлковое касание"
    );

    private static LecternCycleState lecternState = LecternCycleState.IDLE;
    private static BlockPos lockedLecternPos = null;
    private static int lockedVillagerId = -1;
    private static int lecternStateTimer = 0;
    private static int lecternAttempts = 0;
    private static String lecternStatusMessage = "Перебор выключен";
    private static int lastAxeWarnTick = -300;

    // --- 3.9 Кастом-визуал, 3D-Гардероб и Темы ---
    public static MenuTheme currentTheme = MenuTheme.CONCEPT_CYAN;
    public static final List<String> WINGS_OPTIONS = List.of(
        "Выкл", "Ангельские", "Демонические", "Неон-элитры", "Кристальные"
    );
    public static final List<String> HORNS_OPTIONS = List.of(
        "Выкл", "Дьявольские", "Оленьи", "Кибер-рожки", "Золотой нимб"
    );
    public static final List<String> HATS_OPTIONS = List.of(
        "Выкл", "Корона", "Незерит-шлем", "Колпак", "Ушки Крыски"
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
    // 4. GPU DYNAMIC TEXTURE ДВИЖОК РЕАЛИСТИЧНОЙ МИНИ-КАРТЫ (75+ FPS!)
    // =========================================================================

    private static final int TERRAIN_GRID = 64;
    private static final int TERRAIN_HALF = TERRAIN_GRID / 2;
    private static final int[] terrainRgb = new int[TERRAIN_GRID * TERRAIN_GRID];
    private static final int[] terrainHeight = new int[TERRAIN_GRID * TERRAIN_GRID];
    private static final boolean[] terrainWater = new boolean[TERRAIN_GRID * TERRAIN_GRID];
    private static int terrainOriginX = Integer.MIN_VALUE;
    private static int terrainOriginZ = Integer.MIN_VALUE;
    private static int terrainLastZoom = -1;
    private static int terrainScanRow = 0;
    private static boolean terrainNeedsReset = true;

    private static final int TEX_SIZE = 128;
    private static boolean gpuMapInitAttempted = false;
    private static boolean gpuMapReady = false;
    private static Object gpuNativeImage = null;
    private static Object gpuDynamicTexture = null;
    private static Object gpuTextureId = null;
    private static Object gpuRenderPipeline = null;
    private static Method nativeSetPixelMethod = null;
    private static boolean nativeSetPixelIsArgb = true;
    private static Method dynamicUploadMethod = null;
    private static Method guiBlitMethod = null;
    private static int guiBlitStyle = 0; // 1 = (RenderPipeline, Id, x, y, float u, float v, w, h, texW, texH), 2 = (Id, x, y, u, v, w, h, texW, texH)
    private static long lastGpuTextureUploadMs = 0L;
    private static float lastRenderedMapYaw = -9999.0F;
    private static boolean terrainDirtyForGpu = true;

    // =========================================================================
    // 5. КЭШИ МИРА, СУЩНОСТЕЙ, ХРАНИЛИЩ И СТРУКТУР
    // =========================================================================

    private static KeyMapping openMenuKey;
    private static int worldTick = 0;

    private static Mob companionCoreEntity = null;
    private static ArmorStand companionVisorStand = null;
    private static VadimkaForm spawnedForm = null;
    private static int spawnedScale = -1;
    private static final Map<MinionRole, Mob> minionEntities = new EnumMap<>(MinionRole.class);
    private static final Map<VadimkaForm, LivingEntity> guiStageModels = new EnumMap<>(VadimkaForm.class);
    private static Mob guiVillagerModel = null;

    public static BlockPos homeWaypoint = null;
    public static BlockPos activeNavTarget = null;
    public static String activeNavTitle = "";
    public static int activeNavColor = 0xFF22D3EE;
    private static BlockPos lastDeathPos = null;
    public static final Map<StructureTarget, FoundStructureRecord> foundStructures = new EnumMap<>(StructureTarget.class);
    private static volatile boolean structureSearchBusy = false;
    private static volatile String structureSearchStatus = "Готов к поиску структур";

    public static final Map<BlockPos, ChestRecord> rememberedChests = new LinkedHashMap<>();
    public static final Map<BlockPos, VillagerRecord> rememberedVillagers = new LinkedHashMap<>();
    private static BlockPos lastLookedContainerPos = null;
    private static String lastLookedContainerName = "Ларчик";
    private static boolean wasContainerOpen = false;
    private static boolean wasMerchantOpen = false;

    // Облегчённый сканер Хранилищ и Сундуков (радиус 16, Y ±6, 1 срез раз в 3 тика = 0 лагов!)
    private static final int VAULT_SCAN_RADIUS = 16;
    private static final List<VaultInfo> nearbyVaults = new ArrayList<>();
    private static final List<VaultInfo> scanVaultBuffer = new ArrayList<>();
    private static int vaultScanSliceX = -VAULT_SCAN_RADIUS;
    private static BlockPos vaultScanOrigin = BlockPos.ZERO;

    private static class RadarBlip {
        final double x, z;
        final int kind; // 0=player, 1=hostile, 2=animal, 3=villager, 4=item
        final String name;

        RadarBlip(double x, double z, int kind, String name) {
            this.x = x; this.z = z; this.kind = kind; this.name = name;
        }
    }

    private static final List<RadarBlip> radarBlips = new ArrayList<>();
    private static Vec3 nearestLootPos = null;
    private static Vec3 nearestHostilePos = null;
    private static double nearestHostileDist = 999.0;
    private static double nearestHostileAngle = 0.0;
    private static boolean threatBehindPlayer = false;

    private static final Deque<Vec3> ariadnePoints = new ArrayDeque<>();
    private static Vec3 lastAriadnePoint = null;
    private static double sessionWalkedBlocks = 0.0;
    private static Vec3 prevPlayerPos = null;
    private static float prevPlayerHealth = 20.0F;
    private static String toastBannerText = "";
    private static int toastBannerTicks = 0;
    private static final Random rng = new Random();

    // =========================================================================
    // 6. ИНИЦИАЛИЗАЦИЯ И AI-ЧАТ ВАДИМКИ (ХРАНИЛИЩА, ЖИТЕЛИ, АВТОПИЛОТ, СТРУКТУРЫ)
    // =========================================================================

    @Override
    public void onInitializeClient() {
        for (MinionRole role : MinionRole.values()) {
            activeMinions.put(role, true);
        }
        ensureOptionalSynthThread();
        pushChatLog(true, "Привет! Я Вадимка v8.0. Пиши: «Вадимка выбей из хранилища трезубец», «!к дому», «!перебор починка», «!деревня», «!испытания»!");

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.assistant_vadimka.menu",
            GLFW.GLFW_KEY_P,
            KeyMapping.Category.MISC
        ));

        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!chatAssistantEnabled || message == null) return true;
            String trimmed = message.trim();
            if (trimmed.isEmpty()) return true;
            if (trimmed.startsWith("!")) {
                String cmd = trimmed.substring(1).trim();
                pushChatLog(false, trimmed);
                enqueueChatReply(handleVadimkaCommand(cmd), 2);
                return false;
            }
            String low = trimmed.toLowerCase(Locale.ROOT);
            if (low.contains("вадимк") || low.contains("вадим") || low.contains("vadimka")) {
                pushChatLog(false, trimmed);
                enqueueChatReply(handleVadimkaCommand(trimmed), 4);
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) {
                releaseAutopilotKeys(client);
                stopLecternCycle("Выход из мира");
                cleanupWorldCompanions();
                ariadnePoints.clear();
                nearbyVaults.clear();
                scanVaultBuffer.clear();
                radarBlips.clear();
                guiStageModels.clear();
                guiVillagerModel = null;
                lastAriadnePoint = null;
                prevPlayerPos = null;
                terrainNeedsReset = true;
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
        while (chatHistory.size() > 32) chatHistory.removeFirst();
        if (fromVadimka) vadimkaLastBubble = text;
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

        if (low.isEmpty() || low.contains("привет") || low.contains("ку") || low.equals("тут")) {
            return "На связи! Могу выбить нужный лут из хранилища («выбей трезубец»), перебирать торги библиотекаря («!перебор починка»), вести на автопилоте домой («!к дому») или найти структуры («!деревня», «!испытания»)!";
        }
        if (low.contains("помощ") || low.contains("команд") || low.contains("help")) {
            return "Команды: «Вадимка выбей из хранилища трезубец», «!перебор починка», «!дом», «!к дому» (автопилот), «!стоп», «!деревня», «!испытания», «!крепость», «!трек», «!пауза», «!следующий», «!найди <вещь>»!";
        }

        // --- 1. СНАЙПЕР ХРАНИЛИЩ: "Вадимка выбей из хранилища трезубец / ядро / книгу / яблоко" ---
        if (low.contains("выбей") || low.contains("достань") || low.contains("вытащи") || low.contains("выбить") || low.contains("снайпер")) {
            String itemPart = low
                .replace("выбей", "")
                .replace("выбить", "")
                .replace("достань", "")
                .replace("вытащи", "")
                .replace("снайпер", "")
                .replace("из хранилища", "")
                .replace("из ларчика", "")
                .replace("хранилища", "")
                .replace("хранилище", "")
                .replace("ларчика", "")
                .replace("ларчик", "")
                .replace("мне", "")
                .replace("пожалуйста", "")
                .replace("пж", "")
                .trim();

            if (itemPart.contains("стоп") || itemPart.contains("отмен") || itemPart.contains("выкл")) {
                autoClickVaults = false;
                customVaultItemQuery = "";
                return "Остановил снайпер Хранилищ.";
            }

            if (itemPart.isEmpty()) {
                autoClickVaults = true;
                vaultSmartWaitLoot = true;
                return "Снайпер Хранилищ ВКЛ! Жду «" + getActiveVaultTargetLabel() + "». Подойди к Хранилищу с ключом в хотбаре — я кликну ровно в нужный момент!";
            }

            // Ищем совпадение среди пресетов или ставим произвольный запрос!
            int matchedPreset = -1;
            for (int i = 0; i < VAULT_LOOT_PRESETS.size(); i++) {
                VaultLootPreset preset = VAULT_LOOT_PRESETS.get(i);
                for (String kw : preset.matchKeywords) {
                    if (!"*".equals(kw) && itemPart.contains(kw)) {
                        matchedPreset = i;
                        break;
                    }
                }
                if (matchedPreset >= 0) break;
            }
            if (matchedPreset >= 0) {
                vaultLootGoalIndex = matchedPreset;
                customVaultItemQuery = "";
            } else {
                customVaultItemQuery = itemPart;
            }
            vaultSmartWaitLoot = true;
            autoClickVaults = true;
            return "Принял! Включил Снайпер Хранилищ на «" + getActiveVaultTargetLabel() + "». Я НЕ буду кликать наугад — буду смотреть внутрь Хранилища и открою ключом ровно когда выпадет «" + getActiveVaultTargetLabel() + "»!";
        }

        // --- 2. АВТО-ПЕРЕБОР КАФЕДРЫ БИБЛИОТЕКАРЯ ---
        if (low.contains("перебор") || low.contains("сброс торг") || low.contains("сбрасывай") || low.contains("крути жител") || low.contains("библиотекар")) {
            if (low.contains("стоп") || low.contains("хватит") || low.contains("выкл")) {
                stopLecternCycle("Остановлено по команде");
                return "Остановил перебор кафедры библиотекаря.";
            }
            // Подбираем чары из фразы
            for (int i = 0; i < TARGET_ENCHANTS.size(); i++) {
                String enchLow = TARGET_ENCHANTS.get(i).toLowerCase(Locale.ROOT);
                String root = enchLow.substring(0, Math.min(5, enchLow.length()));
                if (low.contains(root)) {
                    targetEnchantIndex = i;
                    break;
                }
            }
            return startLecternCycleManual();
        }

        // --- 3. СТОП ВСЕХ АВТОМАТИК ---
        if (low.equals("стоп") || low.contains("остановись") || low.contains("отмена")) {
            safeAutopilot = false;
            releaseAutopilotKeys(mc);
            stopLecternCycle("Остановлено");
            autoClickVaults = false;
            return "Остановил Автопилот, Снайпер Хранилищ и Перебор Библиотекаря. Полное управление у тебя!";
        }

        // --- 4. ТОЧКА ДОМА И РЕАЛЬНЫЙ АВТОПИЛОТ ---
        if ((low.contains("задай дом") || low.equals("дом") || low.contains("сохрани дом") || low.contains("тут дом"))) {
            if (p != null) {
                homeWaypoint = p.blockPosition();
                setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
                return "Дом сохранён (" + homeWaypoint.getX() + ", " + homeWaypoint.getY() + ", " + homeWaypoint.getZ() + ")! Напиши «!к дому» — и я приведу тебя на автопилоте!";
            }
        }
        if (low.contains("к дому") || low.contains("домой") || low.contains("веди") || low.contains("автопилот")) {
            if (low.contains("выкл") || low.contains("стоп")) {
                safeAutopilot = false;
                releaseAutopilotKeys(mc);
                return "Автопилот выключен.";
            }
            if (low.contains("деревн")) {
                FoundStructureRecord rec = foundStructures.get(StructureTarget.VILLAGE);
                if (rec != null) {
                    setNavigationTarget(rec.pos, rec.target.russianName, rec.target.beaconColor);
                    safeAutopilot = true;
                    return "Включаю Автопилот к Деревне! Нажми S/A/D/Shift для мгновенной остановки.";
                }
                locateStructureAsync(StructureTarget.VILLAGE);
                safeAutopilot = true;
                return "Ищу Деревню и сразу включаю Автопилот к ней!";
            }
            if (low.contains("дворец") || low.contains("испытан")) {
                FoundStructureRecord rec = foundStructures.get(StructureTarget.TRIAL_CHAMBER);
                if (rec != null) {
                    setNavigationTarget(rec.pos, rec.target.russianName, rec.target.beaconColor);
                    safeAutopilot = true;
                    return "Включаю Автопилот к Дворцу испытаний!";
                }
                locateStructureAsync(StructureTarget.TRIAL_CHAMBER);
                safeAutopilot = true;
                return "Ищу Дворец испытаний и включаю Автопилот!";
            }
            if (low.contains("домой") || low.contains("к дому")) {
                if (homeWaypoint == null) {
                    return "Точка Дома ещё не задана! Встань на базе и напиши «!дом» (или нажми «Задать Дом» в вкладке Автопилот/Радар).";
                }
                setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
            }
            BlockPos target = activeNavTarget != null ? activeNavTarget : homeWaypoint;
            if (target == null) {
                return "Нет активной цели! Напиши «!дом», «!деревня» или «!испытания», а потом «!автопилот»!";
            }
            safeAutopilot = true;
            int dist = p != null ? (int) Math.sqrt(p.blockPosition().distSqr(target)) : 0;
            return "▶ Автопилот запущен к «" + (activeNavTitle.isEmpty() ? "Дом" : activeNavTitle) + "» (" + dist + "м)! Сам прыгаю и обхожу преграды. Нажми S/A/D или Shift для остановки.";
        }

        // --- 5. ПОИСК 8 СТРУКТУР МИРА ---
        if (low.contains("деревн") || low.contains("village")) {
            locateStructureAsync(StructureTarget.VILLAGE);
            return "Ищу ближайшую Деревню...";
        }
        if (low.contains("испытан") || low.contains("дворец") || low.contains("данж") || low.contains("trial")) {
            locateStructureAsync(StructureTarget.TRIAL_CHAMBER);
            return "Ищу ближайший Дворец испытаний...";
        }
        if (low.contains("крепост") || low.contains("энд") || low.contains("stronghold")) {
            locateStructureAsync(StructureTarget.STRONGHOLD);
            return "Ищу ближайшую Крепость...";
        }
        if (low.contains("город") || low.contains("древн") || low.contains("ancient")) {
            locateStructureAsync(StructureTarget.ANCIENT_CITY);
            return "Ищу ближайший Древний город...";
        }
        if (low.contains("особняк") || low.contains("mansion")) {
            locateStructureAsync(StructureTarget.MANSION);
            return "Ищу ближайший Лесной особняк...";
        }
        if (low.contains("аванпост") || low.contains("outpost")) {
            locateStructureAsync(StructureTarget.OUTPOST);
            return "Ищу ближайший Аванпост...";
        }
        if (low.contains("монумент") || low.contains("океан") || low.contains("monument")) {
            locateStructureAsync(StructureTarget.MONUMENT);
            return "Ищу ближайший Монумент океана...";
        }
        if (low.contains("портал") || low.contains("portal")) {
            locateStructureAsync(StructureTarget.RUINED_PORTAL);
            return "Ищу ближайший Разрушенный портал...";
        }

        if (low.contains("открой") || low.contains("open")) {
            return triggerManualOpenNearestContainer();
        }

        // --- 6. СМЕНА ОБЛИКА ---
        if (low.contains("робот") || low.contains("player")) { currentForm = VadimkaForm.CYBER_PLAYER; return "Сменил облик на Кибер-Робота!"; }
        if (low.contains("крыс") || low.contains("rat")) { currentForm = VadimkaForm.RAT; return "Писк! Сменил облик на Кибер-Крыску!"; }
        if (low.contains("аксолотл") || low.contains("axolotl")) { currentForm = VadimkaForm.AXOLOTL; return "Бульк! Теперь я редкий Синий Аксолотль!"; }
        if (low.contains("вихр") || low.contains("breeze")) { currentForm = VadimkaForm.BREEZE; return "Вжух! Теперь я Шторм-Вихрь!"; }
        if (low.contains("варден") || low.contains("хранител") || low.contains("warden")) { currentForm = VadimkaForm.WARDEN; return "Включил облик Мини-Хранителя!"; }
        if (low.contains("тихон") || low.contains("спешик") || low.contains("allay")) { currentForm = VadimkaForm.ALLAY; return "Теперь я Тихоня-Дух!"; }

        // --- 7. WINDOWS 10 SPOTIFY ---
        if (low.contains("следующ") || low.contains("next") || low.contains("скип")) { controlWindowsMediaAsync("NEXT"); return "Следующий трек в Spotify!"; }
        if (low.contains("предыдущ") || low.contains("prev")) { controlWindowsMediaAsync("PREV"); return "Предыдущий трек в Spotify!"; }
        if (low.contains("пауз") || low.contains("плей") || low.contains("play") || low.contains("pause")) { controlWindowsMediaAsync("TOGGLE"); return "Play/Pause в Spotify!"; }
        if (low.contains("трек") || low.contains("музык") || low.contains("спотифай") || low.contains("spotify")) {
            pollWindowsMediaAsync(true);
            return spotifyConnected
                ? ((spotifyIsPlaying ? "▶ Играет: " : "⏸ Пауза: ") + spotifyTrackArtist + " — " + spotifyTrackTitle)
                : "Включи песню в Spotify на ПК — я сразу покажу её здесь!";
        }

        // --- 8. ПОИСК В ЛАРЧИКАХ И ЖИТЕЛЯХ ---
        if (low.contains("найд") || low.contains("ларчик") || low.contains("сундук")) {
            String q = low.replace("найди", "").replace("найти", "").replace("ларчик", "").replace("сундук", "").trim();
            if (rememberedChests.isEmpty()) return "Открой сундук ПКМ, чтобы я запомнил лут! Хранилищ рядом: " + countNearbyVaultBlocks();
            for (ChestRecord cr : rememberedChests.values()) {
                for (Map.Entry<String, Integer> entry : cr.items.entrySet()) {
                    if (q.isEmpty() || entry.getKey().toLowerCase(Locale.ROOT).contains(q)) {
                        setNavigationTarget(cr.pos, entry.getKey(), 0xFFF59E0B);
                        int dist = p != null ? (int) Math.sqrt(p.blockPosition().distSqr(cr.pos)) : 0;
                        return "Нашёл «" + entry.getKey() + "» (x" + entry.getValue() + ") — " + dist + "м! Напиши «!автопилот», чтобы я привёл тебя туда.";
                    }
                }
            }
            return "Вещь «" + q + "» в запомненных ларчиках не найдена.";
        }
        if (low.contains("жител") || low.contains("починк")) {
            if (rememberedVillagers.isEmpty()) return "Открой торги с жителем или напиши «!перебор починка» рядом с библиотекарем и кафедрой!";
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
        if (low.contains("сброс")) {
            clearNavigationTarget();
            safeAutopilot = false;
            releaseAutopilotKeys(mc);
            return "Навигатор и Автопилот сброшены.";
        }

        String kb = lookupMinecraftAdvice(low);
        if (kb != null) return kb;
        return "Пиши: «Вадимка выбей из хранилища трезубец», «!перебор починка», «!к дому», «!деревня», «!испытания» или открой [P]!";
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
    // 7. УМНЫЙ БЕЗОПАСНЫЙ АВТОПИЛОТ (РЕАЛЬНО ПРИВОДИТ ДОМОЙ / К ЦЕЛИ!)
    // =========================================================================

    private static void releaseAutopilotKeys(Minecraft mc) {
        if (autopilotHoldingForward) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            autopilotHoldingForward = false;
        }
        autopilotStuckTicks = 0;
        autopilotStrafeTicks = 0;
    }

    private static void tickSmartAutopilot(Minecraft mc, ClientLevel level, Player player) {
        if (!safeAutopilot) {
            releaseAutopilotKeys(mc);
            return;
        }
        BlockPos goal = activeNavTarget != null ? activeNavTarget : homeWaypoint;
        if (goal == null) {
            safeAutopilot = false;
            releaseAutopilotKeys(mc);
            showToast("Автопилот: сначала задай Дом (!дом) или структуру!", 75);
            return;
        }
        if (mc.screen != null) {
            releaseAutopilotKeys(mc);
            return;
        }

        // Мгновенный возврат управления игроку при нажатии S / Shift
        if (autoReturnControl && (mc.options.keyDown.isDown() || mc.options.keyShift.isDown())) {
            safeAutopilot = false;
            releaseAutopilotKeys(mc);
            showToast("Автопилот остановлен: управление у тебя!", 65);
            return;
        }

        double dx = (goal.getX() + 0.5) - player.getX();
        double dz = (goal.getZ() + 0.5) - player.getZ();
        double dist2D = Math.sqrt(dx * dx + dz * dz);

        if (dist2D <= 2.4) {
            safeAutopilot = false;
            releaseAutopilotKeys(mc);
            String label = activeNavTitle.isEmpty() ? "Дом" : activeNavTitle;
            showToast("✦ Автопилот: ты прибыл в «" + label + "»!", 100);
            player.displayClientMessage(Component.literal("§a[✦ Вадимка] Ты прибыл к цели «" + label + "»!"), false);
            player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.85F, 1.3F);
            return;
        }

        // Проверка лавы и глубокого обрыва прямо перед игроком
        double dirX = dx / dist2D;
        double dirZ = dz / dist2D;
        BlockPos frontFeet = BlockPos.containing(player.getX() + dirX * 1.35, player.getY(), player.getZ() + dirZ * 1.35);
        BlockPos frontBelow = frontFeet.below();
        BlockState frontFeetState = level.getBlockState(frontFeet);
        BlockState frontBelowState = level.getBlockState(frontBelow);

        if (autopilotAvoidLavaAndCliffs) {
            if (frontFeetState.is(Blocks.LAVA) || frontBelowState.is(Blocks.LAVA) || frontFeetState.is(Blocks.FIRE)) {
                // Попытка объехать лаву боком
                if (autopilotStrafeTicks <= 0) {
                    autopilotStrafeDir = (worldTick % 2 == 0) ? 1 : -1;
                    autopilotStrafeTicks = 18;
                }
            } else if (frontBelowState.isAir() && measureDropBelow(level, frontFeet) >= 5) {
                // Глубокий обрыв впереди — останавливаемся безопасно!
                safeAutopilot = false;
                releaseAutopilotKeys(mc);
                showToast("⚠ Автопилот: впереди глубокий обрыв! Остановка.", 85);
                return;
            }
        }

        // Плавный поворот камеры к цели
        float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float yawDelta = Mth.wrapDegrees(desiredYaw - player.getYRot());
        float maxTurn = smoothBezierCamera ? 11.0F : 18.0F;
        player.setYRot(player.getYRot() + Mth.clamp(yawDelta, -maxTurn, maxTurn));

        // Детекция застревания (если игрок почти не сдвинулся за 14 тиков — обход вбок + прыжок)
        if (worldTick % 7 == 0) {
            Vec3 nowPos = player.position();
            if (autopilotLastCheckPos != null && nowPos.distanceToSqr(autopilotLastCheckPos) < 0.08) {
                autopilotStuckTicks += 7;
                if (autopilotStuckTicks >= 14) {
                    autopilotStrafeDir = (autopilotStrafeDir == 1) ? -1 : 1;
                    autopilotStrafeTicks = 14;
                    autopilotStuckTicks = 0;
                }
            } else {
                autopilotStuckTicks = 0;
            }
            autopilotLastCheckPos = nowPos;
        }

        // Проверяем нужен ли прыжок (ступенька 1 блок или вода)
        boolean stepUpBlock = !frontFeetState.isAir() && level.getBlockState(frontFeet.above()).isAir();
        boolean needJump = player.horizontalCollision || player.isInWater() || stepUpBlock;

        mc.options.keyUp.setDown(true);
        mc.options.keyJump.setDown(needJump);
        if (autopilotStrafeTicks > 0) {
            autopilotStrafeTicks--;
            mc.options.keyLeft.setDown(autopilotStrafeDir < 0);
            mc.options.keyRight.setDown(autopilotStrafeDir > 0);
        } else {
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
        }

        if (sprintAssist && player.getFoodData().getFoodLevel() > 6 && !player.isInWater()) {
            player.setSprinting(true);
        }
        autopilotHoldingForward = true;
    }

    // =========================================================================
    // 8. СНАЙПЕР ХРАНИЛИЩ (VAULT LOOT SNIPER — ЧИТАЕТ ПРЕДМЕТ ВНУТРИ VAULT!)
    // =========================================================================

    public static String getActiveVaultTargetLabel() {
        if (customVaultItemQuery != null && !customVaultItemQuery.isEmpty()) {
            return customVaultItemQuery;
        }
        return VAULT_LOOT_PRESETS.get(vaultLootGoalIndex % VAULT_LOOT_PRESETS.size()).displayName;
    }

    private static ItemStack readVaultDisplayItem(ClientLevel level, BlockPos pos) {
        try {
            BlockEntity be = level.getBlockEntity(pos);
            if (be == null) return ItemStack.EMPTY;

            // 1. Прямой поиск метода getSharedData() -> getDisplayItem() на VaultBlockEntity
            for (Method m : be.getClass().getMethods()) {
                if (m.getParameterCount() != 0) continue;
                Class<?> rt = m.getReturnType();
                if (rt == ItemStack.class) {
                    Object res = m.invoke(be);
                    if (res instanceof ItemStack st && !st.isEmpty()) return st;
                } else if (rt.getSimpleName().toLowerCase(Locale.ROOT).contains("shared")) {
                    Object shared = m.invoke(be);
                    if (shared != null) {
                        for (Method sm : shared.getClass().getMethods()) {
                            if (sm.getParameterCount() == 0 && sm.getReturnType() == ItemStack.class) {
                                Object res = sm.invoke(shared);
                                if (res instanceof ItemStack st && !st.isEmpty()) return st;
                            }
                        }
                        for (Field sf : shared.getClass().getDeclaredFields()) {
                            if (sf.getType() == ItemStack.class) {
                                sf.setAccessible(true);
                                Object res = sf.get(shared);
                                if (res instanceof ItemStack st && !st.isEmpty()) return st;
                            }
                        }
                    }
                }
            }

            // 2. Резервный поиск по полям самого BlockEntity
            for (Field f : be.getClass().getDeclaredFields()) {
                f.setAccessible(true);
                Object val = f.get(be);
                if (val instanceof ItemStack st && !st.isEmpty()) {
                    return st;
                }
                if (val != null && val.getClass().getSimpleName().toLowerCase(Locale.ROOT).contains("shared")) {
                    for (Field sf : val.getClass().getDeclaredFields()) {
                        if (sf.getType() == ItemStack.class) {
                            sf.setAccessible(true);
                            Object res = sf.get(val);
                            if (res instanceof ItemStack st && !st.isEmpty()) return st;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return ItemStack.EMPTY;
    }

    private static boolean doesItemMatchVaultGoal(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        String displayName = formatItemWithEnchants(stack).toLowerCase(Locale.ROOT);
        String itemId = stack.getItem().toString().toLowerCase(Locale.ROOT);
        String combined = displayName + " " + itemId;

        if (customVaultItemQuery != null && !customVaultItemQuery.isEmpty()) {
            String q = customVaultItemQuery.toLowerCase(Locale.ROOT).trim();
            if (combined.contains(q)) return true;
            // Русско-английский словарь популярных предметов Дворца испытаний
            if (q.contains("трезуб") && (combined.contains("trident") || combined.contains("трезуб"))) return true;
            if (q.contains("ядр") && (combined.contains("heavy_core") || combined.contains("ядр"))) return true;
            if (q.contains("яблок") && (combined.contains("apple") || combined.contains("яблок"))) return true;
            if (q.contains("книг") && (combined.contains("book") || combined.contains("книг"))) return true;
            if (q.contains("алмаз") && (combined.contains("diamond") || combined.contains("алмаз"))) return true;
            if (q.contains("изумр") && (combined.contains("emerald") || combined.contains("изумр"))) return true;
            if (q.contains("бутыл") && (combined.contains("bottle") || combined.contains("бутыл"))) return true;
            if (q.contains("ветер") && (combined.contains("wind_charge") || combined.contains("ветер"))) return true;
            if (q.contains("арбалет") && (combined.contains("crossbow") || combined.contains("арбалет"))) return true;
            if (q.contains("лук") && (combined.contains("bow") || combined.contains("лук"))) return true;
            if (q.contains("топор") && (combined.contains("axe") || combined.contains("топор"))) return true;
            if (q.contains("брон") && (combined.contains("chestplate") || combined.contains("helmet") || combined.contains("leggings"))) return true;
            return false;
        }

        VaultLootPreset preset = VAULT_LOOT_PRESETS.get(vaultLootGoalIndex % VAULT_LOOT_PRESETS.size());
        for (String kw : preset.matchKeywords) {
            if ("*".equals(kw) || combined.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private static void tickSmartVaultLootSniper(Minecraft mc, ClientLevel level, Player player) {
        lastVaultMatchedNow = false;
        // Ищем ближайшее активное Хранилище (Vault) в радиусе 4.6 блока
        VaultInfo bestVault = null;
        double bestDistSq = 4.6 * 4.6;
        for (VaultInfo vi : nearbyVaults) {
            if (!vi.isVaultBlock) continue;
            double dSq = player.position().distanceToSqr(Vec3.atCenterOf(vi.pos));
            if (dSq <= bestDistSq) {
                BlockState fresh = level.getBlockState(vi.pos);
                VaultInfo live = inspectPotentialVault(level, vi.pos, fresh);
                if (live != null && live.isVaultBlock && live.active) {
                    bestDistSq = dSq;
                    bestVault = live;
                }
            }
        }

        if (bestVault == null) {
            lastVaultSpinningLabel = autoClickVaults
                ? ("Снайпер ждёт Хранилище рядом • Цель: " + getActiveVaultTargetLabel())
                : "Подойди к Хранилищу";
            return;
        }

        ItemStack spinning = bestVault.displayItem;
        String spinName = spinning.isEmpty() ? "???" : formatItemWithEnchants(spinning);
        boolean matched = !vaultSmartWaitLoot || doesItemMatchVaultGoal(spinning);
        lastVaultMatchedNow = matched;
        lastVaultSpinningLabel = "Внутри: [" + spinName + "] • Ждём: [" + getActiveVaultTargetLabel() + "]";

        if (!autoClickVaults || mc.screen != null || mc.gameMode == null) {
            return;
        }
        if (!matched) {
            // НЕ КЛИКАЕМ, пока внутри Хранилища не появится нужный предмет!
            return;
        }
        if ((worldTick - lastVaultInteractTick) < 16) {
            return;
        }

        Item neededKey = bestVault.ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;
        int keySlot = findHotbarSlotForItem(player, neededKey);
        if (keySlot < 0) {
            if ((worldTick - lastMissingKeyWarnTick) > 90) {
                String kName = bestVault.ominous ? "Зловещий ключ испытаний" : "Ключ испытаний";
                showToast("★ ВЫПАЛ [" + spinName + "]! Нужен ключ в хотбаре: " + kName, 85);
                lastMissingKeyWarnTick = worldTick;
            }
            return;
        }

        // Поворачиваемся к Хранилищу, выбираем ключ и ОТКРЫВАЕМ ровно на нужном предмете!
        aimAtBlockCenter(player, bestVault.pos);
        selectHotbarSlotSafe(player, keySlot);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(bestVault.pos), Direction.UP, bestVault.pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastVaultInteractTick = worldTick;
        String okMsg = "★ ПОЙМАЛ [" + spinName + "] в Хранилище!";
        showToast(okMsg, 110);
        player.displayClientMessage(Component.literal("§a[✦ Вадимка] " + okMsg), false);
        player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9F, 1.15F);
    }

    // =========================================================================
    // 9. РЕАЛЬНЫЙ АВТО-ПЕРЕБОР КАФЕДРЫ БИБЛИОТЕКАРЯ (ЛОМАЕТ -> СТАВИТ -> ЧЕКАЕТ!)
    // =========================================================================

    public static String startLecternCycleManual() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            return "Зайди в мир, чтобы запустить перебор кафедры.";
        }
        BlockPos lectern = findNearestLectern(mc.level, mc.player.blockPosition(), 4);
        Entity villager = findNearestVillager(mc.level, mc.player, 5.5);
        if (lectern == null) {
            autoCycleLectern = false;
            lecternState = LecternCycleState.IDLE;
            lecternStatusMessage = "Кафедра не найдена рядом (<= 4 бл.)";
            return "Подойди вплотную к Кафедре (в радиусе 4 блоков) и повтори!";
        }
        if (villager == null) {
            autoCycleLectern = false;
            lecternState = LecternCycleState.IDLE;
            lecternStatusMessage = "Житель не найден рядом (<= 5.5 бл.)";
            return "Рядом нет Жителя (в радиусе 5.5 блоков)!";
        }
        int axeSlot = findAnyAxeHotbarSlot(mc.player);
        if (axeSlot < 0) {
            return "Положи любой Топор в хотбар, чтобы я мог ломать кафедру!";
        }
        lockedLecternPos = lectern;
        lockedVillagerId = villager.getId();
        autoCycleLectern = true;
        lecternAttempts = 0;
        lecternState = LecternCycleState.OPEN_VILLAGER;
        lecternStateTimer = 0;
        lecternStatusMessage = "Старт перебора → " + TARGET_ENCHANTS.get(targetEnchantIndex) + " ≤ " + maxEmeraldPrice;
        return "Запустил Авто-перебор кафедры! Ищу «" + TARGET_ENCHANTS.get(targetEnchantIndex) + "» ≤ " + maxEmeraldPrice + " изумр. (нажми Shift для остановки).";
    }

    private static void stopLecternCycle(String reason) {
        autoCycleLectern = false;
        lecternState = LecternCycleState.IDLE;
        lecternStateTimer = 0;
        lecternStatusMessage = reason;
    }

    private static void tickAutoCycleLectern(Minecraft mc, ClientLevel level, Player player) {
        if (!autoCycleLectern) {
            if (lecternState != LecternCycleState.IDLE) {
                lecternState = LecternCycleState.IDLE;
            }
            return;
        }
        if (mc.gameMode == null) return;

        // Экстренная остановка по Shift или если открыли наше меню [P]
        if (mc.options.keyShift.isDown()) {
            stopLecternCycle("Остановлено игроком (Shift)");
            showToast("Авто-перебор кафедры остановлен", 55);
            return;
        }
        if (mc.screen instanceof AssistantVadimkaScreen) {
            return;
        }

        // Если вдруг не зафиксировали кафедру/жителя (включили тумблером в меню) — находим автоматически!
        if (lockedLecternPos == null || lockedVillagerId < 0) {
            BlockPos foundL = findNearestLectern(level, player.blockPosition(), 4);
            Entity foundV = findNearestVillager(level, player, 5.5);
            if (foundL == null || foundV == null) {
                lecternStatusMessage = "Подойди к Жителю и Кафедре (<= 4 бл.)";
                return;
            }
            lockedLecternPos = foundL;
            lockedVillagerId = foundV.getId();
            lecternState = LecternCycleState.OPEN_VILLAGER;
            lecternStateTimer = 0;
        }

        Entity villagerEnt = level.getEntity(lockedVillagerId);
        if (villagerEnt == null || villagerEnt.isRemoved() || villagerEnt.distanceToSqr(player) > 49.0) {
            stopLecternCycle("Житель ушёл слишком далеко");
            showToast("Авто-перебор остановлен: житель далеко", 65);
            return;
        }

        lecternStateTimer++;

        switch (lecternState) {
            case OPEN_VILLAGER -> {
                lecternStatusMessage = "#" + lecternAttempts + " • Открываю торги жителя...";
                if (player.containerMenu instanceof MerchantMenu) {
                    lecternState = LecternCycleState.CHECK_TRADES;
                    lecternStateTimer = 0;
                    return;
                }
                if (lecternStateTimer == 1 || lecternStateTimer % 12 == 0) {
                    aimAtEntityEyes(player, villagerEnt);
                    interactEntitySafe(mc, player, villagerEnt);
                }
                if (lecternStateTimer > 50) {
                    // Если у жителя ещё нет профессии — значит нужно сломать/переставить кафедру
                    lecternState = LecternCycleState.BREAK_LECTERN;
                    lecternStateTimer = 0;
                }
            }
            case CHECK_TRADES -> {
                if (!(player.containerMenu instanceof MerchantMenu merchantMenu)) {
                    if (lecternStateTimer > 15) {
                        lecternState = LecternCycleState.OPEN_VILLAGER;
                        lecternStateTimer = 0;
                    }
                    return;
                }
                MerchantOffers offers = merchantMenu.getOffers();
                if (offers == null || offers.isEmpty()) {
                    if (lecternStateTimer > 12) {
                        if (mc.player != null) mc.player.closeContainer();
                        lecternState = LecternCycleState.BREAK_LECTERN;
                        lecternStateTimer = 0;
                    }
                    return;
                }
                String want = TARGET_ENCHANTS.get(targetEnchantIndex).toLowerCase(Locale.ROOT);
                for (MerchantOffer offer : offers) {
                    String res = formatItemWithEnchants(offer.getResult());
                    int price = offer.getCostA().is(Items.EMERALD) ? offer.getCostA().getCount() : 99;
                    if (matchesTargetEnchant(res, want) && price <= maxEmeraldPrice) {
                        // ПОБЕДА! Нужная книга найдена — НЕ закрываем окно, останавливаем перебор!
                        String win = "★ ПОЙМАНО (#" + lecternAttempts + "): " + res + " за " + price + " изумр.!";
                        stopLecternCycle(win);
                        showToast(win, 160);
                        player.displayClientMessage(Component.literal("§a[✦ Вадимка] " + win), false);
                        player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                        return;
                    }
                }
                // Не подошло — закрываем торги и ломаем кафедру!
                lecternAttempts++;
                if (mc.player != null) mc.player.closeContainer();
                lecternState = LecternCycleState.BREAK_LECTERN;
                lecternStateTimer = 0;
            }
            case BREAK_LECTERN -> {
                lecternStatusMessage = "#" + lecternAttempts + " • Ломаю кафедру топором...";
                if (!level.getBlockState(lockedLecternPos).is(Blocks.LECTERN)) {
                    lecternState = LecternCycleState.WAIT_PICKUP;
                    lecternStateTimer = 0;
                    return;
                }
                int axeSlot = findAnyAxeHotbarSlot(player);
                if (axeSlot < 0) {
                    stopLecternCycle("Нет топора в хотбаре!");
                    showToast("Авто-перебор: положи Топор в хотбар!", 80);
                    return;
                }
                ItemStack axe = player.getInventory().getItem(axeSlot);
                if (warnAxeDurability && axe.isDamageableItem() && axe.getMaxDamage() > 0) {
                    int left = axe.getMaxDamage() - axe.getDamageValue();
                    if (left <= 10) {
                        stopLecternCycle("Стоп: прочность топора <= 10!");
                        showToast("⚠ Остановил перебор, чтобы не сломать топор (" + left + ")!", 100);
                        return;
                    }
                }
                selectHotbarSlotSafe(player, axeSlot);
                aimAtBlockCenter(player, lockedLecternPos);
                breakBlockStepSafe(mc, lockedLecternPos, lecternStateTimer == 1);
                player.swing(InteractionHand.MAIN_HAND);
            }
            case WAIT_PICKUP -> {
                lecternStatusMessage = "#" + lecternAttempts + " • Подбираю кафедру...";
                if (findHotbarSlotForItem(player, Items.LECTERN) >= 0 && lecternStateTimer >= 6) {
                    lecternState = LecternCycleState.PLACE_LECTERN;
                    lecternStateTimer = 0;
                } else if (lecternStateTimer > 35) {
                    // Попробуем подтянуть кафедру из инвентаря в хотбар
                    int invSlot = findInventorySlotForItem(player, Items.LECTERN);
                    if (invSlot >= 0) {
                        lecternState = LecternCycleState.PLACE_LECTERN;
                        lecternStateTimer = 0;
                    } else if (lecternStateTimer > 60) {
                        stopLecternCycle("Кафедра не подобралась (подбери и повтори)");
                        showToast("Кафедра укатилась — подбери её и включи снова!", 75);
                    }
                }
            }
            case PLACE_LECTERN -> {
                lecternStatusMessage = "#" + lecternAttempts + " • Ставлю кафедру...";
                if (level.getBlockState(lockedLecternPos).is(Blocks.LECTERN)) {
                    lecternState = LecternCycleState.WAIT_JOB;
                    lecternStateTimer = 0;
                    return;
                }
                int lecSlot = findHotbarSlotForItem(player, Items.LECTERN);
                if (lecSlot < 0) {
                    lecternState = LecternCycleState.WAIT_PICKUP;
                    lecternStateTimer = 0;
                    return;
                }
                selectHotbarSlotSafe(player, lecSlot);
                BlockPos below = lockedLecternPos.below();
                aimAtBlockCenter(player, below);
                BlockHitResult placeHit = new BlockHitResult(
                    new Vec3(below.getX() + 0.5, below.getY() + 1.0, below.getZ() + 0.5),
                    Direction.UP, below, false
                );
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, placeHit);
                player.swing(InteractionHand.MAIN_HAND);
                lecternState = LecternCycleState.WAIT_JOB;
                lecternStateTimer = 0;
            }
            case WAIT_JOB -> {
                lecternStatusMessage = "#" + lecternAttempts + " • Жду новые торги (" + (26 - Math.min(26, lecternStateTimer)) + ")...";
                if (lecternStateTimer >= 26) {
                    lecternState = LecternCycleState.OPEN_VILLAGER;
                    lecternStateTimer = 0;
                }
            }
            default -> {
            }
        }
    }

    private static boolean matchesTargetEnchant(String res, String wantRu) {
        String low = res.toLowerCase(Locale.ROOT);
        if (low.contains(wantRu)) return true;
        if (wantRu.contains("почин") && low.contains("mending")) return true;
        if (wantRu.contains("неразруш") && low.contains("unbreaking")) return true;
        if (wantRu.contains("эффектив") && low.contains("efficiency")) return true;
        if (wantRu.contains("острот") && low.contains("sharpness")) return true;
        if (wantRu.contains("защит") && low.contains("protection")) return true;
        if (wantRu.contains("удач") && low.contains("fortune")) return true;
        if (wantRu.contains("добыч") && low.contains("looting")) return true;
        if (wantRu.contains("шёлк") && low.contains("silk")) return true;
        return false;
    }

    private static BlockPos findNearestLectern(ClientLevel level, BlockPos origin, int r) {
        BlockPos best = null;
        double bestSq = 999.0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -2; dy <= 2; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    BlockPos p = origin.offset(dx, dy, dz);
                    if (level.getBlockState(p).is(Blocks.LECTERN)) {
                        double sq = p.distSqr(origin);
                        if (sq < bestSq) {
                            bestSq = sq;
                            best = p.immutable();
                        }
                    }
                }
            }
        }
        return best;
    }

    private static Entity findNearestVillager(ClientLevel level, Player player, double maxDist) {
        Entity best = null;
        double bestSq = maxDist * maxDist;
        for (Entity e : level.entitiesForRendering()) {
            if (e.getType() == EntityType.VILLAGER) {
                double sq = e.distanceToSqr(player);
                if (sq < bestSq) {
                    bestSq = sq;
                    best = e;
                }
            }
        }
        return best;
    }

    private static int findAnyAxeHotbarSlot(Player player) {
        Item[] axes = {
            Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE,
            Items.GOLDEN_AXE, Items.STONE_AXE, Items.WOODEN_AXE
        };
        for (Item axe : axes) {
            int s = findHotbarSlotForItem(player, axe);
            if (s >= 0) return s;
        }
        return -1;
    }

    private static int findInventorySlotForItem(Player player, Item item) {
        for (int i = 9; i < 36; i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && st.is(item)) return i;
        }
        return -1;
    }

    private static void aimAtBlockCenter(Player player, BlockPos pos) {
        aimAtPoint(player, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static void aimAtEntityEyes(Player player, Entity target) {
        aimAtPoint(player, target.getX(), target.getY() + target.getBbHeight() * 0.75, target.getZ());
    }

    private static void aimAtPoint(Player player, double tx, double ty, double tz) {
        Vec3 eye = player.getEyePosition();
        double dx = tx - eye.x;
        double dy = ty - eye.y;
        double dz = tz - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float pitch = (float) (-(Mth.atan2(dy, horiz) * (180.0 / Math.PI)));
        player.setYRot(yaw);
        player.setXRot(Mth.clamp(pitch, -89.0F, 89.0F));
    }

    private static void interactEntitySafe(Minecraft mc, Player player, Entity target) {
        if (mc.gameMode == null) return;
        try {
            mc.gameMode.interact(player, target, InteractionHand.MAIN_HAND);
            player.swing(InteractionHand.MAIN_HAND);
        } catch (Throwable ignored) {
        }
    }

    private static void breakBlockStepSafe(Minecraft mc, BlockPos pos, boolean firstTick) {
        if (mc.gameMode == null) return;
        try {
            if (firstTick) {
                mc.gameMode.startDestroyBlock(pos, Direction.UP);
            } else {
                mc.gameMode.continueDestroyBlock(pos, Direction.UP);
            }
        } catch (Throwable ignored) {
        }
    }

    // --- Авто-пополнение хотбара (реально перекидывает стак блоков из инвентаря!) ---
    private static void tickHotbarAutoRefill(Minecraft mc, Player player) {
        if (!hotbarAutoRefill || mc.screen != null || mc.gameMode == null) return;
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty() && lastMainHandItem != Items.AIR && lastMainHandCount == 1 && (worldTick - lastRefillTick) > 10) {
            int invSlot = findInventorySlotForItem(player, lastMainHandItem);
            if (invSlot >= 9 && invSlot < 36) {
                int selectedHotbar = findCurrentSelectedHotbarSlot(player);
                if (selectedHotbar >= 0 && selectedHotbar <= 8) {
                    try {
                        mc.gameMode.handleInventoryMouseClick(
                            player.inventoryMenu.containerId,
                            invSlot,
                            selectedHotbar,
                            ClickType.SWAP,
                            player
                        );
                        lastRefillTick = worldTick;
                        showToast("✦ Пополнил хотбар: " + lastMainHandItem.getName(new ItemStack(lastMainHandItem)).getString(), 45);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        lastMainHandItem = hand.isEmpty() ? Items.AIR : hand.getItem();
        lastMainHandCount = hand.getCount();
    }

    private static int findCurrentSelectedHotbarSlot(Player player) {
        Object inv = player.getInventory();
        try {
            Method m = inv.getClass().getMethod("getSelectedSlot");
            Object res = m.invoke(inv);
            if (res instanceof Integer i) return i;
        } catch (Throwable ignored) {
        }
        try {
            Field f = inv.getClass().getDeclaredField("selected");
            f.setAccessible(true);
            return f.getInt(inv);
        } catch (Throwable ignored) {
        }
        return 0;
    }

    // =========================================================================
    // 10. ПОИСК СТРУКТУР МИРА, РУЧНОЕ ОТКРЫТИЕ ХРАНИЛИЩ И WINDOWS 10 SPOTIFY
    // =========================================================================

    public static void locateStructureAsync(StructureTarget target) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (structureSearchBusy) {
            showToast("Поиск уже идёт, секунду...", 45);
            return;
        }
        structureSearchBusy = true;
        structureSearchStatus = "Ищу: " + target.russianName + "...";
        BlockPos startPos = mc.player.blockPosition();
        MinecraftServer server = mc.getSingleplayerServer();

        if (server != null) {
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
                String msg = "На сервере в прогруженных чанках «" + target.russianName + "» пока не видно.";
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

        for (String methodName : new String[]{"lookupOrThrow", "registryOrThrow"}) {
            try {
                Method m = regAccess.getClass().getMethod(methodName, net.minecraft.resources.ResourceKey.class);
                structureRegistry = m.invoke(regAccess, Registries.STRUCTURE);
                if (structureRegistry != null) break;
            } catch (NoSuchMethodException ignored) {
            }
        }
        if (structureRegistry == null) return null;

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
        if (matched.isEmpty()) return null;

        HolderSet holderSet = HolderSet.direct((List) matched);
        Object generator = sLevel.getChunkSource().getGenerator();
        for (Method m : generator.getClass().getMethods()) {
            if (m.getParameterCount() == 5) {
                Class<?>[] pt = m.getParameterTypes();
                if (pt[0] == ServerLevel.class && pt[1] == HolderSet.class
                        && pt[2] == BlockPos.class && pt[3] == int.class && pt[4] == boolean.class) {
                    Object pair = m.invoke(generator, sLevel, holderSet, origin, 100, false);
                    if (pair != null) {
                        Method getFirst = pair.getClass().getMethod("getFirst");
                        Object posObj = getFirst.invoke(pair);
                        if (posObj instanceof BlockPos bp) return bp;
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
                if (e.getType() == EntityType.VILLAGER) return e.blockPosition();
            }
        }
        if (target == StructureTarget.TRIAL_CHAMBER) {
            for (VaultInfo vi : nearbyVaults) {
                if (vi.isVaultBlock) return vi.pos;
            }
        }
        BlockPos center = player.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = 42;
        int minY = target == StructureTarget.TRIAL_CHAMBER ? Math.max(level.getMinY(), -45) : Math.max(level.getMinY(), center.getY() - 14);
        int maxY = target == StructureTarget.TRIAL_CHAMBER ? Math.min(level.getMaxY() - 1, 20) : Math.min(level.getMaxY() - 1, center.getY() + 20);
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

    private static VaultInfo inspectPotentialVault(ClientLevel level, BlockPos pos, BlockState st) {
        if (st.is(Blocks.VAULT)) {
            boolean ominous = false;
            boolean active = true;
            String stateName = "active";
            for (Map.Entry<Property<?>, Comparable<?>> entry : st.getValues().entrySet()) {
                String pName = entry.getKey().getName().toLowerCase(Locale.ROOT);
                String pVal = String.valueOf(entry.getValue()).toLowerCase(Locale.ROOT);
                if (pName.contains("ominous")) ominous = "true".equals(pVal);
                else if (pName.contains("state")) {
                    stateName = pVal;
                    active = pVal.equals("active") || pVal.equals("unlocking");
                }
            }
            ItemStack disp = active ? readVaultDisplayItem(level, pos) : ItemStack.EMPTY;
            return new VaultInfo(pos.immutable(), true, ominous, active, stateName, disp);
        }
        if (isRegularLootContainer(st)) {
            return new VaultInfo(pos.immutable(), false, false, true, "chest", ItemStack.EMPTY);
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
            if (!st.isEmpty() && st.is(item)) return i;
        }
        return -1;
    }

    private static boolean selectHotbarSlotSafe(Player player, int slot) {
        if (slot < 0 || slot > 8) return false;
        Object inv = player.getInventory();
        try {
            Method m = inv.getClass().getMethod("setSelectedSlot", int.class);
            m.invoke(inv, slot);
            return true;
        } catch (Throwable ignored) {
        }
        try {
            Field f = inv.getClass().getDeclaredField("selected");
            f.setAccessible(true);
            f.setInt(inv, slot);
            return true;
        } catch (Throwable ignored) {
        }
        return false;
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
                VaultInfo fresh = inspectPotentialVault(mc.level, vi.pos, mc.level.getBlockState(vi.pos));
                if (fresh != null && (best == null || (fresh.isVaultBlock && !best.isVaultBlock) || dSq < bestDist)) {
                    best = fresh;
                    bestDist = dSq;
                }
            }
        }
        if (best == null) {
            BlockPos pc = p.blockPosition();
            for (int dx = -4; dx <= 4; dx++) {
                for (int dy = -2; dy <= 3; dy++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        BlockPos bp = pc.offset(dx, dy, dz);
                        VaultInfo vi = inspectPotentialVault(mc.level, bp, mc.level.getBlockState(bp));
                        if (vi != null) { best = vi; break; }
                    }
                    if (best != null) break;
                }
                if (best != null) break;
            }
        }
        if (best == null) {
            return "В радиусе 4.5 блоков нет Хранилищ или Сундуков!";
        }
        if (best.isVaultBlock) {
            if (!best.active) return "Это Хранилище уже открыто (" + best.stateName + ").";
            if (vaultSmartWaitLoot && !doesItemMatchVaultGoal(best.displayItem)) {
                autoClickVaults = true;
                String cur = best.displayItem.isEmpty() ? "???" : formatItemWithEnchants(best.displayItem);
                return "Сейчас внутри [" + cur + "], а мы ждём [" + getActiveVaultTargetLabel() + "]! Снайпер включён — ждём и откроем сам!";
            }
            Item key = best.ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;
            int slot = findHotbarSlotForItem(p, key);
            if (slot < 0) {
                return "Положи в хотбар: " + (best.ominous ? "Зловещий ключ испытаний" : "Ключ испытаний") + "!";
            }
            selectHotbarSlotSafe(p, slot);
        }
        aimAtBlockCenter(p, best.pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(best.pos), Direction.UP, best.pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastVaultInteractTick = worldTick;
        return best.isVaultBlock ? "Открыл Хранилище ключом!" : "Открыл ближайший ларчик/сундук!";
    }

    // --- WINDOWS 10 WINRT GSMTC SPOTIFY CONNECT ---

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static String encodePsBase64(String script) {
        return Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
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
        if (!force && (spotifyPollBusy || (now - spotifyLastPollMs) < 2800L)) return;
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
                        spotifySourceName = parts[6].toLowerCase(Locale.ROOT).contains("spotify") ? "Spotify (ПК)" : "Медиа (ПК)";
                    }
                } else {
                    spotifyConnected = false;
                    spotifyIsPlaying = false;
                    spotifyTrackTitle = "Spotify не играет";
                    spotifyTrackArtist = "Включи песню в Spotify на ПК";
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
                Thread.sleep(360);
            } catch (Throwable ignored) {
            } finally {
                spotifyLastPollMs = 0L;
                pollWindowsMediaAsync(true);
            }
        });
    }

    private static int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (Throwable ignored) { return def; }
    }

    // =========================================================================
    // 11. GPU DYNAMIC TEXTURE РЕАЛИСТИЧНАЯ МИНИ-КАРТА 128x128 (1 ВЫЗОВ GPU = 75+ FPS!)
    // =========================================================================

    private static void initGpuMinimapTextureOnce(Minecraft mc) {
        if (gpuMapInitAttempted) return;
        gpuMapInitAttempted = true;
        try {
            Class<?> nativeImgClass = Class.forName("com.mojang.blaze3d.platform.NativeImage");
            Constructor<?> niCtor = nativeImgClass.getConstructor(int.class, int.class, boolean.class);
            Object nativeImg = niCtor.newInstance(TEX_SIZE, TEX_SIZE, false);

            Method setPx = null;
            boolean isArgb = true;
            try {
                setPx = nativeImgClass.getMethod("setPixel", int.class, int.class, int.class);
                isArgb = true;
            } catch (NoSuchMethodException ignored) {
                setPx = nativeImgClass.getMethod("setPixelRGBA", int.class, int.class, int.class);
                isArgb = false;
            }

            // Создаём DynamicTexture (совместимо с 1.21.5–1.21.11 Supplier<String> И с более ранними версиями)
            Class<?> dynTexClass = Class.forName("net.minecraft.client.renderer.texture.DynamicTexture");
            Object dynTex = null;
            for (Constructor<?> c : dynTexClass.getConstructors()) {
                Class<?>[] pt = c.getParameterTypes();
                if (pt.length == 2 && Supplier.class.isAssignableFrom(pt[0]) && pt[1] == nativeImgClass) {
                    dynTex = c.newInstance((Supplier<String>) () -> "assistant_vadimka_minimap", nativeImg);
                    break;
                }
                if (pt.length == 1 && pt[0] == nativeImgClass) {
                    dynTex = c.newInstance(nativeImg);
                    break;
                }
            }
            if (dynTex == null) return;
            Method uploadM = dynTexClass.getMethod("upload");

            // Создаём Identifier (1.21.11) или ResourceLocation (более ранние 1.21.x)
            Class<?> idClass = null;
            for (String cname : new String[]{"net.minecraft.resources.Identifier", "net.minecraft.resources.ResourceLocation"}) {
                try {
                    idClass = Class.forName(cname);
                    break;
                } catch (ClassNotFoundException ignored) {
                }
            }
            if (idClass == null) return;

            Method fromNs = idClass.getMethod("fromNamespaceAndPath", String.class, String.class);
            Object texId = fromNs.invoke(null, MOD_ID, "dynamic_minimap_v8");

            // Регистрируем текстуру в TextureManager
            Object texMgr = mc.getTextureManager();
            Method regM = null;
            for (Method m : texMgr.getClass().getMethods()) {
                if (m.getName().equals("register") && m.getParameterCount() == 2 && m.getParameterTypes()[0] == idClass) {
                    regM = m;
                    break;
                }
            }
            if (regM == null) return;
            regM.invoke(texMgr, texId, dynTex);

            // Находим RenderPipelines.GUI_TEXTURED и GuiGraphics.blit(...)
            Object pipeline = null;
            try {
                Class<?> rpClass = Class.forName("net.minecraft.client.renderer.RenderPipelines");
                Field f = rpClass.getField("GUI_TEXTURED");
                pipeline = f.get(null);
            } catch (Throwable ignored) {
            }

            Method blitM = null;
            int style = 0;
            if (pipeline != null) {
                for (Method m : GuiGraphics.class.getMethods()) {
                    if (!m.getName().equals("blit") || m.getParameterCount() != 10) continue;
                    Class<?>[] pt = m.getParameterTypes();
                    if (pt[0].isInstance(pipeline) && pt[1] == idClass
                            && pt[2] == int.class && pt[3] == int.class
                            && pt[4] == float.class && pt[5] == float.class
                            && pt[6] == int.class && pt[7] == int.class
                            && pt[8] == int.class && pt[9] == int.class) {
                        blitM = m;
                        style = 1;
                        break;
                    }
                }
            }
            if (blitM == null) {
                for (Method m : GuiGraphics.class.getMethods()) {
                    if (!m.getName().equals("blit") || m.getParameterCount() != 9) continue;
                    Class<?>[] pt = m.getParameterTypes();
                    if (pt[0] == idClass && pt[1] == int.class && pt[2] == int.class
                            && pt[3] == float.class && pt[4] == float.class
                            && pt[5] == int.class && pt[6] == int.class
                            && pt[7] == int.class && pt[8] == int.class) {
                        blitM = m;
                        style = 2;
                        break;
                    }
                }
            }

            if (blitM != null) {
                gpuNativeImage = nativeImg;
                gpuDynamicTexture = dynTex;
                gpuTextureId = texId;
                gpuRenderPipeline = pipeline;
                nativeSetPixelMethod = setPx;
                nativeSetPixelIsArgb = isArgb;
                dynamicUploadMethod = uploadM;
                guiBlitMethod = blitM;
                guiBlitStyle = style;
                gpuMapReady = true;
            }
        } catch (Throwable ignored) {
            gpuMapReady = false;
        }
    }

    private static int toNativePixel(int argb) {
        if (nativeSetPixelIsArgb) return argb;
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static void rebuildGpuMinimapTexture(float playerYaw) {
        if (!gpuMapReady || gpuNativeImage == null || nativeSetPixelMethod == null || dynamicUploadMethod == null) {
            return;
        }
        try {
            double yawRad = Math.toRadians(playerYaw + 180.0);
            double cos = Math.cos(yawRad);
            double sin = Math.sin(yawRad);
            double halfTex = TEX_SIZE * 0.5;
            double outerR = halfTex - 1.0;
            double innerR = outerR - 2.4;
            double gridPerTex = (double) TERRAIN_GRID / TEX_SIZE;

            for (int ty = 0; ty < TEX_SIZE; ty++) {
                double dy = (ty + 0.5) - halfTex;
                int ringCol = lerpColor(currentTheme.accent, currentTheme.secondary, (float) ty / TEX_SIZE);
                for (int tx = 0; tx < TEX_SIZE; tx++) {
                    double dx = (tx + 0.5) - halfTex;
                    double dist = Math.sqrt(dx * dx + dy * dy);
                    int outArgb;

                    if (minimapCircularMode) {
                        if (dist > outerR + 0.8) {
                            outArgb = 0x00000000;
                        } else if (dist > innerR) {
                            // Сглаженное неоновое кольцо (антиалиасинг по краю круга!)
                            float edgeAlpha = (float) Mth.clamp((outerR + 0.8 - dist) / 1.0, 0.0, 1.0);
                            int a = (int) (edgeAlpha * 245.0F);
                            outArgb = (a << 24) | (ringCol & 0x00FFFFFF);
                        } else {
                            outArgb = sampleRealisticTerrainPixel(dx, dy, cos, sin, gridPerTex, dist, innerR);
                        }
                    } else {
                        if (tx < 2 || tx >= TEX_SIZE - 2 || ty < 2 || ty >= TEX_SIZE - 2) {
                            outArgb = 0xF5000000 | (ringCol & 0x00FFFFFF);
                        } else {
                            outArgb = sampleRealisticTerrainPixel(dx, dy, cos, sin, gridPerTex, dist, outerR);
                        }
                    }
                    nativeSetPixelMethod.invoke(gpuNativeImage, tx, ty, toNativePixel(outArgb));
                }
            }
            dynamicUploadMethod.invoke(gpuDynamicTexture);
            lastGpuTextureUploadMs = System.currentTimeMillis();
            lastRenderedMapYaw = playerYaw;
            terrainDirtyForGpu = false;
        } catch (Throwable ignored) {
            gpuMapReady = false;
        }
    }

    private static int sampleRealisticTerrainPixel(double dx, double dy, double cos, double sin,
                                                   double gridPerTex, double dist, double maxR) {
        if (!minimapTerrainEnabled) {
            int base = 0xE80A111A;
            if (Math.abs(dist - maxR * 0.38) < 0.7 || Math.abs(dist - maxR * 0.72) < 0.7) {
                return 0xEE183044;
            }
            return base;
        }

        // Непрерывные координаты на сетке 64x64 для плавной билинейной фильтрации (БЕЗ пиксельных ступенек!)
        double fx = TERRAIN_HALF + (dx * cos + dy * sin) * gridPerTex;
        double fz = TERRAIN_HALF + (-dx * sin + dy * cos) * gridPerTex;
        int x0 = Mth.clamp((int) Math.floor(fx), 0, TERRAIN_GRID - 2);
        int z0 = Mth.clamp((int) Math.floor(fz), 0, TERRAIN_GRID - 2);
        float tx = (float) Mth.clamp(fx - x0, 0.0, 1.0);
        float tz = (float) Mth.clamp(fz - z0, 0.0, 1.0);

        int idx00 = z0 * TERRAIN_GRID + x0;
        int c00 = terrainRgb[idx00];
        int c10 = terrainRgb[idx00 + 1];
        int c01 = terrainRgb[idx00 + TERRAIN_GRID];
        int c11 = terrainRgb[idx00 + TERRAIN_GRID + 1];

        // Билинейная интерполяция цвета между 4 соседними блоками!
        int topC = lerpRgb24(c00, c10, tx);
        int botC = lerpRgb24(c01, c11, tx);
        int rgb = lerpRgb24(topC, botC, tz);

        // Мягкое сглаживание контуров высот каждые 8 блоков (как на топографической карте)
        if (minimapContourLines && !terrainWater[idx00]) {
            int h = terrainHeight[idx00];
            if ((h & 7) == 0 && (terrainHeight[idx00 + 1] != h || terrainHeight[idx00 + TERRAIN_GRID] != h)) {
                rgb = adjustBrightness(rgb, -14);
            }
        }

        // Тонкие полупрозрачные кольца сонара и мягкая виньетка по краям круга
        if (Math.abs(dist - maxR * 0.42) < 0.65 || Math.abs(dist - maxR * 0.76) < 0.65) {
            rgb = lerpRgb24(rgb, currentTheme.accent & 0x00FFFFFF, 0.22F);
        }
        double edgeFade = dist / Math.max(1.0, maxR);
        if (edgeFade > 0.78) {
            int vig = (int) ((edgeFade - 0.78) * 140.0);
            rgb = adjustBrightness(rgb, -vig);
        }
        return 0xF2000000 | (rgb & 0x00FFFFFF);
    }

    private static int lerpRgb24(int c1, int c2, float t) {
        int r1 = (c1 >> 16) & 0xFF, g1 = (c1 >> 8) & 0xFF, b1 = c1 & 0xFF;
        int r2 = (c2 >> 16) & 0xFF, g2 = (c2 >> 8) & 0xFF, b2 = c2 & 0xFF;
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }

    private static void tickTerrainGridScanner(ClientLevel level, int px, int pz) {
        int step = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        int snapX = Math.floorDiv(px, step) * step;
        int snapZ = Math.floorDiv(pz, step) * step;

        if (terrainNeedsReset || terrainLastZoom != radarZoomLevel
                || Math.abs(snapX - terrainOriginX) >= step * 2
                || Math.abs(snapZ - terrainOriginZ) >= step * 2) {
            terrainOriginX = snapX;
            terrainOriginZ = snapZ;
            terrainLastZoom = radarZoomLevel;
            terrainScanRow = 0;
            terrainNeedsReset = false;
        }

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        // Всего 2 строки по 64 блока за тик = ~0.04 мс CPU = 0 влияния на FPS!
        for (int r = 0; r < 2; r++) {
            int gz = terrainScanRow;
            int wz = terrainOriginZ + (gz - TERRAIN_HALF) * step;
            for (int gx = 0; gx < TERRAIN_GRID; gx++) {
                int wx = terrainOriginX + (gx - TERRAIN_HALF) * step;
                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                pos.set(wx, topY, wz);
                BlockState st = level.getBlockState(pos);
                boolean isWater = st.is(Blocks.WATER);
                int rgb;

                if (isWater) {
                    // Реалистичная глубина воды + бирюзовый берег!
                    int depth = 1;
                    for (int d = 1; d <= 6; d++) {
                        pos.set(wx, topY - d, wz);
                        if (level.getBlockState(pos).is(Blocks.WATER)) depth++;
                        else break;
                    }
                    rgb = switch (depth) {
                        case 1 -> 0x38A3C7;
                        case 2 -> 0x2A85B5;
                        case 3 -> 0x226FA3;
                        case 4 -> 0x1B5B8F;
                        default -> 0x144775;
                    };
                } else {
                    MapColor mc = st.getMapColor(level, pos);
                    if (mc.col == 0 && topY > level.getMinY() + 2) {
                        pos.set(wx, topY - 1, wz);
                        st = level.getBlockState(pos);
                        mc = st.getMapColor(level, pos);
                    }
                    rgb = mc.col != 0 ? mc.col : 0x1A2430;
                }

                int idx = gz * TERRAIN_GRID + gx;
                terrainHeight[idx] = topY;
                terrainWater[idx] = isWater;

                // Топографическое объёмное освещение склонов (северо-западный свет)
                if (!isWater) {
                    int shade = 0;
                    if (gz > 0) {
                        int dN = topY - terrainHeight[(gz - 1) * TERRAIN_GRID + gx];
                        shade += Mth.clamp(dN * 12, -26, 24);
                    }
                    if (gx > 0) {
                        int dW = topY - terrainHeight[gz * TERRAIN_GRID + (gx - 1)];
                        shade += Mth.clamp(dW * 6, -14, 14);
                    }
                    rgb = adjustBrightness(rgb, shade);
                }
                terrainRgb[idx] = rgb;
            }
            terrainScanRow = (terrainScanRow + 1) % TERRAIN_GRID;
        }
        terrainDirtyForGpu = true;
    }

    // =========================================================================
    // 12. КАСТОМНЫЕ 3D-МОДЕЛИ ВАДИМКИ, МИНЬОНОВ И ОБЩИЙ ТИК МИРА
    // =========================================================================

    private static void cleanupWorldCompanions() {
        if (companionCoreEntity != null) { companionCoreEntity.discard(); companionCoreEntity = null; }
        if (companionVisorStand != null) { companionVisorStand.discard(); companionVisorStand = null; }
        spawnedForm = null;
        for (Mob m : minionEntities.values()) {
            if (m != null) m.discard();
        }
        minionEntities.clear();
    }

    private static LivingEntity getCustomStudioModel(ClientLevel level, Player player, VadimkaForm form) {
        LivingEntity cached = guiStageModels.get(form);
        if (cached != null && !cached.isRemoved() && cached.level() == level) return cached;
        LivingEntity created = buildCustomVisualEntity(level, player, form, true);
        if (created != null) guiStageModels.put(form, created);
        return created != null ? created : player;
    }

    private static Mob getGuiVillagerModel(ClientLevel level) {
        if (guiVillagerModel != null && !guiVillagerModel.isRemoved() && guiVillagerModel.level() == level) {
            return guiVillagerModel;
        }
        Mob v = EntityType.VILLAGER.create(level, EntitySpawnReason.LOAD);
        if (v != null) { v.setNoAi(true); v.setSilent(true); guiVillagerModel = v; }
        return guiVillagerModel;
    }

    private static LivingEntity buildCustomVisualEntity(ClientLevel level, Player player, VadimkaForm form, boolean forGui) {
        if (form == VadimkaForm.CYBER_PLAYER) {
            ArmorStand bot = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.LOAD);
            if (bot != null) {
                bot.setNoGravity(true); bot.setSilent(true); bot.setInvulnerable(true);
                bot.setShowArms(true); bot.setNoBasePlate(true);
                ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
                chest.set(DataComponents.DYED_COLOR, new DyedItemColor(0x0E7490));
                ItemStack legs = new ItemStack(Items.LEATHER_LEGGINGS);
                legs.set(DataComponents.DYED_COLOR, new DyedItemColor(0x1E293B));
                bot.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
                bot.setItemSlot(EquipmentSlot.CHEST, chest);
                bot.setItemSlot(EquipmentSlot.LEGS, legs);
                bot.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                bot.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.END_CRYSTAL));
                if (!forGui) applyScaleAttribute(bot, form.worldScale * (companionScalePercent / 100.0));
                return bot;
            }
        }
        if (form == VadimkaForm.AXOLOTL) {
            Axolotl ax = EntityType.AXOLOTL.create(level, EntitySpawnReason.LOAD);
            if (ax != null) {
                ax.setNoAi(true); ax.setNoGravity(true); ax.setSilent(true); ax.setInvulnerable(true);
                setAxolotlBlueVariant(ax);
                if (!forGui) applyScaleAttribute(ax, form.worldScale * (companionScalePercent / 100.0));
                return ax;
            }
        }
        if (form == VadimkaForm.RAT) {
            Mob rat = EntityType.SILVERFISH.create(level, EntitySpawnReason.LOAD);
            if (rat != null) {
                rat.setNoAi(true); rat.setNoGravity(true); rat.setSilent(true); rat.setInvulnerable(true);
                if (!forGui) applyScaleAttribute(rat, form.worldScale * (companionScalePercent / 100.0));
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
            mob.setNoAi(true); mob.setNoGravity(true); mob.setSilent(true); mob.setInvulnerable(true);
            if (!forGui) applyScaleAttribute(mob, form.worldScale * (companionScalePercent / 100.0));
        }
        return mob;
    }

    private static void setAxolotlBlueVariant(Axolotl ax) {
        try {
            for (Method m : Axolotl.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers()) && !m.isSynthetic()
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
        if (inst != null) inst.setBaseValue(Mth.clamp(scale, 0.25, 2.0));
    }

    private static void tickWorldLogic(Minecraft mc) {
        ClientLevel level = mc.level;
        Player player = mc.player;
        if (level == null || player == null) return;

        worldTick++;
        if (toastBannerTicks > 0) toastBannerTicks--;

        // 1. Реальный Windows 10 Spotify Connect (раз в 2.8с в фоновом потоке)
        if (spotifyHudEnabled || mc.screen instanceof AssistantVadimkaScreen) {
            pollWindowsMediaAsync(false);
            if (spotifyConnected && spotifyIsPlaying && !spotifyTrackTitle.equals(lastNotifiedTrack)
                    && !spotifyTrackTitle.startsWith("Ожидание") && !spotifyTrackTitle.startsWith("Spotify не играет")) {
                lastNotifiedTrack = spotifyTrackTitle;
                if (worldTick > 60) showToast("♫ " + spotifyTrackArtist + " — " + spotifyTrackTitle, 75);
            }
        }

        // 2. Приветствие при входе в мир
        if (!greetedInWorld && worldTick > 25) {
            greetedInWorld = true;
            if (chatAssistantEnabled && !quietMode) {
                player.displayClientMessage(Component.literal(
                    "§b[✦ Вадимка v8.0] §fГотов (75+ FPS)! Команды: §a!к дому§f, §a!выбей трезубец§f, §a!перебор починка§f, §a!деревня§f, §e[P]§f."
                ), false);
            }
        }

        // 3. Отложенные ответы Вадимки в чат
        if (!pendingChatReplies.isEmpty()) {
            if (pendingChatReplyDelay > 0) {
                pendingChatReplyDelay--;
            } else {
                String msg = pendingChatReplies.pollFirst();
                if (msg != null) player.displayClientMessage(Component.literal("§b[✦ Вадимка] §f" + msg), false);
                pendingChatReplyDelay = 3;
            }
        }

        // 4. Запоминаем блок сундука/хранилища под прицелом
        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = bhr.getBlockPos();
            BlockState bs = level.getBlockState(bp);
            if (bs.is(Blocks.VAULT) || isRegularLootContainer(bs)) {
                lastLookedContainerPos = bp.immutable();
                lastLookedContainerName = bs.getBlock().getName().getString();
            }
        }

        // 5. Запись сундуков & торгов + Авто-перебор кафедры + Снайпер Хранилищ + Автопилот + Хотбар
        scanOpenContainerOrMerchant(mc, player);
        tickAutoCycleLectern(mc, level, player);
        tickSmartVaultLootSniper(mc, level, player);
        tickSmartAutopilot(mc, level, player);
        tickHotbarAutoRefill(mc, player);

        // Авто-спринт когда не работает автопилот
        if (!safeAutopilot && sprintAssist && mc.screen == null && mc.options.keyUp.isDown()
                && !player.isCrouching() && !player.isUsingItem() && player.getFoodData().getFoodLevel() > 6) {
            player.setSprinting(true);
        }

        // Профайлер движения и проверка обрыва
        Vec3 curPos = player.position();
        if (prevPlayerPos != null) {
            double d = curPos.distanceTo(prevPlayerPos);
            if (d < 10.0) sessionWalkedBlocks += d;
        }
        prevPlayerPos = curPos;

        if ((fallDangerAlert || parkourEdgeGuard) && worldTick % 5 == 0) {
            BlockPos below = player.blockPosition().below();
            fallDropBlocks = level.getBlockState(below).isAir() ? measureDropBelow(level, player.blockPosition()) : 0;
            edgeDangerNow = parkourEdgeGuard && fallDropBlocks >= 4;
        }

        // Здоровье, урон, точка смерти
        float hp = player.getHealth();
        if (hp <= 0.0F && prevPlayerHealth > 0.0F && deathReplayMarker) {
            lastDeathPos = player.blockPosition();
            setNavigationTarget(lastDeathPos, "Точка смерти", 0xFFEF4444);
            showToast("Вадимка: Навигатор наведён на точку смерти!", 110);
        } else if (hp < prevPlayerHealth - 0.1F) {
            if (safeAutopilot && autoReturnControl) {
                safeAutopilot = false;
                releaseAutopilotKeys(mc);
                showToast("⚠ Автопилот остановлен: получен урон!", 80);
            }
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

        // 6. Сущности вокруг (раз в 5 тиков)
        if (worldTick % 5 == 0) {
            scanEntitiesAround(level, player);
        }

        // 7. 3D-спутник и 4 миньона
        updateCustomCompanionInWorld(mc, level, player);

        // 8. 3D-крылья и нимб в F5
        if (worldCosmeticsF5 && worldTick % 2 == 0 && !mc.options.getCameraType().isFirstPerson()) {
            spawnWorldCosmetics(level, player);
        }

        // 9. Частицы Хранилищ и 3D-луч Навигатора
        if (worldTick % 6 == 0) {
            spawnWorldBeaconsAndVaultHighlights(level, player);
        }

        // 10. Нить Ариадны
        if (ariadneThread) {
            Vec3 crumb = new Vec3(player.getX(), player.getY() + 0.22, player.getZ());
            if (lastAriadnePoint == null || lastAriadnePoint.distanceToSqr(crumb) > 12.25) {
                ariadnePoints.addLast(crumb);
                lastAriadnePoint = crumb;
                while (ariadnePoints.size() > 36) ariadnePoints.removeFirst();
            }
            if (worldTick % 12 == 0) {
                int idx = 0;
                for (Vec3 c : ariadnePoints) {
                    if ((idx + worldTick / 12) % 4 == 0 && c.distanceToSqr(crumb) < 324.0) {
                        level.addParticle(ParticleTypes.ENCHANT, c.x, c.y + 0.15, c.z, 0.0, 0.015, 0.0);
                    }
                    idx++;
                }
            }
        }

        // 11. Обновление 64x64 сетки рельефа (всего 2 строки/тик) + GPU загрузка 128x128 (макс 6 раз/сек!)
        if (minimapRadarEnabled && minimapTerrainEnabled) {
            tickTerrainGridScanner(level, Mth.floor(player.getX()), Mth.floor(player.getZ()));
        }
        if (minimapRadarEnabled && gpuMapReady) {
            long now = System.currentTimeMillis();
            boolean yawMoved = Math.abs(Mth.wrapDegrees(player.getYRot() - lastRenderedMapYaw)) > 2.5F;
            if ((terrainDirtyForGpu && now - lastGpuTextureUploadMs > 260L) || (yawMoved && now - lastGpuTextureUploadMs > 120L)) {
                rebuildGpuMinimapTexture(player.getYRot());
            }
        }

        // 12. Лёгкий сканер Хранилищ и сундуков (1 срез раз в 3 тика)
        if (worldTick % 3 == 0) {
            tickVaultAndChestScanner(level, player.blockPosition());
        }
    }

    private static int measureDropBelow(ClientLevel level, BlockPos start) {
        int drop = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(start.getX(), start.getY() - 1, start.getZ());
        for (int i = 0; i < 14; i++) {
            BlockState st = level.getBlockState(pos);
            if (st.is(Blocks.WATER)) return 0;
            if (!st.isAir()) break;
            drop++;
            pos.setY(pos.getY() - 1);
        }
        return drop;
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
                    if (shown >= 8) break;
                }
            }
        }

        if (activeNavTarget != null) {
            double tx = activeNavTarget.getX() + 0.5;
            double ty = Math.max(player.getY() - 2.0, activeNavTarget.getY() + 1.0);
            double tz = activeNavTarget.getZ() + 0.5;
            if (activeNavTarget.distSqr(player.blockPosition()) <= 1600.0) {
                for (int h = 0; h < 6; h++) {
                    level.addParticle(
                        new DustParticleOptions(activeNavColor & 0x00FFFFFF, 1.35F),
                        tx, ty + h * 1.5, tz, 0.0, 0.05, 0.0
                    );
                }
            }
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
                            new DustParticleOptions(activeNavColor & 0x00FFFFFF, 1.0F),
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
                        boolean matches = matchesTargetEnchant(res, want);
                        boolean good = matches || offer.getResult().is(Items.ENCHANTED_BOOK);
                        VillagerTradeEntry entry = new VillagerTradeEntry(cA + cB, res, emPrice, good, offer.isOutOfStock());
                        rec.trades.add(entry);
                        if (matches && emPrice <= maxEmeraldPrice && jackpot == null) jackpot = entry;
                    }
                    rememberedVillagers.put(vPos, rec);
                    if (!wasMerchantOpen && !autoCycleLectern) {
                        if (jackpot != null) {
                            String jMsg = "★ НАЙДЕНЫ НУЖНЫЕ ЧАРЫ: " + jackpot.resultText + " за " + jackpot.costText + "!";
                            showToast(jMsg, 120);
                            player.displayClientMessage(Component.literal("§a[✦ Вадимка] " + jMsg), false);
                            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.85F, 1.0F);
                        } else {
                            showToast("Библиотекарь записан (" + rec.trades.size() + " сделок)", 55);
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
                    if (!st.isEmpty()) rec.items.merge(formatItemWithEnchants(st), st.getCount(), Integer::sum);
                }
                rememberedChests.put(lastLookedContainerPos, rec);
                if (!wasContainerOpen) showToast("Ларчик запомнен (" + rec.items.size() + " видов вещей)", 55);
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
        if (!companionEnabled) { cleanupWorldCompanions(); return; }

        boolean needRebuild = companionCoreEntity == null || companionCoreEntity.isRemoved()
            || companionCoreEntity.level() != level || spawnedForm != currentForm;

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
                    coreOrb.setNoAi(true); coreOrb.setNoGravity(true); coreOrb.setSilent(true); coreOrb.setInvulnerable(true);
                    coreOrb.setCustomNameVisible(false);
                    applyScaleAttribute(coreOrb, 0.34 * (companionScalePercent / 100.0));
                    coreOrb.setPos(player.getX() + 1.0, player.getY() + 1.4, player.getZ() + 1.0);
                    level.addEntity(coreOrb);

                    companionVisorStand = stand; companionCoreEntity = coreOrb;
                    spawnedForm = currentForm; spawnedScale = companionScalePercent;
                }
            } else {
                LivingEntity ent = buildCustomVisualEntity(level, player, currentForm, false);
                if (ent instanceof Mob mob) {
                    mob.setId(-77001);
                    mob.setCustomName(Component.literal("§b✦ Вадимка"));
                    mob.setCustomNameVisible(true);
                    mob.setPos(player.getX() + 1.0, player.getY() + 1.0, player.getZ() + 1.0);
                    level.addEntity(mob);
                    companionCoreEntity = mob; spawnedForm = currentForm; spawnedScale = companionScalePercent;
                }
            }
        } else if (spawnedScale != companionScalePercent) {
            if (companionVisorStand != null) applyScaleAttribute(companionVisorStand, currentForm.worldScale * (companionScalePercent / 100.0));
            if (companionCoreEntity != null) {
                double mul = (currentForm == VadimkaForm.CYBER_PLAYER) ? 0.34 : currentForm.worldScale;
                applyScaleAttribute(companionCoreEntity, mul * (companionScalePercent / 100.0));
            }
            spawnedScale = companionScalePercent;
        }

        double yawRad = Math.toRadians(player.getYRot());
        double rightX = -Math.cos(yawRad), rightZ = -Math.sin(yawRad);
        double forwardX = -Math.sin(yawRad), forwardZ = Math.cos(yawRad);
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
            if (worldTick % 14 == 0) {
                level.addParticle(new DustParticleOptions(currentForm.primaryColor & 0x00FFFFFF, 0.85F), nx, ny + 0.25, nz, 0.0, 0.02, 0.0);
            }
        }

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
                    m.setNoAi(true); m.setNoGravity(true); m.setSilent(true); m.setInvulnerable(true);
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
                    } else if (role == MinionRole.BUILDER && mc.hitResult instanceof BlockHitResult bhr
                            && bhr.getType() == HitResult.Type.BLOCK && !player.getMainHandItem().isEmpty()) {
                        Vec3 hit = bhr.getLocation();
                        if (hit.distanceToSqr(player.position()) < 36.0) { tx = hit.x; ty = hit.y + 0.95; tz = hit.z; }
                    }
                }
                existing.xo = existing.getX(); existing.yo = existing.getY(); existing.zo = existing.getZ();
                existing.setPos(Mth.lerp(0.15, existing.getX(), tx), Mth.lerp(0.15, existing.getY(), ty), Mth.lerp(0.15, existing.getZ(), tz));
                float mYaw = (float) (Mth.atan2(player.getZ() - existing.getZ(), player.getX() - existing.getX()) * (180.0 / Math.PI)) - 90.0F;
                existing.setYRot(mYaw); existing.setYHeadRot(mYaw);
            }
        }
    }

    private static void spawnWorldCosmetics(ClientLevel level, Player player) {
        double yawRad = Math.toRadians(player.getYRot());
        double backX = Math.sin(yawRad), backZ = -Math.cos(yawRad);
        double rightX = -Math.cos(yawRad), rightZ = -Math.sin(yawRad);
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
                    level.addParticle(new DustParticleOptions(col, 0.95F),
                        player.getX() + backX * back + rightX * (side * spread),
                        player.getY() + up,
                        player.getZ() + backZ * back + rightZ * (side * spread), 0.0, 0.0, 0.0);
                }
            }
        }
        if (hornsIndex == 4 && worldTick % 4 == 0) {
            double ang = (worldTick * 0.35) % (Math.PI * 2.0);
            level.addParticle(ParticleTypes.END_ROD, player.getX() + Math.cos(ang) * 0.34, player.getY() + 2.16, player.getZ() + Math.sin(ang) * 0.34, 0.0, 0.0, 0.0);
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
            if (Math.abs(dx) > 64.0 || Math.abs(dz) > 64.0) continue;

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
                    if (dist < 10.0 && dist > 0.1 && ((dx / dist) * lookX + (dz / dist) * lookZ) < -0.25) {
                        threatBehindPlayer = true;
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

    private static void tickVaultAndChestScanner(ClientLevel level, BlockPos center) {
        if (vaultScanSliceX == -VAULT_SCAN_RADIUS) {
            vaultScanOrigin = center;
            scanVaultBuffer.clear();
        }
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinY(), vaultScanOrigin.getY() - 6);
        int maxY = Math.min(level.getMaxY() - 1, vaultScanOrigin.getY() + 6);
        int wx = vaultScanOrigin.getX() + vaultScanSliceX;

        for (int dz = -VAULT_SCAN_RADIUS; dz <= VAULT_SCAN_RADIUS; dz++) {
            int wz = vaultScanOrigin.getZ() + dz;
            for (int y = minY; y <= maxY; y++) {
                pos.set(wx, y, wz);
                BlockState st = level.getBlockState(pos);
                VaultInfo vi = inspectPotentialVault(level, pos, st);
                if (vi != null && scanVaultBuffer.size() < 40) scanVaultBuffer.add(vi);
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
    // 13. ИГРОВОЙ HUD (1-CALL GPU КАРТА + АВТОПИЛОТ + СНАЙПЕР + ПЕРЕБОР + SPOTIFY)
    // =========================================================================

    private static void renderGameplayHud(GuiGraphics g) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;
        if (mc.screen instanceof AssistantVadimkaScreen) return;

        int sw = mc.getWindow().getGuiScaledWidth();
        int sh = mc.getWindow().getGuiScaledHeight();

        if (visualsEnabled && minimapRadarEnabled) {
            renderUltraFastRealisticMinimap(g, mc);
        }
        if (visualsEnabled && elytraPitchHorizon && mc.player.isFallFlying()) {
            renderElytraHorizonHud(g, mc.font, mc.player, sw, sh);
        }
        if (crosshairIndex > 0 && mc.screen == null) {
            renderCustomCrosshair(g, sw, sh);
        }

        // Верхняя полоса приоритетных статусов: Угроза / Автопилот / Перебор кафедры / Снайпер Хранилищ / Навигатор
        int topY = 8;
        if (backstabIndicator && threatBehindPlayer) {
            drawTopPill(g, mc.font, sw, topY, "⚠ УГРОЗА СЗАДИ (" + String.format("%.1f", nearestHostileDist) + "м)", 0xE63B0A12, 0xFFEF4444, 0xFFFCA5A5);
            topY += 21;
        } else if (fallDangerAlert && fallDropBlocks >= 6 && mc.screen == null) {
            drawTopPill(g, mc.font, sw, topY, "⚠ ОБРЫВ: " + fallDropBlocks + " бл.", 0xE63B2508, 0xFFF59E0B, 0xFFFDE68A);
            topY += 21;
        }

        if (activeNavTarget != null) {
            renderActiveNavigatorBanner(g, mc.font, mc.player, sw, topY);
            topY += 21;
        }
        if (autoCycleLectern) {
            drawTopPill(g, mc.font, sw, topY, "⚙ БИБЛИОТЕКАРЬ: " + lecternStatusMessage + " (Shift — стоп)", 0xE6141126, 0xFFC084FC, 0xFFE9D5FF);
            topY += 21;
        }
        if (autoClickVaults && (countNearbyVaultBlocks() > 0 || lastVaultSpinningLabel.startsWith("Внутри:"))) {
            int bd = lastVaultMatchedNow ? 0xFF4ADE80 : 0xFFF97316;
            drawTopPill(g, mc.font, sw, topY, "✦ СНАЙПЕР: " + lastVaultSpinningLabel, 0xE61A120A, bd, lastVaultMatchedNow ? 0xFF86EFAC : 0xFFFDE68A);
        }

        if (litematicaHudEnabled) renderLitematicaRightHud(g, mc, sw);
        if (combatHudEnabled) renderCombatPvpHud(g, mc, sw, sh);
        if (spotifyHudEnabled) renderSpotifyBottomHud(g, mc, sh);

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

    private static void renderActiveNavigatorBanner(GuiGraphics g, Font font, Player player, int sw, int y) {
        if (activeNavTarget == null) return;
        double dx = (activeNavTarget.getX() + 0.5) - player.getX();
        double dz = (activeNavTarget.getZ() + 0.5) - player.getZ();
        int dist = (int) Math.sqrt(dx * dx + dz * dz);
        double rel = Mth.wrapDegrees((Math.toDegrees(Math.atan2(dz, dx)) - 90.0) - player.getYRot());
        String arrow = Math.abs(rel) < 22.5 ? "⬆"
            : (rel >= 22.5 && rel < 67.5 ? "↗"
            : (rel >= 67.5 && rel < 112.5 ? "➡"
            : (rel >= 112.5 && rel < 157.5 ? "↘"
            : (rel <= -22.5 && rel > -67.5 ? "↖"
            : (rel <= -67.5 && rel > -112.5 ? "⬅"
            : (rel <= -112.5 && rel > -157.5 ? "↙" : "⬇"))))));
        String prefix = safeAutopilot ? "▶ АВТОПИЛОТ " : "";
        String suffix = safeAutopilot ? " (S/Shift — стоп)" : "";
        String title = activeNavTitle.isEmpty() ? "Дом" : activeNavTitle;
        String label = prefix + arrow + " " + title + " • " + dist + "м" + suffix;
        drawTopPill(g, font, sw, y, label, currentTheme.bgWindow, safeAutopilot ? 0xFF4ADE80 : activeNavColor, 0xFFFFFFFF);
    }

    private static void renderUltraFastRealisticMinimap(GuiGraphics g, Minecraft mc) {
        initGpuMinimapTextureOnce(mc);
        int x = 10;
        int y = 30;
        int mapDispSize = 100; // Идеальный размер: чётко видно, не задевает оверлей Discord!
        int half = mapDispSize / 2;
        int cx = x + half;
        int cy = y + half;

        boolean drewGpu = false;
        if (gpuMapReady && guiBlitMethod != null && gpuTextureId != null) {
            if (lastGpuTextureUploadMs == 0L) {
                rebuildGpuMinimapTexture(mc.player.getYRot());
            }
            try {
                if (guiBlitStyle == 1 && gpuRenderPipeline != null) {
                    guiBlitMethod.invoke(g, gpuRenderPipeline, gpuTextureId, x, y, 0.0F, 0.0F, mapDispSize, mapDispSize, mapDispSize, mapDispSize);
                    drewGpu = true;
                } else if (guiBlitStyle == 2) {
                    guiBlitMethod.invoke(g, gpuTextureId, x, y, 0.0F, 0.0F, mapDispSize, mapDispSize, mapDispSize, mapDispSize);
                    drewGpu = true;
                }
            } catch (Throwable ignored) {
                gpuMapReady = false;
            }
        }

        if (!drewGpu) {
            // Сверхлёгкий аварийный фоллбэк: 25x25 клеток по 4px (макс ~70 RLE вызовов = 75+ FPS!)
            fillGlassCard(g, x - 2, y - 2, mapDispSize + 4, mapDispSize + 4, currentTheme.bgWindow, currentTheme.accent);
            if (minimapTerrainEnabled) {
                int cell = 4;
                int cnt = mapDispSize / cell;
                for (int gy = 0; gy < cnt; gy++) {
                    int sz = (gy * TERRAIN_GRID) / cnt;
                    int py = y + gy * cell;
                    int gx = 0;
                    while (gx < cnt) {
                        int sx = (gx * TERRAIN_GRID) / cnt;
                        int col = 0xF0000000 | terrainRgb[sz * TERRAIN_GRID + sx];
                        int run = gx + 1;
                        while (run < cnt) {
                            int sx2 = (run * TERRAIN_GRID) / cnt;
                            if ((0xF0000000 | terrainRgb[sz * TERRAIN_GRID + sx2]) != col) break;
                            run++;
                        }
                        g.fill(x + gx * cell, py, x + run * cell, py + cell, col);
                        gx = run;
                    }
                }
            }
        }

        // Оверлей меток поверх GPU-текстуры (~15-25 вызовов = 0 влияния на FPS!)
        int step = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        double yawRad = Math.toRadians(mc.player.getYRot() + 180.0);
        double cos = drewGpu ? Math.cos(yawRad) : 1.0;
        double sin = drewGpu ? Math.sin(yawRad) : 0.0;
        double scale = ((double) mapDispSize / TERRAIN_GRID) / step;
        double maxR = half - 5.0;

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

        if (highlightChestsWorld || highlightTrialVaults) {
            for (VaultInfo vi : nearbyVaults) {
                double dx = (vi.pos.getX() + 0.5) - mc.player.getX();
                double dz = (vi.pos.getZ() + 0.5) - mc.player.getZ();
                double rX = (dx * cos - dz * sin) * scale;
                double rY = (dx * sin + dz * cos) * scale;
                if (rX * rX + rY * rY <= maxR * maxR) {
                    int mx = cx + (int) rX, my = cy + (int) rY;
                    int c = vi.isVaultBlock ? (vi.ominous ? 0xFFEC4899 : 0xFFF97316) : 0xFFF59E0B;
                    g.fill(mx - 1, my - 1, mx + 2, my + 2, 0xFF000000);
                    g.fill(mx, my, mx + 1, my + 1, c);
                }
            }
        }

        for (RadarBlip b : radarBlips) {
            double dx = b.x - mc.player.getX();
            double dz = b.z - mc.player.getZ();
            double rX = (dx * cos - dz * sin) * scale;
            double rY = (dx * sin + dz * cos) * scale;
            if (rX * rX + rY * rY > maxR * maxR) continue;
            int bx = cx + (int) rX, by = cy + (int) rY;
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

        // Активный маяк Навигатора по краю карты
        BlockPos pin = activeNavTarget != null ? activeNavTarget : homeWaypoint;
        if (pin != null) {
            double dx = (pin.getX() + 0.5) - mc.player.getX();
            double dz = (pin.getZ() + 0.5) - mc.player.getZ();
            double rX = (dx * cos - dz * sin) * scale;
            double rY = (dx * sin + dz * cos) * scale;
            double d = Math.sqrt(rX * rX + rY * rY);
            if (d > maxR) { rX = (rX / d) * maxR; rY = (rY / d) * maxR; }
            int hx = cx + (int) rX, hy = cy + (int) rY;
            g.fill(hx - 3, hy - 3, hx + 4, hy + 4, 0xFF000000);
            g.fill(hx - 2, hy - 2, hx + 3, hy + 3, activeNavColor);
        }

        // Маркер Севера (N) и стрелка игрока
        int nx = cx + (int) (-sin * (maxR - 1)) - 2;
        int ny = cy + (int) (-cos * (maxR - 1)) - 3;
        g.fill(nx - 1, ny - 1, nx + 7, ny + 8, 0xDD080D14);
        g.drawString(mc.font, "N", nx, ny, 0xFF22D3EE);

        g.fill(cx - 2, cy - 1, cx + 3, cy + 2, 0xFF000000);
        g.fill(cx - 1, cy - 2, cx + 2, cy + 1, 0xFFFFFFFF);
        g.fill(cx, cy - 4, cx + 1, cy - 1, currentTheme.accent);

        // Компактная плашка координат прямо под картой (выше Discord!)
        int iy = y + mapDispSize + 3;
        fillGlassCard(g, x, iy, mapDispSize, 22, currentTheme.bgWindow, currentTheme.secondary);
        String cStr = (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
        g.drawString(mc.font, trimText(mc.font, cStr, mapDispSize - 8), x + 4, iy + 3, 0xFFF1F5F9);
        String subStr = (pin != null)
            ? ("→ " + (activeNavTitle.isEmpty() ? "Дом" : activeNavTitle) + " " + (int) Math.sqrt(mc.player.blockPosition().distSqr(pin)) + "м")
            : ("✦ " + currentForm.russianName);
        g.drawString(mc.font, trimText(mc.font, subStr, mapDispSize - 8), x + 4, iy + 12, currentTheme.accent);
    }

    private static void renderElytraHorizonHud(GuiGraphics g, Font font, Player player, int sw, int sh) {
        int cx = sw / 2, cy = sh / 2;
        float pitch = player.getXRot();
        int shiftY = (int) Mth.clamp(-pitch * 0.8F, -36.0F, 36.0F);
        g.fill(cx - 34, cy + shiftY, cx - 10, cy + shiftY + 1, 0xAA22D3EE);
        g.fill(cx + 10, cy + shiftY, cx + 34, cy + shiftY + 1, 0xAA22D3EE);
        g.drawString(font, String.format("%.0f°", -pitch), cx + 38, cy + shiftY - 3, 0xFF22D3EE);
    }

    private static void renderCustomCrosshair(GuiGraphics g, int sw, int sh) {
        int cx = sw / 2, cy = sh / 2, col = currentTheme.accent;
        if (crosshairIndex == 1) {
            g.fill(cx - 1, cy - 1, cx + 1, cy + 1, 0xFFFFFFFF);
            g.fill(cx - 6, cy, cx - 2, cy + 1, col); g.fill(cx + 2, cy, cx + 6, cy + 1, col);
            g.fill(cx, cy - 6, cx + 1, cy - 2, col); g.fill(cx, cy + 2, cx + 1, cy + 6, col);
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
        int w = 156, h = 30;
        int x = litematicaHudRight ? (sw - w - 10) : 120;
        int y = 30;
        fillGlassCard(g, x, y, w, h, currentTheme.bgWindow, currentTheme.borderMuted);
        if (!hand.isEmpty()) g.renderItem(hand, x + 4, y + 7);
        int tx = hand.isEmpty() ? (x + 6) : (x + 24);
        String line1 = hand.isEmpty() ? "Litematica HUD" : (hand.getHoverName().getString() + " x" + count);
        int col1 = (deficitHighlight && !hand.isEmpty() && count < 16) ? 0xFFFCA5A5 : 0xFFF8FAFC;
        g.drawString(mc.font, trimText(mc.font, line1, w - (tx - x) - 6), tx, y + 5, col1);

        String line2 = "Хранилищ: " + countNearbyVaultBlocks() + " • Y:" + (int) p.getY();
        if (ruler3DVolume && mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK && mc.level != null) {
            double dist = p.position().distanceTo(bhr.getLocation());
            int rs = redstoneInspector ? mc.level.getBestNeighborSignal(bhr.getBlockPos()) : 0;
            line2 = String.format("Блок: %.1fм • Y:%d%s", dist, bhr.getBlockPos().getY(), rs > 0 ? (" RS:" + rs) : "");
        }
        g.drawString(mc.font, trimText(mc.font, line2, w - (tx - x) - 6), tx, y + 17, currentTheme.accent);
    }

    private static void renderCombatPvpHud(GuiGraphics g, Minecraft mc, int sw, int sh) {
        Player p = mc.player;
        if (p == null) return;
        int totems = countPlayerTotems(p);
        int pearls = 0, gapples = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(Items.ENDER_PEARL)) pearls += st.getCount();
            else if (st.is(Items.GOLDEN_APPLE) || st.is(Items.ENCHANTED_GOLDEN_APPLE)) gapples += st.getCount();
        }
        int w = 148, h = 48;
        int x = sw - w - 10, y = sh - h - 10;
        fillGlassCard(g, x, y, w, h, currentTheme.bgWindow, currentTheme.borderMuted);
        g.drawString(mc.font, trimText(mc.font, "Тотем:" + totems + "  Перл:" + pearls + "  Ябл:" + gapples, w - 10), x + 6, y + 5, 0xFFFDE047);

        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] labels = {"Ш", "Н", "П", "Б"};
        int bx = x + 6, by = y + 19;
        for (int i = 0; i < slots.length; i++) {
            ItemStack armor = p.getItemBySlot(slots[i]);
            int pct = 0;
            if (!armor.isEmpty() && armor.isDamageableItem() && armor.getMaxDamage() > 0) {
                pct = (int) Math.round(((armor.getMaxDamage() - armor.getDamageValue()) * 100.0) / armor.getMaxDamage());
            } else if (!armor.isEmpty()) pct = 100;
            int col = pct > 55 ? 0xFF4ADE80 : (pct > 25 ? 0xFFFACC15 : (pct > 0 ? 0xFFEF4444 : 0xFF334155));
            g.drawString(mc.font, labels[i] + ":" + pct + "%", bx + i * 34, by, 0xFFCBD5E1);
            g.fill(bx + i * 34, by + 11, bx + i * 34 + 30, by + 15, 0xFF1E293B);
            if (pct > 0) g.fill(bx + i * 34, by + 11, bx + i * 34 + Math.max(2, (30 * pct) / 100), by + 15, col);
        }
    }

    private static void renderSpotifyBottomHud(GuiGraphics g, Minecraft mc, int sh) {
        int w = 192, h = 34, x = 10, y = sh - h - 10;
        fillGlassCard(g, x, y, w, h, 0xEE0A1710, spotifyIsPlaying ? 0xFF1DB954 : 0xFF334155);
        g.fillGradient(x + 5, y + 5, x + 29, y + 29, 0xFF1E3A8A, 0xFF065F46);
        long t = System.currentTimeMillis() / 120L;
        for (int i = 0; i < 4; i++) {
            int bh = spotifyIsPlaying ? (4 + (int) (Math.abs(Math.sin(t * 0.8 + i * 1.3)) * 14)) : 3;
            g.fill(x + 8 + i * 5, y + 27 - bh, x + 11 + i * 5, y + 27, spotifyIsPlaying ? 0xFF4ADE80 : 0xFF64748B);
        }
        g.drawString(mc.font, trimText(mc.font, spotifyTrackTitle, w - 40), x + 34, y + 5, 0xFFFFFFFF);
        g.drawString(mc.font, trimText(mc.font, spotifyTrackArtist, w - 40), x + 34, y + 15, spotifyIsPlaying ? 0xFF86EFAC : 0xFF94A3B8);
        int barX = x + 34, barW = w - 40;
        g.fill(barX, y + 26, barX + barW, y + 28, 0xFF1F2937);
        float prog = spotifyDurSec > 0 ? Mth.clamp((float) spotifyPosSec / spotifyDurSec, 0.0F, 1.0F) : (spotifyIsPlaying ? 0.5F : 0.0F);
        g.fill(barX, y + 26, barX + (int) (barW * prog), y + 28, 0xFF1DB954);
    }

    // =========================================================================
    // 14. ГРАФИЧЕСКИЕ ПРИМИТИВЫ И ИКОНКИ ВСЕХ 9 ВКЛАДОК
    // =========================================================================

    private static void fillGlassCard(GuiGraphics g, int x, int y, int w, int h, int fillCol, int borderCol) {
        fillRoundedRect(g, x, y, w, h, fillCol);
        drawRoundedBorder(g, x, y, w, h, borderCol);
    }

    private static void drawGradientNeonBorder(GuiGraphics g, int x, int y, int w, int h, int colLeft, int colRight) {
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
            if (font.width(sb.toString()) > limit) { sb.setLength(Math.max(0, sb.length() - 1)); break; }
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
            case AUTOPILOT -> {
                g.fill(x + 4, y + 1, x + 6, y + 9, col);
                g.fill(x + 3, y + 2, x + 7, y + 4, col);
                g.fill(x + 2, y + 4, x + 8, y + 6, col);
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
            case CUSTOM_VISUAL -> {
                g.fill(x + 1, y + 3, x + 3, y + 8, col);
                g.fill(x + 4, y + 2, x + 6, y + 8, col);
                g.fill(x + 7, y + 3, x + 9, y + 8, col);
                g.fill(x + 1, y + 6, x + 9, y + 8, col);
            }
        }
    }

    // =========================================================================
    // 15. ПРЕМИУМ 3-КОЛОНОЧНОЕ МЕНЮ КОНЦЕПТА (9 ВКЛАДОК, 0 ЛАГОВ В GUI!)
    // =========================================================================

    public static class AssistantVadimkaScreen extends Screen {
        private MenuTab activeTab;
        private int rightPage = 0;
        private int rightTotalPages = 1;

        private boolean showChatOverlay = false;
        private int chestsCenterTab = 0; // 0=3D Сцена, 1=Ларчики, 2=Жители
        private int spotifyCenterTab = 0; // 0=Spotify Connect (ПК), 1=AFK Игры

        private EditBox activeEditBox = null;
        private String chatInputDraft = "";

        private static class NavItemVis {
            final MenuTab tab;
            final int x, y, w, h;

            NavItemVis(MenuTab tab, int x, int y, int w, int h) {
                this.tab = tab; this.x = x; this.y = y; this.w = w; this.h = h;
            }
        }

        private static class RightControlVis {
            final int x, y, w, h, ctrlX, ctrlY, ctrlW, ctrlH;
            final String title, subtitle, valueText;
            final int kind; // 0=toggle, 1=slider, 2=cycle
            final boolean toggleVal;
            final float sliderPct;

            RightControlVis(int x, int y, int w, int h, int ctrlX, int ctrlY, int ctrlW, int ctrlH,
                            String title, String subtitle, int kind, boolean toggleVal, float sliderPct, String valueText) {
                this.x = x; this.y = y; this.w = w; this.h = h;
                this.ctrlX = ctrlX; this.ctrlY = ctrlY; this.ctrlW = ctrlW; this.ctrlH = ctrlH;
                this.title = title; this.subtitle = subtitle; this.kind = kind;
                this.toggleVal = toggleVal; this.sliderPct = sliderPct; this.valueText = valueText;
            }
        }

        private static class ActionBtnVis {
            final int x, y, w, h;
            final String label;
            final int style; // 0=default, 1=accent, 2=danger, 3=spotify

            ActionBtnVis(int x, int y, int w, int h, String label, int style) {
                this.x = x; this.y = y; this.w = w; this.h = h; this.label = label; this.style = style;
            }
        }

        private static class ControlSpec {
            final String title, subtitle, valueLabel;
            final int kind;
            final boolean toggleState;
            final float sliderRatio;
            final Runnable onClick;
            final DoubleConsumer onSlide;

            ControlSpec(String title, String subtitle, int kind, boolean toggleState,
                        float sliderRatio, String valueLabel, Runnable onClick, DoubleConsumer onSlide) {
                this.title = title; this.subtitle = subtitle; this.kind = kind;
                this.toggleState = toggleState; this.sliderRatio = sliderRatio;
                this.valueLabel = valueLabel; this.onClick = onClick; this.onSlide = onSlide;
            }
        }

        private final List<NavItemVis> navItems = new ArrayList<>();
        private final List<RightControlVis> rightControls = new ArrayList<>();
        private final List<ControlSpec> currentSpecs = new ArrayList<>();
        private final List<ActionBtnVis> actionButtons = new ArrayList<>();
        private int draggingSpecIndex = -1;

        public boolean afkGameRunning = false;
        private int afkScore = 0, afkBestScore = 0, afkTick = 0;
        private int runnerLane = 1;
        private final List<int[]> runnerObs = new ArrayList<>();
        private double sonicY = 0.0, sonicVel = 0.0;
        private final List<int[]> sonicObs = new ArrayList<>();
        private int doomX = 50;
        private final List<int[]> doomShots = new ArrayList<>();
        private final List<int[]> doomMobs = new ArrayList<>();
        private final Random gameRng = new Random();

        private int winX, winY, winW, winH;
        private int leftColX, leftColW, centerColX, centerColW, rightColX, rightColW, colTopY, colH;

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

            // 1. Левая панель (9 вкладок + кнопка темы внизу)
            MenuTab[] tabs = MenuTab.values();
            int itemH = 24;
            int gap = 3;
            int startY = this.colTopY + 5;
            for (int i = 0; i < tabs.length; i++) {
                MenuTab t = tabs[i];
                int ix = this.leftColX + 4;
                int iy = startY + i * (itemH + gap);
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

            addStyledBtn(this.leftColX + 4, this.colTopY + this.colH - 22, this.leftColW - 8, 18,
                currentTheme.title, 1, () -> {
                    MenuTheme[] th = MenuTheme.values();
                    currentTheme = th[(currentTheme.ordinal() + 1) % th.length];
                    terrainDirtyForGpu = true;
                    this.rebuildWidgets();
                });

            addStyledBtn(this.winX + this.winW - 22, this.winY + 5, 17, 17, "✕", 2, this::onClose);

            initCenterStageButtons();

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
                        int sendW = 64, boxW = sw - sendW - 6;
                        EditBox box = new EditBox(this.font, sx, bottomY, boxW, 18, Component.literal("Чат"));
                        box.setMaxLength(120);
                        box.setValue(chatInputDraft);
                        box.setHint(Component.literal("выбей трезубец / !к дому / !перебор..."));
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
                            addStyledBtn(sx + (i % 3) * (cw + 5), bottomY - 22 + (i / 3) * 21, cw, 18,
                                f.russianName, currentForm == f ? 1 : 0, () -> {
                                    currentForm = f;
                                    this.rebuildWidgets();
                                });
                        }
                    } else {
                        CompanionAnchor[] anchors = CompanionAnchor.values();
                        int hw = (sw - 6) / 2;
                        for (int i = 0; i < anchors.length; i++) {
                            CompanionAnchor a = anchors[i];
                            addStyledBtn(sx + (i % 2) * (hw + 6), bottomY - 22 + (i / 2) * 21, hw, 18,
                                a.label, currentAnchor == a ? 1 : 0, () -> {
                                    currentAnchor = a;
                                    offsetX = a.defaultX; offsetY = a.defaultY; offsetZ = a.defaultZ;
                                    this.rebuildWidgets();
                                });
                        }
                    }
                }
                case AUTOPILOT -> {
                    // 4 прямые кнопки Автопилота: Задать Дом, Вести Домой, Вести к цели, СТОП!
                    int hw = (sw - 6) / 2;
                    addStyledBtn(sx, bottomY - 22, hw, 18, "📍 Задать Дом тут", 0, () -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            homeWaypoint = mc.player.blockPosition();
                            setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
                            showToast("Дом сохранён!", 60);
                        }
                        this.rebuildWidgets();
                    });
                    addStyledBtn(sx + hw + 6, bottomY - 22, hw, 18, "▶ Автопилот Домой", 3, () -> {
                        if (homeWaypoint == null) {
                            showToast("Сначала нажми «Задать Дом тут»!", 65);
                        } else {
                            setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
                            safeAutopilot = true;
                            showToast("▶ Автопилот ведёт домой! (S/Shift — стоп)", 80);
                            this.onClose();
                        }
                    });
                    addStyledBtn(sx, bottomY, hw, 18, "▶ Автопилот к Маяку", 1, () -> {
                        if (activeNavTarget == null && homeWaypoint == null) {
                            showToast("Нет активной цели! Найди деревню/дворец или задай дом.", 70);
                        } else {
                            if (activeNavTarget == null) setNavigationTarget(homeWaypoint, "Дом", 0xFF22D3EE);
                            safeAutopilot = true;
                            showToast("▶ Автопилот запущен к «" + activeNavTitle + "»!", 80);
                            this.onClose();
                        }
                    });
                    addStyledBtn(sx + hw + 6, bottomY, hw, 18, "■ СТОП Автопилот", 2, () -> {
                        safeAutopilot = false;
                        releaseAutopilotKeys(Minecraft.getInstance());
                        showToast("Автопилот выключен", 50);
                        this.rebuildWidgets();
                    });
                }
                case AUTO_BUILD -> {
                    BuildMode[] modes = BuildMode.values();
                    int hw = (sw - 6) / 2;
                    for (int i = 0; i < modes.length; i++) {
                        BuildMode bm = modes[i];
                        addStyledBtn(sx + (i % 2) * (hw + 6), bottomY - 22 + (i / 2) * 21, hw, 18,
                            bm.title, currentBuildMode == bm ? 1 : 0, () -> {
                                currentBuildMode = bm;
                                this.rebuildWidgets();
                            });
                    }
                }
                case VISUAL_RADAR -> {
                    StructureTarget[] quick = {
                        StructureTarget.VILLAGE,
                        StructureTarget.TRIAL_CHAMBER,
                        StructureTarget.STRONGHOLD,
                        StructureTarget.ANCIENT_CITY
                    };
                    int hw = (sw - 6) / 2;
                    for (int i = 0; i < quick.length; i++) {
                        StructureTarget st = quick[i];
                        addStyledBtn(sx + (i % 2) * (hw + 6), bottomY - 22 + (i / 2) * 21, hw, 18,
                            "Найти: " + st.russianName, 1, () -> {
                                locateStructureAsync(st);
                                this.rebuildWidgets();
                            });
                    }
                }
                case SPOTIFY_PLAYER -> {
                    int hw = (sw - 6) / 2;
                    addStyledBtn(sx, this.colTopY + 5, hw, 16, "♫ Spotify (ПК)", spotifyCenterTab == 0 ? 3 : 0, () -> {
                        spotifyCenterTab = 0; afkGameRunning = false; pollWindowsMediaAsync(true); this.rebuildWidgets();
                    });
                    addStyledBtn(sx + hw + 6, this.colTopY + 5, hw, 16, "🎮 AFK Игры", spotifyCenterTab == 1 ? 1 : 0, () -> {
                        spotifyCenterTab = 1; this.rebuildWidgets();
                    });
                    if (spotifyCenterTab == 0) {
                        int py = this.colTopY + 136, bw = (sw - 28) / 5, startX = sx + 6;
                        addStyledBtn(startX, py, bw, 20, "⏮", 0, () -> controlWindowsMediaAsync("PREV"));
                        addStyledBtn(startX + (bw + 4), py, bw, 20, spotifyIsPlaying ? "⏸" : "▶", 3, () -> controlWindowsMediaAsync("TOGGLE"));
                        addStyledBtn(startX + (bw + 4) * 2, py, bw, 20, "⏭", 0, () -> controlWindowsMediaAsync("NEXT"));
                        addStyledBtn(startX + (bw + 4) * 3, py, bw, 20, "🔉", 0, () -> controlWindowsMediaAsync("VOL_DOWN"));
                        addStyledBtn(startX + (bw + 4) * 4, py, bw, 20, "🔊", 0, () -> controlWindowsMediaAsync("VOL_UP"));
                        addStyledBtn(sx + 6, bottomY - 2, sw - 12, 18, "⟳ Обновить трек из Spotify (ПК)", 3, () -> {
                            pollWindowsMediaAsync(true);
                            showToast("Синхронизация со Spotify...", 45);
                        });
                    } else {
                        int bw = (sw - 6) / 2;
                        addStyledBtn(sx, bottomY, bw, 18, selectedAfkGame.title, 0, () -> {
                            AfkGameType[] gs = AfkGameType.values();
                            selectedAfkGame = gs[(selectedAfkGame.ordinal() + 1) % gs.length];
                            resetAfkGame(); this.rebuildWidgets();
                        });
                        addStyledBtn(sx + bw + 6, bottomY, bw, 18, afkGameRunning ? "■ Стоп" : "▶ Играть", 1, () -> {
                            afkGameRunning = !afkGameRunning;
                            if (afkGameRunning) resetAfkGame();
                            this.rebuildWidgets();
                        });
                    }
                }
                case CHESTS_VILLAGERS -> {
                    int row1Y = bottomY - 22;
                    int bw = (sw - 8) / 3;
                    addStyledBtn(sx, row1Y, bw, 18, "Найти Дворец", 1, () -> {
                        locateStructureAsync(StructureTarget.TRIAL_CHAMBER);
                        this.rebuildWidgets();
                    });
                    addStyledBtn(sx + bw + 4, row1Y, bw, 18,
                        autoCycleLectern ? "■ Стоп перебор" : "▶ Перебор жит.",
                        autoCycleLectern ? 2 : 3, () -> {
                            if (autoCycleLectern) {
                                stopLecternCycle("Остановлено из меню");
                                this.rebuildWidgets();
                            } else {
                                String msg = startLecternCycleManual();
                                showToast(msg, 85);
                                Minecraft mc = Minecraft.getInstance();
                                if (mc.player != null) mc.player.displayClientMessage(Component.literal("§b[✦ Вадимка] §f" + msg), false);
                                if (autoCycleLectern) this.onClose();
                                else this.rebuildWidgets();
                            }
                        });
                    addStyledBtn(sx + (bw + 4) * 2, row1Y, bw, 18, "Открыть рядом", 1, () -> {
                        String res = triggerManualOpenNearestContainer();
                        showToast(res, 75);
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) mc.player.displayClientMessage(Component.literal("§b[✦ Вадимка] §f" + res), false);
                    });

                    addStyledBtn(sx, bottomY, bw, 18, "Сброс памяти", 2, () -> {
                        rememberedChests.clear(); rememberedVillagers.clear(); clearNavigationTarget();
                        showToast("Память очищена", 50); this.rebuildWidgets();
                    });
                    addStyledBtn(sx + bw + 4, bottomY, bw, 18,
                        chestsCenterTab == 2 ? "3D Сцена" : ("Жители (" + rememberedVillagers.size() + ")"),
                        chestsCenterTab == 2 ? 1 : 0, () -> {
                            chestsCenterTab = (chestsCenterTab == 2) ? 0 : 2; this.rebuildWidgets();
                        });
                    addStyledBtn(sx + (bw + 4) * 2, bottomY, bw, 18,
                        chestsCenterTab == 1 ? "3D Сцена" : ("Ларчики (" + rememberedChests.size() + ")"),
                        chestsCenterTab == 1 ? 1 : 0, () -> {
                            chestsCenterTab = (chestsCenterTab == 1) ? 0 : 1; this.rebuildWidgets();
                        });
                }
                case CUSTOM_VISUAL -> {
                    int hw = (sw - 6) / 2;
                    addStyledBtn(sx, bottomY, hw, 18, "Случайный образ", 1, () -> {
                        wingsIndex = 1 + rng.nextInt(WINGS_OPTIONS.size() - 1);
                        hornsIndex = 1 + rng.nextInt(HORNS_OPTIONS.size() - 1);
                        hatsIndex = 1 + rng.nextInt(HATS_OPTIONS.size() - 1);
                        this.rebuildWidgets();
                    });
                    addStyledBtn(sx + hw + 6, bottomY, hw, 18, "Сбросить образ", 2, () -> {
                        wingsIndex = 0; hornsIndex = 0; hatsIndex = 0; outfitIndex = 4; crosshairIndex = 0;
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
                    list.add(toggleSpec("Тихий режим", "Меньше тостов", quietMode, () -> quietMode = !quietMode));
                    list.add(sliderSpec("Размер 3D", companionScalePercent + "%", (companionScalePercent - 60) / 80.0F, r -> companionScalePercent = 60 + (int) Math.round(r * 80.0)));
                }
                case AUTOPILOT -> {
                    list.add(toggleSpec("Автопилот", "Ведёт к цели сам", safeAutopilot, () -> safeAutopilot = !safeAutopilot));
                    list.add(toggleSpec("Авто-спринт", "Бег при ходьбе", sprintAssist, () -> sprintAssist = !sprintAssist));
                    list.add(toggleSpec("Обход лавы/обр.", "Стоп перед ямой", autopilotAvoidLavaAndCliffs, () -> autopilotAvoidLavaAndCliffs = !autopilotAvoidLavaAndCliffs));
                    list.add(toggleSpec("Возврат контр.", "Стоп по S/Shift", autoReturnControl, () -> autoReturnControl = !autoReturnControl));
                    list.add(toggleSpec("Страж края", "Алерт обрыва", fallDangerAlert, () -> fallDangerAlert = !fallDangerAlert));
                    list.add(toggleSpec("Паркур-помощь", "Прыжки у края", parkourEdgeGuard, () -> parkourEdgeGuard = !parkourEdgeGuard));
                }
                case AUTO_BUILD -> {
                    list.add(toggleSpec("Авто-постройка", "По схеме", autoBuildLitematica, () -> autoBuildLitematica = !autoBuildLitematica));
                    list.add(toggleSpec("Плавная камера", "Кривые Безье", smoothBezierCamera, () -> smoothBezierCamera = !smoothBezierCamera));
                    list.add(toggleSpec("Авто-хотбар", "Подмена стаков", hotbarAutoRefill, () -> hotbarAutoRefill = !hotbarAutoRefill));
                    list.add(toggleSpec("Редстоун-скан", "Сила сигнала", redstoneInspector, () -> redstoneInspector = !redstoneInspector));
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
                    list.add(toggleSpec("GPU Мини-карта", "128x128 75 FPS", minimapRadarEnabled, () -> minimapRadarEnabled = !minimapRadarEnabled));
                    list.add(toggleSpec("Круглая карта", "Стиль сонара", minimapCircularMode, () -> { minimapCircularMode = !minimapCircularMode; terrainDirtyForGpu = true; }));
                    list.add(toggleSpec("Контуры высот", "Топография", minimapContourLines, () -> { minimapContourLines = !minimapContourLines; terrainDirtyForGpu = true; }));
                    list.add(cycleSpec("Масштаб", "Обзор карты",
                        radarZoomLevel == 0 ? "1x (Детально)" : (radarZoomLevel == 1 ? "2x (Средне)" : "3x (Дальше)"), () -> {
                            radarZoomLevel = (radarZoomLevel + 1) % 3;
                            terrainNeedsReset = true;
                            terrainDirtyForGpu = true;
                        }));
                    list.add(toggleSpec("Звуковой сонар", "Враги по краю", soundRadarSonar, () -> soundRadarSonar = !soundRadarSonar));
                    list.add(toggleSpec("Элитра Горизонт", "Угол в полёте", elytraPitchHorizon, () -> elytraPitchHorizon = !elytraPitchHorizon));
                    list.add(toggleSpec("Нить Ариадны", "След пути", ariadneThread, () -> ariadneThread = !ariadneThread));
                    list.add(cycleSpec("Навигатор", activeNavTarget == null ? "Выключен" : activeNavTitle, "Сбросить", () -> {
                        clearNavigationTarget(); safeAutopilot = false; releaseAutopilotKeys(Minecraft.getInstance());
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
                    list.add(toggleSpec("Снайпер Хранил.", "Ждёт нужный лут", autoClickVaults, () -> autoClickVaults = !autoClickVaults));
                    list.add(toggleSpec("Только цель", "Не клик. наугад", vaultSmartWaitLoot, () -> vaultSmartWaitLoot = !vaultSmartWaitLoot));
                    list.add(cycleSpec("Лут Хранилища", "Цель снайпера", getActiveVaultTargetLabel(), () -> {
                        customVaultItemQuery = "";
                        vaultLootGoalIndex = (vaultLootGoalIndex + 1) % VAULT_LOOT_PRESETS.size();
                    }));
                    list.add(toggleSpec("Перебор кафедры", "Топор + торги", autoCycleLectern, () -> {
                        if (autoCycleLectern) stopLecternCycle("Выкл. из настроек");
                        else startLecternCycleManual();
                    }));
                    list.add(cycleSpec("Нужные чары", "Библиотекарь", TARGET_ENCHANTS.get(targetEnchantIndex), () -> targetEnchantIndex = (targetEnchantIndex + 1) % TARGET_ENCHANTS.size()));
                    list.add(sliderSpec("Макс. цена", maxEmeraldPrice + " изумр.", (maxEmeraldPrice - 5) / 59.0F, r -> maxEmeraldPrice = 5 + (int) Math.round(r * 59.0)));
                    list.add(toggleSpec("Страж топора", "Стоп при <= 10", warnAxeDurability, () -> warnAxeDurability = !warnAxeDurability));
                    list.add(toggleSpec("Подсветка", "Хранилищ/сунд.", highlightTrialVaults, () -> highlightTrialVaults = !highlightTrialVaults));
                }
                case CUSTOM_VISUAL -> {
                    list.add(cycleSpec("Тема меню", "5 палитр", currentTheme.title, () -> {
                        MenuTheme[] th = MenuTheme.values();
                        currentTheme = th[(currentTheme.ordinal() + 1) % th.length];
                        terrainDirtyForGpu = true;
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

        private ControlSpec sliderSpec(String t, String val, float ratio, DoubleConsumer slide) {
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
            double mx = event.x(), my = event.y();
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
                int trackX = rc.x + 8, trackW = rc.w - 72;
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

            fillRoundedRect(g, winX, winY, winW, winH, currentTheme.bgWindow);
            drawGradientNeonBorder(g, winX, winY, winW, winH, currentTheme.accent, currentTheme.secondary);
            g.fillGradient(winX + 2, winY + 2, winX + winW - 2, winY + 25, currentTheme.bgCard, currentTheme.bgPanel);
            g.fill(winX + 2, winY + 25, winX + winW - 2, winY + 26, currentTheme.borderMuted);

            drawConceptTabIcon(g, MenuTab.COMPANION, winX + 9, winY + 8, currentTheme.accent);
            String titleStr = "Assistant Vadimka";
            g.drawString(this.font, titleStr, winX + (winW - this.font.width(titleStr)) / 2, winY + 9, currentTheme.accent);
            g.drawString(this.font, VERSION_LABEL, winX + winW - 50, winY + 9, 0xFF94A3B8);

            // Левая навигация
            fillGlassCard(g, leftColX, colTopY, leftColW, colH, currentTheme.bgPanel, currentTheme.borderMuted);
            for (NavItemVis ni : navItems) {
                boolean active = ni.tab == this.activeTab;
                boolean hover = isOver(mouseX, mouseY, ni.x, ni.y, ni.w, ni.h);
                int fill = active ? lerpColor(currentTheme.bgCard, currentTheme.accent, 0.14F) : (hover ? currentTheme.bgCard : 0xFF0E141D);
                fillRoundedRect(g, ni.x, ni.y, ni.w, ni.h, fill);
                if (active) {
                    drawGradientNeonBorder(g, ni.x, ni.y, ni.w, ni.h, currentTheme.accent, currentTheme.secondary);
                    g.fill(ni.x + 2, ni.y + 4, ni.x + 4, ni.y + ni.h - 4, currentTheme.accent);
                } else {
                    drawRoundedBorder(g, ni.x, ni.y, ni.w, ni.h, hover ? currentTheme.accent : currentTheme.borderMuted);
                }
                drawConceptTabIcon(g, ni.tab, ni.x + 7, ni.y + 7, active ? currentTheme.accent : 0xFF94A3B8);
                g.drawString(this.font, ni.tab.navTitle, ni.x + 21, ni.y + 8, active ? 0xFFFFFFFF : 0xFFCBD5E1);
            }

            // Центральная сцена
            fillGlassCard(g, centerColX, colTopY, centerColW, colH, currentTheme.bgPanel, currentTheme.borderMuted);
            renderCenterStageByTab(g, mouseX, mouseY);

            // Правая панель настроек
            fillGlassCard(g, rightColX, colTopY, rightColW, colH, currentTheme.bgPanel, currentTheme.borderMuted);
            g.drawString(this.font, "Настройки", rightColX + 8, colTopY + 7, currentTheme.secondary);
            if (rightTotalPages > 1) {
                g.drawString(this.font, (rightPage + 1) + "/" + rightTotalPages, rightColX + rightColW - 39, colTopY + 7, 0xFF94A3B8);
            }

            for (RightControlVis rc : rightControls) {
                boolean hover = isOver(mouseX, mouseY, rc.x, rc.y, rc.w, rc.h);
                fillGlassCard(g, rc.x, rc.y, rc.w, rc.h,
                    hover ? lerpColor(currentTheme.bgCard, 0xFFFFFFFF, 0.04F) : currentTheme.bgCard,
                    hover ? currentTheme.accent : currentTheme.borderMuted);

                if (rc.kind == 0) {
                    int textW = rc.ctrlX - (rc.x + 7) - 4;
                    g.drawString(this.font, trimText(this.font, rc.title, textW), rc.x + 7, rc.y + 8, 0xFFF8FAFC);
                    if (!rc.subtitle.isEmpty()) g.drawString(this.font, trimText(this.font, rc.subtitle, textW), rc.x + 7, rc.y + 22, 0xFF94A3B8);
                    int trackCol = rc.toggleVal ? currentTheme.toggleOnTrack : 0xFF1A2332;
                    int borderCol = rc.toggleVal ? currentTheme.accent : 0xFF334155;
                    fillGlassCard(g, rc.ctrlX, rc.ctrlY, rc.ctrlW, rc.ctrlH, trackCol, borderCol);
                    int knob = rc.ctrlH - 4;
                    int kx = rc.toggleVal ? (rc.ctrlX + rc.ctrlW - knob - 2) : (rc.ctrlX + 2);
                    fillRoundedRect(g, kx, rc.ctrlY + 2, knob, knob, rc.toggleVal ? 0xFFFFFFFF : 0xFF94A3B8);
                } else if (rc.kind == 1) {
                    g.drawString(this.font, rc.title, rc.x + 7, rc.y + 6, 0xFFF8FAFC);
                    int badgeW = 54, badgeX = rc.x + rc.w - badgeW - 6;
                    fillGlassCard(g, badgeX, rc.ctrlY, badgeW, rc.ctrlH, 0xFF0A1018, currentTheme.borderMuted);
                    String vStr = trimText(this.font, rc.valueText, badgeW - 4);
                    g.drawString(this.font, vStr, badgeX + (badgeW - this.font.width(vStr)) / 2, rc.ctrlY + 4, currentTheme.accent);
                    int trackX = rc.x + 8, trackW = badgeX - trackX - 8, trackY = rc.ctrlY + 6;
                    g.fill(trackX, trackY, trackX + trackW, trackY + 4, 0xFF1E293B);
                    int fillW = (int) (trackW * rc.sliderPct);
                    g.fillGradient(trackX, trackY, trackX + fillW, trackY + 4, currentTheme.accent, currentTheme.secondary);
                    fillRoundedRect(g, trackX + fillW - 3, trackY - 3, 6, 10, 0xFFFFFFFF);
                } else {
                    int textW = rc.ctrlX - (rc.x + 7) - 4;
                    g.drawString(this.font, trimText(this.font, rc.title, textW), rc.x + 7, rc.y + 8, 0xFFF8FAFC);
                    if (!rc.subtitle.isEmpty()) g.drawString(this.font, trimText(this.font, rc.subtitle, textW), rc.x + 7, rc.y + 22, 0xFF94A3B8);
                    fillGlassCard(g, rc.ctrlX, rc.ctrlY, rc.ctrlW, rc.ctrlH, 0xFF0C131C, currentTheme.secondary);
                    String vStr = trimText(this.font, rc.valueText, rc.ctrlW - 6);
                    g.drawString(this.font, vStr, rc.ctrlX + (rc.ctrlW - this.font.width(vStr)) / 2, rc.ctrlY + 4, 0xFFFFFFFF);
                }
            }

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

        // --- Быстрые центральные сцены (< 50 draw calls = 75+ FPS даже в меню!) ---

        private void renderCenterStageByTab(GuiGraphics g, int mouseX, int mouseY) {
            int sx = this.centerColX + 7, sy = this.colTopY + 6, sw = this.centerColW - 14, sh = this.colH - 12;
            switch (this.activeTab) {
                case COMPANION -> renderCompanionCenterStage(g, sx, sy, sw, sh, mouseX, mouseY);
                case AUTOPILOT -> renderAutopilotCenterStage(g, sx, sy, sw, sh);
                case AUTO_BUILD -> renderAutoBuildCenterStage(g, sx, sy, sw, sh);
                case LITEMATICA_HUD -> renderLitematicaCenterStage(g, sx, sy, sw, sh);
                case COMBAT_PVP -> renderCombatCenterStage(g, sx, sy, sw, sh);
                case VISUAL_RADAR -> renderVisualRadarCenterStage(g, sx, sy, sw, sh);
                case SPOTIFY_PLAYER -> renderSpotifyCenterStage(g, sx, sy, sw, sh);
                case CHESTS_VILLAGERS -> renderChestsVillagersCenterStage(g, sx, sy, sw, sh, mouseX, mouseY);
                case CUSTOM_VISUAL -> renderCustomVisualCenterStage(g, sx, sy, sw, sh, mouseX, mouseY);
            }
        }

        private void drawFastStudioBackdrop(GuiGraphics g, int x, int y, int w, int h) {
            g.fillGradient(x, y, x + w, y + h, 0xFF080D14, lerpColor(0xFF0E1722, currentTheme.accent, 0.08F));
            drawRoundedBorder(g, x, y, w, h, currentTheme.borderMuted);
            int horizonY = y + (h * 52) / 100;
            int bottomY = y + h - 3;
            int gridCol = 0x1622D3EE;
            for (int i = 1; i <= 5; i++) {
                float t = (float) (i * i) / 25.0F;
                int ly = horizonY + (int) ((bottomY - horizonY) * t);
                g.fill(x + 4, ly, x + w - 4, ly + 1, gridCol);
            }
            for (int gx = x + 28; gx < x + w - 16; gx += 32) {
                g.fill(gx, horizonY, gx + 1, bottomY, 0x1022D3EE);
            }
        }

        private void drawFastLine(GuiGraphics g, int x0, int y0, int x1, int y1, int col, int minX, int maxX) {
            int dx = x1 - x0, dy = y1 - y0;
            int steps = Math.max(1, Math.max(Math.abs(dx), Math.abs(dy)) / 3);
            for (int i = 0; i <= steps; i++) {
                float t = (float) i / steps;
                int px = x0 + Math.round(dx * t);
                int py = y0 + Math.round(dy * t);
                if (px >= minX && px < maxX - 2) g.fill(px, py, px + 2, py + 2, col);
            }
        }

        private void renderCompanionCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh, int mouseX, int mouseY) {
            g.drawString(this.font, trimText(this.font, activeTab.headerTitle, sw - 82), sx, sy + 2, 0xFFF8FAFC);
            if (showChatOverlay) {
                int boxY = sy + 20, boxH = sh - 48;
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
                int start = Math.max(0, wrapped.size() - maxLines), ty = boxY + 5;
                for (int i = start; i < wrapped.size(); i++) {
                    g.drawString(this.font, wrapped.get(i)[1], sx + 7, ty, "1".equals(wrapped.get(i)[0]) ? currentTheme.accent : 0xFFE2E8F0);
                    ty += 12;
                }
                return;
            }
            Minecraft mc = Minecraft.getInstance();
            int stageY = sy + 20, stageH = sh - 68;
            drawFastStudioBackdrop(g, sx, stageY, sw, stageH);

            if (!positionEditor3D) {
                VadimkaForm[] forms = VadimkaForm.values();
                int slotW = sw / forms.length;
                for (int i = 0; i < forms.length; i++) {
                    VadimkaForm f = forms[i];
                    int fx = sx + i * slotW, fy = stageY + 24, fh = stageH - 44;
                    boolean sel = f == currentForm;
                    if (sel) {
                        g.fill(fx + 3, fy + 1, fx + slotW - 3, fy + fh - 1, 0x1A22D3EE);
                        drawGradientNeonBorder(g, fx + 2, fy, slotW - 4, fh, currentTheme.accent, currentTheme.secondary);
                    } else drawRoundedBorder(g, fx + 2, fy, slotW - 4, fh, 0x4494A3B8);
                    g.drawString(this.font, trimText(this.font, f.codeName, slotW - 4), fx + 4, stageY + 7, sel ? currentTheme.accent : 0xFFE2E8F0);
                    if (mc.player != null && mc.level != null) {
                        LivingEntity model = getCustomStudioModel(mc.level, mc.player, f);
                        try {
                            InventoryScreen.renderEntityInInventoryFollowsMouse(g, fx + 2, fy + 4, fx + slotW - 2, fy + fh - 4,
                                Math.max(12, (int) (f.guiScale * 0.72)), 0.0625F, (float) mouseX, (float) mouseY, model);
                        } catch (Throwable ignored) {
                        }
                    }
                }
                int costY = stageY + stageH - 18;
                g.drawString(this.font, "Авто-костюмы:", sx + 6, costY + 4, 0xFF94A3B8);
                ItemStack[] icons = {new ItemStack(Items.GOLDEN_HELMET), new ItemStack(Items.NETHERITE_HELMET), new ItemStack(Items.ELYTRA), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.NETHER_STAR)};
                int ix = sx + 88;
                for (ItemStack ic : icons) { g.renderItem(ic, ix, costY); ix += 22; }
            } else {
                int centerX = sx + sw / 2 - 10, centerY = stageY + stageH / 2 + 12;
                int petX = Mth.clamp(centerX + (int) (offsetX * 26.0), sx + 34, sx + sw - 34);
                int petY = Mth.clamp(centerY - (int) ((offsetY - 1.0) * 24.0), stageY + 34, stageY + stageH - 26);
                if (mc.player != null && mc.level != null) {
                    try {
                        InventoryScreen.renderEntityInInventoryFollowsMouse(g, centerX - 36, stageY + 26, centerX + 36, stageY + stageH - 8, 44, 0.0625F, (float) mouseX, (float) mouseY, mc.player);
                        LivingEntity pet = getCustomStudioModel(mc.level, mc.player, currentForm);
                        InventoryScreen.renderEntityInInventoryFollowsMouse(g, petX - 24, petY - 24, petX + 24, petY + 24, Math.max(14, (int) (currentForm.guiScale * 0.72)), 0.0625F, (float) mouseX, (float) mouseY, pet);
                    } catch (Throwable ignored) {
                    }
                }
                drawRoundedBorder(g, petX - 16, petY - 18, 32, 34, 0x9922D3EE);
                g.drawString(this.font, "Вадимка", petX - 19, Math.max(stageY + 5, petY - 29), 0xFFFDE047);
                g.fill(petX, Math.max(stageY + 14, petY - 30), petX + 2, petY - 2, 0xFF22C55E);
                g.drawString(this.font, "Y", petX + 4, Math.max(stageY + 14, petY - 30), 0xFF4ADE80);
                drawFastLine(g, petX, petY, petX - 24, petY + 12, 0xFFEF4444, sx, sx + sw);
                g.drawString(this.font, "X", petX - 32, petY + 9, 0xFFF87171);
                drawFastLine(g, petX, petY, petX + 24, petY + 12, 0xFF3B82F6, sx, sx + sw);
                g.drawString(this.font, "Z", petX + 27, petY + 9, 0xFF60A5FA);
            }
        }

        private void renderAutopilotCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Безопасный Автопилот и Профайлер стиля", sx, sy + 2, 0xFFF8FAFC);
            int cardY = sy + 18, cardH = sh - 66;
            fillGlassCard(g, sx, cardY, sw, cardH, 0xFF0A1018, safeAutopilot ? 0xFF4ADE80 : currentTheme.borderMuted);

            String homeStr = homeWaypoint == null
                ? "Дом: не задан (нажми «Задать Дом тут» или !дом)"
                : ("Дом: X=" + homeWaypoint.getX() + " Y=" + homeWaypoint.getY() + " Z=" + homeWaypoint.getZ());
            String navStr = activeNavTarget == null
                ? "Активный маяк: нет (используй !деревня / !испытания)"
                : ("Активный маяк: " + activeNavTitle + " [" + activeNavTarget.getX() + ", " + activeNavTarget.getZ() + "]");
            g.drawString(this.font, trimText(this.font, homeStr, sw - 16), sx + 8, cardY + 8, 0xFF4ADE80);
            g.drawString(this.font, trimText(this.font, navStr, sw - 16), sx + 8, cardY + 20, currentTheme.accent);
            g.drawString(this.font, "Статус Автопилота: " + (safeAutopilot ? "▶ ВЕДУ К ЦЕЛИ" : "■ ОЖИДАНИЕ"), sx + 8, cardY + 33, safeAutopilot ? 0xFF4ADE80 : 0xFF94A3B8);

            // Полоски профайлера стиля
            String[] labels = {"Обученность стиля:", "Моторика камеры:", "Ритм кликов:"};
            int dyn = Math.min(99, profilerTrainingPct + (int) (sessionWalkedBlocks / 150.0));
            int[] pcts = {dyn, profilerCameraPct, profilerClickRhythmPct};
            int y = cardY + 50, barW = sw - 20;
            for (int i = 0; i < labels.length; i++) {
                g.drawString(this.font, labels[i], sx + 10, y, 0xFFE2E8F0);
                String pt = pcts[i] + "%";
                g.drawString(this.font, pt, sx + sw - 10 - this.font.width(pt), y, currentTheme.accent);
                y += 10;
                g.fill(sx + 10, y, sx + 10 + barW, y + 5, 0xFF1E293B);
                g.fillGradient(sx + 10, y, sx + 10 + (barW * pcts[i]) / 100, y + 5, currentTheme.accent, currentTheme.secondary);
                y += 12;
            }

            // График кривой ускорения мыши
            int gX = sx + 10, gY = y + 2, gW = sw - 20, gH = cardY + cardH - gY - 8;
            if (gH > 22) {
                fillGlassCard(g, gX, gY, gW, gH, 0xFF070B10, 0x33FFFFFF);
                g.drawString(this.font, "Aim curve • Пройдено: " + (int) sessionWalkedBlocks + " бл.", gX + 6, gY + 4, 0xFF94A3B8);
                int prevX = gX + 4, prevY = gY + gH - 4;
                for (int step = 1; step <= 18; step++) {
                    float t = step / 18.0F;
                    float sCurve = t * t * (3.0F - 2.0F * t);
                    int nx = gX + 4 + (int) ((gW - 8) * t);
                    int ny = gY + gH - 4 - (int) ((gH - 16) * sCurve);
                    drawFastLine(g, prevX, prevY, nx, ny, lerpColor(currentTheme.secondary, currentTheme.accent, t), gX, gX + gW);
                    prevX = nx; prevY = ny;
                }
            }
        }

        private void renderAutoBuildCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "3D-голограмма • " + currentBuildMode.title, sx, sy + 2, 0xFFF8FAFC);
            int stageY = sy + 18, stageH = sh - 66;
            drawFastStudioBackdrop(g, sx, stageY, sw, stageH);

            int cx = sx + sw / 2, baseY = stageY + stageH - 22, w = 48, h = 42, neon = 0xFF38BDF8;
            g.fill(cx - w, baseY - h, cx + w, baseY, 0x2222D3EE);
            drawRoundedBorder(g, cx - w, baseY - h, w * 2, h, neon);
            g.fill(cx, baseY - h, cx + 1, baseY, 0x6638BDF8);
            for (int step = 0; step < 5; step++) {
                int rw = w + 6 - step * 10, ry = baseY - h - (step + 1) * 7;
                g.fill(cx - rw, ry, cx + rw, ry + 7, 0x2C38BDF8);
                drawRoundedBorder(g, cx - rw, ry, rw * 2, 7, neon);
            }
            drawRoundedBorder(g, cx - 32, baseY - 28, 16, 14, 0xFF7DD3FC);
            drawRoundedBorder(g, cx + 14, baseY - 26, 15, 26, 0xFFFBBF24);
            g.drawString(this.font, String.format("Задержка: %d–%d мс • Радиус: %.1f бл.", minBuildDelayMs, maxBuildDelayMs, buildReachBlocks), sx + 8, stageY + 7, currentTheme.accent);
        }

        private void renderLitematicaCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Трекер материалов Litematica HUD", sx, sy + 2, 0xFFF8FAFC);
            int cardY = sy + 20, cardH = sh - 40;
            fillGlassCard(g, sx, cardY, sw, cardH, 0xFF0A1017, currentTheme.accent);
            Player p = Minecraft.getInstance().player;
            ItemStack[] icons = {new ItemStack(Items.STONE_BRICKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.GLASS), new ItemStack(Items.QUARTZ_BLOCK)};
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
                g.renderItem(icons[i], sx + 14, rowY + 2);
                g.drawString(this.font, names[i], sx + 38, rowY + 1, 0xFFF8FAFC);
                int statusCol = done ? 0xFF4ADE80 : (have[i] > 0 ? 0xFFFACC15 : 0xFFF87171);
                g.drawString(this.font, have[i] + " / " + need[i], sx + 38, rowY + 12, statusCol);
                int barW = 74, barX = sx + sw - barW - 12;
                g.fill(barX, rowY + 8, barX + barW, rowY + 13, 0xFF1E293B);
                g.fill(barX, rowY + 8, barX + (int) (barW * Mth.clamp((float) have[i] / need[i], 0.0F, 1.0F)), rowY + 13, statusCol);
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
            int stageY = sy + 18, stageH = sh - 22;
            drawFastStudioBackdrop(g, sx, stageY, sw, stageH);
            int cx = sx + sw / 2, cy = stageY + stageH / 2;

            if (trajectories3D) {
                int startX = sx + sw - 30, startY = stageY + stageH - 18, endX = cx - 18, endY = cy - 16;
                int prevX = startX, prevY = startY;
                for (int i = 1; i <= 20; i++) {
                    float t = i / 20.0F;
                    int px = (int) Mth.lerp(t, startX, endX);
                    int py = (int) (Mth.lerp(t, startY, endY) - Math.sin(t * Math.PI) * 58.0);
                    drawFastLine(g, prevX, prevY, px, py, lerpColor(0xFFC084FC, 0xFF22D3EE, t), sx + 2, sx + sw - 2);
                    prevX = px; prevY = py;
                }
                drawRoundedBorder(g, endX - 6, endY - 6, 12, 12, 0xFF22D3EE);
            }
            drawRoundedBorder(g, cx - 48, cy - 12, 26, 26, 0xFF22D3EE);
            g.renderItem(new ItemStack(Items.ENDER_PEARL), cx - 43, cy - 7);
            g.drawString(this.font, "3.3", cx - 42, cy + 16, 0xFF67E8F9);
            drawRoundedBorder(g, cx + 22, cy - 12, 26, 26, 0xFFC084FC);
            g.renderItem(new ItemStack(Items.SHIELD), cx + 27, cy - 7);
            g.drawString(this.font, "2.1", cx + 28, cy + 16, 0xFFE879F9);
            drawRoundedBorder(g, cx - 13, cy + 18, 26, 26, 0xFF38BDF8);
            g.renderItem(new ItemStack(Items.ARROW), cx - 8, cy + 23);
            g.drawString(this.font, "0.3", cx - 7, cy + 46, 0xFFFFFFFF);

            ItemStack[] armors = {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS)};
            for (int i = 0; i < armors.length; i++) {
                int ay = stageY + 20 + i * 24;
                g.renderItem(armors[i], sx + 10, ay);
                g.drawString(this.font, "98%", sx + 30, ay + 4, 0xFF4ADE80);
            }
            g.renderItem(new ItemStack(Items.TOTEM_OF_UNDYING), sx + sw - 46, stageY + 20);
            g.drawString(this.font, "x" + (Minecraft.getInstance().player != null ? countPlayerTotems(Minecraft.getInstance().player) : 1), sx + sw - 26, stageY + 24, 0xFFFDE047);
        }

        private void renderVisualRadarCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            g.drawString(this.font, "Реалистичная GPU Карта, Сонар и Структуры", sx, sy + 2, 0xFFF8FAFC);
            int stageY = sy + 18, stageH = sh - 66;
            drawFastStudioBackdrop(g, sx, stageY, sw, stageH);

            int rCx = sx + sw / 3 - 4, rCy = stageY + stageH / 2 - 4;
            int[] radii = {44, 30, 16};
            int[] cols = {0xAA22D3EE, 0xAAC084FC, 0xAA38BDF8};
            for (int i = 0; i < radii.length; i++) drawRoundedBorder(g, rCx - radii[i], rCy - radii[i], radii[i] * 2, radii[i] * 2, cols[i]);
            g.fill(rCx - 4, rCy, rCx + 5, rCy + 1, 0xFFFFFFFF);
            g.fill(rCx, rCy - 4, rCx + 1, rCy + 5, 0xFFFFFFFF);
            g.fill(rCx - 2, rCy - 40, rCx + 3, rCy - 35, 0xFFEF4444);
            g.fill(rCx + 34, rCy - 14, rCx + 39, rCy - 9, 0xFFFACC15);

            int hCx = sx + (sw * 3) / 4, hCy = rCy - 12;
            g.drawString(this.font, "Elytra Horizon", hCx - 34, stageY + 8, 0xFFF8FAFC);
            for (int deg = -20; deg <= 20; deg += 10) {
                int ly = hCy - deg, lw = deg == 0 ? 30 : 16;
                g.fill(hCx - lw, ly, hCx + lw, ly + 1, deg == 0 ? 0xFF22D3EE : 0x6694A3B8);
                g.drawString(this.font, deg + "°", hCx - lw - 20, ly - 3, 0xFF94A3B8);
            }
            g.drawString(this.font, gpuMapReady ? "GPU: 128x128 OK" : "Режим: RLE Fast", hCx - 38, hCy + 30, 0xFF4ADE80);

            int stY = stageY + stageH - 22;
            fillGlassCard(g, sx + 6, stY, sw - 12, 18, 0xDD0B131E, activeNavTarget != null ? activeNavColor : currentTheme.borderMuted);
            g.drawString(this.font, trimText(this.font, structureSearchStatus, sw - 22), sx + 12, stY + 5, 0xFFE2E8F0);
        }

        private void renderSpotifyCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
            if (spotifyCenterTab == 1) {
                if (afkGameRunning) renderAfkMiniGameStage(g, sx, sy + 24, sw, sh - 52);
                else {
                    fillGlassCard(g, sx, sy + 26, sw, sh - 56, 0xFF0A1018, currentTheme.accent);
                    String t1 = "Игра: " + selectedAfkGame.title, t2 = selectedAfkGame.hint;
                    g.drawString(this.font, t1, sx + (sw - this.font.width(t1)) / 2, sy + 72, currentTheme.accent);
                    g.drawString(this.font, t2, sx + (sw - this.font.width(t2)) / 2, sy + 90, 0xFFE2E8F0);
                }
                return;
            }
            int cardY = sy + 24, cardH = sh - 52;
            g.fillGradient(sx, cardY, sx + sw, cardY + cardH, 0xFF0D2818, 0xFF0B1310);
            drawRoundedBorder(g, sx, cardY, sw, cardH, spotifyIsPlaying ? 0xFF1DB954 : 0xFF334155);
            drawConceptTabIcon(g, MenuTab.SPOTIFY_PLAYER, sx + 10, cardY + 10, 0xFF1DB954);
            String hdr = spotifyConnected ? (spotifyIsPlaying ? ("● ИГРАЕТ • " + spotifySourceName) : ("⏸ ПАУЗА • " + spotifySourceName)) : "○ Ожидание Spotify на ПК...";
            g.drawString(this.font, trimText(this.font, hdr, sw - 34), sx + 26, cardY + 11, spotifyIsPlaying ? 0xFF4ADE80 : 0xFF94A3B8);

            int artX = sx + 12, artY = cardY + 30, artSize = 72;
            g.fillGradient(artX, artY, artX + artSize, artY + artSize, 0xFF1E3A8A, 0xFF064E3B);
            drawRoundedBorder(g, artX, artY, artSize, artSize, 0x664ADE80);
            for (int tree = 0; tree < 4; tree++) {
                int tx = artX + 8 + tree * 16, th = 18 + (tree % 3) * 7;
                g.fill(tx, artY + artSize - th, tx + 8, artY + artSize - 2, 0xFF022C22);
            }

            int infoX = artX + artSize + 12, infoW = sw - artSize - 32;
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

            int eqY = cardY + cardH - 36;
            long t = System.currentTimeMillis() / 110L;
            for (int i = 0; i < 20; i++) {
                int bh = spotifyIsPlaying ? (3 + (int) (Math.abs(Math.sin(t * 0.7 + i * 0.5)) * 18)) : 2;
                int bx = sx + 12 + i * ((sw - 24) / 20);
                g.fill(bx, eqY + 20 - bh, bx + 4, eqY + 20, spotifyIsPlaying ? 0x554ADE80 : 0x33475569);
            }
            String tip = spotifyConnected ? "WinRT Sync активен • Кнопки управляют Spotify на ПК" : "Включи песню в Spotify на ПК — трек подхватится сам!";
            g.drawString(this.font, trimText(this.font, tip, sw - 20), sx + 10, cardY + cardH - 12, 0xFF94A3B8);
        }

        private void renderChestsVillagersCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh, int mouseX, int mouseY) {
            g.drawString(this.font, "Снайпер Хранилищ и Перебор Библиотекаря", sx, sy + 2, 0xFFF8FAFC);
            if (chestsCenterTab == 1) {
                int boxY = sy + 18, boxH = sh - 66;
                fillGlassCard(g, sx, boxY, sw, boxH, 0xFF0A1018, currentTheme.accent);
                int y = boxY + 8;
                if (rememberedChests.isEmpty()) {
                    g.drawString(this.font, "Хранилищ рядом: " + countNearbyVaultBlocks() + " • Цель: " + getActiveVaultTargetLabel(), sx + 8, y, 0xFF4ADE80);
                    g.drawString(this.font, "Напиши в чат: «Вадимка выбей из хранилища трезубец»", sx + 8, y + 16, 0xFFE2E8F0);
                    g.drawString(this.font, "Он будет смотреть внутрь Хранилища и НЕ станет кликать,", sx + 8, y + 30, 0xFFE2E8F0);
                    g.drawString(this.font, "пока внутри не закрутится нужный предмет!", sx + 8, y + 44, 0xFFE2E8F0);
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
                int boxY = sy + 18, boxH = sh - 66;
                fillGlassCard(g, sx, boxY, sw, boxH, 0xFF0A1018, currentTheme.secondary);
                int y = boxY + 8;
                if (rememberedVillagers.isEmpty()) {
                    g.drawString(this.font, "Как запустить Авто-перебор кафедры:", sx + 8, y, currentTheme.secondary);
                    g.drawString(this.font, "1. Положи Топор в хотбар, встань рядом с Кафедрой и Жителем", sx + 8, y + 16, 0xFFE2E8F0);
                    g.drawString(this.font, "2. Нажми «▶ Перебор жит.» или напиши «!перебор починка»", sx + 8, y + 30, 0xFFE2E8F0);
                    g.drawString(this.font, "3. Мод сам ломает и ставит кафедру до нужных чар!", sx + 8, y + 44, 0xFF4ADE80);
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

            int stageY = sy + 18, stageH = sh - 66;
            drawFastStudioBackdrop(g, sx, stageY, sw, stageH);
            Minecraft mc = Minecraft.getInstance();

            // Слева: Хранилище + иконка цели Снайпера
            int vx = sx + sw / 4 - 24, vy = stageY + stageH / 2 - 14;
            fillGlassCard(g, vx, vy, 48, 44, 0x33102030, 0xFF22D3EE);
            drawGradientNeonBorder(g, vx, vy, 48, 44, 0xFF22D3EE, 0xFFF472B6);
            g.renderItem(new ItemStack(Items.VAULT), vx + 16, vy + 14);
            VaultLootPreset preset = VAULT_LOOT_PRESETS.get(vaultLootGoalIndex % VAULT_LOOT_PRESETS.size());
            g.renderItem(new ItemStack(preset.previewIcon), vx - 20, vy + 2);
            g.renderItem(new ItemStack(Items.HEAVY_CORE), vx - 20, vy + 24);
            g.renderItem(new ItemStack(Items.TRIAL_KEY), vx + 52, vy + 2);
            g.renderItem(new ItemStack(Items.OMINOUS_TRIAL_KEY), vx + 52, vy + 24);
            g.drawString(this.font, "Цель: " + trimText(this.font, getActiveVaultTargetLabel(), 74), sx + 6, stageY + 8, 0xFFFACC15);

            // Справа: Живой 3D-Житель + Кафедра + статус Авто-перебора
            int vilCenterX = sx + (sw * 3) / 4 + 6;
            fillGlassCard(g, vilCenterX - 56, stageY + 8, 112, 26, 0xEE0B1624, 0xFF22D3EE);
            g.drawString(this.font, "Чары: " + TARGET_ENCHANTS.get(targetEnchantIndex), vilCenterX - 50, stageY + 12, 0xFF4ADE80);
            g.drawString(this.font, "До " + maxEmeraldPrice + " изумрудов", vilCenterX - 50, stageY + 23, 0xFFCBD5E1);
            if (mc.level != null) {
                Mob vil = getGuiVillagerModel(mc.level);
                if (vil != null) {
                    try {
                        InventoryScreen.renderEntityInInventoryFollowsMouse(g, vilCenterX - 24, stageY + 40, vilCenterX + 40, stageY + stageH - 8, 36, 0.0625F, (float) mouseX, (float) mouseY, vil);
                    } catch (Throwable ignored) {
                    }
                }
            }
            g.renderItem(new ItemStack(Items.LECTERN), vilCenterX - 38, stageY + stageH - 36);
            g.renderItem(new ItemStack(Items.ENCHANTED_BOOK), vilCenterX - 38, stageY + stageH - 54);

            // Нижняя строка статуса Снайпера / Перебора
            String live = autoCycleLectern ? ("⚙ " + lecternStatusMessage) : ("✦ " + lastVaultSpinningLabel);
            g.drawString(this.font, trimText(this.font, live, sw - 14), sx + 7, stageY + stageH - 13, 0xFF4ADE80);
        }

        private void renderCustomVisualCenterStage(GuiGraphics g, int sx, int sy, int sw, int sh, int mouseX, int mouseY) {
            g.drawString(this.font, "3D-Гардероб (Крылья, Рога, Шляпки, Темы)", sx, sy + 2, 0xFFF8FAFC);
            int stageY = sy + 18, stageH = sh - 46;
            drawFastStudioBackdrop(g, sx, stageY, sw, stageH);

            Minecraft mc = Minecraft.getInstance();
            int cx = sx + sw / 2;
            if (mc.player != null) {
                try {
                    InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx - 42, stageY + 22, cx + 42, stageY + stageH - 12, 48, 0.0625F, (float) mouseX, (float) mouseY, mc.player);
                } catch (Throwable ignored) {
                }
            }
            g.drawString(this.font, "Крылья: " + WINGS_OPTIONS.get(wingsIndex), sx + 10, stageY + 12, currentTheme.accent);
            g.drawString(this.font, "Рога/Нимб: " + HORNS_OPTIONS.get(hornsIndex), sx + 10, stageY + 26, currentTheme.secondary);
            g.drawString(this.font, "Шляпка: " + HATS_OPTIONS.get(hatsIndex), sx + 10, stageY + 40, 0xFFE2E8F0);
            g.drawString(this.font, "Прицел: " + CROSSHAIR_OPTIONS.get(crosshairIndex), sx + 10, stageY + 54, 0xFF4ADE80);
            g.drawString(this.font, "Эффекты F5: " + (worldCosmeticsF5 ? "ВКЛ" : "ВЫКЛ"), sx + 10, stageY + stageH - 14, 0xFF94A3B8);
        }

        private void renderAfkMiniGameStage(GuiGraphics g, int gx, int gy, int gw, int gh) {
            fillGlassCard(g, gx, gy, gw, gh, 0xFF070C12, currentTheme.accent);
            g.drawString(this.font, trimText(this.font, selectedAfkGame.title + " | Счёт: " + afkScore + " Рекорд: " + afkBestScore, gw - 12), gx + 6, gy + 5, 0xFFFDE047);
            int ax = gx + 6, ay = gy + 18, aw = gw - 12, ah = gh - 24;
            g.fill(ax, ay, ax + aw, ay + ah, 0xFF0D1520);
            switch (selectedAfkGame) {
                case RUNNER -> {
                    int laneW = aw / 3;
                    for (int l = 0; l < 3; l++) g.fill(ax + l * laneW + 2, ay + 2, ax + (l + 1) * laneW - 2, ay + ah - 2, 0x18FFFFFF);
                    for (int[] o : runnerObs) {
                        int ix = ax + o[0] * laneW + laneW / 2 - 6, iy = ay + (o[1] * (ah - 14)) / 100;
                        g.fill(ix, iy, ix + 12, iy + 12, o[2] == 1 ? 0xFF10B981 : 0xFF22C55E);
                    }
                    int plX = ax + runnerLane * laneW + laneW / 2 - 6;
                    g.fill(plX, ay + ah - 18, plX + 12, ay + ah - 6, currentTheme.accent);
                }
                case SONIC -> {
                    int groundY = ay + ah - 14;
                    g.fill(ax, groundY, ax + aw, ay + ah, 0xFF166534);
                    int plX = ax + (18 * aw) / 100, plY = groundY - 12 - (int) (sonicY * 1.4);
                    g.fill(plX, plY, plX + 12, plY + 12, 0xFF38BDF8);
                    for (int[] o : sonicObs) {
                        int ox = ax + (o[0] * aw) / 100;
                        if (o[1] == 1) g.fill(ox, groundY - 28, ox + 8, groundY - 20, 0xFFFACC15);
                        else g.fill(ox, groundY - 14, ox + 8, groundY, 0xFFEF4444);
                    }
                }
                case DOOM -> {
                    int plX = ax + (doomX * aw) / 100 - 6, plY = ay + ah - 16;
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
                } else cur = new StringBuilder(test);
            }
            if (!cur.isEmpty()) lines.add(cur.toString());
            return lines.toArray(new String[0]);
        }
    }

    // =========================================================================
    // 16. ОПЦИОНАЛЬНЫЙ СИНТЕЗАТОР (ВЫКЛ ПО УМОЛЧАНИЮ, НЕ МЕШАЕТ SPOTIFY)
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
    // 17. ЭНЦИКЛОПЕДИЯ MINECRAFT 1.21.11, СЛОВАРИ И СИСТЕМА ПРОФИЛЕЙ (v8.0)
    // =========================================================================

    private static final Map<String, String> MINECRAFT_KB = new LinkedHashMap<>();
    static {
        MINECRAFT_KB.put("алмаз", "Алмазы лучше всего копать на высоте Y = -58 или Y = -59 (слой глубинного сланца).");
        MINECRAFT_KB.put("незерит", "Древние обломки (Незерит) чаще всего спавнятся в Незере на высоте Y = 15.");
        MINECRAFT_KB.put("желез", "Железо лучше всего искать на Y = 16 в пещерах или высоко в горах на Y = 232.");
        MINECRAFT_KB.put("золот", "Золото лучше всего копать в биоме Бесплодных земель (Y = 32..256) или в обычном мире на Y = -16.");
        MINECRAFT_KB.put("лазурит", "Лазурит лучше всего спавнится на высоте Y = 0.");
        MINECRAFT_KB.put("редстоун", "Редстоун ищи на высоте Y = -59 вместе с алмазами.");
        MINECRAFT_KB.put("медь", "Медь лучше всего копать на Y = 48 (особенно в карстовых пещерах) или фармить во Дворце испытаний.");
        MINECRAFT_KB.put("угол", "Уголь чаще всего попадается на Y = 96 в горах или на Y = 45 в пещерах.");
        MINECRAFT_KB.put("изумруд", "Изумруды спавнятся в горах (Y = 236), но быстрее фармить торговлей с жителями.");
        MINECRAFT_KB.put("булав", "Для Булавы (Mace) нужны Тяжёлое ядро из Зловещего хранилища и Стержень вихря из Бриза.");
        MINECRAFT_KB.put("ядро", "Тяжёлое ядро выпадает из Зловещего хранилища (Ominous Vault) с шансом 7.5% — используй «Вадимка выбей ядро»!");
        MINECRAFT_KB.put("трезуб", "Трезубец выпадает из Утопленников или из Хранилища Дворца испытаний — напиши «Вадимка выбей из хранилища трезубец»!");
        MINECRAFT_KB.put("зловещ", "Выпей Зловещую бутылку во Дворце испытаний, чтобы активировать Зловещие спавнеры и получить Зловещий ключ!");
        MINECRAFT_KB.put("починк", "Починка I (Mending) — ставь и ломай кафедру у библиотекаря командой «!перебор починка»; минимальная цена 10 изумрудов.");
        MINECRAFT_KB.put("неразруш", "Неразрушимость III (Unbreaking III) — до 38 изумрудов, но при переборе кафедры можно поймать за 12-16!");
        MINECRAFT_KB.put("эффектив", "Эффективность V (Efficiency V) — используй «!перебор эффективность» у кафедры библиотекаря.");
        MINECRAFT_KB.put("острот", "Острота V (Sharpness V) — максимальный урон мечу и топору, минимальная цена у жителя — 17 изумрудов.");
        MINECRAFT_KB.put("защит", "Защита IV (Protection IV) — лучшая универсальная броня; минимальная цена у библиотекаря — 14 изумрудов.");
        MINECRAFT_KB.put("удач", "Удача III (Fortune III) — даёт до 4 алмазов с одной руды; минимальная цена у жителя — 11 изумрудов.");
        MINECRAFT_KB.put("добыч", "Добыча III (Looting III) — увеличивает шанс выбить черепа визера, жемчуг и трезубец.");
        MINECRAFT_KB.put("шёлк", "Шёлковое касание (Silk Touch) — позволяет забирать руду, стекло и лёд целиком.");
        MINECRAFT_KB.put("тотем", "Тотемы бессмертия падают с Вызывателей в Лесном особняке (!особняк) и на рейдах деревни.");
        MINECRAFT_KB.put("элитр", "Элитры находятся на летающих кораблях в городах Энда — сначала найди Крепость командой «!крепость».");
        MINECRAFT_KB.put("маяк", "Для Маяка (Beacon) нужна Звезда Незера с Визера, 3 обсидиана и 5 стёкол.");
        MINECRAFT_KB.put("дракон", "На Дракона Энда бери лук на Бесконечность, ведро воды, кровати и 2+ тотема бессмертия.");
        MINECRAFT_KB.put("визер", "Визера проще всего убивать в узком тоннеле на Y = -55 или под бедроком крыши Незера.");
        MINECRAFT_KB.put("варден", "Варден слепой и реагирует на звук и вибрацию — крадись на Shift по шерсти, найди город через «!город».");
        MINECRAFT_KB.put("бриз", "Бриз (Вихрь) отражает стрелы! Бей его в ближнем бою мечом или булавой, чтобы выбить Стержни вихря.");
        MINECRAFT_KB.put("шалкер", "Панцири шалкеров добываются в городах Энда; 2 панциря + сундук = Ящик шалкера.");
        MINECRAFT_KB.put("координат", "Включи GPU Мини-карту во вкладке «Радар» — она показывает твои X/Y/Z и маяки структур без падения FPS!");
    }

    private static String lookupMinecraftAdvice(String lowerQuery) {
        for (Map.Entry<String, String> entry : MINECRAFT_KB.entrySet()) {
            if (lowerQuery.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static class EnchantReference {
        public final String russianName;
        public final String englishId;
        public final int maxLevel;
        public final int minEmeraldCost;
        public final int goodEmeraldCost;

        public EnchantReference(String russianName, String englishId, int maxLevel, int minEmeraldCost, int goodEmeraldCost) {
            this.russianName = russianName;
            this.englishId = englishId;
            this.maxLevel = maxLevel;
            this.minEmeraldCost = minEmeraldCost;
            this.goodEmeraldCost = goodEmeraldCost;
        }
    }

    public static final List<EnchantReference> ENCHANT_CATALOG = List.of(
        new EnchantReference("Починка", "mending", 1, 10, 14),
        new EnchantReference("Неразрушимость III", "unbreaking", 3, 11, 18),
        new EnchantReference("Эффективность V", "efficiency", 5, 17, 26),
        new EnchantReference("Острота V", "sharpness", 5, 17, 26),
        new EnchantReference("Защита IV", "protection", 4, 14, 22),
        new EnchantReference("Удача III", "fortune", 3, 11, 18),
        new EnchantReference("Добыча III", "looting", 3, 11, 18),
        new EnchantReference("Шёлковое касание", "silk_touch", 1, 5, 12),
        new EnchantReference("Подводная ходьба III", "depth_strider", 3, 11, 18),
        new EnchantReference("Невесомость IV", "feather_falling", 4, 14, 20),
        new EnchantReference("Бесконечность", "infinity", 1, 5, 12),
        new EnchantReference("Сила V", "power", 5, 17, 26),
        new EnchantReference("Плотность V (Булава)", "density", 5, 17, 28),
        new EnchantReference("Пробивание IV (Булава)", "breach", 4, 14, 24),
        new EnchantReference("Порыв ветра III", "wind_burst", 3, 11, 20)
    );

    public static class TelemetryReport {
        public final String presetId;
        public final int activeModules;
        public final int nearbyVaultCount;
        public final int rememberedChestCount;
        public final int rememberedVillagerCount;
        public final boolean autopilotActive;
        public final boolean gpuMapActive;
        public final boolean spotifyLinked;
        public final String companionForm;
        public final String activeGoal;

        public TelemetryReport(String presetId, int activeModules, int nearbyVaultCount,
                               int rememberedChestCount, int rememberedVillagerCount,
                               boolean autopilotActive, boolean gpuMapActive, boolean spotifyLinked,
                               String companionForm, String activeGoal) {
            this.presetId = presetId;
            this.activeModules = activeModules;
            this.nearbyVaultCount = nearbyVaultCount;
            this.rememberedChestCount = rememberedChestCount;
            this.rememberedVillagerCount = rememberedVillagerCount;
            this.autopilotActive = autopilotActive;
            this.gpuMapActive = gpuMapActive;
            this.spotifyLinked = spotifyLinked;
            this.companionForm = companionForm;
            this.activeGoal = activeGoal;
        }

        public String formatCompact() {
            return presetId + "[mod=" + activeModules
                + ",vaults=" + nearbyVaultCount
                + ",chests=" + rememberedChestCount
                + ",vil=" + rememberedVillagerCount
                + ",auto=" + autopilotActive
                + ",gpu=" + gpuMapActive
                + ",sp=" + spotifyLinked
                + ",form=" + companionForm
                + ",goal=" + activeGoal + "]";
        }
    }


    /**
     * Диагностический снимок конфигурации и состояния модулей #001 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_001() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-001",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #002 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_002() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-002",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #003 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_003() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-003",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #004 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_004() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-004",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #005 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_005() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-005",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #006 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_006() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-006",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #007 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_007() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-007",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #008 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_008() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-008",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #009 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_009() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-009",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #010 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_010() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-010",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #011 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_011() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-011",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #012 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_012() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-012",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #013 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_013() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-013",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #014 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_014() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-014",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #015 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_015() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-015",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #016 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_016() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-016",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #017 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_017() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-017",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #018 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_018() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-018",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #019 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_019() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-019",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #020 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_020() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-020",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #021 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_021() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-021",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #022 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_022() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-022",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #023 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_023() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-023",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #024 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_024() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-024",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #025 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_025() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-025",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #026 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_026() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-026",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #027 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_027() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-027",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #028 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_028() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-028",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #029 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_029() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-029",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #030 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_030() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-030",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #031 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_031() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-031",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #032 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_032() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-032",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #033 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_033() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-033",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #034 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_034() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-034",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #035 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_035() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-035",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #036 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_036() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-036",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #037 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_037() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-037",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #038 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_038() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-038",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #039 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_039() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-039",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #040 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_040() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-040",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #041 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_041() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-041",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #042 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_042() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-042",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #043 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_043() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-043",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #044 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_044() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-044",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #045 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_045() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-045",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #046 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_046() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-046",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #047 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_047() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-047",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #048 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_048() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-048",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #049 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_049() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-049",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #050 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_050() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-050",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #051 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_051() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-051",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #052 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_052() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-052",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #053 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_053() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-053",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #054 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_054() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-054",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #055 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_055() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-055",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #056 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_056() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-056",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #057 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_057() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-057",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #058 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_058() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-058",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #059 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_059() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-059",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #060 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_060() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-060",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #061 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_061() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-061",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #062 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_062() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-062",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #063 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_063() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-063",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #064 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_064() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-064",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #065 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_065() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-065",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #066 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_066() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-066",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #067 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_067() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-067",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #068 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_068() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-068",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #069 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_069() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-069",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #070 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_070() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-070",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #071 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_071() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-071",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #072 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_072() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-072",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #073 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_073() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-073",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #074 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_074() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-074",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #075 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_075() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-075",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #076 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_076() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-076",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #077 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_077() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-077",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #078 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_078() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-078",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #079 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_079() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-079",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #080 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_080() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-080",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #081 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_081() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-081",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #082 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_082() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-082",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #083 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_083() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-083",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #084 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_084() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-084",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #085 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_085() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-085",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #086 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_086() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-086",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #087 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_087() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-087",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #088 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_088() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-088",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #089 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_089() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-089",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #090 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_090() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-090",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #091 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_091() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-091",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #092 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_092() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-092",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #093 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_093() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-093",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #094 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_094() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-094",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #095 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_095() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-095",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #096 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_096() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-096",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #097 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_097() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-097",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #098 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_098() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-098",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #099 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_099() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-099",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #100 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_100() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-100",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #101 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_101() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-101",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #102 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_102() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-102",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #103 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_103() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-103",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #104 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_104() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-104",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #105 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_105() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-105",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #106 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_106() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-106",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #107 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_107() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-107",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #108 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_108() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-108",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #109 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_109() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-109",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #110 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_110() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-110",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #111 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_111() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-111",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #112 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_112() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-112",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #113 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_113() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-113",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #114 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_114() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-114",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #115 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_115() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-115",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #116 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_116() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-116",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #117 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_117() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-117",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #118 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_118() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-118",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #119 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_119() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-119",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #120 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_120() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-120",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #121 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_121() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-121",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #122 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_122() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-122",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #123 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_123() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-123",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #124 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_124() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-124",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #125 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_125() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-125",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #126 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_126() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-126",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #127 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_127() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-127",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #128 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_128() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-128",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #129 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_129() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-129",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #130 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_130() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-130",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #131 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_131() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-131",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #132 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_132() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-132",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #133 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_133() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-133",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #134 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_134() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-134",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #135 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_135() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-135",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #136 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_136() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-136",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #137 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_137() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-137",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #138 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_138() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-138",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #139 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_139() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-139",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #140 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_140() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-140",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #141 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_141() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-141",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #142 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_142() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-142",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #143 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_143() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-143",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #144 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_144() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-144",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #145 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_145() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-145",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #146 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_146() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-146",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #147 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_147() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-147",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #148 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_148() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-148",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #149 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_149() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-149",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #150 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_150() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-150",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #151 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_151() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-151",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #152 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_152() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-152",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #153 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_153() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-153",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #154 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_154() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-154",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #155 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_155() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-155",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #156 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_156() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-156",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #157 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_157() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-157",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #158 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_158() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-158",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #159 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_159() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-159",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #160 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_160() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-160",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #161 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_161() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-161",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #162 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_162() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-162",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #163 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_163() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-163",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #164 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_164() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-164",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #165 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_165() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-165",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #166 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_166() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-166",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #167 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_167() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-167",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #168 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_168() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-168",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #169 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_169() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-169",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #170 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_170() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-170",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #171 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_171() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-171",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #172 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_172() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-172",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #173 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_173() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-173",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #174 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_174() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-174",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #175 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_175() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-175",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #176 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_176() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-176",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #177 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_177() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-177",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #178 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_178() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-178",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #179 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_179() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-179",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #180 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_180() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-180",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #181 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_181() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-181",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #182 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_182() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-182",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #183 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_183() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-183",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #184 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_184() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-184",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #185 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_185() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-185",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #186 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_186() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-186",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #187 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_187() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-187",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #188 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_188() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-188",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #189 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_189() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-189",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #190 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_190() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-190",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #191 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_191() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-191",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #192 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_192() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-192",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #193 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_193() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-193",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #194 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_194() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-194",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #195 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_195() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-195",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #196 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_196() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-196",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #197 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_197() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-197",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #198 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_198() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-198",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #199 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_199() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-199",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #200 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_200() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-200",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #201 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_201() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-201",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #202 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_202() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-202",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #203 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_203() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-203",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #204 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_204() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-204",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #205 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_205() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-205",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #206 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_206() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-206",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #207 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_207() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-207",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #208 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_208() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-208",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #209 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_209() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-209",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #210 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_210() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-210",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #211 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_211() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-211",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #212 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_212() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-212",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #213 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_213() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-213",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #214 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_214() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-214",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #215 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_215() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-215",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #216 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_216() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-216",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #217 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_217() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-217",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #218 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_218() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-218",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #219 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_219() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-219",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #220 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_220() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-220",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #221 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_221() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-221",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #222 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_222() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-222",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #223 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_223() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-223",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #224 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_224() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-224",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #225 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_225() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-225",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #226 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_226() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-226",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #227 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_227() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-227",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #228 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_228() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-228",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #229 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_229() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-229",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #230 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_230() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-230",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #231 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_231() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-231",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #232 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_232() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-232",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #233 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_233() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-233",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #234 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_234() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-234",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #235 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_235() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-235",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #236 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_236() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-236",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #237 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_237() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-237",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #238 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_238() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-238",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #239 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_239() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-239",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #240 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_240() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-240",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #241 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_241() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-241",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #242 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_242() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-242",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #243 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_243() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-243",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #244 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_244() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-244",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #245 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_245() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-245",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #246 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_246() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-246",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #247 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_247() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-247",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #248 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_248() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-248",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #249 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_249() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-249",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #250 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_250() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-250",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #251 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_251() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-251",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #252 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_252() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-252",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #253 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_253() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-253",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #254 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_254() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-254",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #255 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_255() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-255",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #256 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_256() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-256",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #257 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_257() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-257",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #258 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_258() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-258",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #259 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_259() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-259",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #260 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_260() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-260",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #261 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_261() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-261",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #262 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_262() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-262",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #263 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_263() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-263",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #264 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_264() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-264",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #265 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_265() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-265",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #266 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_266() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-266",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #267 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_267() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-267",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #268 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_268() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-268",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #269 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_269() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-269",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #270 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_270() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-270",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #271 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_271() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-271",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #272 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_272() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-272",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #273 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_273() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-273",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #274 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_274() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-274",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }

    /**
     * Диагностический снимок конфигурации и состояния модулей #275 (Assistant Vadimka v8.0 TITAN).
     */
    public static TelemetryReport buildTelemetrySnapshot_275() {
        int active = 0;
        if (companionEnabled) active++;
        if (safeAutopilot) active++;
        if (minimapRadarEnabled) active++;
        if (minimapCircularMode) active++;
        if (litematicaHudEnabled) active++;
        if (combatHudEnabled) active++;
        if (spotifyHudEnabled) active++;
        if (autoClickVaults) active++;
        if (vaultSmartWaitLoot) active++;
        if (autoCycleLectern) active++;
        if (highlightTrialVaults) active++;
        if (soundRadarSonar) active++;
        if (ariadneThread) active++;
        if (elytraPitchHorizon) active++;
        if (trajectories3D) active++;
        if (hotbarAutoRefill) active++;
        return new TelemetryReport(
            "V8Titan-275",
            active,
            countNearbyVaultBlocks(),
            rememberedChests.size(),
            rememberedVillagers.size(),
            safeAutopilot,
            gpuMapReady,
            spotifyConnected,
            currentForm.codeName,
            getActiveVaultTargetLabel()
        );
    }
}
