package ru.fozeton.diamondchat.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fozeton.diamondchat.DiamondChat;
import ru.fozeton.diamondchat.events.gui.GuiOpenEvent;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "setScreenAndShow", at = @At(value = "HEAD"))
    public void openGui(Screen screen, CallbackInfo ci) {
        DiamondChat.EVENT_BUS.activate(new GuiOpenEvent(screen));
    }
}
