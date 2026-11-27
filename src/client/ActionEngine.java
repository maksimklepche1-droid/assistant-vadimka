package ru.vadimka.assistant.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Полноценный движок действий и живого комментирования команд Вадимки:
 * - беги / иди / к дому / вперёд <N> (через 3D A* обход препятствий!)
 * - плыви (автоматическое плавание по поверхности воды)
 * - копай (безопасная проходка вперёд 2 блока в высоту с авто-выбором инструмента)
 * - добудь <дерево/камень/железо/уголь/алмаз/золото/медь/песок/землю> (A* подход + добыча!)
 * - ударь (мгновенная атака ближайшего врага лучшим оружием)
 * - убей <моб/враг> (преследование через A* + критические удары по кулдауну + отход от крипера!)
 * - перебор <чары> / деревня / дворец / крепость / трек / стоп
 */
public final class ActionEngine {
    private ActionEngine() {}

    public enum TaskType {
        NONE("Свободен"),
        SWIMMING("Плыву"),
        DIG_TUNNEL("Копаю вперёд"),
        MINE_RESOURCE("Добываю ресурс"),
        COMBAT_HUNT("Охочусь / Атакую");

        public final String title;
        TaskType(String title) { this.title = title; }
    }

    private static TaskType currentTask = TaskType.NONE;
    private static String activeTaskDetail = "";

    // Состояние для SWIMMING
    private static int swimTicksLeft = 0;

    // Состояние для DIG_TUNNEL и MINE_RESOURCE
    private static String resourceQuery = "";
    private static BlockPos miningTargetPos = null;
    private static boolean miningStarted = false;
    private static int mineTicks = 0;
    private static int minedCount = 0;

    // Состояние для COMBAT_HUNT
    private static String huntQuery = "";
    private static int huntEntityId = -1;
    private static int huntRetargetTicks = 0;

    public static TaskType getCurrentTask() {
        return currentTask;
    }

    public static String getActiveSummary() {
        if (currentTask != TaskType.NONE) {
            return currentTask.title + (activeTaskDetail.isEmpty() ? "" : (": " + activeTaskDetail));
        }
        if (PathfinderAStar.isActive()) {
            return PathfinderAStar.getStatusLine();
        }
        if (VillagerRoller.isRunning()) {
            return "Библиотекарь: " + VillagerRoller.getStatusText();
        }
        return "Готов к командам (!помощь)";
    }

    public static void stopAll(Minecraft mc, boolean announce) {
        boolean hadSomething = currentTask != TaskType.NONE || PathfinderAStar.isActive() || VillagerRoller.isRunning();
        currentTask = TaskType.NONE;
        activeTaskDetail = "";
        miningTargetPos = null;
        miningStarted = false;
        huntEntityId = -1;
        swimTicksLeft = 0;
        if (mc != null) {
            if (mc.options != null) {
                mc.options.keyUp.setDown(false);
                mc.options.keyJump.setDown(false);
                mc.options.keyDown.setDown(false);
            }
            if (mc.gameMode != null) {
                try { mc.gameMode.stopDestroyBlock(); } catch (Throwable ignored) {}
            }
            PathfinderAStar.stop(mc, false);
        }
        VillagerRoller.stop("Остановлено");
        if (announce && hadSomething) {
            VadimkaConfig.narrate("Все задачи остановлены. Управление полностью у тебя!");
        }
    }

    // =========================================================================
    // РАЗБОР КОМАНД ИЗ ЧАТА И КНОПОК GUI
    // =========================================================================

