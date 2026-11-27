package ru.vadimka.assistant.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Исправленный Авто-перебор кафедры Библиотекаря:
 * - Сначала ВСЕГДА открывает торги и ждёт прихода пакета MerchantOffers с сервера (до 40 тиков),
 *   а НЕ ломает кафедру вслепую!
 * - Если нужные чары <= макс. цены найдены — останавливается, оставляет торги открытыми и играет фанфару!
 * - Если не подошло — закрывает торги (LocalPlayer.closeContainer), берёт топор (защита прочности > 10),
 *   ломает кафедру, подбирает (при необходимости перекидывает из инвентаря в хотбар), ставит обратно,
 *   ждёт, пока житель снова возьмёт профессию, и повторяет проверку.
 */
public final class VillagerRoller {
    private VillagerRoller() {}

    public enum State {
        IDLE("Выключен"),
        OPEN_TRADE("Открываю торги"),
        WAIT_OFFERS("Жду список книг"),
        BREAK_LECTERN("Ломаю кафедру"),
        PICKUP_LECTERN("Подбираю кафедру"),
        PLACE_LECTERN("Ставлю кафедру"),
        WAIT_RECLAIM("Жду профессию");

        public final String label;
        State(String label) { this.label = label; }
    }

    private static State state = State.IDLE;
    private static BlockPos lecternPos = null;
    private static int villagerId = -1;
    private static int stateTicks = 0;
    private static int attempts = 0;
    private static int openRetries = 0;
    private static String statusText = "Перебор выключен";

    public static boolean isRunning() {
        return state != State.IDLE;
    }

    public static String getStatusText() {
        return statusText;
    }

    public static String start() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return "Зайди в мир!";

        BlockPos foundL = findNearestLectern(mc.level, mc.player.blockPosition(), 4);
        Entity foundV = findNearestVillager(mc.level, mc.player, 5.5);
        if (foundL == null) {
            return "Подойди к Кафедре (в радиусе 4 блоков) и повтори!";
        }
        if (foundV == null) {
            return "Рядом (в радиусе 5.5 блоков) нет Жителя!";
        }
        if (VadimkaConfig.findBestAxeSlot(mc.player) < 0) {
            return "Положи любой Топор в хотбар!";
        }

