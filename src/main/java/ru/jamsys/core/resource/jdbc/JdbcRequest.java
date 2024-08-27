package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
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

    final List<Map<String, Object>> listArgs = new ArrayList<>();

    public List<Map<String, Object>> getListArgs() {
        if (listArgs.size() > 1 && listArgs.getLast().isEmpty()) {
            listArgs.removeLast();
        }
        return listArgs;
    }

    boolean debug = false;

    private final String nameCache;

    public JdbcRequest(JdbcRequestRepository jdbcRequestRepository) {
        this.jdbcRequestRepository = jdbcRequestRepository;
        this.nameCache = jdbcRequestRepository.getNameCamel();
        listArgs.add(new LinkedHashMap<>());
    }

    public JdbcRequest addArg(String key, Object obj) {
        listArgs.getLast().put(key, obj);
        return this;
    }

    public JdbcRequest nextBatch() {
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

    public String getName() {
        return nameCache;
    }

    public boolean getDebug() {
        return debug;
    }

}
