package ru.jamsys.core.i360;

import lombok.Getter;
import ru.jamsys.core.flat.util.Util;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.ScaleTypeRelation;
import ru.jamsys.core.i360.scope.Scope;
import ru.jamsys.core.i360.scope.ScopeImpl;

import java.util.*;

@Getter
public class Main {

    public static final Scope scope = new ScopeImpl();

    public static void main(String[] args) throws Throwable {
        //scope.getJsonDeserialize().read("i360/math.f1.json");
        //List<Scale> byType = scope.getRepositoryScale().getByType(ScaleType.GENERALIZATION);
        //analog(byType.getFirst().getLeft());
        //scope.getJsonSerialize().write("math.json");
    }

    public static void analog(EntityChain entityChain) {
        List<List<EntityChain>> res = new ArrayList<>();
        // entityChain.getListEntity() = ["0", "1"]
        // ("0" => [chain,chain,chain]) Set<EntityChain> result = [[chain,chain,chain], [chain,chain,chain]]
        entityChain.getChain().forEach(entity -> {
            Set<EntityChain> result = new HashSet<>();
            entity.getVariant(ScaleTypeRelation.EQUALS, result);
            entity.getVariant(ScaleTypeRelation.GENERALIZATION, result);
            entity.getVariant(ScaleTypeRelation.CONSEQUENCE, result);
            res.add(new ArrayList<>(result));
        });
        Collection<?> cartesian = Util.cartesian(ArrayList::new, res.toArray(new List[0]));
        cartesian.forEach(object -> {
            EntityChain entityChain1 = new EntityChain();
            @SuppressWarnings("unchecked")
            List<EntityChain> list = (List<EntityChain>) object;
            list.forEach(entityChain2 -> entityChain1.getChain().addAll(entityChain2.getChain()));
        });
    }

}
