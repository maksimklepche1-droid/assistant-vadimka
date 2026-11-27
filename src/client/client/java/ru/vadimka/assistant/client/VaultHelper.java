package ru.vadimka.assistant.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.vault.VaultBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Снайпер Хранилищ Дворца испытаний v9.1 (прямое API 1.21.11 без рефлексии!):
 * - НИКОГДА не открывает Хранилище, если ты не указал конкретную цель!
 * - Напрямую читает VaultBlockEntity.getSharedData().getDisplayItem() (100% ремапится Fabric Loom!)
 * - Показывает вверху экрана, что прямо сейчас крутится внутри Хранилища и чего ждём
 * - Вставляет нужный ключ ровно в тот тик, когда внутри появляется заказанный предмет!
 */
public final class VaultHelper {
    private VaultHelper() {}

    public static class VaultLootPreset {
        public final String displayName;
        public final Item iconItem;
        public final String[] keywords;

        public VaultLootPreset(String displayName, Item iconItem, String... keywords) {
            this.displayName = displayName;
            this.iconItem = iconItem;
            this.keywords = keywords;
        }
    }

    public static final List<VaultLootPreset> LOOT_PRESETS = List.of(
        new VaultLootPreset("Трезубец", Items.TRIDENT, "трезуб", "trident"),
        new VaultLootPreset("Тяжёлое ядро", Items.HEAVY_CORE, "ядр", "heavy_core", "heavy core"),
        new VaultLootPreset("Зачарованная книга", Items.ENCHANTED_BOOK, "книг", "enchanted_book", "book"),
        new VaultLootPreset("Золотое яблоко", Items.ENCHANTED_GOLDEN_APPLE, "яблок", "golden_apple", "apple"),
        new VaultLootPreset("Зловещая бутылка", Items.OMINOUS_BOTTLE, "бутыл", "ominous_bottle", "bottle"),
        new VaultLootPreset("Алмаз / Блок", Items.DIAMOND, "алмаз", "diamond"),
        new VaultLootPreset("Ветровой заряд", Items.WIND_CHARGE, "ветер", "ветров", "wind_charge"),
        new VaultLootPreset("Арбалет", Items.CROSSBOW, "арбалет", "crossbow")
    );

    private static boolean sniperArmed = false;
    private static int selectedPresetIndex = 0;
    private static String customQuery = "";
    private static String liveDisplayStatus = "Снайпер выключен (укажи предмет)";
    private static boolean matchedRightNow = false;

    private static final int SCAN_RADIUS = 18;
    private static final List<VadimkaConfig.VaultInfo> nearbyVaults = new ArrayList<>();
    private static final List<VadimkaConfig.VaultInfo> scanBuffer = new ArrayList<>();
    private static int scanSliceX = -SCAN_RADIUS;
    private static BlockPos scanOrigin = BlockPos.ZERO;

    private static BlockPos lastLookedContainerPos = null;
    private static String lastLookedContainerName = "Ларчик";
    private static boolean wasContainerOpen = false;
    private static boolean wasMerchantOpen = false;
    private static int lastVaultClickTick = -40;
    private static int lastKeyWarnTick = -120;

    public static boolean isSniperArmed() { return sniperArmed; }
    public static boolean isMatchedRightNow() { return matchedRightNow; }
    public static String getLiveDisplayStatus() { return liveDisplayStatus; }
    public static int getSelectedPresetIndex() { return selectedPresetIndex; }

    public static String getActiveTargetLabel() {
        if (customQuery != null && !customQuery.isEmpty()) return customQuery;
        return LOOT_PRESETS.get(selectedPresetIndex % LOOT_PRESETS.size()).displayName;
    }

    public static Item getActiveTargetIcon() {
        return LOOT_PRESETS.get(selectedPresetIndex % LOOT_PRESETS.size()).iconItem;
    }

    public static void cyclePreset() {
        customQuery = "";
        selectedPresetIndex = (selectedPresetIndex + 1) % LOOT_PRESETS.size();
    }

