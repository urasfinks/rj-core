package ru.jamsys.core.flat.template.jdbc;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.sql.Types;

@Getter
public enum ArgumentType {

    VARCHAR(Types.VARCHAR),
    NUMBER(Types.NUMERIC),
    TIMESTAMP(Types.TIMESTAMP),
    ARRAY(Types.ARRAY),

    IN_ENUM_VARCHAR(-1),
    IN_ENUM_TIMESTAMP(-1),
    IN_ENUM_NUMBER(-1);

    private final int type;

    ArgumentType(int type) {
        this.type = type;
    }

    @JsonValue
    public HashMapBuilder<String, Object> getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("name", name())
                .append("sqlType", type);
    }

}
