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
import java.util.Comparator;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AssistantVadimkaClient implements ClientModInitializer {
    public static final String MOD_ID = "assistant_vadimka";
    public static final String VERSION_LABEL = "v3.1";

    // =========================================================================
    // ПЕРЕЧИСЛЕНИЯ МОДЕЛЕЙ, МИНЬОНОВ, ВКЛАДОК, ТЕМ И AFK-ИГР
    // =========================================================================

    public enum VadimkaForm {
        RAT("Rat (Крыска)", "Крыска", EntityType.SILVERFISH, 1.05, 0xFF94A3B8, 0xFFF472B6),
        ALLAY("Allay (Спешик)", "Спешик", EntityType.ALLAY, 0.86, 0xFF22D3EE, 0xFFA5F3FC),
        BREEZE("Breeze (Вихрь)", "Вихрь", EntityType.BREEZE, 0.56, 0xFF818CF8, 0xFFC7D2FE),
        WARDEN("Warden (Хранитель)", "Warden", EntityType.WARDEN, 0.34, 0xFF0D9488, 0xFF2DD4BF),
        AXOLOTL("Blue Axolotl (Аксолотль)", "Аксолотль", EntityType.AXOLOTL, 0.92, 0xFF60A5FA, 0xFFF472B6),
        PLAYER("Player (Мини-игрок)", "Игрок", EntityType.ALLAY, 0.78, 0xFF38BDF8, 0xFFFDE68A);

        public final String title;
        public final String shortName;
        public final EntityType<? extends Mob> entityType;
        public final double baseWorldScale;
        public final int primaryColor;
        public final int accentColor;

        VadimkaForm(String title, String shortName, EntityType<? extends Mob> entityType,
                    double baseWorldScale, int primaryColor, int accentColor) {
            this.title = title;
            this.shortName = shortName;
            this.entityType = entityType;
            this.baseWorldScale = baseWorldScale;
            this.primaryColor = primaryColor;
            this.accentColor = accentColor;
        }
    }

    public enum MinionRole {
        COLLECTOR("Сборщик", "§a✦ Сборщик", 0xFF4ADE80),
        BUILDER("Строитель", "§e✦ Строитель", 0xFFFACC15),
        SCOUT("Разведчик", "§b✦ Разведчик", 0xFF38BDF8),
        SUPPLIER("Снабженец", "§d✦ Снабженец", 0xFFF472B6);

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
        VADIMKA("Вадимка", "Спутник Вадимка и Миньоны"),
        AUTOPILOT("Автопилот", "Автопилот и профайлер стиля"),
        AUTO_BUILD("Постройка", "Авто-постройка, линейка и база"),
        LITEMATICA_HUD("Litematica", "Litematica HUD и материалы"),
        VISUAL_RADAR("Радар", "Мини-карта с рельефом и разведка"),
        CHESTS_VILLAGERS("База и Лут", "Сундуки, дроп и жители"),
        COMBAT_PVP("Бой и PvP", "Боевой HUD, тотемы и смерть"),
        MUSIC_AFK("Музыка / AFK", "Spotify HUD и 3 MC мини-игры"),
        CUSTOM_VISUAL("Кастом", "3D-гардероб, крылья и темы [P]");

        public final String shortTitle;
        public final String fullTitle;

        MenuTab(String shortTitle, String fullTitle) {
            this.shortTitle = shortTitle;
            this.fullTitle = fullTitle;
        }
    }

    public enum MenuTheme {
        CYBER_EMERALD("Cyber Emerald", 0xF8090E14, 0xFF0E1621, 0xFF142130, 0xFF10B981, 0xFF38BDF8, 0xFF1F3448),
        NETHER_CRIMSON("Nether Crimson", 0xF814090D, 0xFF200E15, 0xFF2C141E, 0xFFF43F5E, 0xFFFBBF24, 0xFF4A1F2E),
        AMETHYST_VOID("Amethyst Void", 0xF80F0A1A, 0xFF19112B, 0xFF251940, 0xFFA855F7, 0xFFF472B6, 0xFF3E2863),
        ARCTIC_FROST("Arctic Frost", 0xF809111B, 0xFF101C2B, 0xFF182A40, 0xFF38BDF8, 0xFF7DD3FC, 0xFF294666),
        OBSIDIAN_GOLD("Obsidian Gold", 0xF80E0D0A, 0xFF181611, 0xFF262219, 0xFFF59E0B, 0xFFFDE047, 0xFF4A3F26);

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
    public static int companionScalePercent = 100; // 60..140
    public static boolean quietMode = false;
    public static boolean randomChaosMode = false;
    public static boolean voiceCommentator = true;
    public static final Map<MinionRole, Boolean> activeMinions = new EnumMap<>(MinionRole.class);

    // 2. Автопилот и профайлер
    public static boolean styleProfiler = true;
    public static boolean safeAutopilot = false;
    public static boolean autoReturnControl = true;
    public static boolean parkourEdgeGuard = true;
    public static boolean fallDangerAlert = true;
    public static boolean sprintAssist = false;

    // 3. Постройка
    public static boolean litematicaAutoBuild = false;
    public static boolean smartRestoration = true;
    public static boolean hotbarAutoRefill = true;
    public static boolean redstoneInspector = true;
    public static boolean palettePreview = true;
    public static boolean rulerOverlay = true;

    // 4. Litematica HUD
    public static boolean litematicaHudEnabled = true;
    public static boolean shulkerScanner = true;
    public static boolean layerSliceMode = false;
    public static boolean deficitHighlight = true;
    public static int targetBuildLayerY = 64;

    // 5. Радар и разведка
    public static boolean radarHudEnabled = true;
    public static boolean radarTerrainMode = true;
    public static boolean radarRightSide = false;
    public static int radarZoomLevel = 1; // 0 = близко, 1 = средне, 2 = далеко
    public static boolean soundRadarEnabled = true;
    public static boolean ariadneThread = true;
    public static boolean sixthSense = true;

    // 6. База и лут
    public static boolean chestMemory = true;
    public static boolean smartSorter = false;
    public static boolean radarShowChests = true;
    public static boolean radarShowDrops = true;
    public static boolean villagerTracker = true;

    // 7. Бой и PvP
    public static boolean combatHudEnabled = true;
    public static boolean trajectoriesEnabled = true;
    public static boolean totemScanner = true;
    public static boolean armorDurabilityHud = true;
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
    public static MenuTheme currentTheme = MenuTheme.CYBER_EMERALD;
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
    // ВНУТРЕННИЕ ДАННЫЕ МИРА, КАРТЫ РЕЛЬЕФА, СУНДУКОВ И ПРОФАЙЛЕРА
    // =========================================================================

    private static KeyMapping openMenuKey;
    private static Mob companionEntity = null;
    private static VadimkaForm spawnedForm = null;
    private static int spawnedScalePercent = -1;
    private static final Map<MinionRole, Mob> minionEntities = new EnumMap<>(MinionRole.class);
    private static int worldTick = 0;

    // Кэш реальной карты местности и сундуков
    private static final int MAP_GRID = 44;
    private static final int[] terrainColors = new int[MAP_GRID * MAP_GRID];
    private static final List<BlockPos> nearbyChests = new ArrayList<>();
    private static int cachedMapCenterX = Integer.MIN_VALUE;
    private static int cachedMapCenterZ = Integer.MIN_VALUE;
    private static int cachedMapZoom = -1;
    private static int mapRefreshTicks = 0;
    private static int chestScanTicks = 0;

    // Нить Ариадны
    private static final Deque<Vec3> breadcrumbs = new ArrayDeque<>();
    private static Vec3 lastBreadcrumb = null;

    // Профайлер стиля и статистика
    private static double sessionDistanceWalked = 0.0;
    private static Vec3 lastPlayerPos = null;
    private static int fallDropBlocks = 0;
    private static BlockPos lastDeathPos = null;

    // Угрозы и реплики Вадимки
    private static boolean threatBehindPlayer = false;
    private static double nearestHostileDist = 999.0;
    private static double nearestHostileAngle = 0.0;
    private static String vadimkaToastText = "";
    private static int vadimkaToastTicks = 0;
    private static float lastPlayerHealth = 20.0F;
    private static final Random worldRandom = new Random();

    private static final List<String> CHAOS_QUOTES = List.of(
        "Писк! Пора копать алмазы на Y = -58!",
        "Миньоны на позициях, ждём команду босса!",
        "Осторожно, криперы любят тишину за спиной...",
        "Крысиная разведка докладывает: горизонт чист!",
        "Загляни в меню [P] — примерь новые 3D-крылья!",
        "Сундуки вокруг просканированы и отмечены на карте!"
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
                nearbyChests.clear();
                lastBreadcrumb = null;
                lastPlayerPos = null;
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
    }

    // =========================================================================
    // ИГРОВОЙ ТИК: 3D-СПУТНИК, МИНЬОНЫ, ЧАСТИЦЫ, НИТЬ АРИАДНЫ, УГРОЗЫ, КАРТА
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
            pushVadimkaToast("Вадимка: Точка смерти сохранена (" + lastDeathPos.getX() + ", " + lastDeathPos.getY() + ", " + lastDeathPos.getZ() + ")", 120);
        } else if (hp < lastPlayerHealth - 0.1F) {
            if (voiceCommentator) {
                pushVadimkaToast("Вадимка: Урон! Осталось HP: " + (int) Math.ceil(hp), 70);
            }
            if (afkPanicExit && mc.screen instanceof AssistantVadimkaScreen scr && scr.afkGameRunning) {
                scr.afkGameRunning = false;
                mc.setScreen(null);
                pushVadimkaToast("Вадимка: ЭКСТРЕННЫЙ ВЫХОД ИЗ AFK!", 90);
            }
        }
        lastPlayerHealth = hp;

        // Предупреждение о падении с опасной высоты
        if (fallDangerAlert && worldTick % 5 == 0) {
            fallDropBlocks = measureDropBelow(level, player.blockPosition());
        }

        // Случайные реплики
        if (randomChaosMode && !quietMode && worldTick % 560 == 0) {
            pushVadimkaToast("Вадимка: " + CHAOS_QUOTES.get(worldRandom.nextInt(CHAOS_QUOTES.size())), 90);
        }

        // 1. 3D-спутник Вадимка и 4 миньона в мире
        updateCompanionAndMinions(level, player);

        // 2. Видимые 3D-крылья и нимб/рога на игроке в мире (F5)
        if (worldCosmeticsParticles && worldTick % 2 == 0) {
            spawnPlayerCosmeticParticles(level, player);
        }

        // 3. Нить Ариадны (маршрут обратно)
        if (ariadneThread) {
            Vec3 crumbPos = new Vec3(player.getX(), player.getY() + 0.22, player.getZ());
            if (lastBreadcrumb == null || lastBreadcrumb.distanceToSqr(crumbPos) > 9.0) {
                breadcrumbs.addLast(crumbPos);
                lastBreadcrumb = crumbPos;
                while (breadcrumbs.size() > 40) {
                    breadcrumbs.removeFirst();
                }
            }
            if (worldTick % 8 == 0) {
                int idx = 0;
                for (Vec3 c : breadcrumbs) {
                    if ((idx + worldTick / 8) % 3 == 0 && c.distanceToSqr(crumbPos) < 400.0) {
                        level.addParticle(ParticleTypes.ENCHANT, c.x, c.y + 0.15, c.z, 0.0, 0.02, 0.0);
                    }
                    idx++;
                }
            }
        }

        // 4. Шестое чувство и сканер врагов
        scanThreats(level, player);
        if (afkPanicExit && nearestHostileDist < 4.5 && mc.screen instanceof AssistantVadimkaScreen scr && scr.afkGameRunning) {
            scr.afkGameRunning = false;
            mc.setScreen(null);
            pushVadimkaToast("Вадимка: Моб рядом! AFK-игра закрыта!", 90);
        }

        // 5. Кэш рельефа для мини-карты
        if (radarHudEnabled && radarTerrainMode) {
            mapRefreshTicks++;
            int px = Mth.floor(player.getX());
            int pz = Mth.floor(player.getZ());
            if (mapRefreshTicks >= 6
                    || Math.abs(px - cachedMapCenterX) >= 2
                    || Math.abs(pz - cachedMapCenterZ) >= 2
                    || cachedMapZoom != radarZoomLevel) {
                refreshTerrainMapCache(level, px, pz);
                mapRefreshTicks = 0;
            }
        }

        // 6. Периодический поиск сундуков/бочек вокруг для карты и памяти сундуков
        if (radarHudEnabled && (radarShowChests || chestMemory)) {
            chestScanTicks++;
            if (chestScanTicks >= 35) {
                scanNearbyChests(level, player.blockPosition());
                chestScanTicks = 0;
            }
        }
    }

    private static int measureDropBelow(ClientLevel level, BlockPos start) {
        if (!level.getBlockState(start.below()).isAir()) {
            return 0;
        }
        int drop = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(start.getX(), start.getY() - 1, start.getZ());
        for (int i = 0; i < 20; i++) {
            BlockState st = level.getBlockState(pos);
            if (!st.isAir() && !st.is(Blocks.WATER)) {
                break;
            }
            if (st.is(Blocks.WATER)) {
                return 0;
            }
            drop++;
            pos.setY(pos.getY() - 1);
        }
        return drop;
    }

    private static void scanNearbyChests(ClientLevel level, BlockPos center) {
        nearbyChests.clear();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int r = 18;
        int minY = Math.max(level.getMinY(), center.getY() - 6);
        int maxY = Math.min(level.getMaxY() - 1, center.getY() + 6);
        for (int dx = -r; dx <= r; dx += 1) {
            for (int dz = -r; dz <= r; dz += 1) {
                for (int y = minY; y <= maxY; y++) {
                    pos.set(center.getX() + dx, y, center.getZ() + dz);
                    BlockState st = level.getBlockState(pos);
                    if (st.is(Blocks.CHEST) || st.is(Blocks.TRAPPED_CHEST)
                            || st.is(Blocks.BARREL) || st.is(Blocks.ENDER_CHEST)) {
                        nearbyChests.add(pos.immutable());
                        if (nearbyChests.size() >= 32) {
                            return;
                        }
                    }
                }
            }
        }
    }

    private static void updateCompanionAndMinions(ClientLevel level, Player player) {
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
                    created.setCustomName(Component.literal("§b✦ Вадимка §7[" + currentForm.shortName + "]"));
                    created.setCustomNameVisible(true);
                    applyEntityScale(created, currentForm.baseWorldScale * (companionScalePercent / 100.0));
                    created.setPos(player.getX() + 1.1, player.getY() + 1.0, player.getZ() + 1.1);
                    level.addEntity(created);
                    companionEntity = created;
                    spawnedForm = currentForm;
                    spawnedScalePercent = companionScalePercent;
                    pushVadimkaToast("Вадимка переключился на облик: " + currentForm.shortName, 60);
                }
            } else if (spawnedScalePercent != companionScalePercent) {
                applyEntityScale(companionEntity, currentForm.baseWorldScale * (companionScalePercent / 100.0));
                spawnedScalePercent = companionScalePercent;
            }

            if (companionEntity != null) {
                double yawRad = Math.toRadians(player.getYRot());
                double sideOffset = 1.18;
                double backOffset = 0.72;
                double targetX = player.getX() - Math.cos(yawRad) * sideOffset + Math.sin(yawRad) * backOffset;
                double targetZ = player.getZ() - Math.sin(yawRad) * sideOffset - Math.cos(yawRad) * backOffset;
                double baseHeight = (currentForm == VadimkaForm.RAT || currentForm == VadimkaForm.AXOLOTL) ? 0.14 : 1.05;
                double bob = (currentForm == VadimkaForm.RAT)
                    ? Math.abs(Math.sin(worldTick * 0.24)) * 0.08
                    : Math.sin(worldTick * 0.12) * 0.12;
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

                if (worldTick % 14 == 0) {
                    level.addParticle(ParticleTypes.END_ROD, nextX, nextY + 0.35, nextZ, 0.0, 0.015, 0.0);
                }
            }
        }

        // Обновление 4 миньонов
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
                    minion.setCustomName(Component.literal(role.tag));
                    minion.setCustomNameVisible(true);
                    applyEntityScale(minion, 0.62);
                    minion.setPos(player.getX(), player.getY() + 1.5, player.getZ());
                    level.addEntity(minion);
                    minionEntities.put(role, minion);
                    existing = minion;
                }
            }

            if (existing != null) {
                double angle = (worldTick * 0.038) + (i * (Math.PI / 2.0));
                double radius = 2.15;
                double tx = player.getX() + Math.cos(angle) * radius;
                double tz = player.getZ() + Math.sin(angle) * radius;
                double ty = player.getY() + 1.48 + Math.sin(worldTick * 0.11 + i * 1.4) * 0.18;

                existing.xo = existing.getX();
                existing.yo = existing.getY();
                existing.zo = existing.getZ();
                existing.setPos(
                    Mth.lerp(0.18, existing.getX(), tx),
                    Mth.lerp(0.18, existing.getY(), ty),
                    Mth.lerp(0.18, existing.getZ(), tz)
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

        // 3D-крылья за спиной игрока
        if (wingsIndex > 0) {
            var wingParticle = switch (wingsIndex) {
                case 1 -> ParticleTypes.SNOWFLAKE;
                case 2 -> ParticleTypes.FLAME;
                case 3 -> ParticleTypes.SOUL_FIRE_FLAME;
                default -> ParticleTypes.ELECTRIC_SPARK;
            };
            double flap = Math.sin(worldTick * 0.22) * 0.14;
            for (int side = -1; side <= 1; side += 2) {
                for (int step = 1; step <= 3; step++) {
                    double spread = 0.28 * step;
                    double up = 1.05 + (step == 2 ? 0.28 : (step == 3 ? 0.15 : 0.05)) + flap * step;
                    double back = 0.34 + 0.12 * step;
                    double px = player.getX() + backX * back + rightX * (side * spread);
                    double py = player.getY() + up;
                    double pz = player.getZ() + backZ * back + rightZ * (side * spread);
                    if ((worldTick + step) % 4 == 0) {
                        level.addParticle(wingParticle, px, py, pz, 0.0, 0.0, 0.0);
                    }
                }
            }
        }

        // Рога / Золотой нимб над головой
        if (hornsIndex == 4 && worldTick % 4 == 0) {
            double ang = (worldTick * 0.35) % (Math.PI * 2.0);
            double hx = player.getX() + Math.cos(ang) * 0.35;
            double hz = player.getZ() + Math.sin(ang) * 0.35;
            level.addParticle(ParticleTypes.END_ROD, hx, player.getY() + 2.15, hz, 0.0, 0.0, 0.0);
        } else if (hornsIndex > 0 && worldTick % 6 == 0) {
            var hornP = hornsIndex == 1 ? ParticleTypes.FLAME : ParticleTypes.ELECTRIC_SPARK;
            for (int side = -1; side <= 1; side += 2) {
                double hx = player.getX() + rightX * (side * 0.24);
                double hz = player.getZ() + rightZ * (side * 0.24);
                level.addParticle(hornP, hx, player.getY() + 2.02, hz, 0.0, 0.01, 0.0);
            }
        }
    }

    private static void scanThreats(ClientLevel level, Player player) {
        threatBehindPlayer = false;
        nearestHostileDist = 999.0;
        double lookX = -Math.sin(Math.toRadians(player.getYRot()));
        double lookZ = Math.cos(Math.toRadians(player.getYRot()));

        for (Entity entity : level.entitiesForRendering()) {
            if (!(entity instanceof Monster) || entity.getId() <= -77000) {
                continue;
            }
            double dx = entity.getX() - player.getX();
            double dz = entity.getZ() - player.getZ();
            double dy = Math.abs(entity.getY() - player.getY());
            if (dy > 8.0) {
                continue;
            }
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist < nearestHostileDist) {
                nearestHostileDist = dist;
                nearestHostileAngle = Math.atan2(dz, dx);
            }
            if (dist < 10.0 && dist > 0.1) {
                double dot = (dx / dist) * lookX + (dz / dist) * lookZ;
                if (dot < -0.25) {
                    threatBehindPlayer = true;
                }
            }
        }
    }

    private static void refreshTerrainMapCache(ClientLevel level, int centerX, int centerZ) {
        cachedMapCenterX = centerX;
        cachedMapCenterZ = centerZ;
        cachedMapZoom = radarZoomLevel;

        int stepBlocks = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        int half = MAP_GRID / 2;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int gz = 0; gz < MAP_GRID; gz++) {
            int wz = centerZ + (gz - half) * stepBlocks;
            for (int gx = 0; gx < MAP_GRID; gx++) {
                int wx = centerX + (gx - half) * stepBlocks;
                int topY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                int rgb = 0x16202B;
                for (int checkY = topY; checkY >= topY - 4; checkY--) {
                    pos.set(wx, checkY, wz);
                    BlockState state = level.getBlockState(pos);
                    MapColor mapColor = state.getMapColor(level, pos);
                    if (mapColor.col != 0) {
                        rgb = mapColor.col;
                        int northY = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz - stepBlocks) - 1;
                        int shade = 0;
                        if (checkY > northY) {
                            shade = 18;
                        } else if (checkY < northY) {
                            shade = -20;
                        }
                        rgb = adjustBrightness(rgb, shade);
                        break;
                    }
                }
                terrainColors[gz * MAP_GRID + gx] = 0xFF000000 | rgb;
            }
        }
    }

    // =========================================================================
    // ИГРОВОЙ HUD: МИНИ-КАРТА, ПРИЦЕЛ, PVP, LITEMATICA, SPOTIFY, ЛИНЕЙКА
    // =========================================================================

    private static void renderAllHudWidgets(GuiGraphics graphics) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) {
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
            int ww = mc.font.width(warn) + 16;
            int wx = (sw - ww) / 2;
            int wy = 14;
            fillRoundedRect(graphics, wx, wy, ww, 18, 0xE63B0A12);
            drawRoundedBorder(graphics, wx, wy, ww, 18, 0xFFEF4444);
            graphics.drawString(mc.font, warn, wx + 8, wy + 5, 0xFFFCA5A5);
        } else if (fallDangerAlert && fallDropBlocks >= 6 && mc.screen == null) {
            String warn = "⚠ ОБРЫВ ВНИЗУ: " + fallDropBlocks + " бл.";
            int ww = mc.font.width(warn) + 16;
            int wx = (sw - ww) / 2;
            int wy = 14;
            fillRoundedRect(graphics, wx, wy, ww, 18, 0xE63B2508);
            drawRoundedBorder(graphics, wx, wy, ww, 18, 0xFFF59E0B);
            graphics.drawString(mc.font, warn, wx + 8, wy + 5, 0xFFFDE68A);
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
        int mapSize = 106;
        int pad = 8;
        int x = radarRightSide ? (screenW - mapSize - pad) : pad;
        int y = 32;
        int cx = x + mapSize / 2;
        int cy = y + mapSize / 2;

        // Внешний корпус мини-карты
        fillRoundedRect(graphics, x - 2, y - 2, mapSize + 4, mapSize + 4, currentTheme.bgWindow);
        drawRoundedBorder(graphics, x - 2, y - 2, mapSize + 4, mapSize + 4, currentTheme.accent);

        double yawRad = Math.toRadians(mc.player.getYRot() + 180.0);
        double cos = Math.cos(yawRad);
        double sin = Math.sin(yawRad);
        int stepBlocks = radarZoomLevel == 0 ? 1 : (radarZoomLevel == 1 ? 2 : 3);
        int halfGrid = MAP_GRID / 2;

        if (radarTerrainMode) {
            int cell = 2;
            int count = mapSize / cell;
            for (int py = 0; py < count; py++) {
                double relY = (py - count / 2.0) * 0.9;
                for (int px = 0; px < count; px++) {
                    double relX = (px - count / 2.0) * 0.9;
                    double worldDx = relX * cos + relY * sin;
                    double worldDz = -relX * sin + relY * cos;
                    int gx = halfGrid + (int) Math.round(worldDx);
                    int gz = halfGrid + (int) Math.round(worldDz);
                    int col = 0xFF0E1520;
                    if (gx >= 0 && gx < MAP_GRID && gz >= 0 && gz < MAP_GRID) {
                        col = terrainColors[gz * MAP_GRID + gx];
                    }
                    graphics.fill(x + px * cell, y + py * cell, x + px * cell + cell, y + py * cell + cell, col);
                }
            }
            graphics.fill(x, y, x + mapSize, y + mapSize, 0x24080D14);
        } else {
            graphics.fill(x, y, x + mapSize, y + mapSize, 0xF00A1018);
        }

        // Тонкая сетка радара
        graphics.fill(cx, y + 3, cx + 1, y + mapSize - 3, 0x2CFFFFFF);
        graphics.fill(x + 3, cy, x + mapSize - 3, cy + 1, 0x2CFFFFFF);

        double mapScale = 1.0 / (stepBlocks * 0.52);
        int maxOff = (mapSize / 2) - 5;

        // Звуковой радар: маркер направления ближайшего врага
        if (soundRadarEnabled && nearestHostileDist < 22.0) {
            double hDx = Math.cos(nearestHostileAngle) * 20.0;
            double hDz = Math.sin(nearestHostileAngle) * 20.0;
            double rX = hDx * cos - hDz * sin;
            double rY = hDx * sin + hDz * cos;
            double len = Math.max(1.0, Math.sqrt(rX * rX + rY * rY));
            int ax = cx + (int) ((rX / len) * (maxOff + 2));
            int ay = cy + (int) ((rY / len) * (maxOff + 2));
            graphics.fill(ax - 3, ay - 3, ax + 4, ay + 4, 0xBBEF4444);
        }

        // Сундуки на мини-карте
        if (radarShowChests) {
            for (BlockPos chestPos : nearbyChests) {
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

        // Сущности вокруг игрока
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity == mc.player || entity.getId() <= -77000) {
                continue;
            }
            double dx = entity.getX() - mc.player.getX();
            double dz = entity.getZ() - mc.player.getZ();
            double rotX = (dx * cos - dz * sin) * mapScale;
            double rotY = (dx * sin + dz * cos) * mapScale;
            if (Math.abs(rotX) > maxOff || Math.abs(rotY) > maxOff) {
                continue;
            }
            int dotX = cx + (int) rotX;
            int dotY = cy + (int) rotY;

            if (entity instanceof Player otherPlayer) {
                graphics.fill(dotX - 2, dotY - 2, dotX + 3, dotY + 3, 0xFF000000);
                graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFF38BDF8);
                String nick = trimText(mc.font, otherPlayer.getName().getString(), 48);
                graphics.drawString(mc.font, nick, dotX + 4, dotY - 3, 0xFFE0F2FE);
            } else if (villagerTracker && entity.getType() == EntityType.VILLAGER) {
                graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFFC084FC);
            } else if (entity instanceof Monster) {
                graphics.fill(dotX - 2, dotY - 2, dotX + 2, dotY + 2, 0xAA000000);
                graphics.fill(dotX - 1, dotY - 1, dotX + 1, dotY + 1, 0xFFEF4444);
            } else if (entity instanceof Animal) {
                graphics.fill(dotX - 1, dotY - 1, dotX + 2, dotY + 2, 0xFF4ADE80);
            } else if (radarShowDrops && entity instanceof ItemEntity) {
                graphics.fill(dotX - 1, dotY - 1, dotX + 1, dotY + 1, 0xFFFACC15);
            }
        }

        // Стороны света N / E / S / W
        drawCompassLetter(graphics, mc.font, "N", cx, cy, -sin, -cos, maxOff - 1, 0xFF38BDF8);
        drawCompassLetter(graphics, mc.font, "S", cx, cy, sin, cos, maxOff - 1, 0xFF94A3B8);
        drawCompassLetter(graphics, mc.font, "E", cx, cy, cos, -sin, maxOff - 1, 0xFF94A3B8);
        drawCompassLetter(graphics, mc.font, "W", cx, cy, -cos, sin, maxOff - 1, 0xFF94A3B8);

        // Стрелка игрока в центре
        graphics.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0xFF000000);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);
        graphics.fill(cx, cy - 3, cx + 1, cy - 1, currentTheme.accent);

        // Нижняя инфо-плашка (строго по ширине карты, текст гарантированно внутри!)
        int infoY = y + mapSize + 5;
        int infoW = mapSize + 4;
        int infoH = 30;
        fillRoundedRect(graphics, x - 2, infoY, infoW, infoH, currentTheme.bgWindow);
        drawRoundedBorder(graphics, x - 2, infoY, infoW, infoH, currentTheme.secondary);

        String coords = (int) mc.player.getX() + " " + (int) mc.player.getY() + " " + (int) mc.player.getZ();
        String dir = getCompassDirection(mc.player.getYRot());
        String line1 = trimText(mc.font, coords + " [" + dir + "]", infoW - 8);
        String line2 = trimText(mc.font, "✦ " + currentForm.shortName + (quietMode ? " (Тих)" : ""), infoW - 8);
        graphics.drawString(mc.font, line1, x + 2, infoY + 5, 0xFFF1F5F9);
        graphics.drawString(mc.font, line2, x + 2, infoY + 17, currentTheme.accent);
    }

    private static void drawCompassLetter(GuiGraphics g, Font font, String letter,
                                          int cx, int cy, double dx, double dy, int rad, int color) {
        int lx = cx + (int) (dx * rad) - 2;
        int ly = cy + (int) (dy * rad) - 3;
        g.fill(lx - 1, ly - 1, lx + 7, ly + 8, 0xAA080D14);
        g.drawString(font, letter, lx, ly, color);
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
        int y = sh / 2 + 16;
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
        int w = 118;
        int h = (armorDurabilityHud ? 44 : 22) + (showDeath ? 12 : 0);
        int x = sw - w - 8;
        int y = sh - h - 8;
        fillRoundedRect(g, x, y, w, h, currentTheme.bgWindow);
        drawRoundedBorder(g, x, y, w, h, currentTheme.borderMuted);

        String tText = "Тотемы: " + totems + " | HP: " + (int) Math.ceil(p.getHealth());
        g.drawString(mc.font, trimText(mc.font, tText, w - 10), x + 6, y + 6, totems > 0 ? 0xFFFDE047 : 0xFFE2E8F0);

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
                g.drawString(mc.font, labels[i], bx + i * 27, by, 0xFF94A3B8);
                g.fill(bx + i * 27, by + 11, bx + i * 27 + 22, by + 15, 0xFF1E293B);
                if (pct > 0) {
                    int fillW = Math.max(2, (22 * pct) / 100);
                    g.fill(bx + i * 27, by + 11, bx + i * 27 + fillW, by + 15, barCol);
                }
            }
        }

        if (showDeath) {
            int dDist = (int) Math.sqrt(p.blockPosition().distSqr(lastDeathPos));
            String dStr = "☠ Смерть: " + dDist + "м";
            g.drawString(mc.font, trimText(mc.font, dStr, w - 10), x + 6, y + h - 12, 0xFFFCA5A5);
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
        int x = radarRightSide ? 8 : (sw - w - 8);
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
        int x = 8;
        int y = sh - h - 8;
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
    // КАСТОМНАЯ ГРАФИКА И УТИЛИТЫ (100% БЕЗ СЕРЫХ КНОПОК MINECRAFT)
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

    private static void fillVerticalGradient(GuiGraphics g, int x, int y, int w, int h, int topColor, int bottomColor) {
        int steps = Math.max(1, Math.min(h, 16));
        for (int i = 0; i < steps; i++) {
            int y1 = y + (i * h) / steps;
            int y2 = y + ((i + 1) * h) / steps;
            float t = (float) i / Math.max(1, steps - 1);
            g.fill(x, y1, x + w, y2, lerpColor(topColor, bottomColor, t));
        }
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

    private static int shadeArgb(int argb, float factor) {
        int a = (argb >>> 24) & 0xFF;
        int r = Mth.clamp((int) (((argb >>> 16) & 0xFF) * factor), 0, 255);
        int g = Mth.clamp((int) (((argb >>> 8) & 0xFF) * factor), 0, 255);
        int b = Mth.clamp((int) ((argb & 0xFF) * factor), 0, 255);
        return (a << 24) | (r << 16) | (g << 8) | b;
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

    // =========================================================================
    // ВСТРОЕННЫЙ 3D-РЕНДЕРЕР ДЛЯ ЖИВОЙ 3D-СТУДИИ СПРАВА В МЕНЮ
    // =========================================================================

    private static class VoxelBox {
        final double cx;
        final double cy;
        final double cz;
        final double sx;
        final double sy;
        final double sz;
        final int color;

        VoxelBox(double cx, double cy, double cz, double sx, double sy, double sz, int color) {
            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.color = color;
        }
    }

    private static class ProjectedFace {
        final int[] xs;
        final int[] ys;
        final double depth;
        final int color;

        ProjectedFace(int[] xs, int[] ys, double depth, int color) {
            this.xs = xs;
            this.ys = ys;
            this.depth = depth;
            this.color = color;
        }
    }

    private static void render3DVoxelScene(GuiGraphics g, List<VoxelBox> boxes,
                                           int vpX, int vpY, int vpW, int vpH,
                                           double yawRad, double pitchRad, double scale) {
        double cosY = Math.cos(yawRad);
        double sinY = Math.sin(yawRad);
        double cosP = Math.cos(pitchRad);
        double sinP = Math.sin(pitchRad);
        int centerX = vpX + vpW / 2;
        int centerY = vpY + vpH / 2 + 24;

        int[][] faces = {
            {0, 1, 2, 3},
            {5, 4, 7, 6},
            {4, 0, 3, 7},
            {1, 5, 6, 2},
            {3, 2, 6, 7},
            {4, 5, 1, 0}
        };
        float[] faceLight = {1.05F, 0.72F, 0.82F, 0.90F, 1.15F, 0.58F};

        List<ProjectedFace> drawList = new ArrayList<>(boxes.size() * 3);

        for (VoxelBox b : boxes) {
            double hx = b.sx * 0.5;
            double hy = b.sy * 0.5;
            double hz = b.sz * 0.5;
            double[][] verts = {
                {b.cx - hx, b.cy - hy, b.cz - hz},
                {b.cx + hx, b.cy - hy, b.cz - hz},
                {b.cx + hx, b.cy + hy, b.cz - hz},
                {b.cx - hx, b.cy + hy, b.cz - hz},
                {b.cx - hx, b.cy - hy, b.cz + hz},
                {b.cx + hx, b.cy - hy, b.cz + hz},
                {b.cx + hx, b.cy + hy, b.cz + hz},
                {b.cx - hx, b.cy + hy, b.cz + hz}
            };

            double[] px = new double[8];
            double[] py = new double[8];
            double[] pz = new double[8];

            for (int i = 0; i < 8; i++) {
                double x = verts[i][0];
                double y = verts[i][1];
                double z = verts[i][2];

                double rx = x * cosY - z * sinY;
                double rz = x * sinY + z * cosY;

                double ry = y * cosP - rz * sinP;
                double rz2 = y * sinP + rz * cosP;

                double persp = 9.0 / (9.0 + rz2 * 0.65);
                px[i] = centerX + rx * scale * persp;
                py[i] = centerY - ry * scale * persp;
                pz[i] = rz2;
            }

            for (int f = 0; f < 6; f++) {
                int i0 = faces[f][0];
                int i1 = faces[f][1];
                int i2 = faces[f][2];
                int i3 = faces[f][3];

                double cross = (px[i1] - px[i0]) * (py[i2] - py[i0]) - (py[i1] - py[i0]) * (px[i2] - px[i0]);
                if (cross >= 0) {
                    continue;
                }
                double avgZ = (pz[i0] + pz[i1] + pz[i2] + pz[i3]) * 0.25;
                int[] xs = {
                    (int) Math.round(px[i0]),
                    (int) Math.round(px[i1]),
                    (int) Math.round(px[i2]),
                    (int) Math.round(px[i3])
                };
                int[] ys = {
                    (int) Math.round(py[i0]),
                    (int) Math.round(py[i1]),
                    (int) Math.round(py[i2]),
                    (int) Math.round(py[i3])
                };
                drawList.add(new ProjectedFace(xs, ys, avgZ, shadeArgb(b.color, faceLight[f])));
            }
        }

        drawList.sort(Comparator.comparingDouble((ProjectedFace pf) -> pf.depth).reversed());

        for (ProjectedFace pf : drawList) {
            fillTriangle2px(g, pf.xs[0], pf.ys[0], pf.xs[1], pf.ys[1], pf.xs[2], pf.ys[2], pf.color, vpX, vpY, vpW, vpH);
            fillTriangle2px(g, pf.xs[0], pf.ys[0], pf.xs[2], pf.ys[2], pf.xs[3], pf.ys[3], pf.color, vpX, vpY, vpW, vpH);
        }
    }

    private static void fillTriangle2px(GuiGraphics g, int x0, int y0, int x1, int y1, int x2, int y2,
                                        int color, int vpX, int vpY, int vpW, int vpH) {
        if (y0 > y1) {
            int tx = x0;
            int ty = y0;
            x0 = x1;
            y0 = y1;
            x1 = tx;
            y1 = ty;
        }
        if (y0 > y2) {
            int tx = x0;
            int ty = y0;
            x0 = x2;
            y0 = y2;
            x2 = tx;
            y2 = ty;
        }
        if (y1 > y2) {
            int tx = x1;
            int ty = y1;
            x1 = x2;
            y1 = y2;
            x2 = tx;
            y2 = ty;
        }

        int totalH = y2 - y0;
        if (totalH <= 0) {
            return;
        }

        int minY = Math.max(y0, vpY);
        int maxY = Math.min(y2, vpY + vpH - 1);
        int minXBound = vpX;
        int maxXBound = vpX + vpW;

        for (int y = minY; y <= maxY; y += 2) {
            boolean secondHalf = y > y1 || y1 == y0;
            int segH = secondHalf ? (y2 - y1) : (y1 - y0);
            if (segH == 0) {
                continue;
            }

            float alpha = (float) (y - y0) / totalH;
            float beta = (float) (y - (secondHalf ? y1 : y0)) / segH;
            int ax = x0 + (int) ((x2 - x0) * alpha);
            int bx = secondHalf
                ? (x1 + (int) ((x2 - x1) * beta))
                : (x0 + (int) ((x1 - x0) * beta));

            if (ax > bx) {
                int tmp = ax;
                ax = bx;
                bx = tmp;
            }

            int sx = Math.max(minXBound, ax);
            int ex = Math.min(maxXBound, bx + 1);
            int ey = Math.min(vpY + vpH, y + 2);
            if (ex > sx && ey > y) {
                g.fill(sx, y, ex, ey, color);
            }
        }
    }

    // =========================================================================
    // ГЛАВНОЕ КАСТОМНОЕ МЕНЮ НА КЛАВИШУ [P]
    // =========================================================================

    public static class AssistantVadimkaScreen extends Screen {
        private MenuTab activeTab;
        private int currentPage = 0;
        private int totalPages = 1;

        // 3D-превью параметры
        private double previewYaw = -0.45;
        private boolean previewAutoRotate = true;

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

            this.winW = Math.min(this.width - 16, 616);
            this.winH = Math.min(this.height - 16, 338);
            this.winX = (this.width - this.winW) / 2;
            this.winY = (this.height - this.winH) / 2;

            this.sidebarW = 114;
            this.previewW = 168;
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

            // Кнопки управления 3D-превью справа
            int prevX = this.winX + this.winW - this.previewW - 6;
            int prevCtrlY = this.winY + 178;
            int btn3W = (this.previewW - 24) / 3;
            addSmallButton(prevX + 8, prevCtrlY, btn3W, 17, "◀", false, false, () -> {
                previewAutoRotate = false;
                previewYaw -= 0.45;
            });
            addSmallButton(prevX + 12 + btn3W, prevCtrlY, btn3W, 17, previewAutoRotate ? "⏸" : "⟳", false, previewAutoRotate, () -> {
                previewAutoRotate = !previewAutoRotate;
                this.rebuildWidgets();
            });
            addSmallButton(prevX + 16 + btn3W * 2, prevCtrlY, btn3W, 17, "▶", false, false, () -> {
                previewAutoRotate = false;
                previewYaw += 0.45;
            });

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
                int btnW = spec.isToggle ? 46 : 88;
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
                    list.add(cycleSpec("Облик Вадимки", "6 объёмных 3D-моделей", currentForm.shortName, () -> {
                        VadimkaForm[] f = VadimkaForm.values();
                        currentForm = f[(currentForm.ordinal() + 1) % f.length];
                    }));
                    list.add(cycleSpec("Размер в мире", "Масштаб 3D-модели Вадимки", companionScalePercent + "%", () -> {
                        companionScalePercent = companionScalePercent >= 140 ? 60 : companionScalePercent + 20;
                    }));
                    list.add(toggleSpec("Команда миньонов (4)", "Летающие помощники вокруг тебя", minionsVisibleInWorld, () -> minionsVisibleInWorld = !minionsVisibleInWorld));
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
                    list.add(toggleSpec("Безопасный автопилот", "Ведение с учетом правил сервера", safeAutopilot, () -> safeAutopilot = !safeAutopilot));
                    list.add(toggleSpec("Мгновенный возврат", "Передача управления по клавише", autoReturnControl, () -> autoReturnControl = !autoReturnControl));
                    list.add(toggleSpec("Паркур-страховка края", "Контроль обрыва под ногами", parkourEdgeGuard, () -> parkourEdgeGuard = !parkourEdgeGuard));
                    list.add(toggleSpec("Сигнал обрыва (HUD)", "Предупреждение при высоте >= 6", fallDangerAlert, () -> fallDangerAlert = !fallDangerAlert));
                    list.add(toggleSpec("Ассистент спринта", "Поддержание темпа передвижения", sprintAssist, () -> sprintAssist = !sprintAssist));
                }
                case AUTO_BUILD -> {
                    list.add(toggleSpec("Авто-постройка по схеме", "Помощь по активной Litematica", litematicaAutoBuild, () -> litematicaAutoBuild = !litematicaAutoBuild));
                    list.add(toggleSpec("Умная реставрация", "Восстановление разрушенных стен", smartRestoration, () -> smartRestoration = !smartRestoration));
                    list.add(toggleSpec("Авто-пополнение хотбара", "Подмена закончившихся блоков", hotbarAutoRefill, () -> hotbarAutoRefill = !hotbarAutoRefill));
                    list.add(toggleSpec("3D-линейка замеров", "Блок и дистанция под прицелом", rulerOverlay, () -> rulerOverlay = !rulerOverlay));
                    list.add(toggleSpec("Инспектор редстоуна", "Показ силы сигнала RS на блоке", redstoneInspector, () -> redstoneInspector = !redstoneInspector));
                    list.add(toggleSpec("3D-палитра и градиенты", "Подбор сочетаний материалов", palettePreview, () -> palettePreview = !palettePreview));
                }
                case LITEMATICA_HUD -> {
                    list.add(toggleSpec("Виджет материалов HUD", "Счётчик блоков в руке и сумке", litematicaHudEnabled, () -> litematicaHudEnabled = !litematicaHudEnabled));
                    list.add(toggleSpec("Сканер шалкеров", "Учёт ресурсов внутри шалкеров", shulkerScanner, () -> shulkerScanner = !shulkerScanner));
                    list.add(toggleSpec("Разрез по слоям (Y)", "Подсветка нужного уровня постройки", layerSliceMode, () -> layerSliceMode = !layerSliceMode));
                    list.add(cycleSpec("Целевой слой (Y)", "Высота активного разреза", "Y = " + targetBuildLayerY, () -> {
                        targetBuildLayerY = targetBuildLayerY >= 120 ? 40 : targetBuildLayerY + 8;
                    }));
                    list.add(toggleSpec("Подсветка дефицита", "Красный цвет при остатке < 16", deficitHighlight, () -> deficitHighlight = !deficitHighlight));
                }
                case VISUAL_RADAR -> {
                    list.add(toggleSpec("Мини-карта-радар (HUD)", "Отображать карту на экране", radarHudEnabled, () -> radarHudEnabled = !radarHudEnabled));
                    list.add(toggleSpec("Реальный рельеф мира", "Цветные блоки и тени высот", radarTerrainMode, () -> radarTerrainMode = !radarTerrainMode));
                    list.add(cycleSpec("Угол экрана радара", "Расположение под строкой FPS", radarRightSide ? "Справа" : "Слева", () -> radarRightSide = !radarRightSide));
                    list.add(cycleSpec("Масштаб карты", "Радиус обзора местности",
                        radarZoomLevel == 0 ? "Близко" : (radarZoomLevel == 1 ? "Средне" : "Далеко"), () -> {
                            radarZoomLevel = (radarZoomLevel + 1) % 3;
                        }));
                    list.add(toggleSpec("Звуковой радар угроз", "Красный маркер в сторону врага", soundRadarEnabled, () -> soundRadarEnabled = !soundRadarEnabled));
                    list.add(toggleSpec("Нить Ариадны", "След из частиц обратно домой", ariadneThread, () -> ariadneThread = !ariadneThread));
                    list.add(toggleSpec("Шестое чувство", "Тревога если моб за спиной", sixthSense, () -> sixthSense = !sixthSense));
                }
                case CHESTS_VILLAGERS -> {
                    list.add(toggleSpec("Память сундуков", "Найдено хранилищ рядом: " + nearbyChests.size(), chestMemory, () -> chestMemory = !chestMemory));
                    list.add(toggleSpec("Метки сундуков на карте", "Оранжевые точки хранилищ", radarShowChests, () -> radarShowChests = !radarShowChests));
                    list.add(toggleSpec("Метки дропа на радаре", "Золотые точки выпавшего лута", radarShowDrops, () -> radarShowDrops = !radarShowDrops));
                    list.add(toggleSpec("Трекер жителей", "Фиолетовые метки на мини-карте", villagerTracker, () -> villagerTracker = !villagerTracker));
                    list.add(toggleSpec("Умный сортировщик", "Быстрая раскладка инвентаря", smartSorter, () -> smartSorter = !smartSorter));
                }
                case COMBAT_PVP -> {
                    list.add(toggleSpec("Боевой HUD (Тотемы/HP)", "Панель выживания в углу экрана", combatHudEnabled, () -> combatHudEnabled = !combatHudEnabled));
                    list.add(toggleSpec("Индикатор брони", "Полоски прочности Ш/Н/П/Б", armorDurabilityHud, () -> armorDurabilityHud = !armorDurabilityHud));
                    list.add(toggleSpec("Сканер тотемов", "Контроль тотема в руке и сумке", totemScanner, () -> totemScanner = !totemScanner));
                    list.add(toggleSpec("Метка точки смерти", "Дистанция до последней гибели", deathReplayMarker, () -> deathReplayMarker = !deathReplayMarker));
                    list.add(toggleSpec("Траектории снарядов", "Ассистент дуги лука и жемчуга", trajectoriesEnabled, () -> trajectoriesEnabled = !trajectoriesEnabled));
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
                    list.add(cycleSpec("Тема меню [P]", "Палитра окон и свечения", currentTheme.title, () -> {
                        MenuTheme[] themes = MenuTheme.values();
                        currentTheme = themes[(currentTheme.ordinal() + 1) % themes.length];
                    }));
                    list.add(cycleSpec("Крылья персонажа", "Видны в 3D-студии и в F5", WINGS_OPTIONS.get(wingsIndex), () -> {
                        wingsIndex = (wingsIndex + 1) % WINGS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Рога / Нимб", "Аксессуар над головой", HORNS_OPTIONS.get(hornsIndex), () -> {
                        hornsIndex = (hornsIndex + 1) % HORNS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Шляпка / Ушки", "Головной убор в 3D-превью", HATS_OPTIONS.get(hatsIndex), () -> {
                        hatsIndex = (hatsIndex + 1) % HATS_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Одежда и плащ", "Стиль костюма персонажа", OUTFIT_OPTIONS.get(outfitIndex), () -> {
                        outfitIndex = (outfitIndex + 1) % OUTFIT_OPTIONS.size();
                    }));
                    list.add(cycleSpec("Кастомный прицел", "Замена прицела в центре", CROSSHAIR_OPTIONS.get(crosshairIndex), () -> {
                        crosshairIndex = (crosshairIndex + 1) % CROSSHAIR_OPTIONS.size();
                    }));
                    list.add(toggleSpec("3D-эффекты крыльев в F5", "Отображать крылья/нимб в мире", worldCosmeticsParticles, () -> worldCosmeticsParticles = !worldCosmeticsParticles));
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
            if (previewAutoRotate) {
                previewYaw += 0.035;
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
            // Хитбоксы работают в super.render, а затем поверх рисуется 100% кастомный интерфейс
            super.render(g, mouseX, mouseY, partialTick);

            // 1. Главное окно
            fillRoundedRect(g, winX, winY, winW, winH, currentTheme.bgWindow);
            drawRoundedBorder(g, winX, winY, winW, winH, currentTheme.accent);

            // Шапка
            fillVerticalGradient(g, winX + 2, winY + 2, winW - 4, 29, currentTheme.bgCard, currentTheme.bgPanel);
            g.fill(winX + 2, winY + 31, winX + winW - 2, winY + 32, currentTheme.accent);

            // Заголовок и бейдж версии
            g.fill(winX + 10, winY + 12, winX + 16, winY + 18, currentTheme.accent);
            g.drawString(this.font, "ASSISTANT VADIMKA", winX + 22, winY + 11, 0xFFFFFFFF);
            int verX = winX + 22 + this.font.width("ASSISTANT VADIMKA") + 8;
            fillRoundedRect(g, verX, winY + 9, 80, 13, 0xFF162231);
            drawRoundedBorder(g, verX, winY + 9, 80, 13, currentTheme.secondary);
            g.drawString(this.font, VERSION_LABEL + " • 1.21.11", verX + 5, winY + 12, currentTheme.secondary);

            // 2. Левая боковая панель вкладок
            fillRoundedRect(g, winX + 4, winY + 35, sidebarW - 2, winH - 39, currentTheme.bgPanel);
            drawRoundedBorder(g, winX + 4, winY + 35, sidebarW - 2, winH - 39, currentTheme.borderMuted);

            for (TabVisual tv : tabVisuals) {
                boolean active = tv.tab == this.activeTab;
                boolean hover = isOver(mouseX, mouseY, tv.x, tv.y, tv.w, tv.h);
                int bg = active
                    ? lerpColor(currentTheme.bgCard, currentTheme.accent, 0.28F)
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
                    int trackCol = rv.toggleState ? currentTheme.accent : 0xFF283446;
                    fillRoundedRect(g, rv.btnX, rv.btnY + 1, rv.btnW, rv.btnH - 2, trackCol);
                    drawRoundedBorder(g, rv.btnX, rv.btnY + 1, rv.btnW, rv.btnH - 2,
                        btnHover ? 0xFFFFFFFF : (rv.toggleState ? currentTheme.accent : 0xFF475569));
                    int knobSize = rv.btnH - 6;
                    int knobX = rv.toggleState ? (rv.btnX + rv.btnW - knobSize - 3) : (rv.btnX + 3);
                    fillRoundedRect(g, knobX, rv.btnY + 3, knobSize, knobSize, 0xFFFFFFFF);
                    String tag = rv.toggleState ? "ON" : "OFF";
                    int tagX = rv.toggleState ? (rv.btnX + 5) : (rv.btnX + rv.btnW - this.font.width(tag) - 5);
                    g.drawString(this.font, tag, tagX, rv.btnY + 5, rv.toggleState ? 0xFF061510 : 0xFF94A3B8);
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

            // Поле AFK-мини-игры в 8-й вкладке
            if (activeTab == MenuTab.MUSIC_AFK && afkGameRunning) {
                renderAfkGameCanvas(g, contentX + 8, winY + 166, contentW - 16, winH - 178);
            }

            // 4. Правая 3D-студия превью
            int prevX = winX + winW - previewW - 6;
            int prevY = winY + 35;
            int prevH = winH - 39;
            fillRoundedRect(g, prevX, prevY, previewW, prevH, currentTheme.bgPanel);
            drawRoundedBorder(g, prevX, prevY, previewW, prevH, currentTheme.secondary);
            renderLive3DPreviewStudio(g, prevX, prevY, previewW, prevH);

            // 5. Кастомные малые кнопки (закрыть, смена темы, вращение 3D, пагинация)
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
                        : lerpColor(currentTheme.bgCard, currentTheme.accent, 0.22F);
                    border = currentTheme.accent;
                } else {
                    bg = hover ? currentTheme.bgCard : 0xFF0C121B;
                    border = hover ? currentTheme.secondary : currentTheme.borderMuted;
                }
                fillRoundedRect(g, sb.x, sb.y, sb.w, sb.h, bg);
                drawRoundedBorder(g, sb.x, sb.y, sb.w, sb.h, border);
                String lbl = trimText(this.font, sb.label, sb.w - 6);
                int lx = sb.x + (sb.w - this.font.width(lbl)) / 2;
                int ly = sb.y + (sb.h - 8) / 2;
                g.drawString(this.font, lbl, lx, ly, hover ? 0xFFFFFFFF : 0xFFE2E8F0);
            }
        }

        private boolean isOver(int mx, int my, int x, int y, int w, int h) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }

        // =====================================================================
        // ЖИВАЯ 3D-СТУДИЯ ПРЕВЬЮ СПРАВА
        // =====================================================================

        private void renderLive3DPreviewStudio(GuiGraphics g, int px, int py, int pw, int ph) {
            g.drawString(this.font, "✦ 3D СТУДИЯ ПРЕВЬЮ", px + 8, py + 8, currentTheme.accent);

            int vpX = px + 8;
            int vpY = py + 22;
            int vpW = pw - 16;
            int vpH = 116;
            fillVerticalGradient(g, vpX, vpY, vpW, vpH, 0xFF060B12, 0xFF0E1A2A);
            drawRoundedBorder(g, vpX, vpY, vpW, vpH, currentTheme.borderMuted);

            for (int gy = vpY + 18; gy < vpY + vpH; gy += 18) {
                g.fill(vpX + 2, gy, vpX + vpW - 2, gy + 1, 0x12FFFFFF);
            }

            List<VoxelBox> scene = build3DPreviewModels();
            render3DVoxelScene(g, scene, vpX + 1, vpY + 1, vpW - 2, vpH - 2, previewYaw, 0.24, 24.0);

            int infoY = py + 168;
            fillRoundedRect(g, px + 8, infoY, pw - 16, ph - 176, currentTheme.bgCard);
            drawRoundedBorder(g, px + 8, infoY, pw - 16, ph - 176, currentTheme.borderMuted);

            int iy = infoY + 7;
            int maxW = pw - 28;
            g.drawString(this.font, trimText(this.font, "Вадимка: " + currentForm.shortName, maxW), px + 14, iy, currentTheme.accent);
            iy += 13;
            int activeM = 0;
            for (MinionRole r : MinionRole.values()) {
                if (Boolean.TRUE.equals(activeMinions.get(r))) {
                    activeM++;
                }
            }
            g.drawString(this.font, trimText(this.font, "Миньоны: " + activeM + "/4 • " + companionScalePercent + "%", maxW), px + 14, iy, 0xFFCBD5E1);
            iy += 14;
            g.fill(px + 14, iy - 2, px + pw - 14, iy - 1, currentTheme.borderMuted);
            iy += 4;
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

        private List<VoxelBox> build3DPreviewModels() {
            List<VoxelBox> boxes = new ArrayList<>();
            double time = System.currentTimeMillis() / 350.0;
            double bob = Math.sin(time) * 0.08;

            // Неоновый подиум
            boxes.add(new VoxelBox(0.0, -0.18, 0.0, 3.6, 0.14, 2.2, 0xFF1E293B));
            boxes.add(new VoxelBox(0.0, -0.10, 0.0, 3.3, 0.04, 1.9, currentTheme.accent));

            // 3D-персонаж игрока слева (x = -0.68)
            double px = -0.68;
            int bodyMain = switch (outfitIndex) {
                case 1 -> 0xFF0284C7;
                case 2 -> 0xFF18181B;
                case 3 -> 0xFF059669;
                case 4 -> 0xFF4C1D95;
                default -> 0xFF1E293B;
            };
            int bodyTrim = switch (outfitIndex) {
                case 1 -> 0xFF38BDF8;
                case 2 -> 0xFFF43F5E;
                case 3 -> 0xFF34D399;
                case 4 -> 0xFFC084FC;
                default -> 0xFF475569;
            };

            boxes.add(new VoxelBox(px - 0.15, 0.42, 0.0, 0.26, 0.84, 0.28, 0xFF111827));
            boxes.add(new VoxelBox(px + 0.15, 0.42, 0.0, 0.26, 0.84, 0.28, 0xFF0F172A));
            boxes.add(new VoxelBox(px, 1.24, 0.0, 0.56, 0.80, 0.30, bodyMain));
            boxes.add(new VoxelBox(px, 1.24, -0.16, 0.18, 0.72, 0.04, bodyTrim));
            boxes.add(new VoxelBox(px - 0.42, 1.22, 0.0, 0.24, 0.76, 0.26, bodyMain));
            boxes.add(new VoxelBox(px + 0.42, 1.22, 0.0, 0.24, 0.76, 0.26, bodyMain));

            if (outfitIndex > 0) {
                boxes.add(new VoxelBox(px, 1.12, 0.24, 0.52, 1.02, 0.06, bodyTrim));
            }

            boxes.add(new VoxelBox(px, 1.94, 0.0, 0.58, 0.58, 0.58, 0xFFFDBA74));
            boxes.add(new VoxelBox(px, 2.15, 0.02, 0.62, 0.22, 0.62, 0xFF18181B));
            boxes.add(new VoxelBox(px, 1.96, 0.27, 0.62, 0.52, 0.10, 0xFF18181B));
            boxes.add(new VoxelBox(px - 0.14, 1.92, -0.30, 0.12, 0.10, 0.04, 0xFF7F1D1D));
            boxes.add(new VoxelBox(px + 0.14, 1.92, -0.30, 0.12, 0.10, 0.04, 0xFF7F1D1D));

            // 3D-Крылья
            if (wingsIndex > 0) {
                int wCol = switch (wingsIndex) {
                    case 1 -> 0xFFF8FAFC;
                    case 2 -> 0xFFDC2626;
                    case 3 -> 0xFF06B6D4;
                    default -> 0xFFA855F7;
                };
                int wTip = switch (wingsIndex) {
                    case 1 -> 0xFFBAE6FD;
                    case 2 -> 0xFF7F1D1D;
                    case 3 -> 0xFF67E8F9;
                    default -> 0xFFE879F9;
                };
                boxes.add(new VoxelBox(px - 0.68, 1.38 + bob * 0.5, 0.28, 0.72, 0.64, 0.08, wCol));
                boxes.add(new VoxelBox(px - 1.05, 1.56 + bob * 0.5, 0.36, 0.32, 0.42, 0.08, wTip));
                boxes.add(new VoxelBox(px + 0.68, 1.38 + bob * 0.5, 0.28, 0.72, 0.64, 0.08, wCol));
                boxes.add(new VoxelBox(px + 1.05, 1.56 + bob * 0.5, 0.36, 0.32, 0.42, 0.08, wTip));
            }

            // 3D-Рога / Золотой нимб
            if (hornsIndex == 4) {
                boxes.add(new VoxelBox(px, 2.52 + bob, 0.0, 0.68, 0.08, 0.68, 0xFFFACC15));
            } else if (hornsIndex > 0) {
                int hCol = hornsIndex == 1 ? 0xFFEF4444 : (hornsIndex == 2 ? 0xFFD97706 : 0xFF38BDF8);
                boxes.add(new VoxelBox(px - 0.30, 2.38, -0.05, 0.12, 0.32, 0.12, hCol));
                boxes.add(new VoxelBox(px + 0.30, 2.38, -0.05, 0.12, 0.32, 0.12, hCol));
            }

            // 3D-Шляпка / Ушки
            if (hatsIndex == 1) {
                boxes.add(new VoxelBox(px, 2.32, 0.0, 0.54, 0.18, 0.54, 0xFFFACC15));
            } else if (hatsIndex == 2) {
                boxes.add(new VoxelBox(px - 0.22, 2.34, 0.0, 0.22, 0.24, 0.10, 0xFFF472B6));
                boxes.add(new VoxelBox(px + 0.22, 2.34, 0.0, 0.22, 0.24, 0.10, 0xFFF472B6));
            } else if (hatsIndex == 3) {
                boxes.add(new VoxelBox(px, 2.28, 0.0, 0.72, 0.08, 0.72, 0xFF0F172A));
                boxes.add(new VoxelBox(px, 2.52, 0.0, 0.46, 0.42, 0.46, 0xFF1E293B));
            } else if (hatsIndex == 4) {
                boxes.add(new VoxelBox(px - 0.34, 1.96, 0.0, 0.12, 0.28, 0.26, currentTheme.accent));
                boxes.add(new VoxelBox(px + 0.34, 1.96, 0.0, 0.12, 0.28, 0.26, currentTheme.accent));
                boxes.add(new VoxelBox(px, 2.28, 0.0, 0.68, 0.08, 0.12, currentTheme.accent));
            }

            // 3D-модель Вадимки справа (x = +0.92)
            double vx = 0.92;
            switch (currentForm) {
                case RAT -> {
                    boxes.add(new VoxelBox(vx, 0.24, 0.05, 0.46, 0.34, 0.62, 0xFF94A3B8));
                    boxes.add(new VoxelBox(vx, 0.26, -0.34, 0.34, 0.26, 0.32, 0xFFCBD5E1));
                    boxes.add(new VoxelBox(vx, 0.24, -0.52, 0.12, 0.10, 0.08, 0xFFF472B6));
                    boxes.add(new VoxelBox(vx - 0.12, 0.32, -0.44, 0.07, 0.07, 0.05, 0xFF0F172A));
                    boxes.add(new VoxelBox(vx + 0.12, 0.32, -0.44, 0.07, 0.07, 0.05, 0xFF0F172A));
                    boxes.add(new VoxelBox(vx - 0.18, 0.46, -0.22, 0.18, 0.20, 0.08, 0xFFF472B6));
                    boxes.add(new VoxelBox(vx + 0.18, 0.46, -0.22, 0.18, 0.20, 0.08, 0xFFF472B6));
                    boxes.add(new VoxelBox(vx, 0.14, 0.56, 0.10, 0.08, 0.48, 0xFFF9A8D4));
                }
                case WARDEN -> {
                    boxes.add(new VoxelBox(vx - 0.16, 0.30, 0.0, 0.22, 0.56, 0.24, 0xFF0F3D3E));
                    boxes.add(new VoxelBox(vx + 0.16, 0.30, 0.0, 0.22, 0.56, 0.24, 0xFF0F3D3E));
                    boxes.add(new VoxelBox(vx, 0.88, 0.0, 0.64, 0.68, 0.38, 0xFF0D4F4F));
                    boxes.add(new VoxelBox(vx, 0.88, -0.20, 0.34, 0.30, 0.06, 0xFF2DD4BF));
                    boxes.add(new VoxelBox(vx, 1.44, 0.0, 0.54, 0.46, 0.44, 0xFF115E59));
                    boxes.add(new VoxelBox(vx - 0.36, 1.56, 0.0, 0.20, 0.32, 0.10, 0xFF2DD4BF));
                    boxes.add(new VoxelBox(vx + 0.36, 1.56, 0.0, 0.20, 0.32, 0.10, 0xFF2DD4BF));
                }
                case BREEZE -> {
                    boxes.add(new VoxelBox(vx, 0.45 + bob, 0.0, 0.58, 0.22, 0.58, 0xFFC7D2FE));
                    boxes.add(new VoxelBox(vx, 0.86 + bob, 0.0, 0.30, 0.54, 0.30, 0xFF6366F1));
                    boxes.add(new VoxelBox(vx, 1.32 + bob, 0.0, 0.48, 0.44, 0.48, 0xFF818CF8));
                    boxes.add(new VoxelBox(vx, 1.42 + bob, -0.25, 0.42, 0.12, 0.06, 0xFFFDE047));
                }
                case AXOLOTL -> {
                    boxes.add(new VoxelBox(vx, 0.24, 0.08, 0.44, 0.26, 0.66, 0xFF60A5FA));
                    boxes.add(new VoxelBox(vx, 0.28, -0.34, 0.46, 0.28, 0.34, 0xFF93C5FD));
                    boxes.add(new VoxelBox(vx - 0.32, 0.32, -0.30, 0.22, 0.24, 0.08, 0xFFF472B6));
                    boxes.add(new VoxelBox(vx + 0.32, 0.32, -0.30, 0.22, 0.24, 0.08, 0xFFF472B6));
                    boxes.add(new VoxelBox(vx, 0.22, 0.62, 0.10, 0.22, 0.46, 0xFF3B82F6));
                }
                default -> {
                    boxes.add(new VoxelBox(vx, 0.78 + bob, 0.0, 0.32, 0.42, 0.24, currentForm.primaryColor));
                    boxes.add(new VoxelBox(vx, 1.20 + bob, 0.0, 0.42, 0.42, 0.42, currentForm.accentColor));
                    boxes.add(new VoxelBox(vx - 0.38, 0.88 + bob, 0.16, 0.42, 0.28, 0.06, 0xFFE0F2FE));
                    boxes.add(new VoxelBox(vx + 0.38, 0.88 + bob, 0.16, 0.42, 0.28, 0.06, 0xFFE0F2FE));
                }
            }

            return boxes;
        }

        // =====================================================================
        // ОТРИСОВКА 3 ИГРАБЕЛЬНЫХ AFK-МИНИ-ИГР В 8-Й ВКЛАДКЕ
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
