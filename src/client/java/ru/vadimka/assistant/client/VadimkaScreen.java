package ru.vadimka.assistant.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleConsumer;

/**
 * Иконочный 3-колоночный интерфейс Assistant Vadimka v9.0:
 * - Настоящие 32x32 PNG-иконки на каждой вкладке и кнопках (без стен текста!)
 * - Настоящие 3D-иконки предметов Minecraft в кнопках команд (Топор, Кирка, Меч, Алмаз, Хранилище, Кафедра)
 * - Отдельная вкладка «Команды» для мгновенного запуска Бега / Плавания / Копания / Добычи / Боя
 * - Интеграция статуса Custom Player Models (CPM) во вкладке «Кастом»
 */
public class VadimkaScreen extends Screen {
    private VadimkaConfig.MenuTab activeTab;
    private int rightPage = 0;
    private int rightTotalPages = 1;

    private boolean showChatOverlay = false;
    private int chestsSubTab = 0; // 0=3D Сцена, 1=Ларчики, 2=Жители
    private int spotifySubTab = 0; // 0=Spotify (ПК), 1=AFK Игры

    private EditBox chatBox = null;
    private String chatDraft = "";

    private static class NavItemVis {
        final VadimkaConfig.MenuTab tab;
        final int x, y, w, h;

        NavItemVis(VadimkaConfig.MenuTab tab, int x, int y, int w, int h) {
            this.tab = tab; this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    private static class RightCtrlVis {
        final int x, y, w, h, cx, cy, cw, ch, kind;
        final String title, sub, val;
        final boolean on;
        final float ratio;

        RightCtrlVis(int x, int y, int w, int h, int cx, int cy, int cw, int ch,
                     String title, String sub, int kind, boolean on, float ratio, String val) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.cx = cx; this.cy = cy; this.cw = cw; this.ch = ch;
            this.title = title; this.sub = sub; this.kind = kind;
            this.on = on; this.ratio = ratio; this.val = val;
        }
    }

    private static class IconBtnVis {
        final int x, y, w, h, style; // 0=dark, 1=accent, 2=danger, 3=green
        final String label;
        final Identifier pngIcon;
        final ItemStack itemIcon;

        IconBtnVis(int x, int y, int w, int h, String label, int style, Identifier pngIcon, ItemStack itemIcon) {
            this.x = x; this.y = y; this.w = w; this.h = h;
            this.label = label; this.style = style;
            this.pngIcon = pngIcon; this.itemIcon = itemIcon;
        }
    }

    private static class CtrlSpec {
        final String title, sub, val;
        final int kind; // 0=toggle, 1=slider, 2=cycle
        final boolean on;
        final float ratio;
        final Runnable click;
        final DoubleConsumer slide;

        CtrlSpec(String title, String sub, int kind, boolean on, float ratio, String val, Runnable click, DoubleConsumer slide) {
            this.title = title; this.sub = sub; this.kind = kind;
            this.on = on; this.ratio = ratio; this.val = val; this.click = click; this.slide = slide;
        }
    }

    private final List<NavItemVis> navItems = new ArrayList<>();
    private final List<RightCtrlVis> rightCtrls = new ArrayList<>();
    private final List<CtrlSpec> activeSpecs = new ArrayList<>();
    private final List<IconBtnVis> iconBtns = new ArrayList<>();
    private int draggingIdx = -1;

    // 3 AFK мини-игры
    public boolean afkRunning = false;
    private int afkScore = 0, afkBest = 0, afkTick = 0;
    private int runnerLane = 1;
    private final List<int[]> runnerObs = new ArrayList<>();
    private double sonicY = 0.0, sonicVel = 0.0;
    private final List<int[]> sonicObs = new ArrayList<>();
    private int doomX = 50;
    private final List<int[]> doomShots = new ArrayList<>();
    private final List<int[]> doomMobs = new ArrayList<>();
    private final Random rng = new Random();

    private int winX, winY, winW, winH;
    private int leftX, leftW, centerX, centerW, rightX, rightW, colY, colH;

    public VadimkaScreen(VadimkaConfig.MenuTab tab) {
        super(Component.literal("Assistant Vadimka"));
        this.activeTab = tab;
    }

    @Override
    protected void init() {
        navItems.clear();
        rightCtrls.clear();
        activeSpecs.clear();
        iconBtns.clear();
        chatBox = null;

        winW = Math.min(this.width - 10, 660);
        winH = Math.min(this.height - 10, 354);
        winX = (this.width - winW) / 2;
        winY = (this.height - winH) / 2;

        colY = winY + 30;
        colH = winH - 36;

        leftW = 104;
        rightW = 204;
        leftX = winX + 5;
        centerX = leftX + leftW + 5;
        centerW = winW - leftW - rightW - 20;
        rightX = centerX + centerW + 5;

        // 1. Левая панель навигации (9 вкладок с настоящими PNG-иконками!)
        VadimkaConfig.MenuTab[] tabs = VadimkaConfig.MenuTab.values();
        int itemH = 24, gap = 3, sy = colY + 5;
        for (int i = 0; i < tabs.length; i++) {
            VadimkaConfig.MenuTab t = tabs[i];
            int ix = leftX + 4, iy = sy + i * (itemH + gap), iw = leftW - 8;
            navItems.add(new NavItemVis(t, ix, iy, iw, itemH));
            addHit(ix, iy, iw, itemH, () -> {
                activeTab = t;
                rightPage = 0;
                showChatOverlay = false;
                if (t != VadimkaConfig.MenuTab.SPOTIFY_PLAYER) afkRunning = false;
                this.rebuildWidgets();
            });
        }

        addBtn(leftX + 4, colY + colH - 22, leftW - 8, 18, VadimkaConfig.currentTheme.title, 1, null, null, () -> {
            VadimkaConfig.MenuTheme[] th = VadimkaConfig.MenuTheme.values();
            VadimkaConfig.currentTheme = th[(VadimkaConfig.currentTheme.ordinal() + 1) % th.length];
            GpuMinimap.markDirty();
            this.rebuildWidgets();
        });

        addBtn(winX + winW - 22, winY + 5, 17, 17, "✕", 2, null, null, this::onClose);

        // 2. Иконочные кнопки центральной сцены
        initCenterButtons();

        // 3. Правая колонка настроек
        List<CtrlSpec> specs = buildRightSpecs(activeTab);
        int perPage = 6;
        rightTotalPages = Math.max(1, (specs.size() + perPage - 1) / perPage);
        rightPage = Mth.clamp(rightPage, 0, rightTotalPages - 1);

        if (rightTotalPages > 1) {
            int py = colY + 5;
            addBtn(rightX + rightW - 56, py, 17, 15, "◀", 0, null, null, () -> {
                rightPage = (rightPage - 1 + rightTotalPages) % rightTotalPages;
                this.rebuildWidgets();
            });
            addBtn(rightX + rightW - 21, py, 17, 15, "▶", 0, null, null, () -> {
                rightPage = (rightPage + 1) % rightTotalPages;
                this.rebuildWidgets();
            });
        }

        int sIdx = rightPage * perPage, eIdx = Math.min(specs.size(), sIdx + perPage);
        int ry = colY + 24;
        for (int i = sIdx; i < eIdx; i++) {
            CtrlSpec sp = specs.get(i);
            activeSpecs.add(sp);
            int rx = rightX + 5, rw = rightW - 10, rh = 44;
            int cw = sp.kind == 0 ? 34 : (sp.kind == 1 ? 112 : 80);
            int ch = 16;
            int cx = rx + rw - cw - 6;
            int cy = ry + (sp.kind == 1 ? 22 : 14);
            rightCtrls.add(new RightCtrlVis(rx, ry, rw, rh, cx, cy, cw, ch, sp.title, sp.sub, sp.kind, sp.on, sp.ratio, sp.val));
            if (sp.kind != 1 && sp.click != null) {
                addHit(cx, cy, cw, ch, () -> { sp.click.run(); this.rebuildWidgets(); });
            }
            ry += rh + 4;
        }
    }

