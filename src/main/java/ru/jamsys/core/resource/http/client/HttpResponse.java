package ru.jamsys.core.resource.http.client;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import ru.jamsys.core.extension.trace.Trace;
import ru.jamsys.core.flat.util.UtilJson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@JsonPropertyOrder({"status", "description", "httpStatus", "headers", "body", "exception"})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HttpResponse {

    @Setter
    private String description;

    //Только получение, если хотите изменить статус - вызывайте addException и указывайте причину
    private boolean status = true;

    protected List<Trace<String, Throwable>> exception = new ArrayList<>();

    @Setter
    private HttpStatus httpStatus = HttpStatus.OK;

    private final Map<String, String> headers = new LinkedHashMap<>();

    @Setter
    private String body = null;

    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    public void addException(String e) {
        description = e;
        addException(new RuntimeException(e));
    }

    public void addException(Throwable e) {
        description = e.getMessage();
        status = false;
        httpStatus = HttpStatus.EXPECTATION_FAILED;
        exception.add(new Trace<>(description, e));
    }

    @Override
    public String toString() {
        return UtilJson.toStringPretty(this, "{}");
    }

}
