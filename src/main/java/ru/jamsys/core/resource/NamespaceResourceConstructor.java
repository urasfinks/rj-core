package ru.jamsys.core.resource;

import lombok.Setter;

@Setter
public class NamespaceResourceConstructor {

    // Namespace: пространство из *.properties
    public String ns = "default";

    public NamespaceResourceConstructor(String ns) {
        this.ns = ns;
    }

    public NamespaceResourceConstructor() {
    }

}
