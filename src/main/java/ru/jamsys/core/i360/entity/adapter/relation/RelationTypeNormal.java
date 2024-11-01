package ru.jamsys.core.i360.entity.adapter.relation;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.CamelNormalization;
import ru.jamsys.core.i360.entity.adapter.relation.normal.*;

@Getter
public enum RelationTypeNormal implements CamelNormalization {

    REMOVE(new RemoveRight()), // Удалить из всего контекста ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2"]
    REMOVE_WHILE(new RemoveRightWhile()), // Удалять пока встречаются ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2", "0"]
    FORWARD(new ForwardRight()), // ["2", "3"] mask ["2", "1", "2", "3"] => ["2", "2", "3"]
    FORWARD_WHILE(new ForwardRightWhile()), // ["2", "3"] mask ["2", "1", "2", "3"] => ["2"]
    MAP(new MapRight()),   // ["1", "!", "2", "-"] mask ["2", "1", "2", "3"]  => ["-", "!", "-", "3"]
    ADD(new Add()), // Добавить ["1", "2"] mask ["3"] => ["1", "2", "3"]
    INTERSECTION(new Intersection()), // Пересечение множеств ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["2", "3"]
    WITHOUT_INTERSECTION(new IntersectionWithout()); // Оба множества без пересечения ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["0", "1", "4","5"]

    final private Relation relation;

    RelationTypeNormal(Relation relation) {
        this.relation = relation;
    }

    public static RelationTypeNormal valueOfCamelCase(String type) {
        RelationTypeNormal[] values = values();
        for (RelationTypeNormal relationType : values) {
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
