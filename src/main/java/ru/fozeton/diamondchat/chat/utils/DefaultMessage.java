package ru.fozeton.diamondchat.chat.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import ru.dargen.evoplus.api.EvoPlusApi;
import ru.fozeton.chatmanager.messages.ChatMessageParser;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageType;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultMessage implements ChatMessageParser {
    private final Pattern evo = Pattern.compile("\\w+(?=(?:\\s+[^\\[\\s]+)?\\s+\\[)");
    private final Pattern hub = Pattern.compile(" \\w+(?=:)");
    private final Pattern INVISIBLE_CHARS = Pattern.compile("[\\uE000-\\uF8FF\\u200B-\\u200F\\uFEFF]");

    @Override
    public Message parseSystemChat(ClientboundSystemChatPacket packet) {
        Component content = packet.content();
        String text = content.getString();
        String author = EvoPlusApi.getServer().getId() == 0 ? getNickname(text, hub) : getNickname(text, evo);

        MessageType type = MessageType.SYSTEM;

        if (content.getStyle() != null && content.getStyle().getColor() != null) {
            String colorName = content.getStyle().getColor().serialize();
            boolean isRedError = "red".equals(colorName) || "dark_red".equals(colorName);
            if (isRedError) type = MessageType.ERROR;
        }

        return new Message(
                UUID.randomUUID().toString(),
                author,
                useTimeFormatter(content),
                text,
                type,
                Instant.now()
        );
    }

    private String getNickname(String line, Pattern pattern) {
        String cleaned = INVISIBLE_CHARS.matcher(line).replaceAll("");
        Matcher matcher = pattern.matcher(cleaned);
        if (matcher.find()) {
            return matcher.group().trim();
        }
        return null;
    }
}
