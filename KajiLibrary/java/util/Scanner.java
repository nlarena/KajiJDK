package java.util;

// Same-package imports work around the frozen javac's finder (finding #4).
import java.util.Iterator;
import java.util.Locale;
import java.util.NoSuchElementException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

// A text reader that splits the input into meaningful pieces: words, numbers, lines.
//
// The whole idea fits in one line: **the input is split by a delimiter pattern**, and everything
// else -- `next`, `nextInt`, `hasNextDouble` -- is a variation on that. The default delimiter is
// "one or more whitespace characters", and it can be changed to any regular expression with
// `useDelimiter`. That is why Scanner is at once the shortcut for reading an integer from the
// console and a CSV splitter.
//
// The `hasNextX`/`nextX` pairs go together and not by chance: `hasNextX` looks at the next token
// **without consuming it**, `nextX` consumes it. That asymmetry is what makes a loop
// `while (sc.hasNextInt()) sum += sc.nextInt();` stop by itself when something that is not a number
// appears -- without losing it.
//
// The classic trap is stated here because it is not an implementation detail but a design one:
// `nextInt()` consumes the number and **leaves the line ending**, so the `nextLine()` that follows
// returns that line's empty remainder and not the next one. It is not a bug, it is what "token"
// means.
//
// ---- what is here and what is not ----------------------------------------------------------------
//
// This note used to say nine constructors were missing "for types the library does not have yet" --
// the three over `java.io.File`, the three over `java.nio.file.Path` and the three over
// `java.nio.channels.ReadableByteChannel`. All nine are declared below, each with its own javadoc
// saying how it reads its source. The types arrived and the constructors with them.
//
// ---- deliberate divergences ----------------------------------------------------------------------
//
// | | |
// |---|---|
// | an `InputStream` source is read **whole** on construction | decoding in chunks splits the multibyte characters that fall on the boundary, and there is no `InputStreamReader` to do it properly. Consequence: a Scanner over `System.in` waits for the end of the input instead of reading line by line |
// | the buffer is not compacted | what has been consumed is kept, so memory grows with the input. `match()` and `findInLine` lean on that, and for inputs of reasonable size it does not get in the way |
// | thousands separator `,` and decimal `.`, fixed | our `Locale` carries no numeric symbols, so `useLocale` is stored and does not change the parsing |
// | `tokens()` and `findAll()` are **eager** | they gather everything and return a Stream over the result, instead of producing on demand |
public final class Scanner implements Iterator<String>, Closeable {

    // "one or more whitespace characters". The JDK uses `\p{javaWhitespace}+`, which is almost the
    // same with more code points.
    private static final Pattern WHITESPACE = Pattern.compile("\\s+");

    // The six line endings the JDK recognises, with `\r\n` first so it does not get split in two.
    private static final Pattern LINE_ENDING =
            Pattern.compile("\r\n|[\n\r  ]");

    // The source there is still something to read from, or null if it has run out.
    private Readable source;

    // Everything read so far. `position` is what has been consumed; what is behind is kept because
    // `match()` and `findInLine` look backwards.
    private final StringBuilder buf = new StringBuilder();
    private int position;

    private boolean sourceClosed;
    private boolean closed;

    private Pattern delimiter = WHITESPACE;
    private Locale locale = Locale.getDefault();
    private int radix = 10;

    private MatchResult lastResult;
    private IOException lastException;

    // The bounds of the token `locateToken` located.
    private int tokenStart;
    private int tokenEnd;

    // ---- construction ---------------------------------------------------------------------------

    public Scanner(Readable source) {
        if (source == null) {
            throw new NullPointerException();
        }
        this.source = source;
    }

    // The simplest source: the text is already there whole.
    public Scanner(String source) {
        if (source == null) {
            throw new NullPointerException();
        }
        this.buf.append(source);
        this.sourceClosed = true;
    }

    /**
     * A `Scanner` over a file's content.
     *
     * <p>It leans on `FileInputStream`, which reads the whole file on opening. That one's note
     * applies: what is walked is the **snapshot** from the moment the `Scanner` was constructed, not
     * a file that goes on being read -- a later change is not seen.
     *
     * @throws java.io.FileNotFoundException if it does not exist, is a directory, or cannot be
     *         read
     */
    public Scanner(java.io.File source) throws java.io.FileNotFoundException {
        this(source, Charset.defaultCharset());
    }

    public Scanner(java.io.File source, String charsetName) throws java.io.FileNotFoundException {
        this(source, Charset.forName(charsetName));
    }

