package ru.fozeton.diamondchat.chat.handlers;

import lombok.Builder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageHandler;
import ru.fozeton.chatmanager.messages.metadata.MessageMetadata;

import java.util.regex.Pattern;

@Builder
public class FilterHandler implements MessageHandler {
    private final Pattern pattern;
    private final int lineColor;
    private final int borderColor;
    @Nullable
    private final String replyMessage;

    @Override
    public boolean action(Message message) {
        if(pattern.matcher(message.getFullPlain()).find()) {
            message.getMetadata().push(MessageMetadata.builder().lineColor(lineColor).build());
            message.getMetadata().push(MessageMetadata.builder().borderColor(borderColor).build());
            if(replyMessage != null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if(player != null) player.connection.sendChat(replyMessage);
            }
            return true;
        }

        return false;
    }
}
