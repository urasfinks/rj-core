package ru.jamsys.core.i360.entity.adapter;

import lombok.Getter;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.Adapter;

import java.util.Map;

@Getter
public class ForwardEntity implements Adapter {

    private String uuid;

    public ForwardEntity(Map<String, Object> map) {
    }

    @Override
    public Context transform(Context context) {
        return null;
    }



}
