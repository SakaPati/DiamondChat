package ru.fozeton.diamondchat.gui.widgets;

import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.function.IntPredicate;

public class SearchEditBox extends EditBox {
    private static final int BG = 0xFF14141a;
    private static final int BG_FOCUSED = 0xFF18181f;
    private static final int BORDER = 0xFF3a2e6e;
    private static final int BORDER_HOVER = 0xFF5a4a9e;
    private static final int BORDER_FOCUSED = 0xFF7c6ef5;
    private static final int ACCENT = 0xFF7c6ef5;
    private static final int TEXT = 0xFFc8c4f0;
    private static final int HINT = 0xFF6b6b88;
    private static final int TEXT_PAD_X = 7;

    private Runnable onEnterAction;
    @Setter
    private IntPredicate charFilter = c -> true;

    public SearchEditBox(Font font, int x, int y, int width, int height, Component message) {
        super(font, x, y, width, height, message);
        this.setBordered(false);
        this.setTextColor(TEXT);
    }

    public void setOnEnter(Runnable action) {
        this.onEnterAction = action;
    }

    public void setDigitsOnly() {
        this.charFilter = Character::isDigit;
    }

    @Override
    public void insertText(String text) {
        String filtered = text.codePoints()
                .filter(charFilter)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
        if (!filtered.isEmpty()) super.insertText(filtered);
    }

    @Override
    public void setHint(Component hint) {
        super.setHint(hint.copy().withColor(HINT));
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
        boolean focused = this.isFocused();
        boolean hovered = this.isHovered();

        int border = focused ? BORDER_FOCUSED : hovered ? BORDER_HOVER : BORDER;

        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, focused ? BG_FOCUSED : BG);
        graphics.outline(getX(), getY(), this.width, this.height, border);
        graphics.fill(getX(), getY(), getX() + 3, getY() + this.height, focused ? ACCENT : BORDER);

        int textOffsetY = (this.height - 8) / 2;

        graphics.enableScissor(getX() + 3, getY() + 1, getX() + this.width - 2, getY() + this.height - 1);
        graphics.pose().pushMatrix();
        graphics.pose().translate(TEXT_PAD_X, textOffsetY);
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTick);
        graphics.pose().popMatrix();
        graphics.disableScissor();
    }
}