package ru.jamsys.core;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.jamsys.core.component.RouteGenerator;
import ru.jamsys.core.component.SecurityComponent;
import ru.jamsys.core.component.ServiceClassFinder;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.RouteGeneratorRepository;
import ru.jamsys.core.extension.exception.AuthException;
import ru.jamsys.core.extension.http.ServletHandler;
import ru.jamsys.core.extension.property.PropertyDispatcher;
import ru.jamsys.core.extension.property.repository.RepositoryProperty;
import ru.jamsys.core.flat.util.*;
import ru.jamsys.core.flat.util.validate.ValidateType;
import ru.jamsys.core.handler.web.http.HttpHandler;
import ru.jamsys.core.handler.web.http.HttpInterceptor;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGeneratorExternalRequest;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
@RestController
public class HttpController {

    private final Map<String, String> staticFile = new HashMap<>();

    private HttpInterceptor httpInterceptor;

    private final RepositoryProperty<String> ignoreStaticFile = new RepositoryProperty<>(String.class);

    private final RepositoryProperty<String> ignoreStaticDir = new RepositoryProperty<>(String.class);

    private final Set<String> ignoredStaticFile = new HashSet<>();

    private final ServiceProperty serviceProperty;

    private final RouteGeneratorRepository routeGeneratorRepository;

    private final SecurityComponent securityComponent;

    public HttpController(
            ServiceClassFinder serviceClassFinder,
            ServiceProperty serviceProperty,
            RouteGenerator routeGenerator,
            SecurityComponent securityComponent
    ) {
        this.serviceProperty = serviceProperty;
        this.routeGeneratorRepository = routeGenerator.getRouterRepository(HttpHandler.class);
        this.securityComponent = securityComponent;

        if (serviceProperty.computeIfAbsent("run.args.web", false).get(Boolean.class)) {
            updateStaticFile();
            subscribeIgnoreFile();
            subscribeIgnoreDir();
            UtilLog.info(ignoredStaticFile)
                    .addHeader("description", "IgnoredStaticFile")
                    .print();
        }
        List<Class<HttpInterceptor>> list = serviceClassFinder.findByInstance(HttpInterceptor.class);
        if (!list.isEmpty()) {
            httpInterceptor = serviceClassFinder.instanceOf(list.getFirst());
        }
    }

    private void subscribeIgnoreFile() {
        new PropertyDispatcher<>(
                (key, _, newValue) -> {
                    UtilLog.info(newValue)
                            .addHeader("description", "IgnoreWebStatic.File")
                            .print();
                    updateStaticFile();
                },
                ignoreStaticFile,
                "run.args.web.static.file.ignore.file"
        )
                .addSubscriptionRegexp("run\\.args\\.web\\.static\\.file\\.ignore\\.file.*")
                .run();
    }

