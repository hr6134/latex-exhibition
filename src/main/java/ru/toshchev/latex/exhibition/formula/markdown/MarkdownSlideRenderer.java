package ru.toshchev.latex.exhibition.formula.markdown;

import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTextParagraph;
import org.apache.poi.xslf.usermodel.XSLFTextRun;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.xmlbeans.XmlCursor;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import ru.toshchev.latex.exhibition.formula.LatexConversionException;
import ru.toshchev.latex.exhibition.formula.LatexToMathMl;
import ru.toshchev.latex.exhibition.formula.MathMlToOmml;
import ru.toshchev.latex.exhibition.formula.inline.ContentToken;
import ru.toshchev.latex.exhibition.formula.inline.InlineFormulaParser;

import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * Renders a Markdown string into an Apache POI {@link XSLFSlide}.
 *
 * <p>Block-level elements are appended to a provided {@link XSLFTextShape} (for text
 * content), while tables are created as separate {@link XSLFTable} shapes on the slide.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * MarkdownSlideRenderer renderer = new MarkdownSlideRenderer();
 * renderer.render(slide, contentShape, markdownString, tableX, tableY, tableWidth);
 * }</pre>
 */
public class MarkdownSlideRenderer {

    private static final String A_NS   = "http://schemas.openxmlformats.org/drawingml/2006/main";
    private static final String M_NS   = "http://schemas.openxmlformats.org/officeDocument/2006/math";
    private static final String A14_NS = "http://schemas.microsoft.com/office/drawing/2010/main";

    // Heading font sizes by level
    private static final double[] HEADING_SIZES = {36, 28, 24, 20, 18, 16};

    private final MarkdownBlockParser blockParser;
    private final InlineFormulaParser inlineParser;
    private final LatexToMathMl latexToMathMl;
    private final MathMlToOmml mathMlToOmml;

    public MarkdownSlideRenderer() throws LatexConversionException {
        this.blockParser    = new MarkdownBlockParser();
        this.inlineParser   = new InlineFormulaParser();
        this.latexToMathMl  = new LatexToMathMl();
        this.mathMlToOmml   = new MathMlToOmml();
    }

