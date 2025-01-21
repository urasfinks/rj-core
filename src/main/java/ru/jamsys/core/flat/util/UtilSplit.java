package ru.jamsys.core.flat.util;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class UtilSplit {

    @SuppressWarnings("unused")
    public static List<String> splitMessageSmart(String message, int maxLength) {
        List<String> parts = new ArrayList<>();

        while (message.length() > maxLength) {
            // Попробуем найти перенос строки или конец предложения
            int splitIndex = findSplitIndex(message, maxLength);

            // Добавляем часть текста в список
            parts.add(message.substring(0, splitIndex).trim());
            // Обрезаем обработанную часть
            message = message.substring(splitIndex).trim();
        }

        // Добавляем оставшийся текст
        if (!message.isEmpty()) {
            parts.add(message);
        }

        return parts;
    }

    public static int findSplitIndex(String message, int maxLength) {
        // Ищем последний перенос строки до maxLength
        int newlineIndex = message.lastIndexOf('\n', maxLength);
        if (newlineIndex != -1) {
            return newlineIndex + 1; // Включаем перенос строки
        }

        // Ищем конец предложения (точка, восклицательный знак, вопросительный знак)
        int sentenceEndIndex = Math.max(
                Math.max(message.lastIndexOf('.', maxLength), message.lastIndexOf('!', maxLength)),
                message.lastIndexOf('?', maxLength)
        );
        if (sentenceEndIndex != -1) {
            return sentenceEndIndex + 1; // Включаем знак конца предложения
        }

        // Если ничего не найдено, разрезаем по maxLength
        return maxLength;
    }

}
