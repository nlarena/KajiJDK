package javax.print;

/**
 * KajiLibrary's javax.print.PrintException -- algo salio mal al imprimir.
 *
 * <p>La base de los errores del sistema de impresion. Las tres interfaces del paquete que empiezan con
 * mayuscula y terminan en {@code Exception} --{@link AttributeException}, {@link FlavorException} y
 * {@link URIException}-- <b>no</b> heredan de esta: son interfaces que una subclase de esta implementa
 * para explicar por que fallo.
 *
 * <p>Es un diseno raro y tiene razon: el motivo del fallo puede ser mas de uno a la vez --un atributo
 * no soportado <i>y</i> un formato no soportado-- y con herencia simple no se podria decir. Asi que
 * quien atrapa esto pregunta con {@code instanceof} por cada interfaz.
 *
 * <p>Los constructores toman {@link Exception} y no {@link Throwable}; es de 2001 y quedo asi.
 */
public class PrintException extends Exception {

    private static final long serialVersionUID = -5932531546705242471L;

    /** Sin detalle. */
    public PrintException() {
        super();
    }

    /** Con mensaje. */
    public PrintException(String s) {
        super(s);
    }

    /** Envolviendo otra. */
    public PrintException(Exception e) {
        super(e);
    }

    /** Con mensaje, envolviendo otra. */
    public PrintException(String s, Exception e) {
        super(s, e);
    }
}
