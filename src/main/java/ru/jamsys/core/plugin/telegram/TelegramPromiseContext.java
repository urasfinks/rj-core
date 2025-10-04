package ru.jamsys.core.plugin.telegram;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.extension.UniversalPath;
import ru.jamsys.core.plugin.telegram.message.TelegramInputMessage;

import java.util.Map;

@Setter
@Getter
@Accessors(chain = true)
public class TelegramPromiseContext {

    private final UniversalPath universalPath;

    private final Map<String, Object> session;

    private final TelegramInputMessage telegramInputMessage;

    private final String botNs;

    public TelegramPromiseContext(
            UniversalPath universalPath,
            Map<String, Object> session,
            TelegramInputMessage telegramInputMessage,
            String botNs
    ) {
        this.universalPath = universalPath;
        this.session = session;
        this.telegramInputMessage = telegramInputMessage;
        this.botNs = botNs;
    }

}
