package ru.jamsys.core.extension.http;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletResponse;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import ru.jamsys.core.App;
import ru.jamsys.core.flat.util.Util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString(onlyExplicitlyIncluded = true)
public class ServletResponseWriter {

    public static String buildUrlQuery(String path, Map<String, ?> getParameters) {
        List<String> part = new ArrayList<>();
        if (!getParameters.isEmpty()) {
            getParameters.forEach((key, element) -> {
                if (element instanceof List) {
                    @SuppressWarnings("all")
                    List<Object> list = (List<Object>) element;
                    list.forEach(s1 -> {
                        try {
                            part.add(key + "=" + Util.urlEncode(String.valueOf(s1)));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                }else{
                    try {
                        part.add(key + "=" + Util.urlEncode(String.valueOf(element)));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        return path + (
                getParameters.isEmpty()
                        ? ""
                        : "?" + String.join("&", part)
        );
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
