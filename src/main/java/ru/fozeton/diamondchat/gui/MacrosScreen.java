package ru.fozeton.diamondchat.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.config.MacrosConfig;
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
    private static final int COL_BG = 0xFF212128;
    private static final int COL_DIVIDER = 0xFF2e2e3d;
    private static final int COL_MUTED = 0xFF6b6b88;
    private static final int COL_PURPLE = 0xFF7c6ef5;
    private static final int COL_GREEN = 0xFF4ade80;
    private static final int COL_RED = 0xFFf87171;

    private final MacrosConfig config;
    private final List<String> keys = new ArrayList<>();

    private EditBox textInput;
    private EditBox intervalInput;

    private int scrollOffset = 0;

    public MacrosScreen() {
        super(Component.literal("Макросы"));
        this.config = ChatConfigManager.getInstance().getMacrosConfig();
        this.keys.addAll(config.getMacros().keySet());
    }

    private static String truncate(String s, int max) {
        return s.length() > max ? s.substring(0, max - 1) + "…" : s;
    }

    @Override
    protected void init() {
        int guiX = (this.width - GUI_W) / 2;
        int guiY = (this.height - GUI_H) / 2;

        int inputY = guiY + GUI_H - 60;

        textInput = new EditBox(this.font, guiX + PAD, inputY, MACRO_W, ROW_H, Component.literal("!текст макроса..."));
        textInput.setMaxLength(256);
        textInput.setHint(Component.literal("!текст макроса..."));
        addRenderableWidget(textInput);

        intervalInput = new EditBox(this.font, guiX + PAD + MACRO_W + 5, inputY, TIMER_W, ROW_H, Component.literal("сек..."));
        intervalInput.setHint(Component.literal("сек..."));
        addRenderableWidget(intervalInput);

        int addX = guiX + PAD + MACRO_W + 5 + TIMER_W + 5;
        int addW = GUI_W - PAD - (MACRO_W + TIMER_W) - 20;
        addRenderableWidget(SimpleButton.of(addX, inputY, addW, ROW_H, Component.literal("+ ДОБАВИТЬ"), COL_PURPLE, btn -> addMacro()));

        rebuildMacroButtons(guiX, guiY);
    }

    private void rebuildMacroButtons(int guiX, int guiY) {
        int startY = guiY + 80;
        int visible = Math.min(keys.size(), MAX_VISIBLE);
        int from = Math.min(scrollOffset, Math.max(0, keys.size() - MAX_VISIBLE));

        for (int i = from; i < from + visible && i < keys.size(); i++) {
            String key = keys.get(i);
            MacrosConfig.Macros m = config.getMacros().get(key);
            if (m == null) continue;

            int rowY = startY + (i - from) * (ROW_H + ROW_GAP);

            String label = truncate(key, 24) + (m.isActive() ? " ВКЛ" : " ВЫКЛ");
            int labelColor = m.isActive() ? COL_GREEN : COL_MUTED;
            addRenderableWidget(SimpleButton.of(guiX + PAD, rowY, MACRO_W, ROW_H,
                    Component.literal(label), labelColor, btn -> {
                        m.setActive(!m.isActive());
                        ChatConfigManager.getInstance().save(config.getClass());
                        rebuildWidgets();
                    }));

            addRenderableWidget(SimpleButton.of(guiX + PAD + MACRO_W + 5, rowY, TIMER_W, ROW_H,
                    Component.literal(m.getTime() + "с"), COL_MUTED, btn -> {
                    }));

            addRenderableWidget(SimpleButton.of(guiX + PAD + MACRO_W + 5 + TIMER_W + 5, rowY, DEL_W, ROW_H,
                    Component.literal("✕"), COL_RED, btn -> {
                        config.getMacros().remove(key);
                        keys.remove(key);
                        ChatConfigManager.getInstance().save(config.getClass());
                        rebuildWidgets();
                    }));
        }
    }

    protected void rebuildWidgets() {
        this.clearWidgets();
        this.init();
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
            scrollOffset = Math.max(0, Math.min(scrollOffset - (int) Math.signum(scrollY), keys.size() - MAX_VISIBLE));
            rebuildWidgets();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void addMacro() {
        String key = textInput.getValue().trim();
        String rawTime = intervalInput.getValue().trim();
        if (key.isEmpty() || config.getMacros().containsKey(key)) return;

        int time = 30;
        try {
            time = Math.max(1, Integer.parseInt(rawTime));
        } catch (NumberFormatException ignored) {
        }

        MacrosConfig.Macros entry = new MacrosConfig.Macros(time, true);
        config.getMacros().put(key, entry);
        keys.add(key);
        ChatConfigManager.getInstance().save(config.getClass());
        rebuildWidgets();
    }
}