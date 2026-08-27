package ru.fozeton.diamondchat.chat.utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import ru.fozeton.chatmanager.messages.MessageType;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatMetrics {
    @Getter
    private static final ChatMetrics instance = new ChatMetrics();
    private static final Pattern GIF_PATTERN = Pattern.compile(":\\d{15,19}:");

    public int getLineSlots(ChatChannelView.ChatLine chatLine, int lineStep) {
        if (chatLine.message().getType() == MessageType.ANIMATED) {
            Matcher matcher = GIF_PATTERN.matcher(chatLine.message().getPlainText());
            if (matcher.find()) {
                String gifId = matcher.group().replaceAll(":", "");
                GifTexture gif = GifTexture.get(gifId);
                int gifHeight = gif.getHeight() / 2;

                int linesNeeded = Math.max(1, (int) Math.ceil((double) gifHeight / lineStep));
                int totalBlockHeight = linesNeeded * lineStep + 11;

                return (int) Math.ceil((double) totalBlockHeight / lineStep);
            }
        }
        return chatLine.texts().size();
    }

    public int getTotalLineSlots(Collection<ChatChannelView.ChatLine> chatLines, int lineStep) {
        int total = 0;
        for (ChatChannelView.ChatLine line : chatLines) total += getLineSlots(line, lineStep);

        return total;
    }
}