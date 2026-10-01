package ru.fozeton.diamondchat.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.config.VoiceConfig;
import ru.fozeton.chatmanager.module.SpeechToText;
import ru.fozeton.chatmanager.module.SpeechToText.Language;
import ru.fozeton.diamondchat.gui.widgets.DeleteButton;
import ru.fozeton.diamondchat.gui.widgets.SearchEditBox;
import ru.fozeton.diamondchat.gui.widgets.SimpleButton;
import ru.fozeton.diamondchat.utils.notify.Notification;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class LanguageModelsScreen extends Screen {
    private static final int MAX_GUI_W = 520;
    private static final int GUI_H = 330;
    private static final int PAD = 15;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 4;
    private static final int MAX_VISIBLE = 9;
    private static final int BTN_W = 100;
    private static final int DEL_W = 20;
    private static final int DEL_GAP = 4;
    private static final int TAB_INSTALLED = 0;
    private static final int TAB_DOWNLOAD = 1;

    private static final int COL_BG = 0xFF212128;
    private static final int COL_ROW = 0xFF1a1a24;
    private static final int COL_DIVIDER = 0xFF2e2e3d;
    private static final int COL_MUTED = 0xFF6b6b88;
    private static final int COL_PURPLE = 0xFF7c6ef5;
    private static final int COL_LIGHT = 0xFFc8c4f0;
    private static final int COL_GREEN = 0xFF4ade80;
    private static final int COL_RED = 0xFFf87171;
    private static final int COL_YELLOW = 0xFFfacc15;

    private static final Set<Language> DOWNLOADING = ConcurrentHashMap.newKeySet();
    private static final Set<Language> FAILED = ConcurrentHashMap.newKeySet();
    private static final AtomicBoolean DIRTY = new AtomicBoolean(false);

    private final List<GuiEventListener> rowWidgets = new ArrayList<>();
    private final VoiceConfig voiceConfig = ChatConfigManager.getInstance().getVoiceConfig();
    private List<Language> filtered = new ArrayList<>();

    private String query = "";
    private int guiW = MAX_GUI_W;
    private int tagColW;
    private int scrollOffset = 0;
    private int tab = TAB_INSTALLED;
    private Language pendingDelete;

    public LanguageModelsScreen() {
        super(Component.literal("Language models"));
    }

    public static void markSuccess(Language l) {
        DOWNLOADING.remove(l);
        FAILED.remove(l);
        DIRTY.set(true);
    }

    public static void markFailed(Language l) {
        DOWNLOADING.remove(l);
        FAILED.add(l);
        DIRTY.set(true);
    }

    private static String formatSize(int mb) {
        return mb >= 1024 ? String.format("%.1f GB", mb / 1024.0).replace(",", ".") : mb + " MB";
    }

    @Override
    protected void init() {
        EditBox searchBox;
        rowWidgets.clear();
        guiW = Math.min(MAX_GUI_W, width - 20);
        tagColW = Arrays.stream(Language.values())
                .mapToInt(l -> font.width("[" + l.getPath() + "]"))
                .max()
                .orElse(40);

        int gx = gx();
        int gy = gy();

        int tabW = (guiW - PAD * 2 - 6) / 2;
        addRenderableWidget(SimpleButton.of(
                gx + PAD, gy + 30, tabW, ROW_H,
                Component.literal("УСТАНОВЛЕННЫЕ (" + countInstalled() + ")"),
                tab == TAB_INSTALLED ? COL_PURPLE : COL_MUTED,
                b -> switchTab(TAB_INSTALLED)
        ));
        addRenderableWidget(SimpleButton.of(
                gx + PAD + tabW + 6, gy + 30, tabW, ROW_H,
                Component.literal("ЗАГРУЗКА" + (DOWNLOADING.isEmpty() ? "" : " (" + DOWNLOADING.size() + ")")),
                tab == TAB_DOWNLOAD ? COL_PURPLE : COL_MUTED,
                b -> switchTab(TAB_DOWNLOAD)
        ));

        searchBox = new SearchEditBox(font, gx + PAD, gy + 56, guiW - PAD * 2, ROW_H, Component.literal("Поиск"));
        searchBox.setHint(Component.literal("поиск: ru, en-us, small, french..."));
        searchBox.setValue(query);
        searchBox.setResponder(s -> {
            query = s;
            scrollOffset = 0;
            pendingDelete = null;
            rebuildRows();
        });
        addRenderableWidget(searchBox);

        rebuildRows();
    }

    private int gx() {
        return (width - guiW) / 2;
    }

    private int gy() {
        return (height - GUI_H) / 2;
    }

    private int listStartY() {
        return gy() + 88;
    }

    private int actionsW() {
        return tab == TAB_INSTALLED ? BTN_W + DEL_GAP + DEL_W : BTN_W;
    }

    private int countInstalled() {
        return (int) Arrays.stream(Language.values()).filter(SpeechToText::isInstalled).count();
    }

    private boolean isSelected(Language l) {
        return l.name().equals(voiceConfig.getModel());
    }

    private void switchTab(int newTab) {
        if (tab == newTab) return;
        tab = newTab;
        scrollOffset = 0;
        pendingDelete = null;
        clearWidgets();
        init();
    }

    private void selectModel(Language lang) {
        voiceConfig.setModel(lang.name());
        ChatConfigManager.getInstance().save(VoiceConfig.class);
        rebuildRows();
    }

    private void startDownload(Language lang) {
        if (!DOWNLOADING.add(lang)) return;
        FAILED.remove(lang);
        SpeechToText.download(lang);
        clearWidgets();
        init();
    }

    private void deleteModel(Language lang) {
        if (pendingDelete != lang) {
            pendingDelete = lang;
            rebuildRows();
            return;
        }
        pendingDelete = null;
        if (!SpeechToText.delete(lang)) {
            Notification.showError("Не удалось удалить модель", null);
        }
        clearWidgets();
        init();
    }

    private List<Language> buildList() {
        String q = query.trim().toLowerCase();
        Comparator<Language> order = tab == TAB_INSTALLED
                ? Comparator.comparing((Language l) -> !isSelected(l))
                : Comparator.comparing((Language l) -> !DOWNLOADING.contains(l))
                .thenComparing(l -> !FAILED.contains(l));

        return Arrays.stream(Language.values())
                .filter(l -> (tab == TAB_INSTALLED) == SpeechToText.isInstalled(l))
                .filter(l -> q.isEmpty()
                             || l.getModel().toLowerCase().contains(q)
                             || l.getPath().toLowerCase().contains(q)
                             || l.name().toLowerCase().contains(q.replace(' ', '_')))
                .sorted(order)
                .toList();
    }

    private void rebuildRows() {
        rowWidgets.forEach(this::removeWidget);
        rowWidgets.clear();

        filtered = buildList();
        scrollOffset = Math.clamp(scrollOffset, 0, Math.max(0, filtered.size() - MAX_VISIBLE));

        int startY = listStartY();
        int btnX = gx() + guiW - PAD - actionsW();
        int to = Math.min(filtered.size(), scrollOffset + MAX_VISIBLE);

        for (int i = scrollOffset; i < to; i++) {
            Language lang = filtered.get(i);
            int rowY = startY + (i - scrollOffset) * (ROW_H + ROW_GAP);

            SimpleButton btn;
            if (tab == TAB_INSTALLED) {
                btn = isSelected(lang)
                        ? SimpleButton.of(btnX, rowY, BTN_W, ROW_H,
                                          Component.literal("ИСПОЛЬЗУЕТСЯ"), COL_GREEN, b -> {})
                        : SimpleButton.of(btnX, rowY, BTN_W, ROW_H,
                                          Component.literal("ВЫБРАТЬ"), COL_PURPLE, b -> selectModel(lang));
            } else if (DOWNLOADING.contains(lang)) {
                btn = SimpleButton.of(btnX, rowY, BTN_W, ROW_H,
                                      Component.literal("ЗАГРУЗКА..."), COL_YELLOW, b -> {});
            } else {
                boolean failed = FAILED.contains(lang);
                btn = SimpleButton.of(
                        btnX, rowY, BTN_W, ROW_H,
                        Component.literal(failed ? "ПОВТОРИТЬ" : "СКАЧАТЬ"),
                        failed ? COL_RED : COL_PURPLE,
                        b -> startDownload(lang)
                );
            }
            addRenderableWidget(btn);
            rowWidgets.add(btn);

            if (tab == TAB_INSTALLED) {
                boolean confirm = pendingDelete == lang;
                DeleteButton del = DeleteButton.of(
                        btnX + BTN_W + DEL_GAP, rowY, DEL_W, ROW_H,
                        confirm ? COL_YELLOW : COL_RED,
                        confirm,
                        b -> deleteModel(lang)
                );
                addRenderableWidget(del);
                rowWidgets.add(del);
            }
        }
    }

    @Override
    public void tick() {
        if (DIRTY.compareAndSet(true, false)) {
            clearWidgets();
            init();
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int gx = gx();
        int gy = gy();

        graphics.fill(gx, gy, gx + guiW, gy + GUI_H, COL_BG);
        graphics.outline(gx, gy, guiW, GUI_H, COL_DIVIDER);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(font, "ЯЗЫКОВЫЕ МОДЕЛИ", gx + PAD, gy + 10, COL_MUTED, false);
        String active = "Активна: " + voiceConfig.getModel();
        graphics.text(font, active, gx + guiW - PAD - font.width(active), gy + 10, COL_PURPLE, false);
        graphics.fill(gx + PAD, gy + 22, gx + guiW - PAD, gy + 23, COL_DIVIDER);
        graphics.fill(gx + PAD, gy + 82, gx + guiW - PAD, gy + 83, COL_DIVIDER);

        int startY = listStartY();
        int to = Math.min(filtered.size(), scrollOffset + MAX_VISIBLE);
        for (int i = scrollOffset; i < to; i++) {
            Language l = filtered.get(i);
            int rowY = startY + (i - scrollOffset) * (ROW_H + ROW_GAP);
            int textY = rowY + (ROW_H - 8) / 2;
            int rowRight = gx + guiW - PAD - actionsW() - 5;

            graphics.fill(gx + PAD, rowY, rowRight, rowY + ROW_H, COL_ROW);
            if (tab == TAB_INSTALLED && isSelected(l)) {
                graphics.fill(gx + PAD, rowY, gx + PAD + 2, rowY + ROW_H, COL_GREEN);
            }
            graphics.text(font, "[" + l.getPath() + "]", gx + PAD + 6, textY, COL_PURPLE, false);
            graphics.text(font, l.getModel(), gx + PAD + 6 + tagColW + 8, textY, COL_LIGHT, false);

            String size = formatSize(l.getSizeMb());
            graphics.text(font, size, rowRight - 5 - font.width(size), textY, COL_MUTED, false);
        }

        if (filtered.isEmpty()) {
            String empty = tab == TAB_INSTALLED
                    ? "Нет установленных моделей. Скачай их во вкладке «Загрузка»"
                    : "Ничего не найдено";
            graphics.text(font, empty, gx + PAD, startY + 4, COL_MUTED, false);
        }

        graphics.text(font, "Моделей: " + filtered.size(), gx + PAD, gy + GUI_H - 14, 0xFF434354, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (filtered.size() > MAX_VISIBLE) {
            int old = scrollOffset;
            scrollOffset = Math.clamp(scrollOffset - (long) Math.signum(scrollY), 0, filtered.size() - MAX_VISIBLE);
            if (old != scrollOffset) {
                pendingDelete = null;
                rebuildRows();
            }
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}