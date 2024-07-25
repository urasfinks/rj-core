package ru.jamsys.core.web.http;

import lombok.Setter;
import ru.jamsys.core.extension.annotation.PromiseWeb;
import ru.jamsys.core.promise.Promise;
import ru.jamsys.core.promise.PromiseGenerator;

@Setter
@SuppressWarnings("unused")
@PromiseWeb("/test")
public class FirstHandler implements PromiseGenerator {

    private String index;

    @Override
    public Promise generate() {
        return null;
    }

}
