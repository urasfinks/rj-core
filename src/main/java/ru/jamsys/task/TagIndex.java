package ru.jamsys.task;

import ru.jamsys.util.Util;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public class TagIndex {
    private final Map<String, String> tags = new HashMap<>();
    private String index = null;

    @SuppressWarnings("unused")
    public Map<String, String> getTags() {
        index = null;
        return tags;
    }

    public String getIndex() {
        if (index == null) {
            index = compileIndex();
        }
        return index;
    }

    protected String compileIndex() {
        StringBuilder sb = new StringBuilder();
        sb.append(Util.capitalize(getClass().getSimpleName()));
        SortedSet<String> keys = new TreeSet<>(tags.keySet());
        for (String key : keys) {
            sb.append("-");
            sb.append(Util.capitalize(key));
            sb.append("=");
            sb.append(Util.capitalize(tags.get(key)));
        }
        return sb.toString();
    }
}
