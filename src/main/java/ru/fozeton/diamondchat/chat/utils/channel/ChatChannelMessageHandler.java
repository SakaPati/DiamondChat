package ru.fozeton.diamondchat.chat.utils.channel;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
import ru.fozeton.diamondchat.chat.utils.GifTexture;
import ru.fozeton.diamondchat.events.gui.GuiOpenEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatChannelMessageHandler {
    private static final Pattern GIF_PATTERN = Pattern.compile(":\\d{15,19}:");
    private final MessageColorRemapper colorRemapper = MessageColorRemapper.getInstance();
    private final PlaySounds sounds = PlaySounds.getInstance();
    private final ChatChannel channel;
    private final ChannelsConfig.ChannelSettings settings;
    private final Collection<ChatChannelView.ChatLine> chatLines;
    private final ChatChannelDragHandler dragHandler;
    private final ChatChannelGeometry geometry;
    private final ChatChannelScrollHandler scrollHandler;
    private final int bgHex;
    private final int blinkHex;
    private long lastMentionTime = 0;

    private int x;
    private int y;
    private int viewWidth;
    private int viewHeight;
    private int currentY;
    private int baseBg;
    private int fadeStartMs;
    private int fadeDurationMs;
    private int maxVisible;
    private int renderedLinesCount;
    private int skippedLines;
    private Style hoveredStyle;

    public ChatChannelMessageHandler(
            ChatChannel channel,
            ChannelsConfig config,
            Collection<ChatChannelView.ChatLine> chatLines,
            ChatChannelDragHandler dragHandler,
            ChatChannelGeometry geometry,
            ChatChannelScrollHandler scrollHandler
    ) {
        this.channel = Objects.requireNonNull(channel, "Channel cannot be null");
        this.settings = Objects.requireNonNull(config.getChannels().get(channel.getId()), "Settings cannot be null");
        this.chatLines = chatLines;
        this.dragHandler = dragHandler;
        this.geometry = Objects.requireNonNull(geometry, "Geometry cannot be null");
        this.scrollHandler = scrollHandler;

        this.bgHex = (int) Long.decode(settings.getBackgroundColor()).longValue();
        this.blinkHex = (int) Long.decode(settings.getBlinkColor()).longValue();

        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void render(GuiGraphics context, int mouseX, int mouseY, Font renderer, boolean isChatOpen) {
        if (settings == null || geometry == null || !settings.isVisible()) return;
        this.x = geometry.getX();
        this.y = geometry.getY();
        this.viewWidth = geometry.getViewWidth();
        this.viewHeight = geometry.getViewHeight();
        this.currentY = y + viewHeight;
        long now = System.currentTimeMillis();
        long timeSince = now - lastMentionTime;
        this.baseBg = (timeSince < settings.getBlinkDuration())
                ? (Math.sin((double) timeSince / settings.getBlinkDuration()
                            * Math.PI * settings.getBlinkSpeed()) > 0 ? blinkHex : bgHex)
                : bgHex;
        this.fadeStartMs = settings.getFadingStartTime();
        this.fadeDurationMs = settings.getFadingDuration();
        this.maxVisible = channel.getMaxVisibleMessage();
        this.renderedLinesCount = 0;
        this.skippedLines = 0;
        this.hoveredStyle = null;

        for (ChatChannelView.ChatLine chatLine : chatLines) {
            MessageType type = chatLine.message().getType();
            if (type == MessageType.ANIMATED) {
                if (gifRender(context, renderer, chatLine, isChatOpen)) break;
                else continue;
            }
            if (textRender(context, renderer, mouseX, mouseY, type, chatLine, isChatOpen)) break;
        }

        if (scrollHandler != null) scrollHandler.render(context, renderer, isChatOpen);
        if (dragHandler != null) dragHandler.render(context, renderer, mouseX, mouseY, isChatOpen);

        if (hoveredStyle != null && hoveredStyle.getHoverEvent() != null) {
            context.renderComponentHoverEffect(renderer, hoveredStyle, mouseX, mouseY);
        }
    }

    private boolean gifRender(
            GuiGraphics context,
            Font renderer,
            ChatChannelView.ChatLine chatLine,
            boolean isChatOpen
    ) {
        Matcher matcher = GIF_PATTERN.matcher(chatLine.message().getPlainText());
        if (!matcher.find()) return false;

        String gifId = matcher.group().replaceAll(":", "");
        GifTexture gif = GifTexture.get(gifId);
        int gifHeight = gif.getHeight() / 2;

        int lineStep = renderer.lineHeight + 3;
        int linesNeeded = Math.max(1, (int) Math.ceil((double) gifHeight / lineStep));
        int totalBlockHeight = linesNeeded * lineStep;

        int slots = (int) Math.ceil((double) (totalBlockHeight + 11) / lineStep);

        if (scrollHandler != null && skippedLines < (int) scrollHandler.getScrollOffset()) {
            skippedLines += slots;
            return false;
        }

        currentY -= totalBlockHeight + 11;
        renderedLinesCount += slots;

        if (currentY < y || (renderedLinesCount > maxVisible && !isChatOpen)) return true;

        float alpha = getAlpha(isChatOpen, chatLine, fadeStartMs, fadeDurationMs);
        if (alpha == -1) return true;

        int currentStripe = 0xFF7c6ef5;
        int fadedBg = colorRemapper.applyAlpha(baseBg, alpha);
        int fadedStripe = colorRemapper.applyAlpha(currentStripe, alpha);

        context.fill(x - 5, currentY - 3, x + viewWidth, currentY + totalBlockHeight + 8, fadedBg);
        context.fill(x - 5, currentY - 3, x - 7, currentY + totalBlockHeight + 8, fadedStripe);

        Component cleanedContent = stripGifPattern(chatLine.message().getContent());
        context.drawString(renderer, cleanedContent.getVisualOrderText(), x, currentY, -1, false);

        gif.render(context, x, currentY + totalBlockHeight + 8);

        return false;
    }

    private Component stripGifPattern(Component component) {
        MutableComponent result = Component.empty();
        component.visit(
                (style, text) -> {
                    String filtered = GIF_PATTERN.matcher(text).replaceAll("");
                    if (!filtered.isEmpty()) result.append(Component.literal(filtered).setStyle(style));
                    return Optional.empty();
                }, Style.EMPTY
        );
        return result;
    }

    private boolean textRender(
            GuiGraphics context,
            Font renderer,
            int mouseX,
            int mouseY,
            MessageType type,
            ChatChannelView.ChatLine chatLine,
            boolean isChatOpen
    ) {
        List<FormattedCharSequence> lines = chatLine.texts();

        int stripeColor = switch (type) {
            case PLAYER, CLIENT, CUSTOM -> 0xFFffffff;
            default -> 0xFF7c6ef5;
        };

        int backgroundColor = baseBg;

        Map<String, Object> metadata = chatLine.message().getMetadata();
        String lineColor = (String) metadata.get("lineColor");
        String borderColor = (String) metadata.get("borderColor");

        if (lineColor != null) backgroundColor = Long.decode(lineColor).intValue();
        if (borderColor != null) stripeColor = Long.decode(borderColor).intValue();

        float alpha = getAlpha(isChatOpen, chatLine, fadeStartMs, fadeDurationMs);
        if (alpha == -1) return true;

        for (FormattedCharSequence text : lines) {
            if (scrollHandler != null && skippedLines < (int) scrollHandler.getScrollOffset()) {
                skippedLines++;
                continue;
            }

            currentY -= renderer.lineHeight + 3;
            renderedLinesCount++;

            if (currentY < y || (renderedLinesCount > maxVisible && !isChatOpen)) break;

            int currentBg = type == MessageType.MENTIONED ? blinkHex : backgroundColor;
            int currentStripe = type == MessageType.MENTIONED ? 0xFFc4a020 : stripeColor;

            int fadedBg = colorRemapper.applyAlpha(currentBg, alpha);
            int fadedStripe = colorRemapper.applyAlpha(currentStripe, alpha);
            int fadedText = colorRemapper.applyAlpha(0xFFFFFFFF, alpha);

            context.fill(x - 5, currentY - 3, x + viewWidth, currentY + renderer.lineHeight, fadedBg);
            context.fill(x - 5, currentY - 3, x - 7, currentY + renderer.lineHeight, fadedStripe);
            context.drawString(renderer, text, x, currentY, fadedText, false);

            if (isChatOpen && mouseX >= x && mouseX <= x + viewWidth && mouseY >= currentY &&
                mouseY <= currentY + renderer.lineHeight) {
                hoveredStyle = renderer.getSplitter().componentStyleAtWidth(text, mouseX - x);
            }
        }

        return currentY < y || (renderedLinesCount > maxVisible && !isChatOpen);
    }

    private float getAlpha(
            boolean isChatOpen,
            ChatChannelView.ChatLine chatLine,
            int fadeStartMs,
            int fadeDurationMs
    ) {
        float alpha = 1.0f;
        if (!isChatOpen) {
            long age = Duration.between(chatLine.timestamp(), Instant.now()).toMillis();
            if (age > fadeStartMs + fadeDurationMs) alpha = 0.0f;
            else if (age > fadeStartMs) alpha = 1.0f - (float) (age - fadeStartMs) / fadeDurationMs;
        }
        if (!isChatOpen && alpha <= 0.118f) return -1;
        return alpha;
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

        for (ChatChannelView.ChatLine chatLine : chatLines) {
            List<FormattedCharSequence> lines = chatLine.texts();

            for (FormattedCharSequence text : lines) {
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

        return null;
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;

        Minecraft client = Minecraft.getInstance();
        if (client.screen == null) return;

        Style style = getStyleAt(mouseX, mouseY, client.font);
        if (style != null && style.getClickEvent() != null) client.screen.handleComponentClicked(style);
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