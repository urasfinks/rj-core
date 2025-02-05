package ru.jamsys.core.flat.util;

import lombok.Getter;
import lombok.Setter;
import ru.jamsys.core.flat.trend.LinearTrendLine;
import ru.jamsys.core.flat.trend.PolyTrendLine;

import java.util.*;

public class UtilTrend {

    public static double getPoly(int degree, double[] line) {
        return getPoly(degree, line, 1);
    }

    public static double getPoly(int degree, double[] x, double[] y, double predict) {
        PolyTrendLine t = new PolyTrendLine(degree);
        t.setValues(y, x);
        return t.predict(x.length + predict);
    }

    @Getter
    @Setter
    public static class XY {

        private List<Double> x = new ArrayList<>();
        private List<Double> y = new ArrayList<>();
        private Map<Double, Double> xy = new LinkedHashMap<>();

        public double[] getX() {
            return x.stream().mapToDouble(Double::doubleValue).toArray();
        }

        public double[] getY() {
            return y.stream().mapToDouble(Double::doubleValue).toArray();
        }

        public void add(double iX, double iY) {
            x.add(iX);
            y.add(iY);
            xy.put(iX, iY);
        }

        public void addY(double iY) {
            add((double) x.size() + 1, iY);
        }

    }

    public static double getPoly(int degree, double[] line, double predict) {
        XY xy = new XY();
        for (double v : line) {
            xy.addY(v);
        }
        return getPoly(degree, xy.getX(), xy.getY(), predict);
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
