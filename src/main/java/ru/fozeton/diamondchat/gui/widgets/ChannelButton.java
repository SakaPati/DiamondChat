package ru.fozeton.diamondchat.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class ChannelButton extends AbstractButton {
    private static final int DEFAULT_BG = 0xFF18181f;
    private static final int DEFAULT_BG_HOVER = 0xFF1e1e2a;
    private static final int DEFAULT_BG_ACTIVE = 0xFF23233a;
    private static final int DEFAULT_BORDER = 0xFF3a2e6e;
    private static final int DEFAULT_BORDER_HOVER = 0xFF7c6ef5;
    private static final int DEFAULT_ACCENT = 0xFF7c6ef5;
    private static final int DEFAULT_LABEL = 0xFFd4d0f5;
    private static final int DEFAULT_LABEL_HOVER = 0xFFede9fe;
    private final Runnable onPress;
    private final Supplier<Boolean> selectedCheck;

    public ChannelButton(
            int x,
            int y,
            int width,
            int height,
            Component message,
            Runnable onPress,
            Supplier<Boolean> selectedCheck
    ) {
        super(x, y, width, height, message);
        this.onPress = onPress;
        this.selectedCheck = selectedCheck;
    }

    @Override
    public void onPress() {
        this.onPress.run();
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        boolean selected = this.selectedCheck.get();
        boolean hovered = isHoveredOrFocused();

        int bg = selected ? DEFAULT_BG_ACTIVE : hovered ? DEFAULT_BG_HOVER : DEFAULT_BG;
        int border = selected || hovered ? DEFAULT_BORDER_HOVER : DEFAULT_BORDER;
        int lblCol = selected || hovered ? DEFAULT_LABEL_HOVER : DEFAULT_LABEL;

        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, bg);

        // Обводка
        graphics.fill(getX(), getY(), getX() + this.width, getY() + 1, border);
        graphics.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, border);
        graphics.fill(getX(), getY(), getX() + 1, getY() + this.height, border);
        graphics.fill(getX() + this.width - 1, getY(), getX() + this.width, getY() + this.height, border);

        // Акцентная полоса (3px)
        graphics.fill(getX(), getY(), getX() + 3, getY() + this.height, DEFAULT_ACCENT);

        Minecraft mc = Minecraft.getInstance();
        int textY = getY() + (this.height - mc.font.lineHeight) / 2;
        graphics.drawString(mc.font, getMessage(), getX() + 9, textY, lblCol, false);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }
}