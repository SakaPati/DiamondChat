package ru.fozeton.diamondchat.chat.utils;

import com.mojang.blaze3d.platform.NativeImage;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.module.gif.McAnimCodec;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GifTexture {
    private static final Map<String, GifTexture> CACHE = new HashMap<>();
    private final McAnimCodec.Animation animation;
    private final ResourceLocation location;
    private final DynamicTexture texture;
    private final NativeImage image;
    @Getter
    private final int width, height;
    private final int totalDuration;
    private int currentFrameIndex = -1;
    private long animationStartTime = 0;

    private GifTexture(String gifId) {
        try {
            this.animation = McAnimCodec.read(ChatManagerCore.CONFIG_DIR.resolve("cache").resolve(gifId + ".mcanim"));
            this.width = animation.getWidth();
            this.height = animation.getHeight();

            this.image = new NativeImage(width, height, false);
            this.texture = new DynamicTexture(image);
            this.location = ResourceLocation.fromNamespaceAndPath("diamondchat", "dynamic/" + gifId);

            Minecraft.getInstance().getTextureManager().register(location, texture);

            List<McAnimCodec.Frame> frames = animation.getFrames();
            if (!frames.isEmpty()) this.totalDuration = frames.getLast().timestamp();
            else this.totalDuration = 1000;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static GifTexture get(String gifId) {
        return CACHE.computeIfAbsent(gifId, GifTexture::new);
    }

    public void render(GuiGraphics guiGraphics, int x, int y) {
        List<McAnimCodec.Frame> frames = animation.getFrames();
        if (frames == null || frames.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (animationStartTime == 0) animationStartTime = currentTime;

        long elapsedTime = (currentTime - animationStartTime) % totalDuration;

        int targetFrameIndex = 0;
        for (int i = 0; i < frames.size(); i++) {
            if (frames.get(i).timestamp() <= elapsedTime) targetFrameIndex = i;
            else break;
        }

        if (targetFrameIndex != currentFrameIndex) {
            currentFrameIndex = targetFrameIndex;
            uploadBytes(frames.get(currentFrameIndex).rgbaData());
        }

        int displayWidth = width / 2;
        int displayHeight = height / 2;

        guiGraphics.blit(
                RenderType::guiTextured,
                this.location,
                x, y - displayHeight,
                0.0F, 0.0F,
                displayWidth, displayHeight,
                displayWidth,
                displayHeight
        );
    }

    private void uploadBytes(byte[] rgbaBytes) {
        int width = animation.getWidth();
        int height = animation.getHeight();
        if (rgbaBytes.length < width * height * 4) return;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int index = (y * width + x) * 4;

                int r = rgbaBytes[index] & 0xFF;
                int g = rgbaBytes[index + 1] & 0xFF;
                int b = rgbaBytes[index + 2] & 0xFF;
                int a = rgbaBytes[index + 3] & 0xFF;

                int color = (a << 24) | (r << 16) | (g << 8) | b;
                this.image.setPixel(x, y, color);
            }
        }

        this.texture.upload();
    }
}
