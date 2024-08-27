package ru.jamsys.core.extension.http;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HttpAsyncResponse {

    @Getter
    private final CompletableFuture<Void> completableFuture;

    @Getter
    private final HttpServletRequest request;

    @Getter
    private final HttpServletResponse response;

    @Setter
    private String responseContentType = "application/json";

    @Setter
    private String body = "";

    @Getter
    private final HttpRequestReader httpRequestReader;

    public void setBodyFromMap(Map<?, ?> data) {
        setBody(UtilJson.toStringPretty(data, "{}"));
    }

    public boolean isEmptyBody() {
        return this.body.isEmpty();
    }

    public void setBodyIfEmpty(String body) {
        if (this.body.isEmpty()) {
            this.body = body;
        }
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

    public void setResponseHeader(String key, String value) {
        response.setHeader(key, value);
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

    public void complete(InputStream inputStream) {
        try {
            OutputStream out = getResponse().getOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            inputStream.close();
            out.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        completableFuture.complete(null);
    }

    public CompletableFuture<Void> getServletResponse() {
        return completableFuture;
    }

    public void setError(String cause) {
        setBodyFromMap(new HashMapBuilder<>().append("status", false).append("cause", cause));
    }

    public void setUnauthorized() throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader("WWW-Authenticate", "Basic realm=\"JamSys\"");
        response.getWriter().print("<html><body><h1>401. Unauthorized</h1></body>");
    }

}
