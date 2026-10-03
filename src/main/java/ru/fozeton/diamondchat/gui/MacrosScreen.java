package ru.fozeton.diamondchat.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.config.MacrosConfig;
import ru.fozeton.diamondchat.gui.widgets.SearchEditBox;
import ru.fozeton.diamondchat.gui.widgets.SimpleButton;

import java.util.ArrayList;
import java.util.List;

public class MacrosScreen extends Screen {
    private static final int GUI_W = 370;
    private static final int GUI_H = 400;
    private static final int PAD = 10;
    private static final int MACRO_W = 200;
    private static final int TIMER_W = 65;
    private static final int DEL_W = 28;
    private static final int ROW_H = 20;
    private static final int ROW_GAP = 4;
    private static final int MAX_VISIBLE = 10;
    private static final int DEFAULT_TIME = 30;
    private static final int MAX_TIME = 86400;
    private static final int COL_BG = 0xFF212128;
    private static final int COL_DIVIDER = 0xFF2e2e3d;
    private static final int COL_MUTED = 0xFF6b6b88;
    private static final int COL_PURPLE = 0xFF7c6ef5;
    private static final int COL_GREEN = 0xFF4ade80;
    private static final int COL_RED = 0xFFf87171;

    private final MacrosConfig config;
    private final List<String> keys = new ArrayList<>();

    private SearchEditBox textInput;
    private SearchEditBox intervalInput;

    private int scrollOffset = 0;
    private String editingKey = null;

