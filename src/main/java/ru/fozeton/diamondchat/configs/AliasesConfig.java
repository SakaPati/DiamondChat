package ru.fozeton.diamondchat.configs;

import ru.fozeton.chatmanager.config.AliasConfig;

import java.util.List;

public class AliasesConfig implements Config<AliasConfig> {
    @Override
    public AliasConfig createDefault() {
        AliasConfig config = new AliasConfig();
        List<AliasConfig.Alias> aliases = config.getAliases();

        aliases.add(new AliasConfig.Alias("!lvl", "уровень"));
        aliases.add(new AliasConfig.Alias("!kb", "кол-во блоков"));
        aliases.add(new AliasConfig.Alias("!don", "донат"));
        aliases.add(new AliasConfig.Alias("!tp", "телепорт"));
        aliases.add(new AliasConfig.Alias("!ah", "аукцион"));
        aliases.add(new AliasConfig.Alias("!mine", "шахта"));
        aliases.add(new AliasConfig.Alias("!craft", "крафт"));
        aliases.add(new AliasConfig.Alias("!ench", "зачарование"));
        aliases.add(new AliasConfig.Alias("!clan", "клан"));
        aliases.add(new AliasConfig.Alias("!ks", "клан скилл"));
        aliases.add(new AliasConfig.Alias("!sell", "продажа"));
        aliases.add(new AliasConfig.Alias("!buy", "покупка"));
        aliases.add(new AliasConfig.Alias("!evt", "ивент"));
        aliases.add(new AliasConfig.Alias("!adm", "администрация"));
        aliases.add(new AliasConfig.Alias("!help", "помощь"));
        aliases.add(new AliasConfig.Alias("!ty", "спасибо"));

        return config;
    }

    @Override
    public boolean isEmpty(AliasConfig config) {
        return config == null || config.getAliases() == null || config.getAliases().isEmpty();
    }
}
