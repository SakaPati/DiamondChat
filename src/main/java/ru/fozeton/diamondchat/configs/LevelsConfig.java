package ru.fozeton.diamondchat.configs;

import lombok.Getter;
import lombok.Setter;
import ru.fozeton.chatmanager.config.IConfig;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class LevelsConfig implements IConfig {
    private Map<String, LevelData> levels = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class LevelData {
        private long money;
        private long blocks;
    }
}