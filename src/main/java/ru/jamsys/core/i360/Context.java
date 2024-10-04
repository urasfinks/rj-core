package ru.jamsys.core.i360;

import lombok.Getter;
import lombok.ToString;
import ru.jamsys.core.i360.entity.Entity;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
public class Context {

    private final List<Entity> listEntity = new ArrayList<>();

    public static Context load(List<String> listUuid, Scope scope) {
        Context context = new Context();
        List<Entity> list = context.getListEntity();
        listUuid.forEach(s -> list.add(scope.getEntityByUuid(s)));
        return context;
    }
}
