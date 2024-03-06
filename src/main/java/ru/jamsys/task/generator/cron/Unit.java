package ru.jamsys.task.generator.cron;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class Unit {

    @Getter
    List<Integer> list = new ArrayList<>();

    @Getter
    int min;

    @Getter
    int max;

    @Getter
    MapUnit mapUnit;

    public Unit(int min, int max, MapUnit mapUnit) {
        this.min = min;
        this.max = max;
        this.mapUnit = mapUnit;
    }

    public void add(int x) {
        if (x >= min && x <= max && !list.contains(x)) {
            list.add(x);
        }
    }

    @Override
    public String toString() {
        return list.toString();
    }
}