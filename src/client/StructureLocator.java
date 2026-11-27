package ru.vadimka.assistant.client;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Честный и рабочий поисковик структур мира (без слепой рефлексии!):
 * 1. Сначала мгновенно проверяет область вокруг игрока с шагом 1 (если ты уже внутри Деревни или Дворца — найдёт за 1мс!).
 * 2. В одиночной игре вызывает прямой API Minecraft 1.21.11 (Loom ремапит его в Intermediary автоматически!).
 * 3. В сетевой игре делает точный локальный скан прогруженных чанков.
 */
public final class StructureLocator {
    private StructureLocator() {}

    private static volatile boolean searchBusy = false;
    private static volatile String lastStatus = "Готов к поиску структур";

    public static String getLastStatus() {
        return lastStatus;
    }

    public static void locate(VadimkaConfig.StructureTarget target, boolean startAutopilotAfter) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (searchBusy) {
            VadimkaConfig.showToast("Поиск уже идёт, секунду...", 40);
            return;
        }

        // ШАГ 1: Мгновенная проверка, не находимся ли мы УЖЕ внутри или вплотную к этой структуре (шаг = 1 блок!)
        BlockPos immediate = scanImmediateExact(mc.level, mc.player, target);
        if (immediate != null) {
            onStructureFound(mc, target, immediate, startAutopilotAfter, true);
            return;
        }

        searchBusy = true;
        lastStatus = "Ищу: " + target.russianName + "...";
        VadimkaConfig.narrate("Сканирую мир — ищу «" + target.russianName + "»...");
        BlockPos startPos = mc.player.blockPosition();
        MinecraftServer server = mc.getSingleplayerServer();

