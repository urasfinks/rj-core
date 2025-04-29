package ru.jamsys.core.resource;

import lombok.Setter;
import lombok.experimental.Accessors;

// TODO: избавится, так как только ns является ключевой настройкой
@Setter
@Accessors(chain = true)
public class ResourceConfiguration {

    // Namespace: пространство из *.properties
    public String ns = "default";

    public ResourceConfiguration(String ns) {
        this.ns = ns;
    }

    public ResourceConfiguration() {
    }

}
