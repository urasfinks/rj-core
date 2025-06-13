package ru.jamsys.core.resource.http.client;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ExceptionHandler;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Accessors(chain = true)
public class HttpResponse {

    protected Throwable exception = null;

    @Setter
    private HttpStatus status = HttpStatus.OK;

    private final Map<String, String> headers = new LinkedHashMap<>();

    private byte[] body = null;

    private long timing;

    private Charset charset = StandardCharsets.UTF_8;

    @JsonValue
    public HashMapBuilder<String, Object> getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("statusDesc", status)
                .append("headers", headers)
                .append("body", body == null ? null : new String(body, charset))
                .append("timing", timing)
                .append("exception", exception == null
                        ? null
                        : App.get(ExceptionHandler.class).getExceptionObject(exception, null)
                );
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void addException(Throwable e) {
        status = HttpStatus.EXPECTATION_FAILED;
        exception = e;
    }

    public boolean isSuccess() {
        return exception == null && status.is2xxSuccessful();
    }

    public static HttpResponse instanceOf(
            int status,
            Map<String, List<String>> headerResponse,
            byte[] body,
            Exception exception,
            long timing
    ) {
        HttpResponse httpResponse = new HttpResponse();
        try {
            if (exception != null) {
                String exceptionDescription = switch (status) {
                    case -2 -> "Запроса не было";
                    case -1 -> "Ошибка в моменте выполнения запроса";
                    default -> "Неизвестный код: " + status;
                };
                httpResponse.addException(new ForwardException(exceptionDescription, exception));
            } else {
                httpResponse.setStatus(HttpStatus.valueOf(status));
                httpResponse.setBody(body);
                if (headerResponse != null) {
                    for (String key : headerResponse.keySet()) {
                        List<String> strings = headerResponse.get(key);
                        httpResponse.addHeader(key, String.join("; ", strings));
                    }
                }
            }
            httpResponse.setTiming(timing);
        } catch (Exception e) {
            httpResponse.setException(e);
        }
        return httpResponse;
    }

}
