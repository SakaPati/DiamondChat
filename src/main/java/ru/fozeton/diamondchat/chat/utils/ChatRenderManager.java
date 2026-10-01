package ru.fozeton.diamondchat.chat.utils;

import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.player.ChatVisiblity;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.config.VoiceConfig;
import ru.fozeton.chatmanager.utils.Logger;
import ru.fozeton.chatmanager.utils.stt.VoiceIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatRenderManager {
    @Getter
    private static final ChatRenderManager instance = new ChatRenderManager();
    private final VoiceConfig voiceCfg = ChatConfigManager.getInstance().getVoiceConfig();
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
        VoiceIndicator.render(context, (g, frame) -> {
            final int bars = 5;
            final int barW = 3;
            final int gap = 2;
            final int maxH = 14;
            final int pad = 4;
            final float[] shape = {0.6f, 0.85f, 1.0f, 0.85f, 0.6f};

            int backgroundColor = Long.decode(voiceCfg.getBackgroundColor()).intValue();
            int loadingColor = Long.decode(voiceCfg.getLoadingColor()).intValue();
            int listeningColor = Long.decode(voiceCfg.getListeningColor()).intValue();
            int defaultColor = Long.decode(voiceCfg.getDefaultColor()).intValue();

            long now = System.currentTimeMillis();
            int panelW = bars * barW + (bars - 1) * gap + pad * 2;
            int panelH = maxH + pad * 2;
            int px = g.guiWidth() - panelW - 10;
            int py = 10;

            int border = switch (frame.state()) {
                case LISTENING -> Long.decode(voiceCfg.getBorderColorListening()).intValue();
                case ERROR -> 0xFFc04060;
                default -> Long.decode(voiceCfg.getBorderColorDefault()).intValue();
            };
            g.fill(px, py, px + panelW, py + panelH, backgroundColor);
            g.outline(px, py, panelW, panelH, border);

            for (int i = 0; i < bars; i++) {
                float h;
                int color;
                switch (frame.state()) {
                    case LOADING -> {
                        h = 0.5f + 0.35f * (float) Math.sin(now / 180.0 + i * 0.9);
                        color = loadingColor;
                    }
                    case LISTENING -> {
                        h = 0.15f + 0.85f * Math.min(1f, frame.level() * shape[i]);
                        color = listeningColor;
                    }
                    default -> {
                        h = 0.5f;
                        color = defaultColor;
                    }
                }
                int barH = Math.max(2, (int) (maxH * h));
                int bx = px + pad + i * (barW + gap);
                g.fill(bx, py + pad + maxH - barH, bx + barW, py + pad + maxH, color);
            }
        });

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