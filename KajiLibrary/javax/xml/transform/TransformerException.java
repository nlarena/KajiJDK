package javax.xml.transform;

import java.io.PrintStream;
import java.io.PrintWriter;

/**
 * KajiLibrary's javax.xml.transform.TransformerException -- something went wrong, and **where**.
 *
 * <p>It is the base exception of the whole package. What sets it apart from any `Exception` is the
 * {@link SourceLocator}: a transformation error without the stylesheet line is almost useless,
 * because the message describes a rule and the user needs the place.
 *
 * <p>**The historical detail that has to be respected and that surprises everyone:** this class has
 * its own cause field --`containedException`-- and does **not** use {@link Throwable}'s. It was
 * born in TrAX before Java 1.4 gave `Throwable` a chained cause, and when the platform took it in
 * there was already code calling {@code getException()}. Compatibility was solved by keeping its
 * own field and **overriding** {@link #getCause} and {@link #initCause} to operate on it, so that
 * both routes --the old one and the platform's-- see the same. Copying the cause to both sides
 * would have been worse: two fields that can get out of sync.
 *
 * <p>A concrete consequence of that, respected here to the letter: {@link #initCause} on an
 * exception built with a cause throws {@link IllegalStateException}, as in `Throwable`, but looking
 * at its own field. And {@code initCause(null)} on one without a cause **is valid** and leaves it
 * without a cause -- it is not an error, and `Throwable` behaves the same.
 *
 * <p>Another detail that is not cosmetic: the constructors that receive a cause and an empty or
 * null message use {@code cause.toString()} as the message. A wrapped exception without its own
 * message is a blank message in the log, which is the worst way of losing an error.
 */
public class TransformerException extends Exception {

    private static final long serialVersionUID = 975798773772956428L;

    /** Where it happened, if known. */
    private SourceLocator locator;

    /** The cause. See the header note on why it is not `Throwable`'s. */
    private Throwable containedException;

    /**
     * With a message and nothing else.
     *
     * @param message the description of the error
     */
    public TransformerException(String message) {
        super(message);
        this.containedException = null;
        this.locator = null;
    }

    /**
     * Wrapping another exception; the message comes from it.
     *
     * @param e the cause
     */
    public TransformerException(Throwable e) {
        super(e.toString());
        this.containedException = e;
        this.locator = null;
    }

    /**
     * With message and cause.
     *
     * <p>If the message is null or empty {@code e.toString()} is used: see the header note.
     *
     * @param message the description of the error
     * @param e the cause
     */
    public TransformerException(String message, Throwable e) {
        super((message == null || message.length() == 0) ? e.toString() : message);
        this.containedException = e;
        this.locator = null;
    }

    /**
     * With message and location.
     *
     * @param message the description of the error
     * @param locator where it happened
     */
    public TransformerException(String message, SourceLocator locator) {
        super(message);
        this.containedException = null;
        this.locator = locator;
    }

    /**
     * With message, location and cause.
     *
     * @param message the description of the error
     * @param locator where it happened
     * @param e the cause
     */
    public TransformerException(String message, SourceLocator locator, Throwable e) {
        super(message);
        this.containedException = e;
        this.locator = locator;
    }

    // ---- location ---------------------------------------------------------------------------

    /** Where it happened, or null if not known. */
    public SourceLocator getLocator() {
        return locator;
    }

    /**
     * Sets the location.
     *
     * <p>It exists because whoever detects the error is not always who knows where it is: an inner
     * layer throws, and the outer one --which does have the context-- fills in the location before
     * propagating.
     *
     * @param location where it happened, or null to clear it
     */
    public void setLocator(SourceLocator location) {
        this.locator = location;
    }

    /**
     * The location as text, or null if there is no {@link SourceLocator}.
     *
     * <p>Watch the two "empties", which are different and mean different things: **null** is "there
     * is no location"; the **empty string** is "there is a location but it says nothing" --a
     * locator with a null URI and line and column at zero--. Zero components are left out because
     * there is no line zero: 0 is the "unknown" sentinel, and writing it would be reporting what is
     * not known.
     */
    public String getLocationAsString() {
        if (locator == null) {
            return null;
        }
        StringBuilder buf = new StringBuilder();
        appendLocation(buf);
        return buf.toString();
    }

