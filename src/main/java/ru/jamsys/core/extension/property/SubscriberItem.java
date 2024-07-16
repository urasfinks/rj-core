package ru.jamsys.core.extension.property;

import lombok.Getter;
import lombok.Setter;

@Getter
public class SubscriberItem {

    private final String defValue;

    private final boolean require;

    @Setter
    private boolean isSubscribe = false;

    public SubscriberItem(String defValue, boolean require) {
        this.defValue = defValue;
        this.require = require;
    }

}