    public Scanner(java.io.File source, Charset charset) throws java.io.FileNotFoundException {
        this(new java.io.FileInputStream(source), charset);
    }

    /**
     * The same, by `Path`.
     *
     * <p>A `Path` in this library is a path and nothing more, so this is exactly the `File` form
     * going through `toString()`. Both exist because the JDK has both, and because whoever already
     * has a `Path` should not have to convert it by hand.
     */
    public Scanner(java.nio.file.Path source) throws java.io.FileNotFoundException {
        this(source, Charset.defaultCharset());
    }

    public Scanner(java.nio.file.Path source, String charsetName)
            throws java.io.FileNotFoundException {
        this(source, Charset.forName(charsetName));
    }

    public Scanner(java.nio.file.Path source, Charset charset)
            throws java.io.FileNotFoundException {
        this(source == null ? null : new java.io.File(source.toString()), charset);
    }

    public Scanner(java.nio.channels.ReadableByteChannel source) {
        this(source, Charset.defaultCharset());
    }

    public Scanner(java.nio.channels.ReadableByteChannel source, String charsetName) {
        this(source, Charset.forName(charsetName));
    }

    /**
     * A `Scanner` over a channel.
     *
     * <p>The source is supplied by the caller, already open, so the library does not have to know
     * how to touch the filesystem for this form.
     *
     * <p>Like the `InputStream` ones, it reads the channel **whole** in one go and then decodes.
     * Mind the `0`: on a channel it is a legitimate result --the buffer was full, or a non-blocking
     * one had nothing ready-- and it is **not** end of stream, which is `-1`. Treating it as the end
     * would cut the read short; that is why the loop only ends on the negative.
     */
    public Scanner(java.nio.channels.ReadableByteChannel source, Charset charset) {
        if (source == null || charset == null) {
            throw new NullPointerException();
        }
        this.sourceClosed = true;
        byte[] whole = new byte[0];
        int used = 0;
        java.nio.ByteBuffer chunk = java.nio.ByteBuffer.allocate(8192);
        try {
            int n = source.read(chunk);
            while (n >= 0) {
                if (n > 0) {
                    if (used + n > whole.length) {
                        int updated = whole.length * 2;
                        if (updated < used + n) {
                            updated = used + n;
                        }
                        byte[] bigger = new byte[updated];
                        System.arraycopy(whole, 0, bigger, 0, used);
                        whole = bigger;
                    }
                    chunk.flip();
                    chunk.get(whole, used, n);
                    used = used + n;
                    chunk.clear();
                }
                n = source.read(chunk);
            }
        } catch (java.io.IOException e) {
            // What has been read so far is what there is: a `Scanner` cannot propagate a checked
            // exception from its constructor without declaring it, and the JDK does not declare one
            // for this form either.
        }
        this.buf.append(new String(whole, 0, used, charset));
    }

    public Scanner(InputStream source) {
        this(source, Charset.defaultCharset());
    }

    public Scanner(InputStream source, String charsetName) {
        this(source, Charset.forName(charsetName));
    }

    // The stream is read **whole** and decoded in one go. See the divergence table.
    public Scanner(InputStream source, Charset charset) {
        if (source == null || charset == null) {
            throw new NullPointerException();
        }
        this.sourceClosed = true;
        byte[] whole = new byte[0];
        int used = 0;
        byte[] chunk = new byte[8192];
        try {
            int n = source.read(chunk, 0, chunk.length);
            while (n > 0) {
                if (used + n > whole.length) {
                    int updated = whole.length * 2;
                    if (updated < used + n) {
                        updated = used + n;
                    }
                    byte[] bigger = new byte[updated];
                    System.arraycopy(whole, 0, bigger, 0, used);
                    whole = bigger;
                }
                System.arraycopy(chunk, 0, whole, used, n);
                used = used + n;
                n = source.read(chunk, 0, chunk.length);
            }
        } catch (java.io.IOException e) {
            // The read is cut short and what has been read is what there is: a `Scanner` **does not
            // throw** on I/O failures, it notes them and reports them through `ioException()`. That
            // is its contract.
            this.lastException = e;
        } catch (RuntimeException e) {
            // The same for an unchecked failure of the source's.
        }
        this.buf.append(new String(whole, 0, used, charset));
    }

    // ---- the buffer -------------------------------------------------------------------------------

