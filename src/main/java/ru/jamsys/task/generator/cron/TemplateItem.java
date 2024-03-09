package ru.jamsys.task.generator.cron;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

public class TemplateItem {

    @Getter
    List<Integer> list = new ArrayList<>();

    @Getter
    Unit unit;

    public String getName() {
        return unit.getName();
    }

    public TemplateItem(Unit unit) {
        this.unit = unit;
    }

    public void add(int x) {
        if (x >= unit.getMin() && x <= unit.getMax() && !list.contains(x)) {
            list.add(x);
        }
    }

    @Override
    public String toString() {
        return list.toString();
    }
}