    public static String executeCommand(String raw) {
        Minecraft mc = Minecraft.getInstance();
        Player p = mc.player;
        if (p == null || mc.level == null) return "Зайди в мир!";

        String low = raw.toLowerCase(Locale.ROOT)
            .replace("вадимка", "")
            .replace("вадим", "")
            .replace("vadimka", "")
            .replace(",", " ")
            .trim();

        if (low.isEmpty() || low.equals("привет") || low.equals("ку") || low.equals("тут")) {
            return "На связи! Командуй: «беги вперёд», «плыви», «копай», «добудь дерево/железо/алмаз», «ударь», «убей зомби», «!к дому», «!деревня», «!испытания», «!перебор починка», «!стоп»!";
        }
        if (low.contains("помощ") || low.contains("команд") || low.contains("help")) {
            return "Действия: !беги / !к дому / !плыви / !копай / !добудь дерево (камень, железо, уголь, алмаз) / !ударь / !убей зомби / !перебор починка / !деревня / !испытания / !открой / !трек / !стоп!";
        }

        // 1. СТОП
        if (low.equals("стоп") || low.equals("стой") || low.contains("остановис") || low.contains("отмен") || low.contains("хватит")) {
            stopAll(mc, false);
            return "Остановил все задачи (A*, добычу, бой, перебор). Управление у тебя!";
        }

        // 2. ПЛЫВИ
        if (low.contains("плыв") || low.contains("swim")) {
            stopAll(mc, false);
            currentTask = TaskType.SWIMMING;
            swimTicksLeft = 400; // 20 секунд или пока не выйдет на берег / не нажмёт S/Shift
            activeTaskDetail = "держусь на поверхности";
            VadimkaConfig.narrate("Команда принята: ПЛЫВУ вперёд! Удерживаю поверхность воды (S/Shift — стоп).");
            return "Плыву вперёд! Выйду на берег — остановлюсь сам (или нажми S/Shift).";
        }

        // 3. КОПАЙ (туннель / перед собой)
        if (low.startsWith("копай") || low.contains(" копай") || low.contains("туннел")) {
            stopAll(mc, false);
            currentTask = TaskType.DIG_TUNNEL;
            miningTargetPos = null;
            miningStarted = false;
            minedCount = 0;
            activeTaskDetail = "проходка перед собой";
            VadimkaConfig.narrate("Команда принята: КОПАЮ вперёд! Выбираю инструмент и слежу за лавой (Shift — стоп).");
            return "Начинаю копать туннель перед собой! Остановлюсь, если впереди лава или ты нажмёшь Shift.";
        }

        // 4. ДОБУДЬ <ресурс>
        if (low.contains("добудь") || low.contains("добывай") || low.contains("накопай") || low.contains("сруби")) {
            stopAll(mc, false);
            String res = low
                .replace("добудь", "").replace("добывай", "").replace("накопай", "").replace("сруби", "")
                .replace("мне", "").replace("немного", "").replace("пожалуйста", "").trim();
            if (res.isEmpty()) res = "дерево";
            currentTask = TaskType.MINE_RESOURCE;
            resourceQuery = res;
            miningTargetPos = null;
            miningStarted = false;
            minedCount = 0;
            activeTaskDetail = res;
            VadimkaConfig.narrate("Команда принята: ДОБЫВАЮ «" + res + "»! Ищу ближайший блок и иду к нему по A*...");
            return "Ищу и добываю «" + res + "» вокруг тебя! Буду обходить препятствия и менять инструменты (Shift — стоп).";
        }

        // 5. УДАРЬ (разовая атака ближайшего)
        if (low.contains("удар") || low.contains("бей")) {
            return performSingleStrike(mc, mc.level, p);
        }

        // 6. УБЕЙ <моб / враг>
        if (low.contains("убей") || low.contains("атакуй") || low.contains("завали") || low.contains("охот")) {
            stopAll(mc, false);
            String target = low
                .replace("убей", "").replace("атакуй", "").replace("завали", "").replace("охот", "")
                .replace("ближайшего", "").replace("моба", "").replace("врага", "").trim();
            currentTask = TaskType.COMBAT_HUNT;
            huntQuery = target;
            huntEntityId = -1;
            huntRetargetTicks = 0;
            activeTaskDetail = target.isEmpty() ? "ближайший враг" : target;
            VadimkaConfig.narrate("Команда принята: АТАКУЮ «" + activeTaskDetail + "»! Беру оружие и иду в бой (Shift — стоп).");
            return "Ищу цель «" + activeTaskDetail + "», преследую по A* и бью в полную силу (Shift — стоп)!";
        }

        // 7. БЕГИ / ИДИ / К ДОМУ / ВЕДИ
        if (low.equals("дом") || low.contains("задай дом") || low.contains("сохрани дом") || low.contains("тут дом")) {
            VadimkaConfig.homeWaypoint = p.blockPosition().immutable();
            VadimkaConfig.setNavigationTarget(VadimkaConfig.homeWaypoint, "Дом", 0xFF22D3EE);
            VadimkaConfig.narrate("Сохранил точку Дома (" + VadimkaConfig.homeWaypoint.getX() + ", " + VadimkaConfig.homeWaypoint.getY() + ", " + VadimkaConfig.homeWaypoint.getZ() + ")!");
            return "Дом сохранён! Пиши «!к дому» — приведу туда с обходом стен и деревьев!";
        }
        if (low.contains("к дому") || low.contains("домой")) {
            if (VadimkaConfig.homeWaypoint == null) {
                return "Точка Дома ещё не задана! Встань на базе и напиши «!дом».";
            }
            stopAll(mc, false);
            VadimkaConfig.setNavigationTarget(VadimkaConfig.homeWaypoint, "Дом", 0xFF22D3EE);
            PathfinderAStar.startPathTo(VadimkaConfig.homeWaypoint, 2.2, "Дом", null);
            int dist = (int) Math.sqrt(p.blockPosition().distSqr(VadimkaConfig.homeWaypoint));
            VadimkaConfig.narrate("Прокладываю A*-маршрут ДОМОЙ (" + dist + "м)! Обегаю стены, деревья и обрывы.");
            return "▶ Бегу домой по A* (" + dist + "м)! Нажми S или Shift для остановки.";
        }
        if (low.contains("беги") || low.startsWith("иди") || low.contains(" иди") || low.contains("веди") || low.contains("автопилот")) {
            if (low.contains("деревн")) {
                stopAll(mc, false);
                StructureLocator.locate(VadimkaConfig.StructureTarget.VILLAGE, true);
                return "Ищу Деревню и запускаю A*-автопилот к ней!";
            }
            if (low.contains("дворец") || low.contains("испытан")) {
                stopAll(mc, false);
                StructureLocator.locate(VadimkaConfig.StructureTarget.TRIAL_CHAMBER, true);
                return "Ищу Дворец испытаний и запускаю A*-автопилот!";
            }
            if (VadimkaConfig.activeNavTarget != null && !low.contains("вперёд") && !low.contains("вперед")) {
                stopAll(mc, false);
                PathfinderAStar.startPathTo(VadimkaConfig.activeNavTarget, 2.4, VadimkaConfig.activeNavTitle, null);
                VadimkaConfig.narrate("Бегу по A* к «" + VadimkaConfig.activeNavTitle + "»!");
                return "▶ A*-автопилот запущен к «" + VadimkaConfig.activeNavTitle + "»!";
            }
            // Бежим вперёд на 25 блоков по взгляду игрока с обходом преград!
            stopAll(mc, false);
            Vec3 look = p.getLookAngle();
            BlockPos ahead = BlockPos.containing(p.getX() + look.x * 25.0, p.getY(), p.getZ() + look.z * 25.0);
            VadimkaConfig.setNavigationTarget(ahead, "Точка вперёд", 0xFF4ADE80);
            PathfinderAStar.startPathTo(ahead, 2.2, "Вперёд 25м", null);
            VadimkaConfig.narrate("Команда принята: БЕГУ вперёд (25м) с обходом преград!");
            return "▶ Бегу вперёд на 25 блоков по A* (S/Shift — стоп)!";
        }

        // 8. ПЕРЕБОР ЖИТЕЛЯ
        if (low.contains("перебор") || low.contains("кафедр") || low.contains("библиотекар") || low.contains("сброс торг")) {
            stopAll(mc, false);
            for (int i = 0; i < VadimkaConfig.TARGET_ENCHANTS.size(); i++) {
                String eLow = VadimkaConfig.TARGET_ENCHANTS.get(i).toLowerCase(Locale.ROOT);
                if (low.contains(eLow.substring(0, Math.min(5, eLow.length())))) {
                    VadimkaConfig.targetEnchantIndex = i;
                    break;
                }
            }
            return VillagerRoller.start();
        }

        // 9. СТРУКТУРЫ
        if (low.contains("деревн") || low.contains("village")) { StructureLocator.locate(VadimkaConfig.StructureTarget.VILLAGE, false); return "Ищу Деревню..."; }
        if (low.contains("испытан") || low.contains("дворец") || low.contains("данж") || low.contains("trial")) { StructureLocator.locate(VadimkaConfig.StructureTarget.TRIAL_CHAMBER, false); return "Ищу Дворец испытаний..."; }
        if (low.contains("крепост") || low.contains("энд") || low.contains("stronghold")) { StructureLocator.locate(VadimkaConfig.StructureTarget.STRONGHOLD, false); return "Ищу Крепость Энда..."; }
        if (low.contains("город") || low.contains("древн") || low.contains("ancient")) { StructureLocator.locate(VadimkaConfig.StructureTarget.ANCIENT_CITY, false); return "Ищу Древний город..."; }
        if (low.contains("особняк") || low.contains("mansion")) { StructureLocator.locate(VadimkaConfig.StructureTarget.MANSION, false); return "Ищу Лесной особняк..."; }
        if (low.contains("аванпост") || low.contains("outpost")) { StructureLocator.locate(VadimkaConfig.StructureTarget.OUTPOST, false); return "Ищу Аванпост..."; }
        if (low.contains("монумент") || low.contains("океан") || low.contains("monument")) { StructureLocator.locate(VadimkaConfig.StructureTarget.MONUMENT, false); return "Ищу Монумент океана..."; }
        if (low.contains("портал") || low.contains("portal")) { StructureLocator.locate(VadimkaConfig.StructureTarget.RUINED_PORTAL, false); return "Ищу Портал в Незер..."; }

        // 10. ХРАНИЛИЩА И ЛАРЧИКИ
        if (low.contains("открой") || low.contains("выбей") || low.contains("хранилищ")) {
            return VaultHelper.openNearestManually();
        }
        if (low.contains("найд") || low.contains("ларчик") || low.contains("сундук")) {
            String q = low.replace("найди", "").replace("найти", "").replace("ларчик", "").replace("сундук", "").trim();
            if (VadimkaConfig.rememberedChests.isEmpty()) {
                return "Сначала открой сундуки ПКМ — я запомню лут! Рядом найдено ларчиков: " + VaultHelper.getNearbyVaults().size();
            }
            for (VadimkaConfig.ChestRecord cr : VadimkaConfig.rememberedChests.values()) {
                for (var e : cr.items.entrySet()) {
                    if (q.isEmpty() || e.getKey().toLowerCase(Locale.ROOT).contains(q)) {
                        VadimkaConfig.setNavigationTarget(cr.pos, e.getKey(), 0xFFF59E0B);
                        return "Нашёл «" + e.getKey() + "» (x" + e.getValue() + ") — маяк поставлен! Напиши «!иди», чтобы дойти по A*.";
                    }
                }
            }
            return "Вещь «" + q + "» в запомненных сундуках не найдена.";
        }

        // 11. SPOTIFY
        if (low.contains("следующ") || low.contains("next") || low.contains("скип")) { SpotifyBridge.sendCommand("NEXT"); return "Следующий трек в Spotify!"; }
        if (low.contains("предыдущ") || low.contains("prev")) { SpotifyBridge.sendCommand("PREV"); return "Предыдущий трек в Spotify!"; }
        if (low.contains("пауз") || low.contains("плей") || low.contains("play") || low.contains("pause")) { SpotifyBridge.sendCommand("TOGGLE"); return "Play / Pause в Spotify!"; }
        if (low.contains("трек") || low.contains("музык") || low.contains("спотифай") || low.contains("spotify")) {
            return SpotifyBridge.isConnected()
                ? ((SpotifyBridge.isPlaying() ? "▶ Играет: " : "⏸ Пауза: ") + SpotifyBridge.getArtist() + " — " + SpotifyBridge.getTitle())
                : "Включи трек в Spotify на ПК — мост подхватит его автоматически!";
        }

        // 12. СМЕНА ОБЛИКА
        if (low.contains("робот") || low.contains("player")) { VadimkaConfig.currentForm = VadimkaConfig.VadimkaForm.CYBER_PLAYER; return "Облик: Кибер-Робот!"; }
        if (low.contains("крыс") || low.contains("rat")) { VadimkaConfig.currentForm = VadimkaConfig.VadimkaForm.RAT; return "Облик: Кибер-Крыска!"; }
        if (low.contains("аксолотл") || low.contains("axolotl")) { VadimkaConfig.currentForm = VadimkaConfig.VadimkaForm.AXOLOTL; return "Облик: Синий Аксолотль!"; }
        if (low.contains("вихр") || low.contains("breeze")) { VadimkaConfig.currentForm = VadimkaConfig.VadimkaForm.BREEZE; return "Облик: Шторм-Вихрь!"; }
        if (low.contains("варден") || low.contains("хранител") || low.contains("warden")) { VadimkaConfig.currentForm = VadimkaConfig.VadimkaForm.WARDEN; return "Облик: Хранитель!"; }
        if (low.contains("тихон") || low.contains("allay")) { VadimkaConfig.currentForm = VadimkaConfig.VadimkaForm.ALLAY; return "Облик: Тихоня-Дух!"; }

        return "Команда не распознана. Попробуй: !беги, !плыви, !копай, !добудь дерево, !ударь, !убей зомби, !к дому, !деревня, !испытания, !перебор починка, !стоп.";
    }

