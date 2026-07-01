package ru.fozeton.diamondchat.gui.widgets;

import lombok.Setter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageType;
import ru.fozeton.diamondchat.chat.utils.channel.MessageColorRemapper;
import ru.fozeton.diamondchat.utils.notify.Notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class ChatHistoryWidget extends AbstractWidget {
    private static final int BAR_W = 4;
    private static final int COL_TRACK = 0xFF2e2e3d;
    private static final int COL_THUMB = 0xFF7c6ef5;
    private static final int COL_THUMB_HOV = 0xFFa78bfa;
    private static final int COL_THUMB_DARK = 0xFF5b50c4;

    private final MessageColorRemapper colorRemapper = MessageColorRemapper.getInstance();
    private final ChannelsConfig settings = ChatConfigManager.getInstance().load(ChannelsConfig.class);
    private final int blinkHex;
    private final AtomicLong currentBuildId = new AtomicLong(0);
    @Setter
    private boolean useColorRemapper = settings.getHistoryChat().isUseColorRemapper();
    private volatile List<CachedLine> cachedLines = List.of();
    private volatile int fullHeight = 10;
    private double scrollAmount = 0;
    private boolean dragging = false;
    private double dragStartY = 0;
    private double dragStartScroll = 0;
    private boolean isHoveringScrollbar = false;

    public ChatHistoryWidget(int x, int y, int width, int height) {
        super(x, y, width, height, Component.empty());
        this.blinkHex = Long.decode(settings.getHistoryChat().getBlinkColor()).intValue();
    }

    public void updateCacheAsync(String searchQuery, String channelId) {
        final long buildId = currentBuildId.incrementAndGet();
        final boolean remapColors = this.useColorRemapper;

        CompletableFuture.supplyAsync(() -> buildCache(searchQuery, channelId, remapColors))
                .thenAcceptAsync(result -> {
                    if (buildId != currentBuildId.get()) return;
                    this.cachedLines = result.lines();
                    this.fullHeight = result.totalHeight();
                    scrollToBottom();
                }, Minecraft.getInstance());
    }

    private BuildResult buildCache(String searchQuery, String channelId, boolean remapColors) {
        Font font = Minecraft.getInstance().font;
        List<Message> history = ChatChannel.getMessageHistory();
        List<CachedLine> newCache = new ArrayList<>();
        int currentY = 0;
        int lineH = font.lineHeight + 3;
        int contentWidth = this.width - 25;

        synchronized (history) {
            for (int i = history.size() - 1; i >= 0; i--) {
                Message message = history.get(i);
                if (!matchesFilter(message, channelId, searchQuery)) continue;

                Component text = remapColors ? colorRemapper.remap(message.getContent()) : message.getContent();
                List<FormattedCharSequence> lines = font.split(text, contentWidth);
                int totalH = lines.size() * lineH;

                int baseStripe = switch (message.getType()) {
                    case PLAYER, CLIENT, CUSTOM -> 0xFFffffff;
                    default -> 0xFF7c6ef5;
                };

                int backgroundColor = 0x80000000;
                Map<String, Object> metadata = message.getMetadata();

                if (metadata.get("lineColor") instanceof String s) backgroundColor = Long.decode(s).intValue();
                if (metadata.get("borderColor") instanceof String s) baseStripe = Long.decode(s).intValue();

                int finalBg = message.getType() == MessageType.MENTIONED ? blinkHex : backgroundColor;
                int finalStripe = message.getType() == MessageType.MENTIONED ? 0xFFc4a020 : baseStripe;

                if (remapColors) {
                    finalBg = colorRemapper.applyAlpha(finalBg, 128);
                    finalStripe = colorRemapper.applyAlpha(finalStripe, 255);
                }

                newCache.add(new CachedLine(message, lines, finalBg, finalStripe, currentY, totalH));
                currentY += totalH;
            }
        }
        return new BuildResult(newCache, Math.max(currentY, 10));
    }

    private boolean matchesFilter(Message message, String channelId, String query) {
        if (channelId != null && !channelId.equals(message.getMetadata().get("channelId"))) return false;
        if (query != null && !query.isEmpty() && !message.getPlainText().toLowerCase().contains(query.toLowerCase()))
            return false;
        return true;
    }

    private void scrollToBottom() {
        this.scrollAmount = getMaxScroll();
    }

    private int getMaxScroll() {
        return Math.max(0, this.fullHeight - this.height);
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int lineH = font.lineHeight + 3;

        graphics.enableScissor(getX(), getY(), getX() + this.width, getY() + this.height);

        Style hoveredStyle = null;
        int viewTop = (int) this.scrollAmount;
        int viewBottom = viewTop + this.height;

        for (CachedLine cl : this.cachedLines) {
            if (cl.yOffset() + cl.totalHeight() < viewTop) continue;
            if (cl.yOffset() > viewBottom) break;

            int lineTop = getY() + cl.yOffset() - (int) this.scrollAmount;
            boolean isHovered = mouseX >= getX() && mouseX < getX() + this.width - 15
                    && mouseY >= lineTop && mouseY < lineTop + cl.totalHeight();

            int rowY = lineTop;
            for (FormattedCharSequence textLine : cl.lines()) {
                graphics.fill(getX(), rowY, getX() + this.width - 15, rowY + lineH, cl.bg());
                graphics.fill(getX(), rowY, getX() + 2, rowY + lineH, cl.stripe());
                graphics.drawString(font, textLine, getX() + 6, rowY + 2, -1, false);
                rowY += lineH;
            }

            if (isHovered) {
                graphics.fill(getX(), lineTop, getX() + this.width - 15, lineTop + cl.totalHeight(), 0x1AFFFFFF);
                int relX = mouseX - (getX() + 6);
                int relY = mouseY - lineTop;
                int lineIndex = relY / lineH;
                if (relX >= 0 && lineIndex >= 0 && lineIndex < cl.lines().size()) {
                    hoveredStyle = font.getSplitter().componentStyleAtWidth(cl.lines().get(lineIndex), relX);
                }
            }
        }

        graphics.disableScissor();

        if (hoveredStyle != null) {
            graphics.renderComponentHoverEffect(font, hoveredStyle, mouseX, mouseY);
        }

        renderScrollbar(graphics, mouseX, mouseY);
    }

    private void renderScrollbar(GuiGraphics graphics, int mouseX, int mouseY) {
        if (this.fullHeight <= this.height) return;

        int trackX = getX() + this.width - BAR_W;
        graphics.fill(trackX, getY(), trackX + BAR_W, getY() + this.height, COL_TRACK);

        int thumbH = Math.max(16, (int) ((float) this.height * this.height / this.fullHeight));
        int scrollRange = getMaxScroll();
        int travel = this.height - thumbH;
        int thumbY = getY() + (int) (this.scrollAmount * travel / scrollRange);

        this.isHoveringScrollbar = mouseX >= trackX - 2 && mouseX <= trackX + BAR_W && mouseY >= thumbY && mouseY <= thumbY + thumbH;
        boolean hov = isHoveringScrollbar || dragging;

        graphics.fill(trackX - 1, thumbY, trackX + BAR_W + 1, thumbY + thumbH, COL_THUMB_DARK);
        graphics.fill(trackX, thumbY + 1, trackX + BAR_W, thumbY + thumbH - 1, hov ? COL_THUMB_HOV : COL_THUMB);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        this.scrollAmount = Mth.clamp(this.scrollAmount - scrollY * 20, 0, getMaxScroll());
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!isMouseOver(mouseX, mouseY)) return false;

        if (button == 0 && this.fullHeight > this.height) {
            int trackX = getX() + this.width - BAR_W;
            if (mouseX >= trackX - 2) {
                this.dragging = true;
                this.dragStartY = mouseY;
                this.dragStartScroll = this.scrollAmount;
                return true;
            }
        }

        if (button == 0 || button == 1) {
            Font font = Minecraft.getInstance().font;
            int lineH = font.lineHeight + 3;

            for (CachedLine cl : this.cachedLines) {
                int lineTop = getY() + cl.yOffset() - (int) this.scrollAmount;
                if (mouseY >= lineTop && mouseY < lineTop + cl.totalHeight() && mouseX < getX() + this.width - 15) {
                    if (button == 0) {
                        int relX = (int) mouseX - (getX() + 6);
                        int lineIndex = ((int) mouseY - lineTop) / lineH;
                        if (relX >= 0 && lineIndex >= 0 && lineIndex < cl.lines().size()) {
                            Style style = font.getSplitter().componentStyleAtWidth(cl.lines().get(lineIndex), relX);
                            if (style != null && Minecraft.getInstance().screen != null) {
                                Minecraft.getInstance().screen.handleComponentClicked(style);
                                return true;
                            }
                        }
                    } else {
                        Minecraft.getInstance().keyboardHandler.setClipboard(cl.message().getContent().getString());
                        Notification.showInfo("Сообщение скопировано успешно!");
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.dragging && button == 0) {
            int thumbH = Math.max(16, (int) ((float) this.height * this.height / this.fullHeight));
            int travel = this.height - thumbH;
            if (travel > 0) {
                double deltaY = mouseY - this.dragStartY;
                this.scrollAmount = Mth.clamp(this.dragStartScroll + (deltaY * getMaxScroll() / travel), 0, getMaxScroll());
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) this.dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
    }

    record CachedLine(Message message, List<FormattedCharSequence> lines, int bg, int stripe, int yOffset,
                      int totalHeight) {
    }

    record BuildResult(List<CachedLine> lines, int totalHeight) {
    }
}