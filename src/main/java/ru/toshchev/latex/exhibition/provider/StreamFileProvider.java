package ru.toshchev.latex.exhibition.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * {@link PptxFileProvider} backed by pre-opened streams.
 *
 * <p>Use this when the template comes from an arbitrary source — S3, HTTP response,
 * an in-memory buffer, etc. — and the caller manages the stream lifecycle.</p>
 *
 * <pre>{@code
 * InputStream  s3Stream  = s3Client.getObject(bucket, key);
 * OutputStream outBuffer = new ByteArrayOutputStream();
 *
 * try (PptxFileProvider provider = new StreamFileProvider(s3Stream, outBuffer)) {
 *     LatexExhibition.fromProvider(provider).render(values);
 * }
 * }</pre>
 *
 * <p>{@link #close()} closes both streams.</p>
 */
public class StreamFileProvider implements PptxFileProvider {

    private final InputStream  templateStream;
    private final OutputStream outputStream;

    public StreamFileProvider(InputStream templateStream, OutputStream outputStream) {
        this.templateStream = templateStream;
        this.outputStream   = outputStream;
    }

    @Override
    public InputStream templateInput() {
        return templateStream;
    }

    @Override
    public OutputStream renderedOutput() {
        return outputStream;
    }

    @Override
    public void close() throws IOException {
        templateStream.close();
        outputStream.close();
    }
}
