package ru.jamsys.core.extension.property;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.property.item.PropertySubscription;
import ru.jamsys.core.flat.util.UtilDate;
import ru.jamsys.core.flat.util.UtilRisc;

import java.util.concurrent.ConcurrentHashMap;

@JsonPropertyOrder({"stack", "value", "time"})
@Getter
public class SetTrace {

    private String stack;

    private final String value;

    private final String time;

    @Setter
    private String resource;

    public SetTrace(String value) {
        this.value = value;
        this.time = UtilDate.get(UtilDate.format);
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        int idx = -1;
        for (StackTraceElement stackTraceElement : stackTrace) {
            idx++;
            if (idx < 1) {
                continue;
            }
            if (stackTraceElement.getFileName() == null
                    || stackTraceElement.getFileName().equals(ConcurrentHashMap.class.getSimpleName() + ".java")
                    || stackTraceElement.getFileName().equals(PropertyDispatcher.class.getSimpleName() + ".java")
                    || stackTraceElement.getFileName().equals(PropertySubscription.class.getSimpleName() + ".java")
                    || stackTraceElement.getFileName().equals(ServiceProperty.class.getSimpleName() + ".java")
                    || stackTraceElement.getFileName().equals(Property.class.getSimpleName() + ".java")
                    || stackTraceElement.getFileName().equals(UtilRisc.class.getSimpleName() + ".java")
                    || stackTraceElement.getFileName().equals(SetTrace.class.getSimpleName() + ".java")
            ) {
                continue;
            }
            if (stackTraceElement.getClassName().startsWith(ServiceClassFinder.pkg)) {
                this.stack = ExceptionHandler.getLineStack(stackTraceElement);
                break;
            }
        }
    }

}