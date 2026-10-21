package ru.terentjev.service.render.fb2.parser;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

// http://www.fictionbook.org/index.php/%D0%9E%D0%BF%D0%B8%D1%81%D0%B0%D0%BD%D0%B8%D0%B5_%D1%84%D0%BE%D1%80%D0%BC%D0%B0%D1%82%D0%B0_FB2_%D0%BE%D1%82_Sclex
public record FB2(
        String title,
        List<String> genres,
        List<String> authors,
        @Nullable LocalDate date,
        @Nullable String cover,
        @Nullable String lang,
        List<Sequence> sequences,
        List<Block> body,
        Map<String, List<Block>> notes,
        Map<String, String> binaries
) {
    public record Sequence(String name, @Nullable Integer number) {
    }

    public static sealed class Block permits AbstractPiecesBlock, EmptyLineBlock, ImageBlock {
        private final int position;

        public Block(int position) {
            this.position = position;
        }

        public int position() {
            return position;
        }
    }

    public static final class EmptyLineBlock extends Block {
        public EmptyLineBlock(int position) {
            super(position);
        }
    }

    public static final class ImageBlock extends Block {
        private final String href;

        public ImageBlock(int position, String href) {
            super(position);
            this.href = href;
        }

        public String href() {
            return href;
        }
    }

    public abstract static sealed class AbstractPiecesBlock extends Block permits
            TitleBlock,
            SubTitleBlock,
            ParagraphBlock {
        private final List<Piece> pieces;

        public AbstractPiecesBlock(int position, List<Piece> pieces) {
            super(position);
            this.pieces = pieces;
        }

        public List<Piece> pieces() {
            return pieces;
        }
    }

    public static final class TitleBlock extends AbstractPiecesBlock {
        public TitleBlock(int position, List<Piece> pieces) {
            super(position, pieces);
        }
    }

    public static final class SubTitleBlock extends AbstractPiecesBlock {
        public SubTitleBlock(int position, List<Piece> pieces) {
            super(position, pieces);
        }
    }

    public static final class ParagraphBlock extends AbstractPiecesBlock {
        public ParagraphBlock(int position, List<Piece> pieces) {
            super(position, pieces);
        }
    }

    public record Piece(String text, boolean bold, boolean italic, String linkId) {
    }
}
