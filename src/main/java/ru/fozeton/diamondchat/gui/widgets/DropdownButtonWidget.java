package ru.fozeton.diamondchat.gui.widgets;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class DropdownButtonWidget extends AbstractWidget {
    private static final int BG = 0xFF18181F;
    private static final int BG_ITEM = 0xFF1E1E2A;
    private static final int BG_ITEM_HOVER = 0xFF2A2A3A;
    private static final int BORDER_ACTIVE = 0xFF7C6EF5;
    private static final int TEXT_ACTIVE = 0xFFA78BFA;
    private static final int BORDER_INACTIVE = 0xFF3A2E6E;
    private static final int TEXT_INACTIVE = 0xFF6B6B88;
    private static final int BORDER_HOVER = 0xFF5A4A9E;
    private static final int TEXT_HOVER = 0xFF8878CC;
    private static final int ITEM_TEXT = 0xFFCCCCDD;
    private static final int ITEM_TEXT_HOVER = 0xFFA78BFA;
    private static final int ITEM_HEIGHT = 13;
    private static final int ITEM_PADDING_X = 6;
    private final List<DropdownItem> items = new ArrayList<>();
    @Getter
    private boolean open = false;
    public DropdownButtonWidget(int x, int y, int width, int height, Component label) {
        super(x, y, width, height, label);
    }

    public void addItem(String label, Runnable action) {
        items.add(new DropdownItem(label, action));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return false;

        if (isMouseOver(mouseX, mouseY)) {
            open = !open;
            return true;
        }

        if (open) {
            int ix = getX();
            int iw = getDropdownWidth();
            int startY = getDropdownStartY();

            for (int i = 0; i < items.size(); i++) {
                int iy = startY + i * ITEM_HEIGHT;
                if (mouseX >= ix && mouseX <= ix + iw && mouseY >= iy && mouseY <= iy + ITEM_HEIGHT) {
                    items.get(i).action().run();
                    open = false;
                    return true;
                }
            }

            open = false;
        }

        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        boolean hovered = isMouseOver(mouseX, mouseY);
        int border = open ? BORDER_ACTIVE : hovered ? BORDER_HOVER : BORDER_INACTIVE;
        int textColor = open ? TEXT_ACTIVE : hovered ? TEXT_HOVER : TEXT_INACTIVE;

        graphics.fill(getX(), getY(), getX() + width, getY() + height, BG);
        graphics.renderOutline(getX(), getY(), width, height, border);

        var font = Minecraft.getInstance().font;
        String label = getMessage().getString() + (open ? " ▲" : " ▼");
        int textX = getX() + (width - font.width(label)) / 2;
        int textY = getY() + (height - 8) / 2;
        graphics.drawString(font, label, textX, textY, textColor, false);

        if (open && !items.isEmpty()) {
            int iw = getDropdownWidth();
            int startY = getDropdownStartY();

            graphics.fill(getX(), startY, getX() + iw, startY + items.size() * ITEM_HEIGHT, BG);
            graphics.renderOutline(getX(), startY - 1, iw, items.size() * ITEM_HEIGHT + 1, BORDER_ACTIVE);

            for (int i = 0; i < items.size(); i++) {
                int iy = startY + i * ITEM_HEIGHT;
                boolean itemHovered = mouseX >= getX() && mouseX <= getX() + iw && mouseY >= iy && mouseY <= iy + ITEM_HEIGHT;

                if (itemHovered) {
                    graphics.fill(getX() + 1, iy, getX() + iw - 1, iy + ITEM_HEIGHT, BG_ITEM_HOVER);
                } else if (i % 2 == 0) {
                    graphics.fill(getX() + 1, iy, getX() + iw - 1, iy + ITEM_HEIGHT, BG_ITEM);
                }

                int color = itemHovered ? ITEM_TEXT_HOVER : ITEM_TEXT;
                graphics.drawString(font, items.get(i).label(),
                        getX() + ITEM_PADDING_X,
                        iy + (ITEM_HEIGHT - 8) / 2,
                        color, false);
            }
        }
    }

    private int getDropdownWidth() {
        var font = Minecraft.getInstance().font;
        int maxW = width;
        for (var item : items) {
            maxW = Math.max(maxW, font.width(item.label()) + ITEM_PADDING_X * 2);
        }
        return maxW;
    }

    private int getDropdownStartY() {
        return getY() - items.size() * ITEM_HEIGHT;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }

    public record DropdownItem(String label, Runnable action) {
    }
}