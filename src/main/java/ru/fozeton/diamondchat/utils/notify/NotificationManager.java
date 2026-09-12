package ru.fozeton.diamondchat.utils.notify;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import lombok.Getter;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.events.InputEvent;
import ru.fozeton.diamondchat.DiamondChat;
import ru.fozeton.diamondchat.events.game.NotificationEvent;
import ru.fozeton.diamondchat.utils.enums.NotificationType;

import java.util.ArrayList;
import java.util.List;

public class NotificationManager {
    @Getter
    private static final NotificationManager instance = new NotificationManager();
    private static final float TITLE_SCALE = 1.4f;
    private static final float MESSAGE_SCALE = 1.2f;
    private static final long ANIMATION_DURATION = 150;
    private final List<ActiveNotification> activeNotifications = new ArrayList<>();

    public NotificationManager() {
        super();
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> ScreenEvents.afterRender(screen).register(
                (s, drawContext, mouseX, mouseY, tickDelta) -> renderNotifications(drawContext, client)));

        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
                                                            layeredDrawer.attachLayerAfter(
                                                                    IdentifiedLayer.SUBTITLES,
                                                                    ResourceLocation.fromNamespaceAndPath(
                                                                            "moderation",
                                                                            "notification"
                                                                    ),
                                                                    (drawContext, tickCounter) -> {
                                                                        if (Minecraft.getInstance().screen == null) {
                                                                            renderNotifications(
                                                                                    drawContext,
                                                                                    Minecraft.getInstance()
                                                                            );
                                                                        }
                                                                    }
                                                            ));
        DiamondChat.EVENT_BUS.register(this);
        ChatManagerCore.EVENT_BUS.register(this);
    }

    private void renderNotifications(GuiGraphics graphics, Minecraft client) {
        activeNotifications.removeIf(ActiveNotification::isFullyExpired);
        if (activeNotifications.isEmpty()) return;

        Font font = client.font;

        int windowWidth = graphics.guiWidth();
        int currentY = 10;
        int spacing = 5;

        double mouseX = client.mouseHandler.xpos() * windowWidth / client.getWindow().getWidth();
        double mouseY = client.mouseHandler.ypos() * graphics.guiHeight() / client.getWindow().getHeight();

        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, 5000f);

        for (ActiveNotification active : activeNotifications) {
            float animationScale = getAnimationScale(active);

            RenderGeometry geo = active.calculateGeometry(font, animationScale, windowWidth, currentY);

            boolean isHovered = mouseX >= geo.x() && mouseX <= (geo.x() + geo.width()) && mouseY >= geo.y() &&
                                mouseY <= (geo.y() + geo.height());
            boolean clickable = isHovered && active.action != null;
            int backgroundColor = clickable ? 0xFF1e1e2a : 0xFF18181f;
            int borderColor = clickable ? 0xFF7c6ef5 : 0xFF3a2e6e;
            int accentColor;
            int titleColor;
            int messageColor = 0xFF6b6b88;

            int[] colors = switch (active.type) {
                case SUCCESS -> clickable
                        ? new int[]{0xFF34d68e, 0xFF34d68e}
                        : new int[]{0xFF2aaa72, 0xFF2aaa72};
                case ERROR -> clickable
                        ? new int[]{0xFFe84d78, 0xFFe84d78}
                        : new int[]{0xFFc04060, 0xFFc04060};

                case WARN -> clickable
                        ? new int[]{0xFFf0c830, 0xFFf0c830}
                        : new int[]{0xFFc4a020, 0xFFc4a020};

                default -> clickable
                        ? new int[]{0xFFa78bfa, 0xFFede9fe}
                        : new int[]{0xFF7c6ef5, 0xFFd4d0f5};
            };

            accentColor = colors[0];
            titleColor = colors[1];

            graphics.fill(geo.x(), geo.y(), geo.x() + geo.width(), geo.y() + geo.height(), backgroundColor);
            graphics.renderOutline(geo.x(), geo.y(), geo.width(), geo.height(), borderColor);
            graphics.fill(geo.x() + 1, geo.y(), geo.x() - 2, geo.y() + geo.height(), accentColor);

            graphics.pose().pushPose();
            graphics.pose().translate(geo.x() + 3, geo.y() + 5, 0);
            graphics.pose().scale(TITLE_SCALE * animationScale, TITLE_SCALE * animationScale, 1.0f);
            graphics.drawString(font, geo.title(), 5, 2, titleColor, false);
            graphics.pose().popPose();

            graphics.pose().pushPose();
            graphics.pose().translate(geo.x(), geo.y() + 5, 0);
            graphics.pose().scale(MESSAGE_SCALE * animationScale, MESSAGE_SCALE * animationScale, 1.0f);
            graphics.drawString(font, geo.message(), 8, 15, messageColor, false);
            graphics.pose().popPose();

            float progress = active.getProgress();
            int progressWidth = (int) (geo.width() * progress);
            graphics.fill(
                    geo.x(),
                    geo.y() + geo.height(),
                    geo.x() + progressWidth,
                    geo.y() + geo.height() - 1,
                    accentColor
            );

            if (geo.height() > 0) currentY += geo.height() + spacing;
        }

        graphics.pose().popPose();
    }

    private float getAnimationScale(ActiveNotification active) {
        long timePassed;
        float progress;
        if (active.closing) {
            timePassed = System.currentTimeMillis() - active.closeStartTime;
            progress = 1.0f - ((float) timePassed / ANIMATION_DURATION);
        } else {
            timePassed = System.currentTimeMillis() - active.startTime;
            progress = (float) timePassed / ANIMATION_DURATION;
        }
        return Math.clamp(progress, 0f, 1f);
    }

    @EventSubscriber(event = NotificationEvent.class)
    public void onNotificationReceived(NotificationEvent event) {
        activeNotifications.add(new ActiveNotification(event.getNotification(), event.getNotification().getTextFont()));
    }

    @EventSubscriber(event = InputEvent.MouseInputEvent.class)
    public void onNotificationClick(InputEvent.MouseInputEvent event) {
        if (event.getButton() != 0 || event.getAction() != 1) return;
        if (activeNotifications.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();

        Font font = client.font;
        int windowWidth = client.getWindow().getGuiScaledWidth();
        int windowHeight = client.getWindow().getGuiScaledHeight();

        double mouseX = client.mouseHandler.xpos() * windowWidth / client.getWindow().getScreenWidth();
        double mouseY = client.mouseHandler.ypos() * windowHeight / client.getWindow().getScreenHeight();

        int currentY = 10;
        int spacing = 5;

        for (ActiveNotification active : activeNotifications) {
            if (active.closing || active.action == null) continue;

            float animationScale = getAnimationScale(active);
            RenderGeometry geo = active.calculateGeometry(font, animationScale, windowWidth, currentY);
            if (mouseX >= geo.x() && mouseX <= (geo.x() + geo.width()) && mouseY >= geo.y() &&
                mouseY <= (geo.y() + geo.height())) {
                Notification notification = active.notification;
                if (notification.getAction() != null) client.execute(notification.getAction());
                active.forceClose();
                break;
            }

            if (geo.height() > 0) currentY += geo.height() + spacing;
        }
    }

    private static class ActiveNotification {
        private final Notification notification;
        private final long startTime;
        private final long expiryTime;
        private final NotificationType type;
        private final Runnable action;
        @Nullable
        private final ResourceLocation textFont;

        private boolean closing = false;
        private long closeStartTime;

        public ActiveNotification(Notification notification, @Nullable ResourceLocation textFont) {
            this.notification = notification;
            this.startTime = System.currentTimeMillis();
            this.expiryTime = this.startTime + notification.getDuration().toMillis();
            this.type = notification.getType();
            this.action = notification.getAction();
            this.textFont = textFont;
        }

        public RenderGeometry calculateGeometry(
                Font font,
                float animationScale,
                int windowWidth,
                int currentY
        ) {
            Component title = textFont == null ? Component.literal(notification.getTitle()) : Component.literal(
                    notification.getTitle()).setStyle(Style.EMPTY.withFont(textFont));
            Component message = textFont == null ? Component.literal(notification.getMessage()) : Component.literal(
                    notification.getMessage()).setStyle(Style.EMPTY.withFont(textFont));

            float scaledTitleWidth = font.width(title) * TITLE_SCALE;
            float scaledMessageWidth = font.width(message) * MESSAGE_SCALE;
            float maxTextWidth = Math.max(scaledTitleWidth, scaledMessageWidth);

            int width = (int) ((maxTextWidth + 20) * animationScale);
            int height = (int) (40 * animationScale);
            int x = windowWidth - width - 10;

            return new RenderGeometry(x, currentY, width, height, title, message);
        }

        public void forceClose() {
            if (!this.closing) {
                this.closing = true;
                this.closeStartTime = System.currentTimeMillis();
            }
        }

        public boolean isFullyExpired() {
            if (System.currentTimeMillis() > expiryTime && !closing) {
                this.closing = true;
                this.closeStartTime = System.currentTimeMillis();
            }
            if (closing) {
                return System.currentTimeMillis() - closeStartTime >= ANIMATION_DURATION;
            }
            return false;
        }

        public float getProgress() {
            long timeLeft = this.expiryTime - System.currentTimeMillis();
            float progress = (float) timeLeft / this.notification.getDuration().toMillis();
            return Math.clamp(progress, 0f, 1f);
        }
    }

    private record RenderGeometry(int x, int y, int width, int height, Component title, Component message) {
    }
}