package javax.management;

/**
 * Al evaluar una consulta, el atributo tenia un valor de un tipo que la expresion no sabe comparar.
 *
 * <p>El valor se guarda ya convertido a `String` en el constructor y no como el objeto original.
 * Esa conversion temprana es deliberada en el JDK: la excepcion es serializable y guardar el objeto
 * arbitrario del usuario obligaria a deserializarlo del otro lado.
 *
 * <p>Ojo con {@link #toString()}: dice {@code "BadAttributeValueException"}, sin el {@code Exp} que
 * si lleva el nombre de la clase. Es una rareza del JDK que se conserva porque hay codigo que la
 * compara.
 */
public class BadAttributeValueExpException extends Exception {

    private static final long serialVersionUID = -3105272988410493376L;

    /**
     * @serial el valor ofensivo, ya como cadena
     */
    private String val;

    /** @param val el valor que no se pudo usar; se guarda su `toString()` */
    public BadAttributeValueExpException(Object val) {
        this.val = val == null ? null : val.toString();
    }

    /** Ver la nota de la clase: dice {@code BadAttributeValueException}, sin {@code Exp}. */
    public String toString() {
        return "BadAttributeValueException: " + val;
    }
}
