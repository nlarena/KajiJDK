package java.lang;

// KajiLibrary's java.lang.Exception — the superclass of conditions a program may want to catch.
public class Exception extends Throwable {

    public Exception() {
    }

    public Exception(String message) {
        super(message);
    }

    public Exception(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * El constructor con los dos interruptores de `Throwable`: la supresion y la escritura del
     * stack trace. Es `protected` porque solo tiene sentido para una subclase que quiera una
     * excepcion **barata** -- una que se lanza como senal de control muchas veces y cuya pila nadie
     * va a mirar.
     */
    protected Exception(String message, Throwable cause, boolean enableSuppression,
            boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public Exception(Throwable cause) {
        super(cause);
    }
}
