package ru.fozeton.diamondchat.chat.utils;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
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
import ru.fozeton.chatmanager.utils.MessageColorRemapper;
import ru.fozeton.diamondchat.DiamondChat;
import ru.fozeton.diamondchat.chat.utils.channel.*;
import ru.fozeton.diamondchat.utils.notify.Notification;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ChatChannelView {
    private final ChatChannel channel;
    private final ChatChannelMessageHandler messageHandler;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Map<String, ChatLine> chatMessages = new LinkedHashMap<>();
    private final ChannelsConfig config;
    private final ChannelsConfig.ChannelSettings settings;
    private final ChatChannelGeometry geometry;
    private final ChatChannelScrollHandler scrollHandler;
    private final MessageColorRemapper colorRemapper = MessageColorRemapper.getInstance();

    public ChatChannelView(ChatChannel channel) {
        this.channel = channel;
        Objects.requireNonNull(channel, "Channel cannot be null");

        config = ChatConfigManager.getInstance().getChannelsConfig();
        settings = config.getChannels().get(channel.getId());

        if (settings == null)
            throw new RuntimeException("Config settings for channel '" + channel.getId() + "' not found!");


        ChatChannelGeometry geometry = new ChatChannelGeometry(channel, settings, lock, chatMessages);
        this.geometry = geometry;
        ChatChannelDragHandler dragHandler = new ChatChannelDragHandler(settings, geometry);
        ChatChannelEditOverlay editOverlay = dragHandler.getEditOverlay();
        ChatChannelScrollHandler scrollHandler = new ChatChannelScrollHandler(settings, chatMessages.values(), dragHandler, lock, geometry, editOverlay);
        this.scrollHandler = scrollHandler;
        this.messageHandler = new ChatChannelMessageHandler(channel, config, chatMessages.values(), dragHandler, geometry, lock, scrollHandler);

        DiamondChat.EVENT_BUS.register(messageHandler);
        DiamondChat.EVENT_BUS.register(scrollHandler);
        DiamondChat.EVENT_BUS.register(editOverlay);
        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void render(GuiGraphics context, int mouseX, int mouseY, boolean focused) {
        Minecraft client = Minecraft.getInstance();
        Font renderer = client.font;
        messageHandler.render(context, mouseX, mouseY, renderer, focused);
    }

    @EventSubscriber(event = InputEvent.MouseInputEvent.class)
    public void onMouseClick(InputEvent.MouseInputEvent event) {
        if (event.getAction() != 1) return;

        Minecraft client = Minecraft.getInstance();
        if (!(client.screen instanceof ChatScreen)) return;

        double scale = client.getWindow().getGuiScale();
        double mouseX = event.getMouseX() / scale;
        double mouseY = event.getMouseY() / scale;

        int button = event.getButton();

        switch (button) {
            case 0 -> this.messageHandler.mouseClicked(mouseX, mouseY, button);
            case 1 -> {
                int x = geometry.getX();
                int y = geometry.getY();
                int viewWidth = geometry.getViewWidth();
                int viewHeight = geometry.getViewHeight();

                if (mouseX < x - 10 || mouseX > x + viewWidth || mouseY < y || mouseY > y + viewHeight) {
                    return;
                }

                Font renderer = client.font;
                int currentY = y + viewHeight;

                int maxVisible = channel.getMaxVisibleMessage();
                int renderedLinesCount = 0;
                int skippedLines = 0;
                String clickedMessageId = null;

                lock.readLock().lock();
                try {
                    for (ChatLine chatLine : chatMessages.values()) {
                        List<FormattedCharSequence> lines = chatLine.texts();

                        for (int j = lines.size() - 1; j >= 0; j--) {
                            if (skippedLines < (int) scrollHandler.getScrollOffset()) {
                                skippedLines++;
                                continue;
                            }

                            currentY -= renderer.lineHeight + 3;
                            renderedLinesCount++;

                            if (currentY < y || (renderedLinesCount > maxVisible)) break;

                            int minX = x - 10;
                            int maxX = x + viewWidth;
                            int minY = currentY - 3;
                            int maxY = currentY + renderer.lineHeight;

                            if (mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY) {
                                clickedMessageId = chatLine.message().getId();
                                break;
                            }
                        }
                        if (currentY < y || renderedLinesCount > maxVisible || clickedMessageId != null) break;
                    }

                    if (clickedMessageId != null) {
                        final String targetId = clickedMessageId;
                        channel.getMessages().stream()
                                .filter(m -> m.getId().equals(targetId))
                                .findFirst()
                                .ifPresent(message -> {
                                    String plainText = message.getContent().getString();
                                    client.keyboardHandler.setClipboard(plainText);
                                    Notification.showInfo("Сообщение скопировано успешно!", null);
                                });
                    }
                } finally {
                    lock.readLock().unlock();
                }
            }
        }
    }

    @EventSubscriber(event = MessageAddedToChannelEvent.class)
    public void addedMessage(MessageAddedToChannelEvent event) {
        if (!event.getChannel().equals(this.channel)) return;

        Minecraft client = Minecraft.getInstance();
        Font renderer = client.font;
        Message message = event.getMessage();
        int viewHeight = geometry.getViewHeight();
        int viewWidth = geometry.getViewWidth();

        lock.writeLock().lock();
        try {
            Component newText = settings.isUseColorRemapper() ? colorRemapper.remap(message.getContent()) : message.getContent();
            int newLinesCount = renderer.split(newText, viewWidth).size();

            chatMessages.clear();
            for (Message msg : channel.getMessages()) {
                Component text = settings.isUseColorRemapper() ? colorRemapper.remap(msg.getContent()) : msg.getContent();
                msg.getMetadata().put("channelId", event.getChannel().getId());
                chatMessages.put(msg.getId(), new ChatChannelView.ChatLine(renderer.split(text, viewWidth), msg, msg.getTimestamp()));
            }

            if (settings.isAutoScroll() || !(client.screen instanceof ChatScreen)) {
                scrollHandler.setScrollOffset(0);
            } else {
                int linesInView = viewHeight / (renderer.lineHeight + 3);
                int totalLines = chatMessages.values().stream().mapToInt(l -> l.texts().size()).sum();
                int maxScroll = Math.max(0, totalLines - linesInView);
                scrollHandler.setScrollOffset(Math.clamp((int) (scrollHandler.getScrollOffset() + newLinesCount), 0, maxScroll));
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    @EventSubscriber(event = ChannelVisibilityChangedEvent.class)
    public void onVisibilityChanged(ChannelVisibilityChangedEvent event) {
        if (!event.getChannelId().equals(channel.getId())) return;
        settings.setVisible(event.isVisible());
        ChatConfigManager.getInstance().save(config);
    }

    @EventSubscriber(event = MessageStackEvent.class)
    public void onStackMessage(MessageStackEvent event) {
        if (!event.getChannel().equals(this.channel)) return;

        Message message = event.getMessage();

        Minecraft client = Minecraft.getInstance();
        Font renderer = client.font;
        int viewWidth = geometry.getViewWidth();

        lock.writeLock().lock();
        try {
            Component updatedContent = message.getMutContent()
                    .append(Component.literal(" (x" + message.getStack() + ")")
                            .withColor(Long.decode(settings.getMessageStackColor()).intValue()));
            message.setContent(updatedContent);

            Component text = settings.isUseColorRemapper() ? colorRemapper.remap(updatedContent) : updatedContent;
            chatMessages.put(message.getId(), new ChatLine(
                    new ArrayList<>(renderer.split(text, viewWidth)),
                    message,
                    message.getTimestamp()
            ));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public record ChatLine(List<FormattedCharSequence> texts, Message message, Instant timestamp) {
    }
}