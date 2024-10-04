package ru.jamsys.core.i360;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

// Список контекстов, будет служить для поиска нужных знаний с учётом контекста
@Getter
@Setter
public class Classifier {
    private List<Context> listContext;
}
