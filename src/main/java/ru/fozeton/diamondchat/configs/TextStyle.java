package ru.fozeton.diamondchat.configs;

import ru.fozeton.chatmanager.config.AiStyleTextConfig;

public class TextStyle extends AiStyleTextConfig {

    @Override
    public AiStyleTextConfig createDefault() {
        AiStyleTextConfig config = new AiStyleTextConfig();

        AiStyle gramotey = new AiStyle();
        gramotey.setModel("llama-3.3-70b-versatile");
        gramotey.setMaxTokens(80);
        gramotey.getMessages().add(Message.builder().role("system").content("Исправь в моём тексте орфографию, пунктуацию и тся/ться. Смысл, слова и структуру не меняй — только исправь ошибки. Отвечай ТОЛЬКО исправленным текстом, без пояснений.").build());

        config.getStyles().put("Грамотей", gramotey);

        return config;
    }
}
