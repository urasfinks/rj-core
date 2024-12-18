package ru.jamsys.core.i360.concept;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.Scale;
import ru.jamsys.core.i360.scale.ScaleImpl;
import ru.jamsys.core.i360.scale.ScaleType;
import ru.jamsys.core.i360.scale.ScaleTypeRelation;
import ru.jamsys.core.i360.scale.operation.GeneralizationTree;
import ru.jamsys.core.i360.scope.Scope;
import ru.jamsys.core.i360.scope.ScopeImpl;

import java.util.List;

class UniformGeneralizationTest {

    @Test
    public void test() throws Throwable {
        // 1 - число
        // 1 следует 2
        // Концепт: 2 - число
        Scope scope = new ScopeImpl("i360/investigation/1.json");
        List<Scale> research = UniformGeneralization.research(scope);
        ScaleImpl scale = new ScaleImpl(ScaleType.CONCLUSION, ScaleTypeRelation.GENERALIZATION)
                .setLeft(new EntityChain().add(scope.getMapEntity().get("k1")))
                .setRight(new EntityChain().add(scope.getMapEntity().get("k10")))
                .setStability(1);
        Assertions.assertEquals(new ArrayListBuilder<>().append(scale), research);
    }

    @Test
    public void getFirstGeneralization() throws Throwable {
        // Щенок ~> Млекопитающее
        // Щенок ~> Собака
        // Собака ~> Млекопитающее
        // Для щенка первое обобщение - Собака, а не Млекопитающее, так как Щенок ~> Собака ~> Млекопитающее
        Scope scope = new ScopeImpl("i360/investigation/2.json");
        GeneralizationTree generalizationTree = new GeneralizationTree(scope, scope.getRepositoryScale().getListScale().getFirst().getLeft());
        Assertions.assertEquals(
                new EntityChain().add(scope.getMapEntity().get("k1")),
                generalizationTree.getGeneralization().firstEntry().getKey()
        );
    }

}