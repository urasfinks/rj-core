package ru.jamsys.task.generator.cron;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedDeque;

@ToString
public class Template {

    // * = каждый
    @Getter
    List<Integer> second = new ArrayList<>(); // * = empty; [0-59]

    @Getter
    List<Integer> minute = new ArrayList<>(); // * = empty; [0-59]

    @Getter
    List<Integer> hour = new ArrayList<>(); // * = empty; [0-23]

    @Getter
    List<Integer> dayOfMonth = new ArrayList<>(); // * = empty; [1-31]

    @Getter
    List<Integer> monthOfYear = new ArrayList<>(); // * = empty; [1-12]

    @Getter
    List<Integer> dayOfWeek = new ArrayList<>(); // * = empty; [0-7]

    @ToString.Exclude
    ConcurrentLinkedDeque<Long> deque = new ConcurrentLinkedDeque<>();

    @ToString.Exclude
    private final Map<Integer, List<Integer>> map = new LinkedHashMap<>();

    public Template() {
        map.put(0, second);
        map.put(1, minute);
        map.put(2, hour);
        map.put(3, dayOfMonth);
        map.put(4, monthOfYear);
        map.put(5, dayOfWeek);
    }

    public void parse(String template) {
        String[] s = template.split(" ");
        for (int i = 0; i < s.length; i++) {
            parseItem(s[i], map.get(i));
        }
    }

    public void compileNextExecute() {

    }

    private void parseItem(String template, List<Integer> list) {
        String[] split = template.split(",");
        for (String s : split) {
            if (!"*".equals(s)) {
                list.add(Integer.parseInt(s));
            }
        }
    }
}
