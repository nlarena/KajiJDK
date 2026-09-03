package java.io;

// Same-package import works around the frozen javac's finder (finding #4).
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// KajiLibrary's java.io.BufferedReader — wraps another Reader and adds line-oriented input.
// (The JDK version also buffers in a char[] for throughput; ours reads through to the
// underlying Reader and focuses on the readLine() convenience.)
public class BufferedReader extends Reader {

    private Reader in;

    public BufferedReader(Reader in) {
        this.in = in;
    }

    // The buffer size is accepted for source compatibility; this reader reads through to the
    // underlying Reader rather than keeping its own char buffer.
    public BufferedReader(Reader in, int sz) {
        if (sz <= 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.in = in;
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        return this.in.read(cbuf, off, len);
    }

    public int read() throws IOException {
        return this.in.read();
    }

    // The next line, without its terminator ('\n', '\r', or '\r\n' are all accepted), or
    // null at end of stream.
    public String readLine() throws IOException {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        while (true) {
            int c = this.in.read();
            if (c < 0) {
                if (count == 0) {
                    return null;
                }
                return sb.toString();
            }
            if (c == '\n') {
                return sb.toString();
            }
            if (c != '\r') {
                sb.append((char) c);
                count = count + 1;
            }
        }
    }

    public long skip(long n) throws IOException {
        return this.in.skip(n);
    }

    public boolean ready() throws IOException {
        return this.in.ready();
    }

    public boolean markSupported() {
        return this.in.markSupported();
    }

    public void mark(int readAheadLimit) throws IOException {
        this.in.mark(readAheadLimit);
    }

    public void reset() throws IOException {
        this.in.reset();
    }

    /** The lines of this reader, read eagerly, as a stream. */
    /**
     * The lines of this reader, read eagerly, as a stream.
     *
     * <p>No declara `throws IOException` --el JDK tampoco-- porque un `Stream` se consume despues, y
     * una excepcion chequeada no puede salir de donde se consume. La salida del JDK es envolverla en
     * {@link UncheckedIOException}, y es la que se copia: el error no se pierde, cambia de forma.
     */
    public Stream<String> lines() {
        List<String> all = new ArrayList<String>();
        while (true) {
            String line;
            try {
                line = readLine();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (line == null) {
                break;
            }
            all.add(line);
        }
        String[] arr = all.toArray(new String[0]);
        return Stream.of(arr);
    }

    public void close() throws IOException {
        this.in.close();
    }
}
