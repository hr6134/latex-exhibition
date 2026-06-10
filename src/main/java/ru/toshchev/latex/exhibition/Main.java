package ru.toshchev.latex.exhibition;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws Exception {
        generateSlideBySlide();
    }

    /**
     * Demonstrates the slide-by-slide builder API:
     * each slide is sourced from its own single-slide PPTX template.
     */
    public static void generateSlideBySlide() throws Exception {
        Map<String, String> slide1Values = Map.of(
            "{title}", "Best Presentation Ever!!!",
            "{leftColumn}",
                """
                ## Key equations

                The **Schrödinger equation** governs quantum mechanics:
                $\\mathrm{i}\\hbar \\frac{\\partial}{\\partial t} \\Psi = \\hat{H}\\Psi$

                - **Energy-mass**: $E = mc^2$
                - *Wave-particle* duality: $\\lambda = h / p$
                """,
            "{rightColumn}",
                """
                ## Constants

                | Constant | Symbol | Value |
                |---|---|---|
                | Speed of light | c | 3×10⁸ m/s |
                | Planck constant | h | 6.626×10⁻³⁴ J·s |
                """
        );

        Map<String, String> slide2Values = Map.of(
            "{title}", "Second Slide",
            "{leftColumn}",
                """
                ## Famous results

                - *Wave-particle* duality: $\\lambda = h / p$
                - Uncertainty: $\\Delta x \\, \\Delta p \\geq \\hbar / 2$
                """,
            "{rightColumn}",
                """
                ```python
                # Compute energy
                E = m * c**2
                ```
                """
        );

        try (FileInputStream t1  = new FileInputStream("slide1-template.pptx");
             FileInputStream t2  = new FileInputStream("slide2-template.pptx");
             FileOutputStream out = new FileOutputStream("output-slide-by-slide.pptx")) {

            LatexExhibition.newPresentation()
                .addSlide(t1, slide1Values)
                .addSlide(t2, slide2Values)
                .writeTo(out);
        }
        System.out.println("Saved: output-slide-by-slide.pptx");
    }
}
