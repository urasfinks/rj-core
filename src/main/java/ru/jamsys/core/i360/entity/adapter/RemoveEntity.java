package ru.jamsys.core.i360.entity.adapter;

import lombok.Getter;
import ru.jamsys.core.i360.Context;

import java.util.Map;

@Getter
public class RemoveEntity extends AbstractAdapter {

    public RemoveEntity(Map<String, Object> map) {
        super(map);
    }

    @Override
    public Context transform(Context context) {
        return null;
    }

}
