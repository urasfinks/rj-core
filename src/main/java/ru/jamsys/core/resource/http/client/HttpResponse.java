package ru.jamsys.core.resource.http.client;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;
import ru.jamsys.core.extension.builder.HashMapBuilder;

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
                .append("body", new String(body, charset))
                .append("timing", timing)
                .append("exception", exception);
    }

    public void setStatusDesc(HttpStatus statusDesc) {
        this.statusDesc = statusDesc;
        this.statusCode = statusDesc.value();
    }

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void addException(String e) {
        addException(new RuntimeException(e));
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
        if (exception != null) {
            httpResponse.addException(exception);
        }
        httpResponse.setStatusCode(status);
        if (status == -1) {
            httpResponse.addException("Запроса не было");
        } else {
            httpResponse.setStatusDesc(HttpStatus.valueOf(status));
        }
        if (httpResponse.getException() == null) {
            try {
                httpResponse.setBody(body);
                if (headerResponse != null) {
                    for (String key : headerResponse.keySet()) {
                        List<String> strings = headerResponse.get(key);
                        httpResponse.addHeader(key, String.join("; ", strings));
                    }
                }
            } catch (Exception e) {
                httpResponse.addException(e);
            }
        }
        httpResponse.setTiming(timing);
        return httpResponse;
    }

}
