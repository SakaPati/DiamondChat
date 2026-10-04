package ru.fozeton.diamondchat.chat.utils;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.ChatVisiblity;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.utils.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatRenderManager {
    @Getter
    private static final ChatRenderManager instance = new ChatRenderManager();
    private final List<ChatChannelView> views = new ArrayList<>();

    private ChatRenderManager() {
        Logger log = new Logger(ChatRenderManager.class);
        ChannelsConfig config = ChatConfigManager.getInstance().getChannelsConfig();

        for (Map.Entry<String, ChannelsConfig.ChannelSettings> entry : config.getChannels().entrySet()) {
            String channelId = entry.getKey();
            ChannelsConfig.ChannelSettings settings = entry.getValue();

            ChatChannel chatChannel = new ChatChannel(channelId, settings.getName());
            log.info("Регистрация канала " + channelId + ": " + settings.getName());
            views.add(new ChatChannelView(chatChannel));
        }
    }

    public boolean render(GuiGraphicsExtractor context, int mouseX, int mouseY, boolean focused) {
        if (!this.isChatHidden()) {
            views.forEach(view -> view.render(context, mouseX, mouseY, focused));
            return true;
        }
        return false;
    }

    private boolean isChatHidden() {
        return Minecraft.getInstance().options.chatVisibility().get() == ChatVisiblity.HIDDEN;
    }
}