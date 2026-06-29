package ru.fozeton.diamondchat.mixin.chat;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fozeton.diamondchat.chat.utils.ChatRenderManager;

@Mixin(ChatComponent.class)
public abstract class ChatHudRender {
    @Inject(method = "render", at = @At(value = "HEAD"), cancellable = true)
    public void render(GuiGraphics context, int currentTick, int mouseX, int mouseY, boolean focused, CallbackInfo ci) {
        if (ChatRenderManager.getInstance().render(context, mouseX, mouseY, focused)) ci.cancel();
    }
}
