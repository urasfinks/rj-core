package ru.jamsys.core.i360.concept;

import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.Scale;
import ru.jamsys.core.i360.scale.ScaleImpl;
import ru.jamsys.core.i360.scale.ScaleType;
import ru.jamsys.core.i360.scale.operation.GeneralizationTree;
import ru.jamsys.core.i360.scope.Scope;

import java.util.ArrayList;
import java.util.List;

/*
 * Концепт однотипности обобщения равных или следственных объектов
 * Весы равенства/следствия имеют одинаковые классы объектов
 * */

public class UniformGeneralization {

    public static List<Scale> research(Scope scope) {
        List<Scale> concept = new ArrayList<>();
        scope.getRepositoryScale().getByTypes(new ArrayListBuilder<ScaleType>()
                .append(ScaleType.CONSEQUENCE)
                .append(ScaleType.EQUALS)
        ).forEach(scale -> {
            GeneralizationTree generalizationTree = scope.getGeneralizationTree(scale.getLeft());
            if (!generalizationTree.getGeneralization().isEmpty()) { // Если у левой части есть определённое обобщение
                EntityChain leftGen = generalizationTree.getGeneralization().firstEntry().getKey();
                if (scope
                        .getRepositoryScale()
                        .getByLeft(scale.getRight(), ScaleType.NOT_GENERALIZATION)
                        .stream()
                        .noneMatch(scale1 -> scale1.getRight().equals(leftGen)) // Проверяем, что явного исключения нет
                        &&
                        scope
                                .getRepositoryScale()
                                .getByLeft(scale.getRight(), ScaleType.GENERALIZATION)
                                .stream()
                                .noneMatch(scale1 -> scale1.getRight().equals(leftGen)) // Проверяем, что таких весов ещё нет
                        &&
                        !scale.getRight().equals(leftGen) // Проверяем, что бы на весах были разные объекты, а то смысл взвешивания теряется
                ) {
                    ScaleImpl c = new ScaleImpl();
                    c.setLeft(scale.getRight());
                    c.setRight(leftGen);
                    c.setStability(1);
                    c.setType(ScaleType.GENERALIZATION);
                    concept.add(c);
                }
            }
        });
        return concept;
    }

}
