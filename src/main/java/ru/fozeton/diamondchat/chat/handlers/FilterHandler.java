package ru.fozeton.diamondchat.chat.handlers;

import lombok.RequiredArgsConstructor;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageHandler;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class FilterHandler implements MessageHandler {
    private final Pattern pattern;
    private final String lineColor;
    private final String borderColor;

    @Override
    public boolean action(Message message) {
        if(pattern.matcher(message.getPlainText()).find()) {
            message.getMetadata().put("lineColor", lineColor);
            message.getMetadata().put("borderColor", borderColor);

            return true;
        }

        return false;
    }
}
