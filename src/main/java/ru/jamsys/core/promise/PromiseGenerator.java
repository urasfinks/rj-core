package ru.jamsys.core.promise;

public interface PromiseGenerator {

    void setIndex(String index);

    Promise generate();

}
