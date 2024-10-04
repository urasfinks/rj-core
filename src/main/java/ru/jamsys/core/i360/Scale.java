package ru.jamsys.core.i360;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Scale {

    private Classifier classifier;

    private Context left;

    private Context right;

    private ScaleType type;

    private double stability = 0; // [0-1]

}