    /**
     * Parses {@code markdown} and renders each block element into {@code shape}
     * (paragraphs, headings, lists, code blocks) or as a new table shape on {@code slide}.
     *
     * @param slide      the target slide (needed for table shapes)
     * @param shape      the text shape that receives paragraph-level content
     * @param markdown   the Markdown input string
     * @param tableX     left position in points for any table shapes
     * @param tableY     top position in points for the first table shape
     * @param tableWidth width in points for table shapes
     * @throws LatexConversionException if any inline formula fails to convert
     */
    public void render(XSLFSlide slide, XSLFTextShape shape, String markdown,
                       double tableX, double tableY, double tableWidth)
            throws LatexConversionException {
        List<BlockElement> blocks = blockParser.parse(markdown);
        double nextTableY = tableY;

        for (BlockElement block : blocks) {
            switch (block) {
                case BlockElement.Heading h      -> renderHeading(shape, h);
                case BlockElement.Paragraph p    -> renderParagraph(shape, p.inlineContent());
                case BlockElement.BulletList l   -> renderList(shape, l.items(), false);
                case BlockElement.OrderedList l  -> renderList(shape, l.items(), true);
                case BlockElement.CodeBlock c    -> renderCodeBlock(shape, c);
                case BlockElement.HorizontalRule ignored -> { /* skip — no native equivalent */ }
                case BlockElement.Table t -> {
                    double height = renderTable(slide, t, tableX, nextTableY, tableWidth);
                    nextTableY += height + 10;
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Heading
    // -------------------------------------------------------------------------

    private void renderHeading(XSLFTextShape shape, BlockElement.Heading h)
            throws LatexConversionException {
        String paragraphXml = buildInlineParagraphXml(h.inlineContent());
        appendToShape(shape, paragraphXml, para -> {
            XSLFTextRun run = para.addNewTextRun();
            run.setBold(true);
            run.setFontSize(HEADING_SIZES[Math.min(h.level() - 1, HEADING_SIZES.length - 1)]);
        });
    }

    // -------------------------------------------------------------------------
    // Paragraph
    // -------------------------------------------------------------------------

    private void renderParagraph(XSLFTextShape shape, String inlineContent)
            throws LatexConversionException {
        String paragraphXml = buildInlineParagraphXml(inlineContent);
        appendRawParagraph(shape, paragraphXml);
    }

    // -------------------------------------------------------------------------
    // Lists
    // -------------------------------------------------------------------------

    private void renderList(XSLFTextShape shape, List<BlockElement.ListItem> items,
                            boolean ordered) throws LatexConversionException {
        for (int idx = 0; idx < items.size(); idx++) {
            BlockElement.ListItem item = items.get(idx);
            String bullet = ordered ? (idx + 1) + ". " : "• ";
            String indent = "  ".repeat(item.level());
            String inlineContent = indent + bullet + item.inlineContent();
            String paragraphXml = buildInlineParagraphXml(inlineContent);
            appendRawParagraph(shape, paragraphXml);
        }
    }

    // -------------------------------------------------------------------------
    // Code block
    // -------------------------------------------------------------------------

    private void renderCodeBlock(XSLFTextShape shape, BlockElement.CodeBlock block) {
        for (String line : block.code().split("\n", -1)) {
            XSLFTextParagraph para = shape.addNewTextParagraph();
            XSLFTextRun run = para.addNewTextRun();
            run.setText(line);
            run.setFontFamily("Courier New");
            run.setFontSize(11.0);
        }
    }

    // -------------------------------------------------------------------------
    // Table
    // -------------------------------------------------------------------------

    /**
     * Creates an {@link XSLFTable} shape on the slide and populates it.
     *
     * @return the height of the created table shape in points
     */
    private double renderTable(XSLFSlide slide, BlockElement.Table table,
                               double x, double y, double width) {
        int cols = table.headers().size();
        int rows = 1 + table.rows().size(); // header row + body rows
        double rowHeight = 20.0;
        double colWidth  = width / cols;
        double height    = rows * rowHeight;

        XSLFTable tbl = slide.createTable(rows, cols);
        tbl.setAnchor(new Rectangle2D.Double(x, y, width, height));
        for (int c = 0; c < cols; c++) {
            tbl.setColumnWidth(c, colWidth);
        }

        // Header row
        for (int c = 0; c < cols; c++) {
            XSLFTableCell cell = tbl.getCell(0, c);
            cell.setText(table.headers().get(c));
            XSLFTextRun run = cell.getTextParagraphs().get(0).getTextRuns().get(0);
            run.setBold(true);
        }

        // Body rows
        for (int r = 0; r < table.rows().size(); r++) {
            List<String> row = table.rows().get(r);
            for (int c = 0; c < Math.min(cols, row.size()); c++) {
                tbl.getCell(r + 1, c).setText(row.get(c));
            }
        }

        return height;
    }

    // -------------------------------------------------------------------------
    // Inline XML builder
    // -------------------------------------------------------------------------

    private String buildInlineParagraphXml(String inlineContent)
            throws LatexConversionException {
        List<ContentToken> tokens = inlineParser.parse(inlineContent);

        StringBuilder sb = new StringBuilder();
        sb.append("<a:p")
          .append(" xmlns:a=\"").append(A_NS).append("\"")
          .append(" xmlns:m=\"").append(M_NS).append("\"")
          .append(" xmlns:a14=\"").append(A14_NS).append("\"")
          .append(">");

        for (ContentToken token : tokens) {
            switch (token) {
                case ContentToken.Text t          -> appendTextRun(sb, t.value(), false, false, false);
                case ContentToken.Bold t          -> appendTextRun(sb, t.value(), true,  false, false);
                case ContentToken.Italic t        -> appendTextRun(sb, t.value(), false, true,  false);
                case ContentToken.BoldItalic t    -> appendTextRun(sb, t.value(), true,  true,  false);
                case ContentToken.Strikethrough t -> appendTextRun(sb, t.value(), false, false, true);
                case ContentToken.Code t          -> appendCodeRun(sb, t.value());
                case ContentToken.Formula f       -> appendFormulaRun(sb, f.latex());
            }
        }

        sb.append("</a:p>");
        return sb.toString();
    }

    private void appendTextRun(StringBuilder sb, String text,
                                boolean bold, boolean italic, boolean strike) {
        boolean hasProps = bold || italic || strike;
        sb.append("<a:r>");
        if (hasProps) {
            sb.append("<a:rPr");
            if (bold)   sb.append(" b=\"1\"");
            if (italic) sb.append(" i=\"1\"");
            if (strike) sb.append(" strike=\"sngStrike\"");
            sb.append("/>");
        }
        sb.append("<a:t>").append(escapeXml(text)).append("</a:t></a:r>");
    }

    private void appendCodeRun(StringBuilder sb, String text) {
        sb.append("<a:r>")
          .append("<a:rPr><a:latin typeface=\"Courier New\"/></a:rPr>")
          .append("<a:t>").append(escapeXml(text)).append("</a:t>")
          .append("</a:r>");
    }

    private void appendFormulaRun(StringBuilder sb, String latex)
            throws LatexConversionException {
        String omml = mathMlToOmml.convert(latexToMathMl.convert(latex));
        String ommlBody = stripXmlDeclaration(omml);
        sb.append("<a14:m>")
          .append("<m:oMathPara xmlns:m=\"").append(M_NS).append("\">")
          .append("<m:oMathParaPr><m:jc m:val=\"centerGroup\"/></m:oMathParaPr>")
          .append(ommlBody)
          .append("</m:oMathPara>")
          .append("</a14:m>");
    }

    // -------------------------------------------------------------------------
    // Shape insertion helpers
    // -------------------------------------------------------------------------

    private void appendRawParagraph(XSLFTextShape shape, String paragraphXml)
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
            throw new LatexConversionException("Failed to append paragraph to shape", e);
        }
    }

    /**
     * Appends an inline paragraph, then post-processes the last added paragraph
     * via the provided {@link ParagraphCustomizer} to apply extra run properties
     * (e.g. font size for headings).
     */
    private void appendToShape(XSLFTextShape shape, String paragraphXml,
                                ParagraphCustomizer customizer)
            throws LatexConversionException {
        appendRawParagraph(shape, paragraphXml);
        List<XSLFTextParagraph> paras = shape.getTextParagraphs();
        if (!paras.isEmpty()) {
            customizer.customize(paras.get(paras.size() - 1));
        }
    }

    @FunctionalInterface
    private interface ParagraphCustomizer {
        void customize(XSLFTextParagraph para);
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

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
