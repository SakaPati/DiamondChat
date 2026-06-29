package ru.fozeton.diamondchat.utils.notify;

import lombok.Builder;
import lombok.Getter;
import ru.fozeton.diamondchat.DiamondChat;
import ru.fozeton.diamondchat.events.game.NotificationEvent;
import ru.fozeton.diamondchat.utils.enums.NotificationType;

import java.time.Duration;
import java.util.Objects;

@Getter
@Builder
public class Notification {
    private final String title;
    private final String message;
    private final Runnable action;
    @Builder.Default
    private final Duration duration = Duration.ofSeconds(5);
    @Builder.Default
    private final NotificationType type = NotificationType.INFO;

    private Notification(String title, String message, Runnable action, Duration duration, NotificationType type) {
        this.title = title == null ? type.getTypeName() : title;
        this.message = Objects.requireNonNull(message, "Message cannot be null");
        this.action = action;
        this.duration = duration;
        this.type = type;
    }

    public static void showInfo(String message) {
        showNotification(Notification.builder()
                .type(NotificationType.INFO)
                .message(message)
                .build());
    }

    public static void showSuccess(String message) {
        showNotification(Notification.builder()
                .type(NotificationType.SUCCESS)
                .message(message)
                .build());
    }

    public static void showError(String message) {
        showNotification(Notification.builder()
                .type(NotificationType.ERROR)
                .message(message)
                .build());
    }

    public static void showWarn(String message) {
        showNotification(Notification.builder()
                .type(NotificationType.WARN)
                .message(message)
                .build());
    }

    public static void showCustom(Notification notification) {
        showNotification(notification);
    }

    private static void showNotification(Notification notification) {
        DiamondChat.EVENT_BUS.activate(new NotificationEvent(notification));
    }


}