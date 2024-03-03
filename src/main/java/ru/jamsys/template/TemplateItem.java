package ru.jamsys.template;

public record TemplateItem(boolean isStatic, String value) {

    public TemplateItem(boolean isStatic, String value) {
        this.isStatic = isStatic;
        this.value = isStatic ? value : value.substring(2, value.length() - 1);
    }

}
