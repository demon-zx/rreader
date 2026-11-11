package ru.terentjev.service.render.fb2.layout;

import ru.terentjev.util.Syllables;

import java.awt.FontMetrics;
import java.util.List;

/**
 * Перенос слов по слогам.
 * <p>
 * Слово разрывается по границе слогов, но не если оно содержит заглавную букву:
 * такое слово может оказаться именем собственным. Исключение - слово, которое одно
 * в строке и не помещается в неё: тогда разорвать его можно.
 */
public final class Hyphenation {
    private static final char DASH = '-';
    private static final String HYPHEN = String.valueOf(DASH);
    /** Минимальное количество букв перед переносом. */
    private static final int MIN_HEAD = 2;
    /** Минимальное количество букв после переноса. */
    private static final int MIN_TAIL = 2;

    private Hyphenation() {
    }

    /**
     * Ищет место разрыва слова, чтобы его часть поместилась в строке.
     *
     * @param text      слово
     * @param available свободная ширина строки
     * @param metrics   метрики шрифта слова
     * @param alone     слово единственное в строке
     * @return разрыв слова или null, если разорвать слово нельзя
     */
    public static Break breakWord(String text, int available, FontMetrics metrics, boolean alone) {
        if (available <= 0) {
            return null;
        }
        if (hasCapital(text) && !alone) {
            // слово с большой буквы не переносим - это может оказаться имя собственное
            return null;
        }
        List<Integer> points = Syllables.splitPoints(text);
        for (int i = points.size() - 1; i >= 0; i--) {
            int point = points.get(i);
            boolean dash = text.charAt(point) == DASH;
            int head = dash ? point + 1 : point;
            if (head < MIN_HEAD || text.length() - head < MIN_TAIL) {
                continue;
            }
            int width = metrics.stringWidth(text.substring(0, head));
            if (!dash) {
                width += metrics.stringWidth(HYPHEN);
            }
            if (width <= available) {
                String headText = dash ? text.substring(0, head) : text.substring(0, head) + HYPHEN;
                return new Break(headText, text.substring(head), head);
            }
        }
        return null;
    }

    private static boolean hasCapital(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (Character.isUpperCase(text.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Разрыв слова.
     *
     * @param head   первая часть слова, с дефисом, если он добавлен
     * @param tail   вторая часть слова
     * @param offset количество букв исходного слова в первой части
     */
    public record Break(String head, String tail, int offset) {
    }
}
