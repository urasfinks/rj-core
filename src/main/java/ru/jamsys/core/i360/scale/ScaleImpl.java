package ru.jamsys.core.i360.scale;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import lombok.ToString;
import ru.jamsys.core.i360.entity.EntityChain;

import java.util.Objects;

@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"type", "left", "right", "stability"})
public class ScaleImpl implements Scale {

    private final ScaleTypeRelation typeRelation;

    private final ScaleType type;

    private EntityChain left;

    private EntityChain right;

    private double stability = 0; // [0-1] // В случаях умозаключений стабильность, это то как прошла практика

    public ScaleImpl(ScaleType type, ScaleTypeRelation typeRelation) {
        this.type = type;
        this.typeRelation = typeRelation;
    }

    public ScaleImpl setLeft(EntityChain left) {
        this.left = left;
        return this;
    }

    public ScaleImpl setRight(EntityChain right) {
        this.right = right;
        return this;
    }

    public ScaleImpl setStability(double stability) {
        this.stability = stability;
        return this;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        ScaleImpl scale = (ScaleImpl) object;
        return Double.compare(stability, scale.stability) == 0
                && Objects.equals(left, scale.left)
                && Objects.equals(right, scale.right)
                && typeRelation == scale.typeRelation;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right, typeRelation, stability);
    }

}
