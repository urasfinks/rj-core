package ru.jamsys.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.web.http.lib.HttpRequestReader;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HttpAsyncResponse {

    private final CompletableFuture<Void> completableFuture;

    @Getter
    private final HttpServletRequest request;

    private final HttpServletResponse response;

    @Setter
    private String responseContentType = "application/json";

    @Setter
    private String body = "{}";

    @Getter
    private final HttpRequestReader httpRequestReader;

    public void setBodyFromMap(Map<?, ?> data) {
        setBody(UtilJson.toStringPretty(data, "{}"));
    }

    public HttpAsyncResponse(
            CompletableFuture<Void> completableFuture,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        this.completableFuture = completableFuture;
        this.request = request;
        this.response = response;
        httpRequestReader = new HttpRequestReader(request);
    }

    public void complete() {
        response.setContentType(responseContentType);
        try {
            response.getWriter().print(body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        completableFuture.complete(null);
    }

    public CompletableFuture<Void> getServletResponse() {
        String requestURI = request.getRequestURI();
        new Thread(() -> {
            //Util.sleepMs(3000);
            setBodyFromMap(new HashMapBuilder<>().append("x", "y"));
            complete();
        }).start();
        return completableFuture;
    }

}
