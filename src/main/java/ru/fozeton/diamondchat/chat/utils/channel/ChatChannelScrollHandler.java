package ru.fozeton.diamondchat.chat.utils.channel;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.events.InputEvent;
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

    public void render(GuiGraphicsExtractor context, boolean isChatOpen) {
        if (!settings.isVisibleScroll() || !isChatOpen) return;

        int x = geometry.getX();
        int y = geometry.getY();
        int viewWidth = geometry.getViewWidth();
        int viewHeight = geometry.getViewHeight();

        Font renderer = Minecraft.getInstance().font;

        int linesInView = (int) (viewHeight / 1.4);
        int totalLines = metrics.getTotalPixels(chatLines, renderer, viewWidth);

        if (totalLines <= linesInView) return;

        int paddingTop = 10;
        int paddingBot = 10;
        int trackH = viewHeight - paddingTop - paddingBot;
        int scrollBarH = Math.max(10, (int) ((float) trackH * viewHeight / totalLines));
        int maxScroll = trackH - scrollBarH;
        int scrollBarY =
                y + paddingTop + (int) (maxScroll * (1.0 - scrollOffset / Math.max(1, totalLines - viewHeight + 3)));

        context.fill(x + viewWidth - 2, scrollBarY, x + viewWidth - 4, scrollBarY + scrollBarH, scrollHex);
    }

    @EventSubscriber(event = InputEvent.MouseScrollEvent.class)
    public void scrolled(InputEvent.MouseScrollEvent event) {
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        double vertical = event.getVertical();

        int x = geometry.getX();
        int y = geometry.getY();
        int viewWidth = geometry.getViewWidth();
        int viewHeight = geometry.getViewHeight();

        Font renderer = Minecraft.getInstance().font;

        if (mouseX < x || mouseX > x + viewWidth || mouseY < y || mouseY > y + viewHeight) return;

        if (isPressedShift) vertical *= 5;

        int totalPixels = metrics.getTotalPixels(chatLines, renderer, viewWidth);
        int maxScroll = Math.max(0, totalPixels - viewHeight + 3);
        scrollOffset = Math.clamp((int) (scrollOffset + vertical * 12), 0, maxScroll);
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