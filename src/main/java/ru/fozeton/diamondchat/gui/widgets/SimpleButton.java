package ru.fozeton.diamondchat.gui.widgets;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

public class SimpleButton extends Button {
    private final int color;

    private SimpleButton(int x, int y, int w, int h, Component label, int color, OnPress onPress) {
        super(x, y, w, h, label, onPress, DEFAULT_NARRATION);
        this.color = color;
    }

    public static SimpleButton of(int x, int y, int w, int h, Component label, int color, OnPress onPress) {
        return new SimpleButton(x, y, w, h, label, color, onPress);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        int bg = isHovered() ? 0xFF2a2a35 : 0xFF1a1a24;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.outline(getX(), getY(), width, height, isHovered() ? color : (color & 0x00FFFFFF) | 0x88000000);
        var font = Minecraft.getInstance().font;
        int tx = getX() + (width - font.width(getMessage())) / 2;
        int ty = getY() + (height - 8) / 2;
        g.text(font, getMessage(), tx, ty, color, false);
    }
}