package ru.fozeton.diamondchat.chat.utils;

import com.mojang.blaze3d.platform.NativeImage;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.module.Gif;
import ru.fozeton.chatmanager.module.gif.McAnimCodec;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GifTexture {
    private static final Map<String, GifTexture> CACHE = new HashMap<>();
    private final McAnimCodec.Animation animation;
    private final Gif gif = Gif.getInstance();
    private final Identifier location;
    private final DynamicTexture texture;
    private final NativeImage image;
    @Getter
    private final int width;
    @Getter
    private final int height;
    private final int totalDuration;
    private int currentFrameIndex = -1;
    private long animationStartTime = 0;

    private GifTexture(String gifId) {
        this.animation = loadOrDownloadAnimation(gifId);
        this.width = animation.getWidth();
        this.height = animation.getHeight();

        this.image = new NativeImage(width, height, false);
        this.texture = new DynamicTexture(() -> "diamondchat_voice", image);
        this.location = Identifier.fromNamespaceAndPath("diamondchat", "dynamic/" + gifId);

        Minecraft.getInstance().getTextureManager().register(location, texture);

        this.totalDuration = calculateTotalDuration(animation);
    }

    private McAnimCodec.Animation loadOrDownloadAnimation(String gifId) {
        try {
            return readAnimation(gifId);
        } catch (Exception firstAttempt) {
            if (Boolean.FALSE.equals(gif.download(gifId).join())) {
                throw new RuntimeException("Failed to download gif: " + gifId, firstAttempt);
            }
            try {
                return readAnimation(gifId);
            } catch (Exception afterDownload) {
                throw new RuntimeException("Failed to read gif: " + gifId + " after download", afterDownload);
            }
        }
    }

    private McAnimCodec.Animation readAnimation(String gifId) throws Exception {
        Path path = ChatManagerCore.getConfigDir().resolve("cache").resolve(gifId + ".mcanim");
        return McAnimCodec.read(path);
    }

    private int calculateTotalDuration(McAnimCodec.Animation animation) {
        List<McAnimCodec.Frame> frames = animation.getFrames();
        return frames.isEmpty() ? 1000 : frames.getLast().timestamp();
    }

    public static GifTexture get(String gifId) {
        return CACHE.computeIfAbsent(gifId, GifTexture::new);
    }

    public void render(GuiGraphicsExtractor guiGraphics, int x, int y, float alpha) {
        List<McAnimCodec.Frame> frames = animation.getFrames();
        if (frames == null || frames.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (animationStartTime == 0) animationStartTime = currentTime;

        long elapsedTime = (currentTime - animationStartTime) % totalDuration;

        int targetFrameIndex = resolveFrameIndex(frames, elapsedTime);
        if (targetFrameIndex != currentFrameIndex) {
            currentFrameIndex = targetFrameIndex;
            uploadBytes(frames.get(currentFrameIndex).rgbaData());
        }

        int displayWidth = width / 2;
        int displayHeight = height / 2;

        int alphaInt = (int) (alpha * 255.0f) & 0xFF;
        int color = (alphaInt << 24) | 0x00FFFFFF;

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.location,
                x, y - displayHeight,
                0.0F, 0.0F,
                displayWidth, displayHeight,
                displayWidth,
                displayHeight,
                color
        );
    }

    private int resolveFrameIndex(List<McAnimCodec.Frame> frames, long elapsedTime) {
        int targetFrameIndex = 0;
        for (int i = 0; i < frames.size(); i++) {
            if (frames.get(i).timestamp() <= elapsedTime) targetFrameIndex = i;
            else break;
        }
        return targetFrameIndex;
    }

    private void uploadBytes(byte[] rgbaBytes) {
        int animationWidth = animation.getWidth();
        int animationHeight = animation.getHeight();
        if (rgbaBytes.length < animationWidth * animationHeight * 4) return;

        for (int y = 0; y < animationHeight; y++) {
            for (int x = 0; x < animationWidth; x++) {
                int index = (y * animationWidth + x) * 4;

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