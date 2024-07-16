package ru.jamsys.core.statistic;

import lombok.Getter;
import ru.jamsys.core.extension.CamelNormalization;

@Getter
public enum AvgMetricUnit implements CamelNormalization {
    SELECTION,
    MIN,
    MAX,
    SUM,
    AVG;

    private final String nameCache;

    AvgMetricUnit() {
        this.nameCache = getNameCamel();
    }
}
