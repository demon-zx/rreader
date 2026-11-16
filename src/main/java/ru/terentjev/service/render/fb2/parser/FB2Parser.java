package ru.terentjev.service.render.fb2.parser;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

import ru.terentjev.service.render.fb2.parser.FB2.Block;
import ru.terentjev.service.render.fb2.parser.FB2.EmptyLineBlock;
import ru.terentjev.service.render.fb2.parser.FB2.ImageBlock;
import ru.terentjev.service.render.fb2.parser.FB2.ParagraphBlock;
import ru.terentjev.service.render.fb2.parser.FB2.Piece;
import ru.terentjev.service.render.fb2.parser.FB2.Sequence;
import ru.terentjev.service.render.fb2.parser.FB2.SubTitleBlock;
import ru.terentjev.service.render.fb2.parser.FB2.TitleBlock;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.Location;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class FB2Parser {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(FB2Parser.class);
    private static final String SECTION = "section";
    private static final String BINARY = "binary";
    private static final String TEXT_AUTHOR = "text-author";
    private static final String STROPHA = "v";
    private static final String PARAGRAPH = "p";
    private static final String SUB = "sub";
    private static final String SUP = "sup";
    private static final String SUBTITLE = "subtitle";
    private static final String TITLE_INFO = "title-info";
    private static final String LAST_NAME = "last-name";
    private static final String FIRST_NAME = "first-name";
    private static final String GENRE = "genre";
    private static final String AUTHOR = "author";
    private static final String EMPHASIS = "emphasis";
    private static final String STRONG = "strong";
    private final Path binaryDirectory;
    private final XMLInputFactory factory;
    private final Stack<String> path = new Stack<>();
    private final Set<String> tags = new HashSet<>();
    private Consumer<String> imageHandler = href -> cover = href;
    private Consumer<Block> blockHandler = href -> {
    };
    private StringBuilder textAccumulator = new StringBuilder();
    private int position = 0;
    private String nameFirst;
    private String nameLast;
    private int strong = 0;
    private int italic = 0;
    private List<Piece> pieces = new ArrayList<>();
    private String sectionId;
    private String binaryId;
    private String binaryType;
    private String linkHref;
    private final Deque<SectionBuilder> sections = new ArrayDeque<>();
    private final List<SectionBuilder> sectionTree = new ArrayList<>();
    private StringBuilder sectionTitle;
    private int sectionTitlePosition;
    private boolean contentBody;
    private boolean contentBodyStarted;
    // > book info
    private LocalDate date;
    private String cover;
    private String title;
    private String language;
    private List<String> authors = new ArrayList<>();
    private List<String> genres = new ArrayList<>();
    private List<FB2.Sequence> sequences = new ArrayList<>();
    private List<Block> body = new ArrayList<>();
    private Map<String, List<Block>> notes = new HashMap<>();
    private Map<String, String> binaries = new HashMap<>();

    public FB2Parser(Path bookDirectory) {
        this.binaryDirectory = bookDirectory.resolve("binaries");
        if (!Files.exists(binaryDirectory)) {
            try {
                Files.createDirectories(binaryDirectory);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
        factory = XMLInputFactory.newInstance();
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    }

    public FB2 parse(InputStream is) throws IOException {
        try {
            XMLStreamReader reader = null;
            try {
                reader = factory.createXMLStreamReader(is);
                while (reader.hasNext()) {
                    int event = reader.next();
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT -> start(reader);
                        case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> text(reader);
                        case XMLStreamConstants.END_ELEMENT -> end(reader);
                        default -> {
                        }
                    }
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        } catch (XMLStreamException e) {
            throw new IOException("Failed to parse FB2 stream", e);
        }
        return new FB2(title, genres, authors, date, cover, language, sequences, content(), body, notes, binaries);
    }

    private boolean in(String tag) {
        return tags.contains(tag);
    }

    private boolean inAny(String... any) {
        for (String t : any) {
            if (this.tags.contains(t)) {
                return true;
            }
        }
        return false;
    }

    private String text() {
        String text = textAccumulator.toString();
        textAccumulator.setLength(0);
        return StringUtils.trimToNull(text);
    }

    private void text(XMLStreamReader reader) {
        String value = reader.getText();
        textAccumulator.append(value);
        if (sectionTitle != null) {
            sectionTitle.append(value);
        }
    }

    private void start(XMLStreamReader reader) {
        String name = reader.getLocalName();
        path.push(name);
        tags.add(name);
        Location location = reader.getLocation();
        position = location.getCharacterOffset();
        switch (name) {
            case "sequence" -> sequence(reader);
            case "body" -> body(reader);
            case SECTION -> sectionStart(reader);
            case "image" -> image(reader);
            case "title" -> sectionTitleStart();
            case "empty-line" -> {
                if (sectionTitle != null) {
                    sectionTitle.append(' ');
                }
                blockHandler.accept(new EmptyLineBlock(position));
            }
            case "a" -> linkStart(reader);
            case STRONG -> {
                style();
                strong++;
            }
            case EMPHASIS -> {
                style();
                italic++;
            }
            case BINARY -> binaryStart(reader);
            default -> {
            }
        }
    }

    private void end(XMLStreamReader reader) throws IOException {
        String name = reader.getLocalName();
        switch (name) {
            case SECTION -> sectionEnd();
            case "body" -> contentBody = false;
            case "title" -> sectionTitleEnd();
            case GENRE -> genre(reader);
            case AUTHOR -> author();
            case FIRST_NAME -> nameFirst();
            case LAST_NAME -> nameLast();
            case "book-title" -> title();
            case "lang" -> language();
            case "date" -> date();
            case "a" -> linkStop();
            case STRONG -> {
                style();
                strong--;
            }
            case EMPHASIS -> {
                style();
                italic--;
            }
            case SUBTITLE, PARAGRAPH, STROPHA, TEXT_AUTHOR, SUB, SUP -> paragraph();
            case BINARY -> binaryEnd();
            default -> {
            }
        }
        if (!in("body")) {
            // clear content unprocessed tags
            text();
        }
        path.pop();
        tags.clear();
        tags.addAll(path);
    }

    private void body(XMLStreamReader reader) {
        String name = StringUtils.trimToNull(reader.getAttributeValue(null, "name"));
        contentBody = name == null && !contentBodyStarted;
        if (contentBody) {
            contentBodyStarted = true;
        }
        if (body.isEmpty() && name == null) {
            blockHandler = block -> body.add(block);
            imageHandler = href -> {
                blockHandler.accept(new ImageBlock(position, href));
            };
        }
        if ("notes".equals(name)) {
            blockHandler = block -> {
                body.add(block);
                if (sectionId != null) {
                    var note = notes.computeIfAbsent(sectionId, i -> new ArrayList<>());
                    note.add(block);
                }
            };
            imageHandler = href -> {
            };
        }
    }

    private void sectionStart(XMLStreamReader reader) {
        sectionId = StringUtils.trimToNull(reader.getAttributeValue(null, "id"));
        if (contentBody) {
            SectionBuilder section = new SectionBuilder(position);
            if (sections.isEmpty()) {
                sectionTree.add(section);
            } else {
                sections.peek().children.add(section);
            }
            sections.push(section);
        }
    }

    private void sectionEnd() {
        if (!sections.isEmpty()) {
            sections.pop();
        }
        sectionId = null;
    }

    private void sectionTitleStart() {
        if (contentBody && !sections.isEmpty()) {
            sectionTitle = new StringBuilder();
            sectionTitlePosition = position;
        }
    }

    /**
     * Сохраняет заголовок раздела без разметки. Вложенные теги и лишние пробелы удаляются.
     */
    private void sectionTitleEnd() {
        if (sectionTitle == null) {
            return;
        }
        String value = StringUtils.normalizeSpace(sectionTitle.toString());
        sectionTitle = null;
        if (StringUtils.isEmpty(value) || sections.isEmpty()) {
            return;
        }
        SectionBuilder section = sections.peek();
        // заголовок раздела встречается в книге один раз, повторный не перетирает первый
        if (section.title == null) {
            section.title = value;
            section.position = sectionTitlePosition;
        }
    }

    private List<FB2.Section> content() {
        return content(sectionTree);
    }

    /**
     * Преобразует дерево разбора в содержание. Разделы без заголовка не показываются,
     * их вложенные разделы поднимаются на уровень выше.
     */
    private static List<FB2.Section> content(List<SectionBuilder> sections) {
        List<FB2.Section> result = new ArrayList<>();
        for (SectionBuilder section : sections) {
            List<FB2.Section> children = content(section.children);
            if (section.title == null) {
                result.addAll(children);
            } else {
                result.add(new FB2.Section(section.title, section.position, children));
            }
        }
        return result;
    }

    private void binaryStart(XMLStreamReader reader) {
        binaryId = StringUtils.trimToNull(reader.getAttributeValue(null, "id"));
        binaryType = StringUtils.trimToNull(reader.getAttributeValue(null, "content-type"));
    }

    private void binaryEnd() throws IOException {
        if (in("binary")) {
            var decoder = Base64.getMimeDecoder();
            byte[] value = decoder.decode(text());
            String ext = StringUtils.substringAfter(binaryType, "/");
            Path file;
            if (binaryId.endsWith(ext)) {
                file = binaryDirectory.resolve(binaryId);
            } else {
                file = binaryDirectory.resolve(binaryId + "." + ext);
            }
            if (Files.notExists(file)) {
                try (var os = new BufferedOutputStream(Files.newOutputStream(file, StandardOpenOption.CREATE_NEW))) {
                    os.write(value);
                    os.flush();
                }
            }
            binaries.put(binaryId, file.toString());
        }
    }

    private String href(XMLStreamReader reader) {
        return StringUtils.trimToNull(reader.getAttributeValue("http://www.w3.org/1999/xlink", "href"));
    }

    private void image(XMLStreamReader reader) {
        String href = href(reader);
        if (href != null) {
            imageHandler.accept(StringUtils.stripStart(href, "#"));
        }
    }

    private void linkStart(XMLStreamReader reader) {
        String href = href(reader);
        if (href != null && "note".equals(reader.getAttributeValue(null, "type"))) {
            piece();
            linkHref = href;
        }
    }

    private void linkStop() {
        if (in("a")) {
            piece();
            linkHref = null;
        }
    }

    private void date() {
        if (in(TITLE_INFO)) {
            String value = text();
            if (value != null) {
                try {
                    date = LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE);
                } catch (DateTimeParseException e) {
                    log.warn("Incorrect date format: {}", value);
                }
            }
        }
    }

    private void paragraph() {
        piece();
        if (sectionTitle != null) {
            sectionTitle.append(' ');
        }
        if (!pieces.isEmpty()) {
            blockHandler.accept(paragraphBlock(List.copyOf(pieces)));
            pieces.clear();
        }
    }

    private Block paragraphBlock(List<Piece> copyPieces) {
        if (in("title")) {
            long sections = path.stream()
                    .filter(t -> t.equals(SECTION))
                    .count();
            if (sections <= 1) {
                return new TitleBlock(position, copyPieces);
            } else {
                return new SubTitleBlock(position, copyPieces);
            }
        }
        if (SUBTITLE.equals(path.peek())) {
            return new SubTitleBlock(position, copyPieces);
        }
        return new ParagraphBlock(position, copyPieces);
    }

    private void style() {
        if (inAny(SUBTITLE, PARAGRAPH, STROPHA, TEXT_AUTHOR)) {
            piece();
        }
    }

    private void piece() {
        String text = text();
        if (text != null) {
            Piece piece = new Piece(text, strong > 0, italic > 0, linkHref);
            pieces.add(piece);
        }
    }

    private void language() {
        if (in(TITLE_INFO)) {
            language = text();
        }
    }

    private void title() {
        if (in(TITLE_INFO)) {
            title = text();
        }
    }

    private void author() {
        if (in(TITLE_INFO)) {
            String name = Stream.of(nameFirst, nameLast)
                    .map(StringUtils::trimToNull)
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));
            if (!name.isBlank()) {
                nameFirst = null;
                nameLast = null;
                authors.add(name);
            }
        }
    }

    private void nameFirst() {
        if (in(AUTHOR)) {
            nameFirst = text();
        }
    }

    private void nameLast() {
        if (in(AUTHOR)) {
            nameLast = text();
        }
    }

    private void genre(XMLStreamReader reader) {
        genres.add(text());
    }

    private void sequence(XMLStreamReader reader) {
        String name = StringUtils.trimToNull(reader.getAttributeValue(null, "name"));
        if (name != null) {
            String number = StringUtils.trimToNull(reader.getAttributeValue(null, "number"));
            if (number != null) {
                try {
                    sequences.add(new Sequence(name, Integer.valueOf(number.trim())));
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse sequence number '{}'", number);
                }
            }
        }
    }

    private static class SectionBuilder {
        private int position;
        private String title;
        private final List<SectionBuilder> children = new ArrayList<>();

        private SectionBuilder(int position) {
            this.position = position;
        }
    }
}
