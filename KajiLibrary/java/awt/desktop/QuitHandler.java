package java.awt.desktop;

/**
 * KajiLibrary's java.awt.desktop.QuitHandler -- decide si el programa se cierra.
 *
 * <p>Se registra con {@code Desktop.setQuitHandler}. Es el unico manejador del paquete que puede
 * <b>negarse</b>, y por eso su metodo recibe un {@link QuitResponse} aparte.
 *
 * <p>Ver ahi por que la respuesta no es el valor de retorno.
 */
public interface QuitHandler {

    /**
     * El sistema quiere cerrar el programa.
     *
     * <p>Hay que llamar a {@code performQuit} o a {@code cancelQuit} sobre la respuesta, tarde o
     * temprano. No llamar a ninguno deja al sistema esperando.
     */
    void handleQuitRequestWith(QuitEvent e, QuitResponse response);
}