    private void initCenterButtons() {
        int sx = centerX + 7;
        int sw = centerW - 14;
        int bottomY = colY + colH - 24;

        switch (activeTab) {
            case COMPANION -> {
                addBtn(sx + sw - 68, colY + 5, 68, 16, showChatOverlay ? "3D Вид" : "Чат", 1,
                    showChatOverlay ? VadimkaConfig.MenuTab.COMPANION.iconTex : VadimkaConfig.ICON_BTN_CHAT, null, () -> {
                        showChatOverlay = !showChatOverlay;
                        this.rebuildWidgets();
                    });

                if (showChatOverlay) {
                    int sendW = 58, boxW = sw - sendW - 6;
                    EditBox box = new EditBox(this.font, sx, bottomY, boxW, 18, Component.literal("Чат"));
                    box.setMaxLength(120);
                    box.setValue(chatDraft);
                    box.setHint(Component.literal("добудь дерево / убей зомби / !к дому"));
                    box.setTextColor(0xFFF8FAFC);
                    box.setResponder(s -> chatDraft = s);
                    this.addRenderableWidget(box);
                    chatBox = box;
                    addBtn(sx + boxW + 6, bottomY, sendW, 18, "Пуск", 3, VadimkaConfig.ICON_BTN_PLAY, null, () -> {
                        if (!chatDraft.trim().isEmpty()) {
                            VadimkaConfig.pushChatLog(false, chatDraft.trim());
                            VadimkaConfig.sayInChat(ActionEngine.executeCommand(chatDraft.trim()));
                            chatDraft = "";
                            this.rebuildWidgets();
                        }
                    });
                } else if (!VadimkaConfig.positionEditor3D) {
                    VadimkaConfig.VadimkaForm[] fs = VadimkaConfig.VadimkaForm.values();
                    int cw = (sw - 10) / 3;
                    for (int i = 0; i < fs.length; i++) {
                        VadimkaConfig.VadimkaForm f = fs[i];
                        addBtn(sx + (i % 3) * (cw + 5), bottomY - 22 + (i / 3) * 21, cw, 18, f.codeName,
                            VadimkaConfig.currentForm == f ? 1 : 0, null, null, () -> {
                                VadimkaConfig.currentForm = f;
                                this.rebuildWidgets();
                            });
                    }
                } else {
                    VadimkaConfig.CompanionAnchor[] anc = VadimkaConfig.CompanionAnchor.values();
                    int hw = (sw - 6) / 2;
                    for (int i = 0; i < anc.length; i++) {
                        VadimkaConfig.CompanionAnchor a = anc[i];
                        addBtn(sx + (i % 2) * (hw + 6), bottomY - 22 + (i / 2) * 21, hw, 18, a.label,
                            VadimkaConfig.currentAnchor == a ? 1 : 0, null, null, () -> {
                                VadimkaConfig.currentAnchor = a;
                                VadimkaConfig.offsetX = a.defaultX;
                                VadimkaConfig.offsetY = a.defaultY;
                                VadimkaConfig.offsetZ = a.defaultZ;
                                this.rebuildWidgets();
                            });
                    }
                }
            }

            case AUTOPILOT -> {
                int hw = (sw - 6) / 2;
                addBtn(sx, bottomY - 22, hw, 18, "Задать Дом", 0, VadimkaConfig.ICON_BTN_HOME, null, () -> {
                    ActionEngine.executeCommand("задай дом");
                    this.rebuildWidgets();
                });
                addBtn(sx + hw + 6, bottomY - 22, hw, 18, "A* Домой", 3, VadimkaConfig.ICON_BTN_PLAY, null, () -> {
                    VadimkaConfig.sayInChat(ActionEngine.executeCommand("к дому"));
                    if (PathfinderAStar.isActive()) this.onClose();
                    else this.rebuildWidgets();
                });
                addBtn(sx, bottomY, hw, 18, "A* к Маяку", 1, VadimkaConfig.MenuTab.AUTOPILOT.iconTex, null, () -> {
                    VadimkaConfig.sayInChat(ActionEngine.executeCommand("автопилот"));
                    if (PathfinderAStar.isActive()) this.onClose();
                    else this.rebuildWidgets();
                });
                addBtn(sx + hw + 6, bottomY, hw, 18, "СТОП", 2, VadimkaConfig.ICON_BTN_STOP, null, () -> {
                    ActionEngine.stopAll(Minecraft.getInstance(), true);
                    this.rebuildWidgets();
                });
            }

            case ACTIONS -> {
                // Сетка 3x3 наглядных ИКОНОЧНЫХ кнопок действий (минимум текста, 3D-иконки!)
                int bw = (sw - 8) / 3;
                int bh = 32;
                int y0 = colY + 56;
                addCmdTile(sx, y0, bw, bh, "Беги", 1, VadimkaConfig.MenuTab.AUTOPILOT.iconTex, null, "беги вперёд");
                addCmdTile(sx + bw + 4, y0, bw, bh, "Плыви", 1, VadimkaConfig.ICON_BTN_SWIM, null, "плыви");
                addCmdTile(sx + (bw + 4) * 2, y0, bw, bh, "Копай", 1, null, new ItemStack(Items.DIAMOND_PICKAXE), "копай");

                int y1 = y0 + bh + 6;
                addCmdTile(sx, y1, bw, bh, "Дерево", 0, null, new ItemStack(Items.OAK_LOG), "добудь дерево");
                addCmdTile(sx + bw + 4, y1, bw, bh, "Железо", 0, null, new ItemStack(Items.RAW_IRON), "добудь железо");
                addCmdTile(sx + (bw + 4) * 2, y1, bw, bh, "Алмаз", 0, null, new ItemStack(Items.DIAMOND), "добудь алмаз");

                int y2 = y1 + bh + 6;
                addCmdTile(sx, y2, bw, bh, "Ударь", 0, null, new ItemStack(Items.IRON_SWORD), "ударь");
                addCmdTile(sx + bw + 4, y2, bw, bh, "Убей врага", 3, null, new ItemStack(Items.NETHERITE_SWORD), "убей");
                addBtn(sx + (bw + 4) * 2, y2, bw, bh, "СТОП", 2, VadimkaConfig.ICON_BTN_STOP, null, () -> {
                    ActionEngine.stopAll(Minecraft.getInstance(), true);
                    this.rebuildWidgets();
                });
            }

            case VISUAL_RADAR -> {
                VadimkaConfig.StructureTarget[] quick = {
                    VadimkaConfig.StructureTarget.VILLAGE,
                    VadimkaConfig.StructureTarget.TRIAL_CHAMBER,
                    VadimkaConfig.StructureTarget.STRONGHOLD,
                    VadimkaConfig.StructureTarget.ANCIENT_CITY
                };
                int hw = (sw - 6) / 2;
                for (int i = 0; i < quick.length; i++) {
                    VadimkaConfig.StructureTarget st = quick[i];
                    addBtn(sx + (i % 2) * (hw + 6), bottomY - 22 + (i / 2) * 21, hw, 18, st.russianName, 1,
                        null, new ItemStack(st.iconItem), () -> {
                            StructureLocator.locate(st, false);
                            this.rebuildWidgets();
                        });
                }
            }

            case SPOTIFY_PLAYER -> {
                int hw = (sw - 6) / 2;
                addBtn(sx, colY + 5, hw, 16, "Spotify (ПК)", spotifySubTab == 0 ? 3 : 0, VadimkaConfig.MenuTab.SPOTIFY_PLAYER.iconTex, null, () -> {
                    spotifySubTab = 0; afkRunning = false; SpotifyBridge.sendCommand("POLL"); this.rebuildWidgets();
                });
                addBtn(sx + hw + 6, colY + 5, hw, 16, "AFK Игры", spotifySubTab == 1 ? 1 : 0, null, new ItemStack(Items.SLIME_BALL), () -> {
                    spotifySubTab = 1; this.rebuildWidgets();
                });
                if (spotifySubTab == 0) {
                    int py = colY + 140, bw = (sw - 28) / 5, startX = sx + 6;
                    addBtn(startX, py, bw, 20, "⏮", 0, null, null, () -> SpotifyBridge.sendCommand("PREV"));
                    addBtn(startX + (bw + 4), py, bw, 20, SpotifyBridge.isPlaying() ? "⏸" : "▶", 3, null, null, () -> SpotifyBridge.sendCommand("TOGGLE"));
                    addBtn(startX + (bw + 4) * 2, py, bw, 20, "⏭", 0, null, null, () -> SpotifyBridge.sendCommand("NEXT"));
                    addBtn(startX + (bw + 4) * 3, py, bw, 20, "🔉", 0, null, null, () -> SpotifyBridge.sendCommand("VOL_DOWN"));
                    addBtn(startX + (bw + 4) * 4, py, bw, 20, "🔊", 0, null, null, () -> SpotifyBridge.sendCommand("VOL_UP"));
                } else {
                    int bw = (sw - 6) / 2;
                    addBtn(sx, bottomY, bw, 18, VadimkaConfig.selectedAfkGame.title, 0, null, null, () -> {
                        VadimkaConfig.AfkGameType[] gs = VadimkaConfig.AfkGameType.values();
                        VadimkaConfig.selectedAfkGame = gs[(VadimkaConfig.selectedAfkGame.ordinal() + 1) % gs.length];
                        resetAfkGame(); this.rebuildWidgets();
                    });
                    addBtn(sx + bw + 6, bottomY, bw, 18, afkRunning ? "Стоп" : "Играть", afkRunning ? 2 : 3,
                        afkRunning ? VadimkaConfig.ICON_BTN_STOP : VadimkaConfig.ICON_BTN_PLAY, null, () -> {
                            afkRunning = !afkRunning;
                            if (afkRunning) resetAfkGame();
                            this.rebuildWidgets();
                        });
                }
            }

            case CHESTS_VILLAGERS -> {
                int row1Y = bottomY - 22, bw = (sw - 8) / 3;
                addBtn(sx, row1Y, bw, 18, "Дворец", 1, null, new ItemStack(Items.VAULT), () -> {
                    StructureLocator.locate(VadimkaConfig.StructureTarget.TRIAL_CHAMBER, false);
                    this.rebuildWidgets();
                });
                addBtn(sx + bw + 4, row1Y, bw, 18, VillagerRoller.isRunning() ? "Стоп" : "Перебор",
                    VillagerRoller.isRunning() ? 2 : 3, null, new ItemStack(Items.LECTERN), () -> {
                        if (VillagerRoller.isRunning()) {
                            VillagerRoller.stop("Остановлено");
                            this.rebuildWidgets();
                        } else {
                            String res = VillagerRoller.start();
                            VadimkaConfig.sayInChat(res);
                            if (VillagerRoller.isRunning()) this.onClose();
                            else this.rebuildWidgets();
                        }
                    });
                addBtn(sx + (bw + 4) * 2, row1Y, bw, 18, "Открыть", 1, null, new ItemStack(Items.TRIAL_KEY), () -> {
                    VadimkaConfig.sayInChat(VaultHelper.openNearestManually());
                });

                addBtn(sx, bottomY, bw, 18, "Сброс", 2, VadimkaConfig.ICON_BTN_STOP, null, () -> {
                    VadimkaConfig.rememberedChests.clear();
                    VadimkaConfig.rememberedVillagers.clear();
                    VadimkaConfig.clearNavigationTarget();
                    VadimkaConfig.showToast("Память очищена", 45);
                    this.rebuildWidgets();
                });
                addBtn(sx + bw + 4, bottomY, bw, 18, chestsSubTab == 2 ? "3D" : ("Жит. " + VadimkaConfig.rememberedVillagers.size()),
                    chestsSubTab == 2 ? 1 : 0, null, new ItemStack(Items.EMERALD), () -> {
                        chestsSubTab = chestsSubTab == 2 ? 0 : 2; this.rebuildWidgets();
                    });
                addBtn(sx + (bw + 4) * 2, bottomY, bw, 18, chestsSubTab == 1 ? "3D" : ("Лут " + VadimkaConfig.rememberedChests.size()),
                    chestsSubTab == 1 ? 1 : 0, null, new ItemStack(Items.CHEST), () -> {
                        chestsSubTab = chestsSubTab == 1 ? 0 : 1; this.rebuildWidgets();
                    });
            }

            case CUSTOM_VISUAL -> {
                int hw = (sw - 6) / 2;
                addBtn(sx, bottomY, hw, 18, "Рандом", 1, null, new ItemStack(Items.NETHER_STAR), () -> {
                    VadimkaConfig.wingsIndex = 1 + rng.nextInt(VadimkaConfig.WINGS_OPTIONS.size() - 1);
                    VadimkaConfig.hornsIndex = 1 + rng.nextInt(VadimkaConfig.HORNS_OPTIONS.size() - 1);
                    VadimkaConfig.hatsIndex = 1 + rng.nextInt(VadimkaConfig.HATS_OPTIONS.size() - 1);
                    this.rebuildWidgets();
                });
                addBtn(sx + hw + 6, bottomY, hw, 18, "Сброс", 2, VadimkaConfig.ICON_BTN_STOP, null, () -> {
                    VadimkaConfig.wingsIndex = 0; VadimkaConfig.hornsIndex = 0; VadimkaConfig.hatsIndex = 0; VadimkaConfig.crosshairIndex = 0;
                    this.rebuildWidgets();
                });
            }

            default -> {}
        }
    }

