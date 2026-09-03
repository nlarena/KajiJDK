package java.util.logging;

/**
 * KajiLibrary's java.util.logging.StreamHandler -- escribe a un flujo.
 *
 * <p>Es la base de casi todos los demas. Lo unico con vuelta que hace es **cuando** escribe la
 * cabecera del formateador: no al abrir sino junto con el primer registro, y si no hubo ninguno,
 * recien al cerrar. La diferencia se ve con un {@link XMLFormatter}: la declaracion del documento
 * tiene que salir antes que el primer `<record>`, y el `</log>` de cierre tiene que salir aunque no
 * haya habido ni uno -- un documento XML sin raiz no es un documento, y un archivo de traza vacio que
 * no se puede parsear es peor que uno con un `<log></log>` adentro.
 *
 * <p>Su nivel por omision es {@link Level#INFO}, no {@link Level#ALL} como el de {@link Handler}.
 * Escribir a un flujo cuesta, y el que arma uno a mano casi siempre lo quiere para lo mismo que la
 * consola.
 */
public class StreamHandler extends Handler {

    private java.io.Writer writer;
    private boolean cabeceraEscrita = false;

    public StreamHandler() {
        this.configurar("java.util.logging.StreamHandler");
    }

    public StreamHandler(java.io.OutputStream out, Formatter formatter) {
        this.configurar("java.util.logging.StreamHandler");
        this.setFormatter(formatter);
        this.setOutputStream(out);
    }

    // Lo que este manejador lee de la configuracion. `cname` es el nombre de la clase que manda: una
    // subclase la vuelve a llamar con el suyo para que sus propiedades pisen a estas.
    void configurar(String cname) {
        LogManager m = LogManager.getLogManager();
        this.setLevel(m.getLevelProperty(cname + ".level", Level.INFO));
        this.setFilter(m.getFilterProperty(cname + ".filter", null));
        this.setFormatter(m.getFormatterProperty(cname + ".formatter", new SimpleFormatter()));
        try {
            this.setEncoding(m.getStringProperty(cname + ".encoding", null));
        } catch (Exception e) {
            // Una codificacion que no existe deja al manejador con la de la plataforma en vez de
            // impedir que se construya: no poder escribir la traza no puede tumbar al programa.
            try {
                this.setEncoding(null);
            } catch (Exception e2) {
                // No puede pasar: `null` siempre se acepta.
            }
        }
    }

    /** Cambia el destino, cerrando el anterior. */
    protected synchronized void setOutputStream(java.io.OutputStream out) throws SecurityException {
        if (out == null) {
            throw new NullPointerException("out");
        }
        this.cerrarSalida();
        String enc = this.getEncoding();
        if (enc == null) {
            this.writer = new java.io.OutputStreamWriter(out);
        } else {
            try {
                this.writer = new java.io.OutputStreamWriter(out, enc);
            } catch (java.io.UnsupportedEncodingException e) {
                // `setEncoding` ya la valido; si igual no se puede, la de la plataforma es mejor que
                // ningun destino.
                this.writer = new java.io.OutputStreamWriter(out);
            }
        }
        this.cabeceraEscrita = false;
    }

    public synchronized void publish(LogRecord record) {
        if (!this.isLoggable(record) || this.writer == null) {
            return;
        }
        try {
            if (!this.cabeceraEscrita) {
                this.writer.write(this.getFormatter().getHead(this));
                this.cabeceraEscrita = true;
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
        this.cerrarSalida();
    }

    private void cerrarSalida() {
        if (this.writer == null) {
            return;
        }
        try {
            if (!this.cabeceraEscrita) {
                this.writer.write(this.getFormatter().getHead(this));
                this.cabeceraEscrita = true;
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
