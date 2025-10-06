package ru.jamsys.core.plugin.telegram;

import lombok.Getter;
import lombok.Setter;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.jamsys.core.App;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.UniversalPath;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.expiration.ExpirationMap;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.flat.util.UtilUri;
import ru.jamsys.core.plugin.telegram.message.TelegramInputMessage;
import ru.jamsys.core.plugin.telegram.message.TelegramOutputMessage;
import ru.jamsys.core.plugin.telegram.structure.MessageType;
import ru.jamsys.core.plugin.telegram.structure.SendType;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.HashMap;
import java.util.Map;

@Getter
public class TelegramMessageHandler extends TelegramLongPollingBot {

    private final TelegramBot telegramBot;

    private final ManagerConfiguration<ExpirationMap<Long, Map<String, Object>>> sessions;

    @Setter
    private String defaultUrl = null; //Если приход чистое сообщение от пользователя без команды и нет данных из шага

    public TelegramMessageHandler(TelegramBot telegramBot) throws Exception {
        super(new String(App.get(SecurityComponent.class).get(telegramBot.getBotRepositoryProperty().getSecurityAlias())));
        this.telegramBot = telegramBot;
        if (telegramBot.getBotRepositoryProperty().getDefaultUrl() != null
            && !telegramBot.getBotRepositoryProperty().getDefaultUrl().isEmpty()) {
            defaultUrl = telegramBot.getBotRepositoryProperty().getDefaultUrl();
        }
        sessions = ManagerConfiguration.getInstance(
                telegramBot.getBotRepositoryProperty().getName(),
                telegramBot.getBotRepositoryProperty().getName(),
                ExpirationMap.class,
                integerXTestExpirationMap -> integerXTestExpirationMap
                        .setupTimeoutElementExpirationMs(600_000)
        );
    }

    @Override
    public String getBotUsername() {
        return telegramBot.getBotRepositoryProperty().getName();
    }

    @Override
    public void onUpdateReceived(Update msg) {
        if (msg == null) {
            return;
        }
        UtilLog.printInfo(msg);
        TelegramInputMessage telegramInputMessage = new TelegramInputMessage(msg);
        if (msg.hasCallbackQuery()) {
            new TelegramOutputMessage(
                    MessageType.AnswerCallbackQuery,
                    telegramInputMessage.getIdChat(),
                    SendType.EMBEDDED,
                    telegramBot.getNs()
            )
                    .setIdCallbackQuery(telegramInputMessage.getCallbackQueryId())
                    .setMessage("")
                    .send();


        }
        Long idChat = telegramInputMessage.getIdChat();
        if (idChat == null) {
            return;
        }
        String data = telegramInputMessage.getData();
        if (data == null) {
            // Сообщения может не быть
            // Если надо допустим принять картинку или видео, конечно при условии наличия stepHandler
            data = "";
        }
        String preparedUrl = null;
        if (msg.hasMessage() && msg.getMessage().hasSuccessfulPayment()) {
            preparedUrl = "/successful_payment";
        } else if (msg.hasPreCheckoutQuery()) {
            preparedUrl = "/pre_checkout_query";
        } else {
            Map<String, Object> stringObjectMap = sessions
                    .get()
                    .computeIfAbsent(telegramInputMessage.getIdChat(), _ -> new HashMap<>());
            // Можно наперёд установить в обработчике прошлого шага, что данные которые пошлёт клиент
            // пойдут по установленному url
            Object urlFromLastSetup = stringObjectMap.remove("url");
            if (urlFromLastSetup != null) {
                preparedUrl = urlFromLastSetup.toString();
            }
        }

        if (preparedUrl == null && defaultUrl != null) {
            preparedUrl = defaultUrl;
        }
        // Тут 2 варианта:
        // 1) Приходит чистое сообщение от пользователя
        // 2) Приходит ButtonCallbackData - подразумевает, что имеет полный путь /command/?args=...
        // Не должно быть чистого сообщения от пользователя содержащего контекст и начало с /
        if (data.startsWith("/")) {
            preparedUrl = null;
        }
        if (preparedUrl != null) {
            try {
                data = preparedUrl + UtilUri.encode(data);
            } catch (Exception e) {
                App.error(e);
                return;
            }
        }
        if (data.startsWith("/")) {
            if (idChat < 0 && !telegramBot.isAvailableGroup(idChat)) {
                answer(telegramInputMessage, "Группы не поддерживаются");
                return;
            }
            if (telegramInputMessage.isBot()) {
                answer(telegramInputMessage, "Боты не поддерживаются");
                return;
            }
            if (data.startsWith("/start ")) {
                data = "/start/?payload=" + data.substring(7);
            }
            PromiseGenerator match = telegramBot.getRouterRepository().match(data);
            if (match == null) {
                answer(telegramInputMessage, "Команда " + data + " не поддерживается");
                return;
            }
            Promise promise = match.generate();
            if (promise == null) {
                App.error(new RuntimeException("Promise is null"));
                return;
            }
            Map<String, Object> session = sessions
                    .get()
                    .computeIfAbsent(telegramInputMessage.getIdChat(), _ -> new HashMap<>());
            promise.setRepositoryMapClass(TelegramPromiseContext.class, new TelegramPromiseContext(
                    new UniversalPath(data),
                    session,
                    telegramInputMessage,
                    telegramBot.getNs()
            ));
            promise.run();
        }
    }

    private void answer(TelegramInputMessage telegramInputMessage, String message) {
        TelegramOutputMessage telegramOutputMessage = new TelegramOutputMessage(
                MessageType.SendMessage,
                telegramInputMessage.getIdChat(),
                SendType.EMBEDDED,
                telegramBot.getNs()
        )
                .setMessage(message);
        TelegramRequest.Result result = telegramOutputMessage.send();
        UtilLog.printError(new HashMapBuilder<String, Object>()
                .append("telegramInputMessage", telegramInputMessage)
                .append("telegramOutputMessage", telegramOutputMessage)
                .append("result", result)
        );
    }

}
