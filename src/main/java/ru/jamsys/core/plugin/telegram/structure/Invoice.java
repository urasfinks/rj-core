package ru.jamsys.core.plugin.telegram.structure;

import lombok.Data;

@Data
public class Invoice {
    String title;
    String description;
    String rqUid;
    String providerToken;
    String labelPrice;
    int amount;
    String payload;
}
