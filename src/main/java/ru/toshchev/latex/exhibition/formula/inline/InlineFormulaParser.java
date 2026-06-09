package ru.toshchev.latex.exhibition.formula.inline;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses a string containing Markdown inline formatting and LaTeX formulas
 * into an ordered list of {@link ContentToken}s.
 *
 * <p>Recognised syntax (evaluated in this priority order):
 * <ul>
 *   <li>{@code ***text***} / {@code ___text___} → {@link ContentToken.BoldItalic}</li>
 *   <li>{@code **text**} / {@code __text__} → {@link ContentToken.Bold}</li>
 *   <li>{@code *text*} / {@code _text_} → {@link ContentToken.Italic}</li>
 *   <li>{@code ~~text~~} → {@link ContentToken.Strikethrough}</li>
 *   <li>{@code `text`} → {@link ContentToken.Code}</li>
 *   <li>{@code $text$} / {@code \[text\]} / {@code \(text\)} → {@link ContentToken.Formula}</li>
 * </ul>
 */
public class InlineFormulaParser {

    private static final Pattern PATTERN = Pattern.compile(
            "\\*\\*\\*(.+?)\\*\\*\\*"      // ***bold-italic***
            + "|___(.+?)___"                // ___bold-italic___
            + "|\\*\\*(.+?)\\*\\*"          // **bold**
            + "|__(.+?)__"                  // __bold__
            + "|\\*(.+?)\\*"               // *italic*
            + "|_(.+?)_"                   // _italic_
            + "|~~(.+?)~~"                 // ~~strikethrough~~
            + "|`(.+?)`"                   // `code`
            + "|\\$(.+?)\\$"              // $formula$
            + "|\\\\\\[(.+?)\\\\\\]"       // \[formula\]
            + "|\\\\\\((.+?)\\\\\\)",      // \(formula\)
            Pattern.DOTALL
    );

    /**
     * Parses the input string and returns an ordered list of {@link ContentToken}s.
     */
    public List<ContentToken> parse(String input) {
        List<ContentToken> tokens = new ArrayList<>();
        Matcher m = PATTERN.matcher(input);
        int lastEnd = 0;

        while (m.find()) {
            if (m.start() > lastEnd) {
                tokens.add(new ContentToken.Text(input.substring(lastEnd, m.start())));
            }
            tokens.add(toToken(m));
            lastEnd = m.end();
        }

        if (lastEnd < input.length()) {
            tokens.add(new ContentToken.Text(input.substring(lastEnd)));
        }

        return tokens;
    }

    private static ContentToken toToken(Matcher m) {
        // Groups 1-2: bold-italic
        String g;
        if ((g = m.group(1)) != null || (g = m.group(2)) != null) return new ContentToken.BoldItalic(g);
        // Groups 3-4: bold
        if ((g = m.group(3)) != null || (g = m.group(4)) != null) return new ContentToken.Bold(g);
        // Groups 5-6: italic
        if ((g = m.group(5)) != null || (g = m.group(6)) != null) return new ContentToken.Italic(g);
        // Group 7: strikethrough
        if ((g = m.group(7)) != null) return new ContentToken.Strikethrough(g);
        // Group 8: code
        if ((g = m.group(8)) != null) return new ContentToken.Code(g);
        // Groups 9-11: formula
        if ((g = m.group(9))  != null) return new ContentToken.Formula(g.strip());
        if ((g = m.group(10)) != null) return new ContentToken.Formula(g.strip());
        if ((g = m.group(11)) != null) return new ContentToken.Formula(g.strip());
        throw new IllegalStateException("Unmatched group in regex");
    }
}
