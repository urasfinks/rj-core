package ru.jamsys.core.extension.statistic;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.log.DataHeader;

@Getter
@Setter
@Accessors(chain = true)
public class StatisticDataHeader extends DataHeader {

    private final String cls;
    private final String ns;

    public StatisticDataHeader(Class<?> cls, String ns) {
        this.cls = App.getUniqueClassName(cls);
        this.ns = ns;
    }

    public StatisticDataHeader addHeader(String key, Object value) {
        header.put(key, value);
        return this;
    }

}
