package ru.jamsys.core.extension.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.App;
import ru.jamsys.core.HttpController;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ServletHandler {

    @JsonIgnore
    @Getter
    private final CompletableFuture<Void> completableFuture;

    @JsonIgnore
    @Getter
    private final HttpServletRequest request;

    @JsonIgnore
    private final HttpServletResponse response;

    @Setter
    @Getter
    private String responseContentType = "application/json";

    @Setter
    @Getter
    private String responseBody = "";

    @Getter
    private ServletRequestReader requestReader;

    public ServletHandler(
            CompletableFuture<Void> completableFuture,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        this.completableFuture = completableFuture;
        this.request = request;
        this.response = response;
    }

    public void init() throws ServletException, IOException {
        requestReader = new ServletRequestReader(request);
    }

    public void setResponseBodyFromMap(Map<?, ?> data) {
        setResponseBody(UtilJson.toStringPretty(data, "{}"));
    }

    @JsonIgnore
    public boolean isEmptyBody() {
        return this.responseBody.isEmpty();
    }

    public void setBodyIfEmpty(String body) {
        if (this.responseBody.isEmpty()) {
            this.responseBody = body;
        }
    }

    public void writeFileToOutput(File file) throws IOException {
        String mimeType = HttpController.getMimeType(request.getServletContext(), file.getAbsolutePath());
        response.setContentType(mimeType);
        OutputStream out = response.getOutputStream();
        FileInputStream in = new FileInputStream(file);
        byte[] buffer = new byte[4096];
        int length;
        while ((length = in.read(buffer)) > 0) {
            out.write(buffer, 0, length);
        }
        in.close();
        out.flush();
        getCompletableFuture().complete(null);
    }

    public void setResponseHeader(String key, String value) {
        response.setHeader(key, value);
    }

    public void setResponseStatus(int code) {
        response.setStatus(code);
    }

    private ServletOutputStream servletOutputStream;

    @JsonIgnore
    public ServletOutputStream getResponseOutputStream() throws IOException {
        if (servletOutputStream == null) {
            servletOutputStream = response.getOutputStream();
        }
        return servletOutputStream;
    }

    public void responseComplete() {
        response.setContentType(responseContentType);
        try (ServletOutputStream so = getResponseOutputStream()) {
            so.write(responseBody.getBytes());
        } catch (Throwable th) {
            App.error(new ForwardException(th));
        }
        completableFuture.complete(null);
    }

    public void responseComplete(InputStream inputStream) {
        try (OutputStream out = getResponseOutputStream()) {
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

    public void responseUnauthorized() {
        ServletResponseWriter.setResponseUnauthorized(response);
        completableFuture.complete(null);
    }

    @JsonIgnore
    public CompletableFuture<Void> getServletResponse() {
        return completableFuture;
    }

    public void responseError(String cause) {
        setResponseBodyFromMap(new HashMapBuilder<>().append("status", false).append("cause", cause));
        responseComplete();
    }

    public void response(int httpCode, String body, Charset charset) {
        setResponseStatus(httpCode);
        try (OutputStream out = getResponseOutputStream()) {
            out.write(body.getBytes(charset));
            out.flush();
        } catch (Throwable th) {
            App.error(new ForwardException(th));
        }
        completableFuture.complete(null);
    }

    public void responseError(Object cause) {
        setResponseBodyFromMap(new HashMapBuilder<>().append("status", false).append("cause", cause));
    }

}
