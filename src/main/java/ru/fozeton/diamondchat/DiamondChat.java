package ru.fozeton.diamondchat;

import com.ferra13671.megaevents.eventbus.IEventBus;
import com.ferra13671.megaevents.eventbus.impl.EventBus;
import net.fabricmc.api.ClientModInitializer;
import ru.fozeton.diamondchat.chat.Chat;
import ru.fozeton.diamondchat.chat.utils.ChatRenderManager;
import ru.fozeton.diamondchat.configs.ConfigLoader;
import ru.fozeton.diamondchat.utils.KeyBind;
import ru.fozeton.diamondchat.utils.notify.NotificationManager;

public final class DiamondChat implements ClientModInitializer {
    public static IEventBus EVENT_BUS = new EventBus();

    @Override
    public void onInitializeClient() {
        ConfigLoader.getInstance().loaded();
        ChatRenderManager.getInstance();
        KeyBind.getInstance().register();
        new Chat();
        NotificationManager.getInstance();
    }
}