package java.util.logging;

/**
 * KajiLibrary's java.util.logging.XMLFormatter -- la misma traza, para que la lea un programa.
 *
 * <p>Es el caso que justifica que {@link Formatter} tenga {@link Formatter#getHead} y
 * {@link Formatter#getTail}: un documento XML necesita la declaracion y el `<log>` de apertura antes
 * del primer registro y el cierre despues del ultimo, y sin esos dos ganchos no habria donde
 * ponerlos.
 *
 * <p>Contra {@link SimpleFormatter} la diferencia no es de gusto: aca **nada se pierde**. La fecha va
 * al nanosegundo, el numero de secuencia va, la traza de pila de la excepcion va cuadro por cuadro
 * con su linea. Cuesta unas diez veces mas espacio y es lo que corresponde cuando la traza la va a
 * leer una herramienta y no una persona.
 *
 * <p>Dos decisiones que sorprenden al leer la salida y son del contrato:
 *
 * <ul>
 * <li>La fecha se escribe en **UTC**, no en la zona local. Un archivo de traza se junta con otros de
 *     otras maquinas, y ordenar por hora local es ordenar mal.
 * <li>Los `<param>` salen **solo si** el mensaje no tiene ninguna `{`. Si las tiene, los parametros ya
 *     estan dentro del `<message>` sustituidos, y repetirlos afuera seria decir dos veces lo mismo.
 * </ul>
 *
 * <p>El `<nanos>` aparece solo cuando hay nanosegundos que `<millis>` no alcanza a contar. Es
 * redundante con `<date>` a proposito: `<millis>` mas `<nanos>` reconstruyen el instante exacto sin
 * parsear una fecha.
 */
public class XMLFormatter extends Formatter {

    // El salto de linea es siempre LF, y **no** el de la plataforma. Es un documento XML: lo que se
    // escribe aca lo lee un parser en otra maquina, y el salto de linea del sistema que lo genero no
    // le dice nada a nadie. Ver `SimpleFormatter`, que hace lo contrario porque lo lee una persona.
    private static final String SALTO = "\n";

    public XMLFormatter() {
    }

    public String format(LogRecord record) {
        String nl = SALTO;
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
            escapar(sb, record.getLoggerName());
            sb.append("</logger>").append(nl);
        }

        sb.append("  <level>");
        escapar(sb, record.getLevel().toString());
        sb.append("</level>").append(nl);

        if (record.getSourceClassName() != null) {
            sb.append("  <class>");
            escapar(sb, record.getSourceClassName());
            sb.append("</class>").append(nl);
        }
        if (record.getSourceMethodName() != null) {
            sb.append("  <method>");
            escapar(sb, record.getSourceMethodName());
            sb.append("</method>").append(nl);
        }

        sb.append("  <thread>").append(record.getLongThreadID()).append("</thread>").append(nl);

        if (record.getMessage() != null) {
            sb.append("  <message>");
            escapar(sb, this.formatMessage(record));
            sb.append("</message>").append(nl);
        }

        // La clave y el catalogo solo si el mensaje **de verdad** se tradujo: un `<key>` sobre un
        // mensaje que el catalogo no define seria decir que hay una traduccion donde no la hay.
        java.util.ResourceBundle catalogo = record.getResourceBundle();
        try {
            if (catalogo != null && catalogo.getString(record.getMessage()) != null) {
                sb.append("  <key>");
                escapar(sb, record.getMessage());
                sb.append("</key>").append(nl);
                sb.append("  <catalog>");
                escapar(sb, record.getResourceBundleName());
                sb.append("</catalog>").append(nl);
            }
        } catch (Exception e) {
            // Sin traduccion no van ni la clave ni el catalogo, y nada mas.
        }

        Object[] params = record.getParameters();
        if (params != null && params.length != 0 && record.getMessage() != null
                && record.getMessage().indexOf('{') < 0) {
            int i = 0;
            while (i < params.length) {
                sb.append("  <param>");
                try {
                    escapar(sb, params[i].toString());
                } catch (Exception e) {
                    // Un `toString` que falla --o un parametro nulo-- no puede impedir que el resto
                    // del registro se escriba: el elemento queda, con su contenido marcado.
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
            escapar(sb, th.toString());
            sb.append("</message>").append(nl);
            StackTraceElement[] traza = th.getStackTrace();
            int i = 0;
            while (i < traza.length) {
                StackTraceElement cuadro = traza[i];
                sb.append("    <frame>").append(nl);
                sb.append("      <class>");
                escapar(sb, cuadro.getClassName());
                sb.append("</class>").append(nl);
                sb.append("      <method>");
                escapar(sb, cuadro.getMethodName());
                sb.append("</method>").append(nl);
                if (cuadro.getLineNumber() >= 0) {
                    sb.append("      <line>").append(cuadro.getLineNumber()).append("</line>")
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
     * La declaracion XML, el DOCTYPE y el `<log>` de apertura.
     *
     * <p>La codificacion se toma del manejador cuando la declara, porque el que escribe los bytes es
     * el: anunciar en la cabecera una codificacion distinta de la que se usa produce un archivo que
     * no se puede leer, y eso es peor que no anunciar nada.
     */
    public String getHead(Handler h) {
        String nl = SALTO;
        String codificacion = h == null ? null : h.getEncoding();
        if (codificacion == null) {
            codificacion = java.nio.charset.Charset.defaultCharset().name();
        }
        try {
            codificacion = java.nio.charset.Charset.forName(codificacion).name();
        } catch (Exception e) {
            // Un nombre que no se reconoce se escribe tal cual: es lo que el manejador dijo que usa.
        }
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"").append(codificacion)
                .append("\" standalone=\"no\"?>").append(nl);
        sb.append("<!DOCTYPE log SYSTEM \"logger.dtd\">").append(nl);
        sb.append("<log>").append(nl);
        return sb.toString();
    }

    public String getTail(Handler h) {
        return "</log>" + SALTO;
    }

    // Los tres caracteres que no pueden aparecer crudos dentro de un elemento. Las comillas no se
    // escapan porque nada de lo que esto escribe va dentro de un atributo.
    private void escapar(StringBuilder sb, String texto) {
        if (texto == null) {
            texto = "<null>";
        }
        int i = 0;
        while (i < texto.length()) {
            char c = texto.charAt(i);
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
