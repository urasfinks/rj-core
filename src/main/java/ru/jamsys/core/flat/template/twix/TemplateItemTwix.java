package ru.jamsys.core.flat.template.twix;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public final class TemplateItemTwix {

    private final boolean staticFragment;

    private final String value;

    public TemplateItemTwix(boolean staticFragment, String value) {
        this.staticFragment = staticFragment;
        this.value = staticFragment ? value : value.substring(2, value.length() - 1);
    }

}
