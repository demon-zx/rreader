package ru.terentjev.service.render.fb2.layout;

import org.junit.jupiter.api.Test;

import ru.terentjev.service.render.BundledFont;
import ru.terentjev.service.render.fb2.layout.Hyphenation.Break;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HyphenationTest {
    private static final Font FONT = new Font(BundledFont.SANS, Font.PLAIN, 20);

    @Test
    void wordIsBrokenBySyllables() {
        FontMetrics metrics = metrics();
        Break br = Hyphenation.breakWord("здравствуйте", withHyphen(metrics, "здрав"), metrics, false);
        assertNotNull(br);
        assertEquals("здрав-", br.head());
        assertEquals("ствуйте", br.tail());
        assertEquals(5, br.offset());
    }

    @Test
    void longestPartThatFitsIsChosen() {
        FontMetrics metrics = metrics();
        Break br = Hyphenation.breakWord("здравствуйте", withHyphen(metrics, "здравствуй"), metrics, false);
        assertNotNull(br);
        assertEquals("здравствуй-", br.head());
        assertEquals("те", br.tail());
        assertEquals(10, br.offset());
    }

    @Test
    void wordIsNotBrokenWhenItDoesNotFit() {
        FontMetrics metrics = metrics();
        assertNull(Hyphenation.breakWord("здравствуйте", 1, metrics, false));
    }

    @Test
    void wordWithCapitalLetterIsNotBroken() {
        FontMetrics metrics = metrics();
        assertNull(Hyphenation.breakWord("Здравствуйте", metrics.stringWidth("здра-"), metrics, false));
    }

    @Test
    void capitalizedWordAloneOnLineIsBroken() {
        FontMetrics metrics = metrics();
        Break br = Hyphenation.breakWord("Здравствуйте", withHyphen(metrics, "Здрав"), metrics, true);
        assertNotNull(br);
        assertEquals("Здрав-", br.head());
        assertEquals("ствуйте", br.tail());
    }

    @Test
    void wordWithDashIsBrokenByDash() {
        FontMetrics metrics = metrics();
        Break br = Hyphenation.breakWord("санкт-петербург", metrics.stringWidth("санкт-"), metrics, false);
        assertNotNull(br);
        assertEquals("санкт-", br.head());
        assertEquals("петербург", br.tail());
        assertEquals(6, br.offset());
    }

    @Test
    void wordWithDashAndCapitalLetterIsNotBroken() {
        FontMetrics metrics = metrics();
        assertNull(Hyphenation.breakWord("Санкт-Петербург", metrics.stringWidth("санкт-"), metrics, false));
    }

    @Test
    void partWithLessThanTwoLettersIsNotBroken() {
        FontMetrics metrics = metrics();
        // "имя" разрывается после первой буквы
        assertNull(Hyphenation.breakWord("имя", withHyphen(metrics, "и"), metrics, false));
    }

    @Test
    void tailWithLessThanTwoLettersIsNotBroken() {
        FontMetrics metrics = metrics();
        // "маи" разрывается так, что в хвосте остается одна буква
        assertNull(Hyphenation.breakWord("маи", withHyphen(metrics, "ма"), metrics, false));
    }

    @Test
    void wordIsNotBrokenWhenThereIsNoFreeSpace() {
        FontMetrics metrics = metrics();
        assertNull(Hyphenation.breakWord("здравствуйте", 0, metrics, false));
    }

    /**
     * Ширина части слова вместе с добавленным дефисом.
     */
    private static int withHyphen(FontMetrics metrics, String part) {
        return metrics.stringWidth(part) + metrics.stringWidth("-");
    }

    private static FontMetrics metrics() {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        try {
            return graphics.getFontMetrics(FONT);
        } finally {
            graphics.dispose();
        }
    }
}
