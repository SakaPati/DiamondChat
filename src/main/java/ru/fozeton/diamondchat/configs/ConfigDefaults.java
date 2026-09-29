package ru.fozeton.diamondchat.configs;

import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;

public final class ConfigDefaults {
    private ConfigDefaults() {}

    public static void apply() {
        var manager = ChatConfigManager.getInstance();
        ChannelsConfig config = manager.getOrLoad(ChannelsConfig.class);

        String trade = "Trade";
        if (config.getChannels().containsKey(trade)) return;

        config.getChannels().clear();
        config.getChannels().put("Default", channel("Default", "Основной", null, 15, 360));
        config.getChannels().put("Private", channel("Private", "Личный", "^\\s*ЛС\\s*\\|", 15, 250));
        config.getChannels().put("Clan", channel("Clan", "Клан", "^\\s*\\[Клан]", 15, 150));
        config.getChannels().put("Local", channel("Local", "Локальный чат", "^Ⓛ", 250, 150));
        config.getChannels().put(trade, channel(trade, "Торговый чат", "^Ⓜ", 250, 360));

        manager.save(ChannelsConfig.class);
    }

    private static ChannelsConfig.ChannelSettings channel(String id, String name, String pattern, int x, int y) {
        var s = new ChannelsConfig.ChannelSettings();
        s.setId(id);
        s.setName(name);
        if (pattern != null) s.setPattern(pattern);
        s.setX(x);
        s.setY(y);
        s.setWidth(400);
        s.setHeight(200);
        return s;
    }
}