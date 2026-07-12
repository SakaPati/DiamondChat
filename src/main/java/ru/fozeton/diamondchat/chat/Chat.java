package ru.fozeton.diamondchat.chat;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.channel.MessageHandlingChannel;
import ru.fozeton.chatmanager.events.game.ClientLogin;
import ru.fozeton.diamondchat.chat.handlers.Handler;
import ru.fozeton.diamondchat.chat.utils.DefaultMessage;

@Setter
public class Chat {
    @Nullable
    private MessageHandlingChannel handler = null;

    public Chat() {
        ChatManagerCore.EVENT_BUS.register(this);
        ChatManagerCore.setMessageParser(new DefaultMessage());
    }

    @EventSubscriber(event = ClientLogin.class)
    public void onLogin() {
        if (handler == null) this.handler = new Handler();
    }
}
