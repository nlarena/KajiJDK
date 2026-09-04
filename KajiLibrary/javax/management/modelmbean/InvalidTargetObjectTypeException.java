package javax.management.modelmbean;

/**
 * KajiLibrary's javax.management.modelmbean.InvalidTargetObjectTypeException -- el recurso
 * administrado no es de un tipo que este MBean sepa manejar.
 *
 * <p>La lanza {@code setManagedResource} cuando el segundo argumento --el <b>tipo</b> de referencia--
 * no es uno de los que la implementacion soporta. El unico que
 * {@link RequiredModelMBean} soporta es {@code "ObjectReference"}: una referencia Java directa.
 *
 * <p>Los otros tipos que la especificacion nombra --{@code "Handle"}, {@code "IOR"},
 * {@code "EJBHandle"}, {@code "RMIReference"}-- son de un mundo que ya no existe. Que no esten
 * soportados no es una limitacion de esta biblioteca sino del JDK, que tampoco los soporta.
 */
public class InvalidTargetObjectTypeException extends Exception {

    private static final long serialVersionUID = 1190536278266811217L;

    /** Sin detalle. */
    public InvalidTargetObjectTypeException() {
        super("Invalid target object type exception");
    }

    /** Con el tipo que se paso. */
    public InvalidTargetObjectTypeException(String s) {
        super("Invalid target object type exception: " + s);
    }

    /** Con la causa y un mensaje. */
    public InvalidTargetObjectTypeException(Exception e, String s) {
        super("Invalid target object type exception: " + s
            + ((e == null) ? "" : " " + e.toString()));
    }
}
