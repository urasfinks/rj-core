package ru.jamsys.core.extension.property.item;

public class PropertyInteger extends PropertyInstance<Integer> {

    public PropertyInteger(Integer value) {
        super(value);
    }

    @Override
    public void set(String value) {
        this.value = value == null ? null : Integer.parseInt(value);
    }

    @Override
    public String getAsString() {
        if (value == null) {
            return null;
        }
        return value.toString();
    }

    public void set(int value) {
        this.value = value;
    }

}
