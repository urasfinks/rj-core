package ru.jamsys.core.component;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import ru.jamsys.core.extension.annotation.PropertyKey;
import ru.jamsys.core.extension.property.repository.RepositoryPropertyAnnotationField;

@SuppressWarnings({"UnusedDeclaration"})
@FieldNameConstants
@Getter
public class SecurityComponentProperty extends RepositoryPropertyAnnotationField<String> {
    @Setter
    @PropertyKey("run.args.security.path.storage")
    private String pathStorage;

    @Setter
    @PropertyKey("run.args.security.path.public.key")
    private String pathPublicKey;

    @Setter
    @PropertyKey("run.args.security.path.init")
    private String pathJsonCred;

    @Getter
    @SuppressWarnings("all")
    @PropertyKey("run.args.security.path.java")
    private String pathInitSecurityKeyJava;

}
