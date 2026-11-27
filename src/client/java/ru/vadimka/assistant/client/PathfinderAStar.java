package ru.vadimka.assistant.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Настоящий 3D A*-патфайндер:
 * - Объезжает стены, деревья, заборы, дома, лаву, огонь, кактусы и обрывы >= 4 блоков
 * - Умеет забираться на ступеньки (+1 блок) и аккуратно спрыгивать (-1..-3 блока)
 * - Умеет переплывать реки и озёра (удерживая пробел на поверхности воды)
 * - Автоматически прокладывает цепочки локальных маршрутов на дистанции 1500+ блоков
 * - Рисует неоновую 3D-дорожку маршрута и мгновенно отдаёт управление по S/Shift
 */
public final class PathfinderAStar {
    private PathfinderAStar() {}

    private static final int[][] DIRS = {
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
    };

    private static class Node {
        final BlockPos pos;
        final Node parent;
        final double g;
        final double f;

        Node(BlockPos pos, Node parent, double g, double f) {
            this.pos = pos;
            this.parent = parent;
            this.g = g;
            this.f = f;
        }
    }

    private static boolean active = false;
    private static BlockPos finalGoal = null;
    private static double stopDistance = 2.1;
    private static String goalLabel = "";
    private static List<BlockPos> currentPath = new ArrayList<>();
    private static int pathIndex = 0;
    private static int repathCooldown = 0;
    private static int stuckTicks = 0;
    private static Vec3 lastProgressPos = null;
    private static boolean keysHeld = false;
    private static Runnable onArrivalCallback = null;
    private static int worldTickCounter = 0;

    public static boolean isActive() {
        return active;
    }

    public static List<BlockPos> getCurrentPath() {
        return currentPath;
    }

    public static int getPathIndex() {
        return pathIndex;
    }

    public static String getStatusLine() {
        if (!active || finalGoal == null) return "Выключен";
        Minecraft mc = Minecraft.getInstance();
        int dist = mc.player != null ? (int) Math.sqrt(mc.player.blockPosition().distSqr(finalGoal)) : 0;
        int nodesLeft = Math.max(0, currentPath.size() - pathIndex);
        return "A* → " + goalLabel + " (" + dist + "м, узлов: " + nodesLeft + ")";
    }

    public static void startPathTo(BlockPos target, double arrivalDist, String label, Runnable onArrival) {
        if (target == null) return;
        finalGoal = target.immutable();
        stopDistance = Math.max(1.2, arrivalDist);
        goalLabel = (label == null || label.isEmpty()) ? "Цель" : label;
        onArrivalCallback = onArrival;
        active = true;
        currentPath.clear();
        pathIndex = 0;
        repathCooldown = 0;
        stuckTicks = 0;
        lastProgressPos = null;
    }

    public static void stop(Minecraft mc, boolean notify) {
        boolean wasActive = active;
        active = false;
        finalGoal = null;
        currentPath.clear();
        pathIndex = 0;
        onArrivalCallback = null;
        releaseKeys(mc);
        if (notify && wasActive) {
            VadimkaConfig.showToast("A* Автопилот остановлен", 50);
        }
    }

    public static void releaseKeys(Minecraft mc) {
        if (keysHeld && mc != null && mc.options != null) {
            mc.options.keyUp.setDown(false);
            mc.options.keyJump.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            keysHeld = false;
        }
    }

