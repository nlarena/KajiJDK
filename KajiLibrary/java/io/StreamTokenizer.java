package java.io;

import java.util.Arrays;

// KajiLibrary's java.io.StreamTokenizer -- it breaks a stream of characters into tokens.
//
// It is a configurable and **self-contained** lexer: it depends on nothing from the system, it only
// reads characters and decides where each one ends. That is why it can be written whole and without
// concessions.
//
// The configuration lives in a table of 256 entries, one per character, with the flags for what
// that character is: space, digit, letter, quote, comment. `wordChars`, `whitespaceChars`,
// `quoteChar` and company do nothing but turn bits on there. That the table is 256 and not 65536
// comes from the original contract --it is from 1995-- and it has a consequence worth knowing:
// **every character above 255 is treated as a letter**, without exception. An ideograph is part of
// a word and cannot be configured not to be.
//
// The result of each `nextToken()` does not come back as a return value but spread over three
// public fields --`ttype` says what came out, `sval` the text if it was a word or a string, `nval`
// the number if it was a number. It is an interface from another era and it has to be honoured:
// they are public fields, anyone may read them and write them.
public class StreamTokenizer {

    // Where it reads from. One of the two is null; see `read()`.
    private Reader reader = null;
    private InputStream input = null;

    private char[] buf = new char[20];

    /**
     * The next character, read already but not yet consumed.
     *
     * <p>It is `NEED_CHAR` when there is none stored and one has to be fetched, and `SKIP_LF` when
     * what has to be done is read one and discard it if it turns out to be a `\n` -- which is how
     * the second half of a `\r\n` is handled without counting the line twice.
     */
    private int peekc = NEED_CHAR;

    private static final int NEED_CHAR = Integer.MAX_VALUE;
    private static final int SKIP_LF = Integer.MAX_VALUE - 1;

    private boolean pushedBack;
    private boolean forceLower;

    /** The current line. It starts at 1, not 0: it is for human error messages. */
    private int LINENO = 1;

    private boolean eolIsSignificantP = false;
    private boolean slashSlashCommentsP = false;
    private boolean slashStarCommentsP = false;

    private final byte[] ctype = new byte[256];

    private static final byte CT_WHITESPACE = 1;
    private static final byte CT_DIGIT = 2;
    private static final byte CT_ALPHA = 4;
    private static final byte CT_QUOTE = 8;
    private static final byte CT_COMMENT = 16;

    /**
     * What the last token was: one of the `TT_*`, or the character's code if it was a lone
     * character (a `+` comes out as `43`).
     */
    public int ttype = TT_NOTHING;

    /** No token has been read yet. */
    private static final int TT_NOTHING = -4;

    /** The stream has run out. */
    public static final int TT_EOF = -1;

    /** End of line, only if `eolIsSignificant(true)` was asked for. */
    public static final int TT_EOL = '\n';

    /** The token was a number; it is in `nval`. */
    public static final int TT_NUMBER = -2;

    /** The token was a word; it is in `sval`. */
    public static final int TT_WORD = -3;

    /** The last token's text, if it was a word or a quoted string. */
    public String sval;

    /** The last token's value, if it was a number. */
    public double nval;

    // The default syntax: ASCII letters and the high range are word, everything below the space is
    // whitespace, `/` opens a comment, and the two quote characters delimit strings.
    private StreamTokenizer() {
        this.wordChars('a', 'z');
        this.wordChars('A', 'Z');
        this.wordChars(128 + 32, 255);
        this.whitespaceChars(0, ' ');
        this.commentChar('/');
        this.quoteChar('"');
        this.quoteChar('\'');
        this.parseNumbers();
    }

    /**
     * It reads from a stream of bytes.
     *
     * @deprecated It treats each byte as a character, that is, it only works with one-byte
     *     encodings. The right thing is to wrap it: `new StreamTokenizer(new InputStreamReader(is,
     *     cs))`.
     */
    @Deprecated
    public StreamTokenizer(InputStream is) {
        this();
        if (is == null) {
            throw new NullPointerException();
        }
        this.input = is;
    }

    /** It reads from a stream of characters. This is the one to use. */
    public StreamTokenizer(Reader r) {
        this();
        if (r == null) {
            throw new NullPointerException();
        }
        this.reader = r;
    }

