package ru.fozeton.diamondchat.events.gui;

import com.ferra13671.megaevents.event.Event;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.screens.Screen;

@Getter
@RequiredArgsConstructor
public class GuiOpenEvent extends Event<GuiOpenEvent> {
    private final Screen currentScreen;
}
