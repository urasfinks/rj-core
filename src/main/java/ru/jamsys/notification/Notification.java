package ru.jamsys.notification;

import ru.jamsys.http.JsonHttpResponse;

public interface Notification {

    @SuppressWarnings("unused")
    JsonHttpResponse notify(String title, Object data, String to);

    Notification getInstance();

}
