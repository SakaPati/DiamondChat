package ru.fozeton.diamondchat.chat.utils.channel;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiTextRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStackTemplate;
import org.joml.Matrix3x2f;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.events.PlayerMentionedEvent;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageType;
import ru.fozeton.chatmanager.messages.metadata.MessageMetadata;
import ru.fozeton.chatmanager.utils.MessageColorRemapper;
import ru.fozeton.chatmanager.utils.PlaySounds;
import ru.fozeton.chatmanager.utils.compat.providers.ScreenProvider;
import ru.fozeton.diamondchat.chat.utils.ChatMetrics;
import ru.fozeton.diamondchat.chat.utils.GifTexture;
import ru.fozeton.diamondchat.events.gui.GuiOpenEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;

public class ChatChannelMessageHandler {
    private final ChatMetrics metrics = ChatMetrics.getInstance();
    private final MessageColorRemapper colorRemapper = MessageColorRemapper.getInstance();
    private final PlaySounds sounds = PlaySounds.getInstance();
    private final ChatChannel channel;
    private final ChannelsConfig.ChannelSettings settings;
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
    private Style hoveredStyle;

    public ChatChannelMessageHandler(
            ChatChannel channel,
            ChannelsConfig config,
            ChatChannelDragHandler dragHandler,
            ChatChannelGeometry geometry,
            ChatChannelScrollHandler scrollHandler
    ) {
        this.channel = Objects.requireNonNull(channel, "Channel cannot be null");
        this.settings = Objects.requireNonNull(config.getChannels().get(channel.getId()), "Settings cannot be null");
        this.dragHandler = dragHandler;
        this.geometry = Objects.requireNonNull(geometry, "Geometry cannot be null");
        this.scrollHandler = scrollHandler;

        this.bgHex = (int) ((Number) settings.getBackgroundColor()).longValue();
        this.blinkHex = (int) ((Number) settings.getBlinkColor()).longValue();

        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, Font renderer, boolean isChatOpen) {
        if (!settings.isVisible()) return;

        this.x = geometry.getX();
        this.y = geometry.getY();
        this.viewWidth = geometry.getViewWidth();
        this.viewHeight = geometry.getViewHeight();
        this.currentY = y + viewHeight;

        long timeSince = System.currentTimeMillis() - lastMentionTime;
        this.baseBg = (timeSince < settings.getBlinkDuration())
                ? (Math.sin((double) timeSince / settings.getBlinkDuration()
                            * Math.PI * settings.getBlinkSpeed()) > 0 ? blinkHex : bgHex)
                : bgHex;
        this.fadeStartMs = settings.getFadingStartTime();
        this.fadeDurationMs = settings.getFadingDuration();
        this.maxVisible = channel.getMaxVisibleMessage();
        this.renderedLinesCount = 0;
        this.hoveredStyle = null;

        channel.forEachMessage(messages -> {
            for (Message message : messages) {
                MessageType type = message.getType();
                if (type == MessageType.ANIMATED) {
                    if (gifRender(context, renderer, mouseX, mouseY, message, isChatOpen)) break;
                    else continue;
                }
                if (textRender(context, renderer, mouseX, mouseY, type, message, isChatOpen)) break;
            }
        });

        if (scrollHandler != null) scrollHandler.render(context, isChatOpen);
        if (dragHandler != null) dragHandler.render(context, renderer, mouseX, mouseY, isChatOpen);

        if (hoveredStyle != null && hoveredStyle.getHoverEvent() != null) {
            HoverEvent hover = hoveredStyle.getHoverEvent();
            if (hover instanceof HoverEvent.ShowText(Component value)) {
                context.setTooltipForNextFrame(
                        renderer,
                        renderer.split(value, Math.max(context.guiWidth() / 2, 200)),
                        mouseX, mouseY
                );
            } else if (hover instanceof HoverEvent.ShowItem(ItemStackTemplate item)) {
                context.setTooltipForNextFrame(renderer, item.create(), mouseX, mouseY);
            }
        }
    }

