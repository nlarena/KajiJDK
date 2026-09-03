package javax.naming;

/**
 * Se lanza cuando un nombre no respeta la sintaxis del espacio de nombres al que se lo manda.

 * <p>Es la unica de la familia que se lanza sin que haya un servicio de por medio: la tira el
 * parseo de `CompoundName` y `CompositeName`, y tambien `addAll` cuando le pasan un nombre de otra
 * clase o cuando se le quiere agregar un segundo componente a un nombre plano.
 *
 * <p>La jerarquia entera y el estado que arrastra estan explicados en `NamingException`.
 */
public class InvalidNameException extends NamingException {

    private static final long serialVersionUID = -8370672380823801105L;

    public InvalidNameException(String explanation) {
        super(explanation);
    }

    public InvalidNameException() {
        super();
    }
}
