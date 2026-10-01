package ru.fozeton.diamondchat.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class DeleteButton extends Button {
    private static final int CROSS_SIZE = 7;

    private final int color;
    private final boolean confirm;

    private DeleteButton(int x, int y, int w, int h, int color, boolean confirm, OnPress onPress) {
        super(x, y, w, h, Component.empty(), onPress, DEFAULT_NARRATION);
        this.color = color;
        this.confirm = confirm;
    }

    public static DeleteButton of(int x, int y, int w, int h, int color, boolean confirm, OnPress onPress) {
        return new DeleteButton(x, y, w, h, color, confirm, onPress);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int bg = isHovered() ? 0xFF2a2a35 : 0xFF1a1a24;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.outline(getX(), getY(), width, height, isHovered() ? color : (color & 0x00FFFFFF) | 0x88000000);

        if (confirm) {
            var font = Minecraft.getInstance().font;
            int tx = getX() + (width - font.width("?")) / 2;
            int ty = getY() + (height - font.lineHeight) / 2 + 1;
            g.text(font, "?", tx, ty, color, false);
            return;
        }

        int startX = getX() + (width - CROSS_SIZE) / 2;
        int startY = getY() + (height - CROSS_SIZE) / 2;
        for (int i = 0; i < CROSS_SIZE; i++) {
            g.fill(startX + i, startY + i, startX + i + 1, startY + i + 1, color);
            g.fill(startX + CROSS_SIZE - 1 - i, startY + i, startX + CROSS_SIZE - i, startY + i + 1, color);
        }
    }
}