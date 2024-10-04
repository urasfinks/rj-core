package ru.jamsys.core.i360.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.i360.Context;

@Getter
@Setter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "left", "right", "stability"})
public class Scale {

    private Context classifier;

    private Context left;

    private Context right;

    private ScaleType type;

    private double stability = 0; // [0-1]

}
