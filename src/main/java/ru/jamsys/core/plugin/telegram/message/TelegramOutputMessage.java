package ru.jamsys.core.plugin.telegram.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.plugin.telegram.Button;

import java.util.ArrayList;
import java.util.List;


@Data
@Accessors(chain = true)
public class TelegramOutputMessage {

    public enum MessageType{
        AnswerCallbackQuery,
        DeleteMessage,
        EditMessageText,
        SendMessage,
        AnswerPreCheckoutQuery,
        SendInvoice,
        SendPhoto,
        SendVideo,
        SetMyCommands
    }

    @Data
    public static class Invoice{
        String title;
        String description;
        String rqUid;
        String providerToken;
        String labelPrice;
        int amount;
        String payload;
    }

    @Data
    @Accessors(chain = true)
    public static class Source {
        String path;
        String id;
        String description;
    }

    final MessageType messageType;

    private final long idChat;

    private String message;

    // NOT FINAL
    private Source image = null;

    private Source video = null;

    private List<Button> buttons = new ArrayList<>();

    private Invoice invoice = null;


    private Integer idMessageParent = null;
    private String idCallbackQuery = null;

    public TelegramOutputMessage(
            @JsonProperty("messageType") MessageType messageType,
            @JsonProperty("idChat") long idChat
    ) {
        this.messageType = messageType;
        this.idChat = idChat;
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

}