    private void addCmdTile(int x, int y, int w, int h, String label, int style, Identifier png, ItemStack item, String cmd) {
        addBtn(x, y, w, h, label, style, png, item, () -> {
            VadimkaConfig.sayInChat(ActionEngine.executeCommand(cmd));
            this.onClose();
        });
    }

    private List<CtrlSpec> buildRightSpecs(VadimkaConfig.MenuTab tab) {
        List<CtrlSpec> list = new ArrayList<>();
        switch (tab) {
            case COMPANION -> {
                list.add(toggle("Спутник", "3D-модель", VadimkaConfig.companionEnabled, () -> VadimkaConfig.companionEnabled = !VadimkaConfig.companionEnabled));
                list.add(cycle("Облик", "6 моделей", VadimkaConfig.currentForm.russianName, () -> {
                    VadimkaConfig.VadimkaForm[] fs = VadimkaConfig.VadimkaForm.values();
                    VadimkaConfig.currentForm = fs[(VadimkaConfig.currentForm.ordinal() + 1) % fs.length];
                }));
                list.add(toggle("3D-оси", "Редактор XYZ", VadimkaConfig.positionEditor3D, () -> VadimkaConfig.positionEditor3D = !VadimkaConfig.positionEditor3D));
                list.add(slider("X бок", String.format("%.2f", VadimkaConfig.offsetX), (float) ((VadimkaConfig.offsetX + 2.5) / 5.0), r -> VadimkaConfig.offsetX = -2.5 + r * 5.0));
                list.add(slider("Y высота", String.format("%.2f", VadimkaConfig.offsetY), (float) (VadimkaConfig.offsetY / 3.0), r -> VadimkaConfig.offsetY = r * 3.0));
                list.add(slider("Z дист.", String.format("%.2f", VadimkaConfig.offsetZ), (float) ((VadimkaConfig.offsetZ + 2.5) / 5.0), r -> VadimkaConfig.offsetZ = -2.5 + r * 5.0));
                list.add(toggle("Миньоны", "4 помощника", VadimkaConfig.minionsEnabled, () -> VadimkaConfig.minionsEnabled = !VadimkaConfig.minionsEnabled));
                list.add(toggle("Умный AI", "Лут и враги", VadimkaConfig.minionsSmartAi, () -> VadimkaConfig.minionsSmartAi = !VadimkaConfig.minionsSmartAi));
                list.add(slider("Размер", VadimkaConfig.companionScalePercent + "%", (VadimkaConfig.companionScalePercent - 60) / 80.0F, r -> VadimkaConfig.companionScalePercent = 60 + (int) Math.round(r * 80.0)));
            }
            case AUTOPILOT, ACTIONS -> {
                list.add(toggle("Комментатор", "Отчёт в чат", VadimkaConfig.narrateActionsInChat, () -> VadimkaConfig.narrateActionsInChat = !VadimkaConfig.narrateActionsInChat));
                list.add(toggle("Авто-спринт", "Бег вперёд", VadimkaConfig.sprintAssist, () -> VadimkaConfig.sprintAssist = !VadimkaConfig.sprintAssist));
                list.add(toggle("Плавн. кам.", "Мягкий поворот", VadimkaConfig.smoothCameraTurn, () -> VadimkaConfig.smoothCameraTurn = !VadimkaConfig.smoothCameraTurn));
                list.add(toggle("Стоп S/Shift", "Возврат контр.", VadimkaConfig.autoReturnControl, () -> VadimkaConfig.autoReturnControl = !VadimkaConfig.autoReturnControl));
                list.add(toggle("Страж обрыва", "Алерт падения", VadimkaConfig.fallDangerAlert, () -> VadimkaConfig.fallDangerAlert = !VadimkaConfig.fallDangerAlert));
                list.add(toggle("Тихий режим", "Без тостов", VadimkaConfig.quietMode, () -> VadimkaConfig.quietMode = !VadimkaConfig.quietMode));
            }
            case BUILD_HUD -> {
                list.add(toggle("Litematica", "HUD блоков", VadimkaConfig.litematicaHudEnabled, () -> VadimkaConfig.litematicaHudEnabled = !VadimkaConfig.litematicaHudEnabled));
                list.add(toggle("HUD справа", "Угол экрана", VadimkaConfig.litematicaHudRight, () -> VadimkaConfig.litematicaHudRight = !VadimkaConfig.litematicaHudRight));
                list.add(toggle("Авто-хотбар", "Подмена стака", VadimkaConfig.hotbarAutoRefill, () -> VadimkaConfig.hotbarAutoRefill = !VadimkaConfig.hotbarAutoRefill));
                list.add(toggle("Редстоун", "Сила сигнала", VadimkaConfig.redstoneInspector, () -> VadimkaConfig.redstoneInspector = !VadimkaConfig.redstoneInspector));
                list.add(toggle("3D-линейка", "Дист. блока", VadimkaConfig.ruler3DVolume, () -> VadimkaConfig.ruler3DVolume = !VadimkaConfig.ruler3DVolume));
                list.add(toggle("Скрыть гот.", "Дефицит", VadimkaConfig.hideCompletedMaterials, () -> VadimkaConfig.hideCompletedMaterials = !VadimkaConfig.hideCompletedMaterials));
            }
            case COMBAT_PVP -> {
                list.add(toggle("Боевой HUD", "Тотем/Броня", VadimkaConfig.combatHudEnabled, () -> VadimkaConfig.combatHudEnabled = !VadimkaConfig.combatHudEnabled));
                list.add(toggle("Траектории", "Дуга полёта", VadimkaConfig.trajectories3D, () -> VadimkaConfig.trajectories3D = !VadimkaConfig.trajectories3D));
                list.add(toggle("Удар сзади", "Алерт спины", VadimkaConfig.backstabIndicator, () -> VadimkaConfig.backstabIndicator = !VadimkaConfig.backstabIndicator));
                list.add(toggle("Маяк смерти", "Навигатор", VadimkaConfig.deathReplayMarker, () -> VadimkaConfig.deathReplayMarker = !VadimkaConfig.deathReplayMarker));
            }
            case VISUAL_RADAR -> {
                list.add(toggle("GPU Карта", "128x128 75FPS", VadimkaConfig.minimapRadarEnabled, () -> VadimkaConfig.minimapRadarEnabled = !VadimkaConfig.minimapRadarEnabled));
                list.add(toggle("Круг", "Стиль сонара", VadimkaConfig.minimapCircularMode, () -> { VadimkaConfig.minimapCircularMode = !VadimkaConfig.minimapCircularMode; GpuMinimap.markDirty(); }));
                list.add(toggle("Контуры", "Топография", VadimkaConfig.minimapContourLines, () -> { VadimkaConfig.minimapContourLines = !VadimkaConfig.minimapContourLines; GpuMinimap.markDirty(); }));
                list.add(cycle("Масштаб", "Обзор", VadimkaConfig.radarZoomLevel == 0 ? "1x" : (VadimkaConfig.radarZoomLevel == 1 ? "2x" : "3x"), () -> {
                    VadimkaConfig.radarZoomLevel = (VadimkaConfig.radarZoomLevel + 1) % 3;
                    GpuMinimap.reset();
                }));
                list.add(toggle("Сонар", "Враги по краю", VadimkaConfig.soundRadarSonar, () -> VadimkaConfig.soundRadarSonar = !VadimkaConfig.soundRadarSonar));
                list.add(toggle("Элитра HUD", "Горизонт", VadimkaConfig.elytraPitchHorizon, () -> VadimkaConfig.elytraPitchHorizon = !VadimkaConfig.elytraPitchHorizon));
                list.add(toggle("Нить Ариадны", "След пути", VadimkaConfig.ariadneThread, () -> VadimkaConfig.ariadneThread = !VadimkaConfig.ariadneThread));
            }
            case SPOTIFY_PLAYER -> {
                list.add(toggle("Spotify HUD", "Виджет трека", VadimkaConfig.spotifyHudEnabled, () -> VadimkaConfig.spotifyHudEnabled = !VadimkaConfig.spotifyHudEnabled));
                list.add(toggle("Танцы BPM", "Под музыку", VadimkaConfig.vadimkaDanceToBpm, () -> VadimkaConfig.vadimkaDanceToBpm = !VadimkaConfig.vadimkaDanceToBpm));
                list.add(toggle("Выход AFK", "При уроне", VadimkaConfig.afkPanicExit, () -> VadimkaConfig.afkPanicExit = !VadimkaConfig.afkPanicExit));
                list.add(cycle("Мост ПК", SpotifyBridge.isConnected() ? "Подключён" : "Ожидание", "Обновить", () -> SpotifyBridge.sendCommand("POLL")));
            }
            case CHESTS_VILLAGERS -> {
                list.add(toggle("Авто-ключ", "Откр. Хранил.", VadimkaConfig.autoOpenVaultsWithKey, () -> VadimkaConfig.autoOpenVaultsWithKey = !VadimkaConfig.autoOpenVaultsWithKey));
                list.add(toggle("Подсветка", "Хранилища", VadimkaConfig.highlightTrialVaults, () -> VadimkaConfig.highlightTrialVaults = !VadimkaConfig.highlightTrialVaults));
                list.add(cycle("Чары", "Житель", VadimkaConfig.TARGET_ENCHANTS.get(VadimkaConfig.targetEnchantIndex), () -> {
                    VadimkaConfig.targetEnchantIndex = (VadimkaConfig.targetEnchantIndex + 1) % VadimkaConfig.TARGET_ENCHANTS.size();
                }));
                list.add(slider("Макс. цена", VadimkaConfig.maxEmeraldPrice + " из.", (VadimkaConfig.maxEmeraldPrice - 5) / 59.0F, r -> {
                    VadimkaConfig.maxEmeraldPrice = 5 + (int) Math.round(r * 59.0);
                }));
                list.add(toggle("Страж топора", "Стоп <= 10", VadimkaConfig.warnAxeDurability, () -> VadimkaConfig.warnAxeDurability = !VadimkaConfig.warnAxeDurability));
            }
            case CUSTOM_VISUAL -> {
                list.add(cycle("Тема", "5 палитр", VadimkaConfig.currentTheme.title, () -> {
                    VadimkaConfig.MenuTheme[] th = VadimkaConfig.MenuTheme.values();
                    VadimkaConfig.currentTheme = th[(VadimkaConfig.currentTheme.ordinal() + 1) % th.length];
                    GpuMinimap.markDirty();
                }));
                list.add(cycle("Крылья", "В F5", VadimkaConfig.WINGS_OPTIONS.get(VadimkaConfig.wingsIndex), () -> VadimkaConfig.wingsIndex = (VadimkaConfig.wingsIndex + 1) % VadimkaConfig.WINGS_OPTIONS.size()));
                list.add(cycle("Рога/Нимб", "Над головой", VadimkaConfig.HORNS_OPTIONS.get(VadimkaConfig.hornsIndex), () -> VadimkaConfig.hornsIndex = (VadimkaConfig.hornsIndex + 1) % VadimkaConfig.HORNS_OPTIONS.size()));
                list.add(cycle("Шляпка", "Голова", VadimkaConfig.HATS_OPTIONS.get(VadimkaConfig.hatsIndex), () -> VadimkaConfig.hatsIndex = (VadimkaConfig.hatsIndex + 1) % VadimkaConfig.HATS_OPTIONS.size()));
                list.add(cycle("Прицел", "Центр", VadimkaConfig.CROSSHAIR_OPTIONS.get(VadimkaConfig.crosshairIndex), () -> VadimkaConfig.crosshairIndex = (VadimkaConfig.crosshairIndex + 1) % VadimkaConfig.CROSSHAIR_OPTIONS.size()));
                list.add(toggle("F5 эффекты", "Частицы", VadimkaConfig.worldCosmeticsF5, () -> VadimkaConfig.worldCosmeticsF5 = !VadimkaConfig.worldCosmeticsF5));
            }
        }
        return list;
    }

