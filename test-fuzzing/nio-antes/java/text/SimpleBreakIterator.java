package java.text;

/**
 * The one {@link BreakIterator} implementation KajiLibrary ships: all four break kinds, driven by a
 * {@code kind} field and rules written in code.
 *
 * <p>Package-private on purpose. The JDK's equivalent is {@code RuleBasedBreakIterator}, which
 * compiles rule <em>text</em> into a state machine loaded from locale data; that is a data-format
 * problem, not a breaking-rules problem, and reproducing it would mean reproducing the data files.
 * The rules below are written directly instead, so the class is not part of the public API and
 * nothing depends on its name.
 *
 * <h2>The rules, and where they stop short of Unicode</h2>
 *
 * <p><b>CHARACTER</b> - a boundary between grapheme clusters, not between {@code char}s. A high
 * surrogate is never separated from its low surrogate, and a combining mark never from the base it
 * sits on. That is the part of UAX #29 that matters for a backspace key; the rest (regional
 * indicator pairs, emoji ZWJ sequences, Hangul jamo composition) is not implemented, so those
 * cluster too finely.
 *
 * <p><b>WORD</b> - a run of letters and digits is one segment, and so is a run of whitespace; every
 * other character stands alone. An apostrophe or a hyphen <em>between</em> two letters is absorbed
 * into the word, which is what makes {@code "isn't"} and {@code "well-known"} come back whole. Not
 * implemented: the dictionary-based breaking that Thai, Lao, Chinese and Japanese need, where words
 * are not delimited at all - those languages come back as one long segment.
 *
 * <p><b>SENTENCE</b> - a sentence ends after {@code .}, {@code !} or {@code ?}, plus any closing
 * quotes or brackets that trail it, plus the whitespace that follows; the boundary is at the first
 * character of the next sentence. Not implemented: the abbreviation exceptions ({@code "Mr."},
 * {@code "e.g."}), which need a word list, so those over-break. The numeric case ({@code "3.14"})
 * happens to come out right, because the rule requires whitespace after the period.
 *
 * <p><b>LINE</b> - a break opportunity after each run of whitespace and after a hyphen, which is
 * the "break between words" rule a simple text wrapper needs. Not implemented: the Unicode line
 * breaking algorithm (UAX #14) with its thirty-odd classes, so no breaks are offered inside CJK
 * text, and no no-break constraints (a non-breaking space, a prohibited break before a closing
 * bracket) are honoured.
 *
 * <p>Every rule set is expressed as one function, {@code boundaryAfter}, that answers "given a
 * boundary at {@code from}, where is the next one?". {@code following}, {@code next},
 * {@code previous} and {@code last} are all written on top of it - including the backward ones,
 * which rescan from the start. That is O(n) per step rather than O(1), and it is the deliberate
 * trade: a rule set that only knows how to move forward is a fraction of the code of one that must
 * also run in reverse, and the reverse direction is what makes break rules hard to get right.
 */
final class SimpleBreakIterator extends BreakIterator {

    // The four rule sets, as FACTORIES rather than as `static final int` constants that
    // BreakIterator would then read.
    //
    // Named constants are what this wants to be, and they do not survive the trip. With the frozen
    // compiler a `static final` primitive read from ANOTHER class is emitted as `getfield` on an
    // empty operand stack (the shape of finding #110), and read from its OWN class it is emitted as
    // `getstatic` against a field that exists only as a `ConstantValue` and therefore evaluates to
    // 0 at runtime (finding #112). Either way the kind would arrive wrong and every iterator would
    // silently run the sentence rules. Methods are the one form that is emitted and executed
    // correctly, which is the same reason DecimalFormatSymbols keeps its tables behind methods.
    // These four collapse back into constants when the compiler snapshot in `bin/` is refreshed.

    static SimpleBreakIterator character() {
        return new SimpleBreakIterator(0);
    }

    static SimpleBreakIterator word() {
        return new SimpleBreakIterator(1);
    }

    static SimpleBreakIterator line() {
        return new SimpleBreakIterator(2);
    }

    static SimpleBreakIterator sentence() {
        return new SimpleBreakIterator(3);
    }

    // 0 = character, 1 = word, 2 = line, 3 = sentence. See the factories above for why this is not
    // a set of named constants.
    private final int kind;
    private CharacterIterator text;
    private int position;

    private SimpleBreakIterator(int kind) {
        this.kind = kind;
        this.text = new StringCharacterIterator("");
        this.position = 0;
    }

    public int first() {
        this.position = this.text.getBeginIndex();
        return this.position;
    }

