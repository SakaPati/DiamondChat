package ru.fozeton.diamondchat.mixin.chat;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fozeton.diamondchat.chat.utils.ChatRenderManager;

@Mixin(ChatComponent.class)
public abstract class ChatHudRender {
    @Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V", at = @At(value = "HEAD"), cancellable = true)
    public void render(
            GuiGraphicsExtractor graphics,
            Font font,
            int ticks,
            int mouseX,
            int mouseY,
            ChatComponent.DisplayMode displayMode,
            boolean changeCursorOnInsertions,
            CallbackInfo ci
    ) {
        boolean chatOpen = displayMode == ChatComponent.DisplayMode.FOREGROUND;

        if (ChatRenderManager.getInstance().render(graphics, mouseX, mouseY, chatOpen)) ci.cancel();
    }
}
