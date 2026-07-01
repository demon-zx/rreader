package ru.terentjev.service.render.fb2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Forward-only, streaming FB2 reader built on StAX ({@link XMLStreamReader}).
 *
 * <p>The whole file is never materialised into a DOM. Metadata is captured from the
 * {@code description} block, the body is emitted as a flat list of lightweight {@link Fb2Block}s,
 * and the base64 {@code binary} payloads (cover, illustrations) are decoded into a map. Everything
 * is produced in a single pass over the stream.
 */
@Component
public class Fb2Parser {

    private static final Logger log = LoggerFactory.getLogger(Fb2Parser.class);

    private final XMLInputFactory factory;

    public Fb2Parser() {
        this.factory = XMLInputFactory.newInstance();
        this.factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
        this.factory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
        this.factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    }

    /**
     * Parses the stream. The character encoding is taken from the XML declaration, so the raw
     * byte stream must be passed in (e.g. windows-1251 FB2 files are handled transparently).
     *
     * @param is the FB2 byte stream
     * @return the parsed document
     * @throws IOException if the stream cannot be read or is not well-formed XML
     */
    public Fb2Document parse(InputStream is) throws IOException {
        try {
            XMLStreamReader reader = factory.createXMLStreamReader(is);
            return new Worker(reader).read();
        } catch (XMLStreamException e) {
            throw new IOException("Failed to parse FB2 stream", e);
        }
    }

    /**
     * Holds the mutable state of a single parse pass so {@link Fb2Parser} itself stays stateless
     * and thread-safe as a Spring singleton.
     */
    private static final class Worker {

        private final XMLStreamReader reader;

        private final List<Fb2Block> blocks = new ArrayList<>();
        private final Map<String, byte[]> binaries = new HashMap<>();
        private final StringBuilder leaf = new StringBuilder();

        private boolean inDescription;
        private boolean inTitleInfo;
        private boolean inAuthor;
        private boolean inCoverpage;
        private String title;
        private String author;
        private String language;
        private String coverHref;
        private String sequenceName;
        private Integer sequenceNumber;
        private String authorFirst;
        private String authorLast;

        private boolean inBody;
        private boolean bodyDone;
        private int sectionDepth;
        private int titleDepth;
        private boolean inParagraph;
        private String paragraphTag;
        private int emphasisDepth;
        private int strongDepth;
        private List<Fb2Run> runs;
        private final StringBuilder runText = new StringBuilder();

        private Fb2Metadata metadata;
        private boolean collecting;
        private String binaryId;
        private final StringBuilder binary = new StringBuilder();

        Worker(XMLStreamReader reader) {
            this.reader = reader;
        }

        Fb2Document read() throws XMLStreamException {
            while (reader.hasNext()) {
                int event = reader.next();
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT -> startElement();
                    case XMLStreamConstants.CHARACTERS, XMLStreamConstants.CDATA -> characters();
                    case XMLStreamConstants.END_ELEMENT -> endElement();
                    default -> { }
                }
            }
            reader.close();
            return document();
        }

        private void startElement() {
            String name = reader.getLocalName();
            leaf.setLength(0);
            switch (name) {
                case "description" -> inDescription = true;
                case "title-info" -> {
                    if (inDescription) {
                        inTitleInfo = true;
                    }
                }
                case "author" -> {
                    if (inTitleInfo) {
                        inAuthor = true;
                    }
                }
                case "coverpage" -> {
                    if (inTitleInfo) {
                        inCoverpage = true;
                    }
                }
                case "sequence" -> {
                    if (inTitleInfo) {
                        startSequence();
                    }
                }
                case "image" -> startImage();
                case "body" -> {
                    if (!bodyDone && !inBody) {
                        inBody = true;
                    }
                }
                case "section" -> {
                    if (inBody) {
                        sectionDepth++;
                    }
                }
                case "title" -> {
                    if (inBody) {
                        titleDepth++;
                    }
                }
                case "subtitle", "p", "v", "text-author" -> {
                    if (inBody) {
                        startParagraph(name);
                    }
                }
                case "empty-line" -> {
                    if (inBody) {
                        blocks.add(Fb2Block.emptyLine());
                    }
                }
                case "emphasis" -> {
                    if (inParagraph) {
                        flushRun();
                        emphasisDepth++;
                    }
                }
                case "strong" -> {
                    if (inParagraph) {
                        flushRun();
                        strongDepth++;
                    }
                }
                case "binary" -> startBinary();
                default -> { }
            }
        }

