package ru.jamsys.template.jdbc;

import lombok.Data;

@Data
public class Argument implements Cloneable {

    int index;
    ArgumentDirection direction;
    ArgumentType type;
    String key;
    Object value;
    String sqlKeyTemplate;

    public void parseSqlKey(String sqlKeyTemplate) {
        this.sqlKeyTemplate = sqlKeyTemplate;
        String[] keys = sqlKeyTemplate.split("\\.");
        if (keys.length == 1) {
            throw new RuntimeException("Не достаточно описания для " + sqlKeyTemplate + "; Должно быть ${direction.var::type}");
        }
        this.direction = ArgumentDirection.valueOf(keys[0]);
        String[] types = keys[1].split("::");
        if (types.length == 1) {
            throw new RuntimeException("Не достаточно описания для " + sqlKeyTemplate + "; Должно быть ${direction.var::type}");
        }
        this.key = types[0];
        this.type = ArgumentType.valueOf(types[1]);

        if (key.isEmpty()) {
            throw new RuntimeException("Не достаточно описания для " + sqlKeyTemplate + "; Должно быть ${direction.var::type}");
        }
    }

    @Override
    public Argument clone() throws CloneNotSupportedException {
        return (Argument) super.clone();
    }

}
