package ru.jamsys.core.i360;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.i360.entity.Entity;

import java.util.List;

@Getter
@Setter
public class Context {
    private List<Entity> listEntity;
}
