package ru.fozeton.diamondchat.configs;

import lombok.Getter;
import ru.fozeton.chatmanager.config.AiStyleTextConfig;
import ru.fozeton.chatmanager.config.AliasConfig;
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
    @Getter
    private AliasConfig aliasConfig;
    @Getter
    private ColorRemapperConfig remapperConfig;

    public void loaded() {
        channelsConfig = loadOrCreateDefault(ChannelsConfig.class, new ChatChannelsConfig(), "чатов");
        messagesFilterConfig = loadOrCreateDefault(MessagesFilterConfig.class, new MessagesFilterConfig(), "фильтров сообщений");
        textStyle = loadOrCreateDefault(AiStyleTextConfig.class, new TextStyle(), "стилей сообщений");
        aliasConfig = loadOrCreateDefault(AliasConfig.class, new AliasesConfig(), "алиасов сообщений");
        remapperConfig = loadOrCreateDefault(ColorRemapperConfig.class, new ColorRemapperConfig(), "ремаппера цветов");
    }

    private <T> T loadOrCreateDefault(Class<T> clazz, Config<T> factory, String logMessage) {
        T config = manager.load(clazz);
        if (config == null || factory.isEmpty(config)) {
            log.info("Создание дефолтной конфигурации " + logMessage);
            config = factory.createDefault();
            manager.save(config);
        }
        return config;
    }
}
