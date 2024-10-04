package ru.jamsys.core.i360;

import lombok.Getter;
import ru.jamsys.core.i360.entity.Entity;

import java.util.ArrayList;
import java.util.List;

@Getter
public class Scope {

    final private List<Entity> listEntity = new ArrayList<>();

    final private List<Scale> listScale = new ArrayList<>();

}
