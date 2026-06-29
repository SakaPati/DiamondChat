package ru.fozeton.diamondchat.configs;

import lombok.Getter;
import lombok.Setter;
import ru.fozeton.chatmanager.config.ChannelsConfig;

@Getter
@Setter
public class ChatChannelsConfig extends ChannelsConfig {
    @Override
    public ChannelsConfig createDefault() {
        ChannelsConfig config = new ChannelsConfig();

        ChannelSettings defaultSettings = new ChannelSettings();
        defaultSettings.setId("Default");
        defaultSettings.setName("Основной");
        defaultSettings.setX(15);
        defaultSettings.setY(360);
        defaultSettings.setWidth(400);
        defaultSettings.setHeight(200);

        ChannelSettings privateSettings = new ChannelSettings();
        privateSettings.setId("Private");
        privateSettings.setName("Личный");
        privateSettings.setPattern("^\\s*ЛС\\s*\\|");
        privateSettings.setX(15);
        privateSettings.setY(250);
        privateSettings.setWidth(400);
        privateSettings.setHeight(200);

        ChannelSettings clanSettings = new ChannelSettings();
        clanSettings.setId("Clan");
        clanSettings.setName("Клан");
        clanSettings.setPattern("^\\s*\\[Клан]");
        clanSettings.setX(15);
        clanSettings.setY(150);
        clanSettings.setWidth(400);
        clanSettings.setHeight(200);

        ChannelSettings localSettings = new ChannelSettings();
        localSettings.setId("Local");
        localSettings.setName("Локальный чат");
        localSettings.setPattern("^Ⓛ");
        localSettings.setX(250);
        localSettings.setY(150);
        localSettings.setWidth(400);
        localSettings.setHeight(200);

        config.getChannels().put("Default", defaultSettings);
        config.getChannels().put("Private", privateSettings);
        config.getChannels().put("Clan", clanSettings);
        config.getChannels().put("Local", localSettings);

        return config;
    }
}