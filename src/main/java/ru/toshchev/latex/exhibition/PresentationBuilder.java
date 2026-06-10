package ru.toshchev.latex.exhibition;

import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.openxml4j.opc.PackagePart;
import org.apache.poi.openxml4j.opc.PackagePartName;
import org.apache.poi.openxml4j.opc.PackageRelationship;
import org.apache.poi.openxml4j.opc.PackageRelationshipCollection;
import org.apache.poi.openxml4j.opc.PackagingURIHelper;
import org.apache.poi.openxml4j.opc.TargetMode;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import ru.toshchev.latex.exhibition.formula.LatexConversionException;
import ru.toshchev.latex.exhibition.formula.markdown.MarkdownSlideRenderer;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Builds a PPTX presentation slide by slide.
 *
 * <p>Each call to {@link #addSlide(InputStream, Map)} takes a single-slide PPTX,
 * fills its placeholder shapes with Markdown+LaTeX content, and merges that slide
 * (together with its layout and master) into the presentation being assembled.</p>
 *
 * <p>The first call initialises the base presentation from the supplied template.
 * Subsequent calls append their slide into that base, preserving each slide's own
 * master and layout so the output file is never corrupt.</p>
 *
 * <h2>Usage</h2>
 * <pre>{@code
 * try (InputStream s1 = new FileInputStream("slide1-template.pptx");
 *      InputStream s2 = new FileInputStream("slide2-template.pptx");
 *      OutputStream out = new FileOutputStream("output.pptx")) {
 *
 *     LatexExhibition.newPresentation()
 *         .addSlide(s1, Map.of("{title}", "Slide 1", "{body}", "Hello $E=mc^2$"))
 *         .addSlide(s2, Map.of("{title}", "Slide 2", "{body}", "- item one\n- item two"))
 *         .writeTo(out);
 * }
 * }</pre>
 */
public class PresentationBuilder {

    private final MarkdownSlideRenderer markdownRenderer;

    /**
     * Each element is the bytes of a fully-rendered single-slide PPTX.
     * Serialising early avoids POI's in-memory object graph becoming stale
     * when multiple sources are open simultaneously.
     */
    private final List<byte[]> renderedSlides = new ArrayList<>();

    PresentationBuilder(MarkdownSlideRenderer markdownRenderer) {
        this.markdownRenderer = markdownRenderer;
    }

    /**
     * Opens {@code slideTemplate}, fills its placeholder shapes according to
     * {@code values}, serialises the result, and queues it for the final output.
     *
     * @param slideTemplate input stream of a single-slide PPTX whose shapes carry
     *                      placeholder tokens matching keys in {@code values}
     * @param values        map of {@code {token}} → Markdown+LaTeX string
     * @return {@code this}, for chaining
     * @throws IOException              if the template stream cannot be read
     * @throws LatexConversionException if a LaTeX formula cannot be converted
     */
    public PresentationBuilder addSlide(InputStream slideTemplate, Map<String, String> values)
            throws IOException, LatexConversionException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (XMLSlideShow source = new XMLSlideShow(slideTemplate)) {
            if (source.getSlides().isEmpty()) {
                throw new IllegalArgumentException("Slide template contains no slides");
            }
            if (source.getSlides().size() > 1) {
                throw new IllegalArgumentException("Slide template contains more than one slide");
            }
            fillSlide(source.getSlides().get(0), values);
            source.write(buf);
        }
        renderedSlides.add(buf.toByteArray());
        return this;
    }

    /**
     * Merges all accumulated slides into a single presentation and writes it to
     * {@code out}.
     *
     * <p>Does not close {@code out} — the caller retains ownership.</p>
     *
     * @param out destination stream for the generated {@code .pptx}
     * @throws IOException if writing fails
     */
    public void writeTo(OutputStream out) throws IOException {
        if (renderedSlides.isEmpty()) {
            try (XMLSlideShow empty = new XMLSlideShow()) {
                empty.write(out);
            }
            return;
        }

        if (renderedSlides.size() == 1) {
            out.write(renderedSlides.get(0));
            renderedSlides.clear();
            return;
        }

        try {
            ByteArrayOutputStream merged = mergeAtOpcLevel(renderedSlides);
            out.write(merged.toByteArray());
        } finally {
            renderedSlides.clear();
        }
    }

    /**
     * Merges multiple single-slide PPTX byte arrays into one multi-slide PPTX
     * by copying slide parts directly at the OPC package level, avoiding the
     * POI-level createSlide()+importContent() which introduces spurious
     * notesSlide relationships on the presentation part that break Keynote.
     */
    private static ByteArrayOutputStream mergeAtOpcLevel(List<byte[]> slides) throws IOException {
        final String SLIDE_REL_TYPE =
            "http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide";
        final String SLIDE_CONTENT_TYPE =
            "application/vnd.openxmlformats-officedocument.presentationml.slide+xml";

        try (OPCPackage base = OPCPackage.open(new ByteArrayInputStream(slides.get(0)))) {
            PackagePart presentationPart = base.getPartsByName(
                    java.util.regex.Pattern.compile("/ppt/presentation\\.xml")).get(0);

            for (int i = 1; i < slides.size(); i++) {
                try (OPCPackage src = OPCPackage.open(new ByteArrayInputStream(slides.get(i)))) {
                    PackagePart srcPres = src.getPartsByName(
                            java.util.regex.Pattern.compile("/ppt/presentation\\.xml")).get(0);

                    // Find the slide part in the source
                    PackageRelationshipCollection srcSlideRels =
                            srcPres.getRelationshipsByType(SLIDE_REL_TYPE);
                    if (srcSlideRels.isEmpty()) continue;
                    PackageRelationship srcSlideRel = srcSlideRels.iterator().next();
                    PackagePart srcSlidePart = src.getPart(
                            PackagingURIHelper.createPartName(
                                    PackagingURIHelper.resolvePartUri(
                                            srcPres.getPartName().getURI(),
                                            srcSlideRel.getTargetURI())));

                    // Determine next available slide index in base
                    int nextIdx = base.getPartsByContentType(SLIDE_CONTENT_TYPE).size() + 1;
                    PackagePartName newSlideName = PackagingURIHelper.createPartName(
                            "/ppt/slides/slide" + nextIdx + ".xml");

                    // Copy the slide part bytes into base
                    PackagePart newSlidePart = base.createPart(newSlideName, SLIDE_CONTENT_TYPE);
                    try (InputStream in = srcSlidePart.getInputStream();
                         OutputStream os = newSlidePart.getOutputStream()) {
                        in.transferTo(os);
                    }

                    // Copy the slide's own relationships (layout, notes, images, etc.)
                    for (PackageRelationship rel : srcSlidePart.getRelationships()) {
                        if (rel.getTargetMode() == TargetMode.INTERNAL) {
                            PackagePartName targetName = PackagingURIHelper.createPartName(
                                    PackagingURIHelper.resolvePartUri(
                                            srcSlidePart.getPartName().getURI(),
                                            rel.getTargetURI()));
                            PackagePart targetPart = src.getPart(targetName);
                            if (targetPart != null && base.getPart(targetName) == null) {
                                PackagePart newTarget = base.createPart(targetName,
                                        targetPart.getContentType());
                                try (InputStream in = targetPart.getInputStream();
                                     OutputStream os = newTarget.getOutputStream()) {
                                    in.transferTo(os);
                                }
                            }
                            newSlidePart.addRelationship(targetName, TargetMode.INTERNAL,
                                    rel.getRelationshipType(), rel.getId());
                        }
                    }

                    // Register the new slide in the presentation's relationship list
                    PackageRelationship newSlideRel = presentationPart.addRelationship(
                            newSlideName, TargetMode.INTERNAL, SLIDE_REL_TYPE);

                    // Register the new slide in presentation.xml sldIdLst
                    addSlideIdToPresentation(presentationPart, newSlideRel.getId(),
                            256 + nextIdx);
                }
            }

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            base.save(buf);
            return buf;
        } catch (org.apache.poi.openxml4j.exceptions.InvalidFormatException e) {
            throw new IOException("Failed to merge slides at OPC level", e);
        }
    }

    private static void addSlideIdToPresentation(PackagePart presentationPart,
                                                  String relId, int slideId)
            throws IOException {
        final String P_NS = "http://schemas.openxmlformats.org/presentationml/2006/main";
        final String R_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
        try {
            javax.xml.parsers.DocumentBuilderFactory dbf =
                    javax.xml.parsers.DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            org.w3c.dom.Document doc;
            try (InputStream in = presentationPart.getInputStream()) {
                doc = dbf.newDocumentBuilder().parse(in);
            }
            org.w3c.dom.NodeList sldIdLst = doc.getElementsByTagNameNS(P_NS, "sldIdLst");
            if (sldIdLst.getLength() > 0) {
                org.w3c.dom.Element lst = (org.w3c.dom.Element) sldIdLst.item(0);
                org.w3c.dom.Element sldId = doc.createElementNS(P_NS, "p:sldId");
                sldId.setAttribute("id", String.valueOf(slideId));
                sldId.setAttributeNS(R_NS, "r:id", relId);
                lst.appendChild(sldId);
            }
            javax.xml.transform.Transformer t =
                    javax.xml.transform.TransformerFactory.newInstance().newTransformer();
            t.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
            try (OutputStream os = presentationPart.getOutputStream()) {
                t.transform(new javax.xml.transform.dom.DOMSource(doc),
                        new javax.xml.transform.stream.StreamResult(os));
            }
        } catch (Exception e) {
            throw new IOException("Failed to update sldIdLst", e);
        }
    }

    // -------------------------------------------------------------------------

    private void fillSlide(XSLFSlide slide, Map<String, String> values)
            throws LatexConversionException {
        for (XSLFShape shape : List.copyOf(slide.getShapes())) {
            if (!(shape instanceof XSLFTextShape textShape)) continue;
            String token = textShape.getText().strip();
            if (!values.containsKey(token)) continue;
            textShape.clearText();
            markdownRenderer.render(
                    slide,
                    textShape,
                    values.get(token),
                    textShape.getAnchor().getX(),
                    textShape.getAnchor().getY() + 100,
                    textShape.getAnchor().getWidth()
            );
        }
    }
}
