package ru.fozeton.diamondchat.utils;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.events.InputEvent;
import ru.fozeton.chatmanager.module.SpeechToText;
import ru.fozeton.chatmanager.utils.compat.providers.ScreenProvider;
import ru.fozeton.chatmanager.utils.stt.VoiceIndicator;
import ru.fozeton.diamondchat.gui.ChatLogScreen;
import ru.fozeton.diamondchat.gui.LanguageModelsScreen;
import ru.fozeton.diamondchat.utils.enums.KeyBinds;

import java.io.IOException;

public class KeyBind {
    @Getter
    private static final KeyBind instance = new KeyBind();
    private SpeechToText sst;

    public KeyBind() {
        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void register() {
        KeyBinds.registerAll();
    }

    @EventSubscriber(event = InputEvent.KeyInputEvent.class)
    public void onKeyEvent(InputEvent.KeyInputEvent event) {
        if (event.getAction() != InputEvent.KeyInputEvent.Action.PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        int key = event.getKeyCode();
        int scanCode = InputConstants.UNKNOWN.getValue();

        if (KeyBinds.OPEN_CHAT_HISTORY.matches(key, scanCode)) {
            if (ScreenProvider.getScreen() == null) mc.setScreenAndShow(new ChatLogScreen());
        } else if (KeyBinds.CLEAR_CHAT.matches(key, scanCode)) {
            for (ChatChannel channel : ChatManagerCore.getChannels().values()) channel.clear();
        } else if (KeyBinds.OPEN_VOICE_MODELS.matches(key, scanCode)) {
            if (ScreenProvider.getScreen() == null) mc.setScreenAndShow(new LanguageModelsScreen());
        } else if (KeyBinds.TOGGLE_VOICE.matches(key, scanCode) && ScreenProvider.isChatScreen()) {
            if (!VoiceIndicator.start()) return;
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
                }
            });
        }
    }
}