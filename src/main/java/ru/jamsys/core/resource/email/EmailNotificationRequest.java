package ru.jamsys.core.resource.email;

import lombok.Getter;
import lombok.Setter;

@Getter
public class EmailNotificationRequest {

    private final String title;

    private final String data;

    @Setter
    private String dataHtml;

    private final String to;

    public EmailNotificationRequest(String title, String data, String dataHtml, String to) {
        this.title = title;
        this.data = data;
        this.dataHtml = dataHtml;
        this.to = to;
    }

}
