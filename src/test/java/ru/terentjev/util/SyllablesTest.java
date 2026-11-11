package ru.terentjev.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyllablesTest {
    @Test
    void splitsWordByVowels() {
        assertEquals(List.of("сло", "во"), Syllables.split("слово"));
        assertEquals(List.of("че", "ло", "век"), Syllables.split("человек"));
        assertEquals(List.of("ав", "то", "бус"), Syllables.split("автобус"));
        assertEquals(List.of("зо", "ло", "то"), Syllables.split("золото"));
    }

    @Test
    void singleConsonantBetweenVowelsGoesToNextSyllable() {
        assertEquals(List.of("у", "хо", "дить"), Syllables.split("уходить"));
        assertEquals(List.of("взво", "дить"), Syllables.split("взводить"));
    }

    @Test
    void twoConsonantsBetweenVowelsAreSplit() {
        assertEquals(List.of("мас", "си", "ра"), Syllables.split("массира"));
        assertEquals(List.of("вок", "зал"), Syllables.split("вокзал"));
    }

    @Test
    void threeConsonantsLeaveOneToPreviousSyllable() {
        assertEquals(List.of("мед", "ведь"), Syllables.split("медведь"));
        assertEquals(List.of("здрав", "ствуй", "те"), Syllables.split("здравствуйте"));
    }

    @Test
    void softSignStaysWithPrecedingConsonant() {
        assertEquals(List.of("пись", "мо"), Syllables.split("письмо"));
        assertEquals(List.of("лис", "тья"), Syllables.split("листья"));
        assertEquals(List.of("день", "ги"), Syllables.split("деньги"));
        assertEquals(List.of("маль", "чик"), Syllables.split("мальчик"));
        assertEquals(List.of("паль", "то"), Syllables.split("пальто"));
    }

    @Test
    void endingsWithSoftSignAndSAreNotSplit() {
        assertEquals(List.of("у", "чи", "ться"), Syllables.split("учиться"));
        assertEquals(List.of("встре", "ти", "ться"), Syllables.split("встретиться"));
        assertEquals(List.of("ста", "тья"), Syllables.split("статья"));
    }

    @Test
    void hardSignStaysWithPrecedingConsonant() {
        assertEquals(List.of("объ", "ект"), Syllables.split("объект"));
        assertEquals(List.of("разъ", "яс", "не", "ни", "е"), Syllables.split("разъяснение"));
        assertEquals(List.of("подъ", "езд"), Syllables.split("подъезд"));
    }

    @Test
    void wordWithSingleVowelIsNotSplit() {
        assertEquals(List.of("стол"), Syllables.split("стол"));
        assertEquals(List.of("сталь"), Syllables.split("сталь"));
        assertEquals(List.of("ъ"), Syllables.split("ъ"));
    }

    @Test
    void emptyWordIsNotSplit() {
        assertEquals(List.of(), Syllables.split(""));
        assertEquals(List.of(), Syllables.split(null));
    }

    @Test
    void hyphenIsBoundaryOfSyllables() {
        assertEquals(List.of("ка", "кой-", "то"), Syllables.split("какой-то"));
        assertEquals(List.of(2, 5), Syllables.splitPoints("какой-то"));
    }

    @Test
    void splitPointsPointToBeginningOfNextPart() {
        assertEquals(List.of(3, 5), Syllables.splitPoints("массира"));
        assertEquals(List.of(), Syllables.splitPoints("стол"));
    }

    @Test
    void detectsVowels() {
        assertTrue(Syllables.isVowel('а'));
        assertTrue(Syllables.isVowel('Ё'));
        assertTrue(Syllables.isVowel('e'));
        assertFalse(Syllables.isVowel('ь'));
        assertFalse(Syllables.isVowel('1'));
    }
}
