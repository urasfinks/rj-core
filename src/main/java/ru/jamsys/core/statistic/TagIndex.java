package ru.jamsys.core.statistic;

import ru.jamsys.core.extension.CLassNameTitle;
import ru.jamsys.core.util.Util;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

public interface TagIndex extends CLassNameTitle {

    Map<String, String> getTag();

    String getIndexCache();

    void setIndexCache(String index);

    default String compileIndex() {
        Map<String, String> tags = getTags();
        StringBuilder sb = new StringBuilder();
        sb.append(Util.capitalize(getClassNameTitle(null)));
        SortedSet<String> keys = new TreeSet<>(tags.keySet());
        for (String key : keys) {
            sb.append("-");
            sb.append(Util.capitalize(key));
            sb.append("=");
            sb.append(Util.capitalize(tags.get(key)));
        }
        return sb.toString();
    }

    default Map<String, String> getTags() {
        setIndexCache(null);
        return getTag();
    }

    default String getIndex() {
        if (getIndexCache() == null) {
            setIndexCache(compileIndex());
        }
        return getIndexCache();
    }
}
