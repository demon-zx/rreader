package ru.terentjev.service.model;

/**
 * Книга, которую читают сейчас: с текущей позицией чтения и процентом
 * прочитанного.
 */
public class BookRecent extends Book {
    private final long position;
    private final int pages;
    private final int percent;

    public BookRecent(
            Book book,
            long position,
            int pages,
            int percent
    ) {
        super(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getLanguage(),
                book.getSequenceName(),
                book.getSequenceNumber(),
                book.getType(),
                book.getUri(),
                book.getChecksum(),
                book.getIsbn(),
                book.getSize()
        );
        this.position = position;
        this.pages = pages;
        this.percent = percent;
    }

    public long getPosition() {
        return position;
    }

    public int getPages() {
        return pages;
    }

    public int getPercent() {
        return percent;
    }
}
