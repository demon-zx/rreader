package ru.terentjev.service.render.fb2;

import java.util.List;
import java.util.Map;

/**
 * Parsed representation of an FB2 book: metadata, body blocks and the decoded binaries that were
 * actually requested during parsing.
 */
public final class Fb2Document {

    private final Fb2Metadata metadata;
    private final List<Fb2Block> blocks;
    private final Map<String, byte[]> binaries;

    public Fb2Document(Fb2Metadata metadata, List<Fb2Block> blocks, Map<String, byte[]> binaries) {
        this.metadata = metadata;
        this.blocks = blocks;
        this.binaries = binaries;
    }

    public Fb2Metadata metadata() {
        return metadata;
    }

    public List<Fb2Block> blocks() {
        return blocks;
    }

    /**
     * Returns the decoded bytes of a binary by its id, or {@code null} when it was not requested
     * (binaries are only decoded for the page currently being rendered).
     *
     * @param id the binary id referenced by an {@code l:href}
     * @return the decoded bytes, or {@code null}
     */
    public byte[] binary(String id) {
        return binaries.get(id);
    }
}
