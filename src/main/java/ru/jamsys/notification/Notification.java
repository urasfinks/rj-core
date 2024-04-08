package ru.jamsys.notification;

import ru.jamsys.http.HttpClient;

import java.util.Map;

public interface Notification {

    HttpClient notify(String title, Map<String, Object> data) throws Exception;

    Notification getInstance();

}
