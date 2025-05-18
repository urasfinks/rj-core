package ru.jamsys.core.extension.exception;

import ru.jamsys.core.flat.util.UtilLog;

public class ForwardException extends RuntimeException {

    public ForwardException(Throwable cause) {
        super(null, cause);
    }

    public ForwardException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForwardException(Object context, Throwable cause) {
        super(UtilLog.error(context).toStringBuilder().toString(), cause);
    }

}
