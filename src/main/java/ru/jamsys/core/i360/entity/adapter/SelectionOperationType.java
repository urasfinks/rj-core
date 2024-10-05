package ru.jamsys.core.i360.entity.adapter;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.extension.CamelNormalization;
import ru.jamsys.core.i360.Context;
import ru.jamsys.core.i360.entity.adapter.selection.*;

public enum SelectionOperationType implements CamelNormalization {
    REMOVE(new Remove()), // Удалить из всего контекста ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2"]
    REMOVE_WHILE(new RemoveWhile()), // Удалять пока встречаются ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2", "0"]
    FORWARD(new Forward()), // Пробросить все которые есть ["2", "1", "2", "3"] mask ["2", "3"] => ["2", "2", "3"]
    FORWARD_WHILE(new ForwardWhile()), // Пробрасывать пока встречаются ["2", "1", "2", "3"] mask ["2", "3"] => ["2"]
    REVERSE(new Reverse()), // Задом на перёд, для решения задач удаления с конца всех нулей например
    MAP(new Map()), // Заменить ["2", "1", "2", "3"] mask ["1", "2"]/["!", "-"] => ["-", "!", "-", "3"]
    ADD(new Map()), // Добавить ["1", "2"] mask ["3"] => ["1", "2", "3"]
    INTERSECTION(new Intersection()); // Пересечение множеств ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["2", "3"]

    final private ContextSelection adapter;

    SelectionOperationType(ContextSelection adapter) {
        this.adapter = adapter;
    }

    public Context transform(Context context, Context contextSelection) {
        return adapter.transform(context, contextSelection);
    }

    static SelectionOperationType valueOfCamelCase(String type) {
        SelectionOperationType[] values = values();
        for (SelectionOperationType selectionOperationType : values) {
            if (selectionOperationType.getNameCamel().equals(type)) {
                return selectionOperationType;
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
