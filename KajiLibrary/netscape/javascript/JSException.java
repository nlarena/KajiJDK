package netscape.javascript;

/**
 * Lo que sale cuando el motor de JavaScript del otro lado falla.
 *
 * <p>Es una {@link RuntimeException} y no una chequeada, y eso no es descuido: los metodos de
 * {@link JSObject} nombran un miembro o evaluan una expresion <em>por su nombre en texto</em>, asi
 * que cualquiera de ellos puede fallar por razones que el compilador de Java no tiene forma de ver.
 * Obligar a declararla en cada llamada no agregaria seguridad, solo ruido.
 *
 * @deprecated el puente de applets a JavaScript quedo sin usos cuando el modelo de applets entro en
 *     desuso. Vive todavia porque {@link JSObject} lo nombra en cada firma.
 */
@Deprecated(since = "9", forRemoval = true)
public class JSException extends RuntimeException {

    private static final long serialVersionUID = -7132931832235736974L;

    /** Sin detalle: el motor fallo y no dijo por que. */
    public JSException() {
        super();
    }

    /** Con el mensaje que dio el motor. */
    public JSException(String s) {
        super(s);
    }

    /**
     * Envolviendo lo que realmente fallo.
     *
     * <p>La causa se conserva entera: si el motor tiro algo propio, esta abajo y se lee con
     * {@link Throwable#getCause}.
     */
    public JSException(Throwable cause) {
        super(cause);
    }
}
