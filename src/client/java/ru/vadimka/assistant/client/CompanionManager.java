package ru.vadimka.assistant.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.axolotl.Axolotl;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.EnumMap;
import java.util.Map;

/**
 * Управляет 3D-спутником Вадимкой (6 обликов включая редкого Синего Аксолотля),
 * 4 умными миньонами, косметикой F5 и интеграцией статуса Custom Player Models (CPM).
 */
public final class CompanionManager {
    private CompanionManager() {}

    private static Mob companionCore = null;
    private static ArmorStand companionSuit = null;
    private static VadimkaConfig.VadimkaForm spawnedForm = null;
    private static int spawnedScale = -1;

    private static final Map<VadimkaConfig.MinionRole, Mob> minions = new EnumMap<>(VadimkaConfig.MinionRole.class);
    private static final Map<VadimkaConfig.VadimkaForm, LivingEntity> guiModels = new EnumMap<>(VadimkaConfig.VadimkaForm.class);
    private static Mob guiVillager = null;

    public static boolean isCpmInstalled() {
        return FabricLoader.getInstance().isModLoaded("cpm");
    }

    public static void cleanup() {
        if (companionCore != null) { companionCore.discard(); companionCore = null; }
        if (companionSuit != null) { companionSuit.discard(); companionSuit = null; }
        spawnedForm = null;
        for (Mob m : minions.values()) {
            if (m != null) m.discard();
        }
        minions.clear();
        guiModels.clear();
        guiVillager = null;
    }

    public static LivingEntity getGuiModel(ClientLevel level, Player player, VadimkaConfig.VadimkaForm form) {
        LivingEntity cached = guiModels.get(form);
        if (cached != null && !cached.isRemoved() && cached.level() == level) return cached;
        LivingEntity created = buildEntity(level, form, true);
        if (created != null) guiModels.put(form, created);
        return created != null ? created : player;
    }

    public static Mob getGuiVillager(ClientLevel level) {
        if (guiVillager != null && !guiVillager.isRemoved() && guiVillager.level() == level) return guiVillager;
        Mob v = EntityType.VILLAGER.create(level, EntitySpawnReason.LOAD);
        if (v != null) { v.setNoAi(true); v.setSilent(true); guiVillager = v; }
        return guiVillager;
    }

