package ru.jamsys.core.plugin.telegram.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.Manager;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.plugin.telegram.TelegramRequest;
import ru.jamsys.core.plugin.telegram.sender.TelegramSenderEmbedded;
import ru.jamsys.core.plugin.telegram.sender.TelegramSenderHttp;
import ru.jamsys.core.plugin.telegram.structure.Button;
import ru.jamsys.core.plugin.telegram.structure.FileSource;
import ru.jamsys.core.plugin.telegram.structure.Invoice;
import ru.jamsys.core.plugin.telegram.structure.MessageType;
import ru.jamsys.core.plugin.telegram.structure.SendType;

import java.util.ArrayList;
import java.util.List;


@Data
@Accessors(chain = true)
public class TelegramOutputMessage {

    private final MessageType messageType;

    private final long idChat;

    private final SendType sendType;

    private final String botNs;

    // NOT FINAL
    private String message;

    private FileSource image = null;

    private FileSource video = null;

    private List<Button> buttons = new ArrayList<>();

    private Invoice invoice = null;

    private Integer idMessageParent = null;

    private String idCallbackQuery = null;

    private Integer replyToMessageId = null;

    public TelegramOutputMessage(
            @JsonProperty("messageType") MessageType messageType,
            @JsonProperty("idChat") long idChat,
            @JsonProperty("sendType") SendType sendType,
            @JsonProperty("botNs") String botNs
    ) {
        this.messageType = messageType;
        this.idChat = idChat;
        this.sendType = sendType;
        this.botNs = botNs;
    }

    @SuppressWarnings("unused")
    @JsonIgnore
    public TelegramOutputMessage fromJson(String json) throws JsonProcessingException {
        return UtilJson.toObject(json, TelegramOutputMessage.class);
    }

    @SuppressWarnings("unused")
    @JsonIgnore
    public String toJson() {
        return UtilJson.toStringPretty(this, "{}");
    }

    @JsonIgnore
    public TelegramRequest.Result send() {
        switch (sendType) {
            case EMBEDDED -> {
                return App.get(Manager.class).get(
                                TelegramSenderEmbedded.class,
                                botNs,
                                botNs,
                                null
                        )
                        .send(this);
            }
            case HTTP -> {
                return App.get(Manager.class).get(
                                TelegramSenderHttp.class,
                                botNs,
                                botNs,
                                null
                        )
                        .send(this);
            }
        }
        return null;
    }

}
