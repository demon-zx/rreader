package ru.terentjev.service.render.fb2.layout;

import ru.terentjev.service.render.fb2.parser.FB2RenderSettings;

import java.awt.Font;

public final class Fonts {
    private final Font body;
    private final Font bodyBold;
    private final Font bodyItalic;
    private final Font bodyBoldItalic;
    private final Font title;
    private final Font subTitle;

    public Fonts(FB2RenderSettings settings) {
        body = new Font(settings.fontName(), Font.PLAIN, settings.fontSize());
        bodyBold = body.deriveFont(Font.BOLD);
        bodyItalic = body.deriveFont(Font.ITALIC);
        bodyBoldItalic = body.deriveFont(Font.BOLD | Font.ITALIC);
        title = new Font(settings.fontName(), Font.BOLD, settings.fontSize() * 2);
        subTitle = new Font(settings.fontName(), Font.BOLD, (int) (settings.fontSize() * 1.5));
    }

    public Font body() {
        return body;
    }

    public Font bodyBold() {
        return bodyBold;
    }

    public Font bodyItalic() {
        return bodyItalic;
    }

    public Font bodyBoldItalic() {
        return bodyBoldItalic;
    }

    public Font title() {
        return title;
    }

    public Font subTitle() {
        return subTitle;
    }
}