    private static LivingEntity buildEntity(ClientLevel level, VadimkaConfig.VadimkaForm form, boolean forGui) {
        if (form == VadimkaConfig.VadimkaForm.CYBER_PLAYER) {
            ArmorStand bot = EntityType.ARMOR_STAND.create(level, EntitySpawnReason.LOAD);
            if (bot != null) {
                bot.setNoGravity(true); bot.setSilent(true); bot.setInvulnerable(true);
                bot.setShowArms(true); bot.setNoBasePlate(true);
                ItemStack chest = new ItemStack(Items.LEATHER_CHESTPLATE);
                chest.set(DataComponents.DYED_COLOR, new DyedItemColor(0x0E7490));
                ItemStack legs = new ItemStack(Items.LEATHER_LEGGINGS);
                legs.set(DataComponents.DYED_COLOR, new DyedItemColor(0x1E293B));
                bot.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.PLAYER_HEAD));
                bot.setItemSlot(EquipmentSlot.CHEST, chest);
                bot.setItemSlot(EquipmentSlot.LEGS, legs);
                bot.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
                bot.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.END_CRYSTAL));
                if (!forGui) applyScale(bot, form.worldScale * (VadimkaConfig.companionScalePercent / 100.0));
                return bot;
            }
        }
        if (form == VadimkaConfig.VadimkaForm.AXOLOTL) {
            Axolotl ax = EntityType.AXOLOTL.create(level, EntitySpawnReason.LOAD);
            if (ax != null) {
                ax.setNoAi(true); ax.setNoGravity(true); ax.setSilent(true); ax.setInvulnerable(true);
                setAxolotlBlueByType(ax);
                if (!forGui) applyScale(ax, form.worldScale * (VadimkaConfig.companionScalePercent / 100.0));
                return ax;
            }
        }
        EntityType<? extends Mob> type = switch (form) {
            case RAT -> EntityType.SILVERFISH;
            case BREEZE -> EntityType.BREEZE;
            case WARDEN -> EntityType.WARDEN;
            default -> EntityType.ALLAY;
        };
        Mob mob = type.create(level, EntitySpawnReason.LOAD);
        if (mob != null) {
            mob.setNoAi(true); mob.setNoGravity(true); mob.setSilent(true); mob.setInvulnerable(true);
            if (!forGui) applyScale(mob, form.worldScale * (VadimkaConfig.companionScalePercent / 100.0));
        }
        return mob;
    }

    /**
     * Поиск по типу параметра (Axolotl.Variant), а не по строковому имени — 100% переживает ремаппинг Fabric!
     */
    private static void setAxolotlBlueByType(Axolotl ax) {
        try {
            for (Method m : Axolotl.class.getDeclaredMethods()) {
                if (!Modifier.isStatic(m.getModifiers()) && !m.isSynthetic()
                        && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == Axolotl.Variant.class
                        && m.getReturnType() == void.class) {
                    m.setAccessible(true);
                    m.invoke(ax, Axolotl.Variant.BLUE);
                    return;
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void applyScale(LivingEntity entity, double scale) {
        AttributeInstance inst = entity.getAttribute(Attributes.SCALE);
        if (inst != null) inst.setBaseValue(Mth.clamp(scale, 0.25, 2.0));
    }

    public static void tick(Minecraft mc, ClientLevel level, Player player, int worldTick) {
        if (!VadimkaConfig.companionEnabled) {
            if (companionCore != null || companionSuit != null || !minions.isEmpty()) cleanup();
            return;
        }

        boolean rebuild = companionCore == null || companionCore.isRemoved()
            || companionCore.level() != level || spawnedForm != VadimkaConfig.currentForm;

        if (rebuild) {
            if (companionCore != null) { companionCore.discard(); companionCore = null; }
            if (companionSuit != null) { companionSuit.discard(); companionSuit = null; }

            if (VadimkaConfig.currentForm == VadimkaConfig.VadimkaForm.CYBER_PLAYER) {
                ArmorStand stand = (ArmorStand) buildEntity(level, VadimkaConfig.currentForm, false);
                Mob orb = EntityType.ALLAY.create(level, EntitySpawnReason.LOAD);
                if (stand != null && orb != null) {
                    stand.setId(-77001);
                    stand.setCustomName(Component.literal("§b✦ Вадимка"));
                    stand.setCustomNameVisible(true);
                    stand.setPos(player.getX() + 1.0, player.getY() + 1.0, player.getZ() + 1.0);
                    level.addEntity(stand);

                    orb.setId(-77002);
                    orb.setNoAi(true); orb.setNoGravity(true); orb.setSilent(true); orb.setInvulnerable(true);
                    orb.setCustomNameVisible(false);
                    applyScale(orb, 0.34 * (VadimkaConfig.companionScalePercent / 100.0));
                    orb.setPos(player.getX() + 1.0, player.getY() + 1.4, player.getZ() + 1.0);
                    level.addEntity(orb);

                    companionSuit = stand;
                    companionCore = orb;
                    spawnedForm = VadimkaConfig.currentForm;
                    spawnedScale = VadimkaConfig.companionScalePercent;
                }
            } else {
                LivingEntity ent = buildEntity(level, VadimkaConfig.currentForm, false);
                if (ent instanceof Mob mob) {
                    mob.setId(-77001);
                    mob.setCustomName(Component.literal("§b✦ Вадимка"));
                    mob.setCustomNameVisible(true);
                    mob.setPos(player.getX() + 1.0, player.getY() + 1.0, player.getZ() + 1.0);
                    level.addEntity(mob);
                    companionCore = mob;
                    spawnedForm = VadimkaConfig.currentForm;
                    spawnedScale = VadimkaConfig.companionScalePercent;
                }
            }
        } else if (spawnedScale != VadimkaConfig.companionScalePercent) {
            if (companionSuit != null) applyScale(companionSuit, VadimkaConfig.currentForm.worldScale * (VadimkaConfig.companionScalePercent / 100.0));
            if (companionCore != null) {
                double mul = VadimkaConfig.currentForm == VadimkaConfig.VadimkaForm.CYBER_PLAYER ? 0.34 : VadimkaConfig.currentForm.worldScale;
                applyScale(companionCore, mul * (VadimkaConfig.companionScalePercent / 100.0));
            }
            spawnedScale = VadimkaConfig.companionScalePercent;
        }

        double yawRad = Math.toRadians(player.getYRot());
        double rightX = -Math.cos(yawRad), rightZ = -Math.sin(yawRad);
        double forwardX = -Math.sin(yawRad), forwardZ = Math.cos(yawRad);
        double freq = (VadimkaConfig.vadimkaDanceToBpm && SpotifyBridge.isPlaying()) ? 0.28 : 0.14;
        double bob = Math.sin(worldTick * freq) * 0.12;

        double tx = player.getX() + rightX * VadimkaConfig.offsetX + forwardX * VadimkaConfig.offsetZ;
        double ty = player.getY() + VadimkaConfig.offsetY + bob;
        double tz = player.getZ() + rightZ * VadimkaConfig.offsetX + forwardZ * VadimkaConfig.offsetZ;

        if (companionSuit != null) {
            companionSuit.xo = companionSuit.getX(); companionSuit.yo = companionSuit.getY(); companionSuit.zo = companionSuit.getZ();
            companionSuit.setPos(Mth.lerp(0.24, companionSuit.getX(), tx), Mth.lerp(0.24, companionSuit.getY(), ty - 0.35), Mth.lerp(0.24, companionSuit.getZ(), tz));
            companionSuit.setYRot(player.getYRot()); companionSuit.setYHeadRot(player.getYRot());
        }
        if (companionCore != null) {
            companionCore.xo = companionCore.getX(); companionCore.yo = companionCore.getY(); companionCore.zo = companionCore.getZ();
            double extraH = companionSuit != null ? 0.72 : 0.0;
            double nx = Mth.lerp(0.24, companionCore.getX(), tx);
            double ny = Mth.lerp(0.24, companionCore.getY(), ty + extraH);
            double nz = Mth.lerp(0.24, companionCore.getZ(), tz);
            companionCore.setPos(nx, ny, nz);
            companionCore.setYRot(player.getYRot()); companionCore.setYHeadRot(player.getYRot()); companionCore.setYBodyRot(player.getYRot());
            if (worldTick % 14 == 0) {
                level.addParticle(new DustParticleOptions(VadimkaConfig.currentForm.primaryColor & 0x00FFFFFF, 0.85F), nx, ny + 0.25, nz, 0.0, 0.02, 0.0);
            }
        }

        // 4 умных миньона
        VadimkaConfig.MinionRole[] roles = VadimkaConfig.MinionRole.values();
        for (int i = 0; i < roles.length; i++) {
            VadimkaConfig.MinionRole role = roles[i];
            boolean on = VadimkaConfig.minionsEnabled && Boolean.TRUE.equals(VadimkaConfig.activeMinions.get(role));
            Mob existing = minions.get(role);
            if (!on) {
                if (existing != null) { existing.discard(); minions.remove(role); }
                continue;
            }
            if (existing == null || existing.isRemoved() || existing.level() != level) {
                if (existing != null) existing.discard();
                Mob m = EntityType.ALLAY.create(level, EntitySpawnReason.LOAD);
                if (m != null) {
                    m.setId(-77010 - i);
                    m.setNoAi(true); m.setNoGravity(true); m.setSilent(true); m.setInvulnerable(true);
                    m.setCustomName(Component.literal(VadimkaConfig.compactNameplates ? role.compactTag : role.fullTag));
                    m.setCustomNameVisible(true);
                    applyScale(m, 0.54);
                    m.setPos(player.getX(), player.getY() + 1.5, player.getZ());
                    level.addEntity(m);
                    minions.put(role, m);
                    existing = m;
                }
            }
            if (existing != null) {
                double slotAng = yawRad + Math.PI + ((i - 1.5) * 0.72);
                double mtx = player.getX() - Math.sin(slotAng) * 2.1;
                double mtz = player.getZ() + Math.cos(slotAng) * 2.1;
                double mty = player.getY() + 1.55 + Math.sin(worldTick * 0.11 + i * 1.5) * 0.16;

                if (VadimkaConfig.minionsSmartAi) {
                    if (role == VadimkaConfig.MinionRole.COLLECTOR && VadimkaConfig.nearestLootPos != null) {
                        mtx = VadimkaConfig.nearestLootPos.x; mty = VadimkaConfig.nearestLootPos.y + 0.85; mtz = VadimkaConfig.nearestLootPos.z;
                    } else if (role == VadimkaConfig.MinionRole.SCOUT && VadimkaConfig.nearestHostilePos != null && VadimkaConfig.nearestHostileDist < 16.0) {
                        Vec3 mid = player.position().lerp(VadimkaConfig.nearestHostilePos, 0.45);
                        mtx = mid.x; mty = mid.y + 1.85; mtz = mid.z;
                    } else if (role == VadimkaConfig.MinionRole.BUILDER && mc.hitResult instanceof BlockHitResult bhr
                            && bhr.getType() == HitResult.Type.BLOCK && !player.getMainHandItem().isEmpty()) {
                        Vec3 hit = bhr.getLocation();
                        if (hit.distanceToSqr(player.position()) < 36.0) { mtx = hit.x; mty = hit.y + 0.95; mtz = hit.z; }
                    }
                }
                existing.xo = existing.getX(); existing.yo = existing.getY(); existing.zo = existing.getZ();
                existing.setPos(Mth.lerp(0.15, existing.getX(), mtx), Mth.lerp(0.15, existing.getY(), mty), Mth.lerp(0.15, existing.getZ(), mtz));
                float mYaw = (float) (Mth.atan2(player.getZ() - existing.getZ(), player.getX() - existing.getX()) * (180.0 / Math.PI)) - 90.0F;
                existing.setYRot(mYaw); existing.setYHeadRot(mYaw);
            }
        }

        // Косметика F5 (3D крылья и нимб)
        if (VadimkaConfig.worldCosmeticsF5 && worldTick % 2 == 0 && !mc.options.getCameraType().isFirstPerson()) {
            spawnCosmetics(level, player, worldTick);
        }
    }

    private static void spawnCosmetics(ClientLevel level, Player player, int worldTick) {
        double yawRad = Math.toRadians(player.getYRot());
        double backX = Math.sin(yawRad), backZ = -Math.cos(yawRad);
        double rightX = -Math.cos(yawRad), rightZ = -Math.sin(yawRad);

        if (VadimkaConfig.wingsIndex > 0) {
            int col = switch (VadimkaConfig.wingsIndex) {
                case 1 -> 0xF8FAFC;
                case 2 -> 0xEF4444;
                case 3 -> 0x22D3EE;
                default -> 0xC084FC;
            };
            double flap = Math.sin(worldTick * 0.22) * 0.14;
            for (int side = -1; side <= 1; side += 2) {
                for (int step = 1; step <= 3; step++) {
                    if ((worldTick + step) % 4 != 0) continue;
                    level.addParticle(new DustParticleOptions(col, 0.95F),
                        player.getX() + backX * (0.34 + 0.12 * step) + rightX * (side * 0.28 * step),
                        player.getY() + 1.06 + (step == 2 ? 0.28 : (step == 3 ? 0.14 : 0.05)) + flap * step,
                        player.getZ() + backZ * (0.34 + 0.12 * step) + rightZ * (side * 0.28 * step),
                        0.0, 0.0, 0.0);
                }
            }
        }
        if (VadimkaConfig.hornsIndex == 4 && worldTick % 4 == 0) {
            double ang = (worldTick * 0.35) % (Math.PI * 2.0);
            level.addParticle(ParticleTypes.END_ROD,
                player.getX() + Math.cos(ang) * 0.34,
                player.getY() + 2.16,
                player.getZ() + Math.sin(ang) * 0.34, 0.0, 0.0, 0.0);
        }
    }
}
