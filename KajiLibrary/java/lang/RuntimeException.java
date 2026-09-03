package java.lang;

// KajiLibrary's java.lang.RuntimeException — the superclass of unchecked exceptions.
public class RuntimeException extends Exception {

    public RuntimeException() {
    }

    public RuntimeException(String message) {
        super(message);
    }

    public RuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * El constructor con los dos interruptores de `Throwable`: la supresion y la escritura del
     * stack trace. Es `protected` porque solo tiene sentido para una subclase que quiera una
     * excepcion **barata** -- una que se lanza como senal de control muchas veces y cuya pila nadie
     * va a mirar.
     */
    protected RuntimeException(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public RuntimeException(Throwable cause) {
        super(cause);
    }
}
