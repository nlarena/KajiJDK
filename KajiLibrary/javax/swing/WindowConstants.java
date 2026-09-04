package javax.swing;

/**
 * Que hacer cuando el usuario cierra una ventana.
 *
 * <p>Es una interfaz de solo constantes, y esta implementada por {@link JFrame}, `JDialog` e
 * `JInternalFrame` para que las constantes se puedan nombrar sin calificar desde una subclase. Es
 * un patron viejo --hoy se haria con un `enum`-- y sobrevive porque cambiarlo romperia todo lo
 * compilado contra el.
 *
 * <p>Las cuatro son excluyentes: la ventana hace una sola de estas cosas.
 */
public interface WindowConstants {

    /** No hacer nada: el programa decide, escuchando el evento de cierre. */
    int DO_NOTHING_ON_CLOSE = 0;

    /** Ocultarla. Sigue existiendo y se puede volver a mostrar. */
    int HIDE_ON_CLOSE = 1;

    /** Ocultarla y liberar sus recursos nativos. No se puede volver a mostrar sin recrearlos. */
    int DISPOSE_ON_CLOSE = 2;

    /**
     * Terminar el programa.
     *
     * <p>Solo para la ventana principal de una aplicacion: en un applet o en un componente
     * embebido se lleva puesto al que lo hospeda.
     */
    int EXIT_ON_CLOSE = 3;
}
