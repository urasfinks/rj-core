package ru.jamsys.thread.task;

import lombok.Getter;
import ru.jamsys.template.jdbc.Template;
import ru.jamsys.template.jdbc.JdbcTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

public class JdbcRequest extends AbstractTask {

    final String poolName;

    @Getter
    final JdbcTemplate jdbcTemplate;

    @Getter
    final Map<String, Object> args = new LinkedHashMap<>();

    boolean debug = false;

    private final String nameCache;

    public JdbcRequest(String poolName, JdbcTemplate jdbcTemplate, int maxTimeExecute) {
        super(maxTimeExecute);
        this.poolName = poolName;
        this.jdbcTemplate = jdbcTemplate;
        this.nameCache = jdbcTemplate.getName();
    }

    @SuppressWarnings("unused")
    public JdbcRequest addArg(String key, Object obj) {
        args.put(key, obj);
        return this;
    }

    @SuppressWarnings("unused")
    public JdbcRequest setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public String getPoolName() {
        return poolName;
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
