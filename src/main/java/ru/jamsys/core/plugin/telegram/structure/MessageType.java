package ru.jamsys.core.plugin.telegram.structure;

// TODO: переделать в константы
public enum MessageType {
    AnswerCallbackQuery,
    DeleteMessage,
    EditMessageText,
    SendMessage,
    AnswerPreCheckoutQuery,
    SendInvoice,
    SendPhoto,
    SendVideo,
    SetMyCommands,
    forwardMessage
}
