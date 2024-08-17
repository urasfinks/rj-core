package ru.jamsys.core.web.http;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Перехватчик http запросов, пока планируется для авторизации

public interface HttpInterceptor {
    boolean handle(HttpServletRequest request, HttpServletResponse response);
}
