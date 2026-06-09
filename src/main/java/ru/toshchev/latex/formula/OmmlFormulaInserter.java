package ru.toshchev.latex.formula;

import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextBox;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;

import java.awt.geom.Rectangle2D;

public class OmmlFormulaInserter {

    private static final String OMML_NS =
            "http://schemas.openxmlformats.org/officeDocument/2006/math";

    private static final String A14_NS =
            "http://schemas.microsoft.com/office/drawing/2010/main";

    /**
     * Inserts an OMML formula string into the given slide at the specified position.
     *
     * @param slide  the target slide
     * @param omml   OMML string (the {@code <m:oMath>} element and its content)
     * @param x      left position in points
     * @param y      top position in points
     * @param width  width in points
     * @param height height in points
     * @throws LatexConversionException if the OMML XML cannot be parsed
     */
    public void insert(XSLFSlide slide, String omml,
                       double x, double y, double width, double height)
            throws LatexConversionException {
        XSLFTextBox textBox = slide.createTextBox();
        textBox.setAnchor(new Rectangle2D.Double(x, y, width, height));

        CTShape ctShape = (CTShape) textBox.getXmlObject();
        CTTextBody txBody = ctShape.getTxBody();

        // Remove any default paragraphs POI added
        while (txBody.sizeOfPArray() > 0) {
            txBody.removeP(0);
        }

        // Parse as CTTextParagraph so the cursor is positioned directly at <a:p>,
        // with no xml-fragment wrapper around it.
        String paragraphXml = buildParagraphXml(omml);
        try {
            CTTextParagraph pObj = CTTextParagraph.Factory.parse(paragraphXml);
            try (XmlCursor txCursor = txBody.newCursor();
                 XmlCursor pCursor = pObj.newCursor()) {
                // txCursor at END token = insertion point just before </txBody>
                txCursor.toEndToken();
                // pCursor starts at START token of <a:p> element itself
                pCursor.toStartDoc();
                pCursor.toNextToken();
                pCursor.moveXml(txCursor);
            }
        } catch (org.apache.xmlbeans.XmlException e) {
            throw new LatexConversionException("Failed to insert OMML paragraph", e);
        }
    }

    private static String stripXmlDeclaration(String xml) {
        String trimmed = xml.trim();
        if (trimmed.startsWith("<?xml")) {
            int end = trimmed.indexOf("?>");
            if (end >= 0) {
                return trimmed.substring(end + 2).trim();
            }
        }
        return trimmed;
    }

    private String buildParagraphXml(String omml) {
        String ommlBody = stripXmlDeclaration(omml);
        return "<a:p" +
                " xmlns:a=\"http://schemas.openxmlformats.org/drawingml/2006/main\"" +
                " xmlns:m=\"" + OMML_NS + "\"" +
                " xmlns:a14=\"" + A14_NS + "\">" +
                "<a14:m>" +
                "<m:oMathPara xmlns:m=\"" + OMML_NS + "\">" +
                "<m:oMathParaPr><m:jc m:val=\"centerGroup\"/></m:oMathParaPr>" +
                ommlBody +
                "</m:oMathPara>" +
                "</a14:m>" +
                "</a:p>";
    }
}
