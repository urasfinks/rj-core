package ru.jamsys;

import lombok.Data;

import java.util.*;

@Data
public class JsonHttpResponse {
    public String description;
    public boolean status = true;
    public Map<String, Object> data = new HashMap<>();
    public List<String> exception = new ArrayList<>();

    public JsonHttpResponse() {

    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void addData(String key, Object value) {
        data.put(key, value);
    }

    public void setException(String e) {
        status = false;
        exception.add(e);
    }

    @Override
    public String toString() {
        return Optional.ofNullable(Util.jsonObjectToStringPretty(this)).orElse("{}");
    }
}
