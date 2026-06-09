package ru.toshchev.latex.exhibition.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * {@link PptxFileProvider} that loads the template from the classpath
 * (e.g. bundled inside a jar) and writes output to a local file.
 *
 * <pre>{@code
 * try (PptxFileProvider provider = new ClasspathFileProvider(
 *         "/templates/my-template.pptx",
 *         Path.of("output.pptx"))) {
 *     LatexExhibition.fromProvider(provider).render(values);
 * }
 * }</pre>
 */
public class ClasspathFileProvider implements PptxFileProvider {

    private final String classpathResource;
    private final Path   outputPath;

    private InputStream  templateStream;
    private OutputStream outputStream;

    /**
     * @param classpathResource absolute classpath path, e.g. {@code "/templates/slide.pptx"}
     * @param outputPath        local path for the rendered output
     */
    public ClasspathFileProvider(String classpathResource, Path outputPath) {
        this.classpathResource = classpathResource;
        this.outputPath        = outputPath;
    }

    public ClasspathFileProvider(String classpathResource, String outputPath) {
        this(classpathResource, Path.of(outputPath));
    }

    @Override
    public InputStream templateInput() throws IOException {
        templateStream = getClass().getResourceAsStream(classpathResource);
        if (templateStream == null) {
            throw new IOException("Classpath resource not found: " + classpathResource);
        }
        return templateStream;
    }

    @Override
    public OutputStream renderedOutput() throws IOException {
        outputStream = new java.io.FileOutputStream(outputPath.toFile());
        return outputStream;
    }

    @Override
    public void close() throws IOException {
        if (templateStream != null) templateStream.close();
        if (outputStream   != null) outputStream.close();
    }
}
