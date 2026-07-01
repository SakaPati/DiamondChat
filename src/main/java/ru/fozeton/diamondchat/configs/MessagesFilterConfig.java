package ru.fozeton.diamondchat.configs;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.LinkedList;
import java.util.List;

@Getter
@Setter
public class MessagesFilterConfig implements Config<MessagesFilterConfig>{
    private final List<MessageFilter> filters = new LinkedList<>();

    @Override
    public MessagesFilterConfig createDefault() {
        MessagesFilterConfig config = new MessagesFilterConfig();

        MessageFilter plainTextFilter = new MessageFilter(
                "test",
                "0xFF55FF55",
                "0xFF55FF55"
        );

        MessageFilter regexFilter = new MessageFilter(
                "^(Тест|Test)",
                "0xFFFF5555",
                "0xFFFF5555"
        );

        MessageFilter advancedFilter = new MessageFilter(
                "Ⓖ Fozeton\\[\\d+\\]: .*",
                "0xFF7C6EF5",
                "0xFF7C6EF5"
        );

        config.getFilters().add(plainTextFilter);
        config.getFilters().add(regexFilter);
        config.getFilters().add(advancedFilter);

        return config;
    }

    @Override
    public boolean isEmpty(MessagesFilterConfig config) {
        return config == null || config.getFilters() == null || config.getFilters().isEmpty();
    }

    @Getter
    @Setter
    @RequiredArgsConstructor
    public static class MessageFilter {
        private final String pattern;
        private final String borderColor;
        private final String lineColor;
    }
}