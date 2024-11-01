package ru.jamsys.core.i360.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.i360.entity.EntityChain;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "left", "right", "stability"})
public class Scale {

    private EntityChain left;

    private EntityChain right;

    private ScaleType type;

    private double stability = 0; // [0-1]

}
