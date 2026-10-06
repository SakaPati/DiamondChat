package ru.fozeton.diamondchat.chat.handlers;


import lombok.Getter;
import lombok.RequiredArgsConstructor;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageHandler;

import java.util.regex.Pattern;

@Getter
@RequiredArgsConstructor
public class ChannelHandler implements MessageHandler {
    private final Pattern pattern;
    private final String channelId;
    private final Runnable action;

    @Override
    public boolean action(Message message) {
        if(action != null) action.run();
        return pattern.matcher(message.getFullPlain()).find();
    }
}
