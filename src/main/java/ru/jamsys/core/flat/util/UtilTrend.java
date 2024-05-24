package ru.jamsys.core.flat.util;

import ru.jamsys.core.flat.trend.LinearTrendLine;
import ru.jamsys.core.flat.trend.PolyTrendLine;

public class UtilTrend {

    public static double getPoly(int degree, double[] line) {
        return getPoly(degree, line, 1);
    }

    public static double getPoly(int degree, double[] x, double[] y, double predict) {
        PolyTrendLine t = new PolyTrendLine(degree);
        t.setValues(y, x);
        return t.predict(x.length + predict);
    }

    public static double getPoly(int degree, double[] line, double predict) {
        double[] x = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            x[i] = i + 1;
        }
        return getPoly(degree, x, line, predict);
    }

    public static double getLinear(double[] line) {
        return getLinear(line, 1);
    }

    public static double getLinear(double[] line, double predict) {
        return LinearTrendLine.get(line, predict);
    }

    public static double getLinear(double[] x, double[] y, double predict) {
        return LinearTrendLine.get(x, y, predict);
    }

}
