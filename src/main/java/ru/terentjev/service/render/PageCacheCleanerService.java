package ru.terentjev.service.render;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ru.terentjev.config.CacheCleanupParameters;
import ru.terentjev.config.DirectoryParameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;

@Component
public class PageCacheCleanerService {

    private static final Logger log = LoggerFactory.getLogger(PageCacheCleanerService.class);

    private final CacheCleanupParameters parameters;
    private final DirectoryParameters directories;
    private final Clock clock;

    public PageCacheCleanerService(CacheCleanupParameters parameters, DirectoryParameters directories, Clock clock) {
        this.parameters = parameters;
        this.directories = directories;
        this.clock = clock;
    }

    @Scheduled(cron = "${application.cache.cleanup.cron:0 30 3 * * *}")
    public void onSchedule() {
        if (!parameters.enabled()) {
            return;
        }
        try {
            cleanup();
        } catch (IOException e) {
            log.error("Page cache cleanup failed", e);
        }
    }

    void cleanup() throws IOException {
        cleanup(directories.pages());
    }

    void cleanup(Path root) throws IOException {
        if (Files.notExists(root)) {
            log.info("Page cache cleanup skipped: {} does not exist", root);
            return;
        }
        Instant threshold = clock.instant()
                .minus(parameters.maxAge());
        int files = deleteExpiredFiles(root, threshold);
        int directories = pruneEmptyDirectories(root);
        log.info(
                "Page cache cleanup complete: removed {} files older than {} and {} empty directories",
                files, threshold, directories
        );
    }

    private int deleteExpiredFiles(Path root, Instant threshold) throws IOException {
        int[] count = new int[1];
        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .forEach(path -> {
                        try {
                            Instant modified = Files.getLastModifiedTime(path)
                                    .toInstant();
                            if (modified.isBefore(threshold)) {
                                Files.delete(path);
                                count[0]++;
                                log.debug("Deleted cached file {}", path);
                            }
                        } catch (IOException e) {
                            log.warn("Failed to delete cached file {}", path, e);
                        }
                    });
        }
        return count[0];
    }

    private int pruneEmptyDirectories(Path root) throws IOException {
        int[] count = new int[1];
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder())
                    .filter(path -> !path.equals(root))
                    .filter(Files::isDirectory)
                    .forEach(path -> {
                        try {
                            try (Stream<Path> entries = Files.list(path)) {
                                Optional<Path> anyFiles = entries.findAny();
                                if (anyFiles.isEmpty()) {
                                    Files.delete(path);
                                    count[0]++;
                                    log.debug("Deleted empty directory {}", path);
                                }
                            }
                        } catch (IOException e) {
                            log.warn("Failed to delete directory {}", path, e);
                        }
                    });
        }
        return count[0];
    }

}
