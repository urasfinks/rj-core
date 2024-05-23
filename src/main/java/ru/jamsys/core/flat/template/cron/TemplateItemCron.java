package ru.jamsys.core.flat.template.cron;

import lombok.Getter;
import ru.jamsys.core.flat.util.Util;

import java.util.ArrayList;
import java.util.List;

@Getter
public class TemplateItemCron {

    final List<Integer> list = new ArrayList<>();

    TimeUnit timeUnit;

    public String getName() {
        return timeUnit.getNameCache();
    }

    public TemplateItemCron(TimeUnit timeUnit) {
        this.timeUnit = timeUnit;
    }

    public void add(int x) {
        // Не конкурентная проверка
        if (x >= timeUnit.getMin() && x <= timeUnit.getMax() && !list.contains(x)) {
            list.add(x);
        } else {
            // Не конкурентная проверка
            if (!list.contains(x)) {
                Util.printStackTrace(getClass().getName() + " unit value: " + x + " overflow between [" + timeUnit.getMin() + ", " + timeUnit.getMax() + "]");
            }
        }
    }

    @Override
    public String toString() {
        return list.toString();
    }
}