package ru.jamsys.core;

import org.springframework.stereotype.Component;
import ru.jamsys.core.component.Security;

@SuppressWarnings("unused")
@Component
public class SecurityKey {

    @SuppressWarnings("unused")
    public SecurityKey(Security security) {
        security.setPrivateKey("""
{privateKey}
                """.toCharArray());
    }
}
