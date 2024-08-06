package ru.jamsys.core;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.extension.http.HttpAsyncResponse;
import ru.jamsys.core.flat.util.ListSort;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
@RestController
public class HttpController {

    private final Map<String, PromiseGenerator> path = new LinkedHashMap<>();

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    public HttpController(ApplicationContext applicationContext, ServiceClassFinder serviceClassFinder) {
        fill(path, applicationContext, serviceClassFinder, HttpHandler.class);
    }

    public static void fill(Map<String, PromiseGenerator> path, ApplicationContext applicationContext, ServiceClassFinder serviceClassFinder, Class<?> iFaceMatcher) {
        /*
        {
          "[/*]" : "SecondHandler",
          "[/hello/*]" : "FirstHandler"
        }

        При таком раскладе первое правило будет подходить для всех типов запросов, поэтому результирующую выборку
        отсортируем
        * */
        Map<String, String> info = new LinkedHashMap<>();
        Map<String, PromiseGenerator> tmp = new HashMap<>();
        serviceClassFinder.findByInstance(PromiseGenerator.class).forEach(promiseGeneratorClass -> {
            if (!ServiceClassFinder.instanceOf(promiseGeneratorClass, iFaceMatcher)) {
                return;
            }
            for (Annotation annotation : promiseGeneratorClass.getAnnotations()) {
                if (ServiceClassFinder.instanceOf(annotation.annotationType(), RequestMapping.class)) {
                    PromiseGenerator promiseGenerator = applicationContext.getBean(promiseGeneratorClass);
                    promiseGenerator.setIndex(UniqueClassNameImpl.getClassNameStatic(
                            promiseGeneratorClass,
                            null,
                            applicationContext
                    ));
                    String[] values = promiseGeneratorClass.getAnnotation(RequestMapping.class).value();
                    for (String value : values) {
                        tmp.put(value, promiseGenerator);
                    }
                    break;
                }
            }
        });
        ListSort.sortDesc(new ArrayList<>(tmp.keySet())).forEach(s -> {
            info.put(s, tmp.get(s).getIndex());
            path.put(s, tmp.get(s));
        });

        Util.logConsole("RequestMapping(" + iFaceMatcher.getSimpleName() + ") : " + UtilJson.toStringPretty(info, "[]"));
    }

    private PromiseGenerator getGeneratorByUri(String requestUri) {
        for (String pattern : path.keySet()) {
            if (antPathMatcher.match(pattern, requestUri)) {
                return path.get(pattern);
            }
        }
        return null;
    }

    @RequestMapping(value = "/**")
    public CompletableFuture<Void> handler(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {
        PromiseGenerator promiseGenerator = getGeneratorByUri(request.getRequestURI());
        HttpAsyncResponse httpAsyncResponse = new HttpAsyncResponse(new CompletableFuture<>(), request, response);
        if (promiseGenerator == null) {
            httpAsyncResponse.setError("Generator not found");
            httpAsyncResponse.complete();
            return httpAsyncResponse.getServletResponse();
        }
        Promise promise = promiseGenerator.generate();

        if (promise == null) {
            httpAsyncResponse.setError("Promise is null");
            httpAsyncResponse.complete();
            return httpAsyncResponse.getServletResponse();
        }

        if (!promise.isSetErrorHandler()) {
            promise.onError((atomicBoolean, p) -> {
                HttpAsyncResponse ar = p.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class);
                ar.setError(p.getException().getMessage());
                ar.complete();
            });
        }
        if (!promise.isSetCompleteHandler()) {
            promise.onComplete((atomicBoolean, p)
                    -> p.getRepositoryMap("HttpAsyncResponse", HttpAsyncResponse.class).complete());
        }
        promise.setMapRepository("HttpAsyncResponse", httpAsyncResponse);
        promise.run();
        return httpAsyncResponse.getServletResponse();
    }

}
