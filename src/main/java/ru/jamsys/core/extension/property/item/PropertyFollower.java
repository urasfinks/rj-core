package ru.jamsys.core.extension.property.item;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.extension.property.PropertyUpdateDelegate;

import java.util.Map;

@Getter
@ToString
public class PropertyFollower {

    private String pattern;

    public PropertyFollower setRegexp(String pattern) {
        this.pattern = pattern;
        return this;
    }

    private String key;

    public PropertyFollower setKey(String key) {
        this.key = key;
        return this;
    }

    @ToString.Exclude
    @Setter
    private PropertyUpdateDelegate follower;

    public void onPropertyUpdate(Map<String, String> map){
        follower.onPropertyUpdate(map);
    }

}
