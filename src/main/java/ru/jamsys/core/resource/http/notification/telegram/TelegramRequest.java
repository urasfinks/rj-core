package ru.jamsys.core.resource.http.notification.telegram;

import lombok.Getter;

@Getter
public class TelegramRequest {

    private String title;

    private String data;

    public TelegramRequest(String title, String data) {
        this.title = title;
        this.data = data;
    }

}
