package ru.jamsys.core.i360;

import lombok.Getter;
import lombok.Setter;

// Выдуманные весы, которым необходимо подтверждение, с возможностью хранения локальных, приоритетных весов.
// На вход должны подаваться весы и вероятно набор знаний для создания концепта.
// Концепт не должен противоречить типу бинарности весов, то есть следсвие чего-то не может быть отрицанием
// следствия и так для всех типов (справедливость утверждения).
@Getter
@Setter
public class ScaleConcept extends Scale {

    private final Scope scope = new Scope();

    public boolean check(Scale scale) {
        return false;
    }

}
