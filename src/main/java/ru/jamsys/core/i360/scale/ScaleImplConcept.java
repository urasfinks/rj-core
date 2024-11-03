package ru.jamsys.core.i360.scale;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.i360.scope.Scope;
import ru.jamsys.core.i360.scope.ScopeImpl;

// Выдуманные весы, которым необходимо подтверждение, с возможностью хранения локальных, приоритетных весов.
// На вход должны подаваться весы и вероятно набор знаний для создания концепта.
// Концепт не должен противоречить типу бинарности весов, то есть следсвие чего-то не может быть отрицанием
// следствия и так для всех типов (справедливость утверждения).
@Getter
@Setter
public class ScaleImplConcept extends ScaleImpl {

    private final Scope scope = new ScopeImpl();

    public boolean check(ScaleImpl scale) {
        return false;
    }

}
