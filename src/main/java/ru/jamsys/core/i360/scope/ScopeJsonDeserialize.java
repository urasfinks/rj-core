package ru.jamsys.core.i360.scope;

import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.TriFunctionThrowing;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityImpl;
import ru.jamsys.core.i360.scale.Scale;
import ru.jamsys.core.i360.scale.ScaleType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ScopeJsonDeserialize extends Scope {

    default void fromJson(String json) throws Throwable {
        Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(json);
        Map<String, Entity> entityRepository = getEntityRepository();

        List<Entity> _ = forEachFilter(
                mapOrThrow,
                "entityRepository",
                EntityImpl.class.getName(),
                (jsonBloc, cls, map) -> entityRepository.computeIfAbsent(
                        (String) map.get("uuid"),
                        _ -> {
                            try {
                                return Entity.newInstance(jsonBloc, cls);
                            } catch (Throwable th) {
                                throw new ForwardException(th);
                            }
                        }
                )
        );

        getListScale().addAll(forEachFilter(
                mapOrThrow,
                "listScale",
                Scale.class.getName(),
                (_, _, map) -> {
                    Scale scale = new Scale();
                    scale.setType(ScaleType.valueOfReduction((String) map.get("type")));
                    scale.setStability(Double.parseDouble(map.get("stability") + ""));

                    @SuppressWarnings("unchecked")
                    List<String> left = (List<String>) map.get("left");
                    scale.setLeft(getContext(left));

                    @SuppressWarnings("unchecked")
                    List<String> right = (List<String>) map.get("right");
                    scale.setRight(getContext(right));

                    if (map.containsKey("classifier")) {
                        @SuppressWarnings("unchecked")
                        List<String> classifier = (List<String>) map.get("classifier");
                        scale.setClassifier(getContext(classifier));
                    }
                    return scale;
                }
        ));
    }

    private <T> List<T> forEachFilter(
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

}
