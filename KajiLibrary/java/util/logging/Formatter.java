package java.util.logging;

/**
 * KajiLibrary's java.util.logging.Formatter -- it turns a {@link LogRecord} into text.
 *
 * <p>{@link #getHead} and {@link #getTail} exist for the formats that **wrap**: an XML formatter
 * needs to open and close the document, and without these hooks it would have to guess when the
 * session begins and ends. For a one-line-per-message format they return empty.
 */
public abstract class Formatter {

    protected Formatter() {
    }

    /** That record's text. */
    public abstract String format(LogRecord record);

    /** What goes before the first record. */
    public String getHead(Handler h) {
        return "";
    }

    /** What goes after the last. */
    public String getTail(Handler h) {
        return "";
    }

    /**
     * The record's message, translated and with its parameters substituted.
     *
     * <p>Two steps, in this order. First, if the record carries a bundle, the message **is a key**
     * and what is formatted is whatever the bundle has for it; a missing key is not an error but
     * lets the raw message through, which is the only useful thing when the translation is missing.
     *
     * <p>Second, the substitution, which {@link java.text.MessageFormat} does and not a hand-rolled
     * pass over the braces. The difference shows at once: `''` is one quote, `'{0}'` is literal text
     * and `{0}` with a number formats it by region. Substituting by hand would give something else
     * in all three cases.
     *
     * <p>And it substitutes **only if** there are parameters and the text has some `{`: without that
     * guard, a message talking about quotes would be altered without anybody having asked for any
     * formatting. If the pattern is malformed, the message comes out raw instead of propagating the
     * exception -- failing to emit a log entry cannot bring down the program emitting it.
     */
    public synchronized String formatMessage(LogRecord record) {
        String text = record.getMessage();
        java.util.ResourceBundle bundle = record.getResourceBundle();
        if (bundle != null && text != null) {
            try {
                text = bundle.getString(text);
            } catch (java.util.MissingResourceException e) {
                text = record.getMessage();
            }
        }
        try {
            Object[] params = record.getParameters();
            if (params == null || params.length == 0) {
                return text;
            }
            if (text.indexOf('{') >= 0) {
                return java.text.MessageFormat.format(text, params);
            }
            return text;
        } catch (Exception e) {
            return text;
        }
    }
}
