package ru.fozeton.diamondchat.gui.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class SearchEditBox extends EditBox {
    private Runnable onEnterAction;

    public SearchEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.setBordered(false);
        this.setTextColor(0x3f3f46);
    }

    public void setOnEnter(Runnable action) {
        this.onEnterAction = action;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            if (this.onEnterAction != null) {
                this.onEnterAction.run();
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX() - 1, getY() - 1, getX() + this.width + 1, getY() + this.height + 1, 0xFF3f3f46);
        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0xFF09090b);

        int textOffsetY = (this.height - 8) / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(2, textOffsetY, 0);
        super.renderWidget(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popPose();
    }
}