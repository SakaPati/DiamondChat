package ru.fozeton.diamondchat.configs;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class LevelsConfig {
    private Map<String, LevelData> levels = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class LevelData {
        private long money;
        private long blocks;
    }
}