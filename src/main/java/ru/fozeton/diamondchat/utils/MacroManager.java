package ru.fozeton.diamondchat.utils;

import com.ferra13671.megaevents.eventbus.EventSubscriber;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import ru.dargen.evoplus.api.EvoPlusApi;
import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.config.MacrosConfig;
import ru.fozeton.chatmanager.events.game.SecondElapsedEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MacroManager {
    @Getter
    private static final MacroManager instance = new MacroManager();
    private static final Map<String, Integer> timers = new HashMap<>();
    private static final MacrosConfig config = ChatConfigManager.getInstance().getMacrosConfig();

    public MacroManager() {
        ChatManagerCore.EVENT_BUS.register(this);
    }

    @EventSubscriber(event = SecondElapsedEvent.class)
    public void onSecondElapsed(SecondElapsedEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if(EvoPlusApi.getServer().getId() == 0) return;

        if (!config.isEnabled()) return;
        for (String key : new ArrayList<>(config.getMacros().keySet())) {
            MacrosConfig.Macros m = config.getMacros().get(key);
            if (m == null || !m.isActive()) {
                timers.put(key, 0);
                continue;
            }

            int elapsed = timers.getOrDefault(key, 0) + 1;

            if (elapsed >= m.getTime()) {
                timers.put(key, 0);
                if (key.startsWith("/")) mc.player.connection.sendCommand(key.substring(1));
                else mc.player.connection.sendChat(key);

            } else {
                timers.put(key, elapsed);
            }
        }
    }
}