    // It brings more characters from the source. It returns whether it brought any.
    private boolean readMore() {
        if (this.sourceClosed || this.source == null) {
            return false;
        }
        CharBuffer cb = CharBuffer.allocate(1024);
        int n;
        try {
            n = this.source.read(cb);
        } catch (IOException e) {
            this.lastException = e;
            this.sourceClosed = true;
            return false;
        }
        if (n < 0) {
            this.sourceClosed = true;
            return false;
        }
        if (n == 0) {
            // Zero is not end of input: the buffer had no room. With a freshly created one it cannot
            // happen, so it is taken as there being nothing more for now.
            return false;
        }
        cb.flip();
        char[] readCount = new char[n];
        cb.get(readCount, 0, n);
        this.buf.append(readCount, 0, n);
        return true;
    }

    private void checkOpen() {
        if (this.closed) {
            throw new IllegalStateException("Scanner closed");
        }
    }

    // It locates the next token without consuming it. It leaves the bounds in tokenStart/tokenEnd.
    //
    // The loop is there because of the incremental reading: when what is in the buffer is not enough
    // to decide --the delimiters reach the end, or the token did not close-- more is brought and it
    // starts again.
    private boolean locateToken() {
        while (true) {
            int p = this.position;
            Matcher m = this.delimiter.matcher(this.buf);
            m.region(p, this.buf.length());
            if (m.lookingAt()) {
                p = m.end();
                if (p == this.buf.length() && !this.sourceClosed && this.readMore()) {
                    continue;
                }
            }
            if (p >= this.buf.length()) {
                if (!this.sourceClosed && this.readMore()) {
                    continue;
                }
                return false; // only delimiters through to the end
            }
            Matcher d = this.delimiter.matcher(this.buf);
            d.region(p, this.buf.length());
            int end;
            if (d.find()) {
                end = d.start();
            } else {
                if (!this.sourceClosed && this.readMore()) {
                    continue;
                }
                end = this.buf.length();
            }
            this.tokenStart = p;
            this.tokenEnd = end;
            return true;
        }
    }

    // The next token without consuming it, or null if there is none.
    private String peekToken() {
        this.checkOpen();
        if (!this.locateToken()) {
            return null;
        }
        return this.buf.substring(this.tokenStart, this.tokenEnd);
    }

    // It leaves `lastResult` pointing at the given range, so `match()` has something to return.
    //
    // It is built by hand and not with a Matcher, on purpose. The natural thing would be to match
    // `[\s\S]*` over the region, but our engine **rejects** a negated predefined class inside
    // another class (`\S` inside `[...]`), and says so in as many words in `Node.addClassEscape`. An
    // already located token needs no engine either: its bounds are already known.
    private void recordMatch(int from, int to) {
        this.lastResult = new ScanMatch(this.buf.substring(from, to), from, to);
    }

    // ---- tokens -------------------------------------------------------------------------------------

    public boolean hasNext() {
        return this.peekToken() != null;
    }

    public String next() {
        this.checkOpen();
        if (!this.locateToken()) {
            throw new NoSuchElementException();
        }
        String t = this.buf.substring(this.tokenStart, this.tokenEnd);
        this.recordMatch(this.tokenStart, this.tokenEnd);
        this.position = this.tokenEnd;
        return t;
    }

    public boolean hasNext(String pattern) {
        return this.hasNext(Pattern.compile(pattern));
    }

    // The next token, but only if it matches the pattern **whole**.
    public boolean hasNext(Pattern pattern) {
        String t = this.peekToken();
        if (t == null) {
            return false;
        }
        return pattern.matcher(t).matches();
    }

    public String next(String pattern) {
        return this.next(Pattern.compile(pattern));
    }

    public String next(Pattern pattern) {
        String t = this.peekToken();
        if (t == null) {
            throw new NoSuchElementException();
        }
        if (!pattern.matcher(t).matches()) {
            throw new InputMismatchException();
        }
        return this.next();
    }

    // Iterator declares it and Scanner refuses it: a Scanner has nothing to take anything out of.
    public void remove() {
        throw new UnsupportedOperationException();
    }

    // ---- lines  ----------------------------------------------------------------------------------

    public boolean hasNextLine() {
        this.checkOpen();
        return this.findLineEnding() != null;
    }

    // It returns { endStart, endEnd } of the next line break, or { -1, -1 } if the input ends with no
    // break but with content. null if nothing is left.
    private int[] findLineEnding() {
        while (true) {
            Matcher m = LINE_ENDING.matcher(this.buf);
            m.region(this.position, this.buf.length());
            if (m.find()) {
                int[] out = new int[2];
                out[0] = m.start();
                out[1] = m.end();
                return out;
            }
            if (!this.sourceClosed && this.readMore()) {
                continue;
            }
            if (this.position < this.buf.length()) {
                int[] out = new int[2];
                out[0] = -1;
                out[1] = -1;
                return out;
            }
            return null;
        }
    }

