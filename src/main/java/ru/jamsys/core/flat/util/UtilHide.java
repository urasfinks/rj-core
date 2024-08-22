package ru.jamsys.core.flat.util;

public class UtilHide {

    public static String mask(String str, int countFirst, int countLast) {
        return mask(str, countFirst, countLast, 50);
    }

    public static String mask(String str, int countFirst, int countLast, int maskPrc) {
        int countShowCharacter = str.length() - (int) Math.ceil(Double.parseDouble(maskPrc + "") * str.length() / 100);
        //System.out.println("Можно показать: " + countShowCharacter);

        if (countFirst + countLast > countShowCharacter) {
            double fPrc = Double.parseDouble(countFirst + "") * 100 / (countFirst + countLast);

            countFirst = (int) Math.floor(fPrc * countShowCharacter / 100);
            if (countFirst <= 0) {
                countFirst = 1;
            }
            countLast = countShowCharacter - countFirst;
            if (countLast < 0) {
                countLast = 0;
            }
//            System.out.println("prc f: " + fPrc + " = " + countFirst);
//            System.out.println("prc e: " + " = " + countLast);

        }

        switch (str.length()) {
            case 0, 1:
                return str;
            case 2:
                return str.charAt(0) + "*";
            case 3:
                return str.charAt(0) + "*" + str.charAt(2);
        }
        int tail = str.length() - countFirst;
        if (tail >= countLast * 2) {
            int middle = str.length() - countFirst - countLast;
            return str.substring(0, countFirst) + Util.padLeft("", middle, "*") + str.substring(countFirst + middle);
        }
        int middle = (int) Math.ceil(Double.parseDouble(tail + "") / 2);
        return str.substring(0, countFirst) + Util.padLeft("", middle, "*") + str.substring(countFirst + middle);
    }

}
