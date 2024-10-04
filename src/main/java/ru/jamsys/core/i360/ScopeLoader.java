package ru.jamsys.core.i360;

import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.extension.functional.TriFunctionThrowing;
import ru.jamsys.core.flat.util.FileWriteOptions;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.entity.EntityImpl;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface ScopeLoader extends Scope {

    default void load(String path) throws Throwable {
        fromJson(UtilFileResource.getAsString(path, UtilFileResource.Direction.PROJECT));
    }

    default void save(String path) throws Throwable {
        UtilFile.writeBytes(path, toJson().getBytes(StandardCharsets.UTF_8), FileWriteOptions.CREATE_OR_REPLACE);
    }

    default String toJson() {
        return UtilJson.toStringPretty(this, "{}");
    }

    default void fromJson(String json) throws Throwable {
        Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(json);
        getListEntity().addAll(keyLoad(
                mapOrThrow,
                "listEntity",
                EntityImpl.class.getName(),
                (s, aClass, _) -> Entity.fromJson(s, aClass)
        ));
        getListScale().addAll(keyLoad(
                mapOrThrow,
                "listScale",
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

                    if (map.containsKey("classifier")) {
                        @SuppressWarnings("unchecked")
                        List<String> classifier = (List<String>) map.get("classifier");
                        scale.setClassifier(Context.load(classifier, this));
                    }
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
}
