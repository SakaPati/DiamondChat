package ru.fozeton.diamondchat.chat.utils.channel;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import lombok.Getter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.events.InputEvent;
import ru.fozeton.chatmanager.utils.compat.providers.ScreenProvider;
import ru.fozeton.diamondchat.utils.enums.DragMode;
import ru.fozeton.diamondchat.utils.enums.Resizing;

import java.util.EnumSet;

public class ChatChannelEditOverlay {
    private final ChannelsConfig.ChannelSettings settings;
    private final ChatChannelDragHandler dragHandler;
    private final ChatChannelGeometry geometry;

    @Getter
    private final EnumSet<Resizing> activeResizing = EnumSet.noneOf(Resizing.class);

    private int lastMouseX, lastMouseY;

    @Getter
    private int dragStartMouseX, dragStartMouseY;
    @Getter
    private int dragStartViewW, dragStartViewH;
    @Getter
    private int dragStartPosX, dragStartPosY;

    public ChatChannelEditOverlay(ChatChannelDragHandler dragHandler, ChatChannelGeometry geometry) {
        this.dragHandler = dragHandler;
        this.geometry = geometry;
        this.settings = geometry.getSettings();
        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void render(GuiGraphicsExtractor context, Font renderer, int mouseX, int mouseY, boolean isChatOpen) {

        lastMouseX = mouseX;
        lastMouseY = mouseY;

        if (!isChatOpen || dragHandler.getCurrentMode() == DragMode.NONE) return;

        int x = geometry.getX();
        int y = geometry.getY();
        int vw = geometry.getViewWidth();
        int vh = geometry.getViewHeight();
        int markSize = settings.getEditMode().getMarkSize();

        context.outline(x - 8, y, vw + 8, vh - 2, 0xFF7c6ef5);

        boolean cornerTL = isNearTopLeftCorner(mouseX, mouseY);
        boolean cornerTR = isNearTopRightCorner(mouseX, mouseY);
        boolean cornerBL = isNearBottomLeftCorner(mouseX, mouseY);
        boolean cornerBR = isNearBottomRightCorner(mouseX, mouseY);
        boolean onRight = isNearRightEdge(mouseX, mouseY);
        boolean onLeft = isNearLeftEdge(mouseX, mouseY);
        boolean onBottom = isNearBottomEdge(mouseX, mouseY);
        boolean onTop = isNearTopEdge(mouseX, mouseY);

        renderHandle(context, handleMidX(), handleTopY(), markSize, markSize, onTop);
        renderHandle(context, handleMidX(), handleBottomY(), markSize, markSize, onBottom);
        renderHandle(context, handleLeftX(), handleMidY(), markSize, markSize, onLeft);
        renderHandle(context, handleRightX(), handleMidY(), markSize, markSize, onRight);
        renderHandle(context, handleLeftX(), handleTopY(), markSize, markSize, cornerTL);
        renderHandle(context, handleRightX(), handleTopY(), markSize, markSize, cornerTR);
        renderHandle(context, handleLeftX(), handleBottomY(), markSize, markSize, cornerBL);
        renderHandle(context, handleRightX(), handleBottomY(), markSize, markSize, cornerBR);

        String hint = vw + " × " + vh;
        context.text(renderer, Component.literal(hint),
                x + vw - renderer.width(hint) - 4,
                y + 4, 0xFFb8aff8, false);

        context.text(renderer, Component.literal(settings.getName()), x, y + 5, 0xFFb8aff8, false);
    }

    private int markSize() {
        return settings.getEditMode().getMarkSize();
    }

    private int handleRightX() {
        return geometry.getX() + geometry.getViewWidth() - markSize() / 2;
    }

    private int handleLeftX() {
        return geometry.getX() - 12;
    }

    private int handleTopY() {
        return geometry.getY() - markSize() / 2;
    }

    private int handleBottomY() {
        return geometry.getY() + geometry.getViewHeight() - markSize() + 2;
    }

    private int handleMidX() {
        return geometry.getX() + (geometry.getViewWidth() / 2) - markSize();
    }

    private int handleMidY() {
        return geometry.getY() + geometry.getViewHeight() / 2 - markSize() / 2;
    }

    private boolean isNearHandle(int mx, int my, int hx, int hy) {
        int s = markSize();
        return mx >= hx - s && mx <= hx + s * 2 && my >= hy - s && my <= hy + s * 2;
    }

    private boolean isNearTopLeftCorner(int mx, int my) {
        return isNearHandle(mx, my, handleLeftX(), handleTopY());
    }

    private boolean isNearTopRightCorner(int mx, int my) {
        return isNearHandle(mx, my, handleRightX(), handleTopY());
    }

    private boolean isNearBottomLeftCorner(int mx, int my) {
        return isNearHandle(mx, my, handleLeftX(), handleBottomY());
    }

    private boolean isNearBottomRightCorner(int mx, int my) {
        return isNearHandle(mx, my, handleRightX(), handleBottomY());
    }

    private boolean isNearRightEdge(int mx, int my) {
        return isNearHandle(mx, my, handleRightX(), handleMidY());
    }

    private boolean isNearLeftEdge(int mx, int my) {
        return isNearHandle(mx, my, handleLeftX(), handleMidY());
    }

    private boolean isNearTopEdge(int mx, int my) {
        return isNearHandle(mx, my, handleMidX(), handleTopY());
    }

    private boolean isNearBottomEdge(int mx, int my) {
        return isNearHandle(mx, my, handleMidX(), handleBottomY());
    }

    private boolean isInsideWindow(int mx, int my) {
        return mx >= geometry.getX() && mx <= geometry.getX() + geometry.getViewWidth()
                && my >= geometry.getY() && my <= geometry.getY() + geometry.getViewHeight();
    }

    private void renderHandle(GuiGraphicsExtractor ctx, int x, int y, int w, int h, boolean hovered) {
        ctx.fill(x, y, x + w, y + h, hovered ? Long.decode(settings.getEditMode().getMarkHover()).intValue() : Long.decode(settings.getEditMode().getMarkColor()).intValue());
        ctx.outline(x, y, w, h, 0xFF3a2f8a);
    }

    @EventSubscriber(event = InputEvent.MouseInputEvent.class)
    public void mouseInput(InputEvent.MouseInputEvent event) {
        if (!(ScreenProvider.isChatScreen())) return;
        if (dragHandler.getCurrentMode() == DragMode.NONE) return;
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT) return;

        if (event.getAction() == GLFW.GLFW_PRESS) {
            boolean cornerTL = isNearTopLeftCorner(lastMouseX, lastMouseY);
            boolean cornerTR = isNearTopRightCorner(lastMouseX, lastMouseY);
            boolean cornerBL = isNearBottomLeftCorner(lastMouseX, lastMouseY);
            boolean cornerBR = isNearBottomRightCorner(lastMouseX, lastMouseY);
            boolean onRight = isNearRightEdge(lastMouseX, lastMouseY);
            boolean onLeft = isNearLeftEdge(lastMouseX, lastMouseY);
            boolean onBottom = isNearBottomEdge(lastMouseX, lastMouseY);
            boolean onTop = isNearTopEdge(lastMouseX, lastMouseY);
            boolean inside = isInsideWindow(lastMouseX, lastMouseY);

            boolean anyEdge = cornerTL || cornerTR || cornerBL || cornerBR || onRight || onLeft || onBottom || onTop;

            if (!anyEdge && !inside) return;

            dragStartMouseX = lastMouseX;
            dragStartMouseY = lastMouseY;
            dragStartViewW = geometry.getViewWidth();
            dragStartViewH = geometry.getViewHeight();
            dragStartPosX = geometry.getX();
            dragStartPosY = geometry.getY();

            activeResizing.clear();

            if (anyEdge) {
                if (onRight || cornerTR || cornerBR) activeResizing.add(Resizing.RIGHT);
                if (onLeft || cornerTL || cornerBL) activeResizing.add(Resizing.LEFT);
                if (onBottom || cornerBL || cornerBR) activeResizing.add(Resizing.BOTTOM);
                if (onTop || cornerTL || cornerTR) activeResizing.add(Resizing.TOP);
                dragHandler.setCurrentMode(DragMode.RESIZE_WIDTH);
            } else {
                dragHandler.setCurrentMode(DragMode.MOVE);
            }

        } else if (event.getAction() == GLFW.GLFW_RELEASE) {
            DragMode mode = dragHandler.getCurrentMode();
            if (mode == DragMode.RESIZE_WIDTH
                    || mode == DragMode.RESIZE_HEIGHT
                    || mode == DragMode.MOVE) {
                geometry.persistDimensions();
                dragHandler.setCurrentMode(DragMode.EDIT);
                activeResizing.clear();
            }
        }
    }

    @EventSubscriber(event = InputEvent.KeyInputEvent.class)
    public void keyPress(InputEvent.KeyInputEvent event) {
        boolean active = event.getAction() == InputEvent.KeyInputEvent.Action.PRESS || event.getAction() == InputEvent.KeyInputEvent.Action.HOLDING;

        if (event.getKeyCode() == GLFW.GLFW_KEY_LEFT_CONTROL && ScreenProvider.isChatScreen()) {
            if (active) {
                if (dragHandler.getCurrentMode() == DragMode.NONE) dragHandler.setCurrentMode(DragMode.EDIT);
            } else {
                if (dragHandler.getCurrentMode() != DragMode.NONE) {
                    geometry.persistDimensions();
                    dragHandler.setCurrentMode(DragMode.NONE);
                    activeResizing.clear();
                }
            }
        }
    }
}