    // =========================================================================
    // ТИК ВЫПОЛНЕНИЯ ТЕКУЩЕЙ ЗАДАЧИ
    // =========================================================================

    public static void tick(Minecraft mc, ClientLevel level, Player player) {
        if (currentTask == TaskType.NONE) return;
        if (mc.screen != null) return;

        // Экстренная остановка по Shift или S
        if (VadimkaConfig.autoReturnControl && (mc.options.keyShift.isDown() || (currentTask != TaskType.COMBAT_HUNT && mc.options.keyDown.isDown()))) {
            stopAll(mc, true);
            return;
        }

        switch (currentTask) {
            case SWIMMING -> tickSwimming(mc, player);
            case DIG_TUNNEL -> tickDigTunnel(mc, level, player);
            case MINE_RESOURCE -> tickMineResource(mc, level, player);
            case COMBAT_HUNT -> tickCombatHunt(mc, level, player);
            default -> {}
        }
    }

    private static void tickSwimming(Minecraft mc, Player player) {
        swimTicksLeft--;
        mc.options.keyUp.setDown(true);
        mc.options.keyJump.setDown(player.isInWater());
        if (player.isInWater()) {
            player.setSprinting(true);
            activeTaskDetail = "плыву по воде (" + (swimTicksLeft / 20) + "с)";
        } else {
            activeTaskDetail = "иду к воде / берегу";
        }
        if (swimTicksLeft <= 0) {
            stopAll(mc, false);
            VadimkaConfig.narrate("Закончил заплыв!");
        }
    }

