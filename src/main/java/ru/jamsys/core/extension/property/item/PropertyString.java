package ru.jamsys.core.extension.property.item;

public class PropertyString extends PropertyConverter<String>{

    public PropertyString(String value) {
        super(value);
    }

    @Override
    public void set(String value) {
        this.value = value;
    }

    @Override
    public String getAsString() {
        return value;
    }

}
