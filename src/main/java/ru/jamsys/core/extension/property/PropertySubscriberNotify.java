package ru.jamsys.core.extension.property;

import java.util.Set;

public interface PropertySubscriberNotify {

    void onPropertyUpdate(Set<String> updatedProp);

}
