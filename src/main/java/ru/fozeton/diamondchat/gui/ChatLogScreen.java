package ru.fozeton.diamondchat.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.diamondchat.gui.widgets.ChannelButton;
import ru.fozeton.diamondchat.gui.widgets.ChatHistoryWidget;
import ru.fozeton.diamondchat.gui.widgets.SearchEditBox;

import java.util.Map;
import java.util.Objects;

public class ChatLogScreen extends Screen {
    private ChatHistoryWidget historyWidget;
    private String currentChannelId = null;
    private String currentSearchQuery = "";

    public ChatLogScreen() {
        super(Component.literal("Chat Log"));
    }

    @Override
    protected void init() {
        super.init();
        int historyY = 50;
        int historyHeight = this.height - historyY - 10;
        int historyWidth = this.width - 20;

        this.historyWidget = new ChatHistoryWidget(10, historyY, historyWidth, historyHeight);
        this.addRenderableWidget(this.historyWidget);

        updateChannel(null);

        int xPadding = 10;

        this.addRenderableWidget(new ChannelButton(xPadding, 20, 90, 20, Component.literal("Все"),
                () -> updateChannel(null),
                () -> this.currentChannelId == null
        ));
        xPadding += 100;

        ChannelsConfig channelsConfig = ChatConfigManager.getInstance().load(ChannelsConfig.class);
        for (Map.Entry<String, ChannelsConfig.ChannelSettings> entry : channelsConfig.getChannels().entrySet()) {
            String channelId = entry.getKey();
            ChannelsConfig.ChannelSettings settings = entry.getValue();

            this.addRenderableWidget(new ChannelButton(xPadding, 20, 120, 20, Component.literal(settings.getName()),
                    () -> updateChannel(channelId),
                    () -> Objects.equals(this.currentChannelId, channelId)
            ));
            xPadding += 130;
        }

        int inputWidth = 100;
        int margin = 25;
        SearchEditBox searchBox = new SearchEditBox(this.font, this.width - inputWidth - margin, 20, inputWidth, 20, Component.literal("Поиск"));
        searchBox.setOnEnter(() -> {
            this.currentSearchQuery = searchBox.getValue();
            this.historyWidget.updateCacheAsync(this.currentSearchQuery, this.currentChannelId);
        });
        this.addRenderableWidget(searchBox);
    }

    private void updateChannel(String channelId) {
        this.currentChannelId = channelId;
        if (this.historyWidget != null) {
            this.historyWidget.updateCacheAsync(this.currentSearchQuery, channelId);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0x8018181f);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}