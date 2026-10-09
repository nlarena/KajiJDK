package javax.script;

/**
 * KajiLibrary's javax.script.ScriptException -- what an engine throws when the script does not
 * work.
 *
 * <p>It covers the two things that can go wrong for an engine: the script not compiling and it
 * blowing up while running. It is `checked` on purpose -- whoever evaluates somebody else's text
 * has to decide what to do when the text is wrong, and the compiler reminds them.
 *
 * <p>The only thing with logic of its own is {@link #getMessage()}. The exception optionally keeps
 * where the thing happened (file, line, column) and builds the message by appending those data at
 * the end, skipping the ones it does not have. With file and line, `"boom"` becomes
 * `"boom in a.js at line number 5"`; without a file, the whole position is ignored even if there is
 * a line, because a line without a file locates nothing. The value that means "I do not know" is
 * `-1`, and it is what the two constructors that do not receive it put.
 */
public class ScriptException extends Exception {

    private static final long serialVersionUID = 8265071037049225001L;

    private final String fileName;
    private final int lineNumber;
    private final int columnNumber;

    /** With a message and no position. */
    public ScriptException(String s) {
        super(s);
        this.fileName = null;
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    /**
     * Wrapping another exception, which stays as the cause.
     *
     * <p>The message becomes `e`'s `toString()`, which is what {@link Throwable} does when it is
     * built with a cause and no text.
     */
    public ScriptException(Exception e) {
        super(e);
        this.fileName = null;
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    /** With message, file and line; the column is left at -1. */
    public ScriptException(String message, String fileName, int lineNumber) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = -1;
    }

    /** With message and the complete position. */
    public ScriptException(String message, String fileName, int lineNumber, int columnNumber) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * The message with the position appended at the end.
     *
     * <p>Without a file nothing is added: the line and the column alone do not locate. With a file
     * the file is added, and then the line and the column that are not -1, in that order.
     */
    @Override
    public String getMessage() {
        String ret = super.getMessage();
        if (fileName != null) {
            ret = ret + (" in " + fileName);
            if (lineNumber != -1) {
                ret = ret + " at line number " + lineNumber;
            }
            if (columnNumber != -1) {
                ret = ret + " at column number " + columnNumber;
            }
        }
        return ret;
    }

    /** The line where it happened, or -1. */
    public int getLineNumber() {
        return lineNumber;
    }

    /** The column where it happened, or -1. */
    public int getColumnNumber() {
        return columnNumber;
    }

    /** The file where it happened, or null. */
    public String getFileName() {
        return fileName;
    }
}
