package ru.toshchev.latex.formula.inline;

import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import ru.toshchev.latex.formula.LatexConversionException;
import ru.toshchev.latex.formula.LatexToMathMl;
import ru.toshchev.latex.formula.MathMlToOmml;

import java.util.List;

/**
 * Appends a mixed text+formula paragraph to an existing {@link XSLFTextShape}.
 *
 * <p>Each {@link ContentToken.Text} becomes an {@code <a:r>} run and each
 * {@link ContentToken.Formula} becomes an {@code <a14:m>} inline equation,
 * all inside the same {@code <a:p>} paragraph.</p>
 */
public class InlineParagraphAppender {

    private static final String A_NS  = "http://schemas.openxmlformats.org/drawingml/2006/main";
    private static final String M_NS  = "http://schemas.openxmlformats.org/officeDocument/2006/math";
    private static final String A14_NS = "http://schemas.microsoft.com/office/drawing/2010/main";

    private final LatexToMathMl latexToMathMl;
    private final MathMlToOmml mathMlToOmml;
    private final InlineFormulaParser parser;

    public InlineParagraphAppender() throws LatexConversionException {
        this.latexToMathMl = new LatexToMathMl();
        this.mathMlToOmml  = new MathMlToOmml();
        this.parser        = new InlineFormulaParser();
    }

    /**
     * Parses {@code text} for embedded LaTeX formulas and appends the resulting
     * mixed paragraph to {@code shape}.
     *
     * <p>Formulas are delimited by {@code $...$}, {@code \(...\)}, or {@code \[...\]}.</p>
     *
     * @param shape the target text shape (e.g. a content placeholder)
     * @param text  the input string, e.g.
     *              {@code "See $E=mc^2$ — beautiful!"}
     * @throws LatexConversionException if any formula fails to convert
     */
    public void append(XSLFTextShape shape, String text) throws LatexConversionException {
        List<ContentToken> tokens = parser.parse(text);
        String paragraphXml = buildParagraphXml(tokens);
        appendParagraphToShape(shape, paragraphXml);
    }

    private String buildParagraphXml(List<ContentToken> tokens) throws LatexConversionException {
        StringBuilder sb = new StringBuilder();
        sb.append("<a:p")
          .append(" xmlns:a=\"").append(A_NS).append("\"")
          .append(" xmlns:m=\"").append(M_NS).append("\"")
          .append(" xmlns:a14=\"").append(A14_NS).append("\"")
          .append(">");

        for (ContentToken token : tokens) {
            switch (token) {
                case ContentToken.Text t -> appendTextRun(sb, t.value());
                case ContentToken.Formula f -> appendFormulaRun(sb, f.latex());
            }
        }

        sb.append("</a:p>");
        return sb.toString();
    }

    private void appendTextRun(StringBuilder sb, String text) {
        sb.append("<a:r><a:t>")
          .append(escapeXml(text))
          .append("</a:t></a:r>");
    }

    private void appendFormulaRun(StringBuilder sb, String latex) throws LatexConversionException {
        String mathMl = latexToMathMl.convert(latex);
        String omml   = mathMlToOmml.convert(mathMl);
        String ommlBody = stripXmlDeclaration(omml);

        sb.append("<a14:m>")
          .append("<m:oMathPara xmlns:m=\"").append(M_NS).append("\">")
          .append("<m:oMathParaPr><m:jc m:val=\"centerGroup\"/></m:oMathParaPr>")
          .append(ommlBody)
          .append("</m:oMathPara>")
          .append("</a14:m>");
    }

    private void appendParagraphToShape(XSLFTextShape shape, String paragraphXml)
            throws LatexConversionException {
        CTShape ctShape = (CTShape) shape.getXmlObject();
        CTTextBody txBody = ctShape.getTxBody();
        try {
            CTTextParagraph pObj = CTTextParagraph.Factory.parse(paragraphXml);
            try (XmlCursor txCursor = txBody.newCursor();
                 XmlCursor pCursor  = pObj.newCursor()) {
                txCursor.toEndToken();
                pCursor.toStartDoc();
                pCursor.toNextToken();
                pCursor.moveXml(txCursor);
            }
        } catch (org.apache.xmlbeans.XmlException e) {
            throw new LatexConversionException("Failed to append inline paragraph to shape", e);
        }
    }

    private static String escapeXml(String s) {
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String stripXmlDeclaration(String xml) {
        String s = xml.trim();
        if (s.startsWith("<?xml")) {
            int end = s.indexOf("?>");
            if (end >= 0) return s.substring(end + 2).trim();
        }
        return s;
    }
}
