package ru.jamsys.template.twix;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public final class TemplateItem {

    public final boolean isStatic;
    public final String value;

    public TemplateItem(boolean isStatic, String value) {
        this.isStatic = isStatic;
        this.value = isStatic ? value : value.substring(2, value.length() - 1);
    }

}
