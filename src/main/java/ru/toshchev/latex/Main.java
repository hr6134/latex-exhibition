package ru.toshchev.latex;

import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import ru.toshchev.latex.formula.LatexFormulaInserter;

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

            // Subtitle / content placeholder
            XSLFTextShape content = slide.getPlaceholder(1);
            content.clearText();
            content.addNewTextParagraph().addNewTextRun().setText("If you thought physic is easy");
            content.getTextParagraphs().get(0).getTextRuns().get(0).setFontSize(24.0);
            content.addNewTextParagraph().addNewTextRun().setText(
                    "Then look at this beauty.");
            content.addNewTextParagraph().addNewTextRun().setText(
                    "Schrödinger's cat is jealous of this presentation.");

            // Insert a LaTeX formula as a native OMML equation shape
            LatexFormulaInserter formulaInserter = new LatexFormulaInserter();
            formulaInserter.insert(slide,
                    "\\[ \\mathrm{i}\\hbar \\frac{\\partial}{\\partial t} \\Psi(\\mathbf{r}, t) = \\left[ -\\frac{\\hbar^2}{2m} \\nabla^2 + V(\\mathbf{r}, t) \\right] \\Psi(\\mathbf{r}, t) \\]\n",
                    50, 420, 620, 80);

            try (FileOutputStream out = new FileOutputStream("output.pptx")) {
                pptx.write(out);
            }
            System.out.println("Saved: output.pptx");
        }
    }
}
