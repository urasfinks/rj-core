package ru.jamsys.statistic;

import lombok.Getter;
import ru.jamsys.extension.EnumName;

public enum AvgMetricUnit implements EnumName {
    AVG_COUNT,
    MIN,
    MAX,
    SUM,
    AVG;

    @Getter
    private final String nameCache;

    AvgMetricUnit() {
        this.nameCache = getName();
    }
}