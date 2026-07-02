package ru.fozeton.diamondchat.configs;

import ru.fozeton.chatmanager.config.AiStyleTextConfig;
import ru.fozeton.chatmanager.config.defaults.AiStyleTextDefault;

public class AiStyleTextModify extends AiStyleTextDefault {
    @Override
    public AiStyleTextConfig createDefault() {
        AiStyleTextConfig config = new AiStyleTextConfig();

        AiStyleTextConfig.AiStyle literate = new AiStyleTextConfig.AiStyle();
        literate.setModel("llama-3.3-70b-versatile");
        literate.setMaxTokens(80);
        literate.getMessages().add(AiStyleTextConfig.Message.builder().role("system").content("Исправь в моём тексте орфографию, пунктуацию и тся/ться. Смысл, слова и структуру не меняй — только исправь ошибки. Отвечай ТОЛЬКО исправленным текстом, без пояснений.").build());

        config.getStyles().put("Грамотей", literate);

        return config;
    }
}
