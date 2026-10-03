package ru.fozeton.diamondchat.chat.utils;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import ru.fozeton.chatmanager.messages.MessageType;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;

import static ru.fozeton.chatmanager.ChatManagerCore.GIF_PATTERN;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatMetrics {
    public static final int GIF_SPACING = 3;
    @Getter
    private static final ChatMetrics instance = new ChatMetrics();

    public int getLineSlots(ChatChannelView.ChatLine chatLine, Font renderer, int viewWidth) {
        if (chatLine.message().getType() == MessageType.ANIMATED) {
            Matcher matcher = GIF_PATTERN.matcher(chatLine.message().getPlainText());
            if (matcher.find()) {
                String gifId = matcher.group().replace(":", "");
                GifTexture gif = GifTexture.get(gifId);
                int gifHeight = gif.getHeight() / 2;

                int lineStep = renderer.lineHeight + 3;
                Component cleanedContent = stripGifPattern(chatLine.message().getContent());
                int textLines = renderer.split(cleanedContent, viewWidth).size();
                int textBlockHeight = textLines * lineStep;

                return textBlockHeight + gifHeight + GIF_SPACING;
            }
        }
        return chatLine.texts().size() * 12;
    }

    public int getTotalPixels(Collection<ChatChannelView.ChatLine> chatLines, Font renderer, int viewWidth) {
        int total = 0;
        for (ChatChannelView.ChatLine line : chatLines) total += getLineSlots(line, renderer, viewWidth);

        return total;
    }

    public Component stripGifPattern(Component component) {
        MutableComponent result = Component.empty();
        component.visit(
                (style, text) -> {
                    String filtered = GIF_PATTERN.matcher(text).replaceAll("");
                    if (!filtered.isEmpty()) result.append(Component.literal(filtered).setStyle(style));
                    return Optional.empty();
                }, Style.EMPTY
        );
        return result;
    }
}