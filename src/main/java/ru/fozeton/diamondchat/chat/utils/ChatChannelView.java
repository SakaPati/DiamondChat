package ru.fozeton.diamondchat.chat.utils;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.events.InputEvent;
import ru.fozeton.chatmanager.events.channel.ChannelVisibilityChangedEvent;
import ru.fozeton.chatmanager.events.channel.MessageAddedToChannelEvent;
import ru.fozeton.chatmanager.events.channel.MessageStackEvent;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageType;
import ru.fozeton.chatmanager.utils.MessageColorRemapper;
import ru.fozeton.chatmanager.utils.compat.providers.ScreenProvider;
import ru.fozeton.diamondchat.DiamondChat;
import ru.fozeton.diamondchat.chat.utils.channel.*;
import ru.fozeton.diamondchat.utils.notify.Notification;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.regex.Matcher;

public class ChatChannelView {
    private final ChatChannel channel;
    private final ChatChannelMessageHandler messageHandler;
    private final SequencedMap<String, ChatLine> chatMessages = new LinkedHashMap<>();
    private final ChannelsConfig config;
    private final ChannelsConfig.ChannelSettings settings;
    private final ChatChannelGeometry geometry;
    private final ChatChannelScrollHandler scrollHandler;
    private final MessageColorRemapper colorRemapper = MessageColorRemapper.getInstance();
    private final ChatMetrics metrics = ChatMetrics.getInstance();

    public ChatChannelView(ChatChannel channel)  {
        this.channel = channel;
        Objects.requireNonNull(channel, "Channel cannot be null");

        config = ChatConfigManager.getInstance().getChannelsConfig();
        settings = config.getChannels().get(channel.getId());

        if (settings == null)
            throw new RuntimeException("Config settings for channel '" + channel.getId() + "' not found!");


        ChatChannelGeometry geometry = new ChatChannelGeometry(channel, settings, chatMessages);
        this.geometry = geometry;
        ChatChannelDragHandler dragHandler = new ChatChannelDragHandler(settings, geometry);
        ChatChannelEditOverlay editOverlay = dragHandler.getEditOverlay();
        ChatChannelScrollHandler scrollHandler = new ChatChannelScrollHandler(
                settings,
                chatMessages.values(),
                geometry
        );
        this.scrollHandler = scrollHandler;
        this.messageHandler = new ChatChannelMessageHandler(
                channel,
                config,
                chatMessages.values(),
                dragHandler,
                geometry,
                scrollHandler
        );

        DiamondChat.EVENT_BUS.register(messageHandler);
        DiamondChat.EVENT_BUS.register(scrollHandler);
        DiamondChat.EVENT_BUS.register(editOverlay);
        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean focused) {
        Minecraft client = Minecraft.getInstance();
        Font renderer = client.font;
        messageHandler.render(context, mouseX, mouseY, renderer, focused);
    }

    @EventSubscriber(event = InputEvent.MouseInputEvent.class)
    public void onMouseClick(InputEvent.MouseInputEvent event) {
        if (event.getAction() != 1) return;

        Minecraft client = Minecraft.getInstance();
        if (!(ScreenProvider.isChatScreen())) return;

        double scale = client.getWindow().getGuiScale();
        double mouseX = event.getMouseX() / scale;
        double mouseY = event.getMouseY() / scale;

        int button = event.getButton();

        switch (button) {
            case 0 -> this.messageHandler.mouseClicked(mouseX, mouseY, button);
            case 1 -> {
                String targetId = findMessageIdAt(mouseX, mouseY);
                if (targetId == null) return;

                channel.getMessages().stream()
                        .filter(m -> m.getId().equals(targetId))
                        .findFirst()
                        .ifPresent(message -> {
                            client.keyboardHandler.setClipboard(message.getContent().getString());
                            Notification.showInfo("Сообщение скопировано успешно!", null);
                        });
            }
        }
    }

