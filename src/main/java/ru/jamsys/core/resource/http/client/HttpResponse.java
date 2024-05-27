package ru.jamsys.core.resource.http.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public String description;

    //Только получение, если хотите изменить статус - вызывайте addException и указывайте причину
    public boolean status = true;

    protected List<Trace<String, Throwable>> exception = new ArrayList<>();

    @Setter
    public HttpStatus httpStatus = HttpStatus.OK;

    public Map<String, String> headers = new LinkedHashMap<>();

    @Setter
    public String body = null;

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

    public void setUnauthorized() {
        addException(new RuntimeException("Unauthorized"));
        httpStatus = HttpStatus.UNAUTHORIZED;
        headers.clear();
        headers.put("WWW-Authenticate", "Basic realm=\"JamSys\"");
        setBody("<html><body><h1>401. Unauthorized</h1></body>");
    }

    // source = true - кишки вернёт
    // source = false - чистое тело
    @JsonIgnore
    public ResponseEntity<?> getResponseEntity(boolean source) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(httpStatus);
        HttpHeaders httpHeaders = new HttpHeaders();
        for (String key : headers.keySet()) {
            httpHeaders.add(key, headers.get(key));
        }
        builder.headers(httpHeaders);
        return builder.body(source ? toString() : body);
    }

    @Override
    public String toString() {
        return UtilJson.toStringPretty(this, "{}");
    }

}
