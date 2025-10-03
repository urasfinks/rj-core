package ru.jamsys.core.plugin.telegram.message;

import lombok.Getter;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.jamsys.core.flat.util.UtilJson;

// Получение информации из сообщения от Telegram
@Getter
public class TelegramInputMessage {

    private final Update msg;

    public TelegramInputMessage(Update msg) {
        this.msg = msg;
    }

    @SuppressWarnings("unused")
    public Integer getIdMessage() {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getMessage().getMessageId();
        } else if (msg.hasMessage()) {
            return msg.getMessage().getMessageId();
        }
        return null;
    }

    public String getCallbackQueryId() {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getId();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public Long getIdChat() {
        // Если это запрос от Telegram Payments, фиктивно подменяем idChat, так как у pre_checkout_query нет id_chat
        if (msg.hasPreCheckoutQuery()) {
            return msg.getPreCheckoutQuery().getFrom().getId();
        } else if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getMessage().getChatId();
        } else if (msg.hasMessage()) {
            return msg.getMessage().getChatId();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public String getData() {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getData();
        } else if (msg.hasMessage()) {
            return msg.getMessage().getText();
        }
        return null;
    }

    @SuppressWarnings("unused")
    public String getUserInfo() {
        if (msg.hasCallbackQuery()) {
            return UtilJson.toStringPretty(msg.getCallbackQuery().getFrom(), "{}");
        } else if (msg.hasMessage()) {
            return UtilJson.toStringPretty(msg.getMessage().getFrom(), "{}");
        }
        return null;
    }

    public boolean isBot() {
        if (msg.hasCallbackQuery()) {
            return msg.getCallbackQuery().getFrom().getIsBot();
        } else // Если какие-то обходные пути будут, лучше прикрыть, так как я не знаю
            if (msg.hasMessage()) {
            return msg.getMessage().getFrom().getIsBot();
        } else return !msg.hasPreCheckoutQuery();
    }

}
