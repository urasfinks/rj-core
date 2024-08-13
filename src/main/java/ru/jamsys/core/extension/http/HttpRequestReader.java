package ru.jamsys.core.extension.http;


import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import lombok.Getter;
import lombok.ToString;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@ToString(onlyExplicitlyIncluded = true)
public class HttpRequestReader {

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

    private final HttpServletRequest request;

    @Getter
    @ToString.Include
    public String data;

    @Getter
    @ToString.Include
    public Map<String, String> map = new LinkedHashMap<>();

    public byte[] bytes;

    public HttpRequestReader(@Nonnull HttpServletRequest request) throws ServletException, IOException {
        this.request = request;
        read();
    }

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

    public InputStream getMultiPartFormData(String key) throws ServletException, IOException {
        return request.getPart(key).getInputStream();
    }

    public void readFormUrlEncoded() {
        Enumeration<String> parameterNames = request.getParameterNames();
        while (parameterNames.hasMoreElements()) {
            String name = parameterNames.nextElement();
            String values = String.join(" ", request.getParameterValues(name));
            map.put(name, URLDecoder.decode(values, StandardCharsets.UTF_8));
        }
    }

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

    public void readTextPlain(String contentType) throws IOException {
        data = new String(request.getInputStream().readAllBytes(), getContentCharset(contentType));
    }

    public String getClientIp() {
        for (String header : ipHeaders) {
            String ip = request.getHeader(header);
            if ((ip != null) && (!ip.isEmpty()) && !"unknown".equalsIgnoreCase(ip))
                return ip;
        }
        return request.getRemoteAddr();
    }

    public Map<String, String> getHeaders() {
        Map<String, String> result = new HashMap<>();
        Enumeration<String> requestHeader = request.getHeaderNames();
        if (requestHeader != null) {
            while (requestHeader.hasMoreElements()) {
                String key = requestHeader.nextElement();
                if (key != null) {
                    result.put(key, request.getHeader(key));
                }
            }
        }
        return result;
    }

}
