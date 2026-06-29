package ru.fozeton.diamondchat.utils.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum NotificationType {
    INFO("ИНФОРМАЦИЯ"),
    SUCCESS("УСПЕХ"),
    ERROR("ОШИБКА"),
    WARN("ВНИМАНИЕ");

    @Getter
    private final String typeName;
}