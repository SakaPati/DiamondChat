package ru.fozeton.diamondchat.configs;

import lombok.Getter;
import lombok.Setter;
import me.shedaniel.autoconfig.annotation.ConfigEntry;
import ru.fozeton.chatmanager.config.IConfig;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class LevelsConfig implements IConfig {
    @ConfigEntry.Gui.Excluded
    private Map<String, LevelData> levels = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class LevelData {
        private long money;
        private long blocks;
    }
}