package ru.jamsys.core.plugin.telegram.sender;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.webapp.WebAppInfo;
import ru.jamsys.core.App;
import ru.jamsys.core.component.manager.ManagerConfiguration;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.UniversalPath;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.plugin.telegram.structure.Button;
import ru.jamsys.core.plugin.telegram.TelegramBot;
import ru.jamsys.core.plugin.telegram.TelegramRequest;
import ru.jamsys.core.plugin.telegram.message.TelegramOutputMessage;
import ru.jamsys.core.plugin.telegram.structure.Invoice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

// Для того, что бы отправлять сообщения через встроенную библиотеку Telegram необходимо поднять бота
// Мы это делаем через ManagerConfiguration
@Getter
public class TelegramSenderEmbedded extends AbstractManagerElement implements TelegramSender {

    @JsonIgnore
    ManagerConfiguration<TelegramBot> telegramBotManagerConfiguration;

    private final String ns;

    private final String key;

    public TelegramSenderEmbedded(String ns, String key) {
        this.ns = ns;
        this.key = key;
        telegramBotManagerConfiguration = ManagerConfiguration.getInstance(
                ns,
                ns,
                TelegramBot.class,
                null
        );
    }

    @Override
    public TelegramRequest.Result send(TelegramOutputMessage telegramOutputMessage) {
        markActive();
        switch (telegramOutputMessage.getMessageType()) {
            case SendPhoto -> {
                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(telegramOutputMessage.getIdChat());
                sendPhoto.setPhoto(new InputFile(
                        UtilFile.getWebFile(telegramOutputMessage.getImage().getPath()),
                        new UniversalPath(telegramOutputMessage.getImage().getPath()).getFileName()
                ));
                if (telegramOutputMessage.getMessage() != null) {
                    sendPhoto.setCaption(telegramOutputMessage.getMessage());
                }
                return TelegramRequest.execute(result -> result.setEmbeddedResponse(telegramBotManagerConfiguration
                        .get()
                        .getTelegramMessageHandler()
                        .execute(sendPhoto)
                ));
            }
            case SendMessage -> {
                SendMessage message = new SendMessage();
                message.setChatId(telegramOutputMessage.getIdChat());
                message.setText(telegramOutputMessage.getMessage());
                message.setParseMode("HTML");
                if (telegramOutputMessage.getButtons() != null && !telegramOutputMessage.getButtons().isEmpty()) {
                    addMessageButtonList(message, telegramOutputMessage.getButtons());
                }
                return nativeSend(message);
            }
            case AnswerCallbackQuery -> {
                String idCallbackQuery = telegramOutputMessage.getIdCallbackQuery();
                if (idCallbackQuery == null) {
                    return null;
                }
                AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
                answerCallbackQuery.setCallbackQueryId(idCallbackQuery);
                answerCallbackQuery.setText(telegramOutputMessage.getMessage());
                return nativeSend(answerCallbackQuery);
            }
            case DeleteMessage -> {
                Integer idMessageParent = telegramOutputMessage.getIdMessageParent();
                if (idMessageParent == null) {
                    return null;
                }
                DeleteMessage deleteMessage = new DeleteMessage();
                deleteMessage.setChatId(telegramOutputMessage.getIdChat());
                deleteMessage.setMessageId(idMessageParent);
                return nativeSend(deleteMessage);
            }
            case EditMessageText -> {
                Integer idMessageParent = telegramOutputMessage.getIdMessageParent();
                if (idMessageParent == null) {
                    return null;
                }
                EditMessageText editMessage = new EditMessageText();
                editMessage.setChatId(telegramOutputMessage.getIdChat());
                editMessage.setMessageId(idMessageParent);
                editMessage.setText(telegramOutputMessage.getMessage());
                return nativeSend(editMessage);
            }
            case AnswerPreCheckoutQuery -> {
                AnswerPreCheckoutQuery answer = new AnswerPreCheckoutQuery();
                answer.setPreCheckoutQueryId(telegramOutputMessage.getMessage());
                answer.setOk(true);
                return nativeSend(answer);
            }
            case SendInvoice -> {
                Invoice invoice = telegramOutputMessage.getInvoice();
                if (invoice == null) {
                    return null;
                }
                SendInvoice sendInvoice = new SendInvoice();
                sendInvoice.setChatId(telegramOutputMessage.getIdChat());
                sendInvoice.setTitle(invoice.getTitle());
                sendInvoice.setDescription(invoice.getDescription());
                sendInvoice.setPayload(invoice.getRqUid()); // Уникальный идентификатор платежа
                sendInvoice.setProviderToken(invoice.getProviderToken()); // Токен платежного провайдера
                sendInvoice.setCurrency("RUB"); // Валюта
                sendInvoice.setPrices(List.of(new LabeledPrice(
                        invoice.getLabelPrice(),
                        invoice.getAmount()
                ))); // Цена в копейках (10000 = 100 рублей)
                sendInvoice.setStartParameter(invoice.getPayload());

                // Добавляем кнопку "Оплатить"
                InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton payButton = new InlineKeyboardButton();
                payButton.setText("Оплатить");
                payButton.setPay(true);
                row.add(payButton);
                keyboard.add(row);
                keyboardMarkup.setKeyboard(keyboard);
                sendInvoice.setReplyMarkup(keyboardMarkup);
                return nativeSend(sendInvoice);
            }
            case SetMyCommands -> {
                List<BotCommand> botCommands = new ArrayListBuilder<>();
                for (Button button : telegramOutputMessage.getButtons()) {
                    botCommands.add(new BotCommand(button.getUrl(), button.getData()));
                }
                return TelegramRequest.execute(result -> result.setEmbeddedResponse(telegramBotManagerConfiguration
                        .get()
                        .getTelegramMessageHandler()
                        .execute(new SetMyCommands(botCommands, new BotCommandScopeDefault(), null))
                ));
            }
        }
        return null;
    }

    private <T extends Serializable, Method extends BotApiMethod<T>> TelegramRequest.Result nativeSend(Method method) {
        return TelegramRequest.execute(result -> {
                    try {
                        TelegramBot telegramBot = telegramBotManagerConfiguration.get();
                        T execute = telegramBot.
                                getTelegramMessageHandler()
                                .execute(method);

                        result.setEmbeddedResponse(execute);
                    } catch (Throwable th) {
                        App.error(th);
                        throw th;
                    }
                }
        );
    }

    private static void addMessageButtonList(SendMessage message, List<Button> listButtons) {
        List<List<InlineKeyboardButton>> list = new ArrayList<>();
        listButtons.forEach(button -> {
            InlineKeyboardButton markupInline = new InlineKeyboardButton(button.getData());
            if (button.getWebapp() != null) {
                markupInline.setWebApp(new WebAppInfo(button.getWebapp()));
            }
            if (button.getCallback() != null) {
                markupInline.setCallbackData(button.getCallback());
            }
            if (button.getUrl() != null) {
                markupInline.setUrl(button.getUrl());
            }
            list.add(List.of(markupInline));
        });
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(list);
        message.setReplyMarkup(markup);
    }

    @Override
    public void runOperation() {

    }

    @Override
    public void shutdownOperation() {

    }

}
