package ru.jamsys.core.extension;

import java.util.Set;

public interface PropertySubscriberNotify {

    void onPropertyUpdate(Set<String> updatedProp);

}