    private static void tickDigTunnel(Minecraft mc, ClientLevel level, Player player) {
        if (mc.gameMode == null) return;
        Direction facing = player.getDirection();
        BlockPos feetAhead = player.blockPosition().relative(facing);
        BlockPos headAhead = feetAhead.above();

        // Проверка опасности (лава впереди!)
        if (level.getBlockState(feetAhead).is(Blocks.LAVA) || level.getBlockState(headAhead).is(Blocks.LAVA)
                || level.getBlockState(feetAhead.below()).is(Blocks.LAVA)) {
            stopAll(mc, false);
            VadimkaConfig.narrate("⚠ Впереди ЛАВА! Срочно остановил копание!");
            return;
        }

        BlockPos target = null;
        if (!level.getBlockState(headAhead).isAir() && !level.getBlockState(headAhead).is(Blocks.WATER)) {
            target = headAhead;
        } else if (!level.getBlockState(feetAhead).isAir() && !level.getBlockState(feetAhead).is(Blocks.WATER)) {
            target = feetAhead;
        }

        if (target == null) {
            // Проход чист — шаг вперёд!
            miningStarted = false;
            mc.options.keyUp.setDown(true);
            activeTaskDetail = "иду вперёд (выкопано: " + minedCount + ")";
        } else {
            mc.options.keyUp.setDown(false);
            selectBestToolForBlock(player, level.getBlockState(target));
            VadimkaConfig.aimAtBlock(player, target);
            if (!target.equals(miningTargetPos) || !miningStarted) {
                miningTargetPos = target;
                miningStarted = true;
                mc.gameMode.startDestroyBlock(target, facing.getOpposite());
            } else {
                mc.gameMode.continueDestroyBlock(target, facing.getOpposite());
            }
            player.swing(InteractionHand.MAIN_HAND);
            activeTaskDetail = "ломаю " + level.getBlockState(target).getBlock().getName().getString() + " (" + minedCount + ")";
            if (level.getBlockState(target).isAir()) {
                minedCount++;
                miningStarted = false;
            }
        }
    }