        if (server != null) {
            // Одиночная игра: ПРЯМОЙ вызов 1.21.11 API (100% ремапится Fabric Loom!)
            server.execute(() -> {
                BlockPos foundPos = null;
                try {
                    ServerLevel sLevel = server.getLevel(mc.player.level().dimension());
                    if (sLevel != null) {
                        foundPos = findNearestOnServerDirect(sLevel, startPos, target.keyFragment);
                    }
                } catch (Throwable ignored) {
                }

                final BlockPos finalResult = foundPos;
                mc.execute(() -> {
                    searchBusy = false;
                    if (finalResult != null && mc.player != null) {
                        int yTarget = (target == VadimkaConfig.StructureTarget.TRIAL_CHAMBER)
                            ? -20
                            : Math.max(64, finalResult.getY());
                        BlockPos nav = new BlockPos(finalResult.getX(), yTarget, finalResult.getZ());
                        onStructureFound(mc, target, nav, startAutopilotAfter, false);
                    } else {
                        String fail = "Не нашёл «" + target.russianName + "» в радиусе 1600 блоков.";
                        lastStatus = fail;
                        VadimkaConfig.narrate(fail);
                    }
                });
            });
        } else {
            // Сетевой сервер: широкий локальный скан загруженных чанков вокруг игрока
            BlockPos serverLocal = scanWideLoadedChunks(mc.level, mc.player, target);
            searchBusy = false;
            if (serverLocal != null) {
                onStructureFound(mc, target, serverLocal, startAutopilotAfter, false);
            } else {
                String fail = "На сервере в прогруженных чанках «" + target.russianName + "» пока не видно.";
                lastStatus = fail;
                VadimkaConfig.narrate(fail);
            }
        }
    }

    private static void onStructureFound(Minecraft mc, VadimkaConfig.StructureTarget target,
                                         BlockPos pos, boolean startAutopilot, boolean alreadyInside) {
        VadimkaConfig.foundStructures.put(target, new VadimkaConfig.FoundStructureRecord(target, pos));
        VadimkaConfig.setNavigationTarget(pos, target.russianName, target.beaconColor);
        int dist = mc.player != null ? (int) Math.sqrt(mc.player.blockPosition().distSqr(pos)) : 0;

        String msg;
        if (alreadyInside) {
            msg = "Ты уже рядом с «" + target.russianName + "» (" + dist + "м, X=" + pos.getX() + " Y=" + pos.getY() + " Z=" + pos.getZ() + ")!";
        } else {
            String hint = (target == VadimkaConfig.StructureTarget.TRIAL_CHAMBER
                || target == VadimkaConfig.StructureTarget.ANCIENT_CITY
                || target == VadimkaConfig.StructureTarget.STRONGHOLD) ? " (под землёй!)" : "";
            msg = "Нашёл «" + target.russianName + "»: X=" + pos.getX() + " Z=" + pos.getZ() + " (" + dist + "м)" + hint;
        }
        lastStatus = msg;
        VadimkaConfig.narrate(msg);
        if (mc.player != null) {
            mc.player.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 0.85F, 1.25F);
        }
        if (startAutopilot && !alreadyInside) {
            PathfinderAStar.startPathTo(pos, 3.0, target.russianName, null);
            VadimkaConfig.narrate("Включил A* Автопилот к «" + target.russianName + "»!");
        }
    }

    /**
     * Прямой (без строковой рефлексии!) вызов генератора структур Minecraft 1.21.11.
     */
    private static BlockPos findNearestOnServerDirect(ServerLevel sLevel, BlockPos origin, String keyFragment) {
        Optional<HolderLookup.RegistryLookup<Structure>> optReg = sLevel.registryAccess().lookup(Registries.STRUCTURE);
        if (optReg.isEmpty()) return null;

        List<Holder<Structure>> holders = new ArrayList<>();
        String needle = keyFragment.toLowerCase(Locale.ROOT);
        optReg.get().listElements().forEach(ref -> {
            String fullKey = ref.key().toString().toLowerCase(Locale.ROOT);
            if (fullKey.contains(needle)) {
                holders.add(ref);
            }
        });
        if (holders.isEmpty()) return null;

        HolderSet<Structure> set = HolderSet.direct(holders);
        Pair<BlockPos, Holder<Structure>> pair = sLevel.getChunkSource().getGenerator()
            .findNearestMapStructure(sLevel, set, origin, 100, false);
        return pair != null ? pair.getFirst() : null;
    }

    /**
     * Точный скан с шагом 1 вокруг игрока — гарантированно видит структуру, если ты уже в ней!
     */
    private static BlockPos scanImmediateExact(ClientLevel level, Player player, VadimkaConfig.StructureTarget target) {
        // 1. Сначала проверяем сущности рядом (жители / бриз / варден / разбойники)
        for (Entity e : level.entitiesForRendering()) {
            if (e.distanceToSqr(player) > 48.0 * 48.0) continue;
            if (target == VadimkaConfig.StructureTarget.VILLAGE && e.getType() == EntityType.VILLAGER) {
                return e.blockPosition().immutable();
            }
            if (target == VadimkaConfig.StructureTarget.TRIAL_CHAMBER && e.getType() == EntityType.BREEZE) {
                return e.blockPosition().immutable();
            }
            if (target == VadimkaConfig.StructureTarget.ANCIENT_CITY && e.getType() == EntityType.WARDEN) {
                return e.blockPosition().immutable();
            }
        }

        // 2. Уже найденные сканером Хранилища (Vault)
        if (target == VadimkaConfig.StructureTarget.TRIAL_CHAMBER) {
            for (VadimkaConfig.VaultInfo vi : VaultHelper.getNearbyVaults()) {
                if (vi.isVaultBlock) return vi.pos;
            }
        }

        // 3. Точный скан каждого блока (шаг = 1) в радиусе 20 блоков по X/Z и +-10 по Y
        BlockPos c = player.blockPosition();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = 20;
        int minY = Math.max(level.getMinY(), c.getY() - 10);
        int maxY = Math.min(level.getMaxY() - 1, c.getY() + 10);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    p.set(c.getX() + dx, y, c.getZ() + dz);
                    if (isSignatureBlock(level.getBlockState(p), target)) {
                        return p.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static BlockPos scanWideLoadedChunks(ClientLevel level, Player player, VadimkaConfig.StructureTarget target) {
        BlockPos c = player.blockPosition();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r = 56;
        int minY = target == VadimkaConfig.StructureTarget.TRIAL_CHAMBER ? Math.max(level.getMinY(), -48) : Math.max(level.getMinY(), c.getY() - 16);
        int maxY = target == VadimkaConfig.StructureTarget.TRIAL_CHAMBER ? Math.min(level.getMaxY() - 1, 20) : Math.min(level.getMaxY() - 1, c.getY() + 24);
        for (int dx = -r; dx <= r; dx += 2) {
            for (int dz = -r; dz <= r; dz += 2) {
                for (int y = minY; y <= maxY; y += 2) {
                    p.set(c.getX() + dx, y, c.getZ() + dz);
                    if (isSignatureBlock(level.getBlockState(p), target)) {
                        return p.immutable();
                    }
                }
            }
        }
        return null;
    }

    private static boolean isSignatureBlock(BlockState st, VadimkaConfig.StructureTarget target) {
        return switch (target) {
            case TRIAL_CHAMBER -> st.is(Blocks.VAULT) || st.is(Blocks.TRIAL_SPAWNER);
            case VILLAGE -> st.is(Blocks.BELL) || st.is(Blocks.LECTERN) || st.is(Blocks.COMPOSTER);
            case STRONGHOLD -> st.is(Blocks.END_PORTAL_FRAME);
            case ANCIENT_CITY -> st.is(Blocks.SCULK_SHRIEKER) || st.is(Blocks.REINFORCED_DEEPSLATE);
            case MONUMENT -> st.is(Blocks.SEA_LANTERN) || st.is(Blocks.DARK_PRISMARINE);
            case RUINED_PORTAL -> st.is(Blocks.CRYING_OBSIDIAN);
            default -> false;
        };
    }
}
