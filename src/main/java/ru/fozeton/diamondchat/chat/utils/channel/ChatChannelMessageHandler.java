package ru.fozeton.diamondchat.chat.utils.channel;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.events.PlayerMentionedEvent;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageType;
import ru.fozeton.chatmanager.utils.MessageColorRemapper;
import ru.fozeton.chatmanager.utils.PlaySounds;
import ru.fozeton.diamondchat.chat.utils.ChatChannelView;
import ru.fozeton.diamondchat.events.gui.GuiOpenEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;

public class ChatChannelMessageHandler {
    private final MessageColorRemapper colorRemapper = MessageColorRemapper.getInstance();
    private final PlaySounds sounds = PlaySounds.getInstance();
    private final ChatChannel channel;
    private final ChannelsConfig.ChannelSettings settings;
    private final Collection<ChatChannelView.ChatLine> chatLines;
    private final ChatChannelDragHandler dragHandler;
    private final ChatChannelGeometry geometry;
    private final ReadWriteLock lock;
    private final ChatChannelScrollHandler scrollHandler;

    private final int bgHex;
    private final int blinkHex;
    private long lastMentionTime = 0;

    public ChatChannelMessageHandler(
            ChatChannel channel,
            ChannelsConfig config,
            Collection<ChatChannelView.ChatLine> chatLines,
            ChatChannelDragHandler dragHandler,
            ChatChannelGeometry geometry,
            ReadWriteLock lock,
            ChatChannelScrollHandler scrollHandler
    ) {
        this.channel = channel;
        this.settings = config.getChannels().get(channel.getId());
        this.chatLines = chatLines;
        this.dragHandler = dragHandler;
        this.geometry = geometry;
        this.lock = lock;
        this.scrollHandler = scrollHandler;
        this.bgHex = (int) Long.decode(settings.getBackgroundColor()).longValue();
        this.blinkHex = (int) Long.decode(settings.getBlinkColor()).longValue();
        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void render(GuiGraphics context, int mouseX, int mouseY, Font renderer, boolean isChatOpen) {
        if (!settings.isVisible()) return;

        int x = geometry.getX();
        int y = geometry.getY();
        int viewWidth = geometry.getViewWidth();
        int viewHeight = geometry.getViewHeight();
        int currentY = y + viewHeight;

        long now = System.currentTimeMillis();
        long timeSince = now - lastMentionTime;

        final int baseBg = (timeSince < settings.getBlinkDuration())
                ? (Math.sin((double) timeSince / settings.getBlinkDuration()
                * Math.PI * settings.getBlinkSpeed()) > 0 ? blinkHex : bgHex)
                : bgHex;

        int fadeStartMs = settings.getFadingStartTime();
        int fadeDurationMs = settings.getFadingDuration();
        int maxVisible = channel.getMaxVisibleMessage();

        int renderedLinesCount = 0;
        int skippedLines = 0;

        Style hoveredStyle = null;

        lock.readLock().lock();
        try {
            for (ChatChannelView.ChatLine chatLine : chatLines) {
                List<FormattedCharSequence> lines = chatLine.texts();

                int stripeColor = switch (chatLine.message().getType()) {
                    case PLAYER, CLIENT, CUSTOM -> 0xFFffffff;
                    default -> 0xFF7c6ef5;
                };

                int backgroundColor = baseBg;

                Map<String, Object> metadata = chatLine.message().getMetadata();
                String lineColor = (String) metadata.get("lineColor");
                String borderColor = (String) metadata.get("borderColor");

                if (lineColor != null) backgroundColor = Long.decode(lineColor).intValue();
                if (borderColor != null) stripeColor = Long.decode(borderColor).intValue();

                float alpha = 1.0f;
                if (!isChatOpen) {
                    long age = Duration.between(chatLine.timestamp(), Instant.now()).toMillis();
                    if (age > fadeStartMs + fadeDurationMs) alpha = 0.0f;
                    else if (age > fadeStartMs) alpha = 1.0f - (float) (age - fadeStartMs) / fadeDurationMs;
                }

                if (!isChatOpen && alpha <= 0.118f) break;

                for (int j = lines.size() - 1; j >= 0; j--) {
                    FormattedCharSequence text = lines.get(j);

                    if (skippedLines < (int) scrollHandler.getScrollOffset()) {
                        skippedLines++;
                        continue;
                    }

                    currentY -= renderer.lineHeight + 3;
                    renderedLinesCount++;

                    if (currentY < y || (renderedLinesCount > maxVisible && !isChatOpen)) break;

                    int currentBg = chatLine.message().getType() == MessageType.MENTIONED ? blinkHex : backgroundColor;
                    int currentStripe = chatLine.message().getType() == MessageType.MENTIONED ? 0xFFc4a020 : stripeColor;

                    int fadedBg = colorRemapper.applyAlpha(currentBg, alpha);
                    int fadedStripe = colorRemapper.applyAlpha(currentStripe, alpha);
                    int fadedText = colorRemapper.applyAlpha(0xFFFFFFFF, alpha);

                    context.fill(x - 5, currentY - 3, x + viewWidth, currentY + renderer.lineHeight, fadedBg);
                    context.fill(x - 5, currentY - 3, x - 7, currentY + renderer.lineHeight, fadedStripe);
                    context.drawString(renderer, text, x, currentY, fadedText, false);

                    if (isChatOpen && mouseX >= x && mouseX <= x + viewWidth && mouseY >= currentY && mouseY <= currentY + renderer.lineHeight) {
                        hoveredStyle = renderer.getSplitter().componentStyleAtWidth(text, mouseX - x);
                    }
                }

                if (currentY < y || (renderedLinesCount > maxVisible && !isChatOpen)) break;
            }
            scrollHandler.render(context, renderer, isChatOpen);
        } finally {
            lock.readLock().unlock();
        }

        dragHandler.render(context, renderer, mouseX, mouseY, isChatOpen);

        if (hoveredStyle != null && hoveredStyle.getHoverEvent() != null) {
            context.renderComponentHoverEffect(renderer, hoveredStyle, mouseX, mouseY);
        }
    }

    public Style getStyleAt(double mouseX, double mouseY, Font renderer) {
        int x = geometry.getX();
        int y = geometry.getY();
        int viewWidth = geometry.getViewWidth();
        int viewHeight = geometry.getViewHeight();
        int currentY = y + viewHeight;

        if (mouseX < x || mouseX > x + viewWidth || mouseY < y || mouseY > y + viewHeight) return null;

        int maxVisible = channel.getMaxVisibleMessage();
        int renderedLinesCount = 0;
        int skippedLines = 0;

        lock.readLock().lock();
        try {
            for (ChatChannelView.ChatLine chatLine : chatLines) {
                List<FormattedCharSequence> lines = chatLine.texts();

                for (int j = lines.size() - 1; j >= 0; j--) {
                    FormattedCharSequence text = lines.get(j);

                    if (skippedLines < (int) scrollHandler.getScrollOffset()) {
                        skippedLines++;
                        continue;
                    }

                    currentY -= renderer.lineHeight + 3;
                    renderedLinesCount++;

                    if (currentY < y || renderedLinesCount > maxVisible) break;

                    if (mouseY >= currentY && mouseY <= currentY + renderer.lineHeight) {
                        return renderer.getSplitter().componentStyleAtWidth(text, (int) (mouseX - x));
                    }
                }
                if (currentY < y || renderedLinesCount > maxVisible) break;
            }
        } finally {
            lock.readLock().unlock();
        }
        return null;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;

        Minecraft client = Minecraft.getInstance();
        if (client.screen == null) return;

        Style style = getStyleAt(mouseX, mouseY, client.font);
        if (style != null && style.getClickEvent() != null) {
            client.screen.handleComponentClicked(style);
        }
    }

    @EventSubscriber(event = PlayerMentionedEvent.class)
    public void mentioned(PlayerMentionedEvent event) {
        Message message = event.getMessage();
        String messageId = message.getId();
        boolean inChannel = channel.getMessages().stream()
                .anyMatch(m -> m.getId().equals(messageId));

        if (inChannel) {
            lastMentionTime = System.currentTimeMillis();
            sounds.playerSound("mention.wav");
        }
    }

    @EventSubscriber(event = GuiOpenEvent.class)
    public void guiOpen() {
        scrollHandler.resetScroll();
    }
}