package ru.terentjev.service.render;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import ru.terentjev.config.CacheCleanupParameters;
import ru.terentjev.config.DirectoryParameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageCacheCleanerServiceTest {
    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");
    private static final Instant OLD = NOW.minus(Duration.ofDays(40));
    private static final Instant FRESH = NOW.minus(Duration.ofDays(1));
    @TempDir
    Path tempDir;
    private PageCacheCleanerService cleaner;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW, ZoneId.of("UTC"));
        cleaner = new PageCacheCleanerService(
                new CacheCleanupParameters(true, "0 30 3 * * *", Duration.ofDays(30)),
                new DirectoryParameters(
                        tempDir.resolve("config"),
                        tempDir.resolve("books"),
                        tempDir.resolve("pages")
                ),
                clock
        );
    }

    @Test
    void cleanupDeletesOldFilesKeepsFreshOnes() throws IOException {
        Path deviceDirectory = Files.createDirectories(
                tempDir.resolve("book1")
                        .resolve("device1")
                        .resolve("100x200")
        );
        Path oldPage = write(deviceDirectory.resolve("1.png"), OLD);
        Path freshPage = write(deviceDirectory.resolve("2.png"), FRESH);
        cleaner.cleanup(tempDir);
        assertFalse(Files.exists(oldPage));
        assertTrue(Files.exists(freshPage));
        assertTrue(Files.exists(deviceDirectory));
    }

    @Test
    void cleanupPrunesEmptyDirectoriesButKeepsRoot() throws IOException {
        Path oldBook = Files.createDirectories(
                tempDir.resolve("book2")
                        .resolve("device2")
        );
        write(oldBook.resolve("cover.png"), OLD);
        Path emptyBook = Files.createDirectories(tempDir.resolve("book3"));
        cleaner.cleanup(tempDir);
        assertFalse(Files.exists(oldBook));
        assertFalse(Files.exists(emptyBook));
        assertTrue(Files.exists(tempDir));
    }

    @Test
    void cleanupOnMissingRootDoesNotThrow() throws IOException {
        Path missing = tempDir.resolve("missing");
        cleaner.cleanup(missing);
        assertFalse(Files.exists(missing));
    }

    @Test
    void cleanupPrunesNestedEmptyDirectoriesBottomUp() throws IOException {
        Path nested = Files.createDirectories(
                tempDir.resolve("book4")
                        .resolve("device3")
                        .resolve("100x200")
        );
        write(nested.resolve("1.png"), OLD);
        cleaner.cleanup(tempDir);
        assertFalse(Files.exists(tempDir.resolve("book4")));
        assertTrue(Files.exists(tempDir));
    }

    private static Path write(Path file, Instant modifiedTime) throws IOException {
        Path written = Files.writeString(file, "data");
        Files.setLastModifiedTime(written, FileTime.from(modifiedTime));
        return written;
    }
}
