package ru.jamsys.core.extension.property;

import java.util.Set;

public interface PropertyUpdateNotifier {

    void onPropertyUpdate(Set<String> updatedPropAlias); // Alias - это то что указано в аннотации @PropertyName

}
