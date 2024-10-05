package ru.jamsys.core.i360.entity.adapter;

import lombok.Getter;
import ru.jamsys.core.i360.Context;

import java.util.Map;

@Getter
public class ForwardEntity extends AbstractAdapter {

    public ForwardEntity(Map<String, Object> map) {
        super(map);
    }

    @Override
    public Context transform(Context context) {
        return null;
    }

}
