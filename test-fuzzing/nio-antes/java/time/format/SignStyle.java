package java.time.format;

// KajiLibrary's java.time.format.SignStyle — how the sign of a numeric field is handled when
// formatting and parsing.
public enum SignStyle {

    NORMAL,
    ALWAYS,
    NEVER,
    NOT_NEGATIVE,
    EXCEEDS_PAD;
}
