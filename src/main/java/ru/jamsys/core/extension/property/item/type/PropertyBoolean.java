package ru.jamsys.core.extension.property.item.type;

public class PropertyBoolean extends PropertyInstance<Boolean> {

    public PropertyBoolean(String value) {
        super(Boolean.parseBoolean(value));
    }

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
