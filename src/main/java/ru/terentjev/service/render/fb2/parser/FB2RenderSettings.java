package ru.terentjev.service.render.fb2.parser;

import java.awt.Color;

public record FB2RenderSettings(
    int pageWidth,
    int pageHeight,
    int paddingLeft,
    int paddingTop,
    int paddingRight,
    int paddingBottom,
    int indent,
    String fontName,
    int fontSize,
    float lineHeight,
    Color color,
    Color background
) {

}
