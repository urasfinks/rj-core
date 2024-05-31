package ru.jamsys.core.resource.notification.android;

import lombok.Getter;

import java.util.Map;

@Getter
public class AndroidNotificationRequest {

    private final String title;

    private final Map<String, Object> data;

    private final String token;

    public AndroidNotificationRequest(String title, Map<String, Object> data, String token) {
        this.title = title;
        this.data = data;
        this.token = token;
    }

}
