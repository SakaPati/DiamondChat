package ru.fozeton.diamondchat.chat.handlers;

import lombok.RequiredArgsConstructor;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageHandler;

import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RequiredArgsConstructor
public class ActionHandler implements MessageHandler {
    private final Pattern pattern;
    private final Consumer<Matcher> consumer;

    @Override
    public boolean action(Message message) {
        Matcher matcher = pattern.matcher(message.getFullPlain());

        if(matcher.find()) {
            consumer.accept(matcher);
            return true;
        }

        return false;
    }
}
