package ru.jamsys.core.extension.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ForwardException extends RuntimeException {

    List<String> contextSnapshot;

    @Setter
    @Accessors(chain = true)
    int line = 1;

    public ForwardException(Throwable cause) {
        super(null, cause);
    }

    public ForwardException(Object context) {
        setContextSnapshot(context);
    }

    public ForwardException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForwardException(Object context, Throwable cause) {
        super(cause);
        setContextSnapshot(context);
    }

    public ForwardException(String message, Object context) {
        super(message);
        setContextSnapshot(context);
    }

    public ForwardException(String message, Object context, Throwable cause) {
        super(message, cause);
        setContextSnapshot(context);
    }

    private void setContextSnapshot(Object context) {
        if (context == null) {
            return;
        }
        // Это сделано специально, что бы был snapshot в текущий момент, а не в момент POST сериализации
        String stringPretty = UtilJson.toStringPretty(context, "{}");
        this.contextSnapshot = new ArrayList<>();
        this.contextSnapshot.addFirst("ForwardException.Context:");
        this.contextSnapshot.addAll(List.of(stringPretty.split("\n")));
    }

}
