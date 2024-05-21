package ru.jamsys.core;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.SecurityComponent;

@SuppressWarnings("unused")
@Component
public class SecurityKey {

    @SuppressWarnings("unused")
    public SecurityKey(SecurityComponent securityComponent) {
        securityComponent.setPrivateKey("""
{privateKey}
                """.toCharArray());
    }
}
