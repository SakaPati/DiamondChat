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
import net.minecraft.resources.ResourceLocation;
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

    private static final float titleScale = 1.6f;
    private static final float messageScale = 1.4f;
    private static final long ANIMATION_DURATION = 150;

    private final List<ActiveNotification> activeNotifications = new ArrayList<>();

    public NotificationManager() {
        super();
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenEvents.afterRender(screen).register((s, drawContext, mouseX, mouseY, tickDelta) -> {
                renderNotifications(drawContext, client);
            });
        });

        HudLayerRegistrationCallback.EVENT.register(layeredDrawer ->
                layeredDrawer.attachLayerAfter(
                        IdentifiedLayer.SUBTITLES,
                        ResourceLocation.fromNamespaceAndPath("moderation", "notification"),
                        (drawContext, tickCounter) -> {
                            if (Minecraft.getInstance().screen == null) {
                                renderNotifications(drawContext, Minecraft.getInstance());
                            }
                        }
                ));
        DiamondChat.EVENT_BUS.register(this);
    }

    private void renderNotifications(GuiGraphics GuiGraphics, Minecraft client) {
        activeNotifications.removeIf(ActiveNotification::isFullyExpired);
        if (activeNotifications.isEmpty()) return;

        Font Font = client.font;

        int windowWidth = GuiGraphics.guiWidth();
        int currentY = 10;
        int spacing = 5;

        double mouseX = client.mouseHandler.xpos() * (double) windowWidth / (double) client.getWindow().getWidth();
        double mouseY = client.mouseHandler.ypos() * (double) GuiGraphics.guiHeight() / (double) client.getWindow().getHeight();

        GuiGraphics.pose().popPose();
        GuiGraphics.pose().translate(0, 0, 5000f);

        for (ActiveNotification active : activeNotifications) {
            float animationScale = getAnimationScale(active);

            RenderGeometry geo = active.calculateGeometry(Font, animationScale, windowWidth, currentY);

            boolean isHovered = mouseX >= geo.x() && mouseX <= (geo.x() + geo.width()) && mouseY >= geo.y() && mouseY <= (geo.y() + geo.height());
            int backgroundColor = (isHovered && active.action != null) ? 0xFF1e1e2a : 0xFF18181f;
            int borderColor = (isHovered && active.action != null) ? 0xFF7c6ef5 : 0xFF3a2e6e;
            int accentColor;
            int titleColor;
            int messageColor = 0xFF6b6b88;

            switch (active.type) {
                case SUCCESS -> {
                    accentColor = (isHovered && active.action != null) ? 0xFF34d68e : 0xFF2aaa72;
                    titleColor = (isHovered && active.action != null) ? 0xFF34d68e : 0xFF2aaa72;
                }
                case ERROR -> {
                    accentColor = (isHovered && active.action != null) ? 0xFFe84d78 : 0xFFc04060;
                    titleColor = (isHovered && active.action != null) ? 0xFFe84d78 : 0xFFc04060;
                }
                case WARN -> {
                    accentColor = (isHovered && active.action != null) ? 0xFFf0c830 : 0xFFc4a020;
                    titleColor = (isHovered && active.action != null) ? 0xFFf0c830 : 0xFFc4a020;
                }
                default -> {
                    accentColor = (isHovered && active.action != null) ? 0xFFa78bfa : 0xFF7c6ef5;
                    titleColor = (isHovered && active.action != null) ? 0xFFede9fe : 0xFFd4d0f5;
                }
            }

            GuiGraphics.fill(geo.x(), geo.y(), geo.x() + geo.width(), geo.y() + geo.height(), backgroundColor);
            GuiGraphics.renderOutline(geo.x(), geo.y(), geo.width(), geo.height(), borderColor);
            GuiGraphics.fill(geo.x() + 1, geo.y(), geo.x() - 2, geo.y() + geo.height(), accentColor);

            GuiGraphics.pose().popPose();
            GuiGraphics.pose().translate(geo.x(), geo.y(), 0);
            GuiGraphics.pose().scale(titleScale * animationScale, titleScale * animationScale, 1.0f);
            GuiGraphics.drawString(Font, geo.title(), 5, 2, titleColor, false);
            GuiGraphics.pose().popPose();

            GuiGraphics.pose().popPose();
            GuiGraphics.pose().translate(geo.x(), geo.y(), 0);
            GuiGraphics.pose().scale(messageScale * animationScale, messageScale * animationScale, 1.0f);
            GuiGraphics.drawString(Font, geo.message(), 8, 15, messageColor, false);
            GuiGraphics.pose().popPose();

            float progress = active.getProgress();
            int progressWidth = (int) (geo.width() * progress);
            GuiGraphics.fill(geo.x(), geo.y() + geo.height(), geo.x() + progressWidth, geo.y() + geo.height() - 1, accentColor);

            if (geo.height() > 0) currentY += geo.height() + spacing;
        }

        GuiGraphics.pose().popPose();
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
        return Math.max(0.0f, Math.min(1.0f, progress));
    }

    @EventSubscriber(event = NotificationEvent.class)
    public void onNotificationReceived(NotificationEvent event) {
        activeNotifications.add(new ActiveNotification(event.getNotification()));
    }

    @EventSubscriber(event = InputEvent.MouseInputEvent.class)
    public void onNotificationClick(InputEvent.MouseInputEvent event) {
        if (event.getButton() != 0 || event.getAction() != 1) return;
        if (activeNotifications.isEmpty()) return;
        Minecraft client = Minecraft.getInstance();

        Font Font = client.font;
        int windowWidth = client.getWindow().getGuiScaledWidth();
        int windowHeight = client.getWindow().getGuiScaledHeight();

        double mouseX = client.mouseHandler.xpos() * (double) windowWidth / (double) client.getWindow().getWidth();
        double mouseY = client.mouseHandler.ypos() * (double) windowHeight / (double) client.getWindow().getHeight();

        int currentY = 10;
        int spacing = 5;

        for (ActiveNotification active : activeNotifications) {
            if (active.closing || active.action == null) continue;

            float animationScale = getAnimationScale(active);
            RenderGeometry geo = active.calculateGeometry(Font, animationScale, windowWidth, currentY);
            if (mouseX >= geo.x() && mouseX <= (geo.x() + geo.width()) && mouseY >= geo.y() && mouseY <= (geo.y() + geo.height())) {
                Notification notification = active.notification;
                if (notification.getAction() != null) {
                    client.execute(notification.getAction());
                }
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

        private boolean closing = false;
        private long closeStartTime;

        public ActiveNotification(Notification notification) {
            this.notification = notification;
            this.startTime = System.currentTimeMillis();
            this.expiryTime = this.startTime + notification.getDuration().toMillis();
            this.type = notification.getType();
            this.action = notification.getAction();
        }

        public RenderGeometry calculateGeometry(
                Font Font,
                float animationScale,
                int windowWidth,
                int currentY
        ) {
            Component title = Component.literal(notification.getTitle());
            Component message = Component.literal(notification.getMessage());

            float scaledTitleWidth = Font.width(title) * titleScale;
            float scaledMessageWidth = Font.width(message) * messageScale;
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
            return Math.max(0.0f, Math.min(1.0f, progress));
        }
    }

    private record RenderGeometry(int x, int y, int width, int height, Component title, Component message) {
    }
}