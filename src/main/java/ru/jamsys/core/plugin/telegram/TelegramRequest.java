package ru.jamsys.core.plugin.telegram;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.functional.ConsumerThrowing;
import ru.jamsys.core.resource.http.client.HttpResponse;

import java.util.Map;

// Это песочница для executorRequest внутри которого исполняется запрос к api telegram
// Это сделано из-за того, что все статусы надо выдёргивать из текста Exception
public class TelegramRequest {

    public enum Status {
        OK,
        RETRY, // Стоит повторить
        NOT_INIT, // Пользователь не инициализировал бота командой /start
        REVOKE, // Отозвать подписку
        OTHER,
        ID_CHAT_EMPTY,
        SENDER_NULL
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Result {

        long timeCreate = System.currentTimeMillis();

        long executeTiming;

        Status status = Status.OK;

        String cause;

        HttpResponse httpResponse;

        Object embeddedResponse;

        Map<String, Object> parsedResponse;

        @SuppressWarnings("unused")
        public boolean isOk() {
            return status == null;
        }

        @SuppressWarnings("unused")
        public boolean isRetry() {
            if (status == Status.OK) { // Если нет исключения - то незамем повторять
                return false;
            }
            return status.equals(Status.RETRY);
        }

    }

    public static Result execute(ConsumerThrowing<Result> executorRequest) {
        long startTime = System.currentTimeMillis();
        Result telegramResult = new Result();
        try {
            // executorRequest в случае критичной ошибки должен сам выбросить исключение
            executorRequest.accept(telegramResult);
        } catch (Throwable th) {
            App.error(th);
            handleExceptionMessage(th.getMessage(), telegramResult);
        }
        telegramResult.setExecuteTiming(System.currentTimeMillis() - startTime);
        return telegramResult;
    }

    public static void handleExceptionMessage(String message, Result telegramResult) {
        if (message == null || message.isEmpty()) {
            telegramResult
                    .setStatus(Status.RETRY)
                    .setCause("th.getMessage() is null");
        } else if (message.contains("bot can't initiate conversation with a user")) {
            telegramResult
                    .setStatus(Status.NOT_INIT)
                    .setCause(message);
        } else if (
                message.contains("Too Many Requests")
                || message.contains("Unable to execute sendmessage method")
                || message.contains("Check your bot token")
        ) {
            telegramResult
                    .setStatus(Status.RETRY)
                    .setCause(message);
        } else if (
                message.contains("bot was blocked by the user")
                || message.contains("user is deactivated")
                || message.contains("bot was kicked from the group chat")
                || message.contains("bot can't send messages to bots")
                || message.contains("bot is not a member of the channel chat")
        ) {
            telegramResult
                    .setStatus(Status.REVOKE)
                    .setCause(message);
        } else {
            telegramResult
                    .setStatus(Status.OTHER)
                    .setCause(message);

        }
    }

}
