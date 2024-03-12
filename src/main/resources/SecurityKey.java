package ru.jamsys;

import org.springframework.stereotype.Component;
import ru.jamsys.component.Security;

@Component
public class SecurityKey {

    public SecurityKey(Security security) {
        security.setPrivateKey("""
{privateKey}
                """.toCharArray());
    }
}
