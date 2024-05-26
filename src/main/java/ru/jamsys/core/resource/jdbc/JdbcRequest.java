package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;
import ru.jamsys.core.flat.template.jdbc.JdbcTemplate;
import ru.jamsys.core.flat.template.jdbc.TemplateJdbc;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class JdbcRequest {

    @Getter
    final JdbcTemplate jdbcTemplate;

    @Getter
    final Map<String, Object> args = new LinkedHashMap<>();

    boolean debug = false;

    private final String nameCache;

    public JdbcRequest(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.nameCache = jdbcTemplate.getName();
    }

    public JdbcRequest addArg(String key, Object obj) {
        args.put(key, obj);
        return this;
    }

    public JdbcRequest setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public TemplateJdbc getTemplate() {
        return jdbcTemplate.getTemplate();
    }

    public String getName() {
        return nameCache;
    }

    public boolean getDebug() {
        return debug;
    }

}