    public int last() {
        this.position = this.text.getEndIndex();
        return this.position;
    }

    public int current() {
        return this.position;
    }

    public CharacterIterator getText() {
        return this.text;
    }

    public void setText(CharacterIterator newText) {
        this.text = newText;
        this.position = newText.getBeginIndex();
    }

    public int next() {
        if (this.position >= this.text.getEndIndex()) {
            return -1;   // BreakIterator.DONE, spelled out: see the factories above
        }
        this.position = this.boundaryAfter(this.position);
        return this.position;
    }

    public int next(int n) {
        int result = this.position;
        int remaining = n;
        while (remaining > 0) {
            result = this.next();
            if (result == -1) {   // BreakIterator.DONE
                return -1;   // BreakIterator.DONE, spelled out: see the factories above
            }
            remaining = remaining - 1;
        }
        while (remaining < 0) {
            result = this.previous();
            if (result == -1) {   // BreakIterator.DONE
                return -1;   // BreakIterator.DONE, spelled out: see the factories above
            }
            remaining = remaining + 1;
        }
        return result;
    }

    public int previous() {
        int begin = this.text.getBeginIndex();
        if (this.position <= begin) {
            return -1;   // BreakIterator.DONE, spelled out: see the factories above
        }
        // Rescan from the start and keep the last boundary below the cursor. See the class note on
        // why the backward direction is not given rules of its own.
        int last = begin;
        int at = begin;
        while (at < this.position) {
            int nextAt = this.boundaryAfter(at);
            if (nextAt >= this.position) {
                break;
            }
            last = nextAt;
            at = nextAt;
        }
        this.position = last;
        return this.position;
    }

    public int following(int offset) {
        int begin = this.text.getBeginIndex();
        int end = this.text.getEndIndex();
        if (offset < begin || offset > end) {
            throw new IllegalArgumentException("offset out of bounds");
        }
        if (offset >= end) {
            this.position = end;
            return -1;   // BreakIterator.DONE, spelled out: see the factories above
        }
        // `offset` need not itself be a boundary, so walk from the start until a boundary passes it.
        // The first one strictly greater is the answer.
        int at = begin;
        while (at <= offset) {
            at = this.boundaryAfter(at);
        }
        this.position = at;
        return at;
    }

    // BreakIterator already defines these two on top of `following`, and they are overridden here
    // anyway -- not to change what they compute, but to change WHERE the call to `following` is
    // resolved. Inherited, the call is `invokevirtual BreakIterator.following`, whose declaration
    // there is `abstract`; the frozen VM fails to dispatch through an abstract declaration and the
    // call dies as a linkage error. Overridden, the same call resolves on this concrete class and
    // runs. The bodies are the JDK's, unchanged, so behaviour is identical once the VM is fixed.

    public int preceding(int offset) {
        int position = this.following(offset);
        while (position >= offset && position != -1) {
            position = this.previous();
        }
        return position;
    }

    public boolean isBoundary(int offset) {
        if (offset == 0) {
            return true;
        }
        return this.following(offset - 1) == offset;
    }

    // ---- the rules ------------------------------------------------------------------------

    // The next boundary strictly after `from`, which must itself be a boundary or the start of the
    // text. Never returns `from`; at the end of the text it returns the end offset.
    private int boundaryAfter(int from) {
        int end = this.text.getEndIndex();
        if (from >= end) {
            return end;
        }
        if (this.kind == 0) {              // character
            return this.characterBoundaryAfter(from, end);
        }
        if (this.kind == 1) {              // word
            return this.wordBoundaryAfter(from, end);
        }
        if (this.kind == 2) {              // line
            return this.lineBoundaryAfter(from, end);
        }
        return this.sentenceBoundaryAfter(from, end);
    }

    // A grapheme cluster: one base, then its surrogate half if it has one, then every combining
    // mark that follows.
    private int characterBoundaryAfter(int from, int end) {
        int at = from + 1;
        if (SimpleBreakIterator.isHighSurrogate(this.charAt(from)) && at < end
                && SimpleBreakIterator.isLowSurrogate(this.charAt(at))) {
            at = at + 1;
        }
        while (at < end && SimpleBreakIterator.isCombiningMark(this.charAt(at))) {
            at = at + 1;
        }
        return at;
    }

