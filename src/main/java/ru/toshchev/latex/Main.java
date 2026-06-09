package ru.toshchev.latex;

import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import ru.toshchev.latex.formula.markdown.MarkdownSlideRenderer;

import java.io.FileOutputStream;

public class Main {
    public static void main(String[] args) throws Exception {
        generatePresentation();
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
            renderer.render(slide, content, markdown, 50, 420, 620);

            try (FileOutputStream out = new FileOutputStream("output.pptx")) {
                pptx.write(out);
            }
            System.out.println("Saved: output.pptx");
        }
    }
}
