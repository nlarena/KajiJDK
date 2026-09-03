package javax.naming;

import java.util.Hashtable;

/**
 * "Eso no lo tengo yo, preguntale a aquel": una referencia a otro servidor, lanzada como excepcion.
 *
 * <p>Un referral no es un error, es una **redireccion**. LDAP la usa todo el tiempo: el servidor
 * contesta "esa rama vive en `ldap://otro/...`" y el cliente decide si sigue. Por eso la clase
 * tiene metodos que no tiene ninguna otra excepcion del paquete: `getReferralContext()` devuelve
 * el contexto del otro servidor, `skipReferral()` descarta este y pasa al siguiente, y
 * `retryReferral()` vuelve a intentar el mismo --el caso tipico es reintentar despues de haber
 * cambiado las credenciales--.
 *
 * <p><strong>Es abstracta, y eso es lo que la hace honesta aca.</strong> Seguir un referral es
 * abrir una conexion a otro servidor, y eso solo lo puede hacer un proveedor. La clase declara la
 * forma del contrato --que es lo que un proveedor tiene que implementar-- y no promete cumplirlo:
 * sin proveedores instalados en este JDK nadie la extiende y nadie la lanza. Un `catch` que la
 * nombre compila y es correcto; simplemente nunca se ejecuta.
 *
 * <p>El resto de la jerarquia esta explicado en `NamingException`.
 */
public abstract class ReferralException extends NamingException {

    private static final long serialVersionUID = -2881363844695698876L;

    protected ReferralException(String explanation) {
        super(explanation);
    }

    protected ReferralException() {
        super();
    }

    /** La informacion cruda del referral, en la forma que use el proveedor (una URL, tipicamente). */
    public abstract Object getReferralInfo();

    /**
     * El contexto por el que se sigue, ya apuntando al otro servidor.
     *
     * <p>La operacion que fallo hay que volver a pedirla sobre este contexto: la excepcion trae la
     * redireccion, no el resultado.
     */
    public abstract Context getReferralContext() throws NamingException;

    /** Igual que el otro, pero con un entorno propio --el caso de reintentar con otras credenciales--. */
    public abstract Context getReferralContext(Hashtable<?, ?> env) throws NamingException;

    /**
     * Descarta este referral y pasa al siguiente si lo hay.
     *
     * <p>Devuelve si quedan mas. Un servidor puede contestar varios y el cliente probarlos en
     * orden hasta que uno ande.
     */
    public abstract boolean skipReferral();

    /**
     * Deja el mismo referral listo para reintentarse.
     *
     * <p>No reintenta: **prepara**. El que llama despues pide de nuevo `getReferralContext()`.
     */
    public abstract void retryReferral();
}
