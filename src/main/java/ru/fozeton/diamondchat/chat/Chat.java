package ru.fozeton.diamondchat.chat;

import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.diamondchat.chat.handlers.Handler;
import ru.fozeton.diamondchat.chat.utils.DefaultMessage;

public class Chat {
    public Chat() {
        new Handler();
        ChatManagerCore.EVENT_BUS.register(this);
        ChatManagerCore.setMessageParser(new DefaultMessage());
    }
}
