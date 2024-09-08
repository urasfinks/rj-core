package ru.jamsys.core;

import jakarta.servlet.ServletContext;
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
import ru.jamsys.core.extension.property.PropertiesAgent;
import ru.jamsys.core.extension.property.repository.RepositoryPropertiesMap;
import ru.jamsys.core.flat.util.*;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;
import ru.jamsys.core.web.http.HttpHandler;
import ru.jamsys.core.web.http.HttpInterceptor;

import java.io.File;
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

    private final RepositoryPropertiesMap<String> ignoreStaticFile = new RepositoryPropertiesMap<>(String.class);

    private final RepositoryPropertiesMap<String> ignoreStaticDir = new RepositoryPropertiesMap<>(String.class);

    private final Set<String> ignoredStaticFile = new HashSet<>();

    private final ServiceProperty serviceProperty;

    public HttpController(ApplicationContext applicationContext, ServiceClassFinder serviceClassFinder, ServiceProperty serviceProperty) {
        this.serviceProperty = serviceProperty;
        fill(path, applicationContext, serviceClassFinder, HttpHandler.class);
        if (serviceProperty.get(Boolean.class, "run.args.web", false)) {
            updateStaticFile();
            subscribeIgnoreFile();
            subscribeIgnoreDir();
            Util.logConsole("IgnoredStaticFile: " + UtilJson.toStringPretty(ignoredStaticFile, "{}"));
        }
        List<Class<HttpInterceptor>> list = serviceClassFinder.findByInstance(HttpInterceptor.class);
        if (!list.isEmpty()) {
            httpInterceptor = serviceClassFinder.instanceOf(list.getFirst());
        }
    }

    private void subscribeIgnoreFile() {
        PropertiesAgent ignoredClassAgent = serviceProperty.getFactory().getPropertiesAgent(
                mapAlias -> {
                    Util.logConsole("IgnoreWebStatic.File: " + UtilJson.toStringPretty(mapAlias, "{}"));
                    updateStaticFile();
                },
                ignoreStaticFile,
                "run.args.web.static.file.ignore.file",
                false
        );
        ignoredClassAgent.series("run\\.args\\.web\\.static\\.file\\.ignore\\.file.*");
    }

    private void subscribeIgnoreDir() {
        PropertiesAgent ignoredClassAgent = serviceProperty.getFactory().getPropertiesAgent(
                mapAlias -> {
                    Util.logConsole("IgnoreWebStatic.Dir: " + UtilJson.toStringPretty(mapAlias, "{}"));
                    updateStaticFile();
                },
                ignoreStaticDir,
                "run.args.web.static.file.ignore.dir",
                false
        );
        ignoredClassAgent.series("run\\.args\\.web\\.static\\.file\\.ignore\\.dir.*");
    }

    private void updateStaticFile() {
        staticFile.clear();
        String location = serviceProperty.get("run.args.web.resource.location");
        String absPath = new File(location).getAbsolutePath();
        List<String> filesRecursive = UtilFile.getFilesRecursive(location);
        filesRecursive.forEach(s -> staticFile.put(s.substring(absPath.length()), s));

        UtilRisc.forEach(null, ignoreStaticFile.getMapRepositoryTyped(), (s, stringRepositoryMapValue) -> {
            String excludeFile = stringRepositoryMapValue.getValue();
            UtilRisc.forEach(null, staticFile, (key, value) -> {
                if (key.equals(excludeFile)) {
                    ignoredStaticFile.add(staticFile.remove(excludeFile));
                }
            });
        });
        UtilRisc.forEach(null, ignoreStaticDir.getMapRepositoryTyped(), (s, stringRepositoryMapValue) -> {
            String excludeDir = stringRepositoryMapValue.getValue();
            UtilRisc.forEach(null, staticFile, (key, value) -> {
                if (key.startsWith(excludeDir)) {
                    ignoredStaticFile.add(staticFile.remove(key));
                }
            });
        });
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
        String mimeType = getMimeType(request.getServletContext(), request.getRequestURI());
        response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
    }

    public static String getMimeType(ServletContext context, String path) {
        return context.getMimeType(path);
    }

    @RequestMapping(value = "/**")
    public CompletableFuture<Void> handler(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        ServletHandler servletHandler = new ServletHandler(new CompletableFuture<>(), request, response);
        try {
            if (httpInterceptor != null && !httpInterceptor.handle(request, response)) {
                return null;
            }
            String uri = request.getRequestURI();
            if (staticFile.containsKey(uri)) {
                servletHandler.writeFileToOutput(new File(staticFile.get(uri)));
                return null;
            }
            servletHandler.init();
            PromiseGenerator promiseGenerator = getGeneratorByUri(uri);
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
                    ServletHandler srvHandler = p.getRepositoryMapClass(ServletHandler.class);
                    srvHandler.setResponseBody(p.getLogString());
                    srvHandler.responseComplete();
                });
            }
            if (!promise.isSetCompleteHandler()) {
                promise.onComplete((atomicBoolean, p) -> {
                    ServletHandler srvHandler = p.getRepositoryMapClass(ServletHandler.class);
                    if (srvHandler.isEmptyBody()) {
                        srvHandler.setBodyIfEmpty(p.getLogString());
                    }
                    srvHandler.responseComplete();
                });
            }
            promise.setRepositoryMapClass(ServletHandler.class, servletHandler);
            promise.run();
        } catch (Throwable th) {
            App.error(th);
            servletHandler.setResponseError(th.getMessage());
            servletHandler.responseComplete();
            return null;
        }
        return servletHandler.getServletResponse();
    }

}
