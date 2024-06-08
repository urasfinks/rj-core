package ru.jamsys.core.extension;

import java.util.Set;

public interface SubscriberPropertyNotifier {

    void onPropertyUpdate(Set<String> updatedProp);

}
