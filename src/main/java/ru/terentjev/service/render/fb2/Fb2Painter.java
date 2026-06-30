package ru.terentjev.service.render.fb2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Lays a list of {@link Fb2Block}s out into fixed-size pages and rasterises a single page to PNG.
 *
 * <p>Layout (pagination) is deliberately independent of the embedded image bytes: every image
 * occupies a fixed-height box, so the number of pages can be computed before the trailing
 * {@code binary} elements are even read. When a page is actually drawn the real image is scaled to
 * fit its box.
 */
public final class Fb2Painter {

    public static final int PAGE_WIDTH = 1080;
    public static final int PAGE_HEIGHT = 1527;

    private static final Logger log = LoggerFactory.getLogger(Fb2Painter.class);

    private static final int MARGIN = 90;
    private static final int CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;
    private static final int CONTENT_TOP = MARGIN;
    private static final int CONTENT_BOTTOM = PAGE_HEIGHT - MARGIN;

    private static final int BODY_SIZE = 34;
    private static final int TITLE_SIZE = 50;
    private static final int SUBTITLE_SIZE = 40;
    private static final float LINE_SPACING = 1.35f;
    private static final int PARAGRAPH_INDENT = 50;
    private static final int PARAGRAPH_GAP = 14;
    private static final int IMAGE_BOX_HEIGHT = 760;

    private final Font bodyFont = new Font(Font.SERIF, Font.PLAIN, BODY_SIZE);
    private final Font bodyBold = bodyFont.deriveFont(Font.BOLD);
    private final Font bodyItalic = bodyFont.deriveFont(Font.ITALIC);
    private final Font bodyBoldItalic = bodyFont.deriveFont(Font.BOLD | Font.ITALIC);
    private final Font titleFont = new Font(Font.SERIF, Font.BOLD, TITLE_SIZE);
    private final Font subtitleFont = new Font(Font.SERIF, Font.BOLD, SUBTITLE_SIZE);

    /**
     * Paginates the blocks. The result always contains at least one (possibly empty) page.
     *
     * @param blocks the body blocks
     * @return the laid out pages
     */
    public List<Page> layout(List<Fb2Block> blocks) {
        BufferedImage scratch = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scratch.createGraphics();
        applyHints(g);
        List<Page> pages = new Layout(g).run(blocks);
        g.dispose();
        return pages;
    }

    /**
     * Returns a single empty page, used as a fallback when there is nothing to render.
     *
     * @return an empty page
     */
    public Page emptyPage() {
        return new Page();
    }

    /**
     * Rasterises a page to PNG and writes it to the stream.
     *
     * @param page the page to draw
     * @param document the source document, used to resolve image binaries
     * @param os the PNG output
     * @throws IOException if writing fails
     */
    public void draw(Page page, Fb2Document document, OutputStream os) throws IOException {
        BufferedImage image = new BufferedImage(PAGE_WIDTH, PAGE_HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, PAGE_WIDTH, PAGE_HEIGHT);
        applyHints(g);
        g.setColor(Color.BLACK);
        for (ImageBox box : page.images()) {
            drawImage(g, box, document);
        }
        for (Line line : page.lines()) {
            for (Segment segment : line.segments()) {
                g.setFont(segment.font());
                g.drawString(segment.text(), segment.x(), line.baseline());
            }
        }
        g.dispose();
        ImageIO.write(image, "PNG", os);
        os.flush();
    }

    private void drawImage(Graphics2D g, ImageBox box, Fb2Document document) {
        byte[] data = document.binary(box.href());
        if (data == null) {
            return;
        }
        try {
            BufferedImage src = ImageIO.read(new ByteArrayInputStream(data));
            if (src == null) {
                return;
            }
            double scale = Math.min(
                    (double) box.width() / src.getWidth(),
                    (double) box.height() / src.getHeight());
            scale = Math.min(scale, 1.0);
            int w = (int) Math.round(src.getWidth() * scale);
            int h = (int) Math.round(src.getHeight() * scale);
            int x = box.x() + (box.width() - w) / 2;
            int y = box.y() + (box.height() - h) / 2;
            g.drawImage(src, x, y, w, h, null);
        } catch (IOException e) {
            log.warn("Cannot draw image {}", box.href(), e);
        }
    }