        lecternPos = foundL;
        villagerId = foundV.getId();
        attempts = 0;
        openRetries = 0;
        state = State.OPEN_TRADE;
        stateTicks = 0;
        String want = VadimkaConfig.TARGET_ENCHANTS.get(VadimkaConfig.targetEnchantIndex);
        statusText = "Старт → " + want + " ≤ " + VadimkaConfig.maxEmeraldPrice;
        VadimkaConfig.narrate("Запускаю перебор библиотекаря: ищу «" + want + "» до " + VadimkaConfig.maxEmeraldPrice + " изумр. (Shift — стоп).");
        return "Запустил перебор на «" + want + "» ≤ " + VadimkaConfig.maxEmeraldPrice + " изумр.!";
    }

    public static void stop(String reason) {
        if (state != State.IDLE) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null) {
                try { mc.gameMode.stopDestroyBlock(); } catch (Throwable ignored) {}
            }
        }
        state = State.IDLE;
        stateTicks = 0;
        statusText = reason;
    }

    public static void tick(Minecraft mc, ClientLevel level, Player player) {
        if (state == State.IDLE || mc.gameMode == null || mc.player == null) return;

        if (mc.options.keyShift.isDown()) {
            stop("Остановлено (Shift)");
            VadimkaConfig.narrate("Остановил перебор кафедры.");
            return;
        }
        if (mc.screen instanceof VadimkaScreen) return;

        Entity villager = level.getEntity(villagerId);
        if (villager == null || villager.isRemoved() || villager.distanceToSqr(player) > 49.0) {
            stop("Житель ушёл далеко");
            VadimkaConfig.narrate("Остановил перебор: житель отошёл слишком далеко.");
            return;
        }

        stateTicks++;

        switch (state) {
            case OPEN_TRADE -> {
                statusText = "#" + attempts + " • Открываю торги...";
                if (player.containerMenu instanceof MerchantMenu) {
                    state = State.WAIT_OFFERS;
                    stateTicks = 0;
                    openRetries = 0;
                    return;
                }
                if (stateTicks == 1 || stateTicks % 14 == 0) {
                    VadimkaConfig.aimAtEntity(player, villager);
                    mc.gameMode.interact(player, villager, InteractionHand.MAIN_HAND);
                    player.swing(InteractionHand.MAIN_HAND);
                    openRetries++;
                }
                // Если житель упорно не открывает торги (например безработный), переходим к переустановке кафедры ТОЛЬКО после 3 попыток!
                if (openRetries >= 3 && stateTicks >= 42) {
                    openRetries = 0;
                    if (level.getBlockState(lecternPos).is(Blocks.LECTERN)) {
                        VadimkaConfig.narrate("Житель пока без профессии — переставляю кафедру...");
                        state = State.BREAK_LECTERN;
                    } else {
                        state = State.PLACE_LECTERN;
                    }
                    stateTicks = 0;
                }
            }

            case WAIT_OFFERS -> {
                statusText = "#" + attempts + " • Читаю книги...";
                if (!(player.containerMenu instanceof MerchantMenu menu)) {
                    if (stateTicks > 18) {
                        state = State.OPEN_TRADE;
                        stateTicks = 0;
                    }
                    return;
                }
                MerchantOffers offers = menu.getOffers();
                // ВАЖНО: ждём до 36 тиков, пока сервер пришлёт пакет с реальными сделками!
                if (offers == null || offers.isEmpty()) {
                    if (stateTicks >= 36) {
                        mc.player.closeContainer();
                        state = State.OPEN_TRADE;
                        stateTicks = 0;
                    }
                    return;
                }

                // Пакет пришёл! Проверяем ВСЕ сделки
                String want = VadimkaConfig.TARGET_ENCHANTS.get(VadimkaConfig.targetEnchantIndex);
                StringBuilder seen = new StringBuilder();
                for (MerchantOffer offer : offers) {
                    String res = VadimkaConfig.formatItemWithEnchants(offer.getResult());
                    int price = offer.getCostA().is(Items.EMERALD) ? offer.getCostA().getCount() : 99;
                    if (offer.getResult().is(Items.ENCHANTED_BOOK)) {
                        if (!seen.isEmpty()) seen.append(", ");
                        seen.append(res).append(" (").append(price).append("из.)");
                    }
                    if (VadimkaConfig.matchesTargetEnchant(res, want) && price <= VadimkaConfig.maxEmeraldPrice) {
                        String win = "★ ПОЙМАНО (#" + attempts + "): " + res + " за " + price + " изумр.!";
                        stop(win);
                        VadimkaConfig.narrate(win);
                        player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                        return;
                    }
                }

                // Не подошло — закрываем торги и ломаем кафедру
                attempts++;
                statusText = "#" + attempts + " • Выпало: " + (seen.isEmpty() ? "без книг" : seen);
                mc.player.closeContainer();
                state = State.BREAK_LECTERN;
                stateTicks = 0;
            }

            case BREAK_LECTERN -> {
                statusText = "#" + attempts + " • Ломаю кафедру...";
                if (!level.getBlockState(lecternPos).is(Blocks.LECTERN)) {
                    mc.gameMode.stopDestroyBlock();
                    state = State.PICKUP_LECTERN;
                    stateTicks = 0;
                    return;
                }
                int axeSlot = VadimkaConfig.findBestAxeSlot(player);
                if (axeSlot < 0) {
                    stop("Нет топора в хотбаре!");
                    VadimkaConfig.narrate("Остановил перебор: положи Топор в хотбар!");
                    return;
                }
                ItemStack axe = player.getInventory().getItem(axeSlot);
                if (VadimkaConfig.warnAxeDurability && axe.isDamageableItem() && axe.getMaxDamage() > 0) {
                    int left = axe.getMaxDamage() - axe.getDamageValue();
                    if (left <= 10) {
                        stop("Прочность топора <= 10!");
                        VadimkaConfig.narrate("⚠ Остановил перебор, чтобы не сломать топор (осталось " + left + ")!");
                        return;
                    }
                }
                VadimkaConfig.selectHotbarSlot(player, axeSlot);
                VadimkaConfig.aimAtBlock(player, lecternPos);
                if (stateTicks == 1) {
                    mc.gameMode.startDestroyBlock(lecternPos, Direction.UP);
                } else {
                    mc.gameMode.continueDestroyBlock(lecternPos, Direction.UP);
                }
                player.swing(InteractionHand.MAIN_HAND);
            }

            case PICKUP_LECTERN -> {
                statusText = "#" + attempts + " • Жду кафедру...";
                int hotbarLec = VadimkaConfig.findHotbarSlot(player, Items.LECTERN);
                if (hotbarLec >= 0 && stateTicks >= 6) {
                    state = State.PLACE_LECTERN;
                    stateTicks = 0;
                    return;
                }
                // Если кафедра упала в основной инвентарь — перекидываем в свободный/текущий слот хотбара!
                int invLec = VadimkaConfig.findInventorySlot(player, Items.LECTERN);
                if (invLec >= 9 && stateTicks >= 8) {
                    int targetHotbar = 8;
                    mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, invLec, targetHotbar, ClickType.SWAP, player);
                    state = State.PLACE_LECTERN;
                    stateTicks = 0;
                    return;
                }
                if (stateTicks > 55) {
                    stop("Кафедра укатилась");
                    VadimkaConfig.narrate("Кафедра не подобралась — подбери её и запусти снова!");
                }
            }

            case PLACE_LECTERN -> {
                statusText = "#" + attempts + " • Ставлю кафедру...";
                if (level.getBlockState(lecternPos).is(Blocks.LECTERN)) {
                    state = State.WAIT_RECLAIM;
                    stateTicks = 0;
                    return;
                }
                int lecSlot = VadimkaConfig.findHotbarSlot(player, Items.LECTERN);
                if (lecSlot < 0) {
                    state = State.PICKUP_LECTERN;
                    stateTicks = 0;
                    return;
                }
                VadimkaConfig.selectHotbarSlot(player, lecSlot);
                BlockPos below = lecternPos.below();
                VadimkaConfig.aimAtBlock(player, below);
                BlockHitResult hit = new BlockHitResult(
                    new Vec3(below.getX() + 0.5, below.getY() + 1.0, below.getZ() + 0.5),
                    Direction.UP, below, false
                );
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
                player.swing(InteractionHand.MAIN_HAND);
                state = State.WAIT_RECLAIM;
                stateTicks = 0;
            }

            case WAIT_RECLAIM -> {
                int waitTarget = 32; // Даём жителю честные 1.6 секунды взять новую профессию!
                statusText = "#" + attempts + " • Житель берёт кафедру (" + Math.max(0, waitTarget - stateTicks) + ")...";
                if (stateTicks >= waitTarget) {
                    openRetries = 0;
                    state = State.OPEN_TRADE;
                    stateTicks = 0;
                }
            }

            default -> {}
        }
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
}
