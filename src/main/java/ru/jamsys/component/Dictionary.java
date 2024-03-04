package ru.jamsys.component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.annotation.Lazy;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@org.springframework.stereotype.Component
@Lazy
@Getter
@Setter
@ToString
public class Dictionary {

    @SafeVarargs
    public static <T extends Component> Class<T>[] getEmptyType(Class<T>... array) {
        return Arrays.copyOf(array, 0);
    }

    Map<Class<? extends Component>, Component> map = new ConcurrentHashMap<>();
}
