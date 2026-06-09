package ru.toshchev.latex.formula.markdown;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import ru.toshchev.latex.formula.LatexConversionException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Template engine for PPTX files.
 *
 * <p>Opens a template {@code .pptx}, finds every text shape whose trimmed content
 * exactly matches a key in the provided map, and replaces it with the
 * corresponding Markdown+LaTeX value rendered as native PPTX content.</p>
 *
 * <h2>Template authoring</h2>
 * <p>In PowerPoint, put placeholder tokens (e.g. {@code {title}}, {@code {body}})
 * as the sole text in a text box or placeholder shape. The token must be the
 * entire text of the shape — no surrounding characters.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try (InputStream template = new FileInputStream("template.pptx");
 *      OutputStream out     = new FileOutputStream("output.pptx")) {
 *
 *     new PptxTemplateRenderer().render(template, out, Map.of(
 *         "{title}", "# My Presentation",
 *         "{body}",  "The equation $E=mc^2$ is **famous**.\n\n- Item one\n- Item two"
 *     ));
 * }
 * }</pre>
 */
public class PptxTemplateRenderer {

    private final MarkdownSlideRenderer markdownRenderer;

    public PptxTemplateRenderer() throws LatexConversionException {
        this.markdownRenderer = new MarkdownSlideRenderer();
    }

    /**
     * Reads a PPTX template from {@code templateIn}, replaces all matching
     * placeholder shapes according to {@code values}, and writes the result
     * to {@code out}.
     *
     * @param templateIn input stream of the template {@code .pptx}
     * @param out        output stream for the generated {@code .pptx}
     * @param values     map of placeholder token → Markdown+LaTeX content
     * @throws IOException              if the template cannot be read or written
     * @throws LatexConversionException if any formula in the values fails to convert
     */
    public void render(InputStream templateIn, OutputStream out, Map<String, String> values)
            throws IOException, LatexConversionException {
        try (XMLSlideShow pptx = new XMLSlideShow(templateIn)) {
            for (XSLFSlide slide : pptx.getSlides()) {
                // Collect matches first to avoid ConcurrentModificationException
                List<XSLFTextShape> matches = new ArrayList<>();
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape
                            && values.containsKey(textShape.getText().strip())) {
                        matches.add(textShape);
                    }
                }
                for (XSLFTextShape textShape : matches) {
                    String token = textShape.getText().strip();
                    textShape.clearText();
                    markdownRenderer.render(
                            slide,
                            textShape,
                            values.get(token),
                            textShape.getAnchor().getX(),
                            textShape.getAnchor().getY() + 100,
                            textShape.getAnchor().getWidth()
                    );
                }
            }
            pptx.write(out);
        }
    }
}