    // The class's only read. The two fields are mutually exclusive and one of the two is always
    // set: the constructors do not allow building a tokenizer with no source.
    private int read() throws IOException {
        if (this.reader != null) {
            return this.reader.read();
        }
        if (this.input != null) {
            return this.input.read();
        }
        throw new IllegalStateException();
    }

    /** It blanks the table: **no** character has any special meaning. */
    public void resetSyntax() {
        for (int i = this.ctype.length; --i >= 0; ) {
            this.ctype[i] = 0;
        }
    }

    /** The characters from `low` to `hi` are part of a word. */
    public void wordChars(int low, int hi) {
        int l = low;
        int h = hi;
        if (l < 0) {
            l = 0;
        }
        if (h >= this.ctype.length) {
            h = this.ctype.length - 1;
        }
        while (l <= h) {
            this.ctype[l] = (byte) (this.ctype[l] | CT_ALPHA);
            l = l + 1;
        }
    }

    /** The characters from `low` to `hi` separate tokens and form part of none. */
    public void whitespaceChars(int low, int hi) {
        int l = low;
        int h = hi;
        if (l < 0) {
            l = 0;
        }
        if (h >= this.ctype.length) {
            h = this.ctype.length - 1;
        }
        while (l <= h) {
            this.ctype[l] = CT_WHITESPACE;
            l = l + 1;
        }
    }

    /** The characters from `low` to `hi` have no special meaning: they come out on their own. */
    public void ordinaryChars(int low, int hi) {
        int l = low;
        int h = hi;
        if (l < 0) {
            l = 0;
        }
        if (h >= this.ctype.length) {
            h = this.ctype.length - 1;
        }
        while (l <= h) {
            this.ctype[l] = 0;
            l = l + 1;
        }
    }

