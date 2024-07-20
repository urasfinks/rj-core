package ru.jamsys.core.extension.property.item;

public class PropertyBoolean extends PropertyConverter<Boolean> {

    public PropertyBoolean(Boolean value) {
        super(value);
    }

    @Override
    public void set(String value) {
        this.value = value == null ? null : Boolean.parseBoolean(value);
    }

    @Override
    public String getAsString() {
        if (value == null) {
            return null;
        }
        return value ? "true" : "false";
    }

    public void set(boolean value) {
        this.value = value;
    }

}
