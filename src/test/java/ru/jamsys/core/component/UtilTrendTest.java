package ru.jamsys.core.component;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.jamsys.core.App;
import ru.jamsys.core.util.UtilTrend;

class UtilTrendTest {
    @BeforeAll
    static void beforeAll() {
        String[] args = new String[]{};
        App.main(args);
    }

    @Test
    void getPoly() {
        UtilTrend bean = App.context.getBean(UtilTrend.class);
        Assertions.assertEquals(28, Math.round(bean.getPoly(2, new double[]{13, 25, 98, 40, 15, 66})), "#1");
        Assertions.assertEquals(160, Math.round(bean.getPoly(3, new double[]{13, 25, 98, 40, 15, 66})), "#2");
        Assertions.assertEquals(512, Math.round(bean.getPoly(4, new double[]{13, 25, 98, 40, 15, 66})), "#3");
        Assertions.assertEquals(506, Math.round(bean.getPoly(5, new double[]{13, 25, 98, 40, 15, 66, 300})), "#4");

    }

    @Test
    void getPredictPoly() {
        UtilTrend bean = App.context.getBean(UtilTrend.class);
        Assertions.assertEquals(5, Math.round(bean.getPoly(2, new double[]{1, 2, 3, 4})), "#1");
        Assertions.assertEquals(5, Math.round(bean.getPoly(2, new double[]{1, 2, 3, 4}, 1)), "#2");
        Assertions.assertEquals(6, Math.round(bean.getPoly(2, new double[]{1, 2, 3, 4}, 2)), "#3");
        Assertions.assertEquals(7, Math.round(bean.getPoly(3, new double[]{1, 2, 3, 4, 5}, 2)), "#4");

    }

    @Test
    void getLinear() {
        UtilTrend bean = App.context.getBean(UtilTrend.class);
        Assertions.assertEquals(6, Math.round(bean.getLinear(new double[]{1, 2, 3, 4, 5}, 1)), "#1");
        Assertions.assertEquals(7, Math.round(bean.getLinear(new double[]{1, 2, 3, 4, 5}, 2)), "#2");
        Assertions.assertEquals(-14419, Math.round(bean.getLinear(new double[]{74311, 37555, 30541, 25938, 30490, 6853, 1523, 1522}, 1)), "#3");
        Assertions.assertEquals(-23421, Math.round(bean.getLinear(new double[]{74311, 37555, 30541, 25938, 30490, 6853, 1523, 1522}, 2)), "#4");

    }
}