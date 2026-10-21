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
import java.util.List;
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

        public Layout(FB2RenderSettings settings, Graphics2D graphics) {
            this.settings = settings;
            this.graphics = graphics;
            fonts = new Fonts(settings);
            contentTop = settings.paddingTop();
            contentBottom = settings.pageHeight() - settings.paddingBottom();
            contentWidth = settings.pageWidth() - settings.paddingLeft() - settings.paddingRight();
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
            int position = block.position();
            List<Token> tokens = tokens(block, fontPiece);
            FontMetrics base = graphics.getFontMetrics(fontBase);
            int ascent = base.getAscent();
            int height = lineHeight(fontBase);
            int space = base.charWidth(' ');
            List<Token> line = new ArrayList<>();
            int lineWidth = 0;
            int indent = paragraphIndent;
            for (Token token : tokens) {
                int tokenWidth = graphics.getFontMetrics(token.font())
                        .stringWidth(token.text());
                if (indent + lineWidth + space + tokenWidth > contentWidth) {
                    newLine(position, List.copyOf(line), lineWidth, indent, ascent, height, space, centered);
                    line.clear();
                    lineWidth = 0;
                    indent = 0;
                }
                position += token.text()
                        .length() + 1;
                lineWidth += lineWidth == 0 ? tokenWidth : space + tokenWidth;
                line.add(token);
            }
            if (!line.isEmpty()) {
                newLine(position, line, lineWidth, indent, ascent, height, space, centered);
            }
        }

        private void newLine(
                int newPosition,
                List<Token> tokens,
                int lineWidth,
                int indent,
                int ascent,
                int height,
                int space,
                boolean centered
        ) {
            if (y + height > contentBottom) {
                newPage(newPosition);
            }
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
                    x += space;
                }
                words.add(new Word(x, token.text(), token.font()));
                x += graphics.getFontMetrics(token.font())
                        .stringWidth(token.text());
            }
            objects.add(new Page.LineObject(baseline, words));
            y += height;
        }

        private List<Token> tokens(AbstractPiecesBlock block, Function<Piece, Font> font) {
            List<Token> tokens = new ArrayList<>();
            for (Piece piece : block.pieces()) {
                String[] words = piece.text()
                        .trim()
                        .split("\\s+");
                Font f = font.apply(piece);
                for (String word : words) {
                    if (StringUtils.isNotBlank(word)) {
                        tokens.add(new Token(word, f));
                    }
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
            FontMetrics fontMetrics = graphics.getFontMetrics(font);
            return Math.round(fontMetrics.getHeight() * settings.lineHeight());
        }

        private void newPage(int newPosition) {
            var page = new Page(position, List.copyOf(objects));
            objects.clear();
            pages.add(page);
            y = contentTop;
            position = newPosition;
        }

        private record Token(String text, Font font) {
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
                Color.WHITE
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