    public static void setSniperArmed(boolean armed) {
        sniperArmed = armed;
        if (!armed) {
            matchedRightNow = false;
            liveDisplayStatus = "Снайпер выключен";
        } else {
            liveDisplayStatus = "Жду Хранилище → " + getActiveTargetLabel();
        }
    }

    public static String armSniperFromQuery(String itemQuery) {
        String q = itemQuery == null ? "" : itemQuery.toLowerCase(Locale.ROOT).trim();
        if (q.isEmpty()) {
            return "Напиши, какой именно предмет выбить! Например: «Вадимка выбей трезубец в хранилище» или «выбей тяжёлое ядро».";
        }
        int matched = -1;
        for (int i = 0; i < LOOT_PRESETS.size(); i++) {
            for (String kw : LOOT_PRESETS.get(i).keywords) {
                if (q.contains(kw)) { matched = i; break; }
            }
            if (matched >= 0) break;
        }
        if (matched >= 0) {
            selectedPresetIndex = matched;
            customQuery = "";
        } else {
            customQuery = q;
        }
        sniperArmed = true;
        liveDisplayStatus = "Жду в Хранилище: [" + getActiveTargetLabel() + "]";
        VadimkaConfig.narrate("Снайпер Хранилищ нацелен на «" + getActiveTargetLabel() + "»! НЕ открою, пока внутри не появится этот предмет.");
        return "Принял! Жду «" + getActiveTargetLabel() + "» внутри Хранилища (положи ключ в хотбар). Без совпадения открывать НЕ буду!";
    }

    public static List<VadimkaConfig.VaultInfo> getNearbyVaults() { return nearbyVaults; }

    public static int countVaultBlocks() {
        int c = 0;
        for (VadimkaConfig.VaultInfo vi : nearbyVaults) if (vi.isVaultBlock) c++;
        return c;
    }

    public static void clear() {
        nearbyVaults.clear();
        scanBuffer.clear();
        scanSliceX = -SCAN_RADIUS;
        sniperArmed = false;
        matchedRightNow = false;
    }

    public static void tick(Minecraft mc, ClientLevel level, Player player, int worldTick) {
        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = bhr.getBlockPos();
            BlockState bs = level.getBlockState(bp);
            if (bs.is(Blocks.VAULT) || isLootContainer(bs)) {
                lastLookedContainerPos = bp.immutable();
                lastLookedContainerName = bs.getBlock().getName().getString();
            }
        }

        if (worldTick % 2 == 0) {
            scanNextSlice(level, player.blockPosition());
        }

        if (worldTick % 6 == 0 && (VadimkaConfig.highlightTrialVaults || VadimkaConfig.highlightChestsWorld)) {
            int shown = 0;
            for (VadimkaConfig.VaultInfo vi : nearbyVaults) {
                if (vi.pos.distSqr(player.blockPosition()) <= 324.0) {
                    int col = vi.isVaultBlock
                        ? (vi.active ? (vi.ominous ? 0xEC4899 : 0xF97316) : 0x64748B)
                        : 0x22D3EE;
                    level.addParticle(
                        new DustParticleOptions(col, vi.isVaultBlock ? 1.25F : 0.85F),
                        vi.pos.getX() + 0.5, vi.pos.getY() + 1.15, vi.pos.getZ() + 0.5,
                        0.0, 0.03, 0.0
                    );
                    if (++shown >= 10) break;
                }
            }
        }

        // Проверяем Снайпер Хранилищ КАЖДЫЙ тик, но кликаем ТОЛЬКО если sniperArmed == true И предмет совпал!
        tickVaultSniper(mc, level, player, worldTick);

