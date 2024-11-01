package ru.jamsys.core.i360;

import lombok.Getter;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.Scale;
import ru.jamsys.core.i360.scale.ScaleType;
import ru.jamsys.core.i360.scope.Scope;
import ru.jamsys.core.i360.scope.ScopeImpl;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
public class Main {

    public static final Scope scope = new ScopeImpl();

    public static void main(String[] args) throws Throwable {
        scope.getJsonDeserialize().read("i360/math.f1.json");
        List<Scale> byType = scope.getRepositoryScale().getByType(ScaleType.EQUALS);
        analog(byType.getFirst().getLeft());
        //System.out.println(UtilJson.toStringPretty(scope.getRepositoryScale().getByType(ScaleType.EQUALS), "{}"));
        //scope.getJsonSerialize().write("math.json");
    }

    public static void analog(EntityChain entityChain) {
        entityChain.getListEntity().forEach(entity -> {
            Set<EntityChain> result = new HashSet<>();
            entity.getVariant(ScaleType.EQUALS, result);
            entity.getVariant(ScaleType.GENERALIZATION, result);
            entity.getVariant(ScaleType.CONSEQUENCE, result);

            System.out.println(result);
        });
    }

}
