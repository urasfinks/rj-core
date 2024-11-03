package ru.jamsys.core.i360.scale.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.ToString;
import ru.jamsys.core.i360.entity.EntityChain;
import ru.jamsys.core.i360.scale.ScaleType;
import ru.jamsys.core.i360.scope.Scope;

import java.util.*;

@ToString(doNotUseGetters = true)
@Getter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GeneralizationTree {

    public final LinkedHashMap<EntityChain, GeneralizationTree> parent = new LinkedHashMap<>();

    public GeneralizationTree(Scope scope, EntityChain ref) {
        fill(scope, ref);
        clearCycleReference();
    }

    private void fill(Scope scope, EntityChain ref) {
        scope.getRepositoryScale().getByLeft(ref, ScaleType.GENERALIZATION).forEach(scale -> {
            GeneralizationTree tree = new GeneralizationTree(scope, scale.getRight());
            parent.put(scale.getRight(), tree);
        });
    }

    private void clearCycleReference() {
        List<EntityChain> remove = new ArrayList<>();
        parent.forEach((_, tree) -> remove.addAll(tree.getParentRecursive()));
        remove.forEach(parent::remove);
    }

    public List<EntityChain> getParentRecursive() {
        List<EntityChain> result = new ArrayList<>();
        parent.forEach((entityChain, tree) -> {
            result.add(entityChain);
            result.addAll(tree.getParentRecursive());
        });
        return result;
    }

}