        private void endElement() {
            String name = reader.getLocalName();
            switch (name) {
                case "description" -> {
                    inDescription = false;
                    inTitleInfo = false;
                    metadata = buildMetadata();
                }
                case "title-info" -> inTitleInfo = false;
                case "author" -> {
                    if (inTitleInfo) {
                        inAuthor = false;
                        composeAuthor();
                    }
                }
                case "coverpage" -> inCoverpage = false;
                case "first-name" -> {
                    if (inAuthor) {
                        authorFirst = leafText();
                    }
                }
                case "last-name" -> {
                    if (inAuthor) {
                        authorLast = leafText();
                    }
                }
                case "book-title" -> {
                    if (inTitleInfo && title == null) {
                        title = leafText();
                    }
                }
                case "lang" -> {
                    if (inTitleInfo && language == null) {
                        language = leafText();
                    }
                }
                case "emphasis" -> {
                    if (inParagraph && emphasisDepth > 0) {
                        flushRun();
                        emphasisDepth--;
                    }
                }
                case "strong" -> {
                    if (inParagraph && strongDepth > 0) {
                        flushRun();
                        strongDepth--;
                    }
                }
                case "subtitle", "p", "v", "text-author" -> {
                    if (inParagraph && name.equals(paragraphTag)) {
                        endParagraph();
                    }
                }
                case "title" -> {
                    if (inBody && titleDepth > 0) {
                        titleDepth--;
                    }
                }
                case "section" -> {
                    if (inBody && sectionDepth > 0) {
                        sectionDepth--;
                    }
                }
                case "body" -> {
                    if (inBody && !bodyDone) {
                        inBody = false;
                        bodyDone = true;
                    }
                }
                case "binary" -> endBinary();
                default -> { }
            }
        }

        private void characters() {
            String text = reader.getText();
            if (collecting) {
                binary.append(text);
            } else if (inParagraph) {
                runText.append(text);
            } else if (inDescription) {
                leaf.append(text);
            }
        }

        private void startSequence() {
            if (sequenceName != null) {
                return;
            }
            String name = reader.getAttributeValue(null, "name");
            if (name == null || name.isBlank()) {
                return;
            }
            sequenceName = name.trim();
            String number = reader.getAttributeValue(null, "number");
            if (number != null && !number.isBlank()) {
                try {
                    sequenceNumber = Integer.valueOf(number.trim());
                } catch (NumberFormatException e) {
                    log.warn("Cannot parse sequence number '{}'", number);
                }
            }
        }

        private void startImage() {
            String href = stripHash(hrefAttribute());
            if (href == null) {
                return;
            }
            if (inCoverpage && coverHref == null) {
                coverHref = href;
            } else if (inBody && !bodyDone) {
                blocks.add(Fb2Block.image(href));
            }
        }

        private void startBinary() {
            String id = reader.getAttributeValue(null, "id");
            if (id != null) {
                collecting = true;
                binaryId = id;
                binary.setLength(0);
            }
        }

        private void endBinary() {
            if (!collecting) {
                return;
            }
            collecting = false;
            try {
                binaries.put(binaryId, Base64.getMimeDecoder().decode(binary.toString()));
            } catch (IllegalArgumentException e) {
                log.warn("Cannot decode binary {}", binaryId, e);
            }
            binary.setLength(0);
        }

        private void startParagraph(String tag) {
            inParagraph = true;
            paragraphTag = tag;
            runs = new ArrayList<>();
            runText.setLength(0);
            emphasisDepth = 0;
            strongDepth = 0;
        }

        private void endParagraph() {
            flushRun();
            if (!runs.isEmpty()) {
                blocks.add(Fb2Block.text(resolveKind(), runs));
            }
            inParagraph = false;
            paragraphTag = null;
            runs = null;
        }

        private Fb2Block.Kind resolveKind() {
            if (titleDepth > 0) {
                return sectionDepth <= 1 ? Fb2Block.Kind.TITLE : Fb2Block.Kind.SUBTITLE;
            }
            if ("subtitle".equals(paragraphTag)) {
                return Fb2Block.Kind.SUBTITLE;
            }
            return Fb2Block.Kind.PARAGRAPH;
        }

        private void flushRun() {
            if (runText.length() == 0) {
                return;
            }
            runs.add(new Fb2Run(runText.toString(), strongDepth > 0, emphasisDepth > 0));
            runText.setLength(0);
        }

        private void composeAuthor() {
            if (author != null) {
                return;
            }
            StringBuilder sb = new StringBuilder();
            if (authorFirst != null && !authorFirst.isBlank()) {
                sb.append(authorFirst.trim());
            }
            if (authorLast != null && !authorLast.isBlank()) {
                if (sb.length() > 0) {
                    sb.append(' ');
                }
                sb.append(authorLast.trim());
            }
            if (sb.length() > 0) {
                author = sb.toString();
            }
        }

        private Fb2Metadata buildMetadata() {
            return new Fb2Metadata(
                    blankToNull(title),
                    blankToNull(author),
                    blankToNull(language),
                    coverHref,
                    blankToNull(sequenceName),
                    sequenceNumber
            );
        }

        private Fb2Document document() {
            if (metadata == null) {
                metadata = buildMetadata();
            }
            return new Fb2Document(metadata, List.copyOf(blocks), binaries);
        }

        private String leafText() {
            String text = leaf.toString().trim();
            return text.isEmpty() ? null : text;
        }

        private String hrefAttribute() {
            for (int i = 0; i < reader.getAttributeCount(); i++) {
                if ("href".equals(reader.getAttributeLocalName(i))) {
                    return reader.getAttributeValue(i);
                }
            }
            return null;
        }

        private String stripHash(String value) {
            if (value == null) {
                return null;
            }
            return value.startsWith("#") ? value.substring(1) : value;
        }

        private String blankToNull(String value) {
            return value == null || value.isBlank() ? null : value;
        }
    }
}