        // Запись открытых сундуков и торгов
        scanOpenMenu(mc, player, worldTick);
    }

    private static void tickVaultSniper(Minecraft mc, ClientLevel level, Player player, int worldTick) {
        matchedRightNow = false;
        VadimkaConfig.VaultInfo nearestActive = null;
        double bestSq = 4.6 * 4.6;
        for (VadimkaConfig.VaultInfo vi : nearbyVaults) {
            if (!vi.isVaultBlock) continue;
            double dSq = player.position().distanceToSqr(Vec3.atCenterOf(vi.pos));
            if (dSq <= bestSq) {
                VadimkaConfig.VaultInfo live = inspectBlock(vi.pos, level.getBlockState(vi.pos));
                if (live != null && live.isVaultBlock && live.active) {
                    nearestActive = live;
                    bestSq = dSq;
                }
            }
        }

        if (nearestActive == null) {
            liveDisplayStatus = sniperArmed
                ? ("Жду Хранилище рядом → [" + getActiveTargetLabel() + "]")
                : "Подойди к Хранилищу";
            return;
        }

        // ПРЯМОЕ 1.21.11 чтение предмета внутри VaultBlockEntity (без рефлексии!)
        ItemStack spinning = ItemStack.EMPTY;
        BlockEntity be = level.getBlockEntity(nearestActive.pos);
        if (be instanceof VaultBlockEntity vaultBe) {
            spinning = vaultBe.getSharedData().getDisplayItem();
        }

        String spinName = spinning.isEmpty() ? "пусто" : VadimkaConfig.formatItemWithEnchants(spinning);
        boolean match = !spinning.isEmpty() && matchesTarget(spinning);
        matchedRightNow = match;
        liveDisplayStatus = "Внутри: [" + spinName + "] → Ждём: [" + getActiveTargetLabel() + "]";

        // КРИТИЧНО: без включённого снайпера НИКОГДА не открываем хранилище!
        if (!sniperArmed || mc.screen != null || mc.gameMode == null) return;
        if (!match) return;
        if ((worldTick - lastVaultClickTick) < 20) return;

        Item neededKey = nearestActive.ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;
        int keySlot = VadimkaConfig.findHotbarSlot(player, neededKey);
        if (keySlot < 0) {
            if ((worldTick - lastKeyWarnTick) > 90) {
                String kName = nearestActive.ominous ? "Зловещий ключ испытаний" : "Ключ испытаний";
                VadimkaConfig.showToast("★ ВЫПАЛ [" + spinName + "]! Нет ключа: " + kName, 80);
                lastKeyWarnTick = worldTick;
            }
            return;
        }

        VadimkaConfig.aimAtBlock(player, nearestActive.pos);
        VadimkaConfig.selectHotbarSlot(player, keySlot);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(nearestActive.pos), Direction.UP, nearestActive.pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastVaultClickTick = worldTick;
        // После открытия выключаем снайпер, чтобы не открывал соседние хранилища случайно!
        sniperArmed = false;
        String msg = "★ ПОЙМАЛ [" + spinName + "] в Хранилище и открыл ключом!";
        VadimkaConfig.sayInChat(msg);
        VadimkaConfig.showToast(msg, 120);
        player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9F, 1.15F);
    }

    private static boolean matchesTarget(ItemStack stack) {
        String ru = VadimkaConfig.formatItemWithEnchants(stack).toLowerCase(Locale.ROOT);
        String id = stack.getItem().toString().toLowerCase(Locale.ROOT);
        String both = ru + " " + id;

        if (customQuery != null && !customQuery.isEmpty()) {
            String q = customQuery.toLowerCase(Locale.ROOT);
            if (both.contains(q)) return true;
            if (q.contains("трезуб") && both.contains("trident")) return true;
            if (q.contains("ядр") && both.contains("heavy_core")) return true;
            if (q.contains("яблок") && both.contains("apple")) return true;
            if (q.contains("книг") && both.contains("book")) return true;
            if (q.contains("алмаз") && both.contains("diamond")) return true;
            if (q.contains("изумр") && both.contains("emerald")) return true;
            if (q.contains("бутыл") && both.contains("bottle")) return true;
            if (q.contains("ветер") && both.contains("wind_charge")) return true;
            if (q.contains("арбалет") && both.contains("crossbow")) return true;
            return false;
        }

        VaultLootPreset preset = LOOT_PRESETS.get(selectedPresetIndex % LOOT_PRESETS.size());
        for (String kw : preset.keywords) {
            if (both.contains(kw)) return true;
        }
        return false;
    }

    private static void scanNextSlice(ClientLevel level, BlockPos center) {
        if (scanSliceX == -SCAN_RADIUS) {
            scanOrigin = center;
            scanBuffer.clear();
        }
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinY(), scanOrigin.getY() - 7);
        int maxY = Math.min(level.getMaxY() - 1, scanOrigin.getY() + 7);
        int wx = scanOrigin.getX() + scanSliceX;

        for (int dz = -SCAN_RADIUS; dz <= SCAN_RADIUS; dz++) {
            int wz = scanOrigin.getZ() + dz;
            for (int y = minY; y <= maxY; y++) {
                p.set(wx, y, wz);
                VadimkaConfig.VaultInfo vi = inspectBlock(p, level.getBlockState(p));
                if (vi != null && scanBuffer.size() < 48) scanBuffer.add(vi);
            }
        }
        scanSliceX++;
        if (scanSliceX > SCAN_RADIUS) {
            nearbyVaults.clear();
            nearbyVaults.addAll(scanBuffer);
            scanSliceX = -SCAN_RADIUS;
        }
    }

    public static VadimkaConfig.VaultInfo inspectBlock(BlockPos pos, BlockState st) {
        if (st.is(Blocks.VAULT)) {
            boolean ominous = false;
            boolean active = true;
            String stateName = "active";
            for (Map.Entry<Property<?>, Comparable<?>> entry : st.getValues().entrySet()) {
                String k = entry.getKey().getName().toLowerCase(Locale.ROOT);
                String v = String.valueOf(entry.getValue()).toLowerCase(Locale.ROOT);
                if (k.contains("ominous")) ominous = "true".equals(v);
                else if (k.contains("state")) {
                    stateName = v;
                    active = v.equals("active") || v.equals("unlocking");
                }
            }
            return new VadimkaConfig.VaultInfo(pos.immutable(), true, ominous, active, stateName);
        }
        if (isLootContainer(st)) {
            return new VadimkaConfig.VaultInfo(pos.immutable(), false, false, true, "chest");
        }
        return null;
    }

    public static boolean isLootContainer(BlockState st) {
        if (st.is(Blocks.CHEST) || st.is(Blocks.TRAPPED_CHEST)
                || st.is(Blocks.BARREL) || st.is(Blocks.ENDER_CHEST)) return true;
        String n = st.getBlock().getName().getString().toLowerCase(Locale.ROOT);
        return n.contains("шалкер") || n.contains("shulker");
    }

    public static String openChestOrArmVault() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return "Зайди в мир!";
        Player p = mc.player;
        VadimkaConfig.VaultInfo best = null;
        double bestSq = 4.6 * 4.6;
        BlockPos pc = p.blockPosition();
        for (int dx = -4; dx <= 4; dx++) {
            for (int dy = -2; dy <= 3; dy++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos bp = pc.offset(dx, dy, dz);
                    VadimkaConfig.VaultInfo vi = inspectBlock(bp, mc.level.getBlockState(bp));
                    if (vi == null) continue;
                    double sq = p.position().distanceToSqr(Vec3.atCenterOf(bp));
                    if (sq <= bestSq && (best == null || (vi.isVaultBlock && !best.isVaultBlock) || sq < bestSq)) {
                        best = vi; bestSq = sq;
                    }
                }
            }
        }
        if (best == null) return "Рядом (в радиусе 4.5м) нет Хранилищ или сундуков!";
        if (best.isVaultBlock) {
            if (!best.active) return "Это Хранилище уже открыто.";
            // НЕ открываем Хранилище вслепую — включаем Снайпер на выбранную цель!
            sniperArmed = true;
            return "Хранилище найдено! Включил Снайпер на «" + getActiveTargetLabel() + "» — открою ровно когда этот предмет появится внутри!";
        }
        VadimkaConfig.aimAtBlock(p, best.pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(best.pos), Direction.UP, best.pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        return "Открыл ближайший ларчик!";
    }

    private static void scanOpenMenu(Minecraft mc, Player player, int worldTick) {
        AbstractContainerMenu menu = player.containerMenu;
        boolean open = menu != null && menu != player.inventoryMenu && mc.screen != null && !(mc.screen instanceof VadimkaScreen);

        if (open && menu instanceof MerchantMenu merchantMenu) {
            if (!wasMerchantOpen || worldTick % 20 == 0) {
                MerchantOffers offers = merchantMenu.getOffers();
                if (offers != null && !offers.isEmpty()) {
                    BlockPos vPos = player.blockPosition();
                    VadimkaConfig.VillagerRecord rec = new VadimkaConfig.VillagerRecord(vPos, "Житель [" + vPos.getX() + ", " + vPos.getZ() + "]");
                    VadimkaConfig.EnchantGoal goal = VadimkaConfig.getCurrentEnchantGoal();
                    VadimkaConfig.VillagerTradeEntry hit = null;
                    for (MerchantOffer offer : offers) {
                        String cA = shortItem(offer.getCostA());
                        String cB = offer.getCostB().isEmpty() ? "" : (" + " + shortItem(offer.getCostB()));
                        String res = VadimkaConfig.formatItemWithEnchants(offer.getResult());
                        int emPrice = offer.getCostA().is(Items.EMERALD) ? offer.getCostA().getCount() : 99;
                        boolean match = VadimkaConfig.matchesTargetEnchantStack(offer.getResult(), goal);
                        VadimkaConfig.VillagerTradeEntry e = new VadimkaConfig.VillagerTradeEntry(cA + cB, res, emPrice, match || offer.getResult().is(Items.ENCHANTED_BOOK));
                        rec.trades.add(e);
                        if (match && emPrice <= VadimkaConfig.maxEmeraldPrice && hit == null) hit = e;
                    }
                    VadimkaConfig.rememberedVillagers.put(vPos, rec);
                    if (!wasMerchantOpen && !VillagerRoller.isRunning()) {
                        if (hit != null) {
                            VadimkaConfig.narrate("★ НАЙДЕНА КНИГА: " + hit.resultText + " за " + hit.costText + "!");
                            player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.85F, 1.0F);
                        } else {
                            VadimkaConfig.showToast("Записан житель (" + rec.trades.size() + " сделок)", 50);
                        }
                    }
                }
            }
            wasMerchantOpen = true;
            wasContainerOpen = false;
            return;
        }
        wasMerchantOpen = false;

        if (open && VadimkaConfig.baseChestMemory && lastLookedContainerPos != null) {
            if (!wasContainerOpen || worldTick % 15 == 0) {
                VadimkaConfig.ChestRecord rec = new VadimkaConfig.ChestRecord(lastLookedContainerPos, lastLookedContainerName);
                for (Slot slot : menu.slots) {
                    if (slot.container == player.getInventory()) continue;
                    ItemStack st = slot.getItem();
                    if (!st.isEmpty()) rec.items.merge(VadimkaConfig.formatItemWithEnchants(st), st.getCount(), Integer::sum);
                }
                VadimkaConfig.rememberedChests.put(lastLookedContainerPos, rec);
                if (!wasContainerOpen) VadimkaConfig.showToast("Ларчик запомнен (" + rec.items.size() + " видов)", 45);
            }
            wasContainerOpen = true;
        } else {
            wasContainerOpen = false;
        }
    }

    private static String shortItem(ItemStack s) {
        if (s.isEmpty()) return "";
        return s.getHoverName().getString() + (s.getCount() > 1 ? (" x" + s.getCount()) : "");
    }
}
