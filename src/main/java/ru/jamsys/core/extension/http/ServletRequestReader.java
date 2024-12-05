package ru.jamsys.core.extension.http;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.ToString;
import org.springframework.web.util.UriComponentsBuilder;
import ru.jamsys.core.extension.exception.AuthException;
import ru.jamsys.core.extension.functional.BiConsumerThrowing;
import ru.jamsys.core.flat.util.Util;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString(onlyExplicitlyIncluded = true)
public class ServletRequestReader {

    @JsonIgnore
    private static final String[] ipHeaders = {
            "X-Forwarded-For",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    @JsonIgnore
    private final HttpServletRequest request;

    @Getter
    @ToString.Include
    public String data;

    @Getter
    @ToString.Include
    private final Map<String, String> map = new LinkedHashMap<>();

    @Getter
    private final Map<String, String> headers = new LinkedHashMap<>();

    private final String remoteAddr;

    @JsonIgnore
    public byte[] bytes;

    @JsonIgnore
    public ServletRequestReader(@Nonnull HttpServletRequest request) throws ServletException, IOException {
        this.request = request;
        remoteAddr = request.getRemoteAddr();
        readHeaders();
        read();
    }

    @JsonIgnore
    public Map<String, String> getMapEscapedHtmlSpecialChars() {
        HashMap<String, String> result = new HashMap<>();
        map.forEach((s, s2) -> result.put(s, Util.htmlEntity(s2)));
        return result;
    }

    @JsonIgnore
    public void read() throws IOException, ServletException {
        String contentType = request.getHeader("content-type");
        request.getParameterMap().forEach((s, strings) -> map.put(s, String.join(", ", strings)));
        if (contentType == null || contentType.contains("text/plain")) {
            readTextPlain(contentType);
        } else if (contentType.contains("multipart/form-data")) {
            readMultiPartFormData();
        } else if (contentType.contains("application/x-www-form-urlencoded")) {
            readFormUrlEncoded();
        } else if (contentType.contains("application/octet-stream")) {
            bytes = request.getInputStream().readAllBytes();
        }else{
            readTextPlain(contentType);
        }
    }

    @JsonIgnore
    public void readMultiPartFormData() throws ServletException, IOException {
        for (Part part : request.getParts()) {
            this.map.put(
                    part.getName(),
                    part.getContentType() == null
                            ? new String(part.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                            : "[bytes] for read use: getMultiPartFormData(_, key)"
            );
        }
    }

    @JsonIgnore
    public Map<String, String> getMultiPartFormSubmittedFileName() throws ServletException, IOException {
        Map<String, String> result = new LinkedHashMap<>();
        for (Part part : request.getParts()) {
            result.put(
                    part.getName(),
                    part.getSubmittedFileName()
            );
        }
        return result;
    }

    @JsonIgnore
    public InputStream getMultiPartFormData(String key) throws ServletException, IOException {
        return request.getPart(key).getInputStream();
    }

    @JsonIgnore
    public void readFormUrlEncoded() {
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String values = String.join(" ", request.getParameterValues(name));
            map.put(name, URLDecoder.decode(values, StandardCharsets.UTF_8));
        }
    }

    @JsonIgnore
    public String getContentCharset(String contentType) {
        String result = "UTF-8";
        if (contentType != null && contentType.contains("charset=")) {
            String parsedCharset = contentType.substring(contentType.indexOf("charset=") + 8);
            if (Charset.isSupported(parsedCharset)) {
                result = parsedCharset;
            }
        }
        return result;
    }

    @JsonIgnore
    public void readTextPlain(String contentType) throws IOException {
        data = new String(request.getInputStream().readAllBytes(), getContentCharset(contentType));
    }

    public String getClientIp() {
        for (String header : ipHeaders) {
            String ip = headers.get(header);
            if ((ip != null) && (!ip.isEmpty()) && !"unknown".equalsIgnoreCase(ip))
                return ip;
        }
        return remoteAddr;
    }

    private void readHeaders() {
        Enumeration<String> requestHeader = request.getHeaderNames();
        if (requestHeader != null) {
            while (requestHeader.hasMoreElements()) {
                String key = requestHeader.nextElement();
                if (key != null) {
                    headers.put(key, request.getHeader(key));
                }
            }
        }
    }

    @JsonIgnore
    public void basicAuthHandler(BiConsumerThrowing<String, String> handler) throws Throwable {
        String authorization = request.getHeader("Authorization");
        basicAuthHandler(authorization, handler);
    }

    @JsonIgnore
    public static void basicAuthHandler(String authorization, BiConsumerThrowing<String, String> handler) throws Throwable {
        if (authorization == null) {
            throw new AuthException("Authorization header is null");
        }
        if (!authorization.startsWith("Basic ")) {
            throw new AuthException("Authorization header is not Basic");
        }
        String base64Decoded = new String(Base64.getDecoder().decode(authorization.substring(6)), StandardCharsets.UTF_8);
        if (!base64Decoded.contains(":")) {
            throw new AuthException("Error parsing");
        }
        String user = base64Decoded.substring(0, base64Decoded.indexOf(":"));
        String password = base64Decoded.substring(base64Decoded.indexOf(":") + 1);
        handler.accept(user, password);
    }

    public static Map<String, List<String>> parseUriParameters(String uri) {
        return new LinkedHashMap<>(UriComponentsBuilder.fromUriString(uri).build().getQueryParams());
    }

}