    public static void tick(Minecraft mc, ClientLevel level, Player player) {
        worldTickCounter++;
        if (!active || finalGoal == null) {
            releaseKeys(mc);
            return;
        }
        if (mc.screen != null) {
            releaseKeys(mc);
            return;
        }

        // Мгновенный возврат управления при нажатии S или Shift
        if (VadimkaConfig.autoReturnControl && (mc.options.keyDown.isDown() || mc.options.keyShift.isDown())) {
            stop(mc, false);
            VadimkaConfig.narrate("Автопилот остановлен — управление полностью у тебя!");
            return;
        }

        // Проверяем прибытие (2D дистанция + разумная высота)
        double dxGoal = (finalGoal.getX() + 0.5) - player.getX();
        double dzGoal = (finalGoal.getZ() + 0.5) - player.getZ();
        double dist2D = Math.sqrt(dxGoal * dxGoal + dzGoal * dzGoal);
        if (dist2D <= stopDistance) {
            Runnable cb = onArrivalCallback;
            String finishedLabel = goalLabel;
            stop(mc, false);
            if (cb != null) {
                cb.run();
            } else {
                VadimkaConfig.narrate("Прибыл к точке «" + finishedLabel + "»!");
            }
            return;
        }

        // Пересчитываем A* путь, если пусто, дошли до конца локального отрезка или застряли
        if (repathCooldown > 0) repathCooldown--;
        if ((currentPath.isEmpty() || pathIndex >= currentPath.size()) && repathCooldown <= 0) {
            recomputeAStarLeg(level, player.blockPosition());
            repathCooldown = 14;
        }

        // Проверка застревания каждые 10 тиков
        if (worldTickCounter % 10 == 0) {
            Vec3 now = player.position();
            if (lastProgressPos != null && now.distanceToSqr(lastProgressPos) < 0.09) {
                stuckTicks += 10;
                if (stuckTicks >= 20) {
                    stuckTicks = 0;
                    recomputeAStarLeg(level, player.blockPosition());
                    repathCooldown = 12;
                }
            } else {
                stuckTicks = 0;
            }
            lastProgressPos = now;
        }

        // Визуализация проложенного A*-маршрута в мире (раз в 5 тиков)
        if (worldTickCounter % 5 == 0 && !currentPath.isEmpty()) {
            int end = Math.min(currentPath.size(), pathIndex + 16);
            for (int i = pathIndex; i < end; i++) {
                BlockPos node = currentPath.get(i);
                level.addParticle(
                    new DustParticleOptions(0x22D3EE, 0.9F),
                    node.getX() + 0.5, node.getY() + 0.25, node.getZ() + 0.5,
                    0.0, 0.01, 0.0
                );
            }
        }

        // Выбираем текущий целевой узел маршрута
        BlockPos steerTarget = finalGoal;
        if (!currentPath.isEmpty() && pathIndex < currentPath.size()) {
            // Пропускаем уже достигнутые узлы
            while (pathIndex < currentPath.size()) {
                BlockPos wp = currentPath.get(pathIndex);
                double wdx = (wp.getX() + 0.5) - player.getX();
                double wdz = (wp.getZ() + 0.5) - player.getZ();
                double wdy = Math.abs(wp.getY() - player.getY());
                if ((wdx * wdx + wdz * wdz) < 0.55 && wdy <= 1.6) {
                    pathIndex++;
                } else {
                    break;
                }
            }
            if (pathIndex < currentPath.size()) {
                steerTarget = currentPath.get(pathIndex);
            }
        }

        // Поворачиваемся плавно к узлу A*
        double tx = steerTarget.getX() + 0.5;
        double tz = steerTarget.getZ() + 0.5;
        double sdx = tx - player.getX();
        double sdz = tz - player.getZ();
        float desiredYaw = (float) (Mth.atan2(sdz, sdx) * (180.0 / Math.PI)) - 90.0F;
        float yawDelta = Mth.wrapDegrees(desiredYaw - player.getYRot());
        float maxTurn = VadimkaConfig.smoothCameraTurn ? 15.0F : 26.0F;
        player.setYRot(player.getYRot() + Mth.clamp(yawDelta, -maxTurn, maxTurn));

        // Прыжок нужен если следующий узел выше, или игрок в воде, или упёрся в ступеньку
        boolean stepUp = steerTarget.getY() > Mth.floor(player.getY()) + 0.2;
        boolean inWater = player.isInWater();
        boolean needJump = inWater || stepUp || player.horizontalCollision;

        // Если нужно резко развернуться (> 75°), притормаживаем вперёд на 2 тика, чтобы не свалиться с тропы
        boolean moveForward = Math.abs(yawDelta) < 75.0F;
        mc.options.keyUp.setDown(moveForward);
        mc.options.keyJump.setDown(needJump);
        mc.options.keyLeft.setDown(false);
        mc.options.keyRight.setDown(false);

        if (moveForward && VadimkaConfig.sprintAssist && !inWater && Math.abs(yawDelta) < 28.0F
                && player.getFoodData().getFoodLevel() > 6) {
            player.setSprinting(true);
        }
        keysHeld = true;
    }

    private static void recomputeAStarLeg(ClientLevel level, BlockPos start) {
        if (finalGoal == null) return;

        // Если финальная цель далеко (> 36 блоков), строим локальную точку в направлении цели на 32 блока
        double dx = finalGoal.getX() - start.getX();
        double dz = finalGoal.getZ() - start.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);
        BlockPos legGoal;
        if (dist > 34.0) {
            int lx = start.getX() + (int) Math.round((dx / dist) * 32.0);
            int lz = start.getZ() + (int) Math.round((dz / dist) * 32.0);
            int ly = findReasonableY(level, lx, lz, start.getY());
            legGoal = new BlockPos(lx, ly, lz);
        } else {
            legGoal = finalGoal;
        }

