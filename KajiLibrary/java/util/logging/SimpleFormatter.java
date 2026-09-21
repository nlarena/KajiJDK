package java.util.logging;

/**
 * KajiLibrary's java.util.logging.SimpleFormatter -- two lines per message.
 *
 * <p>The first carries the instant, the source and the logger; the second the level and the message.
 * It is what is seen by default on the console, and the reason there are **two** lines is that the
 * first is long and almost always the same: putting the message on its own leaves it aligned and
 * legible.
 */
public class SimpleFormatter extends Formatter {

    public String format(LogRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append(record.getInstant().toString());
        sb.append(' ');
        if (record.getSourceClassName() != null) {
            sb.append(record.getSourceClassName());
            if (record.getSourceMethodName() != null) {
                sb.append(' ');
                sb.append(record.getSourceMethodName());
            }
        } else {
            sb.append(String.valueOf(record.getLoggerName()));
        }
        sb.append(System.lineSeparator());
        sb.append(record.getLevel().getName());
        sb.append(": ");
        sb.append(String.valueOf(this.formatMessage(record)));
        sb.append(System.lineSeparator());
        Throwable t = record.getThrown();
        if (t != null) {
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            t.printStackTrace(pw);
            pw.close();
            sb.append(sw.toString());
        }
        return sb.toString();
    }
}
