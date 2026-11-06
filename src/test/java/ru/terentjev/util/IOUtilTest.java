package ru.terentjev.util;

import org.instancio.Instancio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class IOUtilTest {
    @TempDir
    Path tempDir;

    @Test
    void readFileByPathReturnsMappedContent() throws IOException {
        byte[] content = Instancio.create(byte[].class);
        Path file = tempDir.resolve("data.bin");
        Files.write(file, content);
        // act
        byte[] result = IOUtil.readFile(file, InputStream::readAllBytes);
        // assert
        assertArrayEquals(content, result);
    }

    @Test
    void readFileByPathHandlesEmptyFile() throws IOException {
        Path file = tempDir.resolve("empty.bin");
        Files.write(file, new byte[0]);
        // act
        byte[] result = IOUtil.readFile(file, InputStream::readAllBytes);
        // assert
        assertEquals(0, result.length);
    }

    @Test
    void readFileByPathClosesStreamAfterMapping() throws IOException {
        Path file = tempDir.resolve("closed.bin");
        Files.write(file, Instancio.create(byte[].class));
        InputStream[] captured = new InputStream[1];
        // act
        IOUtil.readFile(file, is -> {
            captured[0] = is;
            return null;
        });
        // assert
        assertThrows(IOException.class, () -> captured[0].readAllBytes(), "stream must be closed after mapper returns");
    }

    @Test
    void readFileByPathPropagatesMapperFailureAndClosesStream() throws IOException {
        Path file = tempDir.resolve("failing.bin");
        Files.write(file, Instancio.create(byte[].class));
        InputStream[] captured = new InputStream[1];
        // act
        IOException error = assertThrows(
                IOException.class,
                () -> IOUtil.readFile(file, is -> {
                    captured[0] = is;
                    throw new IOException("mapper failed");
                })
        );
        // assert
        assertEquals("mapper failed", error.getMessage());
        assertThrows(IOException.class, () -> captured[0].readAllBytes(), "stream must be closed when mapper throws");
    }

    @Test
    void readFileByPathThrowsWhenFileMissing() {
        Path missing = tempDir.resolve("missing.bin");
        // act + assert
        assertThrows(
                NoSuchFileException.class,
                () -> IOUtil.readFile(missing, InputStream::readAllBytes)
        );
    }

    @Test
    void readFileByUriWithFileSchemeReturnsMappedContent() throws IOException {
        String text = Instancio.gen()
                .string()
                .length(1, 100)
                .get();
        Path file = tempDir.resolve("text.txt");
        Files.writeString(file, text, StandardCharsets.UTF_8);
        // act
        String result = IOUtil.readFile(
                file.toUri(),
                is -> new String(is.readAllBytes(), StandardCharsets.UTF_8)
        );
        // assert
        assertEquals(text, result);
    }

    @Test
    void readFileByUriWithFileSchemeThrowsWhenFileMissing() {
        URI missing = tempDir.resolve("missing.txt")
                .toUri();
        // act + assert
        assertThrows(
                NoSuchFileException.class,
                () -> IOUtil.readFile(missing, InputStream::readAllBytes)
        );
    }

    @Test
    void readFileByUriWithZipSchemeReadsArchiveEntry() throws IOException {
        byte[] content = Instancio.create(byte[].class);
        Path archive = tempDir.resolve("book.zip");
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(archive))) {
            zos.putNextEntry(new ZipEntry("entry.txt"));
            zos.write(content);
            zos.closeEntry();
        }
        URI uri = URI.create("zip:" + archive.toUri() + "!/entry.txt");
        // act
        byte[] result = IOUtil.readFile(uri, InputStream::readAllBytes);
        // assert
        assertArrayEquals(content, result);
    }

    @Test
    void readFileByUriThrowsForUnsupportedScheme() {
        URI unsupported = URI.create("ftp://example.com/books/book.epub");
        // act + assert
        assertThrows(
                UnsupportedOperationException.class,
                () -> IOUtil.readFile(unsupported, InputStream::readAllBytes)
        );
    }
}
