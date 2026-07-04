package ru.fozeton.diamondchat.chat;

import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.channel.MessageHandlingChannel;
import ru.fozeton.diamondchat.chat.handlers.Handler;
import ru.fozeton.diamondchat.chat.utils.DefaultMessage;

public class Chat {
    private MessageHandlingChannel handler = new Handler();

    public Chat() {
        ChatManagerCore.EVENT_BUS.register(this);
        ChatManagerCore.setMessageParser(new DefaultMessage());
    }

    public void setHandler(MessageHandlingChannel handler) {
        ChatManagerCore.EVENT_BUS.unregister(this.handler);
        this.handler = handler;
    }
}
