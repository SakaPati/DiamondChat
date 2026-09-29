package ru.fozeton.diamondchat.gui.widgets;

import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class TabButtonWidget extends AbstractWidget {
    private static final int BG = 0xFF18181F;
    private static final int BORDER_ACTIVE = 0xFF7C6EF5;
    private static final int TEXT_ACTIVE = 0xFFA78BFA;
    private static final int BORDER_INACTIVE = 0xFF3A2E6E;
    private static final int TEXT_INACTIVE = 0xFF6B6B88;
    private static final int BORDER_HOVER = 0xFF5A4A9E;
    private static final int TEXT_HOVER = 0xFF8878CC;

    private final Runnable onLeftClick;
    private final Runnable onRightClick;
    @Setter
    private boolean active = true;

    public TabButtonWidget(int x, int y, int width, int height,
                           Component label, Runnable onLeftClick, Runnable onRightClick) {
        super(x, y, width, height, label);
        this.onLeftClick = onLeftClick;
        this.onRightClick = onRightClick;
    }

    public boolean isToggled() { return active; }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (!isMouseOver(event.x(), event.y())) return false;
        int button = event.button();
        if (button == 1) {
            if (onRightClick != null) onRightClick.run();
            return true;
        }
        if (button == 0) {
            active = !active;
            if (onLeftClick != null) onLeftClick.run();
            return true;
        }
        return false;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(getX(), getY(), getX() + width, getY() + height, BG);
        int border = active ? BORDER_ACTIVE : isHovered() ? BORDER_HOVER : BORDER_INACTIVE;
        graphics.outline(getX(), getY(), width, height, border);
        int textColor = active ? TEXT_ACTIVE : isHovered() ? TEXT_HOVER : TEXT_INACTIVE;
        var font = Minecraft.getInstance().font;
        int textX = getX() + (width - font.width(getMessage())) / 2;
        int textY = getY() + (height - 8) / 2;
        graphics.text(font, getMessage(), textX, textY, textColor, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}