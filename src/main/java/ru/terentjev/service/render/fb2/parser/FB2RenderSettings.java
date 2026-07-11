package ru.terentjev.service.render.fb2.parser;

public record FB2RenderSettings(
    int pageWidth,
    int pageHeight,
    int paddingLeft,
    int paddingTop,
    int paddingRight,
    int paddingBottom,
    String fontName,
    int fontSize,
    float lineHeight
) {

}
