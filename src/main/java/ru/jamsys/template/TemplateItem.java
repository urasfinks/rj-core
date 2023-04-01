package ru.jamsys.template;

import lombok.Data;

@Data
public class TemplateItem {

    public final boolean isStatic;
    public final String value;

    public TemplateItem(boolean isStatic, String value) {
        this.isStatic = isStatic;
        this.value = isStatic ? value : value.substring(2, value.length() - 1);
    }

}
