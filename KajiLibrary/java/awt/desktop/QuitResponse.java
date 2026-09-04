package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.QuitResponse -- la respuesta a un pedido de cierre.
 *
 * <p>Existe porque la decision de cerrar puede tardar. Si {@link QuitHandler} devolviera un booleano,
 * habria que decidir <b>en el momento</b>; con esto, el manejador puede mostrar un "guardar cambios",
 * volver enseguida, y responder cuando el usuario conteste.
 *
 * <p>Lo que hay que cumplir es simple y se olvida: <b>alguno de los dos metodos tiene que llamarse</b>.
 * Si no, el sistema queda esperando una respuesta que no llega, y en varios escritorios eso deja el
 * dialogo de apagado colgado.
 *
 * <p>{@link #performQuit} no vuelve: cierra el programa.
 */
public interface QuitResponse {

    /** Adelante. No vuelve. */
    void performQuit();

    /** No cerrar. */
    void cancelQuit();
}