    public MacrosScreen() {
        super(Component.literal("Макросы"));
        this.config = ChatConfigManager.getInstance().getMacrosConfig();
        this.keys.addAll(config.getMacros().keySet());
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    private static int parseTime(String raw, int fallback) {
        try {
            return Math.clamp(Integer.parseInt(raw.trim()), 1, MAX_TIME);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    protected void init() {
        int guiX = (this.width - GUI_W) / 2;
        int guiY = (this.height - GUI_H) / 2;

        int inputY = guiY + GUI_H - 60;

        textInput = new SearchEditBox(
                this.font,
                guiX + PAD,
                inputY,
                MACRO_W,
                ROW_H,
                Component.literal("Текст макроса")
        );
        textInput.setMaxLength(256);
        textInput.setHint(Component.literal("!текст макроса..."));
        addRenderableWidget(textInput);

        intervalInput = new SearchEditBox(
                this.font,
                guiX + PAD + MACRO_W + 5,
                inputY,
                TIMER_W,
                ROW_H,
                Component.literal("Секунды")
        );
        intervalInput.setMaxLength(6);
        intervalInput.setDigitsOnly();
        intervalInput.setHint(Component.literal("сек..."));
        intervalInput.setOnEnter(this::addMacro);
        addRenderableWidget(intervalInput);

        int addX = guiX + PAD + MACRO_W + 5 + TIMER_W + 5;
        int addW = GUI_W - PAD - (MACRO_W + TIMER_W) - 20;
        addRenderableWidget(SimpleButton.of(
                addX,
                inputY,
                addW,
                ROW_H,
                Component.literal("+ ДОБАВИТЬ"),
                COL_PURPLE,
                btn -> addMacro()
        ));

        rebuildMacroButtons(guiX, guiY);
    }

    private void rebuildMacroButtons(int guiX, int guiY) {
        int startY = guiY + 80;
        scrollOffset = Math.clamp(scrollOffset, 0, Math.max(0, keys.size() - MAX_VISIBLE));
        int to = Math.min(keys.size(), scrollOffset + MAX_VISIBLE);

        for (int i = scrollOffset; i < to; i++) {
            String key = keys.get(i);
            MacrosConfig.Macros m = config.getMacros().get(key);
            if (m == null) continue;

            int rowY = startY + (i - scrollOffset) * (ROW_H + ROW_GAP);
            int timerX = guiX + PAD + MACRO_W + 5;

            String label = truncate(key, 24) + (m.isActive() ? " ВКЛ" : " ВЫКЛ");
            int labelColor = m.isActive() ? COL_GREEN : COL_MUTED;
            addRenderableWidget(SimpleButton.of(
                    guiX + PAD, rowY, MACRO_W, ROW_H,
                    Component.literal(label), labelColor, btn -> {
                        m.setActive(!m.isActive());
                        ChatConfigManager.getInstance().save(config.getClass());
                        rebuildWidgets();
                    }
            ));

            if (key.equals(editingKey)) {
                SearchEditBox edit = new SearchEditBox(
                        this.font,
                        timerX,
                        rowY,
                        TIMER_W,
                        ROW_H,
                        Component.literal("Время")
                );
                edit.setMaxLength(6);
                edit.setDigitsOnly();
                edit.setValue(String.valueOf(m.getTime()));
                edit.setOnEnter(() -> commitTime(m, edit.getValue()));
                addRenderableWidget(edit);
                setFocused(edit);
                edit.setFocused(true);
            } else {
                addRenderableWidget(SimpleButton.of(
                        timerX, rowY, TIMER_W, ROW_H,
                        Component.literal(m.getTime() + "с"), COL_PURPLE, btn -> {
                            editingKey = key;
                            rebuildWidgets();
                        }
                ));
            }

            addRenderableWidget(SimpleButton.of(
                    timerX + TIMER_W + 5, rowY, DEL_W, ROW_H,
                    Component.literal("✕"), COL_RED, btn -> {
                        config.getMacros().remove(key);
                        keys.remove(key);
                        if (key.equals(editingKey)) editingKey = null;
                        ChatConfigManager.getInstance().save(config.getClass());
                        rebuildWidgets();
                    }
            ));
        }
    }

    private void commitTime(MacrosConfig.Macros m, String raw) {
        m.setTime(parseTime(raw, m.getTime()));
        editingKey = null;
        ChatConfigManager.getInstance().save(config.getClass());
        rebuildWidgets();
    }

    protected void rebuildWidgets() {
        String text = textInput == null ? "" : textInput.getValue();
        String interval = intervalInput == null ? "" : intervalInput.getValue();
        this.clearWidgets();
        this.init();
        textInput.setValue(text);
        intervalInput.setValue(interval);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int guiX = (this.width - GUI_W) / 2;
        int guiY = (this.height - GUI_H) / 2;

        graphics.fill(guiX, guiY, guiX + GUI_W, guiY + GUI_H, COL_BG);
        graphics.outline(guiX, guiY, GUI_W, GUI_H, COL_DIVIDER);
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        graphics.text(font, "МАКРОСЫ", guiX + PAD, guiY + 10, COL_MUTED, false);
        graphics.fill(guiX + PAD, guiY + 22, guiX + GUI_W - PAD, guiY + 23, COL_DIVIDER);

        long active = config.getMacros().values().stream().filter(MacrosConfig.Macros::isActive).count();
        graphics.text(font, "АКТИВНЫХ: " + active, guiX + PAD, guiY + 30, COL_PURPLE, false);
        graphics.text(font, "ВСЕГО: " + keys.size(), guiX + PAD + 140, guiY + 30, COL_MUTED, false);

        graphics.text(font, "СПИСОК МАКРОСОВ", guiX + PAD, guiY + 50, 0xFF3d3d52, false);
        graphics.fill(guiX + PAD, guiY + 62, guiX + GUI_W - PAD, guiY + 63, COL_DIVIDER);

        int inputY = guiY + GUI_H - 60;
        graphics.fill(guiX + PAD, inputY - 8, guiX + GUI_W - PAD, inputY - 7, COL_DIVIDER);
        graphics.text(font, "ДОБАВИТЬ МАКРОС", guiX + PAD, inputY - 18, 0xFF3d3d52, false);

        if (keys.size() > MAX_VISIBLE) {
            graphics.text(font, "↑↓ scroll", guiX + GUI_W - 60, guiY + 50, COL_MUTED, false);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (keys.size() > MAX_VISIBLE) {
            scrollOffset = Math.clamp(scrollOffset - (long) Math.signum(scrollY), 0, keys.size() - MAX_VISIBLE);
            editingKey = null;
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void addMacro() {
        String key = textInput.getValue().trim();
        if (key.isEmpty() || config.getMacros().containsKey(key)) return;

        int time = parseTime(intervalInput.getValue(), DEFAULT_TIME);

        config.getMacros().put(key, new MacrosConfig.Macros(time, true));
        keys.add(key);
        ChatConfigManager.getInstance().save(config.getClass());

        textInput.setValue("");
        intervalInput.setValue("");
        rebuildWidgets();
    }
}