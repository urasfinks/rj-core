package ru.jamsys.core.extension.property;

import org.junit.jupiter.api.Test;
import ru.jamsys.core.flat.util.UtilLog;

class PropertyTest {

    @Test
    void getKeyStructure() {
        KeyStructure keyStructure = PropertyUtil.getKeyStructure("$.ManagerFileByteWriter.StatisticSec<StatisticSec><X>.file.name<Log>");
        UtilLog.printInfo(keyStructure);
    }

    @Test
    void getKeyStructure2() {
        KeyStructure keyStructure = PropertyUtil.getKeyStructure("$.BrokerMemory[$.PoolThreadExecutePromiseTask[PropertyWeb.input]].size");
        UtilLog.printInfo(keyStructure);
    }

}