package ru.jamsys.core.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.jamsys.core.App;
import ru.jamsys.core.extension.RouteGeneratorRepository;
import ru.jamsys.core.flat.util.UtilListSort;
import ru.jamsys.core.flat.util.UtilLog;
import ru.jamsys.core.promise.PromiseGeneratorExternalRequest;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

// Находит классы по интерфейсу, ищет аннотацию RequestMapping (или другую указанную) и создаёт RouteGeneratorRepository
// в котором сопоставлены antTemplate c PromiseGenerator

@Lazy
@Component
public class RouteGenerator {

    private static final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final ServiceClassFinder serviceClassFinder;

    public RouteGenerator(ServiceClassFinder serviceClassFinder) {
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
        Map<String, PromiseGeneratorExternalRequest> repository = routeGeneratorRepository.getRepository();
        Map<String, String> info = routeGeneratorRepository.getInfo();
        Map<String, PromiseGeneratorExternalRequest> tmp = new HashMap<>();
        serviceClassFinder.findByInstance(PromiseGeneratorExternalRequest.class).forEach(promiseGeneratorClass -> {
            if (!ServiceClassFinder.instanceOf(promiseGeneratorClass, interfaceMatcher)) {
                return;
            }
            for (Annotation annotation : promiseGeneratorClass.getAnnotations()) {
                if (ServiceClassFinder.instanceOf(annotation.annotationType(), clsAnnotation)) {
                    PromiseGeneratorExternalRequest promiseGenerator = App.get(promiseGeneratorClass);
                    String[] values = promiseGeneratorClass.getAnnotation(clsAnnotation).value();
                    if (values.length > 0) {
                        for (String value : values) {
                            tmp.put(value, promiseGenerator);
                        }
                    } else {
                        tmp.put("/" + App.getUniqueClassName(promiseGeneratorClass), promiseGenerator);
                    }
                    break;
                }
            }
        });
        UtilListSort.sort(new ArrayList<>(tmp.keySet()), UtilListSort.Type.DESC).forEach(s -> {
            info.put(s, App.getUniqueClassName(tmp.get(s).getClass()));
            repository.put(s, tmp.get(s));
        });

        UtilLog.info(info)
                .addHeader("annotation", "@" + App.getUniqueClassName(clsAnnotation))
                .addHeader("interfaceMatcher", App.getUniqueClassName(interfaceMatcher))
                .print();

        return routeGeneratorRepository;
    }

}
