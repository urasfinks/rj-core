package ru.jamsys.core.i360.entity.adapter;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.extension.CamelNormalization;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.adapter.selection.*;

public enum SetEntityOperator implements CamelNormalization {

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
    WITHOUT_INTERSECTION(null), // Оба множества без пересечения ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["0", "1", "4","5"]
    REVERSE(new Reverse()); // Задом на перёд, для решения задач удаления с конца всех нулей например

    final private SetOperator adapter;

    SetEntityOperator(SetOperator adapter) {
        this.adapter = adapter;
    }

    public Context transform(Context context, Context contextSelection) {
        return adapter.transform(context, contextSelection);
    }

    static SetEntityOperator valueOfCamelCase(String type) {
        SetEntityOperator[] values = values();
        for (SetEntityOperator setEntityOperator : values) {
            if (setEntityOperator.getNameCamel().equals(type)) {
                return setEntityOperator;
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
