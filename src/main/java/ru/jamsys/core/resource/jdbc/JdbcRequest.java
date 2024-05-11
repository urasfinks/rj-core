package ru.jamsys.core.resource.jdbc;

import lombok.Getter;
import ru.jamsys.core.statistic.expiration.mutable.ExpirationMsMutableImpl;
import ru.jamsys.core.template.jdbc.JdbcTemplate;
import ru.jamsys.core.template.jdbc.Template;

import java.util.LinkedHashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class JdbcRequest extends ExpirationMsMutableImpl {

    @Getter
    final String poolName;

    @Getter
    final JdbcTemplate jdbcTemplate;

    @Getter
    final Map<String, Object> args = new LinkedHashMap<>();

    boolean debug = false;

    private final String nameCache;

    public JdbcRequest(String poolName, JdbcTemplate jdbcTemplate, int maxTimeExecute) {
        setKeepAliveOnInactivityMs(maxTimeExecute);
        this.poolName = poolName;
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

    public Template getTemplate() {
        return jdbcTemplate.getTemplate();
    }

    public String getName() {
        return nameCache;
    }

    public boolean getDebug() {
        return debug;
    }

}
