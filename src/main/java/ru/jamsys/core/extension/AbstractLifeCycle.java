package ru.jamsys.core.extension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.extension.functional.ProcedureThrowing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Getter
@Setter
public abstract class AbstractLifeCycle implements LifeCycleInterface {

    @JsonIgnore
    private final AtomicBoolean operation = new AtomicBoolean(false);

    private final AtomicBoolean run = new AtomicBoolean(false);

    private final List<ProcedureThrowing> listOnPostShutdown = new ArrayList<>();

    private final List<LifeCycleInterface> listShutdownAfter = new ArrayList<>();

    private final List<LifeCycleInterface> listShutdownBefore = new ArrayList<>();

    @JsonIgnore
    private Thread threadOperation;

}