package ru.jamsys.core.component.manager;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import ru.jamsys.core.App;
import ru.jamsys.core.component.ServiceProperty;
import ru.jamsys.core.extension.AbstractManagerElement;
import ru.jamsys.core.extension.builder.HashMapBuilder;
import ru.jamsys.core.extension.exception.ForwardException;

import java.util.Objects;
import java.util.function.Consumer;

// Создан для хранения onCreate функционала, что бы в Manager не было утечек builder.
// То есть сам объект ExpiredElement находится в Manager и для двух ManagerConfiguration ссылка будет на один
// элемент. А onCreate же будет два, каждый для своего ManagerConfiguration. GC удаляет ManagerConfiguration и onCreate
// улетает в след за ним. Выросло это всё из того, что хотелось в одном месте сконфигурировать объект, а в другом
// просто получить, ничего не зная о onCreate. Ранее onCreate жил в Manager.mapBuilder, но ссылки не удалялись и текли
// соответственно. Поэтому разместили onCreate в ManagerConfiguration

public class ManagerConfiguration<T extends AbstractManagerElement> {

    @Getter
    private final Class<T> cls;

    @Getter
    private final String key;

    @Getter
    private final String ns;

    private final Manager manager;

    private final Consumer<T> onNativeCreate;

    private ManagerConfiguration(Class<T> cls, String key, String ns, Manager manager, Consumer<T> onCreate) {
        if (key == null || ns == null) {
            throw new ForwardException("Null detected", new HashMapBuilder<String, Object>()
                    .append("cls", cls)
                    .append("key", key)
                    .append("ns", ns)
            );
        }
        this.cls = cls;
        this.key = key;
        this.ns = ns;
        this.manager = manager;
        this.onNativeCreate = onCreate;
    }

    public void onCreate(T item) {
        try {
            item.setInactivityTimeoutMs(App.get(ServiceProperty.class)
                    .computeIfAbsent(item.getCascadeKey(ns) + ".expiration.ms", 6_000)
                    .get(Long.class));
        } catch (Exception e) {
            App.error(e);
        }
        if (this.onNativeCreate != null) {
            this.onNativeCreate.accept(item);
        }
    }

    @JsonValue
    public Object getJsonValue() {
        return new HashMapBuilder<String, Object>()
                .append("hashCode", Integer.toHexString(hashCode()))
                .append("cls", cls)
                .append("managerKey", key)
                .append("ns", ns)
                .append("reference", isAlive() ? manager.get(cls, key, ns, this::onCreate) : null)
                ;
    }

    public boolean isAlive() {
        return manager.contains(cls, key, ns);
    }

    public T get() {
        // Мы не можем себе позволить хранить кеш, так как мастер система хранения элементов является Manager
        // Предположим, если прихраним кеш и Manager реально сольёт экземпляр, мы будем хранить ссылку на экземпляр
        // и этой конфигурацией пользоваться никто не будет. Мы будем держать GC что бы он не удалял этот объект,
        // это плохо, так как конфигурация и нужна для того, что бы дать возможность освободить память, а при
        // необходимости возобновить экземпляр в память
        T t = manager.get(cls, key, ns, this::onCreate);
        t.markActive();
        return t;
    }

    public boolean equalsElement(Object o){
        return Objects.equals(get(), o);
    }

    public void execute(Consumer<T> managerElement) {
        T t = get();
        if (t != null) {
            managerElement.accept(t);
        }
    }

    public void executeIfAlive(Consumer<T> managerElement) {
        if (!isAlive()) {
            return;
        }
        execute(managerElement);
    }

    public static <R extends AbstractManagerElement, S extends AbstractManagerElement> ManagerConfiguration<R> getInstance(
            Class<S> cls,
            String key,
            String ns,
            Consumer<R> onCreate
    ) {
        @SuppressWarnings("unchecked")
        Class<R> newCls = (Class<R>) cls;
        return new ManagerConfiguration<>(newCls, key, ns, App.get(Manager.class), onCreate);
    }

}
