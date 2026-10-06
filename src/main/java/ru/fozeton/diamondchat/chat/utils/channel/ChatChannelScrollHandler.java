package ru.fozeton.diamondchat.chat.utils.channel;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.lwjgl.glfw.GLFW;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.events.InputEvent;
import ru.fozeton.diamondchat.chat.utils.ChatMetrics;

public class ChatChannelScrollHandler {
    private final ChannelsConfig.ChannelSettings settings;
    private final ChatChannel channel;
    private final ChatChannelGeometry geometry;
    private final ChatMetrics metrics = ChatMetrics.getInstance();
    private final int scrollHex;

    @Getter
    @Setter
    private double scrollOffset = 0;
    private boolean isPressedShift = false;

    public ChatChannelScrollHandler(
            ChannelsConfig.ChannelSettings settings,
            ChatChannel channel,
            ChatChannelGeometry geometry
    ) {
        this.settings = settings;
        this.channel = channel;
        this.geometry = geometry;
        this.scrollHex = (int) ((Number) settings.getScrollColor()).longValue();
        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void render(GuiGraphicsExtractor context, boolean isChatOpen) {
        if (!settings.isVisibleScroll() || !isChatOpen) return;

        int x = geometry.getX();
        int y = geometry.getY();
        int viewWidth = geometry.getViewWidth();
        int viewHeight = geometry.getViewHeight();

        int totalPixels = totalPixels(viewWidth);

        int scrollRange = totalPixels - viewHeight + 3;
        if (scrollRange <= 0) return;

        int paddingTop = 10;
        int paddingBot = 10;
        int trackH = viewHeight - paddingTop - paddingBot;
        int scrollBarH = Math.clamp((int) ((float) trackH * viewHeight / totalPixels), 10, trackH);
        int travel = trackH - scrollBarH;

        double progress = Math.clamp(scrollOffset / scrollRange, 0.0, 1.0);
        int scrollBarY = y + paddingTop + (int) (travel * (1.0 - progress));

        context.fill(x + viewWidth - 4, scrollBarY, x + viewWidth - 2, scrollBarY + scrollBarH, scrollHex);
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

        if (mouseX < x || mouseX > x + viewWidth || mouseY < y || mouseY > y + viewHeight) return;

        if (isPressedShift) vertical *= 5;

        int maxScroll = Math.max(0, totalPixels(viewWidth) - viewHeight + 3);
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

    private int totalPixels(int viewWidth) {
        Font renderer = Minecraft.getInstance().font;
        return channel.withMessages(messages -> metrics.getTotalPixels(messages, renderer, viewWidth));
    }
}