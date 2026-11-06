package ru.terentjev.repository;

/**
 * Общие опции URL in-memory H2 для репозиторных тестов.
 */
public final class TestH2 {
    public static final String OPTIONS = "IGNORECASE=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1";

    private TestH2() {
    }
}
