package ru.jamsys.core.i360;

import lombok.Getter;

@Getter
public class Main {

    public static final Scope scope = new Scope();

    public static void main(String[] args) throws Throwable {
        scope.load("i360/math.json");
        scope.save("math.json");
    }
}
