package ru.jamsys.statistic;

import lombok.Getter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class Statistic {

    private final Map<String, String> tags = new LinkedHashMap<>();
    private final Map<String, Object> fields = new LinkedHashMap<>();

    public Statistic() {
    }

    public Statistic(Map<String, String> parentTags, Map<String, Object> parentFields) {
        addTags(parentTags);
        addFields(parentFields);
    }

    public Statistic addTag(String key, String value) {
        if (key != null) {
            tags.put(key, value);
        }
        return this;
    }

    public Statistic addField(String key, Object value) {
        if (key != null) {
            fields.put(key, value);
        }
        return this;
    }

    @SuppressWarnings("UnusedReturnValue")
    public Statistic addTags(Map<String, String> map) {
        if (map != null) {
            map.forEach(this::addTag);
        }
        return this;
    }

    public Statistic addFields(Map<String, Object> map) {
        if (map != null) {
            map.forEach(this::addField);
        }
        return this;
    }

}
