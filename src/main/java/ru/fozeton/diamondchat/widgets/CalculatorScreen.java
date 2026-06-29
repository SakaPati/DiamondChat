package ru.fozeton.diamondchat.widgets;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.diamondchat.configs.LevelsConfig;

public class CalculatorScreen extends Screen {
    private static final int GUI_W = 360;
    private static final int GUI_H = 275;
    private static final int COL_BG = 0xFF18181f;
    private static final int COL_LINE = 0xFF323242;
    private static final int COL_MUTED = 0xFF6b6b88;
    private static final int COL_PURPLE = 0xFFa78bfa;
    private static final int COL_LIGHT = 0xFFc8c4f0;
    private static final int COL_HEADER = 0xFF9d9bb8;
    private final LevelsConfig config;
    private EditBox inpCur;
    private EditBox inpTgt;
    private String resDiff = "—";
    private String resMoney = "—";
    private String resBlocks = "—";

    public CalculatorScreen() {
        super(Component.literal("Калькулятор уровней"));
        this.config = ChatConfigManager.getInstance().loadResource(LevelsConfig.class, "diamondchat", "assets/data");
    }

    private static String formatNum(long n) {
        if (n >= 1_000_000) return String.format("%.1fM", n / 1_000_000.0);
        if (n >= 1_000) return String.format("%.1fK", n / 1_000.0);
        return String.valueOf(n);
    }

    @Override
    protected void init() {
        int gx = (this.width - GUI_W) / 2;
        int gy = (this.height - GUI_H) / 2;

        inpCur = new EditBox(font, gx + 126, gy + 70, 209, 21, Component.literal(""));
        inpCur.setHint(Component.literal("текущий уровень"));
        inpCur.setResponder(s -> calculate());
        addRenderableWidget(inpCur);

        inpTgt = new EditBox(font, gx + 126, gy + 92, 209, 21, Component.literal(""));
        inpTgt.setHint(Component.literal("целевой уровень"));
        inpTgt.setResponder(s -> calculate());
        addRenderableWidget(inpTgt);
    }

    private void calculate() {
        String cs = inpCur.getValue().trim();
        String ts = inpTgt.getValue().trim();
        if (cs.isEmpty() || ts.isEmpty()) {
            resetResults("—");
            return;
        }
        try {
            int cur = Integer.parseInt(cs);
            int tgt = Integer.parseInt(ts);
            if (cur <= 0 || tgt <= 0) {
                resetResults("Уровень > 0");
                return;
            }
            if (tgt > 535) {
                resetResults("Макс. 535 ур.");
                return;
            }
            if (cur >= tgt) {
                resetResults("Текущий >= Целевого");
                return;
            }

            long totalMoney = 0;
            for (int lvl = cur + 1; lvl <= tgt; lvl++) {
                LevelsConfig.LevelData d = config.getLevels().get(String.valueOf(lvl));
                if (d != null) totalMoney += d.getMoney();
            }

            LevelsConfig.LevelData tgtData = config.getLevels().get(String.valueOf(tgt));
            if (tgtData == null) {
                resetResults("Нет данных");
                return;
            }

            resDiff = (tgt - cur) + " ур.";
            resMoney = formatNum(totalMoney);
            resBlocks = formatNum(tgtData.getBlocks());
        } catch (NumberFormatException e) {
            resetResults("Введите числа");
        }
    }

    private void resetResults(String v) {
        resDiff = resMoney = resBlocks = v;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        int gx = (this.width - GUI_W) / 2;
        int gy = (this.height - GUI_H) / 2;

        g.fill(gx, gy, gx + GUI_W, gy + GUI_H, COL_BG);
        g.renderOutline(gx, gy, GUI_W, GUI_H, COL_LINE);

        g.drawString(font, "CALCULATOR — УРОВНИ", gx + 25, gy + 20, COL_HEADER, false);
        g.fill(gx + 25, gy + 40, gx + 335, gy + 41, COL_LINE);

        g.drawString(font, "ПАРАМЕТРЫ", gx + 25, gy + 53, COL_MUTED, false);

        g.fill(gx + 25, gy + 70, gx + 125, gy + 91, 0xFF14141a);
        g.drawString(font, "ТЕКУЩИЙ", gx + 32, gy + 76, 0xFF7c7a9a, false);
        g.fill(gx + 25, gy + 91, gx + 335, gy + 92, 0xFF232330);

        g.fill(gx + 25, gy + 92, gx + 125, gy + 113, 0xFF14141a);
        g.drawString(font, "ЦЕЛЕВОЙ", gx + 32, gy + 98, 0xFF7c7a9a, false);
        g.fill(gx + 25, gy + 113, gx + 335, gy + 114, COL_LINE);

        g.drawString(font, "РЕЗУЛЬТАТ", gx + 25, gy + 126, COL_MUTED, false);

        g.fill(gx + 25, gy + 142, gx + 125, gy + 163, 0xFF14141a);
        g.drawString(font, "РАЗНИЦА", gx + 32, gy + 148, COL_MUTED, false);
        g.drawString(font, resDiff, gx + 135, gy + 148, COL_PURPLE, false);
        g.fill(gx + 25, gy + 163, gx + 335, gy + 164, 0xFF232330);

        g.fill(gx + 25, gy + 164, gx + 125, gy + 185, 0xFF14141a);
        g.drawString(font, "ДЕНЬГИ", gx + 32, gy + 170, COL_MUTED, false);
        g.drawString(font, resMoney, gx + 135, gy + 170, COL_LIGHT, false);
        g.fill(gx + 25, gy + 185, gx + 335, gy + 186, 0xFF232330);

        g.fill(gx + 25, gy + 186, gx + 125, gy + 207, 0xFF14141a);
        g.drawString(font, "БЛОКИ", gx + 32, gy + 192, COL_MUTED, false);
        g.drawString(font, resBlocks, gx + 135, gy + 192, COL_LIGHT, false);

        g.drawString(font, "Результат обновляется автоматически", gx + 25, gy + 248, 0xFF434354, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}