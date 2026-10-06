package ru.fozeton.diamondchat.chat.utils.channel;

import lombok.Getter;
import lombok.Setter;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;

@Getter
@Setter
public class ChatChannelGeometry {
    public static final int MIN_VIEW_W = 80;
    public static final int MIN_VIEW_H = 40;
    private final ChatChannel channel;
    private final ChannelsConfig.ChannelSettings settings;

    private int x, y, width, height;

    public ChatChannelGeometry(ChatChannel channel, ChannelsConfig.ChannelSettings settings) {
        this.channel = channel;
        this.settings = settings;
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

    public void persistDimensions() {
        ChannelsConfig config = ChatConfigManager.getInstance().getChannelsConfig();
        ChannelsConfig.ChannelSettings channelSettings = config.getChannels().get(channel.getId());
        channelSettings.setMaxVisibleLine(getViewHeight() / 12);
        channelSettings.setWidth(width);
        channelSettings.setHeight(height);
        channelSettings.setX(x);
        channelSettings.setY(y);

        ChatConfigManager.getInstance().save(config.getClass());
    }
}