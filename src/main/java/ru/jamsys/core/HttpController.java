package ru.jamsys.core;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.ApplicationContext;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.flat.util.ListSort;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.core.web.http.HttpInterceptor;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
@RestController
public class HttpController {

    private final Map<String, PromiseGenerator> path = new LinkedHashMap<>();

    private final Map<String, String> staticFile = new HashMap<>();

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    private HttpInterceptor httpInterceptor;

    public HttpController(ApplicationContext applicationContext, ServiceClassFinder serviceClassFinder, ServiceProperty serviceProperty) {
        fill(path, applicationContext, serviceClassFinder, HttpHandler.class);
        if (serviceProperty.get(Boolean.class, "run.args.web", false)) {
            String location = serviceProperty.get("run.args.web.resource.location");
            String absPath = new File(location).getAbsolutePath();
            List<String> filesRecursive = UtilFile.getFilesRecursive(location);
            filesRecursive.forEach(s -> staticFile.put(s.substring(absPath.length()), s));
        }
        List<Class<HttpInterceptor>> list = serviceClassFinder.findByInstance(HttpInterceptor.class);
        if (!list.isEmpty()) {
            httpInterceptor = serviceClassFinder.instanceOf(list.getFirst());
        }
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
                    if (values.length > 0) {
                        for (String value : values) {
                            tmp.put(value, promiseGenerator);
                        }
                    } else {
                        tmp.put("/" + promiseGeneratorClass.getSimpleName(), promiseGenerator);
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

    public static void setMimeType(HttpServletRequest request, HttpServletResponse response) {
        ServletContext context = request.getServletContext();
        String mimeType = context.getMimeType(request.getRequestURI());
        response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
    }

    @RequestMapping(value = "/**")
    public CompletableFuture<Void> handler(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        if (httpInterceptor != null && !httpInterceptor.handle(request, response)) {
            return null;
        }

        String uri = request.getRequestURI();

        if (staticFile.containsKey(uri)) {
            setMimeType(request, response);
            OutputStream out = response.getOutputStream();
            FileInputStream in = new FileInputStream(staticFile.get(uri));
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.flush();
            return null;
        }

        PromiseGenerator promiseGenerator = getGeneratorByUri(request.getRequestURI());
        ServletHandler servletHandler = new ServletHandler(new CompletableFuture<>(), request, response);
        if (promiseGenerator == null) {
            servletHandler.setResponseError("Generator not found");
            servletHandler.responseComplete();
            return servletHandler.getServletResponse();
        }
        Promise promise = promiseGenerator.generate();

        if (promise == null) {
            servletHandler.setResponseError("Promise is null");
            servletHandler.responseComplete();
            return servletHandler.getServletResponse();
        }

        if (!promise.isSetErrorHandler()) {
            promise.onError((atomicBoolean, p) -> {
                ServletHandler srvHandler = p.getRepositoryMap(ServletHandler.class);
                srvHandler.setResponseBody(p.getLogString());
                srvHandler.responseComplete();
            });
        }
        if (!promise.isSetCompleteHandler()) {
            promise.onComplete((atomicBoolean, p) -> {
                ServletHandler srvHandler = p.getRepositoryMap(ServletHandler.class);
                if (srvHandler.isEmptyBody()) {
                    srvHandler.setBodyIfEmpty(p.getLogString());
                }
                srvHandler.responseComplete();
            });
        }
        promise.setRepositoryMap(ServletHandler.class, servletHandler);
        promise.run();
        return servletHandler.getServletResponse();
    }

}
