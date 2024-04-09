package ru.jamsys.notification;

import ru.jamsys.http.HttpResponseEnvelope;

public interface Notification {

    @SuppressWarnings("unused")
    HttpResponseEnvelope notify(String title, Object data, String to);

    Notification getInstance();

}