    private int wordBoundaryAfter(int from, int end) {
        char first = this.charAt(from);
        if (SimpleBreakIterator.isWordChar(first)) {
            int at = from + 1;
            while (at < end) {
                char c = this.charAt(at);
                if (SimpleBreakIterator.isWordChar(c)) {
                    at = at + 1;
                } else if ((c == '\'' || c == '-' || c == '\u2019')
                        && at + 1 < end && SimpleBreakIterator.isWordChar(this.charAt(at + 1))) {
                    // An inner apostrophe or hyphen belongs to the word, but only with a letter on
                    // BOTH sides: a trailing one is punctuation ("dogs'" ends the word).
                    at = at + 2;
                } else {
                    break;
                }
            }
            return at;
        }
        if (SimpleBreakIterator.isSpace(first)) {
            int at = from + 1;
            while (at < end && SimpleBreakIterator.isSpace(this.charAt(at))) {
                at = at + 1;
            }
            return at;
        }
        // Any other character is a segment on its own.
        return from + 1;
    }

    private int lineBoundaryAfter(int from, int end) {
        int at = from;
        while (at < end) {
            char c = this.charAt(at);
            if (SimpleBreakIterator.isSpace(c)) {
                // The break opportunity sits AFTER the whitespace run, so the spaces travel with the
                // line they end rather than starting the next one.
                int run = at;
                while (run < end && SimpleBreakIterator.isSpace(this.charAt(run))) {
                    run = run + 1;
                }
                return run;
            }
            if (c == '-' && at + 1 < end) {
                return at + 1;
            }
            at = at + 1;
        }
        return end;
    }

    private int sentenceBoundaryAfter(int from, int end) {
        int at = from;
        while (at < end) {
            char c = this.charAt(at);
            at = at + 1;
            if (c == '.' || c == '!' || c == '?') {
                // Absorb the closers that belong to the sentence just ended.
                while (at < end && SimpleBreakIterator.isCloser(this.charAt(at))) {
                    at = at + 1;
                }
                if (at >= end) {
                    return end;
                }
                if (!SimpleBreakIterator.isSpace(this.charAt(at))) {
                    // No space after the mark: an abbreviation or a decimal point, not an end.
                    continue;
                }
                while (at < end && SimpleBreakIterator.isSpace(this.charAt(at))) {
                    at = at + 1;
                }
                return at;
            }
        }
        return end;
    }

    private char charAt(int index) {
        return this.text.setIndex(index);
    }

    // ---- character classification --------------------------------------------------------
    //
    // java.lang.Character in this library has no isLetter/isDigit/isWhitespace, so the tests are
    // written here. They cover ASCII exactly and the Latin-1/Greek/Cyrillic ranges by construction;
    // a script outside those is treated as a letter if it is above the punctuation blocks, which is
    // the approximation that keeps non-Latin text from shattering into single characters. A real
    // implementation reads the Unicode general category, which is a table this library does not
    // carry (NormTables holds decomposition data, not categories).

    private static boolean isWordChar(char c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        if (c >= '0' && c <= '9') {
            return true;
        }
        if (c == '_') {
            return true;
        }
        if (c < '\u00c0') {
            return false;
        }
        // Above Latin-1 punctuation: letters, marks and digits of other scripts. The general
        // punctuation block (U+2000..U+206F) and the CJK symbol block (U+3000..U+303F) are the two
        // ranges in that region that are definitely NOT word characters; so are the two Latin-1
        // math signs that sit among the letters.
        if (c >= '\u2000' && c <= '\u206f') {
            return false;
        }
        if (c >= '\u3000' && c <= '\u303f') {
            return false;
        }
        return c != '\u00d7' && c != '\u00f7';
    }

    private static boolean isSpace(char c) {
        return c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\f'
                || c == '' || c == '\u00a0' || c == '\u2028' || c == '\u2029'
                || c == '\u3000';
    }

    private static boolean isCloser(char c) {
        return c == '"' || c == '\'' || c == ')' || c == ']' || c == '}'
                || c == '\u2019' || c == '\u201d';
    }

    private static boolean isHighSurrogate(char c) {
        return c >= 0xd800 && c <= 0xdbff;
    }

    private static boolean isLowSurrogate(char c) {
        return c >= 0xdc00 && c <= 0xdfff;
    }

    // The combining-mark blocks a base character must not be separated from: the combining
    // diacriticals, their supplement and extended forms, the marks for symbols, and the half marks.
    private static boolean isCombiningMark(char c) {
        if (c >= '\u0300' && c <= '\u036f') {
            return true;
        }
        if (c >= '\u1ab0' && c <= '\u1aff') {
            return true;
        }
        if (c >= '\u1dc0' && c <= '\u1dff') {
            return true;
        }
        if (c >= '\u20d0' && c <= '\u20f0') {
            return true;
        }
        return c >= '\ufe20' && c <= '\ufe2f';
    }

}
