package ru.fozeton.diamondchat.mixin.chat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.fozeton.chatmanager.config.MacrosConfig;
import ru.fozeton.diamondchat.utils.MacroManager;
import ru.fozeton.diamondchat.widgets.CalculatorScreen;
import ru.fozeton.diamondchat.widgets.MacrosScreen;
import ru.fozeton.diamondchat.widgets.TabButtonWidget;

@Mixin(ChatScreen.class)
public class ChatScreenMixin extends Screen {
    @Unique private TabButtonWidget macrosButton;
    @Unique private TabButtonWidget calculatorButton;

    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        int y = this.height - 30;
        int h = 14;
        int x = 4;
        int gap = 54;

        MacrosConfig config = MacroManager.getConfig();

        this.macrosButton = new TabButtonWidget(x, y, 50, h,
                Component.literal("Macros"),
                () -> {
                    config.setEnabled(!config.isEnabled());
                    MacroManager.saveConfig();
                    if (this.macrosButton != null)
                        this.macrosButton.setActive(config.isEnabled());
                },
                () -> Minecraft.getInstance().setScreen(new MacrosScreen())
        );

        this.calculatorButton = new TabButtonWidget(x + gap, y, 80, h,
                Component.literal("Калькулятор"),
                () -> Minecraft.getInstance().setScreen(new CalculatorScreen()),
                null
        );

        this.macrosButton.setActive(config.isEnabled());
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(GuiGraphics graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (this.macrosButton != null)
            this.macrosButton.render(graphics, mouseX, mouseY, delta);
        if (this.calculatorButton != null)
            this.calculatorButton.render(graphics, mouseX, mouseY, delta);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void onMouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (this.macrosButton != null && this.macrosButton.mouseClicked(mouseX, mouseY, button))
            cir.setReturnValue(true);
        if (this.calculatorButton != null && this.calculatorButton.mouseClicked(mouseX, mouseY, button))
            cir.setReturnValue(true);
    }
}