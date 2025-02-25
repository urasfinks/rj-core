package ru.jamsys.core.extension.http;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.http.HttpServletResponse;
import lombok.ToString;
import org.springframework.http.HttpStatus;
import ru.jamsys.core.App;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString(onlyExplicitlyIncluded = true)
public class ServletResponseWriter {

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
