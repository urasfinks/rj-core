package ru.jamsys.core.flat.util;

import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class UtilUri {

    public static String build(String path, Map<String, ?> getParameters) {
        List<String> part = new ArrayList<>();
        if (!getParameters.isEmpty()) {
            getParameters.forEach((key, element) -> {
                if (element instanceof List) {
                    @SuppressWarnings("all")
                    List<Object> list = (List<Object>) element;
                    list.forEach(s1 -> {
                        try {
                            part.add(key + "=" + encode(String.valueOf(s1)));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    try {
                        part.add(key + "=" + encode(String.valueOf(element)));
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

    public static Map<String, List<String>> parseParameters(String uri) {
        return new LinkedHashMap<>(UriComponentsBuilder.fromUriString(uri).build().getQueryParams());
    }

    public static Map<String, String> parseParameters(String uri, Function<List<String>, String> reduce) {
        Map<String, String> result = new LinkedHashMap<>();
        UriComponentsBuilder
                .fromUriString(uri)
                .build()
                .getQueryParams()
                .forEach((key, listString) -> result.put(key, reduce.apply(listString)));
        return result;
    }

    public static String encode(String data, String charset) throws Exception {
        return URLEncoder.encode(data, charset);
    }

    public static String encode(String data) throws Exception {
        return encode(data, Util.defaultCharset);
    }

    public static String decode(String data, String charset) throws Exception {
        return URLDecoder.decode(data, charset);
    }

    public static String decode(String data) throws Exception {
        return decode(data, Util.defaultCharset);
    }

}
