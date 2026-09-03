package java.util.logging;

/**
 * KajiLibrary's java.util.logging.Formatter -- convierte un {@link LogRecord} en texto.
 *
 * <p>{@link #getHead} y {@link #getTail} existen para los formatos que **envuelven**: un formateador
 * XML necesita abrir y cerrar el documento, y sin estos ganchos tendria que adivinar cuando empieza y
 * termina la sesion. Para un formato de una linea por mensaje devuelven vacio.
 */
public abstract class Formatter {

    protected Formatter() {
    }

    /** El texto de ese registro. */
    public abstract String format(LogRecord record);

    /** Lo que va antes del primer registro. */
    public String getHead(Handler h) {
        return "";
    }

    /** Lo que va despues del ultimo. */
    public String getTail(Handler h) {
        return "";
    }

    /**
     * El mensaje del registro, traducido y con sus parametros sustituidos.
     *
     * <p>Dos pasos, en este orden. Primero, si el registro trae catalogo, el mensaje **es una clave**
     * y lo que se formatea es lo que el catalogo tenga para ella; una clave ausente no es un error
     * sino que deja pasar el mensaje crudo, que es lo unico util cuando la traduccion falta.
     *
     * <p>Segundo, la sustitucion, que la hace {@link java.text.MessageFormat} y no una pasada a mano
     * sobre las llaves. La diferencia se ve enseguida: `''` es una comilla, `'{0}'` es texto literal
     * y `{0}` con un numero lo formatea segun la region. Sustituir a mano daria otra cosa en los tres
     * casos.
     *
     * <p>Y se sustituye **solo si** hay parametros y el texto tiene alguna `{`: sin esa guarda, un
     * mensaje que hable de comillas se veria alterado sin que nadie haya pedido formatear nada. Si el
     * patron esta mal armado, el mensaje sale crudo en vez de propagar la excepcion -- fallar al
     * emitir una traza no puede tumbar al programa que la emite.
     */
    public synchronized String formatMessage(LogRecord record) {
        String texto = record.getMessage();
        java.util.ResourceBundle catalogo = record.getResourceBundle();
        if (catalogo != null && texto != null) {
            try {
                texto = catalogo.getString(texto);
            } catch (java.util.MissingResourceException e) {
                texto = record.getMessage();
            }
        }
        try {
            Object[] params = record.getParameters();
            if (params == null || params.length == 0) {
                return texto;
            }
            if (texto.indexOf('{') >= 0) {
                return java.text.MessageFormat.format(texto, params);
            }
            return texto;
        } catch (Exception e) {
            return texto;
        }
    }
}
