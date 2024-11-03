package ru.jamsys.core.i360.concept;

import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.ScaleImpl;
import ru.jamsys.core.i360.scale.ScaleType;
import ru.jamsys.core.i360.scope.Scope;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Весы равенства/следствия с большой вероятностью имеют одинаковые классы объектов
 * */

public class Investigation {

    public static List<ScaleImpl> research(Scope scope) {
        Map<EntityChain, EntityChain> generalization = new HashMap<>();
        scope.getRepositoryScale().getByType(ScaleType.GENERALIZATION).forEach(
                scale -> generalization.put(scale.getLeft(), scale.getRight())
        );
        List<ScaleImpl> concept = new ArrayList<>();
        // Предположение следствия/равенства, что оно того-же класса
        scope.getRepositoryScale().getByTypes(new ArrayListBuilder<ScaleType>()
                .append(ScaleType.CONSEQUENCE)
                .append(ScaleType.EQUALS)
        ).forEach(scale -> {
            /*
             * Если мы знаем, что левая часть это некоторый класс
             * Смеем предположить, что правая часть это тот же класс
             * */
            if (generalization.containsKey(scale.getLeft())) {
                EntityChain leftGen = generalization.get(scale.getLeft());
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
        // Дерево зависимостей в линейные зависимости
        return concept;
    }

}
