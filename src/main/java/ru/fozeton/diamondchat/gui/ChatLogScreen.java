package ru.fozeton.diamondchat.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.diamondchat.gui.widgets.ChannelButton;
import ru.fozeton.diamondchat.gui.widgets.ChatHistoryWidget;
import ru.fozeton.diamondchat.gui.widgets.SearchEditBox;

import java.util.*;

public class ChatLogScreen extends Screen {
    private static final int BUTTON_GAP = 6;
    private static final int BUTTON_HPADDING = 16;
    private static final int MIN_BUTTON_WIDTH = 50;
    private static final int CHANNELS_AREA_HEIGHT = 20;
    private static final int SEARCH_RESERVED_WIDTH = 140;
    private static final int MAX_CONTENT_WIDTH_THRESHOLD = 1630;
    private static final int SCROLL_STEP = 20;
    private final List<ChannelButton> channelButtonsList = new ArrayList<>();
    private ChatHistoryWidget historyWidget;
    private String currentChannelId = null;
    private String currentSearchQuery = "";
    private int channelsAreaX;
    private int channelsAreaY = 20;
    private int channelsAreaWidth;
    private int channelsContentWidth = 0;
    private int channelsScrollOffset = 0;
    private boolean channelsScrollEnabled = false;

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

        this.channelsAreaX = 10;
        this.channelsAreaWidth = Math.max(100, this.width - this.channelsAreaX - SEARCH_RESERVED_WIDTH);

        buildChannelButtons();

        int inputWidth = 100;
        int margin = 25;
        SearchEditBox searchBox = new SearchEditBox(
                this.font,
                this.width - inputWidth - margin,
                20,
                inputWidth,
                20,
                Component.empty()
        );
        searchBox.setHint(Component.literal("Поиск"));

        searchBox.setOnEnter(() -> {
            this.currentSearchQuery = searchBox.getValue();
            this.historyWidget.updateCacheAsync(this.currentSearchQuery, this.currentChannelId);
        });
        this.addRenderableWidget(searchBox);
    }

    private void buildChannelButtons() {
        this.channelButtonsList.clear();
        this.channelsScrollOffset = 0;

        Map<String, String> entries = new LinkedHashMap<>();
        entries.put(null, "Все");
        ChannelsConfig channelsConfig = ChatConfigManager.getInstance().getChannelsConfig();
        for (Map.Entry<String, ChannelsConfig.ChannelSettings> entry : channelsConfig.getChannels().entrySet()) {
            entries.put(entry.getKey(), entry.getValue().getName());
        }

        int totalWidth = 0;
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            String channelId = entry.getKey();
            String name = entry.getValue();

            int textWidth = this.font.width(name);
            int btnWidth = Math.max(MIN_BUTTON_WIDTH, textWidth + BUTTON_HPADDING);

            ChannelButton button = new ChannelButton(
                    0, this.channelsAreaY, btnWidth, CHANNELS_AREA_HEIGHT,
                    Component.literal(name),
                    () -> updateChannel(channelId),
                    () -> Objects.equals(this.currentChannelId, channelId)
            );

            this.channelButtonsList.add(button);
            totalWidth += btnWidth + BUTTON_GAP;
        }
        this.channelsContentWidth = Math.max(0, totalWidth - BUTTON_GAP);

        int channelsCount = channelsConfig.getChannels().size();
        this.channelsScrollEnabled = channelsCount > 6 || this.channelsContentWidth > MAX_CONTENT_WIDTH_THRESHOLD;

        repositionChannelButtons();
    }

    private void repositionChannelButtons() {
        int x = this.channelsAreaX - this.channelsScrollOffset;
        for (ChannelButton button : this.channelButtonsList) {
            button.setX(x);
            x += button.getWidth() + BUTTON_GAP;
        }
    }

    private boolean isInsideChannelsArea(double mouseX, double mouseY) {
        return mouseX >= this.channelsAreaX && mouseX <= this.channelsAreaX + this.channelsAreaWidth
               && mouseY >= this.channelsAreaY && mouseY <= this.channelsAreaY + CHANNELS_AREA_HEIGHT;
    }

    private void updateChannel(String channelId) {
        this.currentChannelId = channelId;
        if (this.historyWidget != null) {
            this.historyWidget.updateCacheAsync(this.currentSearchQuery, channelId);
        }
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        graphics.fill(0, 0, this.width, this.height, 0x8018181f);
    }

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        extractChannelButtons(graphics, mouseX, mouseY, partialTick);
    }

    private void extractChannelButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        boolean scissor = this.channelsScrollEnabled;
        if (scissor) {
            graphics.enableScissor(
                    this.channelsAreaX, this.channelsAreaY,
                    this.channelsAreaX + this.channelsAreaWidth, this.channelsAreaY + CHANNELS_AREA_HEIGHT
            );
        }
        for (ChannelButton button : this.channelButtonsList) {
            button.extractRenderState(graphics, mouseX, mouseY, partialTick);
        }
        if (scissor) {
            graphics.disableScissor();
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (isInsideChannelsArea(event.x(), event.y())) {
            for (ChannelButton channelButton : this.channelButtonsList) {
                if (channelButton.isMouseOver(event.x(), event.y())
                    && channelButton.mouseClicked(event, doubleClick)) {
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (ChannelButton channelButton : this.channelButtonsList) {
            channelButton.mouseReleased(event);
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.channelsScrollEnabled && isInsideChannelsArea(mouseX, mouseY)) {
            int maxOffset = Math.max(0, this.channelsContentWidth - this.channelsAreaWidth);
            this.channelsScrollOffset -= (int) (scrollY * SCROLL_STEP);
            this.channelsScrollOffset = Math.clamp(this.channelsScrollOffset, 0, maxOffset);
            repositionChannelButtons();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}