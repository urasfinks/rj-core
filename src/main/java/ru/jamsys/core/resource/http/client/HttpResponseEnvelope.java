package ru.jamsys.core.resource.http.client;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import ru.jamsys.core.flat.util.UtilJson;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HttpResponseEnvelope {

    @Getter
    @Setter
    public String description;

    @Getter //Только получение, если хотите изменить статус - вызывайте addException и указывайте причину
    public boolean status = true;

    @Getter
    @Setter
    public Map<String, Object> data = new LinkedHashMap<>();
    public List<String> exception = new ArrayList<>();

    @Getter
    @Setter
    public HttpStatus httpStatus = HttpStatus.OK;

    @Getter
    public Map<String, String> headers = new LinkedHashMap<>();

    public boolean rawBody = false;
    public String body = "";

    public void setRawBody(String body) {
        rawBody = true;
        this.body = body;
    }

    @SuppressWarnings("unused")
    public void addData(String key, Object value) {
        data.put(key, value);
    }

    @SuppressWarnings("unused")
    public void addHeader(String key, String value) {
        headers.put(key, value);
    }

    @SuppressWarnings("unused")
    public void addException(String e) {
        description = e;
        addException(new RuntimeException(e));
    }

    @SuppressWarnings("unused")
    public void addException(Exception e) {
        description = e.getClass().getName() + ": " + e.getMessage();
        status = false;
        httpStatus = HttpStatus.EXPECTATION_FAILED;
        exception.add(LocalDateTime.now() + " " + getStringError(e));
    }

    public String getStringError(Exception e) {
        e.printStackTrace();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        return sw.toString(); // stack trace as a string
    }

    @Override
    public String toString() {
        return UtilJson.toStringPretty(this, "{}");
    }

    @SuppressWarnings("unused")
    public void setUnauthorized() {
        addException(new RuntimeException("Unauthorized"));
        httpStatus = HttpStatus.UNAUTHORIZED;
        headers.clear();
        headers.put("WWW-Authenticate", "Basic realm=\"JamSys\"");
        setRawBody("<html><body><h1>401. Unauthorized</h1></body>");
    }

    @SuppressWarnings("unused")
    @JsonIgnore
    public ResponseEntity<?> getResponseEntity() {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(httpStatus);
        HttpHeaders httpHeaders = new HttpHeaders();
        for (String key : headers.keySet()) {
            httpHeaders.add(key, headers.get(key));
        }
        builder.headers(httpHeaders);
        return builder.body(rawBody ? body : toString());
    }

}
