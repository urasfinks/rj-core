package ru.jamsys.core.extension.log;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
@ToString
public class DataHeader {

    public DataHeader() {
    }

    public DataHeader(Class<?> cls) {
        setBody(App.getUniqueClassName(cls));
    }

    private long timeAdd = System.currentTimeMillis();

    protected Map<String, Object> header = new LinkedHashMap<>();

    private Object body;

    public DataHeader addHeader(String key, Object value) {
        header.put(key, value);
        return this;
    }

    @SuppressWarnings("unused")
    public DataHeader putAll(Map<String, ?> map) {
        if (map != null) {
            map.forEach(this::addHeader);
        }
        return this;
    }

}
