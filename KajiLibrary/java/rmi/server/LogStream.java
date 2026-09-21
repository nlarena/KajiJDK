package java.rmi.server;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

/**
 * RMI's log, from before a logging API existed.
 *
 * @deprecated there is no replacement inside RMI: whoever wants to log uses
 *     {@code java.lang.System.Logger} or the library they prefer. This note used to say it lives
 *     on because {@link RemoteServer#getLog} returns a {@link PrintStream} and this was its
 *     implementation; here {@code RemoteServer.setLog} builds a plain {@link PrintStream} and
 *     nothing in the library uses this class (checked with grep), so it lives on only as public
 *     API of {@code java.rmi.server}.
 */
@Deprecated(since = "1.1")
public class LogStream extends PrintStream {

    /** No logging. */
    public static final int SILENT = 0;

    /** Only the essentials. */
    public static final int BRIEF = 10;

    /** Everything. */
    public static final int VERBOSE = 20;

    private static final Map<String, LogStream> KNOWN = new HashMap<String, LogStream>();
    private static PrintStream defaultStream = System.err;

    private OutputStream logOut;

    private LogStream(OutputStream out) {
        super(out);
        this.logOut = out;
    }

    /**
     * The log with that name, creating it if it was not there.
     *
     * <p>One per name and shared, which is what lets two parts of the program write to the same one
     * without passing it around.
     */
    public static LogStream log(String name) {
        synchronized (KNOWN) {
            LogStream l = KNOWN.get(name);
            if (l == null) {
                l = new LogStream(defaultStream);
                KNOWN.put(name, l);
            }
            return l;
        }
    }

    /** Where new logs go. */
    public static synchronized PrintStream getDefaultStream() {
        return defaultStream;
    }

    /** It changes where new logs go; the ones already created do not move. */
    public static synchronized void setDefaultStream(PrintStream newDefault) {
        defaultStream = newDefault;
    }

    /** Where this log writes. */
    public synchronized OutputStream getOutputStream() {
        return this.logOut;
    }

    /**
     * It changes the stream {@link #getOutputStream} reports.
     *
     * <p>This note used to say it changes where this log writes; it does not: {@code write} goes
     * through {@code super.write}, which never looks at {@code logOut}, and {@link PrintStream}
     * sends every write to the native {@code writeString}, which prints to the VM's stdout whatever
     * stream was given (checked in {@code java/io/PrintStream.java} and the VM's
     * {@code natives.rs}).
     */
    public synchronized void setOutputStream(OutputStream out) {
        this.logOut = out;
    }

    /** It writes a byte. */
    public void write(int b) {
        super.write(b);
    }

    /** It writes a slice. */
    public void write(byte[] b, int off, int len) {
        super.write(b, off, len);
    }

    public String toString() {
        return "LogStream";
    }

    /**
     * It translates {@code "SILENT"}, {@code "BRIEF"} or {@code "VERBOSE"} to its number.
     *
     * @return the level, or {@code -1} if the name is none of the three
     */
    public static int parseLevel(String s) {
        if (s == null) {
            return -1;
        }
        String t = s.trim().toUpperCase(java.util.Locale.ENGLISH);
        if (t.equals("SILENT")) {
            return SILENT;
        }
        if (t.equals("BRIEF")) {
            return BRIEF;
        }
        if (t.equals("VERBOSE")) {
            return VERBOSE;
        }
        return -1;
    }
}
