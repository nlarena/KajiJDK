package java.util.logging;

/**
 * KajiLibrary's java.util.logging.FileHandler -- it writes the log to one or several files.
 *
 * <p>It is a {@link StreamHandler} over a file plus two things a file needs and a stream does not:
 * **a name that is computed** and **a size limit**.
 *
 * <p><strong>The pattern.</strong> The name is not given ready-made but as a template, because the
 * right name depends on things only known at start-up:
 *
 * <ul>
 * <li>`/` is the directory separator, always, whatever system it is written on.
 * <li>`%t` is the temporary directory and `%h` the user's.
 * <li>`%g` is the generation number: it tells the current file from the ones that already rotated.
 * <li>`%u` is a unique number: it tells apart two handlers that asked for the same name.
 * <li>`%%` is a `%`.
 * </ul>
 *
 * <p>If `count` is greater than one and the pattern has no `%g`, a `.%g` is appended. It has to be
 * that way: with no generation in the name, rotating would be writing over the same file forever.
 *
 * <p><strong>The rotation.</strong> When the file passes `limit` bytes it is closed, the ones
 * already there shift up one place --0 becomes 1, 1 becomes 2-- and a new one is opened at 0. The
 * oldest is lost. A `limit` of zero means no limit, which is the only reasonable reading of "do not
 * rotate". Note that the cut is checked **after** writing each record: a record is never split
 * between two files, and that is why the file can go a little over the limit.
 *
 * <p>And each rotated file is a **complete** document: on closing it the formatter's tail is written
 * and on opening the next one its head. With {@link XMLFormatter} that is the difference between
 * three files that can be parsed and three pieces that cannot.
 *
 * <p><strong>Two things this tree cannot give, and they have to be said.</strong>
 *
 * <ol>
 * <li>`%t` and `%h` come from `java.io.tmpdir` and `user.home`, and this VM **defines neither**. A
 *     pattern that uses them fails on construction with a {@link java.io.IOException} that says so
 *     -- which is better than inventing a path and writing the log somewhere nobody asked for. A
 *     program that sets those properties with `System.setProperty` makes them work.
 * <li>`%u` tells apart handlers **of this VM**. The JDK does it with a `.lck` file and a filesystem
 *     lock, so there it tells different processes apart too; here there are no file locks, so the
 *     guarantee is smaller and that is it. Two processes asking for the same pattern overwrite each
 *     other.
 * </ol>
 */
public class FileHandler extends StreamHandler {

    // The files some `FileHandler` of this VM has open, by their path. It is what makes `%u` tell
    // them apart: see note (2) above.
    private static final java.util.HashSet<String> OPEN_FILES = new java.util.HashSet<String>();

    private String pattern;
    private long limit;
    private int count;
    private boolean append;
    private java.io.File[] files;
    private CountingStream counter;
    private String reserved;

    /** The one that comes out of the configuration. */
    public FileHandler() throws java.io.IOException, SecurityException {
        this.configure();
        this.openFiles();
    }

    /**
     * @throws IllegalArgumentException if the pattern is empty
     * @throws NullPointerException if the pattern is `null`
     */
    public FileHandler(String pattern) throws java.io.IOException, SecurityException {
        this(pattern, 0L, 1, false);
    }

    public FileHandler(String pattern, boolean append)
            throws java.io.IOException, SecurityException {
        this(pattern, 0L, 1, append);
    }

    public FileHandler(String pattern, int limit, int count)
            throws java.io.IOException, SecurityException {
        this(pattern, (long) limit, count, false);
    }

    public FileHandler(String pattern, int limit, int count, boolean append)
            throws java.io.IOException, SecurityException {
        this(pattern, (long) limit, count, append);
    }

    /**
     * @throws IllegalArgumentException if `limit` is negative, if `count` is less than one, or if
     *         the pattern is empty
     */
    public FileHandler(String pattern, long limit, int count, boolean append)
            throws java.io.IOException, SecurityException {
        if (limit < 0 || count < 1 || pattern.length() < 1) {
            throw new IllegalArgumentException();
        }
        this.configure();
        this.pattern = pattern;
        this.limit = limit;
        this.count = count;
        this.append = append;
        this.openFiles();
    }

