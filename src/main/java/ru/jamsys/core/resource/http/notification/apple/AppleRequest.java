package ru.jamsys.core.resource.http.notification.apple;

import lombok.Getter;

import java.util.Map;

@Getter
public class AppleRequest {

    private String title;

    private Map<String, Object> data;

    private String device;

    public AppleRequest(String title, Map<String, Object> data, String device) {
        this.title = title;
        this.data = data;
        this.device = device;
    }
}
