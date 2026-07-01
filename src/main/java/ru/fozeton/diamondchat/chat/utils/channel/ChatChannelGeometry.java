package ru.fozeton.diamondchat.chat.utils.channel;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.diamondchat.chat.utils.ChatChannelView;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;

@Getter
@Setter
public class ChatChannelGeometry {
    public static final int MIN_VIEW_W = 80;
    public static final int MIN_VIEW_H = 40;

    private final ChatChannel channel;
    private final ChannelsConfig.ChannelSettings settings;
    private final ReadWriteLock lock;
    private final Map<String, ChatChannelView.ChatLine> chatMessages;
    private final MessageColorRemapper colorRemapper = MessageColorRemapper.getInstance();

    private int x, y, width, height;

    public ChatChannelGeometry(
            ChatChannel channel,
            ChannelsConfig.ChannelSettings settings,
            ReadWriteLock lock,
            Map<String, ChatChannelView.ChatLine> chatMessages
    ) {
        this.channel = channel;
        this.settings = settings;
        this.lock = lock;
        this.chatMessages = chatMessages;
        this.x = settings.getX();
        this.y = settings.getY();
        this.width = settings.getWidth();
        this.height = settings.getHeight();
    }

    public int getViewWidth() {
        return width / 2;
    }

    public int getViewHeight() {
        return height / 2;
    }

    public void rebuildLines(Font renderer, int newViewWidth) {
        lock.writeLock().lock();
        try {
            Map<String, ChatChannelView.ChatLine> rebuilt = new LinkedHashMap<>();
            for (ChatChannelView.ChatLine cl : chatMessages.values()) {
                Message msg = channel.getMessages().stream()
                        .filter(m -> m.getId().equals(cl.message().getId()))
                        .findFirst().orElse(null);
                if (msg == null) {
                    rebuilt.put(cl.message().getId(), cl);
                    continue;
                }
                Component text = settings.isUseColorRemapper() ? colorRemapper.remap(msg.getContent()) : msg.getContent();
                rebuilt.put(cl.message().getId(),
                        new ChatChannelView.ChatLine(new ArrayList<>(renderer.split(text, newViewWidth)), cl.message(), cl.timestamp()));
            }
            chatMessages.clear();
            chatMessages.putAll(rebuilt);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void persistDimensions() {
        ChannelsConfig config = ChatConfigManager.getInstance().load(ChannelsConfig.class);
        config.getChannels().get(channel.getId()).setWidth(width);
        config.getChannels().get(channel.getId()).setHeight(height);
        config.getChannels().get(channel.getId()).setX(x);
        config.getChannels().get(channel.getId()).setY(y);

        ChatConfigManager.getInstance().save(config);
    }
}