    private boolean gifRender(
            GuiGraphicsExtractor context,
            Font renderer,
            int mouseX,
            int mouseY,
            Message message,
            boolean isChatOpen
    ) {
        Matcher matcher = ChatManagerCore.GIF_PATTERN.matcher(message.getFullPlain());
        if (!matcher.find()) return false;

        String gifId = matcher.group().replace(":", "");
        GifTexture gif = GifTexture.get(gifId);
        if (gif.isFailed()) {
            textRender(context, renderer, mouseX, mouseY, MessageType.SYSTEM, message, isChatOpen);
            return false;
        }

        int gifHeight = gif.getHeight() / 2;

        Component cleanedContent = metrics.stripGifPattern(message.getContent());
        List<FormattedCharSequence> cleanedLines = renderer.split(cleanedContent, viewWidth);

        int lineStep = renderer.lineHeight + 3;
        int textBlockHeight = cleanedLines.size() * lineStep;

        int gifBlockHeight = textBlockHeight + gifHeight;
        int totalBlockHeight = gifBlockHeight + ChatMetrics.GIF_SPACING;

        int slots = (int) Math.ceil((double) totalBlockHeight / lineStep);

        currentY -= totalBlockHeight;
        renderedLinesCount += slots;

        int pixelOffset = (scrollHandler != null) ? (int) scrollHandler.getScrollOffset() : 0;
        int drawY = currentY + pixelOffset;

        if (drawY + totalBlockHeight < y || (renderedLinesCount > maxVisible && !isChatOpen)) return true;

        float alpha = getAlpha(isChatOpen, message);
        if (alpha == -1) return true;

        int currentStripe = 0xFF7c6ef5;
        int fadedBg = colorRemapper.applyAlpha(baseBg, alpha);
        int fadedStripe = colorRemapper.applyAlpha(currentStripe, alpha);
        int fadedText = colorRemapper.applyAlpha(0xFFFFFFFF, alpha);

        context.enableScissor(x - 7, y, x + viewWidth, y + viewHeight);

        context.fill(x - 5, drawY - 3, x + viewWidth, drawY + gifBlockHeight, fadedBg);
        context.fill(x - 5, drawY - 3, x - 7, drawY + gifBlockHeight, fadedStripe);

        int textY = drawY;
        for (FormattedCharSequence line : cleanedLines) {
            drawTextLine(context, renderer, line, mouseX, mouseY, textY, fadedText);
            textY += lineStep;
        }

        gif.render(context, x, drawY + gifBlockHeight, alpha);

        context.disableScissor();

        return drawY + totalBlockHeight < y || (renderedLinesCount > maxVisible && !isChatOpen);
    }

    private void drawTextLine(
            GuiGraphicsExtractor context,
            Font renderer,
            FormattedCharSequence text,
            int mouseX,
            int mouseY,
            int currentY,
            int textColor
    ) {
        context.text(renderer, text, x, currentY, textColor, false);

        if (mouseX >= x && mouseX <= x + viewWidth && mouseY >= currentY && mouseY <= currentY + renderer.lineHeight) {
            GuiTextRenderState state = new GuiTextRenderState(
                    renderer, text, new Matrix3x2f(context.pose()),
                    x, currentY, textColor, 0, false, true,
                    context.scissorStack.peek()
            );
            ActiveTextCollector.findElementUnderCursor(
                    state, mouseX, mouseY,
                    style -> hoveredStyle = style
            );
        }
    }

