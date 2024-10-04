package ru.jamsys.core.i360.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EntityImpl implements Entity {

    private String uuid;

    private String data;

}
