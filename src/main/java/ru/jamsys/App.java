package ru.jamsys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import ru.jamsys.component.Core;
import ru.jamsys.component.Security;


@SpringBootApplication
public class App {

    public static ConfigurableApplicationContext context;

    public static void main(String[] args) {
        context = SpringApplication.run(App.class, args);
        App.context.getBean(Security.class).setPrivateKey("""
                MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAsNY/NYvcvP7VPKeLAJWU+icRWJ+G
                eprA2uy/X9k/y23tgI7OY2fJKCMdSlFhpPfaGlsvyNnVrVdvhEoHmlmpiQIDAQABAkAPFDB5oWkm
                LditqQpfogxSXwh0n824y1S3QPQko9gebCbDNyt/p3+Sx+t9lvdiOregXxwPMub84lIaPt8AUAJt
                AiEA/Awwcl7DxOzKMkgQv6443CZWKJaXhHVcGM7sTyRP9G8CIQCznCJwKsziNoPEqHHJqBn2IYVS
                R5Pdg1rtzw0hQ+vthwIgIsRLqnsH5hIDkgv+w3H0xelD2TVskQjfO0zPq9sGbGECIBvY3FoJuMRl
                8V4fQ60hXA0WO2Z7ZIiWohV24bFDp6OnAiEA5o5/0gLNrO+l8XRE5x4U5kq3TMZurOw3ZVsY5vlK
                0Ds=
                """.toCharArray());

        context.getBean(Core.class).run(null);
        System.out.println("Hello World!");
        //context.getBean(Core.class).shutdown();
    }

}
