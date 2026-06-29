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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

@Getter
@Setter
public class ChatChannelGeometry {
    public static final int MIN_VIEW_W = 80;
    public static final int MIN_VIEW_H = 40;

    private final ChatChannel channel;
    private final ChannelsConfig.ChannelSettings settings;
    private final ReadWriteLock lock;
    private final List<ChatChannelView.ChatLine> chatLines;
    private final MessageColorRemapper colorRemapper = MessageColorRemapper.getInstance();

    private int x, y, width, height;

    public ChatChannelGeometry(
            ChatChannel channel,
            ChannelsConfig.ChannelSettings settings,
            ReadWriteLock lock,
            List<ChatChannelView.ChatLine> chatLines
    ) {
        this.channel = channel;
        this.settings = settings;
        this.lock = lock;
        this.chatLines = chatLines;
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
            List<ChatChannelView.ChatLine> rebuilt = new ArrayList<>(chatLines.size());
            for (ChatChannelView.ChatLine cl : chatLines) {
                Message src = channel.getMessages().stream()
                        .filter(m -> m.getId().equals(cl.message().getId()))
                        .findFirst().orElse(null);
                if (src == null) {
                    rebuilt.add(cl);
                    continue;
                }
                Component text = colorRemapper.remap(src.getContent());
                rebuilt.add(new ChatChannelView.ChatLine(renderer.split(text, newViewWidth), cl.message(), cl.timestamp()));
            }
            chatLines.clear();
            chatLines.addAll(rebuilt);
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