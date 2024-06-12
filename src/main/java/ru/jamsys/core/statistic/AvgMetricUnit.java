package ru.jamsys.core.statistic;

import lombok.Getter;
import ru.jamsys.core.extension.EnumName;

@Getter
public enum AvgMetricUnit implements EnumName {
    SELECTION,
    MIN,
    MAX,
    SUM,
    AVG;

    private final String nameCache;

    AvgMetricUnit() {
        this.nameCache = getName();
    }
}
