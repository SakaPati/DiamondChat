package ru.fozeton.diamondchat.configs;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class LevelsConfig {
    private Map<String, LevelData> levels = new HashMap<>();

    @Getter
    @Setter
    public static class LevelData {
        private long money;
        private long blocks;
    }
}