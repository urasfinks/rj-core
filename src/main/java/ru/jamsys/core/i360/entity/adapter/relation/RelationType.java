package ru.jamsys.core.i360.entity.adapter.relation;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.CamelNormalization;

@Getter
public enum RelationType implements CamelNormalization {

    REMOVE_RIGHT(new RemoveRight()), // Удалить из всего контекста ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2"]
    REMOVE_LEFT(new RemoveLeft()), // ["0", "1", "2", "3"] mask ["2", "2", "3", "4", "5"] => ["4", "5"]
    REMOVE_RIGHT_WHILE(new RemoveRightWhile()), // Удалять пока встречаются ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2", "0"]
    REMOVE_LEFT_WHILE(new RemoveLeftWhile()), // ["0"] mask ["0", "0", "1", "2", "0"] => ["1", "2", "0"]
    FORWARD_LEFT(new ForwardLeft()), // Пробросить все которые есть ["2", "1", "2", "3"] mask ["2", "3"] => ["2", "2", "3"]
    FORWARD_RIGHT(new ForwardRight()), // ["2", "3"] mask ["2", "1", "2", "3"] => ["2", "2", "3"]
    FORWARD_LEFT_WHILE(new ForwardLeftWhile()), // ["2", "1", "2", "3"] mask ["2", "3"] => ["2"]
    FORWARD_RIGHT_WHILE(new ForwardRightWhile()), // ["2", "3"] mask ["2", "1", "2", "3"] => ["2"]
    MAP_LEFT(new MapLeft()), // Заменить ["2", "1", "2", "3"] mask ["1", "!", "2", "-"] => ["-", "!", "-", "3"]
    MAP_RIGHT(new MapRight()),   // ["1", "!", "2", "-"] mask ["2", "1", "2", "3"]  => ["-", "!", "-", "3"]
    ADD(new Add()), // Добавить ["1", "2"] mask ["3"] => ["1", "2", "3"]
    INTERSECTION(new Intersection()), // Пересечение множеств ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["2", "3"]
    WITHOUT_INTERSECTION(new IntersectionWithout()); // Оба множества без пересечения ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["0", "1", "4","5"]

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
