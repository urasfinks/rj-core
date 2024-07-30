package ru.jamsys.core.extension.property;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class PropertyFollower {

    private String pattern;

    private String key;

    private PropertyUpdateDelegate follower;

    public void onPropertyUpdate(Map<String, String> map){
        follower.onPropertyUpdate(map);
    }

}
