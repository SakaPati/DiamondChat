package ru.fozeton.diamondchat.configs;

import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.defaults.ChannelsDefault;

public class ChannelsModify extends ChannelsDefault {
    @Override
    public ChannelsConfig createDefault() {
        ChannelsConfig config = new ChannelsConfig();

        ChannelsConfig.ChannelSettings defaultSettings = new ChannelsConfig.ChannelSettings();
        defaultSettings.setId("Default");
        defaultSettings.setName("Основной");
        defaultSettings.setX(15);
        defaultSettings.setY(360);
        defaultSettings.setWidth(400);
        defaultSettings.setHeight(200);

        ChannelsConfig.ChannelSettings privateSettings = new ChannelsConfig.ChannelSettings();
        privateSettings.setId("Private");
        privateSettings.setName("Личный");
        privateSettings.setPattern("^\\s*ЛС\\s*\\|");
        privateSettings.setX(15);
        privateSettings.setY(250);
        privateSettings.setWidth(400);
        privateSettings.setHeight(200);

        ChannelsConfig.ChannelSettings clanSettings = new ChannelsConfig.ChannelSettings();
        clanSettings.setId("Clan");
        clanSettings.setName("Клан");
        clanSettings.setPattern("^\\s*\\[Клан]");
        clanSettings.setX(15);
        clanSettings.setY(150);
        clanSettings.setWidth(400);
        clanSettings.setHeight(200);

        ChannelsConfig.ChannelSettings localSettings = new ChannelsConfig.ChannelSettings();
        localSettings.setId("Local");
        localSettings.setName("Локальный чат");
        localSettings.setPattern("^Ⓛ");
        localSettings.setX(250);
        localSettings.setY(150);
        localSettings.setWidth(400);
        localSettings.setHeight(200);

        ChannelsConfig.ChannelSettings tradeSettings = new ChannelsConfig.ChannelSettings();
        tradeSettings.setId("Trade");
        tradeSettings.setName("Торговый чат");
        tradeSettings.setPattern("^Ⓜ");
        tradeSettings.setX(250);
        tradeSettings.setY(360);
        tradeSettings.setWidth(400);
        tradeSettings.setHeight(200);

        config.getChannels().put("Default", defaultSettings);
        config.getChannels().put("Private", privateSettings);
        config.getChannels().put("Clan", clanSettings);
        config.getChannels().put("Local", localSettings);
        config.getChannels().put("Trade", tradeSettings);

        return config;
    }
}
