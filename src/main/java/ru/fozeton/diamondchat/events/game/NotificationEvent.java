package ru.fozeton.diamondchat.events.game;

import com.ferra13671.megaevents.event.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import ru.fozeton.diamondchat.utils.notify.Notification;

@Getter
@RequiredArgsConstructor
public class NotificationEvent extends Event<NotificationEvent> {
    private final Notification notification;
}
