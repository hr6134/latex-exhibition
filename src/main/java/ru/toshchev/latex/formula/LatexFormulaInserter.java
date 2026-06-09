package ru.toshchev.latex.formula;

import org.apache.poi.xslf.usermodel.XSLFSlide;

/**
 * Facade: converts a LaTeX math string and inserts the resulting formula into a POI slide.
 *
 * <p>Pipeline: LaTeX → MathML (SnuggleTeX) → OMML (XSLT) → PPTX shape (Apache POI)</p>
 */
public class LatexFormulaInserter {

    private final LatexToMathMl latexToMathMl;
    private final MathMlToOmml mathMlToOmml;
    private final OmmlFormulaInserter ommlInserter;

    public LatexFormulaInserter() throws LatexConversionException {
        this.latexToMathMl = new LatexToMathMl();
        this.mathMlToOmml = new MathMlToOmml();
        this.ommlInserter = new OmmlFormulaInserter();
    }

    /**
     * Converts the given LaTeX expression and inserts it as a native OMML equation
     * shape into the slide at the specified bounds.
     *
     * @param slide  target slide
     * @param latex  LaTeX math expression without surrounding {@code $}, e.g.
     *               {@code "\\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}"}
     * @param x      left offset in points
     * @param y      top offset in points
     * @param width  shape width in points
     * @param height shape height in points
     * @throws LatexConversionException if any step of the pipeline fails
     */
    public void insert(XSLFSlide slide, String latex,
                       double x, double y, double width, double height)
            throws LatexConversionException {
        String mathMl = latexToMathMl.convert(latex);
        String omml = mathMlToOmml.convert(mathMl);
        ommlInserter.insert(slide, omml, x, y, width, height);
    }
}
