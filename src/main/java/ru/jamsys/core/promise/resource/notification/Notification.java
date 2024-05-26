package ru.jamsys.core.promise.resource.notification;

import ru.jamsys.core.resource.http.client.HttpResponseEnvelope;

public interface Notification {

    @SuppressWarnings("unused")
    HttpResponseEnvelope notify(String title, Object data, String to);

    Notification getInstance();

}
