package ru.fozeton.diamondchat.utils;


import com.ferra13671.megaevents.eventbus.EventSubscriber;
import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.events.InputEvent;
import ru.fozeton.diamondchat.gui.ChatLogScreen;

public class KeyBind {
    @Getter
    private static final KeyBind instance = new KeyBind();
    public KeyMapping openGui;

    public KeyBind() {
        ChatManagerCore.EVENT_BUS.register(this);
    }

    public void register() {
        openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "Открыть историю чата",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                "DiamondChat"
        ));
    }

    @EventSubscriber(event = InputEvent.KeyInputEvent.class)
    public void onKeyEvent(InputEvent.KeyInputEvent event) {
        if (event.getAction() != InputEvent.KeyInputEvent.Action.PRESS) return;

        if (openGui.matches(event.getKeyCode(), InputConstants.UNKNOWN.getValue())) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen == null) {
                mc.setScreen(new ChatLogScreen());
            }
        }
    }
}