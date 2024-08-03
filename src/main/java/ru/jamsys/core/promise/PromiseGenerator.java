package ru.jamsys.core.promise;

public interface PromiseGenerator {

    void setIndex(String index);

    String getIndex();

    Promise generate();

}
