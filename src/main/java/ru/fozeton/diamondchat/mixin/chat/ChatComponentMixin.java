package ru.fozeton.diamondchat.mixin.chat;

import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.channel.ChatChannel;

@Mixin(ChatComponent.class)
public class ChatComponentMixin {
    @Inject(method = "clearMessages", at = @At("HEAD"))
    public void onClearMessages(boolean bl, CallbackInfo ci) {
        ChatManagerCore.getChannels().values().forEach(ChatChannel::clear);
    }
}
