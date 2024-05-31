package ru.jamsys.core.resource.notification.telegram;

import lombok.Getter;

@Getter
public class TelegramNotificationRequest {

    private final String title;

    private final String data;

    public TelegramNotificationRequest(String title, String data) {
        this.title = title;
        this.data = data;
    }

}
