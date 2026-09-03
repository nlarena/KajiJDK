package java.nio.file;

import java.io.IOException;
import java.util.ConcurrentModificationException;
import java.util.NoSuchElementException;

// Una `IOException` que aparecio en medio de un `for` sobre un `DirectoryStream`.
//
// **Por que hereda de `ConcurrentModificationException` y no de algo de I/O.** El iterador de
// `DirectoryStream` implementa `Iterator`, cuyos metodos no declaran `IOException`; la unica salida
// es una excepcion no chequeada. `ConcurrentModificationException` es la que ya significa "el
// recorrido se interrumpio por algo de afuera", que es exactamente el caso.
//
// **La causa esta acotada a `IOException`**, y el constructor la exige: envolver un
// `RuntimeException` en esto no diria nada, porque ese ya se propaga solo.
//
// KajiJDK nunca la levanta -- no hay `DirectoryStream` que funcione, ver `Files.newDirectoryStream`.
public final class DirectoryIteratorException extends ConcurrentModificationException {

    private static final long serialVersionUID = -6012699886086212874L;

    // La causa, guardada aparte de la que ya lleva `Throwable`. Es redundante en el JDK --alla
    // `getCause()` es `super.getCause()` con un cast-- pero esta VM tiene un bug con
    // `invokespecial` sobre un metodo que la superclase nombrada *hereda* en vez de declarar
    // (`getCause()` se declara en `Throwable`, no en `ConcurrentModificationException`): revienta
    // con `getfield: bad FieldRef`. Un campo propio da el mismo resultado sin depender de eso.
    private final IOException causa;

    /**
     * @param cause la `IOException` que corto el recorrido
     * @throws NullPointerException si `cause` es `null`
     */
    public DirectoryIteratorException(IOException cause) {
        super(cause);
        if (cause == null) {
            throw new NullPointerException();
        }
        this.causa = cause;
    }

    /**
     * La causa, ya con el tipo estrecho.
     *
     * <p>Devolver `IOException` y no `Throwable` es el punto de la clase: quien la atrapa quiere
     * relanzar la de I/O original sin castear.
     */
    public IOException getCause() {
        return this.causa;
    }
}
