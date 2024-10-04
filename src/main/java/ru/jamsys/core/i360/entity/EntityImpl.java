package ru.jamsys.core.i360.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class EntityImpl implements Entity{

    private String uuid;

    private String data;

//    public EntityImpl(String uuid, String data) {
//        this.uuid = uuid == null ? java.util.UUID.randomUUID().toString() : uuid;
//        this.data = data;
//    }

}
