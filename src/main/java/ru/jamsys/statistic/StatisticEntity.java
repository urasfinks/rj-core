package ru.jamsys.statistic;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class StatisticEntity {

    Map<String, String> tags = new LinkedHashMap<>();
    Map<String, Object> fields = new LinkedHashMap<>();

    StatisticEntity addTag(String key, String value) {
        tags.put(key, value);
        return this;
    }

    StatisticEntity addField(String key, Object value) {
        fields.put(key, value);
        return this;
    }

    StatisticEntity addTags(Map<String, String> map) {
        map.forEach(this::addTag);
        return this;
    }

    StatisticEntity addFields(Map<String, Object> map) {
        map.forEach(this::addField);
        return this;
    }

}
