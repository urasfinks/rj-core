package ru.jamsys.core.i360.scope;

import ru.jamsys.core.flat.util.UtilJson;

public interface ScopeJsonSerialize extends Scope {

    default String toJson() {
        return UtilJson.toStringPretty(this, "{}");
    }

}
