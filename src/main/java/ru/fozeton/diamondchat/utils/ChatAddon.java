package ru.fozeton.diamondchat.utils;

import ru.dargen.evoplus.api.addon.Addon;
import ru.dargen.evoplus.api.addon.EvoPlusAddon;
import ru.dargen.evoplus.api.setting.AddonSettings;
import ru.dargen.evoplus.api.setting.SettingCategory;
import ru.dargen.evoplus.api.setting.SettingSection;
import ru.fozeton.diamondchat.gui.widgets.VoiceIndicatorWidget;

public class ChatAddon implements EvoPlusAddon {
    @Override
    public void onInitialize(Addon addon) {
        AddonSettings settings = addon.getSettings();
        SettingCategory general = settings.category("general", "DiamondChat");
        SettingSection widgets = general.section("widgets", "Индикатор голоса");
        VoiceIndicatorWidget.register(widgets);
    }
}
