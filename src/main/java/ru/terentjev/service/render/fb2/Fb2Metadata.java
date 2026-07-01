package ru.terentjev.service.render.fb2;

/**
 * Book metadata extracted from the FB2 {@code description/title-info} block.
 *
 * @param title the book title
 * @param author the first author, first and last name joined
 * @param language the book language code
 * @param coverHref the cover binary id (the {@code #} prefix already stripped), or {@code null}
 * @param sequenceName the series name from {@code title-info/sequence@name}, or {@code null}
 * @param sequenceNumber the position within the series from {@code sequence@number}, or {@code null}
 */
public record Fb2Metadata(
        String title,
        String author,
        String language,
        String coverHref,
        String sequenceName,
        Integer sequenceNumber
) {
}
