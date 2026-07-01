package ru.fozeton.diamondchat.configs;

import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
public class ColorRemapperConfig implements Config<ColorRemapperConfig> {
    private float saturation = 0.65f;
    private float brightness = 0.75f;
    private Map<String, String> remaps = new LinkedHashMap<>();

    @Override
    public ColorRemapperConfig createDefault() {
        ColorRemapperConfig config = new ColorRemapperConfig();
        Map<String, String> map = config.getRemaps();

        map.put("0x000000", "0xFF2C2C3A");
        map.put("0x0000AA", "0xFF3A4A8A");
        map.put("0x00AA00", "0xFF1D7A52");
        map.put("0x00AAAA", "0xFF2A8A8A");
        map.put("0xAA0000", "0xFF8A2030");
        map.put("0xAA00AA", "0xFF5A3A8A");
        map.put("0xFFAA00", "0xFFC4A020");
        map.put("0xAAAAAA", "0xFF6B6B88");
        map.put("0x555555", "0xFF4A4A62");
        map.put("0x5555FF", "0xFF6A6AE0");
        map.put("0x55FF55", "0xFF2AAA72");
        map.put("0x55FFFF", "0xFF7ABFCC");
        map.put("0xFF5555", "0xFFC04060");
        map.put("0xFF55FF", "0xFFB07ACC");
        map.put("0xFFFF55", "0xFFCFB84A");
        map.put("0xFFFFFF", "0xFFD4D0F5");

        return config;
    }

    @Override
    public boolean isEmpty(ColorRemapperConfig config) {
        return config == null || config.getRemaps() == null || config.getRemaps().isEmpty();
    }

    public Integer getRemapColor(int rgb) {
        String key = String.format("0x%06X", rgb);
        String value = getRemaps().get(key);
        if (value == null) return null;
        return Long.decode(value).intValue();
    }
}
