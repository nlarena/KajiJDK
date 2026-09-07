package java.net;

import java.io.IOException;

/**
 * KajiLibrary's java.net.KajiFileHandler -- the `file:` handler, and the only one installed.
 *
 * <p>It is installed because it is the only protocol this VM can walk end to end: reading a file is
 * `java.io`, which works. `http:` and company need a socket, and this VM has none --two sessions
 * verified it by searching the VM's code-- so for those there is no handler and
 * `URL.openConnection()` says so instead of pretending.
 *
 * <p>That **one** exists changes the nature of `openConnection`: it stops being a method that could
 * only fail and becomes one that works for what it can and fails, by name, for what it cannot.
 */
final class KajiFileHandler extends URLStreamHandler {

    protected URLConnection openConnection(URL u) throws IOException {
        return new KajiFileConnection(u);
    }

    protected int getDefaultPort() {
        // `file:` has no port. -1 is what the contract asks for in that case.
        return -1;
    }
}

/**
 * KajiLibrary's java.net.KajiFileConnection -- a connection to a local file.
 *
 * <p>`connect()` opens nothing: it checks that the file **is there**, which is all "connecting" means
 * for a local file. Opening the stream only in `getInputStream` is what makes connecting and reading
 * two steps, as in any other protocol.
 */
final class KajiFileConnection extends URLConnection {

    private java.io.File file;

    KajiFileConnection(URL u) {
        super(u);
    }

    public void connect() throws IOException {
        if (this.connected) {
            return;
        }
        String path = this.getURL().getPath();
        if (path == null || path.length() == 0) {
            throw new IOException("the URL names no file: " + this.getURL());
        }
        // A well-formed `file:` carries the path with slashes and starting at `/`. On Windows the
        // real path is `C:/...`, so the leading slash is one too many: `/C:/x` does not exist and
        // `C:/x` does.
        if (path.length() > 2 && path.charAt(0) == '/' && path.charAt(2) == ':') {
            path = path.substring(1);
        }
        this.file = new java.io.File(path);
        if (!this.file.exists()) {
            throw new java.io.FileNotFoundException(path);
        }
        this.connected = true;
    }

    public java.io.InputStream getInputStream() throws IOException {
        this.connect();
        return new java.io.FileInputStream(this.file);
    }

    /** The file's size, or -1 if it does not fit an `int`. It is what `getContentLength` promises. */
    public int getContentLength() {
        long n = this.getContentLengthLong();
        return n > (long) Integer.MAX_VALUE ? -1 : (int) n;
    }

    public long getContentLengthLong() {
        try {
            this.connect();
        } catch (IOException e) {
            return -1L;
        }
        return this.file.length();
    }

    public long getLastModified() {
        try {
            this.connect();
        } catch (IOException e) {
            return 0L;
        }
        return this.file.lastModified();
    }
}