        List<BlockPos> found = runAStar3D(level, start, legGoal, 900);
        if (!found.isEmpty()) {
            currentPath = found;
            pathIndex = 0;
        }
    }

    private static int findReasonableY(ClientLevel level, int x, int z, int refY) {
        for (int dy = 0; dy <= 10; dy++) {
            BlockPos up = new BlockPos(x, refY + dy, z);
            if (isStandable(level, up)) return up.getY();
            BlockPos down = new BlockPos(x, refY - dy, z);
            if (isStandable(level, down)) return down.getY();
        }
        return refY;
    }

    private static List<BlockPos> runAStar3D(ClientLevel level, BlockPos start, BlockPos goal, int maxNodes) {
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.f));
        Map<BlockPos, Double> bestG = new HashMap<>();
        Set<BlockPos> closed = new HashSet<>();

        Node startNode = new Node(start, null, 0.0, heuristic(start, goal));
        open.add(startNode);
        bestG.put(start, 0.0);

        Node closestNode = startNode;
        double closestH = heuristic(start, goal);
        int expanded = 0;

        while (!open.isEmpty() && expanded < maxNodes) {
            Node cur = open.poll();
            if (!closed.add(cur.pos)) continue;
            expanded++;

            double curH = heuristic(cur.pos, goal);
            if (curH < closestH) {
                closestH = curH;
                closestNode = cur;
            }
            if (curH <= 1.8) {
                return reconstructPath(cur);
            }

            for (int[] d : DIRS) {
                int nx = cur.pos.getX() + d[0];
                int nz = cur.pos.getZ() + d[1];
                boolean diagonal = (d[0] != 0 && d[1] != 0);

                // Запрещаем срезать угол сквозь стену по диагонали!
                if (diagonal) {
                    BlockPos sideA = new BlockPos(cur.pos.getX() + d[0], cur.pos.getY(), cur.pos.getZ());
                    BlockPos sideB = new BlockPos(cur.pos.getX(), cur.pos.getY(), cur.pos.getZ() + d[1]);
                    if (!isPassableBody(level, sideA) || !isPassableBody(level, sideB)) {
                        continue;
                    }
                }

                // Ищем допустимую высоту Y: 0 (ровно), +1 (прыжок), -1..-3 (безопасный спуск)
                BlockPos nextPos = findStepPos(level, cur.pos, nx, nz);
                if (nextPos == null || closed.contains(nextPos)) continue;

                double stepCost = diagonal ? 1.42 : 1.0;
                if (nextPos.getY() > cur.pos.getY()) stepCost += 0.45;
                if (level.getBlockState(nextPos).is(Blocks.WATER)) stepCost += 1.6; // Предпочитаем сушу, но плывём если нужно!

                double ng = cur.g + stepCost;
                Double prevG = bestG.get(nextPos);
                if (prevG == null || ng < prevG) {
                    bestG.put(nextPos, ng);
                    open.add(new Node(nextPos, cur, ng, ng + heuristic(nextPos, goal) * 1.15));
                }
            }
        }

        // Если до точной цели не дотянулись за бюджет узлов — возвращаем путь к самой ближней доступной точке!
        return reconstructPath(closestNode);
    }

    private static BlockPos findStepPos(ClientLevel level, BlockPos from, int nx, int nz) {
        int y = from.getY();
        // 1. Ровно (Y)
        BlockPos same = new BlockPos(nx, y, nz);
        if (isStandable(level, same)) return same;

        // 2. Шаг вверх (+1) — проверяем потолок над текущей позицией!
        BlockPos up = new BlockPos(nx, y + 1, nz);
        if (isPassable(level, from.above(2)) && isStandable(level, up)) return up;

        // 3. Безопасный спуск (-1..-3) — проверяем что над целевой колонкой не стена!
        if (isPassableBody(level, same)) {
            for (int drop = 1; drop <= 3; drop++) {
                BlockPos down = new BlockPos(nx, y - drop, nz);
                if (isStandable(level, down)) return down;
                if (!isPassable(level, down)) break;
            }
        }
        return null;
    }

    private static boolean isStandable(ClientLevel level, BlockPos feet) {
        BlockState feetSt = level.getBlockState(feet);
        BlockState headSt = level.getBlockState(feet.above());
        BlockState belowSt = level.getBlockState(feet.below());

        if (isHazardous(feetSt) || isHazardous(headSt) || isHazardous(belowSt)) {
            return false;
        }
        // Плавание по поверхности воды допустимо
        if (feetSt.is(Blocks.WATER) && isPassable(level, feet.above())) {
            return true;
        }
        if (!isPassable(level, feet) || !isPassable(level, feet.above())) {
            return false;
        }
        // Пол должен быть твёрдым (не воздух и не вода под ногами для ходьбы)
        return !belowSt.getCollisionShape(level, feet.below()).isEmpty();
    }

    private static boolean isPassableBody(ClientLevel level, BlockPos feet) {
        return isPassable(level, feet) && isPassable(level, feet.above());
    }

    private static boolean isPassable(ClientLevel level, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (isHazardous(st)) return false;
        if (st.is(Blocks.WATER)) return true;
        return st.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean isHazardous(BlockState st) {
        return st.is(Blocks.LAVA) || st.is(Blocks.FIRE) || st.is(Blocks.SOUL_FIRE)
            || st.is(Blocks.MAGMA_BLOCK) || st.is(Blocks.CACTUS)
            || st.is(Blocks.SWEET_BERRY_BUSH) || st.is(Blocks.CAMPFIRE)
            || st.is(Blocks.SOUL_CAMPFIRE);
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        double dx = a.getX() - b.getX();
        double dy = (a.getY() - b.getY()) * 0.5;
        double dz = a.getZ() - b.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static List<BlockPos> reconstructPath(Node end) {
        List<BlockPos> list = new ArrayList<>();
        Node c = end;
        while (c != null && c.parent != null) {
            list.add(c.pos);
            c = c.parent;
        }
        Collections.reverse(list);
        return list;
    }
}