    private CtrlSpec toggle(String t, String s, boolean on, Runnable r) { return new CtrlSpec(t, s, 0, on, 0, "", r, null); }
    private CtrlSpec slider(String t, String v, float ratio, DoubleConsumer d) { return new CtrlSpec(t, "", 1, true, Mth.clamp(ratio, 0, 1), v, null, d); }
    private CtrlSpec cycle(String t, String s, String v, Runnable r) { return new CtrlSpec(t, s, 2, true, 0, v, r, null); }

    private void addHit(int x, int y, int w, int h, Runnable act) {
        this.addWidget(Button.builder(Component.empty(), b -> act.run()).bounds(x, y, w, h).build());
    }

    private void addBtn(int x, int y, int w, int h, String lbl, int style, Identifier png, ItemStack item, Runnable act) {
        iconBtns.add(new IconBtnVis(x, y, w, h, lbl, style, png, item));
        addHit(x, y, w, h, act);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        if (event.button() == 0) {
            for (int i = 0; i < rightCtrls.size(); i++) {
                RightCtrlVis rc = rightCtrls.get(i);
                if (rc.kind == 1 && event.x() >= rc.x + 4 && event.x() <= rc.x + rc.w - 52 && event.y() >= rc.cy - 4 && event.y() <= rc.cy + rc.ch + 4) {
                    draggingIdx = i;
                    dragSlider(i, event.x());
                    return true;
                }
            }
        }
        return super.mouseClicked(event, dbl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (draggingIdx >= 0 && draggingIdx < rightCtrls.size()) {
            dragSlider(draggingIdx, event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        draggingIdx = -1;
        return super.mouseReleased(event);
    }

    private void dragSlider(int idx, double mx) {
        RightCtrlVis rc = rightCtrls.get(idx);
        CtrlSpec sp = activeSpecs.get(idx);
        if (sp.slide != null) {
            double r = Mth.clamp((mx - (rc.x + 8)) / Math.max(1.0, rc.w - 68), 0.0, 1.0);
            sp.slide.accept(r);
            this.rebuildWidgets();
        }
    }

    private void resetAfkGame() {
        afkScore = 0; afkTick = 0; runnerLane = 1; runnerObs.clear();
        sonicY = 0; sonicVel = 0; sonicObs.clear();
        doomX = 50; doomShots.clear(); doomMobs.clear();
    }

    @Override
    public void tick() {
        super.tick();
        if (!afkRunning) return;
        afkTick++;
        switch (VadimkaConfig.selectedAfkGame) {
            case RUNNER -> {
                if (afkTick % 8 == 0) {
                    afkScore++;
                    if (rng.nextInt(3) == 0) runnerObs.add(new int[]{rng.nextInt(3), 0, rng.nextInt(5) == 0 ? 1 : 0});
                }
                if (afkTick % 2 == 0) {
                    List<int[]> next = new ArrayList<>();
                    for (int[] o : runnerObs) {
                        o[1] += 4;
                        if (o[1] >= 72 && o[1] <= 90 && o[0] == runnerLane) {
                            if (o[2] == 1) { afkScore += 5; continue; }
                            else { resetAfkGame(); return; }
                        }
                        if (o[1] < 98) next.add(o);
                    }
                    runnerObs.clear(); runnerObs.addAll(next);
                }
            }
            case SONIC -> {
                if (afkTick % 6 == 0) afkScore++;
                sonicY += sonicVel; sonicVel -= 1.35;
                if (sonicY < 0) { sonicY = 0; sonicVel = 0; }
                if (afkTick % 24 == 0) sonicObs.add(new int[]{100, rng.nextInt(3) == 0 ? 1 : 0});
                List<int[]> next = new ArrayList<>();
                for (int[] o : sonicObs) {
                    o[0] -= 4;
                    if (o[0] >= 12 && o[0] <= 24) {
                        if (o[1] == 1 && sonicY >= 8) { afkScore += 10; continue; }
                        else if (o[1] == 0 && sonicY < 10) { resetAfkGame(); return; }
                    }
                    if (o[0] > -6) next.add(o);
                }
                sonicObs.clear(); sonicObs.addAll(next);
            }
            case DOOM -> {
                if (afkTick % 26 == 0) doomMobs.add(new int[]{12 + rng.nextInt(76), 8});
                List<int[]> nb = new ArrayList<>();
                for (int[] b : doomShots) { b[1] -= 6; if (b[1] > 4) nb.add(b); }
                doomShots.clear(); doomShots.addAll(nb);
                List<int[]> ne = new ArrayList<>();
                for (int[] e : doomMobs) {
                    if (afkTick % 3 == 0) e[1] += 2;
                    boolean hit = false;
                    for (int i = doomShots.size() - 1; i >= 0; i--) {
                        int[] b = doomShots.get(i);
                        if (Math.abs(b[0] - e[0]) < 10 && Math.abs(b[1] - e[1]) < 10) {
                            doomShots.remove(i); hit = true; afkScore += 5; break;
                        }
                    }
                    if (hit) continue;
                    if (e[1] >= 84) { resetAfkGame(); return; }
                    ne.add(e);
                }
                doomMobs.clear(); doomMobs.addAll(ne);
            }
        }
        if (afkScore > afkBest) afkBest = afkScore;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        if (chatBox != null && chatBox.isFocused()) {
            if (k == GLFW.GLFW_KEY_ENTER || k == GLFW.GLFW_KEY_KP_ENTER) {
                if (!chatDraft.trim().isEmpty()) {
                    VadimkaConfig.pushChatLog(false, chatDraft.trim());
                    VadimkaConfig.sayInChat(ActionEngine.executeCommand(chatDraft.trim()));
                    chatDraft = "";
                    this.rebuildWidgets();
                }
                return true;
            }
            if (k == GLFW.GLFW_KEY_ESCAPE) { chatBox.setFocused(false); return true; }
            return super.keyPressed(event);
        }
        if (afkRunning) {
            if (k == GLFW.GLFW_KEY_ESCAPE) { afkRunning = false; this.rebuildWidgets(); return true; }
            if (VadimkaConfig.selectedAfkGame == VadimkaConfig.AfkGameType.RUNNER) {
                if (k == GLFW.GLFW_KEY_A || k == GLFW.GLFW_KEY_LEFT) { runnerLane = Math.max(0, runnerLane - 1); return true; }
                if (k == GLFW.GLFW_KEY_D || k == GLFW.GLFW_KEY_RIGHT) { runnerLane = Math.min(2, runnerLane + 1); return true; }
            } else if (VadimkaConfig.selectedAfkGame == VadimkaConfig.AfkGameType.SONIC) {
                if ((k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_W || k == GLFW.GLFW_KEY_UP) && sonicY <= 0.5) { sonicVel = 10.5; return true; }
            } else {
                if (k == GLFW.GLFW_KEY_A || k == GLFW.GLFW_KEY_LEFT) { doomX = Math.max(10, doomX - 9); return true; }
                if (k == GLFW.GLFW_KEY_D || k == GLFW.GLFW_KEY_RIGHT) { doomX = Math.min(90, doomX + 9); return true; }
                if (k == GLFW.GLFW_KEY_SPACE || k == GLFW.GLFW_KEY_W || k == GLFW.GLFW_KEY_UP) { doomShots.add(new int[]{doomX, 82}); return true; }
            }
        }
        if (k == GLFW.GLFW_KEY_P) { this.onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mx, int my, float pt) {}

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        g.fill(0, 0, this.width, this.height, 0x9905080E);
        VadimkaConfig.MenuTheme th = VadimkaConfig.currentTheme;

        VadimkaHud.fillRoundedRect(g, winX, winY, winW, winH, th.bgWindow);
        VadimkaHud.drawGradientNeonBorder(g, winX, winY, winW, winH, th.accent, th.secondary);
        g.fillGradient(winX + 2, winY + 2, winX + winW - 2, winY + 25, th.bgCard, th.bgPanel);
        g.fill(winX + 2, winY + 25, winX + winW - 2, winY + 26, th.borderMuted);

        g.blit(RenderPipelines.GUI_TEXTURED, VadimkaConfig.MenuTab.COMPANION.iconTex, winX + 8, winY + 6, 0.0F, 0.0F, 14, 14, 14, 14);
        String title = "Assistant Vadimka";
        g.drawString(this.font, title, winX + (winW - this.font.width(title)) / 2, winY + 9, th.accent);
        g.drawString(this.font, VadimkaConfig.VERSION_LABEL, winX + winW - 86, winY + 9, 0xFF94A3B8);

        // Левая панель с PNG-иконками
        VadimkaHud.fillGlassCard(g, leftX, colY, leftW, colH, th.bgPanel, th.borderMuted);
        for (NavItemVis ni : navItems) {
            boolean act = ni.tab == activeTab;
            boolean hov = over(mx, my, ni.x, ni.y, ni.w, ni.h);
            int fill = act ? VadimkaHud.lerpColor(th.bgCard, th.accent, 0.14F) : (hov ? th.bgCard : 0xFF0E141D);
            VadimkaHud.fillRoundedRect(g, ni.x, ni.y, ni.w, ni.h, fill);
            if (act) {
                VadimkaHud.drawGradientNeonBorder(g, ni.x, ni.y, ni.w, ni.h, th.accent, th.secondary);
                g.fill(ni.x + 2, ni.y + 4, ni.x + 4, ni.y + ni.h - 4, th.accent);
            } else {
                VadimkaHud.drawRoundedBorder(g, ni.x, ni.y, ni.w, ni.h, hov ? th.accent : th.borderMuted);
            }
            g.blit(RenderPipelines.GUI_TEXTURED, ni.tab.iconTex, ni.x + 6, ni.y + 5, 0.0F, 0.0F, 14, 14, 14, 14);
            g.drawString(this.font, ni.tab.navTitle, ni.x + 24, ni.y + 8, act ? 0xFFFFFFFF : 0xFFCBD5E1);
        }

        // Центральная сцена
        VadimkaHud.fillGlassCard(g, centerX, colY, centerW, colH, th.bgPanel, th.borderMuted);
        renderCenterStage(g, mx, my);

        // Правая панель настроек
        VadimkaHud.fillGlassCard(g, rightX, colY, rightW, colH, th.bgPanel, th.borderMuted);
        g.drawString(this.font, "Настройки", rightX + 8, colY + 7, th.secondary);
        if (rightTotalPages > 1) {
            g.drawString(this.font, (rightPage + 1) + "/" + rightTotalPages, rightX + rightW - 39, colY + 7, 0xFF94A3B8);
        }

        for (RightCtrlVis rc : rightCtrls) {
            boolean hov = over(mx, my, rc.x, rc.y, rc.w, rc.h);
            VadimkaHud.fillGlassCard(g, rc.x, rc.y, rc.w, rc.h,
                hov ? VadimkaHud.lerpColor(th.bgCard, 0xFFFFFFFF, 0.04F) : th.bgCard,
                hov ? th.accent : th.borderMuted);

            if (rc.kind == 0) {
                int tw = rc.cx - (rc.x + 7) - 4;
                g.drawString(this.font, VadimkaConfig.trimText(this.font, rc.title, tw), rc.x + 7, rc.y + 8, 0xFFF8FAFC);
                if (!rc.sub.isEmpty()) g.drawString(this.font, VadimkaConfig.trimText(this.font, rc.sub, tw), rc.x + 7, rc.y + 22, 0xFF94A3B8);
                VadimkaHud.fillGlassCard(g, rc.cx, rc.cy, rc.cw, rc.ch, rc.on ? th.toggleOnTrack : 0xFF1A2332, rc.on ? th.accent : 0xFF334155);
                int knob = rc.ch - 4;
                int kx = rc.on ? (rc.cx + rc.cw - knob - 2) : (rc.cx + 2);
                VadimkaHud.fillRoundedRect(g, kx, rc.cy + 2, knob, knob, rc.on ? 0xFFFFFFFF : 0xFF94A3B8);
            } else if (rc.kind == 1) {
                g.drawString(this.font, rc.title, rc.x + 7, rc.y + 6, 0xFFF8FAFC);
                int bw = 50, bx = rc.x + rc.w - bw - 6;
                VadimkaHud.fillGlassCard(g, bx, rc.cy, bw, rc.ch, 0xFF0A1018, th.borderMuted);
                String vs = VadimkaConfig.trimText(this.font, rc.val, bw - 4);
                g.drawString(this.font, vs, bx + (bw - this.font.width(vs)) / 2, rc.cy + 4, th.accent);
                int tx = rc.x + 8, tw = bx - tx - 8, ty = rc.cy + 6;
                g.fill(tx, ty, tx + tw, ty + 4, 0xFF1E293B);
                int fw = (int) (tw * rc.ratio);
                g.fillGradient(tx, ty, tx + fw, ty + 4, th.accent, th.secondary);
                VadimkaHud.fillRoundedRect(g, tx + fw - 3, ty - 3, 6, 10, 0xFFFFFFFF);
            } else {
                int tw = rc.cx - (rc.x + 7) - 4;
                g.drawString(this.font, VadimkaConfig.trimText(this.font, rc.title, tw), rc.x + 7, rc.y + 8, 0xFFF8FAFC);
                if (!rc.sub.isEmpty()) g.drawString(this.font, VadimkaConfig.trimText(this.font, rc.sub, tw), rc.x + 7, rc.y + 22, 0xFF94A3B8);
                VadimkaHud.fillGlassCard(g, rc.cx, rc.cy, rc.cw, rc.ch, 0xFF0C131C, th.secondary);
                String vs = VadimkaConfig.trimText(this.font, rc.val, rc.cw - 6);
                g.drawString(this.font, vs, rc.cx + (rc.cw - this.font.width(vs)) / 2, rc.cy + 4, 0xFFFFFFFF);
            }
        }

        // Иконочные кнопки (PNG + 3D ItemStack + короткая подпись)
        for (IconBtnVis b : iconBtns) {
            boolean hov = over(mx, my, b.x, b.y, b.w, b.h);
            int bg = switch (b.style) {
                case 1 -> hov ? VadimkaHud.lerpColor(th.bgCard, th.accent, 0.36F) : VadimkaHud.lerpColor(th.bgCard, th.accent, 0.18F);
                case 2 -> hov ? 0xFFDC2626 : 0xFF3A1219;
                case 3 -> hov ? 0xFF1DB954 : 0xFF114023;
                default -> hov ? th.bgCard : 0xFF0F1620;
            };
            int bd = switch (b.style) {
                case 1 -> th.accent;
                case 2 -> 0xFFEF4444;
                case 3 -> 0xFF1DB954;
                default -> hov ? th.secondary : th.borderMuted;
            };
            VadimkaHud.fillGlassCard(g, b.x, b.y, b.w, b.h, bg, bd);

            boolean tallTile = b.h >= 28;
            if (tallTile && (b.pngIcon != null || b.itemIcon != null)) {
                // Большая плитка-карточка: иконка сверху по центру, подпись снизу!
                if (b.pngIcon != null) {
                    g.blit(RenderPipelines.GUI_TEXTURED, b.pngIcon, b.x + (b.w - 14) / 2, b.y + 3, 0.0F, 0.0F, 14, 14, 14, 14);
                } else {
                    g.renderItem(b.itemIcon, b.x + (b.w - 16) / 2, b.y + 2);
                }
                String lbl = VadimkaConfig.trimText(this.font, b.label, b.w - 6);
                g.drawString(this.font, lbl, b.x + (b.w - this.font.width(lbl)) / 2, b.y + b.h - 11, 0xFFFFFFFF);
            } else {
                // Компактная кнопка: иконка слева + текст рядом
                int iconSpace = 0;
                if (b.pngIcon != null) {
                    g.blit(RenderPipelines.GUI_TEXTURED, b.pngIcon, b.x + 4, b.y + (b.h - 12) / 2, 0.0F, 0.0F, 12, 12, 12, 12);
                    iconSpace = 16;
                } else if (b.itemIcon != null) {
                    g.renderItem(b.itemIcon, b.x + 2, b.y + (b.h - 16) / 2);
                    iconSpace = 18;
                }
                String lbl = VadimkaConfig.trimText(this.font, b.label, b.w - iconSpace - 6);
                int tx = b.x + iconSpace + (b.w - iconSpace - this.font.width(lbl)) / 2;
                g.drawString(this.font, lbl, tx, b.y + (b.h - 8) / 2, 0xFFFFFFFF);
            }
        }

        super.render(g, mx, my, pt);
    }

    private void renderCenterStage(GuiGraphics g, int mx, int my) {
        int sx = centerX + 7, sy = colY + 6, sw = centerW - 14, sh = colH - 12;
        g.drawString(this.font, VadimkaConfig.trimText(this.font, activeTab.headerTitle, sw - 74), sx, sy + 2, 0xFFF8FAFC);

        switch (activeTab) {
            case COMPANION -> renderCompanionStage(g, sx, sy, sw, sh, mx, my);
            case AUTOPILOT -> renderAutopilotStage(g, sx, sy, sw, sh);
            case ACTIONS -> renderActionsStage(g, sx, sy, sw, sh);
            case BUILD_HUD -> renderBuildHudStage(g, sx, sy, sw, sh);
            case COMBAT_PVP -> renderCombatStage(g, sx, sy, sw, sh);
            case VISUAL_RADAR -> renderRadarStage(g, sx, sy, sw, sh);
            case SPOTIFY_PLAYER -> renderSpotifyStage(g, sx, sy, sw, sh);
            case CHESTS_VILLAGERS -> renderVaultsStage(g, sx, sy, sw, sh, mx, my);
            case CUSTOM_VISUAL -> renderCustomStage(g, sx, sy, sw, sh, mx, my);
        }
    }

    private void drawBackdrop(GuiGraphics g, int x, int y, int w, int h) {
        g.fillGradient(x, y, x + w, y + h, 0xFF080D14, VadimkaHud.lerpColor(0xFF0E1722, VadimkaConfig.currentTheme.accent, 0.08F));
        VadimkaHud.drawRoundedBorder(g, x, y, w, h, VadimkaConfig.currentTheme.borderMuted);
        int hy = y + (h * 52) / 100;
        for (int i = 1; i <= 5; i++) {
            int ly = hy + (int) (((y + h - 3) - hy) * (i * i / 25.0F));
            g.fill(x + 4, ly, x + w - 4, ly + 1, 0x1622D3EE);
        }
    }

    private void renderCompanionStage(GuiGraphics g, int sx, int sy, int sw, int sh, int mx, int my) {
        if (showChatOverlay) {
            int by = sy + 20, bh = sh - 48;
            VadimkaHud.fillGlassCard(g, sx, by, sw, bh, 0xFF080D14, VadimkaConfig.currentTheme.borderMuted);
            List<String[]> lines = new ArrayList<>();
            for (VadimkaConfig.ChatLogEntry e : VadimkaConfig.chatHistory) {
                for (String ln : wrap((e.fromVadimka ? "✦ " : "Вы: ") + e.text, sw - 14)) {
                    lines.add(new String[]{e.fromVadimka ? "1" : "0", ln});
                }
            }
            int max = Math.max(1, (bh - 8) / 12);
            int start = Math.max(0, lines.size() - max), ty = by + 5;
            for (int i = start; i < lines.size(); i++) {
                g.drawString(this.font, lines.get(i)[1], sx + 7, ty, "1".equals(lines.get(i)[0]) ? VadimkaConfig.currentTheme.accent : 0xFFE2E8F0);
                ty += 12;
            }
            return;
        }
        int stageY = sy + 20, stageH = sh - 68;
        drawBackdrop(g, sx, stageY, sw, stageH);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (!VadimkaConfig.positionEditor3D) {
            VadimkaConfig.VadimkaForm[] fs = VadimkaConfig.VadimkaForm.values();
            int slotW = sw / fs.length;
            for (int i = 0; i < fs.length; i++) {
                VadimkaConfig.VadimkaForm f = fs[i];
                int fx = sx + i * slotW, fy = stageY + 22, fh = stageH - 28;
                boolean sel = f == VadimkaConfig.currentForm;
                if (sel) VadimkaHud.drawGradientNeonBorder(g, fx + 2, fy, slotW - 4, fh, VadimkaConfig.currentTheme.accent, VadimkaConfig.currentTheme.secondary);
                else VadimkaHud.drawRoundedBorder(g, fx + 2, fy, slotW - 4, fh, 0x4494A3B8);
                g.drawString(this.font, VadimkaConfig.trimText(this.font, f.codeName, slotW - 4), fx + 4, stageY + 7, sel ? VadimkaConfig.currentTheme.accent : 0xFFE2E8F0);
                try {
                    LivingEntity m = CompanionManager.getGuiModel(mc.level, mc.player, f);
                    InventoryScreen.renderEntityInInventoryFollowsMouse(g, fx + 2, fy + 4, fx + slotW - 2, fy + fh - 4, Math.max(12, (int) (f.guiScale * 0.7)), 0.0625F, mx, my, m);
                } catch (Throwable ignored) {}
            }
        } else {
            int cx = sx + sw / 2 - 10, cy = stageY + stageH / 2 + 12;
            int px = Mth.clamp(cx + (int) (VadimkaConfig.offsetX * 26.0), sx + 34, sx + sw - 34);
            int py = Mth.clamp(cy - (int) ((VadimkaConfig.offsetY - 1.0) * 24.0), stageY + 34, stageY + stageH - 24);
            try {
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, cx - 36, stageY + 24, cx + 36, stageY + stageH - 8, 44, 0.0625F, mx, my, mc.player);
                LivingEntity pet = CompanionManager.getGuiModel(mc.level, mc.player, VadimkaConfig.currentForm);
                InventoryScreen.renderEntityInInventoryFollowsMouse(g, px - 24, py - 24, px + 24, py + 24, Math.max(14, (int) (VadimkaConfig.currentForm.guiScale * 0.72)), 0.0625F, mx, my, pet);
            } catch (Throwable ignored) {}
            VadimkaHud.drawRoundedBorder(g, px - 16, py - 18, 32, 34, 0x9922D3EE);
            g.drawString(this.font, "Вадимка", px - 19, Math.max(stageY + 5, py - 29), 0xFFFDE047);
        }
    }

    private void renderAutopilotStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
        int cy = sy + 18, ch = sh - 66;
        VadimkaHud.fillGlassCard(g, sx, cy, sw, ch, 0xFF0A1018, PathfinderAStar.isActive() ? 0xFF4ADE80 : VadimkaConfig.currentTheme.borderMuted);
        String home = VadimkaConfig.homeWaypoint == null ? "Дом: не задан" : ("Дом: " + VadimkaConfig.homeWaypoint.getX() + ", " + VadimkaConfig.homeWaypoint.getY() + ", " + VadimkaConfig.homeWaypoint.getZ());
        String nav = VadimkaConfig.activeNavTarget == null ? "Маяк: нет цели" : ("Маяк: " + VadimkaConfig.activeNavTitle + " [" + VadimkaConfig.activeNavTarget.getX() + ", " + VadimkaConfig.activeNavTarget.getZ() + "]");
        g.drawString(this.font, VadimkaConfig.trimText(this.font, home, sw - 16), sx + 8, cy + 10, 0xFF4ADE80);
        g.drawString(this.font, VadimkaConfig.trimText(this.font, nav, sw - 16), sx + 8, cy + 24, VadimkaConfig.currentTheme.accent);
        g.drawString(this.font, VadimkaConfig.trimText(this.font, "Статус: " + PathfinderAStar.getStatusLine(), sw - 16), sx + 8, cy + 40, 0xFFF8FAFC);

        g.drawString(this.font, "3D A* Патфайндер активен:", sx + 8, cy + 62, VadimkaConfig.currentTheme.secondary);
        g.drawString(this.font, "• Обегает стены, дома, деревья и заборы", sx + 10, cy + 76, 0xFFCBD5E1);
        g.drawString(this.font, "• Забирается на ступеньки и переплывает реки", sx + 10, cy + 90, 0xFFCBD5E1);
        g.drawString(this.font, "• Не падает в обрывы >= 4 бл. и обходит лаву", sx + 10, cy + 104, 0xFFCBD5E1);
        g.drawString(this.font, "• Мгновенно отдаёт управление по S / Shift", sx + 10, cy + 118, 0xFF4ADE80);
        g.drawString(this.font, "Пройдено за сессию: " + (int) VadimkaConfig.sessionWalkedBlocks + " бл.", sx + 8, cy + ch - 16, 0xFF94A3B8);
    }

    private void renderActionsStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
        VadimkaHud.fillGlassCard(g, sx, sy + 18, sw, 26, 0xFF0A1018, VadimkaConfig.currentTheme.accent);
        g.drawString(this.font, "Сейчас: " + VadimkaConfig.trimText(this.font, ActionEngine.getActiveSummary(), sw - 54), sx + 8, sy + 27, 0xFF4ADE80);
        g.drawString(this.font, "Или пиши в чат: !добудь <блок> / !убей <моб> / !плыви / !копай", sx + 2, sy + sh - 16, 0xFF94A3B8);
    }

