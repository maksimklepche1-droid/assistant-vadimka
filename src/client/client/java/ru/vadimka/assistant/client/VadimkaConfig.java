package ru.vadimka.assistant.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Центральное хранилище состояния, настроек, справочников и утилит Assistant Vadimka v9.1.
 */
public final class VadimkaConfig {
    public static final String MOD_ID = "assistant_vadimka";
    public static final String VERSION_LABEL = "v9.1 NEXUS";

    private VadimkaConfig() {}

    public static Identifier guiTex(String name) {
        return Identifier.fromNamespaceAndPath(MOD_ID, "textures/gui/" + name + ".png");
    }

    public static final Identifier ICON_BTN_HOME = guiTex("btn_home");
    public static final Identifier ICON_BTN_PLAY = guiTex("btn_play");
    public static final Identifier ICON_BTN_STOP = guiTex("btn_stop");
    public static final Identifier ICON_BTN_CHAT = guiTex("btn_chat");
    public static final Identifier ICON_BTN_SWIM = guiTex("btn_swim");

    // =========================================================================
    // ПЕРЕЧИСЛЕНИЯ
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
        COMPANION("Вадимка", "Спутник Вадимка и Чат", guiTex("tab_companion")),
        AUTOPILOT("Автопилот", "A* Автопилот с обходом преград", guiTex("tab_autopilot")),
        ACTIONS("Команды", "Движок действий: Бег / Плыть / Копать / Бой", guiTex("tab_actions")),
        BUILD_HUD("Стройка", "Litematica HUD, Хотбар и Редстоун", guiTex("tab_build")),
        COMBAT_PVP("Бой PvP", "Боевой HUD, Броня и Траектории", guiTex("tab_combat")),
        VISUAL_RADAR("Радар", "Карта (Xaero Style) и Поиск Структур", guiTex("tab_radar")),
        SPOTIFY_PLAYER("Spotify", "Spotify Connect (ПК) и AFK-игры", guiTex("tab_spotify")),
        CHESTS_VILLAGERS("Ларчики", "Снайпер Хранилищ и Перебор Жителя", guiTex("tab_vaults")),
        CUSTOM_VISUAL("Кастом", "3D-Гардероб, CPM-модели и Темы", guiTex("tab_custom"));

        public final String navTitle;
        public final String headerTitle;
        public final Identifier iconTex;

