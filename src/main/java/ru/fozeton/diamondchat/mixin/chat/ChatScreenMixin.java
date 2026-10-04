package ru.fozeton.diamondchat.mixin.chat;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.config.AiStyleTextConfig;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.config.MacrosConfig;
import ru.fozeton.chatmanager.events.channel.ChannelVisibilityChangedEvent;
import ru.fozeton.chatmanager.events.game.StylizeMessageEvent;
import ru.fozeton.chatmanager.events.speech.VoskPartialResultEvent;
import ru.fozeton.chatmanager.module.SpeechToText;
import ru.fozeton.chatmanager.utils.stt.VoiceIndicator;
import ru.fozeton.diamondchat.gui.CalculatorScreen;
import ru.fozeton.diamondchat.gui.MacrosScreen;
import ru.fozeton.diamondchat.gui.widgets.DropdownButtonWidget;
import ru.fozeton.diamondchat.gui.widgets.GifPickerWidget;
import ru.fozeton.diamondchat.gui.widgets.TabButtonWidget;

import java.io.IOException;
import java.util.Map;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen {
    @Unique
    private static final int GIF_ICON_W = 24;
    @Unique
    private static final int GIF_ICON_H = 10;

    @Unique
    private static final Identifier MIC_TEX = Identifier.fromNamespaceAndPath("diamondchat", "textures/gui/mic.png");
    @Unique
    private static final Identifier MIC_ACTIVE_TEX = Identifier.fromNamespaceAndPath("diamondchat", "textures/gui/mic_active.png");
    @Unique
    private static final int MIC_SIZE = 12;

    @Unique
    private final AiStyleTextConfig styleTextConfig = ChatConfigManager.getInstance().getTextStyleConfig();
    @Unique
    private final MacrosConfig config = ChatConfigManager.getInstance().getMacrosConfig();
    @Shadow
    protected EditBox input;
    @Unique
    private TabButtonWidget macrosButton;

    @Unique
    private TabButtonWidget calculatorButton;

    @Unique
    private DropdownButtonWidget aiStyleText;

    @Unique
    private DropdownButtonWidget chats;

    @Unique
    private GifPickerWidget gifPicker;

    @Unique
    private int inputWidth;

    @Unique
    private int micX;
    @Unique
    private int micY;
    @Unique
    private volatile boolean micRecording = false;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        int y = this.height - 30;
        int h = 14;
        int x = 2;
        int gap = 54;

        Minecraft instance = Minecraft.getInstance();
        this.macrosButton = new TabButtonWidget(
                x, y, 50, h,
                Component.literal("Macros"),
                () -> {
                    config.setEnabled(!config.isEnabled());
                    ChatConfigManager.getInstance().save(config.getClass());
                    if (this.macrosButton != null)
                        this.macrosButton.setActive(config.isEnabled());
                },
                () -> instance.setScreenAndShow(new MacrosScreen())
        );
        this.macrosButton.setActive(config.isEnabled());

        this.chats = new DropdownButtonWidget(x + gap, y, 50, h, Component.literal("Чаты"));

        ChannelsConfig channelsConfig = ChatConfigManager.getInstance().getChannelsConfig();

        for (String channelId : ChatManagerCore.getChannels().keySet()) {
            ChannelsConfig.ChannelSettings channelSettings = channelsConfig.getChannels().get(channelId);
            if (channelSettings == null) continue;

            this.chats.addItem(
                    channelId, () -> {
                        boolean visibility = !channelSettings.isVisible();
                        ChatManagerCore.EVENT_BUS.activate(new ChannelVisibilityChangedEvent(channelId, visibility));
                    }, channelSettings::isVisible
            );
        }

        this.calculatorButton = new TabButtonWidget(
                x + gap * 2, y, 80, h,
                Component.literal("Калькулятор"),
                () -> instance.setScreenAndShow(new CalculatorScreen()),
                null
        );

        this.aiStyleText = new DropdownButtonWidget(
                instance.getWindow().getGuiScaledWidth() - 102, y, 100, h,
                Component.literal("Стиль текста")
        );

        for (Map.Entry<String, AiStyleTextConfig.AiStyle> entry : styleTextConfig.getStyles().entrySet()) {
            this.aiStyleText.addItem(
                    entry.getKey(),
                    () -> ChatManagerCore.EVENT_BUS.activate(new StylizeMessageEvent(entry.getKey()))
            );
        }

        if (this.gifPicker != null) this.gifPicker.close();
        this.gifPicker = new GifPickerWidget(
                this.width - GIF_ICON_W - 2, this.height - 13, GIF_ICON_W, GIF_ICON_H,
                this::chatmanager$appendGif
        );

        this.micX = this.width - GIF_ICON_W - 2 - MIC_SIZE - 2;
        this.micY = this.height - 14;
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void chatmanager_core$registerBus(CallbackInfo ci) {
        ChatManagerCore.EVENT_BUS.register(this);
        int width = this.input.getWidth() - GIF_ICON_W - 2 - MIC_SIZE - 2;
        this.inputWidth = width;
        this.input.setWidth(width - 5);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void chatmanager_core$unregisterBus(CallbackInfo ci) {
        ChatManagerCore.EVENT_BUS.unregister(this);
        VoiceIndicator.finished();
        if (this.gifPicker != null) {
            this.gifPicker.close();
            this.gifPicker = null;
        }
    }

    @Unique
    private void chatmanager$appendGif(String id) {
        String value = this.input.getValue();
        String sep = value.isEmpty() || value.endsWith(" ") ? "" : " ";
        this.input.setValue(value + sep + ":" + id + ":");
    }

    @Unique
    private void chatmanager$syncGifFocus() {
        if (this.gifPicker == null) return;
        EditBox box = this.gifPicker.getSearchBox();
        if (box.isFocused()) {
            if (this.getFocused() != box) this.setFocused(box);
        } else if (this.getFocused() == box) {
            this.setFocused(this.input);
        }
    }

    @Unique
    private boolean chatmanager$isOverMic(double mouseX, double mouseY) {
        return mouseX >= micX && mouseX < micX + MIC_SIZE
               && mouseY >= micY && mouseY < micY + MIC_SIZE;
    }

    @Unique
    private void chatmanager$startVoice() {
        if (!VoiceIndicator.start()) return;
        this.micRecording = true;

        Thread.startVirtualThread(() -> {
            SpeechToText.Language lang;
            try {
                lang = SpeechToText.Language.valueOf(ChatConfigManager.getInstance().getVoiceConfig().getModel());
            } catch (IllegalArgumentException e) {
                lang = SpeechToText.Language.RUSSIAN_SMALL;
            }

            try {
                SpeechToText.get(lang).transcription();
            } catch (IOException | RuntimeException e) {
                VoiceIndicator.error();
            } finally {
                VoiceIndicator.finished();
                this.micRecording = false;
            }
        });
    }

    @Unique
    @EventSubscriber(event = VoskPartialResultEvent.class)
    public void onSendMessage(VoskPartialResultEvent event) {
        if (!event.getPartialResult().isBlank()) input.setValue(event.getPartialResult());
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.macrosButton != null) this.macrosButton.extractRenderState(graphics, mouseX, mouseY, delta);
        if (this.calculatorButton != null) this.calculatorButton.extractRenderState(graphics, mouseX, mouseY, delta);
        if (this.aiStyleText != null) this.aiStyleText.extractRenderState(graphics, mouseX, mouseY, delta);
        if (this.chats != null) this.chats.extractRenderState(graphics, mouseX, mouseY, delta);
        if (this.gifPicker != null) this.gifPicker.extractRenderState(graphics, mouseX, mouseY, delta);

        boolean micHover = chatmanager$isOverMic(mouseX, mouseY);
        graphics.fill(
                micX - 1, micY - 1, micX + MIC_SIZE + 1, micY + MIC_SIZE + 1,
                micHover || micRecording ? 0xFF3a2e6e : 0xFF14141a
        );
        graphics.blit(
                RenderPipelines.GUI_TEXTURED,
                micRecording ? MIC_ACTIVE_TEX : MIC_TEX,
                micX, micY, 0f, 0f, MIC_SIZE, MIC_SIZE, MIC_SIZE, MIC_SIZE
        );

        chatmanager$syncGifFocus();
    }

    @ModifyArg(
            method = "extractRenderState",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;fill(IIIII)V"
            ),
            index = 2
    )
    private int modifyFillArgs(int maxX) {
        return inputWidth;
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() == 0 && chatmanager$isOverMic(event.x(), event.y())) {
            chatmanager$startVoice();
            cir.setReturnValue(true);
            return;
        }

        if (this.gifPicker != null) {
            boolean handled = this.gifPicker.mouseClicked(event, doubleClick);
            chatmanager$syncGifFocus();
            if (handled) {
                cir.setReturnValue(true);
                return;
            }
        }

        if (this.macrosButton != null && this.macrosButton.mouseClicked(event, doubleClick))
            cir.setReturnValue(true);

        if (this.calculatorButton != null && this.calculatorButton.mouseClicked(event, doubleClick))
            cir.setReturnValue(true);

        if (this.aiStyleText != null && this.aiStyleText.mouseClicked(event, doubleClick))
            cir.setReturnValue(true);

        if (this.chats != null && this.chats.mouseClicked(event, doubleClick))
            cir.setReturnValue(true);
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void onMouseScrolled(
            double mouseX,
            double mouseY,
            double scrollX,
            double scrollY,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (this.gifPicker != null && this.gifPicker.mouseScrolled(mouseX, mouseY, scrollX, scrollY))
            cir.setReturnValue(true);
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void onKeyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (this.gifPicker != null && this.gifPicker.keyPressed(event)) {
            chatmanager$syncGifFocus();
            cir.setReturnValue(true);
        }
    }
}