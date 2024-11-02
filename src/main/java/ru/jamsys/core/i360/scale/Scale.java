package ru.jamsys.core.i360.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ru.jamsys.core.i360.entity.EntityChain;

import java.util.Objects;

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

    public Scale setLeft(EntityChain left) {
        this.left = left;
        return this;
    }

    public Scale setRight(EntityChain right) {
        this.right = right;
        return this;
    }

    public Scale setType(ScaleType type) {
        this.type = type;
        return this;
    }

    public Scale setStability(double stability) {
        this.stability = stability;
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        Scale scale = (Scale) object;
        return Double.compare(stability, scale.stability) == 0
                && Objects.equals(left, scale.left)
                && Objects.equals(right, scale.right)
                && type == scale.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, type, stability);
    }

}
