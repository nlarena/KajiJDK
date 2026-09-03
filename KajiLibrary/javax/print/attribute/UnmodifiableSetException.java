package javax.print.attribute;

// La que tiran las vistas de solo lectura de `AttributeSetUtilities.unmodifiableView` cuando les
// piden modificar. Es un RuntimeException: no se declara, se documenta.
//
// Existe en vez de reusar `UnsupportedOperationException` porque el paquete se escribio antes de
// que esa fuera la convencion de las colecciones.
public class UnmodifiableSetException extends RuntimeException {

    private static final long serialVersionUID = 2255250308571511731L;

    public UnmodifiableSetException() {
        super();
    }

    public UnmodifiableSetException(String message) {
        super(message);
    }
}
