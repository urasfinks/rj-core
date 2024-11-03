package ru.jamsys.core.i360.concept;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.extension.builder.ArrayListBuilder;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.ScaleImpl;
import ru.jamsys.core.i360.scale.ScaleType;
import ru.jamsys.core.i360.scope.Scope;
import ru.jamsys.core.i360.scope.ScopeImpl;

import java.util.List;

class InvestigationTest {

    @Test
    public void test() throws Throwable {
        Scope scope = new ScopeImpl("i360/investigation/1.json");
        List<ScaleImpl> research = Investigation.research(scope);
        ScaleImpl scale = new ScaleImpl()
                .setLeft(new EntityChain().add(scope.getMapEntity().get("k1")))
                .setRight(new EntityChain().add(scope.getMapEntity().get("k10")))
                .setType(ScaleType.GENERALIZATION)
                .setStability(1);
        Assertions.assertEquals(new ArrayListBuilder<>().append(scale), research);
    }

    @Test
    public void test2() throws Throwable {
        Scope scope = new ScopeImpl("i360/investigation/2.json");
        ScaleImpl scale = scope.getRepositoryScale().getListScale().getFirst();
        Assertions.assertEquals(
                new EntityChain().add(scope.getMapEntity().get("k1")),
                scale.getGeneralizationOperation().get(scope).getParent().firstEntry().getKey()
        );
    }

}