package ru.jamsys.core.plugin.telegram.structure;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class FileSource {
    String path;
    String id;
    String description;
}
