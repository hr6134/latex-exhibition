package ru.toshchev.latex.exhibition;

import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;

import java.io.FileInputStream;

public class DebugSlide {
    public static void main(String[] args) throws Exception {
        String[] files = {"slide1-template.pptx", "slide2-template.pptx", "output-slide-by-slide.pptx"};
        for (String file : files) {
            System.out.println("=== " + file + " ===");
            try (XMLSlideShow pptx = new XMLSlideShow(new FileInputStream(file))) {
                System.out.println("  Slides: " + pptx.getSlides().size());
                for (XSLFSlide slide : pptx.getSlides()) {
                    System.out.println("  Slide: " + slide.getSlideName());
                    System.out.println("  Layout: " + slide.getSlideLayout().getName());
                    for (XSLFShape shape : slide.getShapes()) {
                        System.out.println("  Shape: " + shape.getClass().getSimpleName()
                                + " shapeId=" + shape.getShapeId());
                        if (shape instanceof XSLFTextShape ts) {
                            String text = ts.getText();
                            System.out.println("    text='" + text.substring(0, Math.min(60, text.length())) + "'");
                        }
                    }
                }
            }
        }
    }
}
