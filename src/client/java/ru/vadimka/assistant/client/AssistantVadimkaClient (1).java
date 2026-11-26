package ru.vadimka.assistant.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AssistantVadimkaClient implements ClientModInitializer {
    public static final String MOD_ID = "assistant_vadimka";
    public static final String VERSION_LABEL = "v4.0 PRO";

    // =========================================================================
    // ПЕРЕЧИСЛЕНИЯ: ОБЛИКИ, МИНЬОНЫ, ВКЛАДКИ, ТЕМЫ, РЕЖИМЫ ПРЕВЬЮ И AFK-ИГРЫ
    // =========================================================================

    public enum VadimkaForm {
        RAT("Rat (Крыска)", "Крыска", EntityType.SILVERFISH, 1.05, 42, 0xFF94A3B8, 0xFFF472B6),
        ALLAY("Allay (Спешик)", "Спешик", EntityType.ALLAY, 0.85, 38, 0xFF22D3EE, 0xFFA5F3FC),
        BREEZE("Breeze (Вихрь)", "Вихрь", EntityType.BREEZE, 0.54, 24, 0xFF818CF8, 0xFFC7D2FE),
        WARDEN("Warden (Хранитель)", "Warden", EntityType.WARDEN, 0.34, 15, 0xFF0D9488, 0xFF2DD4BF),
        AXOLOTL("Blue Axolotl (Аксолотль)", "Аксолотль", EntityType.AXOLOTL, 0.90, 36, 0xFF60A5FA, 0xFFF472B6),
        PLAYER("Player (Мини-игрок)", "Игрок", EntityType.ALLAY, 0.78, 28, 0xFF38BDF8, 0xFFFDE68A);

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
        VADIMKA("Вадимка", "Спутник Вадимка и команда миньонов"),
        AUTOPILOT("Автопилот", "Автопилот, спринт и профайлер стиля"),
        AUTO_BUILD("Постройка", "Строительство, линейка и редстоун"),
        LITEMATICA_HUD("Litematica", "Litematica HUD, слои и материалы"),
        VISUAL_RADAR("Радар", "Мини-карта с рельефом и разведка"),
        CHESTS_VILLAGERS("База и Лут", "Сундуки, точка дома и жители"),
        COMBAT_PVP("Бой и PvP", "Боевой HUD, тотемы, броня и смерть"),
        MUSIC_AFK("Музыка / AFK", "Spotify HUD и 3 MC мини-игры"),
        CUSTOM_VISUAL("Кастом", "3D-гардероб, крылья, прицел и темы");

        public final String shortTitle;
        public final String fullTitle;

        MenuTab(String shortTitle, String fullTitle) {
            this.shortTitle = shortTitle;
            this.fullTitle = fullTitle;
        }
    }

    public enum MenuTheme {
        CYBER_EMERALD("Cyber Emerald", 0xF5080D12, 0xFF0D151F, 0xFF13202E, 0xFF10B981, 0xFF38BDF8, 0xFF1D3347),
        AMETHYST_VOID("Amethyst Void", 0xF50E0918, 0xFF171026, 0xFF211738, 0xFFA855F7, 0xFFF472B6, 0xFF3A255E),
        NETHER_CRIMSON("Nether Crimson", 0xF514090D, 0xFF1E0E14, 0xFF2B141D, 0xFFF43F5E, 0xFFFBBF24, 0xFF4A1F2E),
        ARCTIC_FROST("Arctic Frost", 0xF508101A, 0xFF0F1B2A, 0xFF17283D, 0xFF38BDF8, 0xFF7DD3FC, 0xFF264263),
        OBSIDIAN_GOLD("Obsidian Gold", 0xF50E0C09, 0xFF17140F, 0xFF241F17, 0xFFF59E0B, 0xFFFDE047, 0xFF4A3D24);

        public final String title;
        public final int bgWindow;
        public final int bgPanel;
        public final int bgCard;
        public final int accent;
        public final int secondary;
        public final int borderMuted;

        MenuTheme(String title, int bgWindow, int bgPanel, int bgCard,
                  int accent, int secondary, int borderMuted) {
            this.title = title;
            this.bgWindow = bgWindow;
            this.bgPanel = bgPanel;
            this.bgCard = bgCard;
            this.accent = accent;
            this.secondary = secondary;
            this.borderMuted = borderMuted;
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
    // СОСТОЯНИЕ ВСЕХ 9 МОДУЛЕЙ
    // =========================================================================

    // 1. Вадимка и миньоны
    public static VadimkaForm currentForm = VadimkaForm.RAT;
    public static boolean companionVisibleInWorld = true;
    public static boolean minionsVisibleInWorld = true;
    public static boolean minionsSmartAi = true;
    public static boolean compactNameplates = true;
    public static int companionScalePercent = 100; // 60..140
    public static boolean quietMode = false;
    public static boolean randomChaosMode = false;
    public static boolean voiceCommentator = true;
    public static final Map<MinionRole, Boolean> activeMinions = new EnumMap<>(MinionRole.class);

    // 2. Автопилот и профайлер
    public static boolean styleProfiler = true;
    public static boolean sprintAssist = false;
    public static boolean safeAutopilot = false;
    public static boolean autoReturnControl = true;
    public static boolean parkourEdgeGuard = true;
    public static boolean fallDangerAlert = true;

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

    // 5. Радар и мини-карта (Ультра-быстрый RLE движок)
    public static boolean radarHudEnabled = true;
    public static boolean radarTerrainMode = true;
    public static boolean radarRotateMap = false; // false = чёткий Север вверх (как Xaero's)
    public static boolean radarRightSide = false;
    public static int radarZoomLevel = 1; // 0 = близко (1б), 1 = средне (2б), 2 = далеко (3б)
    public static boolean soundRadarEnabled = true;
    public static boolean ariadneThread = true;
    public static boolean sixthSense = true;

    // 6. База, сундуки и лут
    public static boolean chestMemory = true;
    public static boolean radarShowChests = true;
    public static boolean radarShowDrops = true;
    public static boolean villagerTracker = true;
    public static boolean smartSorter = false;
    public static BlockPos homeWaypoint = null;

    // 7. Бой и PvP
    public static boolean combatHudEnabled = true;
    public static boolean armorDurabilityHud = true;
    public static boolean totemScanner = true;
    public static boolean trajectoriesEnabled = true;
    public static boolean deathReplayMarker = true;

    // 8. Музыка и AFK
    public static boolean spotifyHudEnabled = true;
    public static int spotifyTrackIndex = 0;
    public static boolean afkPanicExit = true;
    public static AfkGameType selectedAfkGame = AfkGameType.RUNNER;
    public static final List<String> DEMO_TRACKS = List.of(
        "C418 — Aria Math",
        "Lena Raine — Pigstep",
        "C418 — Sweden (Lo-Fi)",
        "Aaron Cherof — Precipice",
        "Lena Raine — Otherside"
    );

    // 9. Кастом-визуал
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
    // КЭШИ ПРОИЗВОДИТЕЛЬНОСТИ (НУЛЕВОЕ ПРОСЕДАНИЕ FPS!)
    // =========================================================================

    private static KeyMapping openMenuKey;
    private static Mob companionEntity = null;
    private static VadimkaForm spawnedForm = null;
    private static int spawnedScalePercent = -1;
    private static boolean spawnedCompactTags = true;
    private static final Map<MinionRole, Mob> minionEntities = new EnumMap<>(MinionRole.class);
    private static int worldTick = 0;

    // Отдельная 3D-модель только для студии превью (не спавнится в мир)
    private static Mob studioPreviewMob = null;
    private static VadimkaForm studioPreviewForm = null;

    // Инкрементальный кэш карты местности (32x32 -> по 2 строки за тик = 0 лагов!)
    private static final int MAP_GRID = 32;
    private static final int[] terrainColors = new int[MAP_GRID * MAP_GRID];
    private static final int[] terrainHeights = new int[MAP_GRID * MAP_GRID];
    private static final int[] rowRenderBuf = new int[MAP_GRID];
    private static int mapCenterX = Integer.MIN_VALUE;
    private static int mapCenterZ = Integer.MIN_VALUE;
    private static int mapLastZoom = -1;
    private static int mapScanRow = 0;
    private static boolean mapNeedsFullReset = true;

    // Инкрементальный сканер сундуков (1 срез X за 2 тика = 0 лагов!)
    private static final int CHEST_RADIUS = 14;
    private static final List<BlockPos> confirmedChests = new ArrayList<>();
    private static final List<BlockPos> scanningChests = new ArrayList<>();
    private static int chestScanSliceX = -CHEST_RADIUS;
    private static BlockPos chestScanOrigin = BlockPos.ZERO;

    // Кэшированные сущности для радара и умных миньонов (обновляется раз в 4 тика)
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
        "Писк! Пора за алмазами на Y = -58!",
        "Миньоны работают: разведка и сбор на позициях!",
        "Криперы любят тишину за спиной — я присматриваю.",
        "Крысиный радар докладывает: вокруг спокойно!",
        "Открой [P] — покрути себя и меня в 3D-студии!"
    );

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
                breadcrumbs.clear();
                confirmedChests.clear();
                scanningChests.clear();
                cachedRadarBlips.clear();
                lastBreadcrumb = null;
                lastPlayerPos = null;
                mapNeedsFullReset = true;
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

    private static void pushVadimkaToast(String text, int durationTicks) {
        if (quietMode) {
            return;
        }
        vadimkaToastText = text;
        vadimkaToastTicks = durationTicks;
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
    // ОПТИМИЗИРОВАННЫЙ ИГРОВОЙ ТИК (НУЛЕВЫЕ ФРИЗЫ, 75+ FPS)
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

        // Авто-спринт ассистент (реально работает при зажатом W)
        if (sprintAssist && mc.screen == null && mc.options.keyUp.isDown()
                && !player.isCrouching() && !player.isUsingItem()
                && player.getFoodData().getFoodLevel() > 6) {
            player.setSprinting(true);
        }

        // Профайлер пройденной дистанции
        Vec3 curPos = player.position();
        if (styleProfiler && lastPlayerPos != null) {
            double moved = curPos.distanceTo(lastPlayerPos);
            if (moved < 10.0) {
                sessionDistanceWalked += moved;
            }
        }
        lastPlayerPos = curPos;

        // Проверка здоровья, урона и точки смерти
        float hp = player.getHealth();
        if (hp <= 0.0F && lastPlayerHealth > 0.0F && deathReplayMarker) {
            lastDeathPos = player.blockPosition();
            pushVadimkaToast("Вадимка: Точка смерти (" + lastDeathPos.getX() + ", " + lastDeathPos.getY() + ", " + lastDeathPos.getZ() + ")", 120);
        } else if (hp < lastPlayerHealth - 0.1F) {
            if (voiceCommentator) {
                pushVadimkaToast("Вадимка: Получен урон! HP: " + (int) Math.ceil(hp), 65);
            }
            if (afkPanicExit && mc.screen instanceof AssistantVadimkaScreen scr && scr.afkGameRunning) {
                scr.afkGameRunning = false;
                mc.setScreen(null);
                pushVadimkaToast("Вадимка: ЭКСТРЕННЫЙ ВЫХОД ИЗ AFK!", 90);
            }
        }
        lastPlayerHealth = hp;

        // Предупреждение о прочности инструмента / малом стаке блоков (раз в 20 тиков)
        if (worldTick % 20 == 0 && (toolDurabilityAlert || hotbarAutoRefill)) {
            ItemStack hand = player.getMainHandItem();
            if (!hand.isEmpty() && toolDurabilityAlert && hand.isDamageableItem() && hand.getMaxDamage() > 0) {
                int left = hand.getMaxDamage() - hand.getDamageValue();
                if (left > 0 && (left * 100) / hand.getMaxDamage() <= 12 && (worldTick - lastLowToolWarnTick) > 200) {
                    pushVadimkaToast("Снабженец: " + hand.getHoverName().getString() + " почти сломан (" + left + ")!", 70);
                    lastLowToolWarnTick = worldTick;
                }
            }
        }

        // Проверка обрыва и края блока (раз в 4 тика, сверхдёшево)
        if ((fallDangerAlert || parkourEdgeGuard) && worldTick % 4 == 0) {
            BlockPos below = player.blockPosition().below();
            boolean airBelow = level.getBlockState(below).isAir();
            fallDropBlocks = airBelow ? measureDropBelow(level, player.blockPosition()) : 0;
            edgeDangerNow = parkourEdgeGuard && fallDropBlocks >= 4;
        }

        // Случайные реплики
        if (randomChaosMode && !quietMode && worldTick % 600 == 0) {
            pushVadimkaToast("Вадимка: " + CHAOS_QUOTES.get(worldRandom.nextInt(CHAOS_QUOTES.size())), 85);
        }

        // 1. Сканирование сущностей (раз в 4 тика — обслуживает и радар, и Шестое чувство, и AI миньонов!)
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

        // 3. 3D-крылья и нимб/рога (ТОЛЬКО от 3-го лица F5, чтобы не мешать обзору от 1-го лица!)
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

        // 5. Инкрементальное обновление карты рельефа (всего 2 строки за тик = 0 лагов!)
        if (radarHudEnabled && radarTerrainMode) {
            tickIncrementalTerrainMap(level, Mth.floor(player.getX()), Mth.floor(player.getZ()));
        }

        // 6. Инкрементальный сканер сундуков (1 срез X раз в 2 тика = 0 лагов!)
        if (radarHudEnabled && (radarShowChests || chestMemory) && worldTick % 2 == 0) {
            tickIncrementalChestScanner(level, player.blockPosition());
        }
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
        double nearestLootDistSq = 121.0; // до 11 блоков

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

        // Обновляем ровно 2 строки за тик (64 колонки = ~0.04 мс, нулевой лаг!)
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
        // 1. 3D-спутник Вадимка
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

        // 2. Команда 4 миньонов с живым AI (летят к луту / врагам / блоку стройки!)
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

                // Умное поведение по ролям!
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
    // ИГРОВОЙ HUD (СВЕРХБЫСТРАЯ RLE МИНИ-КАРТА + ЧИСТЫЕ ВИДЖЕТЫ)
    // =========================================================================

    private static void renderAllHudWidgets(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
            return;
        }
        // Не тратим ресурсы на фоновый HUD, пока открыто главное меню [P]
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
        if (sixthSense && threatBehindPlayer) {
            String warn = "⚠ ОПАСНОСТЬ СЗАДИ (" + String.format("%.1f", nearestHostileDist) + "м)";
            int ww = mc.font.width(warn) + 18;
            int wx = (sw - ww) / 2;
            int wy = 12;
            fillRoundedRect(graphics, wx, wy, ww, 18, 0xE63B0A12);
            drawRoundedBorder(graphics, wx, wy, ww, 18, 0xFFEF4444);
            graphics.drawString(mc.font, warn, wx + 9, wy + 5, 0xFFFCA5A5);
        } else if (fallDangerAlert && fallDropBlocks >= 6 && mc.screen == null) {
            String warn = "⚠ ОБРЫВ: " + fallDropBlocks + " бл.";
            int ww = mc.font.width(warn) + 18;
            int wx = (sw - ww) / 2;
            int wy = 12;
            fillRoundedRect(graphics, wx, wy, ww, 18, 0xE63B2508);
            drawRoundedBorder(graphics, wx, wy, ww, 18, 0xFFF59E0B);
            graphics.drawString(mc.font, warn, wx + 9, wy + 5, 0xFFFDE68A);
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

    private static void renderMinimapRadarHud(GuiGraphics graphics, Minecraft mc, int screenW) {
        int cell = 3;
        int mapSize = MAP_GRID * cell; // 96x96 px (чётко, ровно, без мыла и лагов!)
        int pad = 8;
        int x = radarRightSide ? (screenW - mapSize - pad - 4) : pad;
        int y = 32;
        int cx = x + mapSize / 2;
        int cy = y + mapSize / 2;

        // Корпус мини-карты
        fillRoundedRect(graphics, x - 2, y - 2, mapSize + 4, mapSize + 4, currentTheme.bgWindow);
        drawRoundedBorder(graphics, x - 2, y - 2, mapSize + 4, mapSize + 4, currentTheme.accent);

        int stepBlocks = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        int halfGrid = MAP_GRID / 2;
        double yawRad = Math.toRadians(mc.player.getYRot() + 180.0);
        double cos = radarRotateMap ? Math.cos(yawRad) : 1.0;
        double sin = radarRotateMap ? Math.sin(yawRad) : 0.0;

        if (radarTerrainMode) {
            // СВЕРХБЫСТРЫЙ RLE-РЕНДЕР (объединяем соседние пиксели одного цвета в 1 draw call!)
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

        // Тонкая сетка
        graphics.fill(cx, y + 2, cx + 1, y + mapSize - 2, 0x24FFFFFF);
        graphics.fill(x + 2, cy, x + mapSize - 2, cy + 1, 0x24FFFFFF);

        double mapScale = (double) cell / stepBlocks;
        int maxOff = (mapSize / 2) - 4;

        // Звуковой радар угроз (красная метка по краю в сторону ближайшего монстра)
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

        // Точка дома на карте (если сохранена)
        if (homeWaypoint != null) {
            double dx = (homeWaypoint.getX() + 0.5) - mc.player.getX();
            double dz = (homeWaypoint.getZ() + 0.5) - mc.player.getZ();
            double rotX = (dx * cos - dz * sin) * mapScale;
            double rotY = (dx * sin + dz * cos) * mapScale;
            int hx = cx + (int) Mth.clamp(rotX, -maxOff, maxOff);
            int hy = cy + (int) Mth.clamp(rotY, -maxOff, maxOff);
            graphics.fill(hx - 2, hy - 2, hx + 3, hy + 3, 0xFF000000);
            graphics.fill(hx - 1, hy - 1, hx + 2, hy + 2, 0xFF22D3EE);
        }

        // Сундуки на карте (из инкрементального кэша)
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

        // Сущности вокруг игрока (из кэша cachedRadarBlips)
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

        // Маркер Севера (N)
        if (!radarRotateMap) {
            graphics.fill(cx - 4, y + 1, cx + 4, y + 10, 0xCC090E14);
            graphics.drawString(mc.font, "N", cx - 2, y + 2, 0xFF38BDF8);
        } else {
            int nx = cx + (int) (-sin * (maxOff - 2)) - 2;
            int ny = cy + (int) (-cos * (maxOff - 2)) - 3;
            graphics.fill(nx - 1, ny - 1, nx + 7, ny + 8, 0xCC090E14);
            graphics.drawString(mc.font, "N", nx, ny, 0xFF38BDF8);
        }

        // Маркер игрока + направление взгляда
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

        // Аккуратная нижняя инфо-плашка (строго по ширине карты)
        int infoY = y + mapSize + 5;
        int infoW = mapSize + 4;
        int infoH = 29;
        fillRoundedRect(graphics, x - 2, infoY, infoW, infoH, currentTheme.bgWindow);
        drawRoundedBorder(graphics, x - 2, infoY, infoW, infoH, currentTheme.secondary);

        String coords = (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
        String dir = getCompassDirection(mc.player.getYRot());
        graphics.drawString(mc.font, trimText(mc.font, coords + " " + dir, infoW - 8), x + 2, infoY + 4, 0xFFF1F5F9);
        String line2 = (homeWaypoint != null)
            ? ("⌂ " + (int) Math.sqrt(mc.player.blockPosition().distSqr(homeWaypoint)) + "м • " + currentForm.shortName)
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

        boolean showDeath = deathReplayMarker && lastDeathPos != null;
        int w = 132;
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

        // Индикатор натяжения лука / трезубца
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
        int w = 152;
        int h = 24;
        int x = 10;
        int y = sh - h - 10;
        fillRoundedRect(g, x, y, w, h, currentTheme.bgWindow);
        drawRoundedBorder(g, x, y, w, h, 0xFF1DB954);

        long t = System.currentTimeMillis() / 130L;
        for (int i = 0; i < 4; i++) {
            int bh = 4 + (int) (Math.abs(Math.sin(t * 0.7 + i * 1.3)) * 10);
            g.fill(x + 6 + i * 4, y + h - 5 - bh, x + 8 + i * 4, y + h - 5, 0xFF1DB954);
        }
        String track = DEMO_TRACKS.get(spotifyTrackIndex % DEMO_TRACKS.size());
        g.drawString(mc.font, trimText(mc.font, track, w - 30), x + 26, y + 8, 0xFFF1F5F9);
    }

    // =========================================================================
    // КАСТОМНАЯ GPU-ГРАФИКА И УТИЛИТЫ (БЕЗ СЕРЫХ КНОПОК, 1-CALL ГРАДИЕНТЫ)
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
    // ГЛАВНОЕ КАСТОМНОЕ МЕНЮ НА КЛАВИШУ [P] С НАСТОЯЩИМ GPU 3D-ПРЕВЬЮ
    // =========================================================================

    public static class AssistantVadimkaScreen extends Screen {
        private MenuTab activeTab;
        private int currentPage = 0;
        private int totalPages = 1;

        private static class TabVisual {
            final MenuTab tab;
            final int x;
            final int y;
            final int w;
            final int h;

            TabVisual(MenuTab tab, int x, int y, int w, int h) {
                this.tab = tab;
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
            }
        }

        private static class SettingRowVisual {
            final int x;
            final int y;
            final int w;
            final int h;
            final int btnX;
            final int btnY;
            final int btnW;
            final int btnH;
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
            final int x;
            final int y;
            final int w;
            final int h;
            final String label;
            final boolean danger;
            final boolean highlighted;

            SmallBtnVisual(int x, int y, int w, int h, String label, boolean danger, boolean highlighted) {
                this.x = x;
                this.y = y;
                this.w = w;
                this.h = h;
                this.label = label;
                this.danger = danger;
                this.highlighted = highlighted;
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

        // Состояние 3 играбельных AFK-мини-игр
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

        private int winX;
        private int winY;
        private int winW;
        private int winH;
        private int sidebarW;
        private int previewW;
        private int contentX;
        private int contentW;

        public AssistantVadimkaScreen(MenuTab activeTab) {
            super(Component.literal("ASSISTANT VADIMKA"));
            this.activeTab = activeTab;
        }

        @Override
        protected void init() {
            this.tabVisuals.clear();
            this.rowVisuals.clear();
            this.smallBtnVisuals.clear();

            this.winW = Math.min(this.width - 16, 620);
            this.winH = Math.min(this.height - 16, 342);
            this.winX = (this.width - this.winW) / 2;
            this.winY = (this.height - this.winH) / 2;

            this.sidebarW = 114;
            this.previewW = 176;
            this.contentX = this.winX + this.sidebarW + 8;
            this.contentW = this.winW - this.sidebarW - this.previewW - 22;

            // 1. Вкладки в левой боковой панели
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
            int themeBtnY = this.winY + this.winH - 29;
            addSmallButton(this.winX + 7, themeBtnY, this.sidebarW - 14, 20, "Тема: " + trimShortTheme(currentTheme), false, true, () -> {
                MenuTheme[] themes = MenuTheme.values();
                currentTheme = themes[(currentTheme.ordinal() + 1) % themes.length];
                this.rebuildWidgets();
            });

            // Кнопка закрытия в шапке
            addSmallButton(this.winX + this.winW - 26, this.winY + 7, 19, 18, "✕", true, false, this::onClose);

            // Кнопки выбора режима настоящей 3D-студии справа (Дуэт / Игрок / Вадимка)
            int prevX = this.winX + this.winW - this.previewW - 6;
            int prevCtrlY = this.winY + 186;
            int btn3W = (this.previewW - 24) / 3;
            PreviewMode[] modes = PreviewMode.values();
            for (int i = 0; i < modes.length; i++) {
                PreviewMode m = modes[i];
                addSmallButton(prevX + 8 + i * (btn3W + 4), prevCtrlY, btn3W, 17, m.label, false, previewMode == m, () -> {
                    previewMode = m;
                    this.rebuildWidgets();
                });
            }

            // 2. Карточки настроек текущей вкладки
            List<RowSpec> specs = buildTabSpecs(this.activeTab);
            int maxPerPage = (this.activeTab == MenuTab.MUSIC_AFK && this.afkGameRunning) ? 3 : 7;
            this.totalPages = Math.max(1, (specs.size() + maxPerPage - 1) / maxPerPage);
            this.currentPage = Mth.clamp(this.currentPage, 0, this.totalPages - 1);

            if (this.totalPages > 1) {
                int navY = this.winY + 40;
                addSmallButton(this.contentX + this.contentW - 68, navY, 20, 16, "◀", false, false, () -> {
                    this.currentPage = (this.currentPage - 1 + this.totalPages) % this.totalPages;
                    this.rebuildWidgets();
                });
                addSmallButton(this.contentX + this.contentW - 26, navY, 20, 16, "▶", false, false, () -> {
                    this.currentPage = (this.currentPage + 1) % this.totalPages;
                    this.rebuildWidgets();
                });
            }

            int startIdx = this.currentPage * maxPerPage;
            int endIdx = Math.min(specs.size(), startIdx + maxPerPage);
            int rowY = this.winY + 62;

            for (int i = startIdx; i < endIdx; i++) {
                RowSpec spec = specs.get(i);
                int rx = this.contentX + 7;
                int rw = this.contentW - 14;
                int rh = 28;
                int btnW = spec.isToggle ? 50 : 88;
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

        private void addInvisibleHitbox(int x, int y, int w, int h, Runnable action) {
            this.addRenderableWidget(Button.builder(Component.empty(), b -> action.run()).bounds(x, y, w, h).build());
        }

        private void addSmallButton(int x, int y, int w, int h, String label,
                                    boolean danger, boolean highlighted, Runnable action) {
            this.smallBtnVisuals.add(new SmallBtnVisual(x, y, w, h, label, danger, highlighted));
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
                    list.add(toggleSpec("Ассистент спринта", "Авто-бег при зажатой клавише W", sprintAssist, () -> sprintAssist = !sprintAssist));
                    list.add(toggleSpec("Паркур-страховка края", "Предупреждение на краю блока", parkourEdgeGuard, () -> parkourEdgeGuard = !parkourEdgeGuard));
                    list.add(toggleSpec("Сигнал обрыва (HUD)", "Тревога при обрыве >= 6 блоков", fallDangerAlert, () -> fallDangerAlert = !fallDangerAlert));
                    list.add(toggleSpec("Безопасный автопилот", "Ведение с учетом правил сервера", safeAutopilot, () -> safeAutopilot = !safeAutopilot));
                    list.add(toggleSpec("Мгновенный возврат", "Передача управления по клавише", autoReturnControl, () -> autoReturnControl = !autoReturnControl));
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
                    list.add(toggleSpec("Мини-карта-радар (HUD)", "Высокоскоростная RLE-карта", radarHudEnabled, () -> radarHudEnabled = !radarHudEnabled));
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
                }
                case CHESTS_VILLAGERS -> {
                    String homeSub = homeWaypoint == null
                        ? "Метка ещё не задана"
                        : ("Дом: " + homeWaypoint.getX() + " / " + homeWaypoint.getY() + " / " + homeWaypoint.getZ());
                    list.add(cycleSpec("Точка дома на карте", homeSub, homeWaypoint == null ? "Задать" : "Обновить", () -> {
                        Minecraft mc = Minecraft.getInstance();
                        if (mc.player != null) {
                            homeWaypoint = mc.player.blockPosition();
                            pushVadimkaToast("Вадимка: Точка дома сохранена!", 60);
                        }
                    }));
                    list.add(toggleSpec("Память сундуков", "Найдено хранилищ рядом: " + confirmedChests.size(), chestMemory, () -> chestMemory = !chestMemory));
                    list.add(toggleSpec("Метки сундуков на карте", "Оранжевые точки хранилищ", radarShowChests, () -> radarShowChests = !radarShowChests));
                    list.add(toggleSpec("Метки дропа на радаре", "Золотые точки выпавшего лута", radarShowDrops, () -> radarShowDrops = !radarShowDrops));
                    list.add(toggleSpec("Трекер жителей", "Фиолетовые метки на мини-карте", villagerTracker, () -> villagerTracker = !villagerTracker));
                    list.add(toggleSpec("Умный сортировщик", "Быстрая раскладка инвентаря", smartSorter, () -> smartSorter = !smartSorter));
                }
                case COMBAT_PVP -> {
                    list.add(toggleSpec("Боевой HUD (Тотемы/HP)", "Панель выживания справа внизу", combatHudEnabled, () -> combatHudEnabled = !combatHudEnabled));
                    list.add(toggleSpec("Индикатор брони", "Полоски прочности Ш/Н/П/Б", armorDurabilityHud, () -> armorDurabilityHud = !armorDurabilityHud));
                    list.add(toggleSpec("Сканер тотемов", "Контроль тотема в руке и сумке", totemScanner, () -> totemScanner = !totemScanner));
                    list.add(toggleSpec("Шкала натяжения лука", "Прогресс заряда под прицелом", trajectoriesEnabled, () -> trajectoriesEnabled = !trajectoriesEnabled));
                    list.add(toggleSpec("Метка точки смерти", "Дистанция до последней гибели", deathReplayMarker, () -> deathReplayMarker = !deathReplayMarker));
                }
                case MUSIC_AFK -> {
                    list.add(toggleSpec("Spotify Connect HUD", "Виджет трека с эквалайзером", spotifyHudEnabled, () -> spotifyHudEnabled = !spotifyHudEnabled));
                    list.add(cycleSpec("Следующий трек", DEMO_TRACKS.get(spotifyTrackIndex % DEMO_TRACKS.size()), "Трек #" + (spotifyTrackIndex + 1), () -> {
                        spotifyTrackIndex = (spotifyTrackIndex + 1) % DEMO_TRACKS.size();
                    }));
                    list.add(cycleSpec("Выбор MC мини-игры", selectedAfkGame.hint, selectedAfkGame.title, () -> {
                        AfkGameType[] gs = AfkGameType.values();
                        selectedAfkGame = gs[(selectedAfkGame.ordinal() + 1) % gs.length];
                        resetAfkGameState();
                    }));
                    list.add(cycleSpec("Запуск AFK-игры", "Управление: A / D / SPACE / ESC", afkGameRunning ? "■ СТОП" : "▶ ИГРАТЬ", () -> {
                        afkGameRunning = !afkGameRunning;
                        if (afkGameRunning) {
                            resetAfkGameState();
                        }
                    }));
                    list.add(toggleSpec("Экстренный выход", "Закрыть при уроне или враге", afkPanicExit, () -> afkPanicExit = !afkPanicExit));
                }
                case CUSTOM_VISUAL -> {
                    list.add(cycleSpec("Тема меню [P]", "5 премиум-палитр интерфейса", currentTheme.title, () -> {
                        MenuTheme[] themes = MenuTheme.values();
                        currentTheme = themes[(currentTheme.ordinal() + 1) % themes.length];
                    }));
                    list.add(cycleSpec("Крылья персонажа", "Видны в F5 за спиной игрока", WINGS_OPTIONS.get(wingsIndex), () -> {
                        wingsIndex = (wingsIndex + 1) % WINGS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Рога / Нимб", "Аксессуар над головой в F5", HORNS_OPTIONS.get(hornsIndex), () -> {
                        hornsIndex = (hornsIndex + 1) % HORNS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Шляпка / Ушки", "Выбранный стиль гардероба", HATS_OPTIONS.get(hatsIndex), () -> {
                        hatsIndex = (hatsIndex + 1) % HATS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Одежда и плащ", "Пресет экипировки", OUTFIT_OPTIONS.get(outfitIndex), () -> {
                        outfitIndex = (outfitIndex + 1) % OUTFIT_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Кастомный прицел", "Неоновый прицел по центру", CROSSHAIR_OPTIONS.get(crosshairIndex), () -> {
                        crosshairIndex = (crosshairIndex + 1) % CROSSHAIR_OPTIONS.size();
                    }));
                    list.add(toggleSpec("3D-крылья и нимб в F5", "Авто-скрытие от 1-го лица", worldCosmeticsParticles, () -> worldCosmeticsParticles = !worldCosmeticsParticles));
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
            if (afkGameRunning) {
                int k = event.key();
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
            if (event.key() == GLFW.GLFW_KEY_P) {
                this.onClose();
                return true;
            }
            return super.keyPressed(event);
        }

        @Override
        public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(0, 0, this.width, this.height, 0xA805080D);
        }

        @Override
        public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            super.render(g, mouseX, mouseY, partialTick);

            // 1. Главное окно (1-call GPU градиент и неоновая рамка)
            fillRoundedRect(g, winX, winY, winW, winH, currentTheme.bgWindow);
            drawRoundedBorder(g, winX, winY, winW, winH, currentTheme.accent);

            // Шапка
            g.fillGradient(winX + 2, winY + 2, winX + winW - 2, winY + 31, currentTheme.bgCard, currentTheme.bgPanel);
            g.fill(winX + 2, winY + 31, winX + winW - 2, winY + 32, currentTheme.accent);

            // Заголовок и бейдж версии
            g.fill(winX + 10, winY + 12, winX + 16, winY + 18, currentTheme.accent);
            g.drawString(this.font, "ASSISTANT VADIMKA", winX + 22, winY + 11, 0xFFFFFFFF);
            int verX = winX + 22 + this.font.width("ASSISTANT VADIMKA") + 8;
            fillRoundedRect(g, verX, winY + 9, 94, 13, 0xFF141E2C);
            drawRoundedBorder(g, verX, winY + 9, 94, 13, currentTheme.secondary);
            g.drawString(this.font, VERSION_LABEL + " • 1.21.11", verX + 5, winY + 12, currentTheme.secondary);

            // 2. Левая боковая панель вкладок
            fillRoundedRect(g, winX + 4, winY + 35, sidebarW - 2, winH - 39, currentTheme.bgPanel);
            drawRoundedBorder(g, winX + 4, winY + 35, sidebarW - 2, winH - 39, currentTheme.borderMuted);

            for (TabVisual tv : tabVisuals) {
                boolean active = tv.tab == this.activeTab;
                boolean hover = isOver(mouseX, mouseY, tv.x, tv.y, tv.w, tv.h);
                int bg = active
                    ? lerpColor(currentTheme.bgCard, currentTheme.accent, 0.30F)
                    : (hover ? currentTheme.bgCard : 0xFF0C121B);
                fillRoundedRect(g, tv.x, tv.y, tv.w, tv.h, bg);
                if (active) {
                    g.fill(tv.x + 1, tv.y + 4, tv.x + 3, tv.y + tv.h - 4, currentTheme.accent);
                    drawRoundedBorder(g, tv.x, tv.y, tv.w, tv.h, currentTheme.accent);
                } else if (hover) {
                    drawRoundedBorder(g, tv.x, tv.y, tv.w, tv.h, currentTheme.secondary);
                }
                int iconCol = active ? currentTheme.accent : (hover ? currentTheme.secondary : 0xFF64748B);
                drawTabIcon(g, tv.tab, tv.x + 6, tv.y + 6, iconCol);
                String tLabel = trimText(this.font, tv.tab.shortTitle, tv.w - 24);
                g.drawString(this.font, tLabel, tv.x + 20, tv.y + 7, active ? 0xFFFFFFFF : (hover ? 0xFFE2E8F0 : 0xFF94A3B8));
            }

            // 3. Центральная панель настроек
            fillRoundedRect(g, contentX, winY + 35, contentW, winH - 39, currentTheme.bgPanel);
            drawRoundedBorder(g, contentX, winY + 35, contentW, winH - 39, currentTheme.borderMuted);

            int titleMaxW = totalPages > 1 ? (contentW - 84) : (contentW - 18);
            g.drawString(this.font, trimText(this.font, activeTab.fullTitle, titleMaxW), contentX + 10, winY + 44, currentTheme.secondary);
            if (totalPages > 1) {
                String pStr = (currentPage + 1) + "/" + totalPages;
                g.drawString(this.font, pStr, contentX + contentW - 44, winY + 44, 0xFF94A3B8);
            }
            g.fill(contentX + 8, winY + 57, contentX + contentW - 8, winY + 58, currentTheme.borderMuted);

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
                    // Чёткий контрастный тумблер ON / OFF (ничего не наезжает!)
                    int trackCol = rv.toggleState ? currentTheme.accent : 0xFF1E293B;
                    fillRoundedRect(g, rv.btnX, rv.btnY + 1, rv.btnW, rv.btnH - 2, trackCol);
                    drawRoundedBorder(g, rv.btnX, rv.btnY + 1, rv.btnW, rv.btnH - 2,
                        btnHover ? 0xFFFFFFFF : (rv.toggleState ? currentTheme.accent : 0xFF475569));
                    int knobSize = rv.btnH - 6;
                    int knobX = rv.toggleState ? (rv.btnX + rv.btnW - knobSize - 3) : (rv.btnX + 3);
                    fillRoundedRect(g, knobX, rv.btnY + 3, knobSize, knobSize, 0xFFFFFFFF);
                    String tag = rv.toggleState ? "ВКЛ" : "ВЫКЛ";
                    int tagX = rv.toggleState ? (rv.btnX + 6) : (rv.btnX + rv.btnW - this.font.width(tag) - 5);
                    g.drawString(this.font, tag, tagX, rv.btnY + 5, rv.toggleState ? 0xFFFFFFFF : 0xFF94A3B8);
                } else {
                    int selBg = btnHover ? lerpColor(currentTheme.bgPanel, currentTheme.accent, 0.35F) : 0xFF0B111A;
                    fillRoundedRect(g, rv.btnX, rv.btnY, rv.btnW, rv.btnH, selBg);
                    drawRoundedBorder(g, rv.btnX, rv.btnY, rv.btnW, rv.btnH,
                        btnHover ? currentTheme.accent : currentTheme.secondary);
                    String val = trimText(this.font, rv.valueLabel, rv.btnW - 10);
                    int vx = rv.btnX + (rv.btnW - this.font.width(val)) / 2;
                    g.drawString(this.font, val, vx, rv.btnY + 5, btnHover ? 0xFFFFFFFF : currentTheme.secondary);
                }
            }

            if (activeTab == MenuTab.MUSIC_AFK && afkGameRunning) {
                renderAfkGameCanvas(g, contentX + 8, winY + 166, contentW - 16, winH - 178);
            }

            // 4. Правая НАСТОЯЩАЯ GPU 3D-студия превью (реальный скин игрока + реальный 3D-моб Вадимка!)
            int prevX = winX + winW - previewW - 6;
            int prevY = winY + 35;
            int prevH = winH - 39;
            fillRoundedRect(g, prevX, prevY, previewW, prevH, currentTheme.bgPanel);
            drawRoundedBorder(g, prevX, prevY, previewW, prevH, currentTheme.secondary);
            renderReal3DStudio(g, prevX, prevY, previewW, prevH, mouseX, mouseY);

            // 5. Малые кнопки
            for (SmallBtnVisual sb : smallBtnVisuals) {
                boolean hover = isOver(mouseX, mouseY, sb.x, sb.y, sb.w, sb.h);
                int bg;
                int border;
                if (sb.danger) {
                    bg = hover ? 0xFFDC2626 : 0xFF331419;
                    border = hover ? 0xFFFCA5A5 : 0xFFEF4444;
                } else if (sb.highlighted) {
                    bg = hover
                        ? lerpColor(currentTheme.bgCard, currentTheme.accent, 0.45F)
                        : lerpColor(currentTheme.bgCard, currentTheme.accent, 0.25F);
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
        }

        private boolean isOver(int mx, int my, int x, int y, int w, int h) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        // =====================================================================
        // НАСТОЯЩАЯ GPU 3D-СТУДИЯ (РЕАЛЬНЫЙ 3D ИГРОК + РЕАЛЬНЫЙ 3D ВАДИМКА!)
        // =====================================================================

        private void renderReal3DStudio(GuiGraphics g, int px, int py, int pw, int ph, int mouseX, int mouseY) {
            g.drawString(this.font, "✦ 3D СТУДИЯ (GPU)", px + 8, py + 8, currentTheme.accent);

            int vpX = px + 8;
            int vpY = py + 22;
            int vpW = pw - 16;
            int vpH = 124;

            // Глубокий студийный градиент и неоновый подиум (2 GPU вызова, 0 лагов!)
            g.fillGradient(vpX, vpY, vpX + vpW, vpY + vpH, 0xFF060A11, lerpColor(0xFF0C1422, currentTheme.accent, 0.14F));
            drawRoundedBorder(g, vpX, vpY, vpW, vpH, currentTheme.borderMuted);

            // Голографический подиум внизу сцены
            int podY = vpY + vpH - 16;
            g.fill(vpX + 14, podY, vpX + vpW - 14, podY + 2, currentTheme.accent);
            g.fillGradient(vpX + 10, podY + 2, vpX + vpW - 10, vpY + vpH - 2, lerpColor(0xFF0A111C, currentTheme.secondary, 0.25F), 0xFF060A11);

            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.level != null) {
                LivingEntity pet = getOrCreateStudioCompanion(mc.level, mc.player);
                try {
                    if (previewMode == PreviewMode.DUO) {
                        int midX = vpX + (vpW * 54) / 100;
                        // Левая половина — настоящий 3D-игрок со своим скином!
                        InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, vpX + 2, vpY + 6, midX, vpY + vpH - 10,
                            36, 0.0625F, (float) mouseX, (float) mouseY, mc.player
                        );
                        // Правая половина — настоящий 3D-спутник Вадимка!
                        if (pet != null) {
                            InventoryScreen.renderEntityInInventoryFollowsMouse(
                                g, midX - 4, vpY + 12, vpX + vpW - 2, vpY + vpH - 10,
                                currentForm.previewGuiSize, 0.0625F, (float) mouseX, (float) mouseY, pet
                            );
                        }
                    } else if (previewMode == PreviewMode.PLAYER_ONLY) {
                        InventoryScreen.renderEntityInInventoryFollowsMouse(
                            g, vpX + 4, vpY + 4, vpX + vpW - 4, vpY + vpH - 8,
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
                    g.drawString(this.font, "3D Превью: " + currentForm.shortName, vpX + 10, vpY + vpH / 2 - 4, currentTheme.secondary);
                }
            }

            // Карточка сводки ниже кнопок переключения режима
            int infoY = py + 174;
            int infoH = ph - 182;
            fillRoundedRect(g, px + 8, infoY, pw - 16, infoH, currentTheme.bgCard);
            drawRoundedBorder(g, px + 8, infoY, pw - 16, infoH, currentTheme.borderMuted);

            int iy = infoY + 6;
            int maxW = pw - 28;
            g.drawString(this.font, trimText(this.font, "Вадимка: " + currentForm.shortName, maxW), px + 14, iy, currentTheme.accent);
            iy += 12;
            int activeM = 0;
            for (MinionRole r : MinionRole.values()) {
                if (Boolean.TRUE.equals(activeMinions.get(r))) {
                    activeM++;
                }
            }
            g.drawString(this.font, trimText(this.font, "Миньоны: " + activeM + "/4 • " + (minionsSmartAi ? "AI" : "Орбита"), maxW), px + 14, iy, 0xFFCBD5E1);
            iy += 13;
            g.fill(px + 14, iy - 2, px + pw - 14, iy - 1, currentTheme.borderMuted);
            iy += 3;
            g.drawString(this.font, trimText(this.font, "Крылья: " + WINGS_OPTIONS.get(wingsIndex), maxW), px + 14, iy, 0xFFE2E8F0);
            iy += 12;
            g.drawString(this.font, trimText(this.font, "Рога: " + HORNS_OPTIONS.get(hornsIndex), maxW), px + 14, iy, 0xFFE2E8F0);
            iy += 12;
            g.drawString(this.font, trimText(this.font, "Шляпка: " + HATS_OPTIONS.get(hatsIndex), maxW), px + 14, iy, 0xFFE2E8F0);
            iy += 12;
            g.drawString(this.font, trimText(this.font, "Одежда: " + OUTFIT_OPTIONS.get(outfitIndex), maxW), px + 14, iy, 0xFFE2E8F0);
            iy += 12;
            g.drawString(this.font, trimText(this.font, "Прицел: " + CROSSHAIR_OPTIONS.get(crosshairIndex), maxW), px + 14, iy, currentTheme.secondary);
        }

        // =====================================================================
        // 3 ИГРАБЕЛЬНЫЕ AFK-МИНИ-ИГРЫ В 8-Й ВКЛАДКЕ
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
