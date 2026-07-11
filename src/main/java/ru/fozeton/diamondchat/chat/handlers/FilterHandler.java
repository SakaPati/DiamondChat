package ru.fozeton.diamondchat.chat.handlers;

import lombok.Builder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageHandler;

import java.util.regex.Pattern;

@Builder
public class FilterHandler implements MessageHandler {
    private final Pattern pattern;
    private final String lineColor;
    private final String borderColor;
    @Nullable
    private final String replyMessage;

    @Override
    public boolean action(Message message) {
        if(pattern.matcher(message.getPlainText()).find()) {
            message.getMetadata().put("lineColor", lineColor);
            message.getMetadata().put("borderColor", borderColor);
            if(replyMessage != null) {
                LocalPlayer player = Minecraft.getInstance().player;
                if(player != null) player.connection.sendChat(replyMessage);
            }
            return true;
        }

        return false;
    }
}
