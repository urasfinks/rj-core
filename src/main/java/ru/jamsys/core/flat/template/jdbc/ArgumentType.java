package ru.jamsys.core.flat.template.jdbc;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.extension.builder.HashMapBuilder;

import java.sql.Types;

@Getter
public enum ArgumentType {

    VARCHAR(Types.VARCHAR, "text"),
    NUMBER(Types.NUMERIC, "numeric"),
    TIMESTAMP(Types.TIMESTAMP, "timestamp"),
    BOOLEAN(Types.BOOLEAN, "boolean"),
    ARRAY(Types.ARRAY, null),

    IN_ENUM_VARCHAR(-1, null),
    IN_ENUM_TIMESTAMP(-1, null),
    IN_ENUM_NUMBER(-1, null);

    private final int type;

    private final String typeName;

    ArgumentType(int type, String typeName) {
        this.type = type;
        this.typeName = typeName;
    }

    @JsonValue
    public HashMapBuilder<String, Object> getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("name", name())
                .append("sqlType", type);
    }

}
