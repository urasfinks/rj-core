package ru.jamsys.core.i360.scale.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.ToString;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.ScaleTypeRelation;
import ru.jamsys.core.i360.scope.Scope;

import java.util.*;

@ToString(doNotUseGetters = true)
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeneralizationTree {

    public final LinkedHashMap<EntityChain, GeneralizationTree> generalization = new LinkedHashMap<>();

    public GeneralizationTree(Scope scope, EntityChain ref) {
        fill(scope, ref);
        clearCycleReference();
    }

    private void fill(Scope scope, EntityChain ref) {
        scope.getRepositoryScale().getByLeft(ref, ScaleTypeRelation.GENERALIZATION).forEach(scale -> {
            GeneralizationTree tree = new GeneralizationTree(scope, scale.getRight());
            generalization.put(scale.getRight(), tree);
        });
    }

    private void clearCycleReference() {
        List<EntityChain> remove = new ArrayList<>();
        generalization.forEach((_, tree) -> remove.addAll(tree.getGeneralizationRecursive()));
        remove.forEach(generalization::remove);
    }

    public List<EntityChain> getGeneralizationRecursive() {
        List<EntityChain> result = new ArrayList<>();
        generalization.forEach((entityChain, tree) -> {
            result.add(entityChain);
            result.addAll(tree.getGeneralizationRecursive());
        });
        return result;
    }

}