    private boolean textRender(
            GuiGraphicsExtractor context,
            Font renderer,
            int mouseX,
            int mouseY,
            MessageType type,
            Message message,
            boolean isChatOpen
    ) {
        List<FormattedCharSequence> lines = message.getSplitLines(renderer, viewWidth);

        int stripeColor = switch (type) {
            case PLAYER, CLIENT, CUSTOM -> 0xFFffffff;
            default -> 0xFF7c6ef5;
        };
        int backgroundColor = baseBg;

        MessageMetadata meta = message.getMetadata().get(MessageMetadata.class).orElse(null);
        if (meta != null) {
            if (meta.getLineColor() != 0) backgroundColor = meta.getLineColor();
            if (meta.getBorderColor() != 0) stripeColor = meta.getBorderColor();
        }

        float alpha = getAlpha(isChatOpen, message);
        if (alpha == -1) return true;

        int pixelOffset = (scrollHandler != null) ? (int) scrollHandler.getScrollOffset() : 0;

        context.enableScissor(x - 7, y, x + viewWidth, y + viewHeight);

        for (FormattedCharSequence text : lines) {
            currentY -= (renderer.lineHeight + 3);
            renderedLinesCount++;

            int drawY = currentY + pixelOffset;

            if (drawY < y - renderer.lineHeight || (renderedLinesCount > maxVisible && !isChatOpen)) break;

            int currentBg = type == MessageType.MENTIONED ? blinkHex : backgroundColor;
            int currentStripe = type == MessageType.MENTIONED ? 0xFFc4a020 : stripeColor;

            int fadedBg = colorRemapper.applyAlpha(currentBg, alpha);
            int fadedStripe = colorRemapper.applyAlpha(currentStripe, alpha);
            int fadedText = colorRemapper.applyAlpha(0xFFFFFFFF, alpha);

            context.fill(x - 5, drawY - 3, x + viewWidth, drawY + renderer.lineHeight, fadedBg);
            context.fill(x - 5, drawY - 3, x - 7, drawY + renderer.lineHeight, fadedStripe);

            drawTextLine(context, renderer, text, mouseX, mouseY, drawY, fadedText);
        }

        context.disableScissor();

        return currentY + pixelOffset < y || (renderedLinesCount > maxVisible && !isChatOpen);
    }

    private float getAlpha(boolean isChatOpen, Message message) {
        if (isChatOpen) return 1.0f;

        float alpha = 1.0f;
        long age = Duration.between(message.getTimestamp(), Instant.now()).toMillis();
        if (age > fadeStartMs + fadeDurationMs) alpha = 0.0f;
        else if (age > fadeStartMs) alpha = 1.0f - (float) (age - fadeStartMs) / fadeDurationMs;

        return alpha <= 0.118f ? -1 : alpha;
    }

    public Style getStyleAt(double mouseX, double mouseY, Font renderer) {
        int x = geometry.getX();
        int y = geometry.getY();
        int viewWidth = geometry.getViewWidth();
        int viewHeight = geometry.getViewHeight();

        if (mouseX < x || mouseX > x + viewWidth || mouseY < y || mouseY > y + viewHeight) return null;

        int lineStep = renderer.lineHeight + 3;
        int pixelOffset = (int) scrollHandler.getScrollOffset();

        return channel.withMessages(messages -> {
            int currentY = y + viewHeight;

            for (Message message : messages) {
                for (FormattedCharSequence text : message.getSplitLines(renderer, viewWidth)) {
                    currentY -= lineStep;
                    int drawY = currentY + pixelOffset;

                    if (drawY < y - renderer.lineHeight) return null;

                    if (mouseY >= drawY && mouseY <= drawY + renderer.lineHeight) {
                        ActiveTextCollector.ClickableStyleFinder finder =
                                new ActiveTextCollector.ClickableStyleFinder(renderer, (int) mouseX, (int) mouseY);
                        finder.accept(x, drawY, text);
                        return finder.result();
                    }
                }
            }
            return null;
        });
    }

    public void mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return;

        Minecraft client = Minecraft.getInstance();
        if (ScreenProvider.getScreen() == null) return;

        Style style = getStyleAt(mouseX, mouseY, client.font);
        if (style != null && style.getClickEvent() != null) ScreenProvider.handleComponentClicked(style);
    }

    @EventSubscriber(event = PlayerMentionedEvent.class)
    public void mentioned(PlayerMentionedEvent event) {
        UUID messageId = event.getMessage().getId();
        boolean inChannel = channel.withMessages(
                messages -> messages.stream().anyMatch(m -> m.getId().equals(messageId))
        );

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