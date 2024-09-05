package ru.jamsys.core.extension.http;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServletHandler {

    @Getter
    private final CompletableFuture<Void> completableFuture;

    @Getter
    private final HttpServletRequest request;

    private final HttpServletResponse response;

    @Setter
    private String responseContentType = "application/json";

    @Setter
    private String responseBody = "";

    @Getter
    private final ServletRequestReader requestReader;

    public ServletHandler(
            CompletableFuture<Void> completableFuture,
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        this.completableFuture = completableFuture;
        this.request = request;
        this.response = response;
        requestReader = new ServletRequestReader(request);
    }

    public void setResponseBodyFromMap(Map<?, ?> data) {
        setResponseBody(UtilJson.toStringPretty(data, "{}"));
    }

    public boolean isEmptyBody() {
        return this.responseBody.isEmpty();
    }

    public void setBodyIfEmpty(String body) {
        if (this.responseBody.isEmpty()) {
            this.responseBody = body;
        }
    }

    public void setResponseHeader(String key, String value) {
        response.setHeader(key, value);
    }

    ServletOutputStream servletOutputStream;

    public ServletOutputStream getResponseOutputStream() throws IOException {
        if (servletOutputStream == null) {
            servletOutputStream = response.getOutputStream();
        }
        return servletOutputStream;
    }

    public Writer getResponseWriter() throws IOException {
        return response.getWriter();
    }

    public void responseComplete() {
        response.setContentType(responseContentType);
        try {
            getResponseOutputStream().write(responseBody.getBytes());
        } catch (Throwable th) {
            App.error(new ForwardException(th));
        }
        completableFuture.complete(null);
    }

    public void responseComplete(InputStream inputStream) {
        try {
            OutputStream out = getResponseOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            inputStream.close();
            out.flush();
        } catch (Throwable th) {
            App.error(new ForwardException(th));
        }
        completableFuture.complete(null);
    }

    public CompletableFuture<Void> getServletResponse() {
        return completableFuture;
    }

    public void setResponseError(String cause) {
        setResponseBodyFromMap(new HashMapBuilder<>().append("status", false).append("cause", cause));
    }

    public void setResponseError(Object cause) {
        setResponseBodyFromMap(new HashMapBuilder<>().append("status", false).append("cause", cause));
    }

    public void setResponseUnauthorized() {
        setResponseUnauthorized(response);
    }

    public static void setResponseUnauthorized(HttpServletResponse response) {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader("WWW-Authenticate", "Basic realm=\"" + App.applicationName + "\"");
        try {
            response.getWriter().print("<html><body><h1>401. Unauthorized</h1></body>");
        } catch (Throwable th) {
            App.error(th);
        }
    }

}
