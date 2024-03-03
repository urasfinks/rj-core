package ru.jamsys.component;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.annotation.Lazy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@org.springframework.stereotype.Component
@Lazy
@Getter
@Setter
@ToString
public class Dictionary {
    Map<Class<? extends Component>, Component> map = new ConcurrentHashMap<>();
}
