package ru.jamsys.core.extension.http;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import ru.jamsys.core.App;
import ru.jamsys.core.HttpController;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServletHandler {

    @JsonIgnore
    @Getter
    private final CompletableFuture<Void> completableFuture;


    @JsonIgnore
    @Getter
    private final HttpServletRequest request;

    @JsonIgnore
    private final HttpServletResponse response;

    @Getter
    private ServletRequestReader requestReader;

    // Флаги управления жизненным циклом
    @JsonIgnore
    private final AtomicBoolean completed = new AtomicBoolean(false);

    @SuppressWarnings("all")
    public boolean isCompleted() {
        return completed.get();
    }

    public ServletHandler(CompletableFuture<Void> completableFuture, HttpServletRequest request, HttpServletResponse response) {
        this.completableFuture = completableFuture;
        this.request = request;
        this.response = response;
    }

    @Getter
    private final HashMapBuilder<String, String> responseHeader = new HashMapBuilder<>();

    @Getter
    @Setter
    private HttpStatus responseHttpStatus = HttpStatus.OK;

    public void init() throws ServletException, IOException {
        requestReader = new ServletRequestReader(request);
    }

    public void send(String data, Charset charset) {
        if (completed.compareAndSet(false, true)) {
            try {
                response.setStatus(responseHttpStatus.value());
                responseHeader.forEach(response::setHeader);
                try (ServletOutputStream so = response.getOutputStream()) {
                    so.write(data.getBytes(charset));
                    so.flush();
                }
            } catch (Throwable th) {
                App.error(th, this);
            } finally {
                completableFuture.complete(null);
            }
        }
    }

    public void send(InputStream data) {
        if (completed.compareAndSet(false, true)) {
            try {
                response.setStatus(responseHttpStatus.value());
                responseHeader.forEach(response::setHeader);
                try (ServletOutputStream out = response.getOutputStream(); InputStream in = data) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                    out.flush();
                }
            } catch (Throwable th) {
                App.error(th, this);
            } finally {
                completableFuture.complete(null);
            }
        }
    }

    public void send(File file) {
        if (completed.compareAndSet(false, true)) {
            try {
                String mimeType = HttpController.getMimeType(request.getServletContext(), file.getAbsolutePath());
                response.setStatus(responseHttpStatus.value());
                response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
                try (InputStream in = new FileInputStream(file);
                     OutputStream out = response.getOutputStream()) {
                    in.transferTo(out);
                    out.flush();
                }
            } catch (Throwable th) {
                App.error(th, this);
            } finally {
                completableFuture.complete(null);
            }
        }
    }

    public void sendUnauthorized() {
        responseHeader.append("WWW-Authenticate", "Basic realm=\"" + App.applicationName + "\"");
        setResponseHttpStatus(HttpStatus.UNAUTHORIZED);
        send("<html><body><h1>401. Unauthorized</h1></body>", StandardCharsets.UTF_8);
    }

    public void sendErrorJson(HttpStatus httpStatus, String cause) {
        setResponseHttpStatus(httpStatus);
        responseHeader.append("Content-Type", "application/json");
        String body = UtilJson.toStringPretty(
                new HashMapBuilder<>()
                        .append("status", false)
                        .append("cause", cause),
                "{}"
        );
        send(body, StandardCharsets.UTF_8);
    }

    public void sendErrorJson(String cause) {
        sendErrorJson(HttpStatus.BAD_REQUEST, cause);
    }

    public void sendSuccessJson(HttpStatus httpStatus, Map<String, Object> data) {
        setResponseHttpStatus(httpStatus);
        responseHeader.append("Content-Type", "application/json");
        String body = UtilJson.toStringPretty(
                new HashMapBuilder<>()
                        .append("status", true)
                        .append("data", data),
                "{}"
        );
        send(body, StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unused")
    public void sendSuccessJson(Map<String, Object> data) {
        sendSuccessJson(HttpStatus.OK, data);
    }

}
