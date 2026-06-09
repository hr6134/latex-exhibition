package ru.toshchev.latex.formula;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import uk.ac.ed.ph.snuggletex.SnuggleEngine;
import uk.ac.ed.ph.snuggletex.SnuggleInput;
import uk.ac.ed.ph.snuggletex.SnuggleSession;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;

public class LatexToMathMl {

    private final SnuggleEngine engine = new SnuggleEngine();

    /**
     * Converts a LaTeX math expression to a MathML {@code <math>} XML string.
     * The input may optionally be surrounded by {@code $...$} or {@code \[...\]};
     * those delimiters are stripped before parsing.
     *
     * @param latex e.g. {@code "\\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}"}
     * @return MathML string, e.g. {@code <math xmlns="...">...</math>}
     * @throws LatexConversionException if parsing or serialisation fails
     */
    public String convert(String latex) throws LatexConversionException {
        String normalized = stripMathDelimiters(latex);
        SnuggleSession session = engine.createSession();
        try {
            boolean parsed = session.parseInput(new SnuggleInput("$" + normalized + "$"));
            if (!parsed) {
                throw new LatexConversionException(
                        "Failed to parse LaTeX: " + session.getErrors());
            }
            NodeList subtree = session.buildDOMSubtree();
            Node mathNode = findMathNode(subtree);
            if (mathNode == null) {
                throw new LatexConversionException(
                        "SnuggleTeX produced no <math> element for: " + latex);
            }
            return nodeToString(mathNode);
        } catch (java.io.IOException e) {
            throw new LatexConversionException("I/O error during LaTeX conversion", e);
        }
    }

    private static String stripMathDelimiters(String latex) {
        String s = latex.strip();
        if (s.startsWith("\\[") && s.endsWith("\\]")) {
            return s.substring(2, s.length() - 2).strip();
        }
        if (s.startsWith("$") && s.endsWith("$")) {
            return s.substring(1, s.length() - 1).strip();
        }
        return s;
    }

    private static Node findMathNode(NodeList subtree) {
        for (int i = 0; i < subtree.getLength(); i++) {
            Node node = subtree.item(i);
            String localName = node.getLocalName();
            if ("math".equals(localName)) {
                return node;
            }
            // depth-first search into children
            Node found = findMathNode(node.getChildNodes());
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private static String nodeToString(Node node) throws LatexConversionException {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new LatexConversionException("Failed to serialise MathML node", e);
        }
    }
}
