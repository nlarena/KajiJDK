package java.net;

/**
 * KajiLibrary's java.net.SocketAddress — una dirección de socket, sin decir de qué protocolo.
 *
 * <p>**No tiene ningún método, y eso es lo que es.** Existe para que las firmas que hablan de "una
 * dirección" no tengan que nombrar un protocolo concreto: `bind(SocketAddress)` sirve igual para una
 * dirección IP con puerto que para una ruta de socket de dominio Unix. La forma de la dirección la
 * pone la subclase.
 *
 * <p>Es abstracta y sin miembros a propósito: un tipo que sólo aporta un nombre común. Que no se
 * pueda hacer nada con una es correcto — lo único que hace falta es poder pasarla.
 */
public abstract class SocketAddress implements java.io.Serializable {

    private static final long serialVersionUID = 5215720748342549866L;

    public SocketAddress() {
    }
}
