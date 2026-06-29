package ru.fozeton.diamondchat.chat.handlers;

import ru.fozeton.chatmanager.ChatManagerCore;
import ru.fozeton.chatmanager.channel.ChatChannel;
import ru.fozeton.chatmanager.channel.MessageHandlingChannel;
import ru.fozeton.chatmanager.config.ChannelsConfig;
import ru.fozeton.chatmanager.config.ChatConfigManager;
import ru.fozeton.chatmanager.messages.Message;
import ru.fozeton.chatmanager.messages.MessageHandler;
import ru.fozeton.chatmanager.utils.Logger;
import ru.fozeton.diamondchat.configs.MessagesFilterConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class Handler extends MessageHandlingChannel {
    private final Logger log = new Logger(Handler.class);
    private final List<FilterHandler> filterHandlers = new ArrayList<>();

    public Handler() {
        super();
        log.info("Инициализация обработчика чата...");
        MessagesFilterConfig filterConfig = ChatConfigManager.getInstance().load(MessagesFilterConfig.class);

        if (filterConfig != null && filterConfig.getFilters() != null) {
            for (var msg : filterConfig.getFilters()) {
                filterHandlers.add(new FilterHandler(Pattern.compile(msg.getPattern()), msg.getLineColor(), msg.getBorderColor()));
            }
            log.info("Загружено фильтров сообщений: " + filterConfig.getFilters().size());
        }

        ChannelsConfig channelsConfig = ChatConfigManager.getInstance().load(ChannelsConfig.class);
        for (Map.Entry<String, ChannelsConfig.ChannelSettings> entry : channelsConfig.getChannels().entrySet()) {
            ChannelsConfig.ChannelSettings settings = entry.getValue();
            if (settings.getPattern() != null) {
                registerHandler(new ChannelHandler(Pattern.compile(settings.getPattern()), settings.getId(), null));
            } else if (!settings.getId().equals("Default")) {
                log.warn("В канале " + settings.getId() + " отсутствует паттерн сообщений. Инициализация обработчика отменена");
            }
        }
        log.info("Зарегистрировано каналов чата: " + channelsConfig.getChannels().size());
    }

    @Override
    protected boolean onPreProcess(Message message) {
        log.debug("Получено сообщение чата от '" + message.getAuthor() + "': " + message.getPlainText());

        for (MessageHandler handler : getHandlers()) {
            if (handler instanceof ActionHandler) handler.action(message);
        }
        return true;
    }

    @Override
    protected void onFilterMessage(Message message) {
        for (FilterHandler filter : filterHandlers) {
            if (filter.action(message)) {
                log.info("Сообщение от '" + message.getAuthor() + "' попало под фильтр сообщений.");
                break;
            }
        }
    }

    @Override
    protected void onPostProcess(Message message) {
        boolean handled = false;
        for (MessageHandler handler : getHandlers()) {
            if (handler instanceof ChannelHandler) {
                if (handler.action(message)) {
                    log.debug("Сообщение успешно распределено в канал.");
                    handled = true;
                    break;
                }
            }
        }

        if (!handled) {
            log.debug("Канал для сообщения не найден. Отправка в канал по умолчанию.");
            ChatChannel defaultChannel = ChatManagerCore.getChannels().get("Default");
            if (defaultChannel != null) defaultChannel.addMessage(message);
            else log.error("Канал 'Default' не найден в ChatManagerCore!");
        }
    }
}