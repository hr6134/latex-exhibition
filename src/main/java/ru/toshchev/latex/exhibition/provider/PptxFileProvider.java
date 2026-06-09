package ru.toshchev.latex.exhibition.provider;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Provides the template {@link InputStream} and the output {@link OutputStream}
 * for {@link LatexExhibition}.
 *
 * <p>Implement this interface to support different storage backends — local files,
 * classpath resources, S3, HTTP, in-memory buffers, etc.</p>
 *
 * <p>Implementations must be {@link Closeable}: any opened resources (streams,
 * connections) should be released in {@link #close()}. Use the provider in a
 * try-with-resources block:</p>
 *
 * <pre>{@code
 * try (PptxFileProvider provider = new LocalFileProvider("template.pptx", "output.pptx")) {
 *     LatexExhibition.fromProvider(provider).render(values);
 * }
 * }</pre>
 */
public interface PptxFileProvider extends Closeable {

    /**
     * Returns an {@link InputStream} over the PPTX template.
     * Called exactly once by {@link LatexExhibition} before rendering begins.
     *
     * @throws IOException if the template cannot be opened
     */
    InputStream templateInput() throws IOException;

    /**
     * Returns an {@link OutputStream} to write the rendered PPTX into.
     * Called exactly once by {@link LatexExhibition} after rendering is complete.
     *
     * @throws IOException if the output destination cannot be opened
     */
    OutputStream renderedOutput() throws IOException;
}
