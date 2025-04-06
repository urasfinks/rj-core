package ru.jamsys.core.extension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public abstract class AbstractLifeCycle implements LifeCycleInterface {

    @JsonIgnore
    private final AtomicBoolean operation = new AtomicBoolean(false);

    private final AtomicBoolean run = new AtomicBoolean(false);

    @JsonIgnore
    private Thread threadOperation;

}