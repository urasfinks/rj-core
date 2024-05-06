package ru.jamsys.core.component.item;

import lombok.ToString;

import java.util.HashMap;
import java.util.Map;

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

}
