package ru.fozeton.diamondchat.utils.enums;

import com.mojang.blaze3d.platform.InputConstants;
import lombok.Getter;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

@Getter
public enum KeyBinds {
    OPEN_CHAT_HISTORY("key.diamondchat.chat_history", GLFW.GLFW_KEY_I),
    CLEAR_CHAT("key.diamondchat.clear_chat", GLFW.GLFW_KEY_DELETE),
    TOGGLE_VOICE("key.diamondchat.toggle_voice", GLFW.GLFW_KEY_V);

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("diamondchat", "keys")
    );

    private final String translationKey;
    private final int defaultKey;
    private KeyMapping keyMapping;

    KeyBinds(String translationKey, int defaultKey) {
        this.translationKey = translationKey;
        this.defaultKey = defaultKey;
    }

    public static void registerAll() {
        for (KeyBinds bind : values()) {
            bind.keyMapping = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    bind.translationKey,
                    InputConstants.Type.KEYSYM,
                    bind.defaultKey,
                    CATEGORY
            ));
        }
    }

    public boolean matches(int keyCode, int scanCode) {
        if (keyMapping == null) return false;
        return keyMapping.matches(InputConstants.Type.KEYSYM.getOrCreate(keyCode));
    }
}