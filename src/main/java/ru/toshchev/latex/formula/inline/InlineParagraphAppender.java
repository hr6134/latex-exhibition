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
     * Parses {@code text} for Markdown inline formatting and LaTeX formulas,
     * then appends the resulting mixed paragraph to {@code shape}.
     *
     * <p>Supported Markdown: {@code **bold**}, {@code *italic*}, {@code ***bold-italic***},
     * {@code ~~strikethrough~~}, {@code `code`}, {@code $formula$}, {@code \[formula\]},
     * {@code \(formula\)}.</p>
     *
     * @param shape the target text shape (e.g. a content placeholder)
     * @param text  the input string, e.g.
     *              {@code "See **important** result: $E=mc^2$"}
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
                case ContentToken.Text t         -> appendStyledRun(sb, t.value(), false, false, false, false);
                case ContentToken.Bold t         -> appendStyledRun(sb, t.value(), true,  false, false, false);
                case ContentToken.Italic t       -> appendStyledRun(sb, t.value(), false, true,  false, false);
                case ContentToken.BoldItalic t   -> appendStyledRun(sb, t.value(), true,  true,  false, false);
                case ContentToken.Strikethrough t -> appendStyledRun(sb, t.value(), false, false, true,  false);
                case ContentToken.Code t         -> appendCodeRun(sb, t.value());
                case ContentToken.Formula f      -> appendFormulaRun(sb, f.latex());
            }
        }

        sb.append("</a:p>");
        return sb.toString();
    }

    private void appendStyledRun(StringBuilder sb, String text,
                                 boolean bold, boolean italic,
                                 boolean strike, boolean underline) {
        boolean hasProps = bold || italic || strike || underline;
        sb.append("<a:r>");
        if (hasProps) {
            sb.append("<a:rPr");
            if (bold)      sb.append(" b=\"1\"");
            if (italic)    sb.append(" i=\"1\"");
            if (strike)    sb.append(" strike=\"sngStrike\"");
            if (underline) sb.append(" u=\"sng\"");
            sb.append("/>");
        }
        sb.append("<a:t>").append(escapeXml(text)).append("</a:t></a:r>");
    }

    private void appendCodeRun(StringBuilder sb, String text) {
        sb.append("<a:r>")
          .append("<a:rPr>")
          .append("<a:latin typeface=\"Courier New\"/>")
          .append("</a:rPr>")
          .append("<a:t>").append(escapeXml(text)).append("</a:t>")
          .append("</a:r>");
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
