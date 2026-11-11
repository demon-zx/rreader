package ru.terentjev.service.render.fb2.layout;

import org.apache.commons.lang3.StringUtils;

import ru.terentjev.service.render.fb2.layout.Page.ImageObject;
import ru.terentjev.service.render.fb2.layout.Page.PageObject;
import ru.terentjev.service.render.fb2.layout.Page.Word;
import ru.terentjev.service.render.fb2.parser.FB2;
import ru.terentjev.service.render.fb2.parser.FB2.AbstractPiecesBlock;
import ru.terentjev.service.render.fb2.parser.FB2.Block;
import ru.terentjev.service.render.fb2.parser.FB2.EmptyLineBlock;
import ru.terentjev.service.render.fb2.parser.FB2.ImageBlock;
import ru.terentjev.service.render.fb2.parser.FB2.ParagraphBlock;
import ru.terentjev.service.render.fb2.parser.FB2.Piece;
import ru.terentjev.service.render.fb2.parser.FB2.SubTitleBlock;
import ru.terentjev.service.render.fb2.parser.FB2.TitleBlock;
import ru.terentjev.service.render.fb2.parser.FB2Parser;
import ru.terentjev.service.render.fb2.parser.FB2RenderSettings;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class LayoutManager extends AbstractGraphicsHolder {
    private final FB2RenderSettings settings;

    public LayoutManager(FB2RenderSettings settings) {
        this.settings = settings;
    }

    public List<Page> layout(FB2 document) throws IOException {
        return withGraphics(
                new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB),
                g -> new Layout(settings, g).layout(document)
        );
    }

    private static class Layout {
        private int position;
        private final List<Page> pages = new ArrayList<>();
        // TODO содержание
        private final FB2RenderSettings settings;
        private final Fonts fonts;
        private final int contentTop;
        private final int contentBottom;
        private final int contentWidth;
        private final Graphics2D graphics;
        private final Map<Font, FontMetrics> fontMetrics = new HashMap<>();

        public Layout(FB2RenderSettings settings, Graphics2D graphics) {
            this.settings = settings;
            this.graphics = graphics;
            fonts = new Fonts(settings);
            contentTop = settings.paddingTop();
            contentBottom = settings.pageHeight() - settings.paddingBottom();
            contentWidth = settings.pageWidth() - settings.paddingLeft() - settings.paddingRight();
        }

        private FontMetrics fontMetrics(Font font) {
            return graphics.getFontMetrics(font);
        }

        public List<Page> layout(FB2 document) {
            position = document.body()
                    .stream()
                    .findFirst()
                    .map(Block::position)
                    .orElse(0);
            for (Block block : document.body()) {
                block(document, block);
            }
            if (!objects.isEmpty()) {
                newPage(position);
            }
            return pages;
        }

        private void block(FB2 document, Block block) {
            switch (block) {
                case TitleBlock b -> title(b);
                case SubTitleBlock b -> subTitle(b);
                case ParagraphBlock b -> paragraph(b);
                case EmptyLineBlock b -> emptyLine(b);
                case ImageBlock b -> image(document, b);
                default -> throw new IllegalArgumentException("Unknown type: " + block.getClass());
            }
        }

        private void title(TitleBlock block) {
            heading(block, fonts.title(), y > settings.pageHeight() / 2);
        }

        private void subTitle(SubTitleBlock block) {
            heading(block, fonts.subTitle(), false);
        }

        private void paragraph(ParagraphBlock block) {
            paging(
                    block,
                    fonts.body(),
                    p -> {
                        if (p.bold() && p.italic()) {
                            return fonts.bodyBoldItalic();
                        }
                        if (p.bold()) {
                            return fonts.bodyBold();
                        }
                        if (p.italic()) {
                            return fonts.bodyItalic();
                        }
                        return fonts.body();
                    },
                    false,
                    settings.indent()
            );
        }

        private void emptyLine(EmptyLineBlock block) {
            advance(block.position(), lineHeight(fonts.body()));
        }

        private void image(FB2 document, ImageBlock block) {
            String pathToImage = document.binaries()
                    .get(block.href());
            if (pathToImage != null) {
                if (!objects.isEmpty()) {
                    newPage(block.position());
                }
                objects.add(new ImageObject(pathToImage));
                newPage(block.position());
            }
        }

        private int y = 0;
        private List<PageObject> objects = new ArrayList<>();

        private void heading(AbstractPiecesBlock block, Font font, boolean breakPage) {
            if (breakPage && !objects.isEmpty()) {
                newPage(block.position());
            }
            int half = lineHeight(font) / 2;
            advance(block.position(), half);
            paging(block, font, p -> font, true, 0);
            advance(block.position(), half);
        }

        private void paging(
                AbstractPiecesBlock block,
                Font fontBase,
                Function<Piece, Font> fontPiece,
                boolean centered,
                int paragraphIndent
        ) {
            List<Token> tokens = tokens(block, fontPiece);
            FontMetrics base = fontMetrics(fontBase);
            int ascent = base.getAscent();
            int height = lineHeight(fontBase);
            int space = base.charWidth(' ');
            for (Line line : lines(block.position(), tokens, space, paragraphIndent)) {
                boolean justify = settings.justify() && !centered && !line.last();
                newLine(
                        line.position(),
                        line.tokens(),
                        line.width(),
                        line.indent(),
                        ascent,
                        height,
                        space,
                        centered,
                        justify
                );
            }
        }

        /**
         * Разбивает токены на строки, при необходимости перенося слова по слогам.
         *
         * @param tokens          слова абзаца
         * @param space           ширина пробела
         * @param paragraphIndent отступ первой строки
         * @return строки абзаца
         */
        private List<Line> lines(int startPosition, List<Token> tokens, int space, int paragraphIndent) {
            List<Line> result = new ArrayList<>();
            List<Token> line = new ArrayList<>();
            int width = 0;
            int indent = paragraphIndent;
            int index = 0;
            tokens = new ArrayList<>(tokens);
            while (index < tokens.size()) {
                Token token = tokens.get(index);
                Font font = token.font();
                FontMetrics metrics = fontMetrics(font);
                String text = token.text();
                int tokenWidth = metrics.stringWidth(text);
                int before = line.isEmpty() ? 0 : space;
                int available = contentWidth - indent - width - before;
                if (tokenWidth <= available) {
                    // слово помещается в строку
                    line.add(token);
                    width += before + tokenWidth;
                    index++;
                } else {
                    Hyphenation.Break br = null;
                    if (settings.hyphenation()) {
                        br = Hyphenation.breakWord(text, available, metrics, line.isEmpty());
                    }
                    if (br != null) {
                        // разрываем слово по слогам, остаток переносим на следующую строку
                        Token headToken = new Token(br.head(), font, token.position());
                        Token tailToken = new Token(br.tail(), font, token.position() + br.offset());
                        line.add(headToken);
                        width += before + fontMetrics(font).stringWidth(br.head());
                        result.add(new Line(List.copyOf(line), width, indent, tailToken.position(), false));
                        line = new ArrayList<>();
                        width = 0;
                        indent = 0;
                        tokens.set(index, tailToken);
                    } else if (line.isEmpty()) {
                        // слово не помещается даже в пустую строку - выводим как есть
                        line.add(token);
                        width = tokenWidth;
                        index++;
                    } else {
                        // строка заполнена - слово перейдет на следующую строку
                        result.add(new Line(List.copyOf(line), width, indent, token.position(), false));
                        line = new ArrayList<>();
                        width = 0;
                        indent = 0;
                    }
                }
            }
            if (!line.isEmpty()) {
                int endPosition = startPosition;
                if (!tokens.isEmpty()) {
                    // позиция сразу за последним словом абзаца
                    Token last = tokens.get(tokens.size() - 1);
                    endPosition = last.position() + last.text()
                            .length() + 1;
                }
                result.add(new Line(List.copyOf(line), width, indent, endPosition, true));
            }
            return result;
        }

        /**
         * Распределяет свободное место между словами строки.
         *
         * @param tokens    слова строки
         * @param lineWidth ширина строки без распределения
         * @param indent    отступ строки
         * @param space     обычная ширина пробела
         * @return ширины пробелов между словами строки
         */
        private int[] spaces(List<Token> tokens, int lineWidth, int indent, int space) {
            int count = tokens.size() - 1;
            int[] result = new int[Math.max(count, 0)];
            Arrays.fill(result, space);
            if (count <= 0) {
                return result;
            }
            int extra = contentWidth - indent - lineWidth;
            if (extra <= 0) {
                return result;
            }
            int first = 0;
            var firstToken = tokens.get(0);
            if (notContainsLetter(firstToken.text())) {
                // после первого слова без букв пробел остается обычной ширины
                first = 1;
            }
            int stretchable = count - first;
            if (stretchable <= 0) {
                return result;
            }
            int add = extra / stretchable;
            int rest = extra % stretchable;
            for (int i = first; i < count; i++) {
                result[i] += add;
                if (rest > 0) {
                    result[i]++;
                    rest--;
                }
            }
            return result;
        }

        private static boolean notContainsLetter(String text) {
            for (char c : text.toCharArray()) {
                if (Character.isLetter(c)) {
                    return false;
                }
            }
            return true;
        }

        private void newLine(
                int newPosition,
                List<Token> tokens,
                int lineWidth,
                int indent,
                int ascent,
                int height,
                int space,
                boolean centered,
                boolean justify
        ) {
            if (y + height > contentBottom) {
                newPage(newPosition);
            }
            int[] gaps = justify ? spaces(tokens, lineWidth, indent, space) : null;
            int startX;
            if (centered) {
                startX = settings.paddingLeft() + Math.max(0, (contentWidth - lineWidth) / 2);
            } else {
                startX = settings.paddingLeft() + indent;
            }
            int x = startX;
            int baseline = y + ascent;
            List<Page.Word> words = new ArrayList<>(tokens.size());
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                if (i > 0) {
                    x += gaps == null ? space : gaps[i - 1];
                }
                words.add(new Word(x, token.text(), token.font()));
                x += fontMetrics(token.font()).stringWidth(token.text());
            }
            objects.add(new Page.LineObject(baseline, words));
            y += height;
        }

        private List<Token> tokens(AbstractPiecesBlock block, Function<Piece, Font> font) {
            List<Token> tokens = new ArrayList<>();
            int position = block.position();
            for (Piece piece : block.pieces()) {
                String[] words = piece.text()
                        .trim()
                        .split("\\s+");
                Font f = font.apply(piece);
                for (String word : words) {
                    if (StringUtils.isNotBlank(word)) {
                        tokens.add(new Token(word, f, position));
                    }
                    position += word.length() + 1;
                }
            }
            return tokens;
        }

        private void advance(int newPosition, int amount) {
            y += amount;
            if (y > contentBottom) {
                newPage(newPosition);
            }
        }

        private int lineHeight(Font font) {
            return Math.round(fontMetrics(font).getHeight() * settings.lineHeight());
        }

        private void newPage(int newPosition) {
            var page = new Page(position, List.copyOf(objects));
            objects.clear();
            pages.add(page);
            y = contentTop;
            position = newPosition;
        }

        private record Token(String text, Font font, int position) {
        }

        private record Line(List<Token> tokens, int width, int indent, int position, boolean last) {
        }
    }

    public static void main(String[] args) throws Exception {
        Path base = Path.of("d:/tmp/0");
        FB2Parser parser = new FB2Parser(base);
        // Path file = Path.of("etalon", "more-images.fb2");
        Path file = Path.of("etalon", "3.fb2");
        FB2 document;
        try (var is = Files.newInputStream(file)) {
            document = parser.parse(is);
        }
        FB2RenderSettings settings = new FB2RenderSettings(
                1080,
                1527,
                50,
                50,
                50,
                50,
                50,
                "Arial",
                28,
                1.2f,
                Color.BLACK,
                Color.WHITE,
                true,
                true
        );
        LayoutManager layout = new LayoutManager(settings);
        var pages = layout.layout(document);
        Drawer drawer = new Drawer(settings);
        Path pathPages = base.resolve("pages");
        if (Files.notExists(pathPages)) {
            Files.createDirectories(pathPages);
        }
        int i = 0;
        for (Page page : pages) {
            Path pathPage = pathPages.resolve(++i + ".png");
            try (var os = Files.newOutputStream(pathPage)) {
                System.out.println("Draw page " + i);
                drawer.draw(page, os);
                if (i > 10) {
                    break;
                }
            }
        }
    }
}