    /**
     * The message followed by the location.
     *
     * <p>What belongs in a log: the message alone describes the rule violated, and without the
     * place it is not enough to fix it. Unlike {@link #getLocationAsString}, this never returns
     * null -- if there is neither message nor location, it returns the empty string.
     */
    public String getMessageAndLocation() {
        StringBuilder buf = new StringBuilder();
        String message = super.getMessage();
        if (message != null) {
            buf.append(message);
        }
        if (locator != null) {
            appendLocation(buf);
        }
        return buf.toString();
    }

    /** What the two above build in common, so that the formats cannot drift apart. */
    private void appendLocation(StringBuilder buf) {
        String systemID = locator.getSystemId();
        int line = locator.getLineNumber();
        int column = locator.getColumnNumber();
        if (systemID != null) {
            buf.append("; SystemID: ");
            buf.append(systemID);
        }
        if (line != 0) {
            buf.append("; Line#: ");
            buf.append(line);
        }
        if (column != 0) {
            buf.append("; Column#: ");
            buf.append(column);
        }
    }

    // ---- cause ------------------------------------------------------------------------------

    /** The cause, by TrAX's old name. Equivalent to {@link #getCause}. */
    public Throwable getException() {
        return containedException;
    }

    /**
     * The cause, by the platform's name.
     *
     * <p>The comparison with {@code this} is `Throwable`'s convention for "no cause" and is
     * respected here in case someone builds the exception with itself inside.
     */
    public Throwable getCause() {
        return (containedException == this) ? null : containedException;
    }

    /**
     * Sets the cause, only once.
     *
     * <p>It operates on its own field, not on `Throwable`'s: see the header note.
     *
     * @param cause the cause, or null
     * @return this same exception
     * @throws IllegalStateException if it already had a cause
     * @throws IllegalArgumentException if the cause is itself
     */
    public synchronized Throwable initCause(Throwable cause) {
        if (this.containedException != null) {
            throw new IllegalStateException("Can't overwrite cause");
        }
        if (cause == this) {
            throw new IllegalArgumentException("Self-causation not permitted");
        }
        this.containedException = cause;
        return this;
    }

    // ---- printing ---------------------------------------------------------------------------

    /**
     * Prints the location, the trace, and then the chain of causes.
     *
     * <p>The location goes **first**, before the trace: it is the datum the user needs and a stack
     * trace would bury it.
     */
    public void printStackTrace() {
        printStackTrace(new PrintWriter(System.err, true));
    }

    /**
     * Likewise, to a byte stream.
     *
     * @param s where to write; null means standard error
     */
    public void printStackTrace(PrintStream s) {
        printStackTrace(new PrintWriter(s == null ? System.err : s, true));
    }

    /**
     * Likewise, to a character writer. This is the real form; the other two delegate here.
     *
     * <p>The loop over the causes has a cap of 10 and a cut on equality. It is not gratuitous
     * paranoia: the chain is built by whoever throws, and an exception that contains itself --or
     * two that contain each other-- would turn an attempt to log an error into a hang. A diagnostic
     * method can never be worse than the problem it is diagnosing; that is also why everything goes
     * inside a `catch (Throwable)` that swallows whatever comes out.
     *
     * @param s where to write; null means standard error
     */
    public void printStackTrace(PrintWriter s) {
        PrintWriter w = (s == null) ? new PrintWriter(System.err, true) : s;
        try {
            String locInfo = getLocationAsString();
            if (locInfo != null) {
                w.println(locInfo);
            }
            super.printStackTrace(w);
        } catch (Throwable ignored) {
            // Not even the report of an error may throw.
        }
        Throwable exception = getException();
        int i = 0;
        while (i < 10 && exception != null) {
            w.println("---------");
            try {
                exception.printStackTrace(w);
            } catch (Throwable ignored) {
                w.println("Could not print stack trace...");
            }
            if (exception instanceof TransformerException) {
                Throwable previous = exception;
                exception = ((TransformerException) exception).getException();
                if (previous == exception) {
                    break;
                }
            } else {
                exception = null;
            }
            i = i + 1;
        }
        w.flush();
    }
}