    // What this handler reads from the configuration. It is called ALWAYS, from the constructors
    // that take the pattern too: the level, the filter, the formatter and the encoding come from the
    // configuration even when the pattern comes from code.
    private void configure() {
        LogManager m = LogManager.getLogManager();
        String cname = this.getClass().getName();
        this.pattern = m.getStringProperty(cname + ".pattern", "%h/java%u.log");
        this.limit = m.getLongProperty(cname + ".limit", 0);
        if (this.limit < 0) {
            this.limit = 0;
        }
        this.count = m.getIntProperty(cname + ".count", 1);
        if (this.count <= 0) {
            this.count = 1;
        }
        this.append = m.getBooleanProperty(cname + ".append", false);
        this.setLevel(m.getLevelProperty(cname + ".level", Level.ALL));
        this.setFilter(m.getFilterProperty(cname + ".filter", null));
        this.setFormatter(m.getFormatterProperty(cname + ".formatter", new XMLFormatter()));
        try {
            this.setEncoding(m.getStringProperty(cname + ".encoding", null));
        } catch (Exception e) {
            try {
                this.setEncoding(null);
            } catch (Exception e2) {
                // It cannot happen: `null` is always accepted.
            }
        }
    }

    private void openFiles() throws java.io.IOException {
        int unique = 0;
        while (true) {
            java.io.File[] fs = new java.io.File[this.count];
            int i = 0;
            while (i < this.count) {
                fs[i] = this.generate(this.pattern, i, unique);
                i = i + 1;
            }
            String key = fs[0].getPath();
            synchronized (OPEN_FILES) {
                if (!OPEN_FILES.contains(key)) {
                    OPEN_FILES.add(key);
                    this.files = fs;
                    this.reserved = key;
                    break;
                }
            }
            unique = unique + 1;
        }
        try {
            if (!this.append) {
                // Without `append`, what was already there shifts up one place too: file 0 is always
                // this run's, and the previous one is left at 1 instead of being lost.
                this.runAll();
            }
            this.open(this.files[0], this.append);
        } catch (java.io.IOException e) {
            this.release();
            throw e;
        } catch (RuntimeException e) {
            this.release();
            throw e;
        }
    }

    private void release() {
        if (this.reserved == null) {
            return;
        }
        synchronized (OPEN_FILES) {
            OPEN_FILES.remove(this.reserved);
        }
        this.reserved = null;
    }

