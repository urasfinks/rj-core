package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.template.jdbc.JdbcRequestRepository;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class JdbcRequest {

    @Getter
    final JdbcRequestRepository jdbcRequestRepository;

    @Setter
    @Getter
    @Accessors(chain = true)
    boolean batchMaybeEmpty = true;

    @Getter
    boolean debug = false;

    @Getter
    private final String name;

    final List<Map<String, Object>> listArgs = new ArrayList<>();

    public List<Map<String, Object>> getListArgs() {
        // Просто отрезаем пустой элемент, если он был сформирован nextBatch() заканчивающим пачку
        if (listArgs.size() > 1 && listArgs.getLast().isEmpty()) {
            listArgs.removeLast();
        }
        if (listArgs.size() == 1 && listArgs.getFirst().isEmpty() && !isBatchMaybeEmpty()) {
            return null;
        }
        return listArgs;
    }

    public JdbcRequest(JdbcRequestRepository jdbcRequestRepository) {
        this.jdbcRequestRepository = jdbcRequestRepository;
        this.name = jdbcRequestRepository.getNameCamel();
        listArgs.add(new LinkedHashMap<>());
    }

    public JdbcRequest addArg(String key, Object obj) {
        listArgs.getLast().put(key, obj);
        return this;
    }

    public JdbcRequest addArg(Map<String, ?> map) {
        listArgs.getLast().putAll(map);
        return this;
    }

    public JdbcRequest nextBatch() {
        if (!isBatchMaybeEmpty() && listArgs.getLast().isEmpty()) {
            return this;
        }
        listArgs.add(new LinkedHashMap<>());
        return this;
    }

    public JdbcRequest setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcRequestRepository.getJdbcTemplate();
    }

}
