package ru.terentjev.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Разбиение слов на слоги.
 * <p>
 * В слоге ровно одна гласная. Согласные между двумя гласными делятся так, чтобы следующий
 * слог получал как можно меньше согласных, но мягкий и твёрдый знаки никогда не отрываются
 * от предыдущей согласной, окончания "-сть" и "-тся" не разделяются, а "й" относится
 * к предыдущему слогу.
 */
public final class Syllables {
    private static final String VOWELS = "аеёиоуыэюя";
    private static final String VOWELS_LATIN = "aeiouy";
    private static final char SOFT_SIGN = 'ь';
    private static final char HARD_SIGN = 'ъ';
    private static final char SHORT_I = 'й';
    private static final char SI = 'с';

    private Syllables() {
    }

    public static boolean isVowel(char c) {
        char lower = Character.toLowerCase(c);
        return VOWELS.indexOf(lower) >= 0 || VOWELS_LATIN.indexOf(lower) >= 0;
    }

    /**
     * Разбивает слово на слоги.
     *
     * @param word слово
     * @return список слогов, если в слове меньше двух гласных - слово возвращается целиком
     */
    public static List<String> split(String word) {
        if (word == null || word.isEmpty()) {
            return List.of();
        }
        List<Integer> points = splitPoints(word);
        if (points.isEmpty()) {
            return List.of(word);
        }
        List<String> parts = new ArrayList<>(points.size() + 1);
        int start = 0;
        for (int point : points) {
            parts.add(word.substring(start, point));
            start = point;
        }
        parts.add(word.substring(start));
        List<String> result = new ArrayList<>(parts.size());
        for (String part : parts) {
            if (!result.isEmpty() && part.startsWith("-")) {
                // дефис остается в предыдущем слоге
                int last = result.size() - 1;
                result.set(last, result.get(last) + "-");
                part = part.substring(1);
                if (part.isEmpty()) {
                    continue;
                }
            }
            result.add(part);
        }
        return result;
    }

    /**
     * Позиции, на которых слово можно разорвать.
     *
     * @param word слово
     * @return индексы первых букв частей слова, начиная со второй
     */
    public static List<Integer> splitPoints(String word) {
        if (word == null || word.length() < 2) {
            return List.of();
        }
        List<Integer> points = new ArrayList<>();
        int offset = 0;
        for (String part : word.split("-", -1)) {
            points.addAll(partPoints(part, offset));
            offset += part.length();
            if (offset < word.length()) {
                // дефис - тоже место разрыва
                points.add(offset);
                offset++;
            }
        }
        return points;
    }

    /**
     * Позиции, на которых часть слова можно разорвать.
     *
     * @param part   часть слова
     * @param offset смещение части в слове
     * @return индексы первых букв частей, начиная со второй
     */
    private static List<Integer> partPoints(String part, int offset) {
        List<Integer> vowels = new ArrayList<>();
        for (int i = 0; i < part.length(); i++) {
            if (isVowel(part.charAt(i))) {
                vowels.add(i);
            }
        }
        if (vowels.size() < 2) {
            return List.of();
        }
        List<Integer> points = new ArrayList<>(vowels.size() - 1);
        for (int i = 0; i < vowels.size() - 1; i++) {
            points.add(offset + splitPoint(part, vowels.get(i), vowels.get(i + 1)));
        }
        return points;
    }

    /**
     * Ищет границу слогов между двумя гласными.
     *
     * @param word   слово
     * @param first  индекс первой гласной
     * @param second индекс второй гласной
     * @return индекс буквы, с которой начинается следующий слог
     */
    private static int splitPoint(String word, int first, int second) {
        List<Integer> cluster = cluster(word, first, second);
        if (cluster.isEmpty()) {
            // между гласными нет согласных
            return second;
        }
        if (word.charAt(cluster.get(cluster.size() - 1)) == SHORT_I) {
            // "й" не начинает слог, значит остается в предыдущем
            return second;
        }
        int take = take(word, cluster);
        if (take <= 0) {
            return cluster.get(0);
        }
        if (take >= cluster.size()) {
            return second;
        }
        return cluster.get(take);
    }

    /**
     * Собирает согласные между двумя гласными.
     *
     * @param word   слово
     * @param first  индекс первой гласной
     * @param second индекс второй гласной
     * @return индексы согласных
     */
    private static List<Integer> cluster(String word, int first, int second) {
        List<Integer> cluster = new ArrayList<>();
        for (int i = first + 1; i < second; i++) {
            if (i == first + 1 && word.charAt(i) == SHORT_I) {
                // "й" после гласной относится к предыдущему слогу
                continue;
            }
            cluster.add(i);
        }
        return cluster;
    }

    /**
     * Считает, сколько согласных между гласными остается в предыдущем слоге.
     *
     * @param word    слово
     * @param cluster согласные между гласными
     * @return количество согласных предыдущего слога
     */
    private static int take(String word, List<Integer> cluster) {
        for (int i = 0; i < cluster.size() - 1; i++) {
            if (word.charAt(cluster.get(i)) == SOFT_SIGN && word.charAt(cluster.get(i + 1)) == SI) {
                // окончания "-сть" и "-тся" не разделяются
                return 0;
            }
        }
        List<Unit> units = units(word, cluster);
        int sounds = 0;
        for (Unit unit : units) {
            sounds += unit.sounds();
        }
        if (sounds <= 1) {
            return 0;
        }
        // одну согласную оставляем предыдущему слогу, остальные - следующему
        return units.get(0)
                .size();
    }

    /**
     * Делит согласные между двумя гласными на неразрывные группы.
     *
     * @param word    слово
     * @param cluster согласные между гласными
     * @return группы согласных
     */
    private static List<Unit> units(String word, List<Integer> cluster) {
        List<Unit> units = new ArrayList<>();
        int i = 0;
        while (i < cluster.size()) {
            int size = 1;
            int sounds = 1;
            if (i + 1 < cluster.size() && isSign(word.charAt(cluster.get(i + 1)))) {
                // знак не отрывается от согласной
                size++;
                if (word.charAt(cluster.get(i + 1)) != SOFT_SIGN) {
                    sounds++;
                }
            }
            units.add(new Unit(size, sounds));
            i += size;
        }
        return units;
    }

    private static boolean isSign(char c) {
        return c == SOFT_SIGN || c == HARD_SIGN;
    }

    /**
     * Группа согласных, которая не разделяется между слогами.
     *
     * @param size   количество букв
     * @param sounds количество звуков
     */
    private record Unit(int size, int sounds) {
    }
}
