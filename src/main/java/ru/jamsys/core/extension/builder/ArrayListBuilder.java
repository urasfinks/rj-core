package ru.jamsys.core.extension.builder;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

@SuppressWarnings("unused")
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

    public ArrayListBuilder<E> apply(Consumer<ArrayListBuilder<E>> consumer) {
        consumer.accept(this);
        return this;
    }

}