    // The name for that generation and that unique number. See the substitution list in the header.
    private java.io.File generate(String pattern, int generation, int unique)
            throws java.io.IOException {
        java.io.File dir = null;
        StringBuilder word = new StringBuilder();
        boolean sawG = false;
        boolean sawU = false;
        int i = 0;
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            i = i + 1;
            char c2 = 0;
            if (i < pattern.length()) {
                c2 = Character.toLowerCase(pattern.charAt(i));
            }
            if (c == '/') {
                dir = dir == null ? new java.io.File(word.toString())
                        : new java.io.File(dir, word.toString());
                word.setLength(0);
                continue;
            }
            if (c == '%') {
                if (c2 == 't') {
                    dir = new java.io.File(pathProperty("java.io.tmpdir", "user.home", "%t"));
                    i = i + 1;
                    word.setLength(0);
                    continue;
                }
                if (c2 == 'h') {
                    dir = new java.io.File(pathProperty("user.home", null, "%h"));
                    i = i + 1;
                    word.setLength(0);
                    continue;
                }
                if (c2 == 'g') {
                    word.append(generation);
                    sawG = true;
                    i = i + 1;
                    continue;
                }
                if (c2 == 'u') {
                    word.append(unique);
                    sawU = true;
                    i = i + 1;
                    continue;
                }
                if (c2 == '%') {
                    word.append('%');
                    i = i + 1;
                    continue;
                }
            }
            word.append(c);
        }
        if (this.count > 1 && !sawG) {
            word.append('.').append(generation);
        }
        if (unique > 0 && !sawU) {
            word.append('.').append(unique);
        }
        if (word.length() > 0) {
            dir = dir == null ? new java.io.File(word.toString())
                    : new java.io.File(dir, word.toString());
        }
        return dir;
    }

    // The property naming a directory, or a failure that says which one is missing. See note (1) in
    // the header: inventing a path would put the log somewhere nobody asked for.
    private static String pathProperty(String key, String alternative, String mark)
            throws java.io.IOException {
        String v = System.getProperty(key);
        if (v == null && alternative != null) {
            v = System.getProperty(alternative);
        }
        if (v == null) {
            throw new java.io.IOException("can't use " + mark + ": the system property \"" + key
                    + "\" is not defined");
        }
        return v;
    }

    private void open(java.io.File f, boolean append) throws java.io.IOException {
        long alreadyWritten = append ? f.length() : 0;
        java.io.OutputStream out = new java.io.BufferedOutputStream(
                new java.io.FileOutputStream(f.getPath(), append));
        this.counter = new CountingStream(out, alreadyWritten);
        this.setOutputStream(this.counter);
    }

    /**
     * It writes the record, and rotates if that took it past the limit.
     *
     * <p>It flushes after each one, like {@link ConsoleHandler} and for the same reason: the last
     * message before a crash is the one that matters, and it is exactly the one that would be left
     * in the buffer.
     */
    public synchronized void publish(LogRecord record) {
        if (!this.isLoggable(record)) {
            return;
        }
        super.publish(record);
        this.flush();
        if (this.limit > 0 && this.counter != null && this.counter.written() >= this.limit) {
            this.rotate();
        }
    }

    /**
     * It closes the current file and releases the name.
     *
     * <p>Releasing the name is what lets another handler take it again without advancing `%u`.
     */
    public synchronized void close() throws SecurityException {
        super.close();
        this.release();
    }

    // It closes the current one, shifts the ones that were there and opens a new one at generation
    // zero.
    //
    // The level is set to OFF for the duration: the opening can fail and reporting the failure emits
    // a message, and a message entering this same handler halfway through the rotation would find it
    // with no file again.
    private void rotate() {
        Level old = this.getLevel();
        this.setLevel(Level.OFF);
        super.close();
        this.runAll();
        try {
            this.open(this.files[0], false);
        } catch (java.io.IOException e) {
            this.reportError(null, e, ErrorManager.OPEN_FAILURE);
        }
        this.setLevel(old);
    }

    // It shifts each file up one place, from the second-to-last to the first. Back to front on
    // purpose: the other way round, each step would overwrite the one still to be moved.
    private void runAll() {
        int i = this.count - 2;
        while (i >= 0) {
            java.io.File f1 = this.files[i];
            java.io.File f2 = this.files[i + 1];
            if (f1.exists()) {
                if (f2.exists()) {
                    f2.delete();
                }
                moveFile(f1, f2);
            }
            i = i - 1;
        }
    }

    /**
     * It renames, and if the platform does not know how to rename, it copies and deletes.
     *
     * <p>The copy is not a whim: in this tree `File.renameTo` always returns `false` because there is
     * no rename intrinsic. The observable result is the same --the content ends up in the target file
     * and the source stops existing-- it costs the file's size, and it is what lets the rotation
     * genuinely work instead of not working.
     */
    private static void moveFile(java.io.File source, java.io.File target) {
        if (source.renameTo(target)) {
            return;
        }
        java.io.InputStream in = null;
        java.io.OutputStream out = null;
        try {
            in = new java.io.FileInputStream(source.getPath());
            out = new java.io.FileOutputStream(target.getPath(), false);
            byte[] buf = new byte[8192];
            int n = in.read(buf);
            while (n > 0) {
                out.write(buf, 0, n);
                n = in.read(buf);
            }
        } catch (java.io.IOException e) {
            // A rotation that fails cannot bring the program down: the old file is lost and the log
            // goes on in the new one, which is what matters.
            return;
        } finally {
            closeIt(in);
            closeIt(out);
        }
        source.delete();
    }

    private static void closeIt(java.io.Closeable c) {
        if (c == null) {
            return;
        }
        try {
            c.close();
        } catch (java.io.IOException e) {
            // Ver `moveFile`.
        }
    }

    /**
     * A stream that counts what goes through it.
     *
     * <p>It is needed because the limit is on **bytes written** and the handler writes characters:
     * with a variable-width encoding, counting characters would give a limit other than the one asked
     * for. Counting here, on the bytes' side, is counting what the file will take up.
     */
    private static class CountingStream extends java.io.OutputStream {

        private final java.io.OutputStream target;
        private long written;

        CountingStream(java.io.OutputStream target, long alreadyWrittenFiles) {
            this.target = target;
            this.written = alreadyWrittenFiles;
        }

        long written() {
            return this.written;
        }

        public void write(int b) throws java.io.IOException {
            this.target.write(b);
            this.written = this.written + 1;
        }

        public void write(byte[] b, int off, int len) throws java.io.IOException {
            this.target.write(b, off, len);
            this.written = this.written + len;
        }

        public void flush() throws java.io.IOException {
            this.target.flush();
        }

        public void close() throws java.io.IOException {
            this.target.close();
        }
    }
}