    private String findMessageIdAt(double mouseX, double mouseY) {
        Font renderer = Minecraft.getInstance().font;

        int x = geometry.getX();
        int y = geometry.getY();
        int viewWidth = geometry.getViewWidth();
        int viewHeight = geometry.getViewHeight();

        if (mouseX < x - 10 || mouseX > x + viewWidth || mouseY < y || mouseY > y + viewHeight) return null;

        int lineStep = renderer.lineHeight + 3;
        int pixelOffset = (int) scrollHandler.getScrollOffset();
        int currentY = y + viewHeight;

        for (ChatLine chatLine : chatMessages.values()) {
            Message message = chatLine.message();

            if (message.getType() == MessageType.ANIMATED) {
                Matcher matcher = ChatManagerCore.GIF_PATTERN.matcher(message.getPlainText());
                if (matcher.find()) {
                    GifTexture gif = GifTexture.get(matcher.group().replace(":", ""));
                    if (!gif.isFailed()) {
                        int textHeight = renderer.split(
                                metrics.stripGifPattern(message.getContent()), viewWidth
                        ).size() * lineStep;
                        int gifBlockHeight = textHeight + gif.getHeight() / 2;
                        int totalBlockHeight = gifBlockHeight + ChatMetrics.GIF_SPACING;

                        currentY -= totalBlockHeight;
                        int drawY = currentY + pixelOffset;

                        if (drawY + totalBlockHeight < y) return null;

                        if (mouseY >= drawY - 3 && mouseY <= drawY + gifBlockHeight) {
                            return message.getId();
                        }
                        continue;
                    }
                }
            }

            for (int j = 0; j < chatLine.texts().size(); j++) {
                currentY -= lineStep;
                int drawY = currentY + pixelOffset;

                if (drawY < y - renderer.lineHeight) return null;

                if (mouseY >= drawY - 3 && mouseY <= drawY + renderer.lineHeight) {
                    return message.getId();
                }
            }
        }

        return null;
    }

    @EventSubscriber(event = MessageAddedToChannelEvent.class)
    public void addedMessage(MessageAddedToChannelEvent event) {
        if (!event.getChannel().equals(this.channel)) return;

        Minecraft client = Minecraft.getInstance();
        Font renderer = client.font;
        Message message = event.getMessage();
        int viewWidth = geometry.getViewWidth();

        Component text = settings.isUseColorRemapper() ? colorRemapper.remap(message.getContent()) : message.getContent();
        List<FormattedCharSequence> splitLines = renderer.split(text, viewWidth).reversed();

        ChatLine newLine = new ChatLine(splitLines, message, message.getTimestamp());
        chatMessages.putFirst(message.getId(), newLine);

        while (chatMessages.size() > channel.getMaxHistoryMessage()) chatMessages.pollLastEntry();

        int newLinesCount = metrics.getLineSlots(newLine, renderer, viewWidth);

        if (settings.isAutoScroll() || !(ScreenProvider.isChatScreen())) scrollHandler.setScrollOffset(0);
        else {
            int totalLines = metrics.getTotalPixels(chatMessages.values(), renderer, viewWidth);
            int maxScroll = Math.max(0, totalLines - geometry.getViewHeight() + ChatMetrics.GIF_SPACING);
            scrollHandler.setScrollOffset(Math.clamp(
                    (int) (scrollHandler.getScrollOffset() + newLinesCount),
                    0,
                    maxScroll
            ));
        }
    }

    @EventSubscriber(event = ChannelVisibilityChangedEvent.class)
    public void onVisibilityChanged(ChannelVisibilityChangedEvent event) {
        if (!event.getChannelId().equals(channel.getId())) return;
        settings.setVisible(event.isVisible());
        ChatConfigManager.getInstance().save(config.getClass());
    }

    @EventSubscriber(event = MessageStackEvent.class)
    public void onStackMessage(MessageStackEvent event) {
        if (!event.getChannel().equals(this.channel)) return;

        Message message = event.getMessage();

        Minecraft client = Minecraft.getInstance();
        Font renderer = client.font;
        int viewWidth = geometry.getViewWidth();

        Component updatedContent = message.getMutContent()
                .append(Component.literal(" (x" + message.getStack() + ")")
                                .withColor(Long.decode(settings.getMessageStackColor()).intValue()));
        message.setContent(updatedContent);

        Component text = settings.isUseColorRemapper() ? colorRemapper.remap(updatedContent) : updatedContent;
        chatMessages.put(
                message.getId(), new ChatLine(
                        renderer.split(text, viewWidth).reversed(),
                        message,
                        message.getTimestamp()
                )
        );
    }

    public record ChatLine(List<FormattedCharSequence> texts, Message message, Instant timestamp) {
    }
}