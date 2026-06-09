package ru.toshchev.latex.exhibition.formula.inline;

/**
 * A segment of inline content: plain text, styled text, or a LaTeX formula.
 *
 * <p>Markdown spans recognised:
 * <ul>
 *   <li>{@code ***text***} / {@code ___text___} → {@link BoldItalic}</li>
 *   <li>{@code **text**} / {@code __text__} → {@link Bold}</li>
 *   <li>{@code *text*} / {@code _text_} → {@link Italic}</li>
 *   <li>{@code ~~text~~} → {@link Strikethrough}</li>
 *   <li>{@code `text`} → {@link Code}</li>
 *   <li>{@code $text$} / {@code \[text\]} / {@code \(text\)} → {@link Formula}</li>
 * </ul>
 */
public sealed interface ContentToken
        permits ContentToken.Text,
                ContentToken.Bold,
                ContentToken.Italic,
                ContentToken.BoldItalic,
                ContentToken.Code,
                ContentToken.Strikethrough,
                ContentToken.Formula {

    record Text(String value)         implements ContentToken {}
    record Bold(String value)         implements ContentToken {}
    record Italic(String value)       implements ContentToken {}
    record BoldItalic(String value)   implements ContentToken {}
    record Code(String value)         implements ContentToken {}
    record Strikethrough(String value) implements ContentToken {}
    record Formula(String latex)      implements ContentToken {}
}
