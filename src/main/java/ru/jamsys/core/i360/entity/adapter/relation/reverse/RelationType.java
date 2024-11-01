package ru.jamsys.core.i360.entity.adapter.relation.reverse;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.CamelNormalization;
import ru.jamsys.core.i360.entity.adapter.relation.Relation;

@Getter
public enum RelationType implements CamelNormalization {

    REMOVE(new RemoveLeft()), // ["0", "1", "2", "3"] mask ["2", "2", "3", "4", "5"] => ["4", "5"]
    REMOVE_WHILE(new RemoveLeftWhile()), // ["0"] mask ["0", "0", "1", "2", "0"] => ["1", "2", "0"]
    FORWARD(new ForwardLeft()), // Пробросить все которые есть ["2", "1", "2", "3"] mask ["2", "3"] => ["2", "2", "3"]
    FORWARD_WHILE(new ForwardLeftWhile()), // ["2", "1", "2", "3"] mask ["2", "3"] => ["2"]
    MAP(new MapLeft()); // Заменить ["2", "1", "2", "3"] mask ["1", "!", "2", "-"] => ["-", "!", "-", "3"]

    final private Relation relation;

    RelationType(Relation relation) {
        this.relation = relation;
    }

    public static RelationType valueOfCamelCase(String type) {
        RelationType[] values = values();
        for (RelationType relationType : values) {
            if (relationType.getNameCamel().equals(type)) {
                return relationType;
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    @JsonValue
    public String toValue() {
        return getNameCamel();
    }

}
