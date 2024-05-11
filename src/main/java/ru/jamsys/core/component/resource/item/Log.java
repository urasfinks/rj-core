package ru.jamsys.core.component.resource.item;

import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unused", "UnusedReturnValue"})
@ToString
public class Log {

    public Map<String, String> header;
    public String data;

    public Log(Map<String, String> header, String data) {
        this.header = header;
        this.data = data;
    }

    public Log(String data) {
        this.header = new HashMap<>();
        this.data = data;
    }

    public Log() {
        this.header = new HashMap<>();
    }

    public Log setData(String data) {
        this.data = data;
        return this;
    }

    public Log addHeader(String key, String value) {
        this.header.put(key, value);
        return this;
    }

}
