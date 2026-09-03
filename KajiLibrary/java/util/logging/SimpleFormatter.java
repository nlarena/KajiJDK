package java.util.logging;

/**
 * KajiLibrary's java.util.logging.SimpleFormatter -- dos lineas por mensaje.
 *
 * <p>La primera lleva el instante, el origen y el logger; la segunda el nivel y el mensaje. Es lo que
 * se ve por omision en la consola, y la razon de que sean **dos** lineas es que la primera es larga y
 * casi siempre la misma: poner el mensaje aparte lo deja alineado y legible.
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
