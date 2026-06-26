package ru.fozeton.diamondchat.neoforge;

import ru.fozeton.diamondchat.Diamondchat;
import net.neoforged.fml.common.Mod;

@Mod(Diamondchat.MOD_ID)
public final class DiamondchatNeoForge {
    public DiamondchatNeoForge() {
        // Run our common setup.
        Diamondchat.init();
    }
}
