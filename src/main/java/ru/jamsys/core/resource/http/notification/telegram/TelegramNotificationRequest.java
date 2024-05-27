package ru.jamsys.core.resource.http.notification.telegram;

import lombok.Getter;

@Getter
public class TelegramNotificationRequest {

    private String title;

    private String data;

    public TelegramNotificationRequest(String title, String data) {
        this.title = title;
        this.data = data;
    }

}
