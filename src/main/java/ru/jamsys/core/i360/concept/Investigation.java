package ru.jamsys.core.i360.concept;

import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.Scale;
import ru.jamsys.core.i360.scale.ScaleType;
import ru.jamsys.core.i360.scope.Scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Следствие 1 -> 2
 * Если 1 это число, то вероятно 2 это тоже число
 *
 * Любой концепт в виде весов не должен быть опровергнут обратными весами
 * Мы сделали предложение, что 2 это тоже число, надо поискать а нет ли весов, которые говорят, что 2 это не число
 * */

public class Investigation {

    public static List<Scale> research(Scope scope) throws Throwable {
        Map<EntityChain, EntityChain> generalization = new HashMap<>();
        scope.getRepositoryScale().getByType(ScaleType.GENERALIZATION).forEach(scale -> {
            generalization.put(scale.getLeft(), scale.getRight());
        });
        List<Scale> concept = new ArrayList<>();
        scope.getRepositoryScale().getByType(ScaleType.CONSEQUENCE).forEach(scale -> {
            EntityChain known = scale.getLeft();
            if (generalization.containsKey(known)) {
                EntityChain gen = generalization.get(known);
                if (scope
                        .getRepositoryScale()
                        .getByLeft(scale.getRight(), ScaleType.NOT_GENERALIZATION)
                        .stream()
                        .noneMatch(scale1 -> scale1.getRight().equals(gen))
                        &&
                        scope
                                .getRepositoryScale()
                                .getByLeft(scale.getRight(), ScaleType.GENERALIZATION)
                                .stream()
                                .noneMatch(scale1 -> scale1.getRight().equals(gen))
                ) {
                    Scale c = new Scale();
                    c.setLeft(scale.getRight());
                    c.setRight(gen);
                    c.setStability(1);
                    c.setType(ScaleType.GENERALIZATION);
                    concept.add(c);
                }
            }
        });
        return concept;
    }

}
