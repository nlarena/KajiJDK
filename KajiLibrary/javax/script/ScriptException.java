package javax.script;

/**
 * KajiLibrary's javax.script.ScriptException -- lo que tira un motor cuando el script no anda.
 *
 * <p>Cubre las dos cosas que le pueden salir mal a un motor: que el script no compile y que
 * explote corriendo. Es `checked` a proposito -- quien evalua texto ajeno tiene que decidir que
 * hace cuando el texto esta mal, y el compilador se lo recuerda.
 *
 * <p>Lo unico con logica propia es {@link #getMessage()}. La excepcion guarda opcionalmente donde
 * paso la cosa (archivo, linea, columna) y arma el mensaje pegando esos datos al final, saltando
 * los que no tiene. Con archivo y linea, `"boom"` se convierte en
 * `"boom in a.js at line number 5"`; sin archivo, la posicion entera se ignora aunque haya linea,
 * porque una linea sin archivo no ubica nada. El valor que significa "no lo se" es `-1`, y es lo
 * que ponen los dos constructores que no lo reciben.
 */
public class ScriptException extends Exception {

    private static final long serialVersionUID = 8265071037049225001L;

    private final String fileName;
    private final int lineNumber;
    private final int columnNumber;

    /** Con un mensaje y nada de posicion. */
    public ScriptException(String s) {
        super(s);
        this.fileName = null;
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    /**
     * Envolviendo otra excepcion, que queda como causa.
     *
     * <p>El mensaje pasa a ser el `toString()` de `e`, que es lo que hace {@link Throwable} cuando
     * se lo construye con una causa y sin texto.
     */
    public ScriptException(Exception e) {
        super(e);
        this.fileName = null;
        this.lineNumber = -1;
        this.columnNumber = -1;
    }

    /** Con mensaje, archivo y linea; la columna queda en -1. */
    public ScriptException(String message, String fileName, int lineNumber) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = -1;
    }

    /** Con mensaje y la posicion completa. */
    public ScriptException(String message, String fileName, int lineNumber, int columnNumber) {
        super(message);
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
    }

    /**
     * El mensaje con la posicion pegada atras.
     *
     * <p>Sin archivo no se agrega nada: la linea y la columna solas no ubican. Con archivo se
     * agrega el archivo, y despues la linea y la columna que no sean -1, en ese orden.
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

    /** La linea donde paso, o -1. */
    public int getLineNumber() {
        return lineNumber;
    }

    /** La columna donde paso, o -1. */
    public int getColumnNumber() {
        return columnNumber;
    }

    /** El archivo donde paso, o nulo. */
    public String getFileName() {
        return fileName;
    }
}
