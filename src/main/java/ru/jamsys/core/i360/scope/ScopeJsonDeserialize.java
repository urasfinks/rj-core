package ru.jamsys.core.i360.scope;

import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilFileResource;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.scale.ScaleImpl;
import ru.jamsys.core.i360.scale.ScaleType;
import ru.jamsys.core.i360.scale.ScaleTypeRelation;

import java.util.List;
import java.util.Map;

// Загрузка области знаний с файловой системы

public interface ScopeJsonDeserialize extends Scope {

    default void read(String path) throws Throwable {
        fromJson(UtilFileResource.getAsString(path, UtilFileResource.Direction.PROJECT));
    }

    default void fromJson(String json) throws Throwable {
        Map<String, Object> mapOrThrow = UtilJson.getMapOrThrow(json);
        Map<String, Entity> entityRepository = getMapEntity();

        @SuppressWarnings("unchecked")
        Map<String, Object> jsonEntityRepository = (Map<String, Object>) mapOrThrow.get("mapEntity");

        jsonEntityRepository.forEach((uuid, value) -> {
            if (uuid.startsWith("_")) {
                return;
            }
            try {
                entityRepository.computeIfAbsent(uuid, _ -> {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> map = (Map<String, Object>) value;
                        return Entity.newInstance(new HashMapBuilder<>(map).append("uuid", uuid), this);
                    } catch (Throwable th) {
                        throw new ForwardException(th);
                    }
                });
            } catch (Throwable e) {
                throw new ForwardException(e);
            }
        });

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> jsonListScale = (List<Map<String, Object>>) mapOrThrow.get("listScale");

        List<ScaleImpl> listScale = getRepositoryScale().getListScale();

        jsonListScale.forEach(map -> {
            ScaleImpl scale = new ScaleImpl(ScaleType.STATEMENT, ScaleTypeRelation.valueOfReduction((String) map.get("type")));
            scale.setStability(Double.parseDouble(map.get("stability") + ""));

            @SuppressWarnings("unchecked")
            List<String> left = (List<String>) map.get("left");
            scale.setLeft(getRepositoryEntityChain().getByUuids(left));

            @SuppressWarnings("unchecked")
            List<String> right = (List<String>) map.get("right");
            scale.setRight(getRepositoryEntityChain().getByUuids(right));

            listScale.add(scale);
        });
    }

}
