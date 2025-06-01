package ru.jamsys.core.handler.web.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ru.jamsys.core.extension.exception.AuthException;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.extension.http.ServletRequestReader;

// Перехватчик http запросов, пока планируется для авторизации

public interface HttpInterceptor {
    boolean handle(ServletHandler servletHandler) throws AuthException;
}
