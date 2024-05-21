package ru.jamsys.core.flat.trend;

import org.apache.commons.math3.stat.regression.SimpleRegression;

public class LinearTrendLine {

    public static double main(double[] line, double predict) {
        double[] x = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            x[i] = i;
        }
        return main(x, line, predict);
    }

    public static double main(double[] x, double[] y, double predict) {
        SimpleRegression simpleRegression = new SimpleRegression(true);
        double[][] doubles = new double[x.length][2];
        for (int i = 0; i < x.length; i++) {
            doubles[i][0] = x[i];
            doubles[i][1] = y[i];
        }
        simpleRegression.addData(doubles);
        // System.out.println("slope = " + simpleRegression.getSlope());
        // System.out.println("intercept = " + simpleRegression.getIntercept());
        return simpleRegression.predict(x.length - 1 + predict);
    }
}
