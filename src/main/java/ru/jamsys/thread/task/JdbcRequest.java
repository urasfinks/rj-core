package ru.jamsys.thread.task;

import lombok.Getter;
import ru.jamsys.template.jdbc.Template;
import ru.jamsys.template.jdbc.TemplateEnum;

import java.util.LinkedHashMap;
import java.util.Map;

public class JdbcRequest extends AbstractTask {

    final String poolName;

    @Getter
    final TemplateEnum templateEnum;

    @Getter
    final Map<String, Object> args = new LinkedHashMap<>();

    boolean debug = false;

    private final String nameCache;

    public JdbcRequest(String poolName, TemplateEnum templateEnum, int maxTimeExecute) {
        super(maxTimeExecute);
        this.poolName = poolName;
        this.templateEnum = templateEnum;
        this.nameCache = templateEnum.getName();
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
        return templateEnum.getTemplate();
    }

    public String getName() {
        return nameCache;
    }

    public boolean getDebug() {
        return debug;
    }

}
