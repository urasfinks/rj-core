package ru.jamsys.message;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MessageImpl implements Message {

    ConcurrentLinkedQueue<String> routeMap = new ConcurrentLinkedQueue<>();
    ConcurrentLinkedQueue<Exception> errorQueue = new ConcurrentLinkedQueue<>();


    Map<String, Object> userProperty = new ConcurrentHashMap<>();

    public void setHeader(String name, Object value){
        userProperty.put(name, value);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getHeader(String name){
        return (T) userProperty.get(name);
    }

    @Setter
    @Getter
    private String body = "";

    @Getter
    private final String correlation = java.util.UUID.randomUUID().toString();

    @Override
    public void onHandle(MessageHandle handle, Object service) {
        routeMap.add(convertTimestamp(System.currentTimeMillis()) + " " + service.toString() + " " + handle.toString());
    }

    @Override
    public void setError(Exception e) {
        errorQueue.add(e);
    }
}
