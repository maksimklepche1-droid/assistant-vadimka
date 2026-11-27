package ru.vadimka.assistant.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Assistant Vadimka v9.0 NEXUS — модульная клиентская архитектура под Fabric 1.21.11.
 */
public class AssistantVadimkaClient implements ClientModInitializer {
    public static final String MOD_ID = VadimkaConfig.MOD_ID;

    private static KeyMapping openMenuKey;
    private static int worldTick = 0;
    private static boolean greeted = false;
    private static Vec3 prevPos = null;
    private static float prevHp = 20.0F;

    // Авто-пополнение хотбара
    private static Item lastHandItem = Items.AIR;
    private static int lastHandCount = 0;
    private static int lastRefillTick = -30;

    // Нить Ариадны
    private static final Deque<Vec3> ariadne = new ArrayDeque<>();
    private static Vec3 lastCrumb = null;

    @Override
    public void onInitializeClient() {
        VadimkaConfig.initDefaults();
        SpotifyBridge.ensureStarted();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
            "key.assistant_vadimka.menu",
            GLFW.GLFW_KEY_P,
            KeyMapping.Category.MISC
        ));

        // Перехват команд чата (!команда или упоминание «Вадимка ...»)
        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
            if (!VadimkaConfig.chatAssistantEnabled || message == null) return true;
            String trimmed = message.trim();
            if (trimmed.isEmpty()) return true;
            if (trimmed.startsWith("!")) {
                VadimkaConfig.pushChatLog(false, trimmed);
                String reply = ActionEngine.executeCommand(trimmed.substring(1).trim());
                VadimkaConfig.sayInChat(reply);
                return false;
            }
            String low = trimmed.toLowerCase(Locale.ROOT);
            if (low.contains("вадимк") || low.contains("вадим") || low.contains("vadimka")) {
                VadimkaConfig.pushChatLog(false, trimmed);
                String reply = ActionEngine.executeCommand(trimmed);
                VadimkaConfig.sayInChat(reply);
            }
            return true;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.level == null) {
                ActionEngine.stopAll(mc, false);
                CompanionManager.cleanup();
                VaultHelper.clear();
                VadimkaConfig.radarBlips.clear();
                ariadne.clear();
                lastCrumb = null;
                prevPos = null;
                greeted = false;
                GpuMinimap.reset();
                return;
            }

            while (openMenuKey.consumeClick()) {
                if (mc.screen instanceof VadimkaScreen) mc.setScreen(null);
                else mc.setScreen(new VadimkaScreen(VadimkaConfig.MenuTab.COMPANION));
            }

            tickAllModules(mc, mc.level, mc.player);
        });

        HudRenderCallback.EVENT.register((graphics, tickCounter) -> VadimkaHud.render(graphics));
    }

    private static void tickAllModules(Minecraft mc, ClientLevel level, Player player) {
        worldTick++;
        if (VadimkaConfig.toastTicks > 0) VadimkaConfig.toastTicks--;

        if (!greeted && worldTick > 25) {
            greeted = true;
            if (!VadimkaConfig.quietMode) {
                VadimkaConfig.sayInChat("Вадимка v9.0 NEXUS запущен! Меню: [P], команды: !к дому, !плыви, !копай, !добудь дерево, !убей зомби, !деревня, !перебор починка.");
            }
        }

        // 1. A* Автопилот + Движок команд + Авто-перебор Жителя + Хранилища
        PathfinderAStar.tick(mc, level, player);
        ActionEngine.tick(mc, level, player);
        VillagerRoller.tick(mc, level, player);
        VaultHelper.tick(mc, level, player, worldTick);

        // 2. Авто-спринт (когда не запущен автопилот)
        if (!PathfinderAStar.isActive() && VadimkaConfig.sprintAssist && mc.screen == null
                && mc.options.keyUp.isDown() && !player.isCrouching() && !player.isUsingItem()
                && player.getFoodData().getFoodLevel() > 6) {
            player.setSprinting(true);
        }

        // 3. Авто-пополнение хотбара из инвентаря
        tickHotbarRefill(mc, player);

        // 4. Реалистичная GPU Мини-карта 128x128
        GpuMinimap.tick(mc, level, Mth.floor(player.getX()), Mth.floor(player.getZ()), player.getYRot());

        // 5. 3D Спутник, 4 Миньона и F5 Косметика
        CompanionManager.tick(mc, level, player, worldTick);

        // 6. Сущности вокруг (раз в 5 тиков)
        if (worldTick % 5 == 0) {
            scanEntities(level, player);
            BlockPos below = player.blockPosition().below();
            VadimkaConfig.fallDropBlocks = level.getBlockState(below).isAir() ? measureDrop(level, player.blockPosition()) : 0;
        }

        // 7. Пройденная дистанция, урон и маяк смерти
        Vec3 now = player.position();
        if (prevPos != null) {
            double d = now.distanceTo(prevPos);
            if (d < 10.0) VadimkaConfig.sessionWalkedBlocks += d;
        }
        prevPos = now;

        float hp = player.getHealth();
        if (hp <= 0.0F && prevHp > 0.0F && VadimkaConfig.deathReplayMarker) {
            VadimkaConfig.setNavigationTarget(player.blockPosition().immutable(), "Точка смерти", 0xFFEF4444);
            VadimkaConfig.showToast("Навигатор наведён на точку смерти!", 100);
        } else if (hp < prevHp - 0.1F) {
            if (PathfinderAStar.isActive() && VadimkaConfig.autoReturnControl && ActionEngine.getCurrentTask() != ActionEngine.TaskType.COMBAT_HUNT) {
                PathfinderAStar.stop(mc, false);
                VadimkaConfig.narrate("⚠ Остановил автопилот — ты получил урон!");
            }
            if (VadimkaConfig.afkPanicExit && mc.screen instanceof VadimkaScreen scr && scr.afkRunning) {
                scr.afkRunning = false;
                mc.setScreen(null);
                VadimkaConfig.showToast("⚠ ЭКСТРЕННЫЙ ВЫХОД ИЗ AFK!", 80);
            }
        }
        prevHp = hp;

        // 8. 3D-маяк цели Навигатора + Нить Ариадны
        if (worldTick % 6 == 0 && VadimkaConfig.activeNavTarget != null) {
            BlockPos t = VadimkaConfig.activeNavTarget;
            if (t.distSqr(player.blockPosition()) <= 1600.0) {
                double ty = Math.max(player.getY() - 2.0, t.getY() + 1.0);
                for (int h = 0; h < 6; h++) {
                    level.addParticle(new DustParticleOptions(VadimkaConfig.activeNavColor & 0x00FFFFFF, 1.35F),
                        t.getX() + 0.5, ty + h * 1.5, t.getZ() + 0.5, 0.0, 0.04, 0.0);
                }
            }
        }
        if (VadimkaConfig.ariadneThread) {
            Vec3 crumb = new Vec3(player.getX(), player.getY() + 0.22, player.getZ());
            if (lastCrumb == null || lastCrumb.distanceToSqr(crumb) > 12.25) {
                ariadne.addLast(crumb);
                lastCrumb = crumb;
                while (ariadne.size() > 36) ariadne.removeFirst();
            }
            if (worldTick % 12 == 0) {
                int i = 0;
                for (Vec3 c : ariadne) {
                    if ((i++ + worldTick / 12) % 4 == 0 && c.distanceToSqr(crumb) < 324.0) {
                        level.addParticle(ParticleTypes.ENCHANT, c.x, c.y + 0.15, c.z, 0.0, 0.015, 0.0);
                    }
                }
            }
        }
    }

    private static void tickHotbarRefill(Minecraft mc, Player player) {
        if (!VadimkaConfig.hotbarAutoRefill || mc.screen != null || mc.gameMode == null) return;
        ItemStack hand = player.getMainHandItem();
        if (hand.isEmpty() && lastHandItem != Items.AIR && lastHandCount == 1 && (worldTick - lastRefillTick) > 10) {
            int inv = VadimkaConfig.findInventorySlot(player, lastHandItem);
            if (inv >= 9 && inv < 36) {
                int sel = VadimkaConfig.getSelectedHotbarSlot(player);
                mc.gameMode.handleInventoryMouseClick(player.inventoryMenu.containerId, inv, sel, ClickType.SWAP, player);
                lastRefillTick = worldTick;
                VadimkaConfig.showToast("✦ Пополнил: " + new ItemStack(lastHandItem).getHoverName().getString(), 45);
            }
        }
        lastHandItem = hand.isEmpty() ? Items.AIR : hand.getItem();
        lastHandCount = hand.getCount();
    }

    private static void scanEntities(ClientLevel level, Player player) {
        VadimkaConfig.radarBlips.clear();
        VadimkaConfig.threatBehindPlayer = false;
        VadimkaConfig.nearestHostileDist = 999.0;
        VadimkaConfig.nearestHostilePos = null;
        VadimkaConfig.nearestLootPos = null;
        double bestLootSq = 121.0;
        double lookX = -Math.sin(Math.toRadians(player.getYRot()));
        double lookZ = Math.cos(Math.toRadians(player.getYRot()));

        for (Entity e : level.entitiesForRendering()) {
            if (e == player || e.getId() <= -77000) continue;
            double dx = e.getX() - player.getX(), dz = e.getZ() - player.getZ();
            if (Math.abs(dx) > 64.0 || Math.abs(dz) > 64.0) continue;

            if (e instanceof Player other) {
                VadimkaConfig.radarBlips.add(new VadimkaConfig.RadarBlip(e.getX(), e.getZ(), 0, other.getName().getString()));
            } else if (e instanceof Monster) {
                VadimkaConfig.radarBlips.add(new VadimkaConfig.RadarBlip(e.getX(), e.getZ(), 1, null));
                if (Math.abs(e.getY() - player.getY()) <= 8.0) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < VadimkaConfig.nearestHostileDist) {
                        VadimkaConfig.nearestHostileDist = dist;
                        VadimkaConfig.nearestHostileAngle = Math.atan2(dz, dx);
                        VadimkaConfig.nearestHostilePos = e.position();
                    }
                    if (dist < 10.0 && dist > 0.1 && ((dx / dist) * lookX + (dz / dist) * lookZ) < -0.25) {
                        VadimkaConfig.threatBehindPlayer = true;
                    }
                }
            } else if (e.getType() == EntityType.VILLAGER) {
                VadimkaConfig.radarBlips.add(new VadimkaConfig.RadarBlip(e.getX(), e.getZ(), 3, null));
            } else if (e instanceof Animal) {
                VadimkaConfig.radarBlips.add(new VadimkaConfig.RadarBlip(e.getX(), e.getZ(), 2, null));
            } else if (e instanceof ItemEntity) {
                VadimkaConfig.radarBlips.add(new VadimkaConfig.RadarBlip(e.getX(), e.getZ(), 4, null));
                double sq = dx * dx + dz * dz;
                if (sq < bestLootSq && Math.abs(e.getY() - player.getY()) <= 5.0) {
                    bestLootSq = sq;
                    VadimkaConfig.nearestLootPos = e.position();
                }
            }
        }
    }

    private static int measureDrop(ClientLevel level, BlockPos start) {
        int drop = 0;
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos(start.getX(), start.getY() - 1, start.getZ());
        for (int i = 0; i < 14; i++) {
            BlockState st = level.getBlockState(p);
            if (st.is(Blocks.WATER)) return 0;
            if (!st.isAir()) break;
            drop++;
            p.setY(p.getY() - 1);
        }
        return drop;
    }
}