    private void subscribeIgnoreDir() {
        new PropertyDispatcher<>(
                (key, _, newValue) -> {
                    UtilLog.info(newValue)
                            .addHeader("description", "IgnoreWebStatic.Dir")
                            .print();
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

        UtilRisc.forEach(null, ignoreStaticFile.getListPropertyEnvelopeRepository(), propertyEnvelope -> {
            UtilRisc.forEach(null, staticFile, (key, value) -> {
                if (key.equals(propertyEnvelope.getValue())) {
                    ignoredStaticFile.add(staticFile.remove(propertyEnvelope.getValue()));
                }
            });
        });
        UtilRisc.forEach(null, ignoreStaticDir.getListPropertyEnvelopeRepository(), propertyEnvelope -> {
            UtilRisc.forEach(null, staticFile, (key, value) -> {
                if (key.startsWith(propertyEnvelope.getValue())) {
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
            servletHandler.init();
            if (httpInterceptor != null && !httpInterceptor.handle(servletHandler)) {
                return null;
            }
            if (serviceProperty.computeIfAbsent("run.args.web.update-static-file", false).get(Boolean.class)) {
                updateStaticFile();
            }
            String uri = request.getRequestURI();
            if (staticFile.containsKey(uri)) {
                servletHandler.writeFileToOutput(new File(staticFile.get(uri)));
                return null;
            }

            PromiseGeneratorExternalRequest promiseGenerator = routeGeneratorRepository.match(uri);
            if (promiseGenerator == null) {
                servletHandler.responseError("Generator not found");
                return null;
            }
            if (!promiseGenerator.getRateLimitConfiguration().get().check()) {
                servletHandler.response(429, "Too Many Requests", StandardCharsets.UTF_8);
                return null;
            }
            if (promiseGenerator.getProperty().getAuth()) {
                servletHandler
                        .getRequestReader()
                        .basicAuthHandler((user, password) -> {
                            if (!promiseGenerator.getUser().contains(user)) {
                                // 0x2A - просто что бы различать
                                throw new AuthException("Authorization failed 0x2A for user: " + user);
                            }
                            try {
                                if (password == null || !password.equals(new String(securityComponent.get("web.auth.password." + user)))) {
                                    // 0xFA - просто что бы различать
                                    throw new AuthException("Authorization failed 0xFA for user: " + user);
                                }
                            } catch (Exception e) {
                                throw new AuthException("Authorization failed 0xAA for user: " + user, e);
                            }
                        });
            }

            if (!promiseGenerator.getProperty().getValidationType().isEmpty()) {
                try {
                    String webSchemeFolder = "schema/" + promiseGenerator.getCascadeKey().substring(2) + "/";
                    ValidateType
                            .valueOf(promiseGenerator.getProperty().getValidationType())
                            .validate(
                                    servletHandler.getRequestReader().getData(),
                                    //UtilFileResource.getAsString("schema/xsd/false-xsd.xml", UtilFileResource.Direction.RESOURCE_CORE),
                                    UtilFileResource.getAsString(
                                            webSchemeFolder + promiseGenerator.getProperty().getValidationScheme(),
                                            UtilFileResource.Direction.WEB
                                    ),
                                    s -> UtilFileResource.get(webSchemeFolder + s, UtilFileResource.Direction.WEB)
                            );
                } catch (Throwable th) {
                    App.error(th);
                    // При неудачной валидации XML по схеме обычно возвращают HTTP 400 Bad Request, поскольку это
                    // ошибка на стороне клиента: он прислал «неправильный» документ.
                    // HTTP 422 Unprocessable Entity — он чётко говорит, что сервер понял запрос, но не может его
                    // обработать из‑за семантической (валидационной) ошибки.
                    servletHandler.responseError(HttpStatus.UNPROCESSABLE_ENTITY.value(), "Unprocessable Entity (" + th.getMessage() + ")");
                    return null;
                }
            }

            Promise promise = promiseGenerator.generate();

            if (promise == null) {
                servletHandler.responseError("Promise is null");
                return null;
            }

            if (!promise.hasErrorHandler()) {
                promise.onError((_, _, p) -> {
                    ServletHandler srvHandler = p.getRepositoryMapClass(ServletHandler.class);
                    srvHandler.setResponseBody(UtilJson.toStringPretty(p, "{}"));
                    srvHandler.responseComplete();
                });
            }
            if (!promise.hasCompleteHandler()) {
                promise.onComplete((_, _, p) -> {
                    ServletHandler srvHandler = p.getRepositoryMapClass(ServletHandler.class);
                    if (srvHandler.isEmptyBody()) {
                        srvHandler.setBodyIfEmpty(UtilJson.toStringPretty(p, "{}"));
                    }
                    srvHandler.responseComplete();
                });
            }
            promise.setRepositoryMapClass(ServletHandler.class, servletHandler);
            promise.run();
        } catch (AuthException th) {
            App.error(th);
            servletHandler.responseUnauthorized();
            return null;
        } catch (Throwable th) {
            App.error(th);
            servletHandler.responseError(th.getMessage());
            return null;
        }
        return servletHandler.getCompletableFuture();
    }

}
