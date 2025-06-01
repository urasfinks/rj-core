package ru.jamsys.core.promise;

import lombok.Getter;
import ru.jamsys.core.extension.CascadeKey;

@Getter
public abstract class PromiseGenerator implements CascadeKey {

    public abstract Promise generate();

}
