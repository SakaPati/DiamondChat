package ru.fozeton.diamondchat.chat.utils.channel;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.diamondchat.utils.enums.DragMode;
import ru.fozeton.diamondchat.utils.enums.Resizing;

@Getter
@Setter
public class ChatChannelDragHandler {
    private final ChannelsConfig.ChannelSettings settings;
    private final ChatChannelGeometry geometry;
    private final ChatChannelEditOverlay editOverlay;

    private DragMode currentMode = DragMode.NONE;

    public ChatChannelDragHandler(ChannelsConfig.ChannelSettings settings, ChatChannelGeometry geometry) {
        this.settings = settings;
        this.geometry = geometry;
        this.editOverlay = new ChatChannelEditOverlay(this, geometry);
    }

    public void render(
            GuiGraphicsExtractor context,
            Font renderer,
            int mouseX,
            int mouseY,
            boolean isChatOpen
    ) {
        if (!settings.isVisible()) return;
        editOverlay.render(context, renderer, mouseX, mouseY, isChatOpen);

        if (!isChatOpen) return;
        if (currentMode == DragMode.NONE || currentMode == DragMode.EDIT) return;

        Minecraft client = Minecraft.getInstance();
        int dx = mouseX - editOverlay.getDragStartMouseX();
        int dy = mouseY - editOverlay.getDragStartMouseY();

        int screenW = client.getWindow().getGuiScaledWidth();
        int screenH = client.getWindow().getGuiScaledHeight();

        if (currentMode == DragMode.MOVE) {
            int newX = editOverlay.getDragStartPosX() + dx;
            int newY = editOverlay.getDragStartPosY() + dy;
            int vw = geometry.getViewWidth();
            int vh = geometry.getViewHeight();

            newX = Math.clamp(newX, 0, screenW - vw);
            newY = Math.clamp(newY, 0, screenH - vh);

            geometry.setX(newX);
            geometry.setY(newY);

        } else {
            int newX = geometry.getX();
            int newY = geometry.getY();
            int newW = editOverlay.getDragStartViewW();
            int newH = editOverlay.getDragStartViewH();

            for (Resizing side : editOverlay.getActiveResizing()) {
                switch (side) {
                    case RIGHT -> {
                        newW = Math.max(ChatChannelGeometry.MIN_VIEW_W, editOverlay.getDragStartViewW() + dx);
                        if (editOverlay.getDragStartPosX() + newW > screenW)
                            newW = screenW - editOverlay.getDragStartPosX();
                    }
                    case LEFT -> {
                        int delta = Math.min(dx, editOverlay.getDragStartViewW() - ChatChannelGeometry.MIN_VIEW_W);
                        newW = editOverlay.getDragStartViewW() - delta;
                        newX = editOverlay.getDragStartPosX() + delta;
                        if (newX < 0) {
                            newW += newX;
                            newX = 0;
                        }
                    }
                    case BOTTOM -> {
                        newH = Math.max(ChatChannelGeometry.MIN_VIEW_H, editOverlay.getDragStartViewH() + dy);
                        if (editOverlay.getDragStartPosY() + newH > screenH)
                            newH = screenH - editOverlay.getDragStartPosY();
                    }
                    case TOP -> {
                        int delta = Math.min(dy, editOverlay.getDragStartViewH() - ChatChannelGeometry.MIN_VIEW_H);
                        newH = editOverlay.getDragStartViewH() - delta;
                        newY = editOverlay.getDragStartPosY() + delta;
                        if (newY < 0) {
                            newH += newY;
                            newY = 0;
                        }
                    }
                }
            }

            int prevW = geometry.getViewWidth();
            geometry.setWidth(newW * 2);
            geometry.setHeight(newH * 2);
            geometry.setX(newX);
            geometry.setY(newY);

            if (prevW != newW) geometry.rebuildLines(renderer, newW);
        }
    }

}