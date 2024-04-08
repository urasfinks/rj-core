package ru.jamsys.notification;

import ru.jamsys.http.JsonHttpResponse;

public interface Notification2 {

    @SuppressWarnings("unused")
    JsonHttpResponse notify(String title, Object data);

    Notification2 getInstance();

}
