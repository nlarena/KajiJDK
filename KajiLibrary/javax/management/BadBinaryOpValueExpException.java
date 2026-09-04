package javax.management;

/**
 * Se aplico un operador binario a una expresion que no lo admite.
 *
 * <p>A diferencia de {@link BadAttributeValueExpException}, esta si conserva la expresion entera:
 * un {@link ValueExp} es del propio JMX y serializarlo no arrastra tipos del usuario.
 */
public class BadBinaryOpValueExpException extends Exception {

    private static final long serialVersionUID = 5068475589449021227L;

    /**
     * @serial la expresion ofensiva
     */
    private ValueExp exp;

    /** @param exp la expresion a la que no se le pudo aplicar el operador */
    public BadBinaryOpValueExpException(ValueExp exp) {
        this.exp = exp;
    }

    /** La expresion ofensiva. */
    public ValueExp getExp() {
        return exp;
    }

    public String toString() {
        return "BadBinaryOpValueExpException: " + exp;
    }
}
