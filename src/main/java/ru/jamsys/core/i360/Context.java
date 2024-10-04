package ru.jamsys.core.i360;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.ToString;
import ru.jamsys.core.i360.entity.Entity;
import ru.jamsys.core.i360.scope.Scope;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Context {

    private final List<Entity> listEntity = new ArrayList<>();

    @JsonIgnore
    private final Scope scope;

    public Context(Scope scope) {
        this.scope = scope;
    }

    public static Context newInstance(List<String> listUuid, Scope scope) {
        Context context = new Context(scope);
        List<Entity> list = context.getListEntity();
        listUuid.forEach(s -> list.add(scope.getEntityByUuid(s)));
        return context;
    }

    @SuppressWarnings("unused")
    @JsonValue
    public List<String> toValue() {
        List<String> result = new ArrayList<>();
        listEntity.forEach(entity -> result.add(entity.getUuid()));
        return result;
    }

}
