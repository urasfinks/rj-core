package ru.jamsys.core.plugin.telegram.sender;

import ru.jamsys.core.plugin.telegram.TelegramRequest;
import ru.jamsys.core.plugin.telegram.message.TelegramOutputMessage;

public interface TelegramSender {

    TelegramRequest.Result send(TelegramOutputMessage telegramOutputMessage);

}
