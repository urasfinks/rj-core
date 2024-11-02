package ru.jamsys.core.extension.builder;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class ArrayListBuilder<E> extends ArrayList<E> {

    public ArrayListBuilder(@NotNull Collection<? extends E> c) {
        super(c);
    }

    public ArrayListBuilder() {
    }


    public ArrayListBuilder<E> append(E e) {
        add(e);
        return this;
    }

}
