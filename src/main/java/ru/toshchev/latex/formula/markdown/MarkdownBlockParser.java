package ru.toshchev.latex.formula.markdown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a Markdown string into a list of {@link BlockElement}s.
 *
 * <p>Supported block syntax:
 * <ul>
 *   <li>Headings: {@code # H1} … {@code ###### H6}</li>
 *   <li>Unordered lists: {@code - item}, {@code * item} (nested via leading spaces)</li>
 *   <li>Ordered lists: {@code 1. item} (nested via leading spaces)</li>
 *   <li>Tables: GFM pipe syntax {@code | col | col |} with separator row</li>
 *   <li>Fenced code blocks: {@code ```lang … ```}</li>
 *   <li>Horizontal rules: {@code ---}, {@code ***}, {@code ___} (3+ chars, alone on line)</li>
 *   <li>Paragraphs: everything else (consecutive non-blank lines merged)</li>
 * </ul>
 *
 * <p>Inline content within each block is left as a raw string for
 * {@link ru.toshchev.latex.formula.inline.InlineFormulaParser} to handle.
 */
public class MarkdownBlockParser {

    private static final Pattern HEADING      = Pattern.compile("^(#{1,6})\\s+(.+)$");
    private static final Pattern BULLET_ITEM  = Pattern.compile("^(\\s*)[-*]\\s+(.+)$");
    private static final Pattern ORDERED_ITEM = Pattern.compile("^(\\s*)\\d+\\.\\s+(.+)$");
    private static final Pattern HR           = Pattern.compile("^([-*_])\\1{2,}\\s*$");
    private static final Pattern FENCE_OPEN   = Pattern.compile("^```(\\w*)\\s*$");
    private static final Pattern TABLE_ROW    = Pattern.compile("^\\|(.+)\\|\\s*$");
    private static final Pattern TABLE_SEP    = Pattern.compile("^\\|[-|:\\s]+\\|\\s*$");

    /**
     * Parses the given Markdown text into an ordered list of {@link BlockElement}s.
     */
    public List<BlockElement> parse(String markdown) {
        List<BlockElement> blocks = new ArrayList<>();
        String[] lines = markdown.split("\n", -1);
        int i = 0;

        while (i < lines.length) {
            String line = lines[i];

            // Fenced code block
            Matcher fenceMatcher = FENCE_OPEN.matcher(line);
            if (fenceMatcher.matches()) {
                String lang = fenceMatcher.group(1);
                StringBuilder code = new StringBuilder();
                i++;
                while (i < lines.length && !lines[i].startsWith("```")) {
                    code.append(lines[i]).append("\n");
                    i++;
                }
                i++; // skip closing ```
                blocks.add(new BlockElement.CodeBlock(lang, code.toString().stripTrailing()));
                continue;
            }

            // Heading
            Matcher headingMatcher = HEADING.matcher(line);
            if (headingMatcher.matches()) {
                int level = headingMatcher.group(1).length();
                blocks.add(new BlockElement.Heading(level, headingMatcher.group(2).strip()));
                i++;
                continue;
            }

            // Horizontal rule
            if (HR.matcher(line).matches()) {
                blocks.add(new BlockElement.HorizontalRule());
                i++;
                continue;
            }

            // Table (look ahead for separator row)
            if (TABLE_ROW.matcher(line).matches() && i + 1 < lines.length
                    && TABLE_SEP.matcher(lines[i + 1]).matches()) {
                List<String> headers = parseCells(line);
                i += 2; // skip header + separator
                List<List<String>> rows = new ArrayList<>();
                while (i < lines.length && TABLE_ROW.matcher(lines[i]).matches()) {
                    rows.add(parseCells(lines[i]));
                    i++;
                }
                blocks.add(new BlockElement.Table(headers, rows));
                continue;
            }

            // Bullet list
            if (BULLET_ITEM.matcher(line).matches()) {
                List<BlockElement.ListItem> items = new ArrayList<>();
                while (i < lines.length) {
                    Matcher m = BULLET_ITEM.matcher(lines[i]);
                    if (!m.matches()) break;
                    int level = m.group(1).length() / 2;
                    items.add(new BlockElement.ListItem(m.group(2).strip(), level));
                    i++;
                }
                blocks.add(new BlockElement.BulletList(items));
                continue;
            }

            // Ordered list
            if (ORDERED_ITEM.matcher(line).matches()) {
                List<BlockElement.ListItem> items = new ArrayList<>();
                while (i < lines.length) {
                    Matcher m = ORDERED_ITEM.matcher(lines[i]);
                    if (!m.matches()) break;
                    int level = m.group(1).length() / 2;
                    items.add(new BlockElement.ListItem(m.group(2).strip(), level));
                    i++;
                }
                blocks.add(new BlockElement.OrderedList(items));
                continue;
            }

            // Blank line — separator between blocks, skip
            if (line.isBlank()) {
                i++;
                continue;
            }

            // Paragraph: collect consecutive non-blank, non-special lines
            StringBuilder para = new StringBuilder();
            while (i < lines.length && !lines[i].isBlank()
                    && !HEADING.matcher(lines[i]).matches()
                    && !HR.matcher(lines[i]).matches()
                    && !FENCE_OPEN.matcher(lines[i]).matches()
                    && !BULLET_ITEM.matcher(lines[i]).matches()
                    && !ORDERED_ITEM.matcher(lines[i]).matches()) {
                if (para.length() > 0) para.append(" ");
                para.append(lines[i].strip());
                i++;
            }
            if (para.length() > 0) {
                blocks.add(new BlockElement.Paragraph(para.toString()));
            }
        }

        return blocks;
    }

    private static List<String> parseCells(String row) {
        String trimmed = row.strip();
        if (trimmed.startsWith("|")) trimmed = trimmed.substring(1);
        if (trimmed.endsWith("|"))   trimmed = trimmed.substring(0, trimmed.length() - 1);
        return Arrays.stream(trimmed.split("\\|"))
                .map(String::strip)
                .toList();
    }
}
