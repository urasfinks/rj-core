package ru.jamsys.core.resource.http.notification.apple;

import lombok.Getter;

import java.util.Map;

@Getter
public class AppleNotificationRequest {

    private final String title;

    private final Map<String, Object> data;

    private final String device;

    public AppleNotificationRequest(String title, Map<String, Object> data, String device) {
        this.title = title;
        this.data = data;
        this.device = device;
    }
}
