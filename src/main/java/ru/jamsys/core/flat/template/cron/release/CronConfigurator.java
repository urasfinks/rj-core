package ru.jamsys.core.flat.template.cron.release;

import com.fasterxml.jackson.annotation.JsonValue;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.template.cron.Cron;

public interface CronConfigurator {

    String getCronTemplate();

    default boolean isTimeHasCome(Cron.CompileResult compileResult) {
        // Пример, если надо пропустить первый запуск при старте
        // return compileResult.getBeforeTimestamp() != 0;
        return true;
    }

    @JsonValue
    default Object getValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", getClass())
                .append("cronTemplate", getCronTemplate())
                ;
    }

}
