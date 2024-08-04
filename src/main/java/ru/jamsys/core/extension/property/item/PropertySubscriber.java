package ru.jamsys.core.extension.property.item;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;

import java.util.Map;

@Getter
@ToString
public class PropertySubscriber {

    private String pattern;

    public PropertySubscriber setRegexp(String pattern) {
        this.pattern = pattern;
        return this;
    }

    private String key;

    public PropertySubscriber setKey(String key) {
        this.key = key;
        return this;
    }

    @ToString.Exclude
    @Setter
    private PropertyUpdateDelegate propertyUpdateDelegate;

    public void onPropertyUpdate(Map<String, String> map){
        propertyUpdateDelegate.onPropertyUpdate(map);
    }

}
