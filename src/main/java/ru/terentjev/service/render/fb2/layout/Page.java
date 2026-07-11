package ru.terentjev.service.render.fb2.layout;

import java.awt.Font;
import java.util.List;

public record Page(int position, List<PageObject> objects) {
    public static sealed class PageObject permits LineObject, ImageObject {
        
    }

    public static final class LineObject extends PageObject {
        private final int y;
        private final List<Word> words;
        
        public LineObject(int y, List<Word> words) {
            this.y = y;
            this.words = words;
        }
        
        public int y() {
            return y;
        }
        
        public List<Word> words() {
            return words;
        }
        
    }

    public record Word(int x, String value, Font font) {
    }

    public static final class ImageObject extends PageObject {
        
        private final String href;

        public ImageObject(String href) {
            this.href = href;

        }
        
        public String href() {
            return href;
        }
        
    }

}
