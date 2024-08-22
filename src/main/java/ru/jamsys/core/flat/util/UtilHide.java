package ru.jamsys.core.flat.util;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class UtilHide {

    public static String mask(String str, int countFirst, int countLast, int maskPrc) {
        return mask(str, countFirst, countLast, maskPrc, "*");
    }

    public static String mask(String str, int countFirst, int countLast, int maskPrc, String ch) {
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
                return str.charAt(0) + ch;
            case 3:
                return str.charAt(0) + ch + str.charAt(2);
        }
        int tail = str.length() - countFirst;
        if (tail >= countLast * 2) {
            int middle = str.length() - countFirst - countLast;
            return str.substring(0, countFirst) + Util.padLeft("", middle, ch) + str.substring(countFirst + middle);
        }
        int middle = (int) Math.ceil(Double.parseDouble(tail + "") / 2);
        return str.substring(0, countFirst) + Util.padLeft("", middle, ch) + str.substring(countFirst + middle);
    }

    @Getter
    @Setter
    public static class StringItem {
        boolean letter;
        StringBuilder value = new StringBuilder();

        public StringItem(boolean letter) {
            this.letter = letter;
        }

        @Override
        public String toString() {
            return "letter=" + letter + ", value=" + value + '}';
        }
    }

    public static List<StringItem> split(String str) {
        List<StringItem> result = new ArrayList<>();
        StringItem last = null;
        for (int i = 0; i < str.length(); i++) {
            String ch = str.charAt(i) + "";
            boolean letter = isLetter(ch);
            if (last == null || last.isLetter() != letter) {
                result.add(new StringItem(letter));
                last = result.getLast();
            }
            last.getValue().append(ch);
        }
        return result;
    }

    public static boolean isLetter(String ch) {
        return !ch.toLowerCase().equals(ch.toUpperCase()) || Util.isNumeric(ch);
    }

    public static String explodeLetterAndMask(String str, int countFirst, int countLast, int maskPrc, String ch) {
        StringBuilder sb = new StringBuilder();
        for (StringItem stringItem : split(str)) {
            if (stringItem.isLetter()) {
                sb.append(mask(stringItem.getValue().toString(), countFirst, countLast, maskPrc, ch));
            } else {
                sb.append(stringItem.getValue().toString());
            }
        }
        return sb.toString();
    }

}
