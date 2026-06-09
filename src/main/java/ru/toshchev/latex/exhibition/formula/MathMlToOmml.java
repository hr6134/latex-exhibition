package ru.toshchev.latex.exhibition.formula;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;

public class MathMlToOmml {

    private static final String XSL_RESOURCE = "/MML2OMML.XSL";

    private final Transformer transformer;

    public MathMlToOmml() throws LatexConversionException {
        try {
            InputStream xsl = MathMlToOmml.class.getResourceAsStream(XSL_RESOURCE);
            if (xsl == null) {
                throw new LatexConversionException(
                        "MML2OMML.XSL not found on classpath at " + XSL_RESOURCE);
            }
            TransformerFactory factory = TransformerFactory.newInstance(
                    "net.sf.saxon.TransformerFactoryImpl", null);
            transformer = factory.newTransformer(new StreamSource(xsl));
        } catch (TransformerException e) {
            throw new LatexConversionException("Failed to load MML2OMML.XSL transformer", e);
        }
    }

    /**
     * Transforms a MathML XML string into an OMML {@code <m:oMath>} XML string.
     *
     * @param mathMl MathML string produced by {@link LatexToMathMl}
     * @return OMML string, e.g. {@code <m:oMath xmlns:m="...">...</m:oMath>}
     * @throws LatexConversionException if the XSLT transformation fails
     */
    public String convert(String mathMl) throws LatexConversionException {
        try {
            StringWriter writer = new StringWriter();
            transformer.transform(
                    new StreamSource(new StringReader(mathMl)),
                    new StreamResult(writer));
            return writer.toString();
        } catch (TransformerException e) {
            throw new LatexConversionException("MathML → OMML transformation failed", e);
        }
    }
}
