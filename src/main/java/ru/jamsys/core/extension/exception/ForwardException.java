package ru.jamsys.core.extension.exception;

import lombok.Getter;

@Getter
public class ForwardException extends RuntimeException {

    Object context;

    public ForwardException(Throwable cause) {
        super(null, cause);
    }

    public ForwardException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForwardException(Object context, Throwable cause) {
        super(cause);
        this.context = context;
    }

}
