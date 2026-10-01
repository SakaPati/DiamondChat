package ru.fozeton.diamondchat.utils;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.events.speech.VoskModelAbsentEvent;
import ru.fozeton.chatmanager.events.speech.VoskModelDownloadFailedEvent;
import ru.fozeton.chatmanager.events.speech.VoskModelDownloadSuccessEvent;
import ru.fozeton.chatmanager.module.SpeechToText;
import ru.fozeton.diamondchat.gui.LanguageModelsScreen;
import ru.fozeton.diamondchat.utils.enums.NotificationType;
import ru.fozeton.diamondchat.utils.notify.Notification;

import java.time.Duration;

public class VoskModelListener {
    @Getter
    private static final VoskModelListener instance = new VoskModelListener();

    private VoskModelListener() {
        ChatManagerCore.EVENT_BUS.register(this);
    }

    @EventSubscriber(event = VoskModelAbsentEvent.class)
    public void onAbsent(VoskModelAbsentEvent event) {
        Notification.showCustom(Notification.builder()
                                        .type(NotificationType.WARN)
                                        .title("Распознавание речи")
                                        .message("Языковая модель не найдена. Нажми, чтобы скачать")
                                        .duration(Duration.ofSeconds(10))
                                        .action(() -> {
                                            Minecraft mc = Minecraft.getInstance();
                                            mc.execute(() -> mc.setScreenAndShow(new LanguageModelsScreen()));
                                        })
                                        .build());
    }

    @EventSubscriber(event = VoskModelDownloadSuccessEvent.class)
    public void onSuccess(VoskModelDownloadSuccessEvent event) {
        SpeechToText.Language lang = event.getLanguage();
        LanguageModelsScreen.markSuccess(lang);
        Notification.showSuccess("Модель " + lang.getModel() + " скачана", null);
    }

    @EventSubscriber(event = VoskModelDownloadFailedEvent.class)
    public void onFailed(VoskModelDownloadFailedEvent event) {
        SpeechToText.Language lang = event.getLanguage();
        LanguageModelsScreen.markFailed(lang);
        Notification.showError("Не удалось скачать " + lang.getModel(), null);
    }
}