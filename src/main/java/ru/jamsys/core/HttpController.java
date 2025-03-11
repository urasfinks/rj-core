package ru.jamsys.core;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.jamsys.core.component.RouteGenerator;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.component.manager.item.RouteGeneratorRepository;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.repository.PropertyRepositoryMap;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilFile;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilRisc;
import ru.jamsys.core.handler.web.http.HttpHandler;
import ru.jamsys.core.handler.web.http.HttpInterceptor;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
@RestController
public class HttpController {

    private final Map<String, String> staticFile = new HashMap<>();

    private HttpInterceptor httpInterceptor;

    private final PropertyRepositoryMap<String> ignoreStaticFile = new PropertyRepositoryMap<>(String.class);

    private final PropertyRepositoryMap<String> ignoreStaticDir = new PropertyRepositoryMap<>(String.class);

    private final Set<String> ignoredStaticFile = new HashSet<>();

    private final ServiceProperty serviceProperty;

    private final RouteGeneratorRepository routeGeneratorRepository;

    public HttpController(
            ServiceClassFinder serviceClassFinder,
            ServiceProperty serviceProperty,
            RouteGenerator routeGenerator
    ) {
        this.serviceProperty = serviceProperty;
        routeGeneratorRepository = routeGenerator.getRouterRepository(HttpHandler.class);

        if (serviceProperty.computeIfAbsent("run.args.web", false).get(Boolean.class)) {
            updateStaticFile();
            subscribeIgnoreFile();
            subscribeIgnoreDir();
            Util.logConsole(
                    getClass(),
                    "IgnoredStaticFile: " + UtilJson.toStringPretty(ignoredStaticFile, "[]")
            );
        }
        List<Class<HttpInterceptor>> list = serviceClassFinder.findByInstance(HttpInterceptor.class);
        if (!list.isEmpty()) {
            httpInterceptor = serviceClassFinder.instanceOf(list.getFirst());
        }
    }

    private void subscribeIgnoreFile() {
        new PropertyDispatcher(
                serviceProperty,
                (key, _, property) -> {
                    Util.logConsole(getClass(), "IgnoreWebStatic.File: " + property.get());
                    updateStaticFile();
                },
                ignoreStaticFile,
                "run.args.web.static.file.ignore.file"
        )
                .addSubscriptionRegexp("run\\.args\\.web\\.static\\.file\\.ignore\\.file.*")
                .run();
    }

    private void subscribeIgnoreDir() {
        new PropertyDispatcher(
                serviceProperty,
                (key, _, property) -> {
                    Util.logConsole(getClass(), "IgnoreWebStatic.Dir: " + property.get());
                    updateStaticFile();
                },
                ignoreStaticDir,
                "run.args.web.static.file.ignore.dir"
        )
                .addSubscriptionRegexp("run\\.args\\.web\\.static\\.file\\.ignore\\.dir.*")
                .run();
    }

    private void updateStaticFile() {
        staticFile.clear();
        String location = serviceProperty.computeIfAbsent(
                "run.args.web.resource.location",
                "web/"
        ).get();
        String absPath = new File(location).getAbsolutePath();
        List<String> filesRecursive = UtilFile.getFilesRecursive(location);
        filesRecursive.forEach(s -> staticFile.put(s.substring(absPath.length()), s));

        UtilRisc.forEach(null, ignoreStaticFile.getMapRepository(), (_, excludeFile) -> {
            UtilRisc.forEach(null, staticFile, (key, value) -> {
                if (key.equals(excludeFile)) {
                    ignoredStaticFile.add(staticFile.remove(excludeFile));
                }
            });
        });
        UtilRisc.forEach(null, ignoreStaticDir.getMapRepository(), (_, excludeDir) -> {
            UtilRisc.forEach(null, staticFile, (key, value) -> {
                if (key.startsWith(excludeDir)) {
                    ignoredStaticFile.add(staticFile.remove(key));
                }
            });
        });
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
            PromiseGenerator promiseGenerator = routeGeneratorRepository.match(uri);
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
                promise.onError((_, _, p) -> {
                    ServletHandler srvHandler = p.getRepositoryMapClass(ServletHandler.class);
                    srvHandler.setResponseBody(p.getLogString());
                    srvHandler.responseComplete();
                });
            }
            if (!promise.isSetCompleteHandler()) {
                promise.onComplete((_, _, p) -> {
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
