package ru.jamsys.core.flat.template.cron;

import lombok.Getter;
import ru.jamsys.core.flat.util.Util;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
public class TemplateItemCron {

    final Set<Integer> list = new LinkedHashSet<>();

    TimeUnit timeUnit;

    public String getName() {
        return timeUnit.getNameCache();
    }

    public TemplateItemCron(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public void add(int x) {
        if (x >= timeUnit.getMin() && x <= timeUnit.getMax()) {
            list.add(x);
        } else {
            Util.printStackTrace(getClass().getName() + " unit value: " + x + " overflow between [" + timeUnit.getMin() + ", " + timeUnit.getMax() + "]");
        }
    }

    @Override
    public String toString() {
        return list.toString();
    }
}