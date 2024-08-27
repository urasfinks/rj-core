package ru.jamsys.core.extension.exception;

import com.networknt.schema.ValidationMessage;
import lombok.Getter;

import java.util.Set;

@Getter
public class JsonSchemaException extends RuntimeException {

    Set<ValidationMessage> validate;

    public JsonSchemaException(Set<ValidationMessage> validate) {
        this.validate = validate;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        validate.forEach(vm -> sb.append(vm.getMessage()).append("\n"));
        return sb.toString().trim();
    }

}
