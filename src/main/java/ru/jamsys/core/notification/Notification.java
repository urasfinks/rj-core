package ru.jamsys.core.notification;

import ru.jamsys.core.resource.http.HttpResponseEnvelope;

public interface Notification {

    @SuppressWarnings("unused")
    HttpResponseEnvelope notify(String title, Object data, String to);

    Notification getInstance();

}
