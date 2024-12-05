package ru.jamsys.core.component;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.component.manager.item.RouteGeneratorRepository;
import ru.jamsys.core.extension.UniqueClassNameImpl;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.flat.util.UtilJson;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.promise.PromiseGenerator;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

@Lazy
@Component
public class RouteGenerator {

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final ServiceClassFinder serviceClassFinder;
    private final ApplicationContext applicationContext;

    public RouteGenerator(ApplicationContext applicationContext, ServiceClassFinder serviceClassFinder) {
        this.applicationContext = applicationContext;
        this.serviceClassFinder = serviceClassFinder;
    }

    public RouteGeneratorRepository getRouterRepository(Class<?> interfaceMatcher) {
        return getRouterRepository(RequestMapping.class, interfaceMatcher);
    }

    public RouteGeneratorRepository getRouterRepository(Class<? extends RequestMapping> clsAnnotation, Class<?> interfaceMatcher) {
        /*
        {
            "[/*]" : "SecondHandler",
            "[/hello/*]" : "FirstHandler"
        }

        При таком раскладе первое правило будет подходить для всех типов запросов, поэтому результирующую выборку
        отсортируем
        */
        RouteGeneratorRepository routeGeneratorRepository = new RouteGeneratorRepository(antPathMatcher);
        Map<String, PromiseGenerator> repository = routeGeneratorRepository.getRepository();
        Map<String, String> info = routeGeneratorRepository.getInfo();
        Map<String, PromiseGenerator> tmp = new HashMap<>();
        serviceClassFinder.findByInstance(PromiseGenerator.class).forEach(promiseGeneratorClass -> {
            if (!ServiceClassFinder.instanceOf(promiseGeneratorClass, interfaceMatcher)) {
                return;
            }
            for (Annotation annotation : promiseGeneratorClass.getAnnotations()) {
                if (ServiceClassFinder.instanceOf(annotation.annotationType(), clsAnnotation)) {
                    PromiseGenerator promiseGenerator = applicationContext.getBean(promiseGeneratorClass);
                    promiseGenerator.setIndex(UniqueClassNameImpl.getClassNameStatic(
                            promiseGeneratorClass,
                            null,
                            applicationContext
                    ));
                    String[] values = promiseGeneratorClass.getAnnotation(clsAnnotation).value();
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
        UtilListSort.sort(new ArrayList<>(tmp.keySet()), UtilListSort.Type.DESC).forEach(s -> {
            info.put(s, tmp.get(s).getIndex());
            repository.put(s, tmp.get(s));
        });

        Util.logConsole("@" + clsAnnotation.getSimpleName() + " for " + interfaceMatcher.getSimpleName() + " : " + UtilJson.toStringPretty(info, "[]"));
        return routeGeneratorRepository;
    }

}
