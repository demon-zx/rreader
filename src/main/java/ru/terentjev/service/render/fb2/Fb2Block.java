package ru.terentjev.service.render.fb2;

import java.util.List;

/**
 * A single layout-level block of the book body produced by the streaming parser.
 *
 * <p>Blocks are intentionally lightweight (text runs and image references only), so the whole
 * body of a book can be held in memory while the heavyweight binary payloads are decoded lazily.
 */
public final class Fb2Block {

    /**
     * Kind of a body block, driving how the painter lays it out.
     */
    public enum Kind {
        TITLE,
        SUBTITLE,
        PARAGRAPH,
        EMPTY_LINE,
        IMAGE
    }

    private final Kind kind;
    private final List<Fb2Run> runs;
    private final String imageHref;

    private Fb2Block(Kind kind, List<Fb2Run> runs, String imageHref) {
        this.kind = kind;
        this.runs = runs;
        this.imageHref = imageHref;
    }

    public static Fb2Block text(Kind kind, List<Fb2Run> runs) {
        return new Fb2Block(kind, List.copyOf(runs), null);
    }

    public static Fb2Block emptyLine() {
        return new Fb2Block(Kind.EMPTY_LINE, List.of(), null);
    }

    public static Fb2Block image(String href) {
        return new Fb2Block(Kind.IMAGE, List.of(), href);
    }

    public Kind kind() {
        return kind;
    }

    public List<Fb2Run> runs() {
        return runs;
    }

    public String imageHref() {
        return imageHref;
    }
}