    // The rest of the current line, **without** the break -- which IS consumed.
    //
    // It is what makes `nextInt()` followed by `nextLine()` return empty: the integer left the break
    // unconsumed, and this call takes it.
    public String nextLine() {
        this.checkOpen();
        int[] end = this.findLineEnding();
        if (end == null) {
            throw new NoSuchElementException("No line found");
        }
        String line;
        if (end[0] < 0) {
            line = this.buf.substring(this.position, this.buf.length());
            this.recordMatch(this.position, this.buf.length());
            this.position = this.buf.length();
        } else {
            line = this.buf.substring(this.position, end[0]);
            this.recordMatch(this.position, end[1]);
            this.position = end[1];
        }
        return line;
    }

    // ---- numbers and booleans ------------------------------------------------------------------------

    // It strips the thousands separator and normalises the sign, so java.lang's parsers accept it.
    private String cleanNumber(String s) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c != ',') {
                sb.append(c);
            }
            i = i + 1;
        }
        return sb.toString();
    }

    // What the eight numeric `hasNextX` have in common: the token is peeked at, a conversion is
    // attempted, and **nothing** is consumed. `kind` chooses the parser.
    private boolean canParse(int kind, int radix) {
        String t = this.peekToken();
        if (t == null) {
            return false;
        }
        return this.convertible(t, kind, radix);
    }

    private static final int T_BYTE = 0;
    private static final int T_SHORT = 1;
    private static final int T_INT = 2;
    private static final int T_LONG = 3;
    private static final int T_FLOAT = 4;
    private static final int T_DOUBLE = 5;
    private static final int T_BIGINT = 6;
    private static final int T_BIGDEC = 7;

    private boolean convertible(String t, int kind, int radix) {
        String s = this.cleanNumber(t);
        try {
            if (kind == T_BYTE) {
                Byte.parseByte(s, radix);
            } else if (kind == T_SHORT) {
                Short.parseShort(s, radix);
            } else if (kind == T_INT) {
                Integer.parseInt(s, radix);
            } else if (kind == T_LONG) {
                Long.parseLong(s, radix);
            } else if (kind == T_FLOAT) {
                Float.parseFloat(s);
            } else if (kind == T_DOUBLE) {
                Double.parseDouble(s);
            } else if (kind == T_BIGINT) {
                new BigInteger(s, radix);
            } else {
                new BigDecimal(s);
            }
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    // And what the `nextX` have in common: it is required to convert **before** consuming, so that a
    // token that is not a number is still there when it is asked for another way.
    private String numericToken(int kind, int radix) {
        String t = this.peekToken();
        if (t == null) {
            throw new NoSuchElementException();
        }
        if (!this.convertible(t, kind, radix)) {
            throw new InputMismatchException();
        }
        this.next();
        return this.cleanNumber(t);
    }

    public boolean hasNextByte() {
        return this.hasNextByte(this.radix);
    }

    public boolean hasNextByte(int radix) {
        return this.canParse(T_BYTE, radix);
    }

    public byte nextByte() {
        return this.nextByte(this.radix);
    }

    public byte nextByte(int radix) {
        return Byte.parseByte(this.numericToken(T_BYTE, radix), radix);
    }

    public boolean hasNextShort() {
        return this.hasNextShort(this.radix);
    }

    public boolean hasNextShort(int radix) {
        return this.canParse(T_SHORT, radix);
    }

    public short nextShort() {
        return this.nextShort(this.radix);
    }

    public short nextShort(int radix) {
        return Short.parseShort(this.numericToken(T_SHORT, radix), radix);
    }

    public boolean hasNextInt() {
        return this.hasNextInt(this.radix);
    }

    public boolean hasNextInt(int radix) {
        return this.canParse(T_INT, radix);
    }

    public int nextInt() {
        return this.nextInt(this.radix);
    }

    public int nextInt(int radix) {
        return Integer.parseInt(this.numericToken(T_INT, radix), radix);
    }

    public boolean hasNextLong() {
        return this.hasNextLong(this.radix);
    }

    public boolean hasNextLong(int radix) {
        return this.canParse(T_LONG, radix);
    }

    public long nextLong() {
        return this.nextLong(this.radix);
    }

    public long nextLong(int radix) {
        return Long.parseLong(this.numericToken(T_LONG, radix), radix);
    }

    public boolean hasNextFloat() {
        return this.canParse(T_FLOAT, 10);
    }

    public float nextFloat() {
        return Float.parseFloat(this.numericToken(T_FLOAT, 10));
    }

    public boolean hasNextDouble() {
        return this.canParse(T_DOUBLE, 10);
    }

    public double nextDouble() {
        return Double.parseDouble(this.numericToken(T_DOUBLE, 10));
    }

    public boolean hasNextBigInteger() {
        return this.hasNextBigInteger(this.radix);
    }

    public boolean hasNextBigInteger(int radix) {
        return this.canParse(T_BIGINT, radix);
    }

    public BigInteger nextBigInteger() {
        return this.nextBigInteger(this.radix);
    }

    public BigInteger nextBigInteger(int radix) {
        return new BigInteger(this.numericToken(T_BIGINT, radix), radix);
    }

    public boolean hasNextBigDecimal() {
        return this.canParse(T_BIGDEC, 10);
    }

    public BigDecimal nextBigDecimal() {
        return new BigDecimal(this.numericToken(T_BIGDEC, 10));
    }

    public boolean hasNextBoolean() {
        String t = this.peekToken();
        if (t == null) {
            return false;
        }
        return t.equalsIgnoreCase("true") || t.equalsIgnoreCase("false");
    }

    public boolean nextBoolean() {
        String t = this.peekToken();
        if (t == null) {
            throw new NoSuchElementException();
        }
        if (!t.equalsIgnoreCase("true") && !t.equalsIgnoreCase("false")) {
            throw new InputMismatchException();
        }
        this.next();
        return t.equalsIgnoreCase("true");
    }

    // ---- direct searching, without going through the tokens ------------------------------------------

    public String findInLine(String pattern) {
        return this.findInLine(Pattern.compile(pattern));
    }

    // The pattern, searched for **within the current line**. It does not cross the line break, which
    // is what distinguishes it from `findWithinHorizon`.
    public String findInLine(Pattern pattern) {
        this.checkOpen();
        int[] end = this.findLineEnding();
        int bound;
        if (end == null) {
            bound = this.buf.length();
        } else if (end[0] < 0) {
            bound = this.buf.length();
        } else {
            bound = end[0];
        }
        Matcher m = pattern.matcher(this.buf);
        m.region(this.position, bound);
        if (!m.find()) {
            return null;
        }
        this.lastResult = m.toMatchResult();
        this.position = m.end();
        return m.group();
    }

    public String findWithinHorizon(String pattern, int horizon) {
        return this.findWithinHorizon(Pattern.compile(pattern), horizon);
    }

    // The pattern, searched for in the next `horizon` characters. With `horizon` at 0 there is no limit.
    public String findWithinHorizon(Pattern pattern, int horizon) {
        this.checkOpen();
        if (horizon < 0) {
            throw new IllegalArgumentException("horizon < 0");
        }
        while (!this.sourceClosed) {
            if (horizon > 0 && this.buf.length() - this.position >= horizon) {
                break;
            }
            if (!this.readMore()) {
                break;
            }
        }
        int bound = this.buf.length();
        if (horizon > 0 && this.position + horizon < bound) {
            bound = this.position + horizon;
        }
        Matcher m = pattern.matcher(this.buf);
        m.region(this.position, bound);
        if (!m.find()) {
            return null;
        }
        this.lastResult = m.toMatchResult();
        this.position = m.end();
        return m.group();
    }

    public Scanner skip(String pattern) {
        return this.skip(Pattern.compile(pattern));
    }

    // It skips whatever matches **from the current position**, with no delimiters in between. Unlike
    // `find*`, the pattern has to start right here.
    public Scanner skip(Pattern pattern) {
        this.checkOpen();
        while (!this.sourceClosed && this.readMore()) {
            // everything possible is brought in: `skip` has no horizon
        }
        Matcher m = pattern.matcher(this.buf);
        m.region(this.position, this.buf.length());
        if (!m.lookingAt()) {
            throw new NoSuchElementException();
        }
        this.lastResult = m.toMatchResult();
        this.position = m.end();
        return this;
    }

    // ---- streams -----------------------------------------------------------------------------------

    public Stream<String> tokens() {
        this.checkOpen();
        ArrayList<String> out = new ArrayList<String>();
        while (this.hasNext()) {
            out.add(this.next());
        }
        String[] a = new String[out.size()];
        int i = 0;
        while (i < a.length) {
            a[i] = out.get(i);
            i = i + 1;
        }
        return Stream.of(a);
    }

    public Stream<String> findAll(String pattern) {
        return this.findAll(Pattern.compile(pattern));
    }

    public Stream<String> findAll(Pattern pattern) {
        this.checkOpen();
        while (!this.sourceClosed && this.readMore()) {
            // the same: everything is gathered before searching
        }
        ArrayList<String> out = new ArrayList<String>();
        Matcher m = pattern.matcher(this.buf);
        m.region(this.position, this.buf.length());
        while (m.find()) {
            out.add(m.group());
            this.lastResult = m.toMatchResult();
            this.position = m.end();
        }
        String[] a = new String[out.size()];
        int i = 0;
        while (i < a.length) {
            a[i] = out.get(i);
            i = i + 1;
        }
        return Stream.of(a);
    }

    // ---- configuration --------------------------------------------------------------------------

    public Pattern delimiter() {
        return this.delimiter;
    }

    public Scanner useDelimiter(Pattern pattern) {
        this.delimiter = pattern;
        return this;
    }

    public Scanner useDelimiter(String pattern) {
        return this.useDelimiter(Pattern.compile(pattern));
    }

    public Locale locale() {
        return this.locale;
    }

    // It is stored and does not change the parsing: see the divergence table.
    public Scanner useLocale(Locale locale) {
        if (locale == null) {
            throw new NullPointerException();
        }
        this.locale = locale;
        return this;
    }

    public int radix() {
        return this.radix;
    }

    public Scanner useRadix(int radix) {
        if (radix < 2 || radix > 36) {
            throw new IllegalArgumentException("radix:" + radix);
        }
        this.radix = radix;
        return this;
    }

    // It goes back to the factory configuration -- delimiter, locale and radix -- **without**
    // touching the position. Resetting the state is not rewinding the input.
    public Scanner reset() {
        this.delimiter = WHITESPACE;
        this.locale = Locale.getDefault();
        this.radix = 10;
        return this;
    }

    // ---- state  --------------------------------------------------------------------------------------

    // The result of the last operation that matched something.
    public MatchResult match() {
        if (this.lastResult == null) {
            throw new IllegalStateException("No match result available");
        }
        return this.lastResult;
    }

    // The last IOException the source threw, or null.
    //
    // It exists because Scanner's methods do **not** declare IOException: it swallows it and leaves
    // it available here. Whoever reads from a file has to ask, or they will not learn that the read
    // was cut short by an error rather than by end of input.
    public IOException ioException() {
        return this.lastException;
    }

    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        this.sourceClosed = true;
        if (this.source instanceof Closeable) {
            try {
                ((Closeable) this.source).close();
            } catch (java.io.IOException e) {
                // `Scanner.close()` declares no `throws` in the JDK: the failure is noted and
                // reported through `ioException()`, like the read one. Closing is the last thing that
                // happens, and forcing a catch there would be asking the caller to handle an error
                // that can no longer change anything.
                this.lastException = e;
            }
        }
        this.source = null;
    }

    public String toString() {
        return "java.util.Scanner[delimiters=" + this.delimiter.pattern()
                + "][position=" + this.position
                + "][match valid=" + (this.lastResult != null)
                + "][closed=" + this.closed
                + "][radix=" + this.radix
                + "][locale=" + this.locale
                + "]";
    }
}

// The MatchResult of a Scanner operation that did not go through a Matcher -- `next()` and
// `nextLine()`, which locate their text by counting delimiters and not by matching.
// Package-private.
//
// A single group, zero: there are no subgroups to report because there was no pattern with
// parentheses.
final class ScanMatch implements MatchResult {

    private final String text;
    private final int from;
    private final int to;

    ScanMatch(String text, int from, int to) {
        this.text = text;
        this.from = from;
        this.to = to;
    }

    private void checkArg(int group) {
        if (group != 0) {
            throw new IndexOutOfBoundsException("No group " + group);
        }
    }

    public int start() {
        return this.from;
    }

    public int start(int group) {
        this.checkArg(group);
        return this.from;
    }

    public int end() {
        return this.to;
    }

    public int end(int group) {
        this.checkArg(group);
        return this.to;
    }

    public String group() {
        return this.text;
    }

    public String group(int group) {
        this.checkArg(group);
        return this.text;
    }

    public int groupCount() {
        return 0;
    }
}
