package ru.terentjev.service.render;

import java.util.List;

/**
 * Элемент содержания книги. Содержание иерархическое: у элемента есть вложенные элементы.
 *
 * @param title    заголовок элемента без разметки
 * @param position позиция элемента в книге: смещение в тексте (fb2) или номер страницы (pdf)
 * @param children вложенные элементы содержания
 */
public record Content(String title, int position, List<Content> children) {
    public Content {
        children = children == null ? List.of() : List.copyOf(children);
    }
}
