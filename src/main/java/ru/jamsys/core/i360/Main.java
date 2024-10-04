package ru.jamsys.core.i360;

import lombok.Getter;
import ru.jamsys.core.i360.scope.Scope;
import ru.jamsys.core.i360.scope.ScopeImpl;

@Getter
public class Main {

    public static final Scope scope = new ScopeImpl();

    public static void main(String[] args) throws Throwable {
        scope.read("i360/math.json");
        scope.write("math.json");
    }
}
