package ru.jamsys.core.extension;

public class ForwardException extends RuntimeException {

    public ForwardException(Throwable cause) {
        super(null, cause);
    }

    public ForwardException(String message, Throwable cause) {
        super(message, cause);
    }

}
