package ru.jamsys.core.extension;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.util.PathMatcher;
import ru.jamsys.core.promise.PromiseGenerator;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
public class RouteGeneratorRepository {

    private final Map<String, PromiseGenerator> repository = new LinkedHashMap<>();
    public final Map<String, String> info = new LinkedHashMap<>();

    @Setter
    @Accessors(chain = true)
    private PathMatcher pathMatcher;

    public RouteGeneratorRepository(PathMatcher pathMatcher) {
        this.pathMatcher = pathMatcher;
    }

    public PromiseGenerator match(String path) {
        for (String pattern : repository.keySet()) {
            if (pathMatcher.match(pattern, path)) {
                return repository.get(pattern);
            }
        }
        return null;
    }

}
