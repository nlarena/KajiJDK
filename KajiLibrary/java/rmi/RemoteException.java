package java.rmi;

import java.io.IOException;

/**
 * KajiLibrary's java.rmi.RemoteException -- fallo una llamada remota.
 *
 * <p>La base de casi todo lo que puede salir mal en RMI, y la que <b>todo</b> metodo de una interfaz
 * {@link Remote} tiene que declarar. Es comprobada a proposito: obliga a que quien escribe el cliente
 * se haga cargo de que la red existe.
 *
 * <h2>Lo que no se sabe cuando salta</h2>
 *
 * <p>Es lo importante de esta clase y casi nunca se tiene en cuenta: en general <b>no se sabe si el
 * metodo remoto llego a ejecutarse</b>. Si la conexion se corto despues de enviar la llamada y antes
 * de recibir la respuesta, la operacion pudo haberse hecho igual.
 *
 * <p>Por eso reintentar a ciegas es peligroso, y por eso las operaciones remotas conviene disenarlas
 * idempotentes. Las unicas de las que si se sabe son {@link MarshalException} --no salio-- y
 * {@link NoSuchObjectException} --no existe--.
 *
 * <h2>El campo {@link #detail} y la causa</h2>
 *
 * <p>{@code detail} es publico y es de 1996, anterior al mecanismo de causas encadenadas. Cuando
 * llego, en 1.4, se hizo que {@link #getCause} lo devuelva, asi que <b>son lo mismo</b>.
 *
 * <p>Eso trae dos consecuencias: {@link #getMessage} pega el mensaje del detalle al propio, y
 * {@code initCause} lanza {@link IllegalStateException} porque el constructor ya la fijo -- aunque se
 * haya construido sin causa.
 */
public class RemoteException extends IOException {

    private static final long serialVersionUID = -5148567311918794206L;

    /**
     * La excepcion original, si la hay.
     *
     * <p>Publico por compatibilidad; lo mismo que devuelve {@link #getCause}.
     */
    public Throwable detail;

    /** Sin detalle. */
    public RemoteException() {
        initCause(null);
    }

    /** @param s el mensaje */
    public RemoteException(String s) {
        super(s);
        initCause(null);
    }

    /**
     * @param s el mensaje
     * @param cause la original
     */
    public RemoteException(String s, Throwable cause) {
        super(s);
        initCause(null);
        this.detail = cause;
    }

    /** El mensaje propio, y el del detalle debajo si lo hay. */
    @Override
    public String getMessage() {
        if (this.detail == null) {
            return super.getMessage();
        }
        return super.getMessage() + "; nested exception is: \n\t" + this.detail.toString();
    }

    /** El detalle. Ver la nota de la clase. */
    @Override
    public Throwable getCause() {
        return this.detail;
    }
}
