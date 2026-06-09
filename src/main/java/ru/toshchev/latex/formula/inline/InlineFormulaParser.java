package ru.toshchev.latex.formula.inline;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Splits a string containing embedded LaTeX formulas into a list of {@link ContentToken}s.
 *
 * <p>Recognised delimiters:
 * <ul>
 *   <li>{@code $...$} — inline math</li>
 *   <li>{@code \(...\)} — inline math</li>
 *   <li>{@code \[...\]} — display math</li>
 * </ul>
 *
 * <p>Example input:
 * <pre>
 *   "Schrödinger's cat. $E=mc^2$. Look how lovely it is."
 * </pre>
 * produces: Text("Schrödinger's cat. "), Formula("E=mc^2"), Text(". Look how lovely it is.")
 */
public class InlineFormulaParser {

    private static final Pattern FORMULA_PATTERN = Pattern.compile(
            "\\$(.+?)\\$"                  // $...$
            + "|\\\\\\[(.+?)\\\\\\]"        // \[...\]
            + "|\\\\\\((.+?)\\\\\\)",       // \(...\)
            Pattern.DOTALL
    );

    /**
     * Parses the input string and returns an ordered list of {@link ContentToken}s.
     */
    public List<ContentToken> parse(String input) {
        List<ContentToken> tokens = new ArrayList<>();
        Matcher m = FORMULA_PATTERN.matcher(input);
        int lastEnd = 0;

        while (m.find()) {
            if (m.start() > lastEnd) {
                tokens.add(new ContentToken.Text(input.substring(lastEnd, m.start())));
            }
            String latex = firstNonNull(m.group(1), m.group(2), m.group(3));
            tokens.add(new ContentToken.Formula(latex.strip()));
            lastEnd = m.end();
        }

        if (lastEnd < input.length()) {
            tokens.add(new ContentToken.Text(input.substring(lastEnd)));
        }

        return tokens;
    }

    private static String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null) return v;
        }
        throw new IllegalStateException("All groups were null — regex match inconsistency");
    }
}