    private void applyHints(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    private Font fontFor(Fb2Run run) {
        if (run.bold() && run.italic()) {
            return bodyBoldItalic;
        }
        if (run.bold()) {
            return bodyBold;
        }
        if (run.italic()) {
            return bodyItalic;
        }
        return bodyFont;
    }

    /**
     * A positioned styled word ready to be drawn.
     */
    private record Segment(String text, Font font, int x) {
    }

    /**
     * A laid out line: the segments plus the absolute baseline they share.
     */
    private record Line(int baseline, List<Segment> segments) {
    }

    /**
     * A reserved rectangle for an embedded image, resolved to bytes only at draw time.
     */
    private record ImageBox(String href, int x, int y, int width, int height) {
    }

    /**
     * A token (word) carrying the font it must be drawn with.
     */
    private record Token(String text, Font font) {
    }

    /**
     * One rendered page: its text lines and image boxes.
     */
    public static final class Page {

        private final List<Line> lines = new ArrayList<>();
        private final List<ImageBox> images = new ArrayList<>();

        private void addLine(Line line) {
            lines.add(line);
        }

        private void addImage(ImageBox box) {
            images.add(box);
        }

        private boolean isEmpty() {
            return lines.isEmpty() && images.isEmpty();
        }

        private List<Line> lines() {
            return lines;
        }

        private List<ImageBox> images() {
            return images;
        }
    }

    /**
     * Carries the per-pass cursor (current page and vertical position) through a single layout.
     */
    private final class Layout {

        private final Graphics2D g;
        private final List<Page> pages = new ArrayList<>();
        private Page page = new Page();
        private int y = CONTENT_TOP;

        Layout(Graphics2D g) {
            this.g = g;
        }

        List<Page> run(List<Fb2Block> blocks) {
            for (Fb2Block block : blocks) {
                block(block);
            }
            if (!page.isEmpty()) {
                pages.add(page);
            }
            if (pages.isEmpty()) {
                pages.add(page);
            }
            return pages;
        }

        private void block(Fb2Block block) {
            switch (block.kind()) {
                case TITLE -> heading(block, titleFont, true);
                case SUBTITLE -> heading(block, subtitleFont, false);
                case PARAGRAPH -> paragraph(block);
                case EMPTY_LINE -> emptyLine();
                case IMAGE -> image(block.imageHref());
                default -> { }
            }
        }

        private void heading(Fb2Block block, Font font, boolean chapter) {
            if (chapter && !page.isEmpty()) {
                newPage();
            }
            int half = lineHeight(font) / 2;
            advance(half);
            wrap(tokens(block.runs(), font), font, true, 0);
            advance(half);
        }

        private void paragraph(Fb2Block block) {
            wrap(tokens(block.runs(), null), bodyFont, false, PARAGRAPH_INDENT);
            advance(PARAGRAPH_GAP);
        }

        private void emptyLine() {
            advance(lineHeight(bodyFont));
        }

        private void image(String href) {
            if (href == null) {
                return;
            }
            int boxHeight = Math.min(IMAGE_BOX_HEIGHT, CONTENT_BOTTOM - CONTENT_TOP);
            if (y + boxHeight > CONTENT_BOTTOM) {
                newPage();
            }
            page.addImage(new ImageBox(href, MARGIN, y, CONTENT_WIDTH, boxHeight));
            y += boxHeight + PARAGRAPH_GAP;
        }

        private List<Token> tokens(List<Fb2Run> runs, Font forced) {
            List<Token> tokens = new ArrayList<>();
            for (Fb2Run run : runs) {
                Font font = forced != null ? forced : fontFor(run);
                for (String word : run.text().trim().split("\\s+")) {
                    if (!word.isEmpty()) {
                        tokens.add(new Token(word, font));
                    }
                }
            }
            return tokens;
        }

        private void wrap(List<Token> tokens, Font baseFont, boolean centered, int firstIndent) {
            FontMetrics base = g.getFontMetrics(baseFont);
            int ascent = base.getAscent();
            int height = lineHeight(baseFont);
            int space = base.charWidth(' ');
            List<Token> lineTokens = new ArrayList<>();
            int lineWidth = 0;
            int indent = firstIndent;
            for (Token token : tokens) {
                int width = g.getFontMetrics(token.font()).stringWidth(token.text());
                int add = lineTokens.isEmpty() ? width : space + width;
                if (!lineTokens.isEmpty() && indent + lineWidth + add > CONTENT_WIDTH) {
                    emitLine(lineTokens, lineWidth, indent, ascent, height, space, centered);
                    lineTokens = new ArrayList<>();
                    lineWidth = 0;
                    indent = 0;
                }
                lineWidth += lineTokens.isEmpty() ? width : space + width;
                lineTokens.add(token);
            }
            if (!lineTokens.isEmpty()) {
                emitLine(lineTokens, lineWidth, indent, ascent, height, space, centered);
            }
        }

        private void emitLine(
                List<Token> tokens,
                int lineWidth,
                int indent,
                int ascent,
                int height,
                int space,
                boolean centered) {
            if (y + height > CONTENT_BOTTOM) {
                newPage();
            }
            int startX = centered ? MARGIN + Math.max(0, (CONTENT_WIDTH - lineWidth) / 2) : MARGIN + indent;
            int x = startX;
            int baseline = y + ascent;
            List<Segment> segments = new ArrayList<>(tokens.size());
            for (int i = 0; i < tokens.size(); i++) {
                Token token = tokens.get(i);
                if (i > 0) {
                    x += space;
                }
                segments.add(new Segment(token.text(), token.font(), x));
                x += g.getFontMetrics(token.font()).stringWidth(token.text());
            }
            page.addLine(new Line(baseline, segments));
            y += height;
        }

        private void advance(int amount) {
            y += amount;
            if (y > CONTENT_BOTTOM) {
                newPage();
            }
        }

        private void newPage() {
            pages.add(page);
            page = new Page();
            y = CONTENT_TOP;
        }

        private int lineHeight(Font font) {
            return Math.round(g.getFontMetrics(font).getHeight() * LINE_SPACING);
        }
    }
}
