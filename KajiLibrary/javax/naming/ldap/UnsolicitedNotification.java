package javax.naming.ldap;

import javax.naming.NamingException;

/**
 * Un mensaje que el servidor manda sin que nadie se lo haya pedido.
 *
 * <h2>Por que existe</h2>
 *
 * <p>Porque el servidor a veces tiene algo que decir que no es respuesta de nada: que va a cerrar la
 * conexion, que la sesion cambio de estado, que hubo un aviso. Sin este mecanismo tendria que
 * esperar a la proxima operacion para contarlo, o simplemente cortar.
 *
 * <p>Extiende {@link ExtendedResponse} —es una respuesta con OID y datos— y {@link HasControls},
 * porque puede traer controles.
 *
 * <p>El caso mas conocido es el <em>Notice of Disconnection</em>: el servidor avisa por que va a
 * cerrar, y sin esto la conexion simplemente se caeria sin explicacion.
 */
public interface UnsolicitedNotification extends ExtendedResponse, HasControls {

    /** Las URLs a las que redirige, o {@code null} si no redirige. */
    String[] getReferrals();

    /** El error que reporta, o {@code null} si no es un error. */
    NamingException getException();
}
