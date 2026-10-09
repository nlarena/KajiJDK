package java.util.logging;

/**
 * KajiLibrary's java.util.logging.StreamHandler -- it writes to a stream.
 *
 * <p>It is the base of almost all the others. The one thing with a twist that it does is **when** it
 * writes the formatter's head: not on opening but along with the first record, and if there was
 * none, only on closing. The difference shows with an {@link XMLFormatter}: the document's
 * declaration has to come out before the first `<record>`, and the closing `</log>` has to come out
 * even if there was not a single one -- an XML document with no root is not a document, and an empty
 * log file that cannot be parsed is worse than one with a `<log></log>` inside.
 *
 * <p>Its default level is {@link Level#INFO}, not {@link Level#ALL} like {@link Handler}'s. Writing
 * to a stream costs, and whoever builds one by hand almost always wants it for the same thing as the
 * console.
 */
public class StreamHandler extends Handler {

    private java.io.Writer writer;
    private boolean headWritten = false;

    public StreamHandler() {
        this.configure("java.util.logging.StreamHandler");
    }

    public StreamHandler(java.io.OutputStream out, Formatter formatter) {
        this.configure("java.util.logging.StreamHandler");
        this.setFormatter(formatter);
        this.setOutputStream(out);
    }

    // What this handler reads from the configuration. `cname` is the name of the class in charge: a
    // subclass calls it again with its own so that its properties override these.
    void configure(String cname) {
        LogManager m = LogManager.getLogManager();
        this.setLevel(m.getLevelProperty(cname + ".level", Level.INFO));
        this.setFilter(m.getFilterProperty(cname + ".filter", null));
        this.setFormatter(m.getFormatterProperty(cname + ".formatter", new SimpleFormatter()));
        try {
            this.setEncoding(m.getStringProperty(cname + ".encoding", null));
        } catch (Exception e) {
            // An encoding that does not exist leaves the handler with the platform's instead of
            // stopping it being constructed: not being able to write the log cannot bring the program
            // down.
            try {
                this.setEncoding(null);
            } catch (Exception e2) {
                // It cannot happen: `null` is always accepted.
            }
        }
    }

    /** It changes the target, closing the previous one. */
    protected synchronized void setOutputStream(java.io.OutputStream out) throws SecurityException {
        if (out == null) {
            throw new NullPointerException("out");
        }
        this.closeOutput();
        String enc = this.getEncoding();
        if (enc == null) {
            this.writer = new java.io.OutputStreamWriter(out);
        } else {
            try {
                this.writer = new java.io.OutputStreamWriter(out, enc);
            } catch (java.io.UnsupportedEncodingException e) {
                // `setEncoding` already validated it; if it still cannot be used, the platform's is
                // better than no target at all.
                this.writer = new java.io.OutputStreamWriter(out);
            }
        }
        this.headWritten = false;
    }

    public synchronized void publish(LogRecord record) {
        if (!this.isLoggable(record) || this.writer == null) {
            return;
        }
        try {
            if (!this.headWritten) {
                this.writer.write(this.getFormatter().getHead(this));
                this.headWritten = true;
            }
            this.writer.write(this.getFormatter().format(record));
        } catch (Exception e) {
            this.reportError(null, e, ErrorManager.WRITE_FAILURE);
        }
    }

    public synchronized void flush() {
        if (this.writer == null) {
            return;
        }
        try {
            this.writer.flush();
        } catch (Exception e) {
            this.reportError(null, e, ErrorManager.FLUSH_FAILURE);
        }
    }

    public synchronized void close() throws SecurityException {
        this.closeOutput();
    }

    private void closeOutput() {
        if (this.writer == null) {
            return;
        }
        try {
            if (!this.headWritten) {
                this.writer.write(this.getFormatter().getHead(this));
                this.headWritten = true;
            }
            this.writer.write(this.getFormatter().getTail(this));
            this.writer.flush();
            this.writer.close();
        } catch (Exception e) {
            this.reportError(null, e, ErrorManager.CLOSE_FAILURE);
        }
        this.writer = null;
    }
}
