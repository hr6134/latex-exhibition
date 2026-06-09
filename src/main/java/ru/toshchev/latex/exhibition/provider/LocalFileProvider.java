package ru.toshchev.latex.exhibition.provider;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;

/**
 * {@link PptxFileProvider} backed by local filesystem paths.
 *
 * <pre>{@code
 * try (PptxFileProvider provider = new LocalFileProvider(
 *         Path.of("template.pptx"),
 *         Path.of("output.pptx"))) {
 *     LatexExhibition.fromProvider(provider).render(values);
 * }
 * }</pre>
 */
public class LocalFileProvider implements PptxFileProvider {

    private final Path templatePath;
    private final Path outputPath;

    private FileInputStream templateStream;
    private FileOutputStream outputStream;

    public LocalFileProvider(Path templatePath, Path outputPath) {
        this.templatePath = templatePath;
        this.outputPath   = outputPath;
    }

    public LocalFileProvider(String templatePath, String outputPath) {
        this(Path.of(templatePath), Path.of(outputPath));
    }

    @Override
    public InputStream templateInput() throws IOException {
        templateStream = new FileInputStream(templatePath.toFile());
        return templateStream;
    }

    @Override
    public OutputStream renderedOutput() throws IOException {
        outputStream = new FileOutputStream(outputPath.toFile());
        return outputStream;
    }

    @Override
    public void close() throws IOException {
        if (templateStream != null) templateStream.close();
        if (outputStream  != null) outputStream.close();
    }
}
