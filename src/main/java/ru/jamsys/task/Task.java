package ru.jamsys.task;

import lombok.Data;
import ru.jamsys.Procedure;
import ru.jamsys.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

@Data
public class Task {
    private final Map<String, String> tags = new HashMap<>();
    private final Consumer<AtomicBoolean> consumer;
    private final long timeOutExecuteMillis;

    private String index = null;

    public Map<String, String> getTags() {
        index = null;
        return tags;
    }

    public Task(Consumer<AtomicBoolean> consumer, long timeOutExecuteMillis) {
        this.consumer = consumer;
        this.timeOutExecuteMillis = timeOutExecuteMillis; //TimeManager будет кидать interrupt если что
    }

    public void onKill(){

    }

    public String getIndex() {
        if (index == null) {
            index = compileIndex();
        }
        return index;
    }

    private String compileIndex() {
        StringBuilder sb = new StringBuilder();
        sb.append(Util.capitalize(getClass().getSimpleName()));
        SortedSet<String> keys = new TreeSet<>(tags.keySet());
        for (String key : keys) {
            sb.append(Util.capitalize(key));
            sb.append(Util.capitalize(tags.get(key)));
        }
        return sb.toString();
    }
}
