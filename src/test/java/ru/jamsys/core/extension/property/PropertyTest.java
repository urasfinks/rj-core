package ru.jamsys.core.extension.property;

import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.Util;

class PropertyTest {

    @Test
    void getKeyStructure() {
        KeyStructure keyStructure = PropertyUtil.getKeyStructure("App.ManagerFileByteWriter.StatisticSec<StatisticSec><X>.file.name<Log>");
        Util.logConsoleJson(PropertyTest.class, keyStructure);
    }

}