package ru.fozeton.diamondchat.chat.utils;

import com.mojang.blaze3d.platform.NativeImage;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.module.Gif;
import ru.fozeton.chatmanager.module.gif.McAnimCodec;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class GifTexture {
    private static final Map<String, GifTexture> CACHE = new HashMap<>();
    private static final AtomicInteger ID_COUNTER = new AtomicInteger();
    private static int pickerCounter = 0;
    private final Identifier location;
    private final AtomicBoolean closed = new AtomicBoolean();
    private DynamicTexture texture;
    private NativeImage image;
    private McAnimCodec.Animation animation;
    @Getter
    private int width = 16;
    @Getter
    private int height = 16;
    @Getter
    private boolean failed;
    private int totalDuration = 1000;
    private int currentFrameIndex = -1;
    private long animationStartTime = 0;

    public GifTexture(String gifId) {
        this.location = Identifier.fromNamespaceAndPath("diamondchat", "dynamic/gif_" + ID_COUNTER.getAndIncrement());
        loadOrDownloadAnimation(gifId).whenComplete((loaded, ex) -> Minecraft.getInstance().execute(() -> {
            if (closed.get()) return;
            if (ex != null || loaded == null) {
                failed = true;
                return;
            }
            try {
                init(loaded);
            } catch (Exception _) {
                failed = true;
            }
        }));
    }

    private GifTexture(McAnimCodec.Animation animation, String path) {
        this.location = Identifier.fromNamespaceAndPath("diamondchat", path);
        init(animation);
    }

    private static CompletableFuture<McAnimCodec.Animation> loadOrDownloadAnimation(String gifId) {
        try {
            return CompletableFuture.completedFuture(readAnimation(gifId));
        } catch (Exception _) {
            return Gif.getInstance().download(gifId)
                    .thenApply(ok -> {
                        if (!Boolean.TRUE.equals(ok)) {
                            throw new RuntimeException("Failed to download gif: " + gifId);
                        }
                        try {
                            return readAnimation(gifId);
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to read gif: " + gifId + " after download", e);
                        }
                    });
        }
    }

    private static McAnimCodec.Animation readAnimation(String gifId) throws Exception {
        Path path = ChatManagerCore.getConfigDir().resolve("cache").resolve(gifId + ".mcanim");
        return McAnimCodec.read(path);
    }

    private static int calculateTotalDuration(McAnimCodec.Animation animation) {
        List<McAnimCodec.Frame> frames = animation.getFrames();
        return frames.isEmpty() ? 1000 : Math.max(1, frames.getLast().timestamp());
    }

    public static GifTexture get(String gifId) {
        return CACHE.computeIfAbsent(gifId, GifTexture::new);
    }

    public static GifTexture standalone(McAnimCodec.Animation animation) {
        return new GifTexture(animation, "dynamic/picker/" + (pickerCounter++));
    }

    private void init(McAnimCodec.Animation loaded) {
        this.width = loaded.getWidth();
        this.height = loaded.getHeight();
        this.totalDuration = calculateTotalDuration(loaded);

        this.image = new NativeImage(width, height, false);
        this.texture = new DynamicTexture(() -> "diamondchat_gif", image);
        Minecraft.getInstance().getTextureManager().register(location, texture);

        this.animation = loaded;
    }

    public boolean isReady() {
        return animation != null;
    }

    public void close() {
        closed.set(true);
        if (texture != null) Minecraft.getInstance().getTextureManager().release(location);
    }

    public void render(GuiGraphicsExtractor guiGraphics, int x, int y, float alpha) {
        render(guiGraphics, x, y - height / 2, width / 2, height / 2, alpha);
    }

    public void render(GuiGraphicsExtractor guiGraphics, int x, int y, int w, int h, float alpha) {
        McAnimCodec.Animation anim = this.animation;
        if (anim == null) return;

        List<McAnimCodec.Frame> frames = anim.getFrames();
        if (frames == null || frames.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        if (animationStartTime == 0) animationStartTime = currentTime;

        long elapsedTime = (currentTime - animationStartTime) % totalDuration;

        int targetFrameIndex = resolveFrameIndex(frames, elapsedTime);
        if (targetFrameIndex != currentFrameIndex) {
            currentFrameIndex = targetFrameIndex;
            uploadBytes(anim, frames.get(currentFrameIndex).rgbaData());
        }

        int alphaInt = (int) (alpha * 255.0f) & 0xFF;
        int color = (alphaInt << 24) | 0x00FFFFFF;

        guiGraphics.blit(
                RenderPipelines.GUI_TEXTURED,
                this.location,
                x, y,
                0.0F, 0.0F,
                w, h,
                w, h,
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

    private void uploadBytes(McAnimCodec.Animation anim, byte[] rgbaBytes) {
        int animationWidth = anim.getWidth();
        int animationHeight = anim.getHeight();
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