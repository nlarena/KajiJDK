package javax.management;

/**
 * Se pidio una operacion de cadena que no existe dentro de una consulta.
 *
 * <p>Cuelga de `Exception` y no de {@link JMException}: las cuatro excepciones del subsistema de
 * consultas son anteriores al arbol de JMX y quedaron sueltas.
 */
public class BadStringOperationException extends Exception {

    private static final long serialVersionUID = 7802201238441662100L;

    /**
     * @serial la operacion que no se reconocio
     */
    private String op;

    /** @param message la operacion que no se reconocio */
    public BadStringOperationException(String message) {
        op = message;
    }

    /** Nombre de la clase seguido de la operacion ofensiva. */
    public String toString() {
        return "BadStringOperationException: " + op;
    }
}