    private void renderBuildHudStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
        int cy = sy + 20, ch = sh - 28;
        VadimkaHud.fillGlassCard(g, sx, cy, sw, ch, 0xFF0A1017, VadimkaConfig.currentTheme.accent);
        Player p = Minecraft.getInstance().player;
        ItemStack[] icons = {new ItemStack(Items.STONE_BRICKS), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.GLASS), new ItemStack(Items.QUARTZ_BLOCK)};
        String[] names = {"Кам. кирпичи", "Дуб. доски", "Стекло", "Кварц"};
        int[] have = {countItem(p, Items.STONE_BRICKS), countItem(p, Items.OAK_PLANKS), countItem(p, Items.GLASS), countItem(p, Items.QUARTZ_BLOCK)};
        int[] need = {256, 128, 64, 32};
        int ry = cy + 14;
        for (int i = 0; i < names.length; i++) {
            g.renderItem(icons[i], sx + 12, ry + 2);
            g.drawString(this.font, names[i], sx + 34, ry + 1, 0xFFF8FAFC);
            int col = have[i] >= need[i] ? 0xFF4ADE80 : (have[i] > 0 ? 0xFFFACC15 : 0xFFF87171);
            g.drawString(this.font, have[i] + " / " + need[i], sx + 34, ry + 12, col);
            int bw = 76, bx = sx + sw - bw - 12;
            g.fill(bx, ry + 8, bx + bw, ry + 13, 0xFF1E293B);
            g.fill(bx, ry + 8, bx + (int) (bw * Mth.clamp((float) have[i] / need[i], 0.0F, 1.0F)), ry + 13, col);
            ry += 34;
        }
    }

    private void renderCombatStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
        int stageY = sy + 18, stageH = sh - 22;
        drawBackdrop(g, sx, stageY, sw, stageH);
        int cx = sx + sw / 2, cy = stageY + stageH / 2;
        VadimkaHud.drawRoundedBorder(g, cx - 48, cy - 12, 26, 26, 0xFF22D3EE);
        g.renderItem(new ItemStack(Items.ENDER_PEARL), cx - 43, cy - 7);
        VadimkaHud.drawRoundedBorder(g, cx + 22, cy - 12, 26, 26, 0xFFC084FC);
        g.renderItem(new ItemStack(Items.SHIELD), cx + 27, cy - 7);
        VadimkaHud.drawRoundedBorder(g, cx - 13, cy + 18, 26, 26, 0xFF38BDF8);
        g.renderItem(new ItemStack(Items.ARROW), cx - 8, cy + 23);

        ItemStack[] armors = {new ItemStack(Items.DIAMOND_HELMET), new ItemStack(Items.DIAMOND_CHESTPLATE), new ItemStack(Items.DIAMOND_LEGGINGS), new ItemStack(Items.DIAMOND_BOOTS)};
        for (int i = 0; i < armors.length; i++) {
            g.renderItem(armors[i], sx + 10, stageY + 18 + i * 24);
            g.drawString(this.font, "98%", sx + 30, stageY + 22 + i * 24, 0xFF4ADE80);
        }
        g.renderItem(new ItemStack(Items.TOTEM_OF_UNDYING), sx + sw - 46, stageY + 18);
        int tot = Minecraft.getInstance().player != null ? VadimkaConfig.countPlayerTotems(Minecraft.getInstance().player) : 1;
        g.drawString(this.font, "x" + tot, sx + sw - 26, stageY + 22, 0xFFFDE047);
    }

    private void renderRadarStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
        int stageY = sy + 18, stageH = sh - 66;
        drawBackdrop(g, sx, stageY, sw, stageH);
        int rCx = sx + sw / 3, rCy = stageY + stageH / 2 - 4;
        VadimkaHud.drawRoundedBorder(g, rCx - 42, rCy - 42, 84, 84, 0xAA22D3EE);
        VadimkaHud.drawRoundedBorder(g, rCx - 26, rCy - 26, 52, 52, 0xAAC084FC);
        g.drawString(this.font, "GPU 128x128", sx + sw / 2 + 8, stageY + 22, 0xFF4ADE80);
        g.drawString(this.font, "Биомы + рельеф", sx + sw / 2 + 8, stageY + 36, 0xFFCBD5E1);
        g.drawString(this.font, "1 вызов / кадр", sx + sw / 2 + 8, stageY + 50, VadimkaConfig.currentTheme.accent);

        int stY = stageY + stageH - 22;
        VadimkaHud.fillGlassCard(g, sx + 6, stY, sw - 12, 18, 0xDD0B131E, VadimkaConfig.activeNavTarget != null ? VadimkaConfig.activeNavColor : VadimkaConfig.currentTheme.borderMuted);
        g.drawString(this.font, VadimkaConfig.trimText(this.font, StructureLocator.getLastStatus(), sw - 22), sx + 12, stY + 5, 0xFFE2E8F0);
    }

    private void renderSpotifyStage(GuiGraphics g, int sx, int sy, int sw, int sh) {
        if (spotifySubTab == 1) {
            if (afkRunning) renderAfkGame(g, sx, sy + 24, sw, sh - 52);
            else {
                VadimkaHud.fillGlassCard(g, sx, sy + 26, sw, sh - 56, 0xFF0A1018, VadimkaConfig.currentTheme.accent);
                String t1 = VadimkaConfig.selectedAfkGame.title, t2 = VadimkaConfig.selectedAfkGame.hint;
                g.drawString(this.font, t1, sx + (sw - this.font.width(t1)) / 2, sy + 72, VadimkaConfig.currentTheme.accent);
                g.drawString(this.font, t2, sx + (sw - this.font.width(t2)) / 2, sy + 90, 0xFFE2E8F0);
            }
            return;
        }
        int cy = sy + 24, ch = sh - 32;
        boolean play = SpotifyBridge.isPlaying();
        g.fillGradient(sx, cy, sx + sw, cy + ch, 0xFF0D2818, 0xFF0B1310);
        VadimkaHud.drawRoundedBorder(g, sx, cy, sw, ch, play ? 0xFF1DB954 : 0xFF334155);
        g.blit(RenderPipelines.GUI_TEXTURED, VadimkaConfig.MenuTab.SPOTIFY_PLAYER.iconTex, sx + 12, cy + 12, 0.0F, 0.0F, 22, 22, 22, 22);
        String hdr = SpotifyBridge.isConnected() ? ((play ? "● ИГРАЕТ • " : "⏸ ПАУЗА • ") + SpotifyBridge.getSource()) : "○ Ожидание Spotify...";
        g.drawString(this.font, VadimkaConfig.trimText(this.font, hdr, sw - 46), sx + 40, cy + 19, play ? 0xFF4ADE80 : 0xFF94A3B8);

        g.drawString(this.font, VadimkaConfig.trimText(this.font, SpotifyBridge.getTitle(), sw - 24), sx + 12, cy + 48, 0xFFFFFFFF);
        g.drawString(this.font, VadimkaConfig.trimText(this.font, SpotifyBridge.getArtist(), sw - 24), sx + 12, cy + 62, 0xFF86EFAC);

        int pSec = SpotifyBridge.getPosSec(), dSec = SpotifyBridge.getDurSec();
        String tl = String.format("%d:%02d", pSec / 60, pSec % 60);
        String tr = dSec > 0 ? String.format("%d:%02d", dSec / 60, dSec % 60) : "--:--";
        g.drawString(this.font, tl, sx + 12, cy + 84, 0xFFCBD5E1);
        g.drawString(this.font, tr, sx + sw - 12 - this.font.width(tr), cy + 84, 0xFF94A3B8);

        int bw = sw - 24;
        g.fill(sx + 12, cy + 96, sx + 12 + bw, cy + 100, 0xFF1F2937);
        float prog = dSec > 0 ? Mth.clamp((float) pSec / dSec, 0.0F, 1.0F) : (play ? 0.5F : 0.0F);
        g.fill(sx + 12, cy + 96, sx + 12 + (int) (bw * prog), cy + 100, 0xFF1DB954);
        g.drawString(this.font, "Постоянный WinRT-демон (0 лагов CPU)", sx + 12, cy + ch - 14, 0xFF94A3B8);
    }

    private void renderVaultsStage(GuiGraphics g, int sx, int sy, int sw, int sh, int mx, int my) {
        if (chestsSubTab == 1) {
            int by = sy + 18, bh = sh - 66;
            VadimkaHud.fillGlassCard(g, sx, by, sw, bh, 0xFF0A1018, VadimkaConfig.currentTheme.accent);
            int y = by + 8;
            if (VadimkaConfig.rememberedChests.isEmpty()) {
                g.drawString(this.font, "Открой сундук ПКМ — лут запишется сюда!", sx + 8, y, 0xFF4ADE80);
            } else {
                for (VadimkaConfig.ChestRecord cr : VadimkaConfig.rememberedChests.values()) {
                    if (y > by + bh - 24) break;
                    g.drawString(this.font, cr.containerName + " [" + cr.pos.getX() + ", " + cr.pos.getZ() + "]", sx + 8, y, VadimkaConfig.currentTheme.accent);
                    g.drawString(this.font, VadimkaConfig.trimText(this.font, cr.summaryLine(), sw - 16), sx + 8, y + 10, 0xFFCBD5E1);
                    y += 24;
                }
            }
            return;
        }
        if (chestsSubTab == 2) {
            int by = sy + 18, bh = sh - 66;
            VadimkaHud.fillGlassCard(g, sx, by, sw, bh, 0xFF0A1018, VadimkaConfig.currentTheme.secondary);
            int y = by + 8;
            if (VadimkaConfig.rememberedVillagers.isEmpty()) {
                g.drawString(this.font, "Нажми «Перебор» рядом с жителем и кафедрой!", sx + 8, y, 0xFF4ADE80);
            } else {
                for (VadimkaConfig.VillagerRecord vr : VadimkaConfig.rememberedVillagers.values()) {
                    for (VadimkaConfig.VillagerTradeEntry tr : vr.trades) {
                        if (y > by + bh - 16) break;
                        int col = (tr.valuable && tr.emeraldPrice <= VadimkaConfig.maxEmeraldPrice) ? 0xFF4ADE80 : 0xFFE2E8F0;
                        g.drawString(this.font, VadimkaConfig.trimText(this.font, tr.resultText + " — " + tr.costText, sw - 16), sx + 8, y, col);
                        y += 13;
                    }
                }
            }
            return;
        }
        int stageY = sy + 18, stageH = sh - 66;
        drawBackdrop(g, sx, stageY, sw, stageH);
        int vx = sx + sw / 4 - 24, vy = stageY + stageH / 2 - 14;
        VadimkaHud.fillGlassCard(g, vx, vy, 48, 44, 0x33102030, 0xFF22D3EE);
        g.renderItem(new ItemStack(Items.VAULT), vx + 16, vy + 14);
        g.renderItem(new ItemStack(Items.TRIDENT), vx - 20, vy + 2);
        g.renderItem(new ItemStack(Items.HEAVY_CORE), vx - 20, vy + 24);
        g.renderItem(new ItemStack(Items.TRIAL_KEY), vx + 52, vy + 2);
        g.renderItem(new ItemStack(Items.OMINOUS_TRIAL_KEY), vx + 52, vy + 24);
        g.drawString(this.font, "Хранилищ: " + VaultHelper.countVaultBlocks(), sx + 8, stageY + 8, 0xFFFACC15);

        int vilX = sx + (sw * 3) / 4 + 6;
        VadimkaHud.fillGlassCard(g, vilX - 54, stageY + 8, 108, 24, 0xEE0B1624, 0xFF22D3EE);
        g.drawString(this.font, VadimkaConfig.TARGET_ENCHANTS.get(VadimkaConfig.targetEnchantIndex) + " ≤ " + VadimkaConfig.maxEmeraldPrice, vilX - 48, stageY + 15, 0xFF4ADE80);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Mob vil = CompanionManager.getGuiVillager(mc.level);
            if (vil != null) {
                try { InventoryScreen.renderEntityInInventoryFollowsMouse(g, vilX - 24, stageY + 38, vilX + 38, stageY + stageH - 8, 36, 0.0625F, mx, my, vil); } catch (Throwable ignored) {}
            }
        }
        g.renderItem(new ItemStack(Items.LECTERN), vilX - 38, stageY + stageH - 34);
        g.drawString(this.font, VadimkaConfig.trimText(this.font, VillagerRoller.getStatusText(), sw - 14), sx + 7, stageY + stageH - 13, 0xFF4ADE80);
    }

    private void renderCustomStage(GuiGraphics g, int sx, int sy, int sw, int sh, int mx, int my) {
        int stageY = sy + 18, stageH = sh - 46;
        drawBackdrop(g, sx, stageY, sw, stageH);
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            try { InventoryScreen.renderEntityInInventoryFollowsMouse(g, sx + sw / 2 - 42, stageY + 22, sx + sw / 2 + 42, stageY + stageH - 12, 48, 0.0625F, mx, my, mc.player); } catch (Throwable ignored) {}
        }
        boolean cpm = CompanionManager.isCpmInstalled();
        g.drawString(this.font, "CPM 3D-модели: " + (cpm ? "✓ Подключён" : "Не установлен"), sx + 8, stageY + 10, cpm ? 0xFF4ADE80 : 0xFFFACC15);
        g.drawString(this.font, "Крылья: " + VadimkaConfig.WINGS_OPTIONS.get(VadimkaConfig.wingsIndex), sx + 8, stageY + 26, VadimkaConfig.currentTheme.accent);
        g.drawString(this.font, "Нимб/Рога: " + VadimkaConfig.HORNS_OPTIONS.get(VadimkaConfig.hornsIndex), sx + 8, stageY + 40, VadimkaConfig.currentTheme.secondary);
        g.drawString(this.font, "Прицел: " + VadimkaConfig.CROSSHAIR_OPTIONS.get(VadimkaConfig.crosshairIndex), sx + 8, stageY + 54, 0xFFE2E8F0);
    }

    private void renderAfkGame(GuiGraphics g, int gx, int gy, int gw, int gh) {
        VadimkaHud.fillGlassCard(g, gx, gy, gw, gh, 0xFF070C12, VadimkaConfig.currentTheme.accent);
        g.drawString(this.font, VadimkaConfig.selectedAfkGame.title + " | Счёт: " + afkScore + " Рекорд: " + afkBest, gx + 6, gy + 5, 0xFFFDE047);
        int ax = gx + 6, ay = gy + 18, aw = gw - 12, ah = gh - 24;
        g.fill(ax, ay, ax + aw, ay + ah, 0xFF0D1520);
        switch (VadimkaConfig.selectedAfkGame) {
            case RUNNER -> {
                int lw = aw / 3;
                for (int l = 0; l < 3; l++) g.fill(ax + l * lw + 2, ay + 2, ax + (l + 1) * lw - 2, ay + ah - 2, 0x18FFFFFF);
                for (int[] o : runnerObs) g.fill(ax + o[0] * lw + lw / 2 - 6, ay + (o[1] * (ah - 14)) / 100, ax + o[0] * lw + lw / 2 + 6, ay + (o[1] * (ah - 14)) / 100 + 12, o[2] == 1 ? 0xFF10B981 : 0xFF22C55E);
                g.fill(ax + runnerLane * lw + lw / 2 - 6, ay + ah - 18, ax + runnerLane * lw + lw / 2 + 6, ay + ah - 6, VadimkaConfig.currentTheme.accent);
            }
            case SONIC -> {
                int gy2 = ay + ah - 14;
                g.fill(ax, gy2, ax + aw, ay + ah, 0xFF166534);
                g.fill(ax + (18 * aw) / 100, gy2 - 12 - (int) (sonicY * 1.4), ax + (18 * aw) / 100 + 12, gy2 - (int) (sonicY * 1.4), 0xFF38BDF8);
                for (int[] o : sonicObs) {
                    int ox = ax + (o[0] * aw) / 100;
                    if (o[1] == 1) g.fill(ox, gy2 - 28, ox + 8, gy2 - 20, 0xFFFACC15);
                    else g.fill(ox, gy2 - 14, ox + 8, gy2, 0xFFEF4444);
                }
            }
            case DOOM -> {
                g.fill(ax + (doomX * aw) / 100 - 6, ay + ah - 16, ax + (doomX * aw) / 100 + 6, ay + ah - 6, VadimkaConfig.currentTheme.accent);
                for (int[] b : doomShots) g.fill(ax + (b[0] * aw) / 100 - 1, ay + (b[1] * ah) / 100, ax + (b[0] * aw) / 100 + 2, ay + (b[1] * ah) / 100 + 6, 0xFFFDE047);
                for (int[] e : doomMobs) g.fill(ax + (e[0] * aw) / 100 - 6, ay + (e[1] * ah) / 100, ax + (e[0] * aw) / 100 + 6, ay + (e[1] * ah) / 100 + 12, 0xFFF43F5E);
            }
        }
    }

    private int countItem(Player p, Item item) {
        if (p == null) return 0;
        int c = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack st = p.getInventory().getItem(i);
            if (!st.isEmpty() && st.is(item)) c += st.getCount();
        }
        return c;
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private String[] wrap(String text, int maxW) {
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            String test = cur.isEmpty() ? w : (cur + " " + w);
            if (this.font.width(test) > maxW && !cur.isEmpty()) {
                lines.add(cur.toString());
                cur = new StringBuilder(w);
            } else cur = new StringBuilder(test);
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        return lines.toArray(new String[0]);
    }
}