        MenuTab(String navTitle, String headerTitle, Identifier iconTex) {
            this.navTitle = navTitle;
            this.headerTitle = headerTitle;
            this.iconTex = iconTex;
        }
    }

    public enum StructureTarget {
        TRIAL_CHAMBER("Дворец испытаний", "trial_chambers", 0xFFF97316, Items.VAULT),
        VILLAGE("Деревня", "village", 0xFF4ADE80, Items.BELL),
        STRONGHOLD("Крепость Энда", "stronghold", 0xFFA855F7, Items.ENDER_EYE),
        ANCIENT_CITY("Древний город", "ancient_city", 0xFF14B8A6, Items.SCULK_SHRIEKER),
        MANSION("Лесной особняк", "mansion", 0xFFEAB308, Items.TOTEM_OF_UNDYING),
        OUTPOST("Аванпост", "pillager_outpost", 0xFFEF4444, Items.CROSSBOW),
        MONUMENT("Монумент океана", "monument", 0xFF38BDF8, Items.PRISMARINE_SHARD),
        RUINED_PORTAL("Портал в Незер", "ruined_portal", 0xFFC084FC, Items.OBSIDIAN);

        public final String russianName;
        public final String keyFragment;
        public final int beaconColor;
        public final Item iconItem;

        StructureTarget(String russianName, String keyFragment, int beaconColor, Item iconItem) {
            this.russianName = russianName;
            this.keyFragment = keyFragment;
            this.beaconColor = beaconColor;
            this.iconItem = iconItem;
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

    public enum AfkGameType {
        RUNNER("MC-Раннер", "A / D — уворот от криперов"),
        SONIC("Рывок", "SPACE / W — прыжки за кольцами"),
        DOOM("Данж-Шутер", "A / D + SPACE — огонь");

        public final String title;
        public final String hint;

        AfkGameType(String title, String hint) {
            this.title = title;
            this.hint = hint;
        }
    }

    public enum MobIconKind {
        PLAYER, CREEPER, ZOMBIE, SKELETON, SPIDER, ENDERMAN, VILLAGER,
        PIG, COW, SHEEP, CHICKEN, HOSTILE_OTHER, ANIMAL_OTHER, ITEM
    }

    public static class EnchantGoal {
        public final String russianName;
        public final String mcId;
        public final String[] chatAliases;

        public EnchantGoal(String russianName, String mcId, String... chatAliases) {
            this.russianName = russianName;
            this.mcId = mcId;
            this.chatAliases = chatAliases;
        }
    }

    public static final List<EnchantGoal> ENCHANT_GOALS = List.of(
        new EnchantGoal("Починка", "mending", "почин", "mending"),
        new EnchantGoal("Неразрушимость", "unbreaking", "неразруш", "прочност", "unbreaking"),
        new EnchantGoal("Эффективность", "efficiency", "эффектив", "efficiency"),
        new EnchantGoal("Острота", "sharpness", "острот", "sharpness"),
        new EnchantGoal("Защита", "protection", "защит", "protection"),
        new EnchantGoal("Удача", "fortune", "удач", "fortune"),
        new EnchantGoal("Добыча", "looting", "добыч", "looting"),
        new EnchantGoal("Шёлковое касание", "silk_touch", "шёлк", "шелк", "silk"),
        new EnchantGoal("Бесконечность", "infinity", "бесконеч", "infinity"),
        new EnchantGoal("Сила", "power", "сил", "power"),
        new EnchantGoal("Невесомость", "feather_falling", "невесом", "feather"),
        new EnchantGoal("Плотность (Булава)", "density", "плотност", "density"),
        new EnchantGoal("Пробивание (Булава)", "breach", "пробиван", "breach")
    );

    // =========================================================================
    // СТРУКТУРЫ ДАННЫХ
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

        public ChestRecord(BlockPos pos, String containerName) {
            this.pos = pos;
            this.containerName = containerName;
        }

        public String summaryLine() {
            if (items.isEmpty()) return "Пусто (открой заново)";
            StringBuilder sb = new StringBuilder();
            int c = 0;
            for (Map.Entry<String, Integer> e : items.entrySet()) {
                if (c > 0) sb.append(", ");
                sb.append(e.getKey()).append(" x").append(e.getValue());
                if (++c >= 3) break;
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

        public VillagerTradeEntry(String costText, String resultText, int emeraldPrice, boolean valuable) {
            this.costText = costText;
            this.resultText = resultText;
            this.emeraldPrice = emeraldPrice;
            this.valuable = valuable;
        }
    }

    public static class VillagerRecord {
        public final BlockPos pos;
        public final String label;
        public final List<VillagerTradeEntry> trades = new ArrayList<>();

        public VillagerRecord(BlockPos pos, String label) {
            this.pos = pos;
            this.label = label;
        }
    }

    public static class FoundStructureRecord {
        public final StructureTarget target;
        public final BlockPos pos;

        public FoundStructureRecord(StructureTarget target, BlockPos pos) {
            this.target = target;
            this.pos = pos;
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

    public static class RadarBlip {
        public final double x, z;
        public final MobIconKind iconKind;
        public final String name;

        public RadarBlip(double x, double z, MobIconKind iconKind, String name) {
            this.x = x; this.z = z; this.iconKind = iconKind; this.name = name;
        }
    }

    // =========================================================================
    // ГЛОБАЛЬНЫЕ НАСТРОЙКИ И СОСТОЯНИЕ
    // =========================================================================

    // Спутник
    public static VadimkaForm currentForm = VadimkaForm.AXOLOTL;
    public static boolean companionEnabled = true;
    public static boolean positionEditor3D = true;
    public static CompanionAnchor currentAnchor = CompanionAnchor.RIGHT_SHOULDER;
    public static double offsetX = 0.95, offsetY = 1.15, offsetZ = 0.25;
    public static int companionScalePercent = 100;
    public static boolean minionsEnabled = true;
    public static boolean minionsSmartAi = true;
    public static boolean compactNameplates = true;
    public static boolean chatAssistantEnabled = true;
    public static boolean narrateActionsInChat = true;
    public static boolean quietMode = false;
    public static final Map<MinionRole, Boolean> activeMinions = new EnumMap<>(MinionRole.class);
    public static final Deque<ChatLogEntry> chatHistory = new ArrayDeque<>();

    // Автопилот
    public static boolean sprintAssist = true;
    public static boolean autoReturnControl = true;
    public static boolean fallDangerAlert = true;
    public static boolean smoothCameraTurn = true;
    public static int fallDropBlocks = 0;

    // Стройка и Litematica HUD
    public static boolean litematicaHudEnabled = true;
    public static boolean litematicaHudRight = true;
    public static boolean hotbarAutoRefill = true;
    public static boolean redstoneInspector = true;
    public static boolean ruler3DVolume = true;
    public static boolean hideCompletedMaterials = false;

    // Бой и PvP
    public static boolean combatHudEnabled = true;
    public static boolean trajectories3D = true;
    public static boolean backstabIndicator = true;
    public static boolean deathReplayMarker = true;

    // Радар и Xaero-style GPU-карта
    public static boolean visualsEnabled = true;
    public static boolean minimapRadarEnabled = true;
    public static boolean minimapCircularMode = true;
    public static boolean minimapTerrainEnabled = true;
    public static boolean minimapRotateWithPlayer = false; // Как в Xaero: Север сверху = кристальная 1:1 чёткость!
    public static boolean minimapMobHeads = true;          // Иконки голов крипера, жителя, зомби, скелета и т.д.!
    public static boolean soundRadarSonar = true;
    public static boolean elytraPitchHorizon = true;
    public static boolean highlightChestsWorld = true;
    public static boolean baseChestMemory = true;
    public static boolean ariadneThread = true;
    public static int radarZoomLevel = 0; // 0=1x, 1=2x, 2=3x

    // Spotify и AFK
    public static boolean spotifyHudEnabled = true;
    public static boolean vadimkaDanceToBpm = true;
    public static boolean afkPanicExit = true;
    public static AfkGameType selectedAfkGame = AfkGameType.RUNNER;

    // Хранилища и Библиотекарь
    public static boolean highlightTrialVaults = true;
    public static boolean warnAxeDurability = true;
    public static int targetEnchantIndex = 0;
    public static int maxEmeraldPrice = 64; // ПО УМОЛЧАНИЮ 64 — чтобы НЕ скипать Починку за 16-38 изумрудов!

    // Кастом-визуал и Темы
    public static MenuTheme currentTheme = MenuTheme.CONCEPT_CYAN;
    public static final List<String> WINGS_OPTIONS = List.of("Выкл", "Ангельские", "Демон", "Неон", "Кристалл");
    public static final List<String> HORNS_OPTIONS = List.of("Выкл", "Рога", "Оленьи", "Кибер", "Нимб");
    public static final List<String> HATS_OPTIONS = List.of("Выкл", "Корона", "Шлем", "Колпак", "Ушки");
    public static final List<String> CROSSHAIR_OPTIONS = List.of("Ваниль", "Крест+", "Круг", "Тактик");
    public static int wingsIndex = 1;
    public static int hornsIndex = 3;
    public static int hatsIndex = 1;
    public static int crosshairIndex = 1;
    public static boolean worldCosmeticsF5 = true;

    // Навигатор, память, кэши
    public static BlockPos homeWaypoint = null;
    public static BlockPos activeNavTarget = null;
    public static String activeNavTitle = "";
    public static int activeNavColor = 0xFF22D3EE;
    public static final Map<StructureTarget, FoundStructureRecord> foundStructures = new EnumMap<>(StructureTarget.class);
    public static final Map<BlockPos, ChestRecord> rememberedChests = new LinkedHashMap<>();
    public static final Map<BlockPos, VillagerRecord> rememberedVillagers = new LinkedHashMap<>();
    public static final List<RadarBlip> radarBlips = new ArrayList<>();
    public static Vec3 nearestLootPos = null;
    public static Vec3 nearestHostilePos = null;
    public static double nearestHostileDist = 999.0;
    public static double nearestHostileAngle = 0.0;
    public static boolean threatBehindPlayer = false;
    public static double sessionWalkedBlocks = 0.0;

    public static String toastText = "";
    public static int toastTicks = 0;
    private static String lastNarrationText = "";
    private static long lastNarrationMs = 0L;

    public static void initDefaults() {
        for (MinionRole r : MinionRole.values()) {
            activeMinions.put(r, true);
        }
        pushChatLog(true, "Вадимка v9.1 готов! Пиши: «выбей трезубец в хранилище», «выбей книгу на починку у жителя», «!к дому», «!добудь дерево».");
    }

    public static void pushChatLog(boolean fromVadimka, String text) {
        if (text == null || text.isEmpty()) return;
        chatHistory.addLast(new ChatLogEntry(fromVadimka, text));
        while (chatHistory.size() > 36) chatHistory.removeFirst();
    }

    public static void showToast(String text, int ticks) {
        if (quietMode || text == null || text.isEmpty()) return;
        toastText = text;
        toastTicks = ticks;
    }

    public static void sayInChat(String text) {
        if (text == null || text.isEmpty()) return;
        pushChatLog(true, text);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("§b[✦ Вадимка] §f" + text), false);
        }
    }

    public static void narrate(String text) {
        if (text == null || text.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (text.equals(lastNarrationText) && (now - lastNarrationMs) < 3000L) return;
        lastNarrationText = text;
        lastNarrationMs = now;
        showToast("✦ " + text, 65);
        if (narrateActionsInChat && !quietMode) sayInChat(text);
        else pushChatLog(true, text);
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

    public static EnchantGoal getCurrentEnchantGoal() {
        return ENCHANT_GOALS.get(Mth.clamp(targetEnchantIndex, 0, ENCHANT_GOALS.size() - 1));
    }

    /**
     * 100% точная проверка зачарования книги по внутреннему Minecraft ResourceKey ID (mending, unbreaking и т.д.)
     * + запасная проверка по русскому названию. Никогда не пропустит нужную книгу!
     */
    public static boolean matchesTargetEnchantStack(ItemStack stack, EnchantGoal goal) {
        if (stack == null || stack.isEmpty() || goal == null) return false;
        ItemEnchantments stored = stack.get(DataComponents.STORED_ENCHANTMENTS);
        ItemEnchantments active = stack.get(DataComponents.ENCHANTMENTS);
        ItemEnchantments ench = (stored != null && !stored.isEmpty()) ? stored : active;
        if (ench != null && !ench.isEmpty()) {
            for (var entry : ench.entrySet()) {
                String keyId = entry.getKey().unwrapKey().map(k -> k.toString().toLowerCase(Locale.ROOT)).orElse("");
                if (keyId.contains(goal.mcId)) {
                    return true;
                }
                String fullRu = Enchantment.getFullname(entry.getKey(), entry.getValue()).getString().toLowerCase(Locale.ROOT);
                for (String alias : goal.chatAliases) {
                    if (fullRu.contains(alias)) return true;
                }
            }
        }
        String formatted = formatItemWithEnchants(stack).toLowerCase(Locale.ROOT);
        for (String alias : goal.chatAliases) {
            if (formatted.contains(alias)) return true;
        }
        return false;
    }

    // =========================================================================
    // ПОМОЩНИКИ ДЛЯ ИНВЕНТАРЯ, КАМЕРЫ И ТЕКСТА
    // =========================================================================

    public static int findHotbarSlot(Player player, Item item) {
        for (int i = 0; i < 9; i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && st.is(item)) return i;
        }
        return -1;
    }

    public static int findInventorySlot(Player player, Item item) {
        for (int i = 9; i < 36; i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && st.is(item)) return i;
        }
        return -1;
    }

    public static void selectHotbarSlot(Player player, int slot) {
        if (slot >= 0 && slot <= 8) player.getInventory().setSelectedSlot(slot);
    }

    public static int getSelectedHotbarSlot(Player player) {
        return player.getInventory().getSelectedSlot();
    }

    public static int findBestSwordOrAxeSlot(Player player) {
        Item[] weapons = {
            Items.NETHERITE_SWORD, Items.DIAMOND_SWORD, Items.IRON_SWORD, Items.STONE_SWORD, Items.GOLDEN_SWORD, Items.WOODEN_SWORD,
            Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE, Items.STONE_AXE, Items.WOODEN_AXE
        };
        for (Item w : weapons) { int s = findHotbarSlot(player, w); if (s >= 0) return s; }
        return -1;
    }

    public static int findBestPickaxeSlot(Player player) {
        Item[] picks = {Items.NETHERITE_PICKAXE, Items.DIAMOND_PICKAXE, Items.IRON_PICKAXE, Items.STONE_PICKAXE, Items.GOLDEN_PICKAXE, Items.WOODEN_PICKAXE};
        for (Item p : picks) { int s = findHotbarSlot(player, p); if (s >= 0) return s; }
        return -1;
    }

    public static int findBestAxeSlot(Player player) {
        Item[] axes = {Items.NETHERITE_AXE, Items.DIAMOND_AXE, Items.IRON_AXE, Items.STONE_AXE, Items.GOLDEN_AXE, Items.WOODEN_AXE};
        for (Item a : axes) { int s = findHotbarSlot(player, a); if (s >= 0) return s; }
        return -1;
    }

    public static int findBestShovelSlot(Player player) {
        Item[] shovels = {Items.NETHERITE_SHOVEL, Items.DIAMOND_SHOVEL, Items.IRON_SHOVEL, Items.STONE_SHOVEL, Items.GOLDEN_SHOVEL, Items.WOODEN_SHOVEL};
        for (Item sh : shovels) { int s = findHotbarSlot(player, sh); if (s >= 0) return s; }
        return -1;
    }

    public static void aimAtPoint(Player player, double tx, double ty, double tz, float maxStepDeg) {
        Vec3 eye = player.getEyePosition();
        double dx = tx - eye.x, dy = ty - eye.y, dz = tz - eye.z;
        double horiz = Math.sqrt(dx * dx + dz * dz);
        float desiredYaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        float desiredPitch = (float) (-(Mth.atan2(dy, horiz) * (180.0 / Math.PI)));
        float yawDelta = Mth.wrapDegrees(desiredYaw - player.getYRot());
        float pitchDelta = desiredPitch - player.getXRot();
        player.setYRot(player.getYRot() + Mth.clamp(yawDelta, -maxStepDeg, maxStepDeg));
        player.setXRot(Mth.clamp(player.getXRot() + Mth.clamp(pitchDelta, -maxStepDeg, maxStepDeg), -88.0F, 88.0F));
    }

    public static void aimAtBlock(Player player, BlockPos pos) {
        aimAtPoint(player, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 45.0F);
    }

    public static void aimAtEntity(Player player, Entity target) {
        aimAtPoint(player, target.getX(), target.getY() + target.getBbHeight() * 0.65, target.getZ(), 35.0F);
    }

    public static int countPlayerTotems(Player p) {
        int totems = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (st.is(Items.TOTEM_OF_UNDYING)) totems += st.getCount();
        }
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) totems += p.getOffhandItem().getCount();
        return totems;
    }

    public static String formatItemWithEnchants(ItemStack stack) {
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
                if (++c >= 2) break;
            }
            return stack.is(Items.ENCHANTED_BOOK) ? ("Книга: " + sb) : (base + " (" + sb + ")");
        }
        return base + (stack.getCount() > 1 ? (" x" + stack.getCount()) : "");
    }

    public static String trimText(Font font, String text, int maxWidth) {
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
}
