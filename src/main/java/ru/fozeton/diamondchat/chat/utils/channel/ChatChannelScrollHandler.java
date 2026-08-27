package ru.fozeton.diamondchat.chat.utils.channel;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.events.InputEvent;
import ru.fozeton.chatmanager.messages.MessageType;
import ru.fozeton.diamondchat.chat.utils.ChatChannelView;
import ru.fozeton.diamondchat.chat.utils.ChatMetrics;

import java.util.Collection;

public class ChatChannelScrollHandler {
    private final ChannelsConfig.ChannelSettings settings;
    private final Collection<ChatChannelView.ChatLine> chatLines;
    private final ChatChannelGeometry geometry;
    private final ChatMetrics metrics = ChatMetrics.getInstance();
    private final int scrollHex;

    @Getter
    @Setter
    private double scrollOffset = 0;
    private boolean isPressedShift = false;

    public ChatChannelScrollHandler(
            ChannelsConfig.ChannelSettings settings,
            Collection<ChatChannelView.ChatLine> chatLines,
            ChatChannelGeometry geometry
    ) {
        this.settings = settings;
        this.chatLines = chatLines;
        this.geometry = geometry;
        this.scrollHex = (int) Long.decode(settings.getScrollColor()).longValue();
        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void render(GuiGraphics context, Font renderer, boolean isChatOpen) {
        if (!settings.isVisibleScroll() || !isChatOpen) return;

        int viewWidth = geometry.getViewWidth();
        int viewHeight = geometry.getViewHeight();
        int x = geometry.getX();
        int y = geometry.getY();

        int lineStep = renderer.lineHeight + 3;
        int linesInView = viewHeight / lineStep;
        int totalLines = metrics.getTotalLineSlots(chatLines, lineStep);

        if (totalLines <= linesInView) return;

        int paddingTop = 10;
        int paddingBot = 10;
        int trackH = viewHeight - paddingTop - paddingBot;
        int scrollBarH = Math.max(10, (int) ((float) trackH * linesInView / totalLines));
        int maxScroll = trackH - scrollBarH;
        int scrollBarY =
                y + paddingTop + (int) (maxScroll * (1.0 - scrollOffset / Math.max(1, totalLines - linesInView)));

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

        int lineStep = renderer.lineHeight + 3;
        int linesInView = vh / lineStep;
        if (isPressedShift || isScrollingOverGif((int) scrollOffset, lineStep)) vertical *= 5;

        int totalLines = metrics.getTotalLineSlots(chatLines, lineStep);
        int maxScroll = Math.max(0, totalLines - linesInView);
        scrollOffset = Math.clamp((int) (scrollOffset + vertical), 0, maxScroll);
    }

    private boolean isScrollingOverGif(int scrollOffset, int lineStep) {
        int accumulated = 0;
        for (ChatChannelView.ChatLine chatLine : chatLines) {
            int slots = metrics.getLineSlots(chatLine, lineStep);
            int rangeStart = accumulated;
            int rangeEnd = accumulated + slots;

            if (scrollOffset >= rangeStart && scrollOffset < rangeEnd) {
                return chatLine.message().getType() == MessageType.ANIMATED;
            }

            accumulated = rangeEnd;
        }
        return false;
    }

    @EventSubscriber(event = InputEvent.KeyInputEvent.class)
    public void keyPress(InputEvent.KeyInputEvent event) {
        boolean active = event.getAction() == InputEvent.KeyInputEvent.Action.PRESS ||
                         event.getAction() == InputEvent.KeyInputEvent.Action.HOLDING;

        if (event.getKeyCode() == GLFW.GLFW_KEY_LEFT_SHIFT) isPressedShift = active;
    }

    public void resetScroll() {
        scrollOffset = 0;
    }
}