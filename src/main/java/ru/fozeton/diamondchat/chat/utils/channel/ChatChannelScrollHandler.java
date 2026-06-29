package ru.fozeton.diamondchat.chat.utils.channel;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.lwjgl.glfw.GLFW;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.events.InputEvent;
import ru.fozeton.diamondchat.chat.utils.ChatChannelView;
import ru.fozeton.diamondchat.utils.enums.DragMode;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

public class ChatChannelScrollHandler {
    private final ChannelsConfig.ChannelSettings settings;
    private final List<ChatChannelView.ChatLine> chatLines;
    private final ReadWriteLock lock;
    private final ChatChannelDragHandler dragHandler;
    private final ChatChannelGeometry geometry;
    private final ChatChannelEditOverlay editOverlay;
    private final int scrollHex;

    @Getter
    @Setter
    private double scrollOffset = 0;
    private boolean isPressedShift = false;

    public ChatChannelScrollHandler(
            ChannelsConfig.ChannelSettings settings,
            List<ChatChannelView.ChatLine> chatLines,
            ChatChannelDragHandler dragHandler,
            ReadWriteLock lock,
            ChatChannelGeometry geometry,
            ChatChannelEditOverlay editOverlay
    ) {
        this.settings = settings;
        this.chatLines = chatLines;
        this.lock = lock;
        this.dragHandler = dragHandler;
        this.geometry = geometry;
        this.editOverlay = editOverlay;
        this.scrollHex = (int) Long.decode(settings.getScrollColor()).longValue();
        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void render(GuiGraphics context, Font renderer, boolean isChatOpen) {
        if (!settings.isVisibleScroll() || !isChatOpen) return;

        int viewWidth = geometry.getViewWidth();
        int viewHeight = geometry.getViewHeight();
        int x = geometry.getX();
        int y = geometry.getY();

        int linesInView = viewHeight / (renderer.lineHeight + 3);
        int totalLines = chatLines.stream().mapToInt(l -> l.texts().size()).sum();

        if (totalLines <= linesInView) return;

        int paddingTop = 10;
        int paddingBot = 10;
        int trackH = viewHeight - paddingTop - paddingBot;
        int scrollBarH = Math.max(10, (int) ((float) trackH * linesInView / totalLines));
        int maxScroll = trackH - scrollBarH;
        int scrollBarY = y + paddingTop + (int) (maxScroll * (1.0 - scrollOffset / Math.max(1, totalLines - linesInView)));

        context.fill(x + viewWidth - 2, scrollBarY, x + viewWidth - 4, scrollBarY + scrollBarH, scrollHex);
    }

    @EventSubscriber(event = InputEvent.MouseScrollEvent.class)
    public void scrolled(InputEvent.MouseScrollEvent event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        double vertical = event.getVertical();

        int x = geometry.getX();
        int y = geometry.getY();
        int vw = geometry.getViewWidth();
        int vh = geometry.getViewHeight();

        if (mouseX < x || mouseX > x + vw || mouseY < y || mouseY > y + vh) return;

        Font renderer = Minecraft.getInstance().font;
        if (isPressedShift) vertical *= 5;

        int linesInView = vh / (renderer.lineHeight + 3);

        lock.writeLock().lock();
        try {
            int totalLines = chatLines.stream().mapToInt(l -> l.texts().size()).sum();
            int maxScroll = Math.max(0, totalLines - linesInView);
            scrollOffset = Math.clamp((int) (scrollOffset + vertical), 0, maxScroll);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @EventSubscriber(event = InputEvent.KeyInputEvent.class)
    public void keyPress(InputEvent.KeyInputEvent event) {
        boolean active = event.getAction() == InputEvent.KeyInputEvent.Action.PRESS || event.getAction() == InputEvent.KeyInputEvent.Action.HOLDING;

        if (event.getKeyCode() == GLFW.GLFW_KEY_LEFT_SHIFT) {
            isPressedShift = active;

        } else if (event.getKeyCode() == GLFW.GLFW_KEY_LEFT_CONTROL
                && Minecraft.getInstance().screen instanceof ChatScreen) {

            if (active) {
                if (dragHandler.getCurrentMode() == DragMode.NONE)
                    dragHandler.setCurrentMode(DragMode.EDIT);
            } else {
                if (dragHandler.getCurrentMode() != DragMode.NONE) {
                    geometry.persistDimensions();
                    dragHandler.setCurrentMode(DragMode.NONE);
                    editOverlay.getActiveResizing().clear();
                }
            }
        }
    }

    public void resetScroll() {
        lock.writeLock().lock();
        try {
            scrollOffset = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }
}