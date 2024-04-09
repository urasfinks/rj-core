package ru.jamsys.component;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.jamsys.trend.LinearTrendLine;
import ru.jamsys.trend.PolyTrendLine;

@Component
@Lazy
public class Trend {

    public double getPoly(int degree, double[] line) {
        return getPoly(degree, line, 1);
    }

    public double getPoly(int degree, double[] x, double[] y, double predict) {
        PolyTrendLine t = new PolyTrendLine(degree);
        t.setValues(y, x);
        return t.predict(x.length + predict);
    }

    public double getPoly(int degree, double[] line, double predict) {
        double[] x = new double[line.length];
        for (int i = 0; i < line.length; i++) {
            x[i] = i + 1;
        }
        return getPoly(degree, x, line, predict);
    }

    public double getLinear(double[] line) {
        return getLinear(line, 1);
    }

    public double getLinear(double[] line, double predict) {
        return LinearTrendLine.main(line, predict);
    }

    public double getLinear(double[] x, double[] y, double predict) {
        return LinearTrendLine.main(x, y, predict);
    }

}
