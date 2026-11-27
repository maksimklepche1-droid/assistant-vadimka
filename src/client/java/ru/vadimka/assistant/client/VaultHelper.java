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
 * Честный помощник Дворца испытаний, Хранилищ (Vault) и памяти Ларчиков:
 * - Точно сканирует активные/зловещие Хранилища и сундуки в радиусе 18 блоков (шаг 1!)
 * - Подсвечивает активные Хранилища оранжевым, Зловещие — розовым, обычные ларчики — голубым
 * - Автоматически подбирает нужный ключ (Ключ испытаний / Зловещий ключ) при открытии
 * - Автоматически запоминает содержимое всех открытых сундуков и сделки всех жителей
 */
public final class VaultHelper {
    private VaultHelper() {}

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

    public static List<VadimkaConfig.VaultInfo> getNearbyVaults() {
        return nearbyVaults;
    }

    public static int countVaultBlocks() {
        int c = 0;
        for (VadimkaConfig.VaultInfo vi : nearbyVaults) {
            if (vi.isVaultBlock) c++;
        }
        return c;
    }

    public static void clear() {
        nearbyVaults.clear();
        scanBuffer.clear();
        scanSliceX = -SCAN_RADIUS;
    }

    public static void tick(Minecraft mc, ClientLevel level, Player player, int worldTick) {
        // 1. Запоминаем сундук/хранилище под прицелом
        if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
            BlockPos bp = bhr.getBlockPos();
            BlockState bs = level.getBlockState(bp);
            if (bs.is(Blocks.VAULT) || isLootContainer(bs)) {
                lastLookedContainerPos = bp.immutable();
                lastLookedContainerName = bs.getBlock().getName().getString();
            }
        }

        // 2. Инкрементальный скан (1 срез по X раз в 2 тика = 0 лагов, шаг 1!)
        if (worldTick % 2 == 0) {
            scanNextSlice(level, player.blockPosition());
        }

        // 3. Подсветка Хранилищ и Ларчиков в мире
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

        // 4. Авто-открытие активных Хранилищ при наличии нужного ключа в хотбаре
        if (VadimkaConfig.autoOpenVaultsWithKey && mc.screen == null && mc.gameMode != null
                && (worldTick - lastVaultClickTick) >= 20) {
            tryAutoOpenVault(mc, level, player, worldTick);
        }

        // 5. Запись содержимого открытых сундуков и торгов жителей
        scanOpenMenu(mc, player, worldTick);
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
                if (vi != null && scanBuffer.size() < 48) {
                    scanBuffer.add(vi);
                }
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
                || st.is(Blocks.BARREL) || st.is(Blocks.ENDER_CHEST)) {
            return true;
        }
        String n = st.getBlock().getName().getString().toLowerCase(Locale.ROOT);
        return n.contains("шалкер") || n.contains("shulker");
    }

    private static void tryAutoOpenVault(Minecraft mc, ClientLevel level, Player player, int worldTick) {
        VadimkaConfig.VaultInfo best = null;
        double bestSq = 4.4 * 4.4;
        for (VadimkaConfig.VaultInfo vi : nearbyVaults) {
            if (!vi.isVaultBlock) continue;
            double dSq = player.position().distanceToSqr(Vec3.atCenterOf(vi.pos));
            if (dSq <= bestSq) {
                VadimkaConfig.VaultInfo live = inspectBlock(vi.pos, level.getBlockState(vi.pos));
                if (live != null && live.isVaultBlock && live.active) {
                    best = live;
                    bestSq = dSq;
                }
            }
        }
        if (best == null) return;

        Item neededKey = best.ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;
        int keySlot = VadimkaConfig.findHotbarSlot(player, neededKey);
        if (keySlot < 0) {
            if ((worldTick - lastKeyWarnTick) > 120) {
                String kName = best.ominous ? "Зловещий ключ испытаний" : "Ключ испытаний";
                VadimkaConfig.showToast("Активное Хранилище рядом! Положи в хотбар: " + kName, 70);
                lastKeyWarnTick = worldTick;
            }
            return;
        }

        VadimkaConfig.aimAtBlock(player, best.pos);
        VadimkaConfig.selectHotbarSlot(player, keySlot);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(best.pos), Direction.UP, best.pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        lastVaultClickTick = worldTick;
        VadimkaConfig.narrate("Открыл " + (best.ominous ? "Зловещее хранилище" : "Хранилище") + " нужным ключом!");
    }

    public static String openNearestManually() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            return "Зайди в мир!";
        }
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
                        best = vi;
                        bestSq = sq;
                    }
                }
            }
        }
        if (best == null) {
            return "В радиусе 4.5 блоков нет Хранилищ или сундуков!";
        }
        if (best.isVaultBlock) {
            if (!best.active) {
                return "Это Хранилище уже открыто (состояние: " + best.stateName + ").";
            }
            Item key = best.ominous ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY;
            int slot = VadimkaConfig.findHotbarSlot(p, key);
            if (slot < 0) {
                return "Положи в хотбар: " + (best.ominous ? "Зловещий ключ испытаний" : "Ключ испытаний") + "!";
            }
            VadimkaConfig.selectHotbarSlot(p, slot);
        }
        VadimkaConfig.aimAtBlock(p, best.pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(best.pos), Direction.UP, best.pos, false);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
        mc.player.swing(InteractionHand.MAIN_HAND);
        return best.isVaultBlock
            ? "Открыл Хранилище ключом! (Важно: дроп в Minecraft 1.21 сервер кидает случайно в момент открытия.)"
            : "Открыл ближайший ларчик!";
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
                    String want = VadimkaConfig.TARGET_ENCHANTS.get(VadimkaConfig.targetEnchantIndex);
                    VadimkaConfig.VillagerTradeEntry hit = null;

                    for (MerchantOffer offer : offers) {
                        String cA = shortItem(offer.getCostA());
                        String cB = offer.getCostB().isEmpty() ? "" : (" + " + shortItem(offer.getCostB()));
                        String res = VadimkaConfig.formatItemWithEnchants(offer.getResult());
                        int emPrice = offer.getCostA().is(Items.EMERALD) ? offer.getCostA().getCount() : 99;
                        boolean match = VadimkaConfig.matchesTargetEnchant(res, want);
                        boolean valuable = match || offer.getResult().is(Items.ENCHANTED_BOOK);
                        VadimkaConfig.VillagerTradeEntry e = new VadimkaConfig.VillagerTradeEntry(cA + cB, res, emPrice, valuable);
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
