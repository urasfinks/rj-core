package ru.jamsys.core.resource;

import lombok.Setter;

@Setter
public class ResourceArguments {

    // Namespace: пространство из *.properties
    public String ns = "default";

    public ResourceArguments(String ns) {
        this.ns = ns;
    }

    public ResourceArguments() {
    }

}
