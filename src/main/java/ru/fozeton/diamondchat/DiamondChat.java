package ru.fozeton.diamondchat;

import com.ferra13671.megaevents.eventbus.IEventBus;
import com.ferra13671.megaevents.eventbus.impl.EventBus;
import net.fabricmc.api.ClientModInitializer;
import ru.fozeton.chatmanager.config.AiStyleTextConfig;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.diamondchat.chat.Chat;
import ru.fozeton.diamondchat.chat.utils.ChatRenderManager;
import ru.fozeton.diamondchat.configs.AiStyleTextModify;
import ru.fozeton.diamondchat.configs.ChannelsModify;
import ru.fozeton.diamondchat.utils.KeyBind;
import ru.fozeton.diamondchat.utils.notify.NotificationManager;

public final class DiamondChat implements ClientModInitializer {
    public static IEventBus EVENT_BUS = new EventBus();

    @Override
    public void onInitializeClient() {
        ChatConfigManager manager = ChatConfigManager.getInstance();
        manager.registerDefault(ChannelsConfig.class, new ChannelsModify());
        manager.registerDefault(AiStyleTextConfig.class, new AiStyleTextModify());

        ChatRenderManager.getInstance();
        KeyBind.getInstance().register();
        new Chat();
        NotificationManager.getInstance();
    }
}