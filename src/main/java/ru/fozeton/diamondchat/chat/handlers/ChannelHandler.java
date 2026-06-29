package ru.fozeton.diamondchat.chat.handlers;


import lombok.RequiredArgsConstructor;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageHandler;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ChannelHandler implements MessageHandler {
    private final Pattern pattern;
    private final String channelId;
    private final Runnable action;

    @Override
    public boolean action(Message message) {
        if(action != null) action.run();
        if (pattern.matcher(message.getPlainText()).find()) {
            ChatManagerCore.getChannels().get(channelId).addMessage(message);
            return true;
        }
        return false;
    }
}
