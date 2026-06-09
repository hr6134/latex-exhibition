package ru.toshchev.latex.exhibition;

import ru.toshchev.latex.exhibition.formula.LatexConversionException;
import ru.toshchev.latex.exhibition.formula.markdown.PptxTemplateRenderer;
import ru.toshchev.latex.exhibition.provider.PptxFileProvider;

import java.io.IOException;
import java.util.Map;

/**
 * Main entry point for the <strong>latex-exhibition</strong> library.
 *
 * <p>Fills a PPTX template with Markdown+LaTeX content. Each text shape in the
 * template whose text exactly matches a key in the supplied map is replaced with
 * the rendered value (Markdown, inline formulas, lists, tables, code blocks).</p>
 *
 * <h2>Quick start</h2>
 * <pre>{@code
 * try (PptxFileProvider provider = new LocalFileProvider("template.pptx", "output.pptx")) {
 *     LatexExhibition.fromProvider(provider)
 *                    .render(Map.of(
 *                        "{title}", "My Slide",
 *                        "{body}",  "The equation $E=mc^2$ is **famous**."
 *                    ));
 * }
 * }</pre>
 *
 * <h2>Template authoring</h2>
 * <p>Create a {@code .pptx} file in PowerPoint. In each text shape that should
 * receive dynamic content, type the token (e.g. {@code {title}}) as the shape's
 * only text. Style the shape however you like — the renderer only replaces the
 * text content, leaving position, size, and background intact.</p>
 */
public class LatexExhibition {

    private final PptxFileProvider    provider;
    private final PptxTemplateRenderer renderer;

    private LatexExhibition(PptxFileProvider provider) throws LatexConversionException {
        this.provider = provider;
        this.renderer = new PptxTemplateRenderer();
    }

    /**
     * Creates a {@link LatexExhibition} bound to the given {@link PptxFileProvider}.
     *
     * @param provider supplies the template input and rendered output streams
     * @throws LatexConversionException if the internal XSLT processor cannot be initialised
     */
    public static LatexExhibition fromProvider(PptxFileProvider provider)
            throws LatexConversionException {
        return new LatexExhibition(provider);
    }

    /**
     * Renders the template: each shape whose text matches a key in {@code values}
     * is replaced with the corresponding Markdown+LaTeX content.
     *
     * @param values map of {@code {token}} → Markdown+LaTeX string
     * @throws IOException              if reading the template or writing the output fails
     * @throws LatexConversionException if a LaTeX formula cannot be converted
     */
    public void render(Map<String, String> values) throws IOException, LatexConversionException {
        renderer.render(provider.templateInput(), provider.renderedOutput(), values);
    }
}
