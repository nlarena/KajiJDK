package java.util.logging;

/**
 * KajiLibrary's java.util.logging.XMLFormatter -- the same log, for a program to read.
 *
 * <p>It is the case that justifies {@link Formatter} having {@link Formatter#getHead} and
 * {@link Formatter#getTail}: an XML document needs the declaration and the opening `<log>` before
 * the first record and the closing one after the last, and without those two hooks there would be
 * nowhere to put them.
 *
 * <p>Against {@link SimpleFormatter} the difference is not one of taste: here **nothing is lost**.
 * The date goes to the nanosecond, the sequence number goes, the exception's stack trace goes frame
 * by frame with its line. It costs some ten times the space and it is what suits when the log is
 * going to be read by a tool and not by a person.
 *
 * <p>Two decisions that surprise on reading the output and are the contract's:
 *
 * <ul>
 * <li>The date is written in **UTC**, not in the local zone. A log file gets joined with others from
 *     other machines, and ordering by local time is ordering wrongly.
 * <li>The `<param>` come out **only if** the message has no `{` at all. If it has, the parameters are
 *     already inside the `<message>`, substituted, and repeating them outside would say the same
 *     thing twice.
 * </ul>
 *
 * <p>The `<nanos>` appears only when there are nanoseconds `<millis>` cannot count. It is redundant
 * with `<date>` on purpose: `<millis>` plus `<nanos>` reconstruct the exact instant without parsing
 * a date.
 */
public class XMLFormatter extends Formatter {

    // The line break is always LF, and **not** the platform's. It is an XML document: what is
    // written here is read by a parser on another machine, and the line break of the system that
    // generated it tells nobody anything. See `SimpleFormatter`, which does the opposite because a
    // person reads it.
    private static final String LINE_BREAK = "\n";

    public XMLFormatter() {
    }

    public String format(LogRecord record) {
        String nl = LINE_BREAK;
        StringBuilder sb = new StringBuilder();
        sb.append("<record>").append(nl);

        sb.append("  <date>");
        sb.append(java.time.ZonedDateTime.ofInstant(record.getInstant(), java.time.ZoneOffset.UTC)
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        sb.append("</date>").append(nl);

        sb.append("  <millis>").append(record.getMillis()).append("</millis>").append(nl);

        int nanos = record.getInstant().getNano() % 1000000;
        if (nanos != 0) {
            sb.append("  <nanos>").append(nanos).append("</nanos>").append(nl);
        }

        sb.append("  <sequence>").append(record.getSequenceNumber()).append("</sequence>").append(nl);

        if (record.getLoggerName() != null) {
            sb.append("  <logger>");
            escape(sb, record.getLoggerName());
            sb.append("</logger>").append(nl);
        }

        sb.append("  <level>");
        escape(sb, record.getLevel().toString());
        sb.append("</level>").append(nl);

        if (record.getSourceClassName() != null) {
            sb.append("  <class>");
            escape(sb, record.getSourceClassName());
            sb.append("</class>").append(nl);
        }
        if (record.getSourceMethodName() != null) {
            sb.append("  <method>");
            escape(sb, record.getSourceMethodName());
            sb.append("</method>").append(nl);
        }

        sb.append("  <thread>").append(record.getLongThreadID()).append("</thread>").append(nl);

        if (record.getMessage() != null) {
            sb.append("  <message>");
            escape(sb, this.formatMessage(record));
            sb.append("</message>").append(nl);
        }

        // The key and the bundle only if the message **really** was translated: a `<key>` over a
        // message the bundle does not define would be saying there is a translation where there is
        // none.
        java.util.ResourceBundle bundle = record.getResourceBundle();
        try {
            if (bundle != null && bundle.getString(record.getMessage()) != null) {
                sb.append("  <key>");
                escape(sb, record.getMessage());
                sb.append("</key>").append(nl);
                sb.append("  <catalog>");
                escape(sb, record.getResourceBundleName());
                sb.append("</catalog>").append(nl);
            }
        } catch (Exception e) {
            // With no translation neither the key nor the bundle goes, and that is all.
        }

        Object[] params = record.getParameters();
        if (params != null && params.length != 0 && record.getMessage() != null
                && record.getMessage().indexOf('{') < 0) {
            int i = 0;
            while (i < params.length) {
                sb.append("  <param>");
                try {
                    escape(sb, params[i].toString());
                } catch (Exception e) {
                    // A `toString` that fails --or a null parameter-- cannot stop the rest of the
                    // record being written: the element stays, with its content marked.
                    sb.append("???");
                }
                sb.append("</param>").append(nl);
                i = i + 1;
            }
        }

        Throwable th = record.getThrown();
        if (th != null) {
            sb.append("  <exception>").append(nl);
            sb.append("    <message>");
            escape(sb, th.toString());
            sb.append("</message>").append(nl);
            StackTraceElement[] trace = th.getStackTrace();
            int i = 0;
            while (i < trace.length) {
                StackTraceElement frame = trace[i];
                sb.append("    <frame>").append(nl);
                sb.append("      <class>");
                escape(sb, frame.getClassName());
                sb.append("</class>").append(nl);
                sb.append("      <method>");
                escape(sb, frame.getMethodName());
                sb.append("</method>").append(nl);
                if (frame.getLineNumber() >= 0) {
                    sb.append("      <line>").append(frame.getLineNumber()).append("</line>")
                            .append(nl);
                }
                sb.append("    </frame>").append(nl);
                i = i + 1;
            }
            sb.append("  </exception>").append(nl);
        }

        sb.append("</record>").append(nl);
        return sb.toString();
    }

    /**
     * The XML declaration, the DOCTYPE and the opening `<log>`.
     *
     * <p>The encoding is taken from the handler when it declares one, because it is the handler that
     * writes the bytes: announcing in the header an encoding other than the one used produces a file
     * that cannot be read, and that is worse than announcing nothing.
     */
    public String getHead(Handler h) {
        String nl = LINE_BREAK;
        String encodingName = h == null ? null : h.getEncoding();
        if (encodingName == null) {
            encodingName = java.nio.charset.Charset.defaultCharset().name();
        }
        try {
            encodingName = java.nio.charset.Charset.forName(encodingName).name();
        } catch (Exception e) {
            // A name that is not recognised is written as it stands: it is what the handler said it uses.
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"").append(encodingName)
                .append("\" standalone=\"no\"?>").append(nl);
        sb.append("<!DOCTYPE log SYSTEM \"logger.dtd\">").append(nl);
        sb.append("<log>").append(nl);
        return sb.toString();
    }

    public String getTail(Handler h) {
        return "</log>" + LINE_BREAK;
    }

    // The three characters that cannot appear raw inside an element. Quotes are not escaped because
    // nothing this writes goes inside an attribute.
    private void escape(StringBuilder sb, String text) {
        if (text == null) {
            text = "<null>";
        }
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '<') {
                sb.append("&lt;");
            } else if (c == '>') {
                sb.append("&gt;");
            } else if (c == '&') {
                sb.append("&amp;");
            } else {
                sb.append(c);
            }
            i = i + 1;
        }
    }
}
