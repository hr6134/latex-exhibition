package ru.toshchev.latex.formula.inline;

/**
 * A segment of inline content — either plain text or a LaTeX formula.
 */
public sealed interface ContentToken permits ContentToken.Text, ContentToken.Formula {

    record Text(String value) implements ContentToken {}

    record Formula(String latex) implements ContentToken {}
}