    /** The character `ch` has no special meaning: it comes out on its own. */
    public void ordinaryChar(int ch) {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = 0;
        }
    }

    /** From `ch` to the end of the line is a comment. */
    public void commentChar(int ch) {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = CT_COMMENT;
        }
    }

    /**
     * `ch` opens and closes a string.
     *
     * <p>Inside the string C's escapes are interpreted --`\n`, `\t`, `\\`, and the octals `\0` to
     * `\377`-- and the token comes out with `ttype` equal to the quote character and the text
     * already unescaped in `sval`.
     */
    public void quoteChar(int ch) {
        if (ch >= 0 && ch < this.ctype.length) {
            this.ctype[ch] = CT_QUOTE;
        }
    }

    /**
     * The digits, the dot and the minus form numbers.
     *
     * <p>The number is built in a `double` and **there are no integers**: `1` comes out as `1.0`.
     * Nor is there exponential notation -- `1e5` is split into the number `1.0` and the word `e5`.
     */
    public void parseNumbers() {
        for (int i = '0'; i <= '9'; i++) {
            this.ctype[i] = (byte) (this.ctype[i] | CT_DIGIT);
        }
        this.ctype['.'] = (byte) (this.ctype['.'] | CT_DIGIT);
        this.ctype['-'] = (byte) (this.ctype['-'] | CT_DIGIT);
    }

    /** Whether the line endings come out as a `TT_EOL` token instead of counting as
     * whitespace. */
    public void eolIsSignificant(boolean flag) {
        this.eolIsSignificantP = flag;
    }

    /** Whether `/* ... *&#47;` is a comment. */
    public void slashStarComments(boolean flag) {
        this.slashStarCommentsP = flag;
    }

    /** Whether `//` opens a comment up to the end of the line. */
    public void slashSlashComments(boolean flag) {
        this.slashSlashCommentsP = flag;
    }

    /** Whether the words are lowercased before being left in `sval`. */
    public void lowerCaseMode(boolean fl) {
        this.forceLower = fl;
    }

    /**
     * It reads the next token and leaves it in `ttype`, `sval` and `nval`.
     *
     * @return the same value that is left in `ttype`
     */
    public int nextToken() throws IOException {
        if (this.pushedBack) {
            this.pushedBack = false;
            return this.ttype;
        }
        byte[] ct = this.ctype;
        this.sval = null;

        int c = this.peekc;
        if (c < 0) {
            c = NEED_CHAR;
        }
        if (c == SKIP_LF) {
            c = this.read();
            if (c < 0) {
                this.ttype = TT_EOF;
                return this.ttype;
            }
            if (c == '\n') {
                c = NEED_CHAR;
            }
        }
        if (c == NEED_CHAR) {
            c = this.read();
            if (c < 0) {
                this.ttype = TT_EOF;
                return this.ttype;
            }
        }
        // It is stored right away just in case: if something further down returns without touching
        // `peekc`, the next call has to fetch a new character and not repeat this one.
        this.ttype = c;
        this.peekc = NEED_CHAR;

        int kind;
        if (c < 256) {
            kind = ct[c];
        } else {
            kind = CT_ALPHA;
        }

        // ---- whitespace ----
        while ((kind & CT_WHITESPACE) != 0) {
            if (c == '\r') {
                this.LINENO = this.LINENO + 1;
                if (this.eolIsSignificantP) {
                    this.peekc = SKIP_LF;
                    this.ttype = TT_EOL;
                    return this.ttype;
                }
                c = this.read();
                if (c == '\n') {
                    c = this.read();
                }
            } else {
                if (c == '\n') {
                    this.LINENO = this.LINENO + 1;
                    if (this.eolIsSignificantP) {
                        this.ttype = TT_EOL;
                        return this.ttype;
                    }
                }
                c = this.read();
            }
            if (c < 0) {
                this.ttype = TT_EOF;
                return this.ttype;
            }
            if (c < 256) {
                kind = ct[c];
            } else {
                kind = CT_ALPHA;
            }
        }

        // ---- numbers ----
        if ((kind & CT_DIGIT) != 0) {
            boolean neg = false;
            if (c == '-') {
                c = this.read();
                // A `-` that does not start a number is a `-` and nothing more. Without this
                // backtrack, `a - b` would read as `a` and the number `-b`.
                if (c != '.' && (c < '0' || c > '9')) {
                    this.peekc = c;
                    this.ttype = '-';
                    return this.ttype;
                }
                neg = true;
            }
            double v = 0;
            int decexp = 0;
            int seendot = 0;
            while (true) {
                if (c == '.' && seendot == 0) {
                    seendot = 1;
                } else if ('0' <= c && c <= '9') {
                    v = v * 10 + (c - '0');
                    decexp = decexp + seendot;
                } else {
                    break;
                }
                c = this.read();
            }
            this.peekc = c;
            if (decexp != 0) {
                // A single division at the end instead of dividing digit by digit: accumulating the
                // integer and scaling it once loses less precision than adding fractions up.
                double denom = 10;
                decexp = decexp - 1;
                while (decexp > 0) {
                    denom = denom * 10;
                    decexp = decexp - 1;
                }
                v = v / denom;
            }
            if (neg) {
                this.nval = -v;
            } else {
                this.nval = v;
            }
            this.ttype = TT_NUMBER;
            return this.ttype;
        }

        // ---- words ----
        if ((kind & CT_ALPHA) != 0) {
            int i = 0;
            while (true) {
                if (i >= this.buf.length) {
                    this.buf = Arrays.copyOf(this.buf, this.buf.length * 2);
                }
                this.buf[i] = (char) c;
                i = i + 1;
                c = this.read();
                if (c < 0) {
                    kind = CT_WHITESPACE;
                } else if (c < 256) {
                    kind = ct[c];
                } else {
                    kind = CT_ALPHA;
                }
                // The digits continue a word even though they do not start one: `a1` is one
                // token.
                if ((kind & (CT_ALPHA | CT_DIGIT)) == 0) {
                    break;
                }
            }
            this.peekc = c;
            this.sval = String.copyValueOf(this.buf, 0, i);
            if (this.forceLower) {
                this.sval = this.sval.toLowerCase();
            }
            this.ttype = TT_WORD;
            return this.ttype;
        }

        // ---- quoted strings ----
        if ((kind & CT_QUOTE) != 0) {
            this.ttype = c;
            int i = 0;
            // A permanent look-ahead character (`d`) is needed because an octal escape eats up to
            // three digits and the leftover one has to be given back.
            int d = this.read();
            while (d >= 0 && d != this.ttype && d != '\n' && d != '\r') {
                if (d == '\\') {
                    c = this.read();
                    int first = c;
                    if (c >= '0' && c <= '7') {
                        c = c - '0';
                        int c2 = this.read();
                        if ('0' <= c2 && c2 <= '7') {
                            c = (c << 3) + (c2 - '0');
                            c2 = this.read();
                            // `first <= '3'` is what stops `\477` from being read as a
                            // three-digit octal: it does not fit in a byte.
                            if ('0' <= c2 && c2 <= '7' && first <= '3') {
                                c = (c << 3) + (c2 - '0');
                                d = this.read();
                            } else {
                                d = c2;
                            }
                        } else {
                            d = c2;
                        }
                    } else {
                        if (c == 'a') {
                            c = 0x7;
                        } else if (c == 'b') {
                            c = '\b';
                        } else if (c == 'f') {
                            c = 0xC;
                        } else if (c == 'n') {
                            c = '\n';
                        } else if (c == 'r') {
                            c = '\r';
                        } else if (c == 't') {
                            c = '\t';
                        } else if (c == 'v') {
                            c = 0xB;
                        }
                        d = this.read();
                    }
                } else {
                    c = d;
                    d = this.read();
                }
                if (i >= this.buf.length) {
                    this.buf = Arrays.copyOf(this.buf, this.buf.length * 2);
                }
                this.buf[i] = (char) c;
                i = i + 1;
            }

            // If it stopped at the closing quote, that quote is consumed; if it stopped at an end
            // of line or at the end of the stream, that character is given back for the next token.
            if (d == this.ttype) {
                this.peekc = NEED_CHAR;
            } else {
                this.peekc = d;
            }
            this.sval = String.copyValueOf(this.buf, 0, i);
            return this.ttype;
        }

        // ---- comments starting with `/` ----
        if (c == '/' && (this.slashSlashCommentsP || this.slashStarCommentsP)) {
            c = this.read();
            if (c == '*' && this.slashStarCommentsP) {
                int prevc = 0;
                while (true) {
                    c = this.read();
                    if (c == '/' && prevc == '*') {
                        break;
                    }
                    if (c == '\r') {
                        this.LINENO = this.LINENO + 1;
                        c = this.read();
                        if (c == '\n') {
                            c = this.read();
                        }
                    } else if (c == '\n') {
                        this.LINENO = this.LINENO + 1;
                        c = this.read();
                    }
                    if (c < 0) {
                        this.ttype = TT_EOF;
                        return this.ttype;
                    }
                    prevc = c;
                }
                return this.nextToken();
            } else if (c == '/' && this.slashSlashCommentsP) {
                while (true) {
                    c = this.read();
                    if (c == '\n' || c == '\r' || c < 0) {
                        break;
                    }
                }
                this.peekc = c;
                return this.nextToken();
            } else {
                // It was neither `//` nor `/*`. If `/` is also declared a comment character in its
                // own right, it still opens a line comment; if not, it is a lone `/`.
                if ((ct['/'] & CT_COMMENT) != 0) {
                    while (true) {
                        c = this.read();
                        if (c == '\n' || c == '\r' || c < 0) {
                            break;
                        }
                    }
                    this.peekc = c;
                    return this.nextToken();
                } else {
                    this.peekc = c;
                    this.ttype = '/';
                    return this.ttype;
                }
            }
        }

        // ---- comments from any single character ----
        if ((kind & CT_COMMENT) != 0) {
            while (true) {
                c = this.read();
                if (c == '\n' || c == '\r' || c < 0) {
                    break;
                }
            }
            this.peekc = c;
            return this.nextToken();
        }

        // ---- any other character comes out on its own ----
        this.ttype = c;
        return this.ttype;
    }

    /**
     * It makes the next `nextToken()` return the current token again without reading anything.
     *
     * <p>It is a look-back of one and not a stack: calling it twice in a row does not go back two
     * tokens.
     */
    public void pushBack() {
        if (this.ttype != TT_NOTHING) {
            this.pushedBack = true;
        }
    }

    /** The last token's line. The first is 1. */
    public int lineno() {
        return this.LINENO;
    }

    @Override
    public String toString() {
        String ret;
        if (this.ttype == TT_EOF) {
            ret = "EOF";
        } else if (this.ttype == TT_EOL) {
            ret = "EOL";
        } else if (this.ttype == TT_WORD) {
            ret = this.sval;
        } else if (this.ttype == TT_NUMBER) {
            ret = "n=" + this.nval;
        } else if (this.ttype == TT_NOTHING) {
            ret = "NOTHING";
        } else if (this.ttype < 256 && (this.ctype[this.ttype] & CT_QUOTE) != 0) {
            ret = this.sval;
        } else {
            char[] s = new char[3];
            s[0] = '\'';
            s[2] = '\'';
            s[1] = (char) this.ttype;
            ret = new String(s);
        }
        return "Token[" + ret + "], line " + this.LINENO;
    }
}
