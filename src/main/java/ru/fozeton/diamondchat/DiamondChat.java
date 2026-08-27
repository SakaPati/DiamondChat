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
import ru.fozeton.diamondchat.utils.MacroManager;
import ru.fozeton.diamondchat.utils.notify.NotificationManager;

public final class DiamondChat implements ClientModInitializer {
    public static final IEventBus EVENT_BUS = new EventBus();
    public static Chat CHAT_INSTANCE;
    public static ChannelsModify CHAT_MODIFY_INSTANCE = new ChannelsModify();

    @Override
    public void onInitializeClient() {
        ChatConfigManager manager = ChatConfigManager.getInstance();
        manager.registerDefault(ChannelsConfig.class, CHAT_MODIFY_INSTANCE);
        manager.registerDefault(AiStyleTextConfig.class, new AiStyleTextModify());

        ChatRenderManager.getInstance();
        KeyBind.getInstance().register();
        CHAT_INSTANCE = new Chat();
        NotificationManager.getInstance();
        MacroManager.getInstance();
    }
}