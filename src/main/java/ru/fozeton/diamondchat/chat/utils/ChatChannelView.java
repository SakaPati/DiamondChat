package ru.fozeton.diamondchat.chat.utils;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;

public class ChatChannelView {
    private final ChatChannel channel;
    private final ChatChannelMessageHandler messageHandler;
    private final ChannelsConfig config;
    private final ChannelsConfig.ChannelSettings settings;
    private final ChatChannelGeometry geometry;
    private final ChatChannelScrollHandler scrollHandler;
    private final MessageColorRemapper colorRemapper = MessageColorRemapper.getInstance();
    private final ChatMetrics metrics = ChatMetrics.getInstance();

    public ChatChannelView(ChatChannel channel) {
        this.channel = Objects.requireNonNull(channel, "Channel cannot be null");

        config = ChatConfigManager.getInstance().getChannelsConfig();
        settings = config.getChannels().get(channel.getId());
        if (settings == null)
            throw new RuntimeException("Config settings for channel '" + channel.getId() + "' not found!");

        this.geometry = new ChatChannelGeometry(channel, settings);
        ChatChannelDragHandler dragHandler = new ChatChannelDragHandler(settings, geometry);
        ChatChannelEditOverlay editOverlay = dragHandler.getEditOverlay();
        this.scrollHandler = new ChatChannelScrollHandler(settings, channel, geometry);
        this.messageHandler = new ChatChannelMessageHandler(
                channel, config, dragHandler, geometry, scrollHandler
        );

        DiamondChat.EVENT_BUS.register(messageHandler);
        DiamondChat.EVENT_BUS.register(scrollHandler);
        DiamondChat.EVENT_BUS.register(editOverlay);
        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void render(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean focused) {
        Font renderer = Minecraft.getInstance().font;
        messageHandler.render(context, mouseX, mouseY, renderer, focused);
    }

    @EventSubscriber(event = InputEvent.MouseInputEvent.class)
    public void onMouseClick(InputEvent.MouseInputEvent event) {
        if (event.getAction() != 1) return;
        if (!ScreenProvider.isChatScreen()) return;

        Minecraft client = Minecraft.getInstance();
        double scale = client.getWindow().getGuiScale();
        double mouseX = event.getMouseX() / scale;
        double mouseY = event.getMouseY() / scale;
        int button = event.getButton();

        switch (button) {
            case 0 -> messageHandler.mouseClicked(mouseX, mouseY, button);
            case 1 -> {
                Message message = findMessageAt(mouseX, mouseY);
                if (message == null) return;

                client.keyboardHandler.setClipboard(message.getContent().getString());
                Notification.showInfo("Сообщение скопировано успешно!", null);
            }
        }
    }

    private Message findMessageAt(double mouseX, double mouseY) {
        Font renderer = Minecraft.getInstance().font;

        int x = geometry.getX();
        int y = geometry.getY();
        int viewWidth = geometry.getViewWidth();
        int viewHeight = geometry.getViewHeight();

        if (mouseX < x - 10 || mouseX > x + viewWidth || mouseY < y || mouseY > y + viewHeight) return null;

        int lineStep = renderer.lineHeight + 3;
        int pixelOffset = (int) scrollHandler.getScrollOffset();

        return channel.withMessages(messages -> {
            int currentY = y + viewHeight;

            for (Message message : messages) {
                if (message.getType() == MessageType.ANIMATED) {
                    Matcher matcher = ChatManagerCore.GIF_PATTERN.matcher(message.getFullPlain());
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
                            if (mouseY >= drawY - 3 && mouseY <= drawY + gifBlockHeight) return message;
                            continue;
                        }
                    }
                }

                int lines = message.getSplitLines(renderer, viewWidth).size();
                for (int j = 0; j < lines; j++) {
                    currentY -= lineStep;
                    int drawY = currentY + pixelOffset;

                    if (drawY < y - renderer.lineHeight) return null;
                    if (mouseY >= drawY - 3 && mouseY <= drawY + renderer.lineHeight) return message;
                }
            }
            return null;
        });
    }

    @EventSubscriber(event = MessageAddedToChannelEvent.class)
    public void addedMessage(MessageAddedToChannelEvent event) {
        if (!event.getChannel().equals(channel)) return;

        Font renderer = Minecraft.getInstance().font;
        Message message = event.getMessage();
        int viewWidth = geometry.getViewWidth();

        if (settings.isUseColorRemapper()) message.setContent(colorRemapper.remap(message.getContent()));

        if (settings.isAutoScroll() || !ScreenProvider.isChatScreen()) {
            scrollHandler.setScrollOffset(0);
            return;
        }

        int newLinesCount = metrics.getLineSlots(message, renderer, viewWidth);
        int totalPixels = channel.withMessages(msgs -> metrics.getTotalPixels(msgs, renderer, viewWidth));
        int maxScroll = Math.max(0, totalPixels - geometry.getViewHeight() + ChatMetrics.GIF_SPACING);

        scrollHandler.setScrollOffset(Math.clamp(
                (int) (scrollHandler.getScrollOffset() + newLinesCount), 0, maxScroll
        ));
    }

    @EventSubscriber(event = ChannelVisibilityChangedEvent.class)
    public void onVisibilityChanged(ChannelVisibilityChangedEvent event) {
        if (!event.getChannelId().equals(channel.getId())) return;
        settings.setVisible(event.isVisible());
        ChatConfigManager.getInstance().save(config.getClass());
    }

    @EventSubscriber(event = MessageStackEvent.class)
    public void onStackMessage(MessageStackEvent event) {
        if (!event.getChannel().equals(channel)) return;

        Message message = event.getMessage();
        MutableComponent updated = message.getMutContent();

        List<Component> siblings = new ArrayList<>(updated.getSiblings());
        if (message.getStack() > 2 && !siblings.isEmpty() && siblings.getLast().getString().startsWith(" (x")) {
            siblings.removeLast();
            updated = MutableComponent.create(updated.getContents()).setStyle(updated.getStyle());
            siblings.forEach(updated::append);
        }

        updated.append(Component.literal(" (x" + message.getStack() + ")")
                               .withColor(settings.getMessageStackColor()));
        message.setContent(updated);
    }
}