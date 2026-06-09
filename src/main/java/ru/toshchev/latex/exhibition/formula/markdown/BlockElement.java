package ru.toshchev.latex.exhibition.formula.markdown;

import java.util.List;

/**
 * A block-level Markdown element.
 *
 * <p>Block elements are the top-level building blocks of a Markdown document:
 * headings, paragraphs, lists, tables, and code blocks. Each block may contain
 * inline content (text, formatting, formulas) represented as raw strings that
 * are further parsed by {@link ru.toshchev.latex.formula.inline.InlineFormulaParser}.
 */
public sealed interface BlockElement
        permits BlockElement.Heading,
                BlockElement.Paragraph,
                BlockElement.BulletList,
                BlockElement.OrderedList,
                BlockElement.Table,
                BlockElement.CodeBlock,
                BlockElement.HorizontalRule {

    /**
     * A heading: {@code # H1}, {@code ## H2}, etc.
     *
     * @param level 1–6
     * @param inlineContent raw inline markdown string (may contain bold, formulas, etc.)
     */
    record Heading(int level, String inlineContent) implements BlockElement {}

    /**
     * A plain paragraph — one or more inline-content lines merged into one block.
     */
    record Paragraph(String inlineContent) implements BlockElement {}

    /**
     * An unordered bullet list ({@code -} or {@code *} prefix).
     */
    record BulletList(List<ListItem> items) implements BlockElement {}

    /**
     * An ordered (numbered) list ({@code 1.} prefix).
     */
    record OrderedList(List<ListItem> items) implements BlockElement {}

    /**
     * A table with a header row and body rows.
     * Each cell value is a raw inline markdown string.
     */
    record Table(List<String> headers, List<List<String>> rows) implements BlockElement {}

    /**
     * A fenced code block ({@code ```lang ... ```}).
     */
    record CodeBlock(String language, String code) implements BlockElement {}

    /**
     * A horizontal rule ({@code ---}, {@code ***}, {@code ___}).
     */
    record HorizontalRule() implements BlockElement {}

    /**
     * A single item in a bullet or ordered list.
     *
     * @param inlineContent raw inline markdown content of this item
     * @param level         nesting depth (0 = top level)
     */
    record ListItem(String inlineContent, int level) {}
}
