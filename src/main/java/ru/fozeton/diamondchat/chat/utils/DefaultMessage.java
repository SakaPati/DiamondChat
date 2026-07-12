package ru.fozeton.diamondchat.chat.utils;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket;
import ru.fozeton.chatmanager.messages.ChatMessageParser;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageType;

import java.time.Instant;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DefaultMessage implements ChatMessageParser {
    private final Pattern universalPattern = Pattern.compile("(?<=\\s)[A-Za-z0-9_]{3,16}(?=[^:\\n]*:)");
    private final Pattern INVISIBLE_CHARS = Pattern.compile("[\\uE000-\\uF8FF\\u200B-\\u200F\\uFEFF]");
    private final Pattern COLOR_CODES = Pattern.compile("§.");

    @Override
    public Message parseSystemChat(ClientboundSystemChatPacket packet) {
        Component content = packet.content();
        String text = content.getString();
        String cleanColors = COLOR_CODES.matcher(text).replaceAll("");
        String cleaned = INVISIBLE_CHARS.matcher(cleanColors).replaceAll("");

        String author = getNickname(cleaned);

        MessageType type = MessageType.SYSTEM;

        if (content.getStyle().getColor() != null) {
            String colorName = content.getStyle().getColor().serialize();
            boolean isRedError = "red".equals(colorName) || "dark_red".equals(colorName);
            if (isRedError) type = MessageType.ERROR;
        }

        return new Message(
                UUID.randomUUID().toString(),
                author,
                useTimeFormatter(content),
                cleaned,
                type,
                Instant.now()
        );
    }

    private String getNickname(String cleanedLine) {
        Matcher matcher = universalPattern.matcher(cleanedLine);

        String lastMatch = null;
        while (matcher.find()) lastMatch = matcher.group().trim();

        return lastMatch;
    }
}