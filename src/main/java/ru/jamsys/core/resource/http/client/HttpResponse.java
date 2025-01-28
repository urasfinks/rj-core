package ru.jamsys.core.resource.http.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.http.HttpStatus;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@JsonPropertyOrder({"statusCode", "statusDesc", "timing", "exception", "body", "headers"})
@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Accessors(chain = true)
public class HttpResponse {

    protected Throwable exception = null;

    private int statusCode;

    private HttpStatus statusDesc = HttpStatus.OK;

    private final Map<String, String> headers = new LinkedHashMap<>();

    private String body = null;

    private long timing;

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

    @Override
    public String toString() {
        return UtilJson.toStringPretty(this, "{}");
    }

}
