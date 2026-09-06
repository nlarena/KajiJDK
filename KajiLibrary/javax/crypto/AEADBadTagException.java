package javax.crypto;

/**
 * La etiqueta de autenticacion no coincide.
 *
 * <p>En un cifrado autenticado --GCM y los suyos-- cada mensaje viaja con una etiqueta que prueba
 * que nadie lo toco. Si no coincide, lo descifrado se descarta entero: no se entrega ni una parte,
 * porque una parte de un mensaje alterado es un mensaje alterado.
 *
 * <p>Es una {@link BadPaddingException} por herencia historica, no porque tenga que ver con el
 * relleno: los cifrados autenticados llegaron despues y se colgaron del tipo que ya atrapaba todo
 * el mundo.
 *
 * @since 1.7
 */
public class AEADBadTagException extends BadPaddingException {

    private static final long serialVersionUID = 1L;

    /** Una sin mensaje. */
    public AEADBadTagException() {
        super();
    }

    /**
     * Una con mensaje.
     *
     * @param msg que paso
     */
    public AEADBadTagException(String msg) {
        super(msg);
    }
}
