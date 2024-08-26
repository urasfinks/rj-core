package ru.jamsys.core.extension.exception;

import lombok.Getter;

@Getter
public class RateLimitException extends RuntimeException {

    String info;

    public RateLimitException(String message, String info) {
        super(message);
        this.info = info;
    }

}
