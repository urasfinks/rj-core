package ru.jamsys.core.i360.entity;

import lombok.Getter;

@Getter
public class EntityImpl implements Entity{

    final private String uuid = java.util.UUID.randomUUID().toString();

}
