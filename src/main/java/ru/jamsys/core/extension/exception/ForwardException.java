package ru.jamsys.core.extension.exception;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
public class ForwardException extends RuntimeException {

    Object context;

    @Setter
    @Accessors(chain = true)
    int line = 1;

    public ForwardException(Throwable cause) {
        super(null, cause);
    }

    public ForwardException(Object context) {
        this.context = context;
    }

    public ForwardException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForwardException(Object context, Throwable cause) {
        super(cause);
        this.context = context;
    }

    public ForwardException(String message, Object context) {
        super(message);
        this.context = context;
    }

    public ForwardException(String message, Object context, Throwable cause) {
        super(message, cause);
        this.context = context;
    }

}
