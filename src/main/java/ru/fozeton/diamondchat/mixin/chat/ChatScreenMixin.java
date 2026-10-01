package ru.fozeton.diamondchat.mixin.chat;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
import ru.fozeton.chatmanager.utils.stt.VoiceIndicator;
import ru.fozeton.diamondchat.gui.CalculatorScreen;
import ru.fozeton.diamondchat.gui.MacrosScreen;
import ru.fozeton.diamondchat.gui.widgets.DropdownButtonWidget;
import ru.fozeton.diamondchat.gui.widgets.TabButtonWidget;

import java.util.Map;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen {
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
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void chatmanager_core$registerBus(CallbackInfo ci) {
        ChatManagerCore.EVENT_BUS.register(this);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void chatmanager_core$unregisterBus(CallbackInfo ci) {
        ChatManagerCore.EVENT_BUS.unregister(this);
        VoiceIndicator.finished();
    }

    @Unique
    @EventSubscriber(event = VoskPartialResultEvent.class)
    public void onSendMessage(VoskPartialResultEvent event) {
        if(!event.getPartialResult().isBlank()) input.setValue(event.getPartialResult());
    }

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    private void onRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.macrosButton != null) this.macrosButton.extractRenderState(graphics, mouseX, mouseY, delta);
        if (this.calculatorButton != null) this.calculatorButton.extractRenderState(graphics, mouseX, mouseY, delta);
        if (this.aiStyleText != null) this.aiStyleText.extractRenderState(graphics, mouseX, mouseY, delta);
        if (this.chats != null) this.chats.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (this.macrosButton != null && this.macrosButton.mouseClicked(event, doubleClick))
            cir.setReturnValue(true);

        if (this.calculatorButton != null && this.calculatorButton.mouseClicked(event, doubleClick))
            cir.setReturnValue(true);

        if (this.aiStyleText != null && this.aiStyleText.mouseClicked(event, doubleClick))
            cir.setReturnValue(true);

        if (this.chats != null && this.chats.mouseClicked(event, doubleClick))
            cir.setReturnValue(true);
    }
}