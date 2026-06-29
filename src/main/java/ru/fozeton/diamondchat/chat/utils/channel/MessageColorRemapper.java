package ru.fozeton.diamondchat.chat.utils.channel;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

import java.util.Map;

public class MessageColorRemapper {
    @Getter
    private static final MessageColorRemapper instance = new MessageColorRemapper();

    private static final float CUSTOM_SATURATION = 0.65f;
    private static final float CUSTOM_BRIGHTNESS = 0.75f;

    private static final Map<Integer, Integer> VANILLA_REMAP = Map.ofEntries(
            Map.entry(0x000000, 0xFF2C2C3A),
            Map.entry(0x0000AA, 0xFF3A4A8A),
            Map.entry(0x00AA00, 0xFF1D7A52),
            Map.entry(0x00AAAA, 0xFF2A8A8A),
            Map.entry(0xAA0000, 0xFF8A2030),
            Map.entry(0xAA00AA, 0xFF5A3A8A),
            Map.entry(0xFFAA00, 0xFFC4A020),
            Map.entry(0xAAAAAA, 0xFF6B6B88),
            Map.entry(0x555555, 0xFF4A4A62),
            Map.entry(0x5555FF, 0xFF6A6AE0),
            Map.entry(0x55FF55, 0xFF2AAA72),
            Map.entry(0x55FFFF, 0xFF7ABFCC),
            Map.entry(0xFF5555, 0xFFC04060),
            Map.entry(0xFF55FF, 0xFFB07ACC),
            Map.entry(0xFFFF55, 0xFFCFB84A),
            Map.entry(0xFFFFFF, 0xFFD4D0F5)
    );

    public Component remap(Component original) {
        return remapRecursive(original.copy());
    }

    private MutableComponent remapRecursive(MutableComponent text) {
        Style style = text.getStyle();

        if (style.getColor() != null) {
            int rgb = style.getColor().getValue();

            int newRgb;
            Integer vanilla = VANILLA_REMAP.get(rgb);
            if (vanilla != null) newRgb = vanilla & 0xFFFFFF;
            else newRgb = muteColor(rgb);

            text.setStyle(style.withColor(
                    TextColor.fromRgb(newRgb)
            ));
        }

        text.getSiblings().replaceAll(text1 -> remapRecursive(text1.copy()));

        return text;
    }

    private int muteColor(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        float[] hsb = rgbToHsb(r, g, b);

        hsb[1] *= CUSTOM_SATURATION;
        hsb[2] *= CUSTOM_BRIGHTNESS;

        hsb[2] = Math.max(hsb[2], 0.45f);

        return hsbToRgb(hsb[0], hsb[1], hsb[2]);
    }

    private float[] rgbToHsb(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float h = 0;
        float s = 0;

        if (delta != 0) {
            s = delta / max;
            if (max == r) h = ((g - b) / delta) % 6;
            else if (max == g) h = (b - r) / delta + 2;
            else h = (r - g) / delta + 4;
            h /= 6;
            if (h < 0) h += 1;
        }

        return new float[]{h, s, max};
    }

    private int hsbToRgb(float h, float s, float b) {
        float r, g, bl;
        if (s == 0) {
            r = g = bl = b;
        } else {
            float i = (float) Math.floor(h * 6);
            float f = h * 6 - i;
            float p = b * (1 - s);
            float q = b * (1 - f * s);
            float t = b * (1 - (1 - f) * s);
            switch ((int) i % 6) {
                case 0 -> {
                    r = b;
                    g = t;
                    bl = p;
                }
                case 1 -> {
                    r = q;
                    g = b;
                    bl = p;
                }
                case 2 -> {
                    r = p;
                    g = b;
                    bl = t;
                }
                case 3 -> {
                    r = p;
                    g = q;
                    bl = b;
                }
                case 4 -> {
                    r = t;
                    g = p;
                    bl = b;
                }
                default -> {
                    r = b;
                    g = p;
                    bl = q;
                }
            }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (bl * 255);
    }

    public int applyAlpha(int argb, int alpha) {
        return (argb & 0x00FFFFFF) | (alpha << 24);
    }
}