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

    private UniversalPath universalPath;

    @JsonIgnore
    private Map<Long, String> stepHandler;

    private TelegramInputMessage telegramInputMessage;

}
