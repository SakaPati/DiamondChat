package ru.fozeton.diamondchat.chat.utils.channel;

import lombok.Getter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.diamondchat.configs.ColorRemapperConfig;
import ru.fozeton.diamondchat.configs.ConfigLoader;

import java.util.Map;

public class MessageColorRemapper {
    @Getter
    private static final MessageColorRemapper instance = new MessageColorRemapper();
    private final ColorRemapperConfig remapperConfig = ConfigLoader.getInstance().getRemapperConfig();

    public Component remap(Component original) {
        return remapRecursive(original.copy());
    }

    private MutableComponent remapRecursive(MutableComponent text) {
        Style style = text.getStyle();

        if (style.getColor() != null) {
            int rgb = style.getColor().getValue();

            Integer vanilla = remapperConfig.getRemapColor(rgb);
            int newRgb = (vanilla != null) ? (vanilla & 0xFFFFFF) : muteColor(rgb);

            text.setStyle(style.withColor(TextColor.fromRgb(newRgb)));
        }

        text.getSiblings().replaceAll(text1 -> remapRecursive(text1.copy()));

        return text;
    }

    private int muteColor(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        float[] hsb = rgbToHsb(r, g, b);

        hsb[1] *= remapperConfig.getSaturation();
        hsb[2] *= remapperConfig.getBrightness();

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

    public int applyAlpha(int argb, float fadeAlpha) {
        int nativeAlpha = (argb >> 24) & 0xFF;
        if (nativeAlpha == 0 && (argb & 0xFF000000) == 0) {
            nativeAlpha = 0xFF;
        }

        int finalAlpha = (int) (nativeAlpha * fadeAlpha) & 0xFF;

        return (argb & 0x00FFFFFF) | (finalAlpha << 24);
    }
}