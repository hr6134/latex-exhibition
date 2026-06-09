package ru.toshchev.latex.exhibition;

import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import ru.toshchev.latex.exhibition.formula.markdown.MarkdownSlideRenderer;
import ru.toshchev.latex.exhibition.provider.ClasspathFileProvider;
import ru.toshchev.latex.exhibition.provider.LocalFileProvider;
import ru.toshchev.latex.exhibition.provider.PptxFileProvider;
import ru.toshchev.latex.exhibition.provider.StreamFileProvider;

import java.awt.geom.Rectangle2D;
import java.io.FileOutputStream;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        generatePresentation();
        // createTemplate();
        generateFromTemplate();
    }

    /**
     * Creates latex-exhibition.pptx — a blank template with {title} and {body} token shapes.
     * Run this once to produce the template file, then use generateFromTemplate().
     */
    public static void createTemplate() throws Exception {
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            XSLFSlideMaster master = pptx.getSlideMasters().get(0);
            XSLFSlideLayout layout = master.getLayout(SlideLayout.BLANK);
            XSLFSlide slide = pptx.createSlide(layout);

            XSLFTextBox titleBox = slide.createTextBox();
            titleBox.setAnchor(new Rectangle2D.Double(50, 30, 620, 60));
            titleBox.setText("{title}");

            XSLFTextBox bodyBox = slide.createTextBox();
            bodyBox.setAnchor(new Rectangle2D.Double(50, 110, 620, 380));
            bodyBox.setText("{body}");

            try (FileOutputStream out = new FileOutputStream("latex-exhibition.pptx")) {
                pptx.write(out);
            }
            System.out.println("Template saved: latex-exhibition.pptx");
        }
    }

    /**
     * Demonstrates PptxTemplateRenderer: opens latex-exhibition.pptx, fills {title} and {body}
     * with Markdown+LaTeX content, and writes the result to output-from-template.pptx.
     */
    public static void generateFromTemplate() throws Exception {
        Map<String, String> values = Map.of(
            "{title}", 
                "Best Presentation Ever!!!",
            "{leftColumn}",
                """
                ## Key equations

                The **Schrödinger equation** governs quantum mechanics:
                $\\mathrm{i}\\hbar \\frac{\\partial}{\\partial t} \\Psi = \\hat{H}\\Psi$

                ## Famous results

                - **Energy-mass**: $E = mc^2$
                - *Wave-particle* duality: $\\lambda = h / p$
                - Uncertainty: $\\Delta x \\, \\Delta p \\geq \\hbar / 2$
                """,
            "{rightColumn}",
                """
                ## Constants

                | Constant | Symbol | Value |
                |---|---|---|
                | Speed of light | c | 3×10⁸ m/s |
                | Planck constant | h | 6.626×10⁻³⁴ J·s |

                ```python
                # Compute energy
                E = m * c**2
                ```
                """
        );

        // Option 1: local files (used here)
        try (PptxFileProvider provider = new LocalFileProvider(
                "latex-exibition.pptx", "output-from-template.pptx")) {
            LatexExhibition.fromProvider(provider).render(values);
        }

        // Option 2: template bundled inside the jar (classpath resource)
        // try (PptxFileProvider provider = new ClasspathFileProvider(
        //         "/templates/latex-exibition.pptx", "output-from-template.pptx")) {
        //     LatexExhibition.fromProvider(provider).render(values);
        // }

        // Option 3: arbitrary streams (e.g. S3, HTTP, ByteArrayOutputStream)
        // try (PptxFileProvider provider = new StreamFileProvider(templateInputStream, outputStream)) {
        //     LatexExhibition.fromProvider(provider).render(values);
        // }

        System.out.println("Saved: output-from-template.pptx");
    }

    public static void generatePresentation() throws Exception {
        try (XMLSlideShow pptx = new XMLSlideShow()) {
            XSLFSlideMaster master = pptx.getSlideMasters().get(0);
            XSLFSlideLayout layout = master.getLayout(SlideLayout.TITLE_AND_CONTENT);

            XSLFSlide slide = pptx.createSlide(layout);

            // Title
            XSLFTextShape title = slide.getPlaceholder(0);
            title.setText("Best Presentation Ever");
            title.getTextParagraphs().get(0).getTextRuns().get(0).setFontSize(36.0);

            // Render Markdown content into the content placeholder
            XSLFTextShape content = slide.getPlaceholder(1);
            content.clearText();

            String markdown = """
                    ## Key equations
                    
                    The **Schrödinger equation** governs quantum mechanics:
                    $\\mathrm{i}\\hbar \\frac{\\partial}{\\partial t} \\Psi = \\hat{H}\\Psi$
                    
                    ## Famous results
                    
                    - **Energy-mass**: $E = mc^2$
                    - *Wave-particle* duality: $\\lambda = h / p$
                    - Uncertainty: $\\Delta x \\, \\Delta p \\geq \\hbar / 2$
                    
                    ## Constants
                    
                    | Constant | Symbol | Value |
                    |---|---|---|
                    | Speed of light | c | 3×10⁸ m/s |
                    | Planck constant | h | 6.626×10⁻³⁴ J·s |
                    
                    ```python
                    # Compute energy
                    E = m * c**2
                    ```
                    """;

            MarkdownSlideRenderer renderer = new MarkdownSlideRenderer();
            renderer.render(slide, content, markdown, 50, 100, 620);

            try (FileOutputStream out = new FileOutputStream("output.pptx")) {
                pptx.write(out);
            }
            System.out.println("Saved: output.pptx");
        }
    }
}
