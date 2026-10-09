package java.time.format;

// KajiLibrary's java.time.format.TextStyle — the width of textual names (month/day-of-week/era), each
// in a formatting and a standalone flavour. The standalone variants have an odd ordinal, so the flag
// and the sibling lookups are ordinal arithmetic.
public enum TextStyle {

    FULL,
    FULL_STANDALONE,
    SHORT,
    SHORT_STANDALONE,
    NARROW,
    NARROW_STANDALONE;

    public boolean isStandalone() {
        return (this.ordinal() & 1) == 1;
    }

    public TextStyle asStandalone() {
        return TextStyle.values()[this.ordinal() | 1];
    }

    public TextStyle asNormal() {
        int o = this.ordinal();
        return TextStyle.values()[o - (o & 1)];
    }
}
