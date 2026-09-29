package ru.fozeton.diamondchat.gui.widgets;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class SearchEditBox extends EditBox {
    private Runnable onEnterAction;

    public SearchEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.setBordered(false);
        this.setTextColor(0xFF3f3f46);
    }

    public void setOnEnter(Runnable action) {
        this.onEnterAction = action;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if ((key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) && this.onEnterAction != null) {
                this.onEnterAction.run();
                return true;
            }

        return super.keyPressed(event);
    }

    @Override
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(getX() - 1, getY() - 1, getX() + this.width + 1, getY() + this.height + 1, 0xFF3f3f46);
        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0xFF09090b);

        int textOffsetY = (this.height - 8) / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(2, textOffsetY);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popMatrix();
    }
}