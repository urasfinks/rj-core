package ru.jamsys.core.template.cron;

import lombok.Getter;
import ru.jamsys.core.util.Util;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TemplateItem {

    final List<Integer> list = new ArrayList<>();

    TimeUnit timeUnit;

    public String getName() {
        return timeUnit.getNameCache();
    }

    public TemplateItem(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public void add(int x) {
        if (x >= timeUnit.getMin() && x <= timeUnit.getMax() && !list.contains(x)) {
            list.add(x);
        } else {
            if (!list.contains(x)) {
                Util.printStackTrace(getClass().getSimpleName() + " unit value: " + x + " overflow between [" + timeUnit.getMin() + ", " + timeUnit.getMax() + "]");
            }
        }
    }

    @Override
    public String toString() {
        return list.toString();
    }
}