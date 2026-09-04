package java.security;

// Envuelve la excepcion chequeada que tiro un `PrivilegedExceptionAction`.
//
// Existe por un problema de tipos y no de seguridad: `AccessController.doPrivileged` no puede
// declarar `throws` de algo que depende de la accion que le pasen, asi que envuelve lo que salga
// en esta y declara solo esta. El `run()` de la accion declara `throws Exception`, y este es el
// sobre en el que llega del otro lado.
//
// Deprecada en el JDK junto con todo el mecanismo de privilegios, que ya no gobierna nada desde
// que el `SecurityManager` quedo deshabilitado. Se implementa porque sigue siendo el tipo que
// aparece en las firmas.
@Deprecated
public class PrivilegedActionException extends Exception {

    // La excepcion envuelta. Se guarda aparte de la causa de `Throwable` porque este tipo es
    // anterior al encadenamiento de causas y su serializacion tiene el campo propio.
    private final Exception exception;

    public PrivilegedActionException(Exception exception) {
        super((Throwable) null);
        this.exception = exception;
    }

    // La excepcion que tiro la accion.
    public Exception getException() {
        return this.exception;
    }

    @Override
    public Throwable getCause() {
        return this.exception;
    }

    @Override
    public String toString() {
        String s = this.getClass().getName();
        if (this.exception != null) {
            return s + ": " + this.exception.toString();
        }
        return s;
    }
}
