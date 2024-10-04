package ru.jamsys.core.i360;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class Scale {

    private Context classifier;

    private Context left;

    private Context right;

    private ScaleType type;

    private double stability = 0; // [0-1]

}
