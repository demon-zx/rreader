package ru.terentjev.service.render.fb2;

/**
 * A contiguous piece of paragraph text sharing the same inline styling.
 *
 * @param text the run text
 * @param bold whether the run is wrapped in a {@code strong} element
 * @param italic whether the run is wrapped in an {@code emphasis} element
 */
public record Fb2Run(String text, boolean bold, boolean italic) {
}
