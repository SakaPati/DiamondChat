package ru.fozeton.diamondchat.configs;

import lombok.Getter;
import ru.fozeton.chatmanager.config.AiStyleTextConfig;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.utils.Logger;

public class ConfigLoader {
    @Getter
    private static final ConfigLoader instance = new ConfigLoader();
    private final ChatConfigManager manager = ChatConfigManager.getInstance();
    private final Logger log = new Logger(ConfigLoader.class);

    @Getter
    private ChannelsConfig channelsConfig;
    @Getter
    private MessagesFilterConfig messagesFilterConfig;
    @Getter
    private AiStyleTextConfig textStyle;

    public void loaded() {
        channelsConfig = manager.load(ChannelsConfig.class);
        if (channelsConfig == null || channelsConfig.getChannels() == null || channelsConfig.getChannels().isEmpty()) {
            log.info("Создание дефолтной конфигурации чатов");
            channelsConfig = new ChatChannelsConfig().createDefault();
            manager.save(channelsConfig);
        }

        messagesFilterConfig = manager.load(MessagesFilterConfig.class);
        if (messagesFilterConfig == null || messagesFilterConfig.getFilters() == null || messagesFilterConfig.getFilters().isEmpty()) {
            log.info("Создание дефолтной конфигурации фильтров сообщений");
            messagesFilterConfig = MessagesFilterConfig.createDefault();
            manager.save(messagesFilterConfig);
        }
        textStyle = manager.load(AiStyleTextConfig.class);
        if (textStyle == null || textStyle.getStyles() == null || textStyle.getStyles().isEmpty()) {
            log.info("Создание дефолтной конфигурации фильтров сообщений");
            textStyle = new TextStyle().createDefault();
            manager.save(textStyle);
        }
    }
}
