package ru.jamsys.statistic;

public enum AvgMetricUnit {

    AVG_COUNT("AvgCount"),
    MIN("Min"),
    MAX("Max"),
    SUM("Sum"),
    AVG("Avg");

    final String name;

    AvgMetricUnit(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
