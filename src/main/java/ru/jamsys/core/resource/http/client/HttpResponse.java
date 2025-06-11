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

    private int statusCode;

    private HttpStatus statusDesc = HttpStatus.OK;

    private final Map<String, String> headers = new LinkedHashMap<>();

    private byte[] body = null;

    private long timing;

    private Charset charset = StandardCharsets.UTF_8;

    @JsonValue
    public HashMapBuilder<String, Object> getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("statusCode", statusCode)
                .append("statusDesc", statusDesc)
                .append("headers", headers)
                .append("body", body == null ? null : new String(body, charset))
                .append("timing", timing)
                .append("exception", App.get(ExceptionHandler.class).getLog(exception, null).getRawBody());
    }

    public void setStatusDesc(HttpStatus statusDesc) {
        this.statusDesc = statusDesc;
        this.statusCode = statusDesc.value();
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void addException(Throwable e) {
        statusDesc = HttpStatus.EXPECTATION_FAILED;
        statusCode = HttpStatus.EXPECTATION_FAILED.value();
        exception = e;
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
                httpResponse.setStatusCode(status);
                httpResponse.setStatusDesc(HttpStatus.valueOf(status));
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
