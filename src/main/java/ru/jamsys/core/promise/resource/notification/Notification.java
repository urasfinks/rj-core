package ru.jamsys.core.promise.resource.notification;

import ru.jamsys.core.resource.http.client.HttpResponse;

public interface Notification {

    @SuppressWarnings("unused")
    HttpResponse notify(String title, Object data, String to);

    Notification getInstance();

}
