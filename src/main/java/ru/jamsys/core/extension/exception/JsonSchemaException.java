package ru.jamsys.core.extension.exception;

import com.networknt.schema.ValidationMessage;
import lombok.Getter;

import java.util.Set;

@Getter
public class JsonSchemaException extends RuntimeException {

    Set<ValidationMessage> validate;

    String information;

    String json;

    String schema;

    public JsonSchemaException(Set<ValidationMessage> validate, String information, String json, String schema) {
        this.validate = validate;
        this.information = information;
        this.json = json;
        this.schema = schema;
    }

    @Override
    public String getMessage() {
        StringBuilder sb = new StringBuilder();
        if (information != null) {
            sb.append("information: ").append(information).append("; cause: ");
        }
        validate.forEach(vm -> sb.append(vm.getMessage()).append("\n"));
        return sb.toString().trim();
    }

}