    private static void tickMineResource(Minecraft mc, ClientLevel level, Player player) {
        if (mc.gameMode == null) return;

        // Если текущий целевой блок уже сломан или не выбран — ищем ближайший подходящий блок (шаг = 1!)
        if (miningTargetPos == null || !matchesResourceBlock(level.getBlockState(miningTargetPos), resourceQuery)) {
            if (miningTargetPos != null && level.getBlockState(miningTargetPos).isAir()) {
                minedCount++;
                VadimkaConfig.narrate("Добыл блок! Всего добыто: " + minedCount + ". Ищу следующий «" + resourceQuery + "»...");
            }
            miningStarted = false;
            miningTargetPos = findNearestResourceBlock(level, player.blockPosition(), resourceQuery, 18);
            if (miningTargetPos == null) {
                stopAll(mc, false);
                VadimkaConfig.narrate("Рядом больше нет «" + resourceQuery + "» (добыто: " + minedCount + "). Задача выполнена!");
                return;
            }
            double dist = Math.sqrt(player.blockPosition().distSqr(miningTargetPos));
            if (dist > 3.6) {
                VadimkaConfig.narrate("Нашёл «" + level.getBlockState(miningTargetPos).getBlock().getName().getString() + "» (" + (int) dist + "м) — иду к нему по A*!");
                PathfinderAStar.startPathTo(miningTargetPos, 3.0, "Добыча: " + resourceQuery, null);
            }
        }

        double distSq = player.getEyePosition().distanceToSqr(Vec3.atCenterOf(miningTargetPos));
        if (distSq > 4.2 * 4.2) {
            // Ещё далеко — ждём пока A* подведёт игрока вплотную
            if (!PathfinderAStar.isActive()) {
                PathfinderAStar.startPathTo(miningTargetPos, 3.0, "Добыча: " + resourceQuery, null);
            }
            activeTaskDetail = "иду к «" + resourceQuery + "» (" + (int) Math.sqrt(distSq) + "м)";
            return;
        }

        // Подошли! Останавливаем A* и добываем нужным инструментом
        if (PathfinderAStar.isActive()) {
            PathfinderAStar.stop(mc, false);
        }
        BlockState st = level.getBlockState(miningTargetPos);
        selectBestToolForBlock(player, st);
        VadimkaConfig.aimAtBlock(player, miningTargetPos);
        if (!miningStarted) {
            miningStarted = true;
            mineTicks = 0;
            mc.gameMode.startDestroyBlock(miningTargetPos, Direction.UP);
        } else {
            mc.gameMode.continueDestroyBlock(miningTargetPos, Direction.UP);
        }
        player.swing(InteractionHand.MAIN_HAND);
        mineTicks++;
        activeTaskDetail = "добываю " + st.getBlock().getName().getString() + " (шт: " + minedCount + ")";
        if (mineTicks > 200) {
            // Блок слишком крепкий (например обсидиан рукой) — пропускаем
            miningTargetPos = null;
            miningStarted = false;
        }
    }

