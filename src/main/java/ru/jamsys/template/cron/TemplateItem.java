package ru.jamsys.template.cron;

import lombok.Getter;
import ru.jamsys.util.Util;

import java.util.ArrayList;
import java.util.List;

public class TemplateItem {

    @Getter
    final List<Integer> list = new ArrayList<>();

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
        } else {
            if (!list.contains(x)) {
                Util.printStackTrace(getClass().getSimpleName() + " unit value: " + x + " overflow between [" + unit.getMin() + ", " + unit.getMax() + "]");
            }
        }
    }

    @Override
    public String toString() {
        return list.toString();
    }
}