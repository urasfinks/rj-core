package ru.jamsys.thread.task;

import lombok.Getter;
import ru.jamsys.template.jdbc.Template;
import ru.jamsys.template.jdbc.TemplateEnum;

import java.util.LinkedHashMap;
import java.util.Map;

public class JdbcRequest extends AbstractTask implements Task {

    final String poolName;

    @Getter
    final TemplateEnum templateEnum;

    @Getter
    final Map<String, Object> args = new LinkedHashMap<>();

    boolean debug = false;

    public JdbcRequest(String poolName, TemplateEnum templateEnum) {
        this.poolName = poolName;
        this.templateEnum = templateEnum;
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
        return templateEnum.getName();
    }

    public boolean getDebug() {
        return debug;
    }

}