    private static String performSingleStrike(Minecraft mc, ClientLevel level, Player player) {
        if (mc.gameMode == null) return "Не могу атаковать сейчас.";
        LivingEntity target = findBestTarget(level, player, "", 5.2);
        if (target == null) {
            return "В радиусе удара (5м) никого нет! Напиши «!убей <моб>», чтобы я сам догнал цель.";
        }
        int weaponSlot = VadimkaConfig.findBestSwordOrAxeSlot(player);
        if (weaponSlot >= 0) VadimkaConfig.selectHotbarSlot(player, weaponSlot);
        VadimkaConfig.aimAtEntity(player, target);
        mc.gameMode.attack(player, target);
        player.swing(InteractionHand.MAIN_HAND);
        String name = target.getName().getString();
        VadimkaConfig.narrate("Нанёс удар по «" + name + "»!");
        return "Ударил «" + name + "»!";
    }

    private static void tickCombatHunt(Minecraft mc, ClientLevel level, Player player) {
        if (mc.gameMode == null) return;

        LivingEntity target = null;
        if (huntEntityId >= 0) {
            Entity e = level.getEntity(huntEntityId);
            if (e instanceof LivingEntity le && le.isAlive() && !le.isRemoved() && le.distanceToSqr(player) <= 36.0 * 36.0) {
                target = le;
            }
        }
        if (target == null) {
            if (huntEntityId >= 0) {
                VadimkaConfig.narrate("Цель повержена!");
                huntEntityId = -1;
            }
            target = findBestTarget(level, player, huntQuery, 26.0);
            if (target == null) {
                stopAll(mc, false);
                VadimkaConfig.narrate("В радиусе 26м целей «" + (huntQuery.isEmpty() ? "враги" : huntQuery) + "» больше нет!");
                return;
            }
            huntEntityId = target.getId();
            VadimkaConfig.narrate("Заметил «" + target.getName().getString() + "» (" + (int) target.distanceTo(player) + "м) — атакую!");
        }

        double dist = target.distanceTo(player);
        activeTaskDetail = target.getName().getString() + " (" + String.format("%.1f", dist) + "м)";

        // Уворот назад, если рядом крипер надулся!
        if (target instanceof Creeper cr && cr.isIgnited() && dist < 4.5) {
            PathfinderAStar.stop(mc, false);
            VadimkaConfig.aimAtEntity(player, target);
            mc.options.keyUp.setDown(false);
            mc.options.keyDown.setDown(true);
            VadimkaConfig.narrate("⚠ Крипер взрывается — отступаю назад!");
            return;
        }
        mc.options.keyDown.setDown(false);

        int weaponSlot = VadimkaConfig.findBestSwordOrAxeSlot(player);
        if (weaponSlot >= 0) VadimkaConfig.selectHotbarSlot(player, weaponSlot);

        if (dist > 3.1) {
            // Догоняем цель по A* с обновлением каждые 15 тиков
            huntRetargetTicks++;
            if (!PathfinderAStar.isActive() || huntRetargetTicks >= 15) {
                PathfinderAStar.startPathTo(target.blockPosition(), 2.4, target.getName().getString(), null);
                huntRetargetTicks = 0;
            }
        } else {
            // В дистанции удара — бьём на полном заряде оружия!
            if (PathfinderAStar.isActive()) PathfinderAStar.stop(mc, false);
            VadimkaConfig.aimAtEntity(player, target);
            if (player.getAttackStrengthScale(0.5F) >= 0.9F) {
                mc.gameMode.attack(player, target);
                player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    private static LivingEntity findBestTarget(ClientLevel level, Player player, String query, double maxDist) {
        LivingEntity best = null;
        double bestSq = maxDist * maxDist;
        String q = query == null ? "" : query.toLowerCase(Locale.ROOT).trim();

        for (Entity e : level.entitiesForRendering()) {
            if (!(e instanceof LivingEntity le) || e == player || e.getId() <= -77000 || !le.isAlive()) continue;
            double sq = e.distanceToSqr(player);
            if (sq > bestSq) continue;

            if (q.isEmpty()) {
                if (le instanceof Monster) {
                    bestSq = sq;
                    best = le;
                }
            } else {
                String ruName = le.getName().getString().toLowerCase(Locale.ROOT);
                String typeId = le.getType().toString().toLowerCase(Locale.ROOT);
                boolean match = ruName.contains(q) || typeId.contains(q)
                    || (q.contains("зомб") && typeId.contains("zombie"))
                    || (q.contains("скелет") && typeId.contains("skeleton"))
                    || (q.contains("крипер") && typeId.contains("creeper"))
                    || (q.contains("паук") && typeId.contains("spider"))
                    || (q.contains("свин") && typeId.contains("pig"))
                    || (q.contains("коров") && typeId.contains("cow"))
                    || (q.contains("овц") && typeId.contains("sheep"))
                    || (q.contains("куриц") && typeId.contains("chicken"))
                    || (q.contains("эндер") && typeId.contains("enderman"))
                    || (q.contains("живост") && le instanceof Animal);
                if (match) {
                    bestSq = sq;
                    best = le;
                }
            }
        }
        return best;
    }

    private static BlockPos findNearestResourceBlock(ClientLevel level, BlockPos center, String query, int r) {
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int minY = Math.max(level.getMinY(), center.getY() - 6);
        int maxY = Math.min(level.getMaxY() - 1, center.getY() + 8);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    p.set(center.getX() + dx, y, center.getZ() + dz);
                    if (matchesResourceBlock(level.getBlockState(p), query)) {
                        double sq = p.distSqr(center);
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

    private static boolean matchesResourceBlock(BlockState st, String query) {
        if (st.isAir()) return false;
        Block b = st.getBlock();
        String name = b.getName().getString().toLowerCase(Locale.ROOT);
        String id = b.toString().toLowerCase(Locale.ROOT);
        String q = query.toLowerCase(Locale.ROOT);

        if (q.contains("дерев") || q.contains("бревн") || q.contains("дуб") || q.contains("берез") || q.contains("берёз") || q.contains("ель")) {
            return id.contains("_log") || id.contains("_wood") || name.contains("бревн");
        }
        if (q.contains("камен") || q.contains("булыж")) {
            return st.is(Blocks.STONE) || st.is(Blocks.COBBLESTONE) || st.is(Blocks.DEEPSLATE);
        }
        if (q.contains("желез")) return st.is(Blocks.IRON_ORE) || st.is(Blocks.DEEPSLATE_IRON_ORE);
        if (q.contains("угол")) return st.is(Blocks.COAL_ORE) || st.is(Blocks.DEEPSLATE_COAL_ORE);
        if (q.contains("алмаз")) return st.is(Blocks.DIAMOND_ORE) || st.is(Blocks.DEEPSLATE_DIAMOND_ORE);
        if (q.contains("золот")) return st.is(Blocks.GOLD_ORE) || st.is(Blocks.DEEPSLATE_GOLD_ORE);
        if (q.contains("мед")) return st.is(Blocks.COPPER_ORE) || st.is(Blocks.DEEPSLATE_COPPER_ORE);
        if (q.contains("изумр")) return st.is(Blocks.EMERALD_ORE) || st.is(Blocks.DEEPSLATE_EMERALD_ORE);
        if (q.contains("песок") || q.contains("песк")) return st.is(Blocks.SAND) || st.is(Blocks.RED_SAND);
        if (q.contains("земл") || q.contains("дёрн") || q.contains("дерн")) return st.is(Blocks.DIRT) || st.is(Blocks.GRASS_BLOCK);
        return name.contains(q) || id.contains(q);
    }

    private static void selectBestToolForBlock(Player player, BlockState st) {
        String id = st.getBlock().toString().toLowerCase(Locale.ROOT);
        int slot;
        if (id.contains("log") || id.contains("plank") || id.contains("wood") || st.is(Blocks.LECTERN)) {
            slot = VadimkaConfig.findBestAxeSlot(player);
        } else if (st.is(Blocks.DIRT) || st.is(Blocks.GRASS_BLOCK) || st.is(Blocks.SAND) || st.is(Blocks.GRAVEL) || st.is(Blocks.SNOW_BLOCK)) {
            slot = VadimkaConfig.findBestShovelSlot(player);
        } else {
            slot = VadimkaConfig.findBestPickaxeSlot(player);
        }
        if (slot >= 0) VadimkaConfig.selectHotbarSlot(player, slot);
    }
}
