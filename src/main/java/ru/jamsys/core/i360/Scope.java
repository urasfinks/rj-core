package ru.jamsys.core.i360;

import lombok.Getter;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.TriFunctionThrowing;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
public class Scope {

    final private List<Entity> listEntity = new ArrayList<>();

    final private List<Scale> listScale = new ArrayList<>();

    public void load(String path) throws Throwable {
        String asString = UtilFileResource.getAsString(path, UtilFileResource.Direction.PROJECT);
        Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(asString);
        listEntity.addAll(keyLoad(
                mapOrThrow,
                "knowledge",
                EntityImpl.class.getName(),
                (s, aClass, _) -> UtilJson.toObject(s, aClass)
        ));
        listScale.addAll(keyLoad(
                mapOrThrow,
                "scale",
                Scale.class.getName(),
                (_, _, map) -> {
                    Scale scale = new Scale();
                    scale.setType(ScaleType.valueOfReduction((String) map.get("type")));
                    scale.setStability(Double.parseDouble(map.get("stability") + ""));

                    @SuppressWarnings("unchecked")
                    List<String> left = (List<String>) map.get("left");
                    scale.setLeft(Context.load(left, this));

                    @SuppressWarnings("unchecked")
                    List<String> right = (List<String>) map.get("right");
                    scale.setRight(Context.load(right, this));

                    System.out.println(scale);
                    return scale;
                }
        ));
    }

    private <T> List<T> keyLoad(
            Map<String, Object> mapOrThrow,
            String key,
            String defClass,
            TriFunctionThrowing<String, Class<? extends T>, Map<String, Object>, T> fn
    ) {
        List<T> result = new ArrayList<>();
        if (mapOrThrow.containsKey(key)) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> list = (List<Map<String, Object>>) mapOrThrow.get(key);
            list.forEach(stringObjectMap -> {
                try {
                    @SuppressWarnings("unchecked")
                    Class<? extends T> cls = (Class<? extends T>) Class.forName(
                            (String) stringObjectMap.getOrDefault("class", defClass)
                    );
                    result.add(fn.apply(UtilJson.toString(stringObjectMap), cls, stringObjectMap));
                } catch (Throwable th) {
                    throw new ForwardException(th);
                }
            });
        }
        return result;
    }

    public Entity getEntityByUuid(String uuid) {
        for (Entity entity : listEntity) {
            if (entity.getUuid().equals(uuid)) {
                return entity;
            }
        }
        return null;
    }

}
