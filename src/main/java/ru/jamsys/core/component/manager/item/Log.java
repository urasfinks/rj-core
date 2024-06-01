package ru.jamsys.core.component.manager.item;

import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

@ToString
public class Log {

    public Map<String, String> header = new HashMap<>();
    public String data;

    public Log(Map<String, String> header, String data) {
        this.header.putAll(header);
        this.data = data;
    }

    public Log(String data) {
        this.data = data;
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
