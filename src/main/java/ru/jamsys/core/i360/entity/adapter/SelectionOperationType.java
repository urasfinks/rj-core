package ru.jamsys.core.i360.entity.adapter;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.extension.CamelNormalization;

public enum SelectionOperationType implements CamelNormalization {
    REMOVE, // Удалить из всего контекста ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2"]
    REMOVE_WHILE, // Удалять пока встречаются ["0", "0", "1", "2", "0"] mask ["0"] => ["1", "2", "0"]
    FORWARD, // Пробросить все которые есть ["2", "1", "2", "3"] mask ["2", "3"] => ["2", "2", "3"]
    FORWARD_WHILE, // Пробрасывать пока встречаются ["2", "1", "2", "3"] mask ["2", "3"] => ["2"]
    REVERSE, // Задом на перёд, для решения задач удаления с конца всех нулей например
    MAP, // Заменить ["2", "1", "2", "3"] mask ["1", "2"]/["!", "-"] => ["-", "!", "-", "3"]
    ADD, // Добавить ["1", "2"] mask ["3"] => ["1", "2", "3"]
    INTERSECTION; // Пересечение множеств ["0", "1", "2", "3"] mask ["2", "3", "4", "5"] => ["2", "3"]

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
