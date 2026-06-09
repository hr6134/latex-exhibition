package ru.toshchev.latex;

import org.apache.poi.xslf.usermodel.SlideLayout;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFSlideLayout;
import org.apache.poi.xslf.usermodel.XSLFSlideMaster;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import ru.toshchev.latex.formula.inline.InlineParagraphAppender;

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
            // Inline formula embedded in surrounding text
            InlineParagraphAppender appender = new InlineParagraphAppender();
            appender.append(content,
                    "Schrödinger's cat is **jealous** of this *presentation*: " +
                    "$\\mathrm{i}\\hbar \\frac{\\partial}{\\partial t} \\Psi = \\hat{H}\\Psi$" +
                    ". ~~Not really~~ — look how `lovely` it is.");

            appender.append(content, "See **important** result: $E=mc^2$");

            try (FileOutputStream out = new FileOutputStream("output.pptx")) {
                pptx.write(out);
            }
            System.out.println("Saved: output.pptx");
        }
    }
}
