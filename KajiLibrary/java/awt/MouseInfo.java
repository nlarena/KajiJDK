package java.awt;

/**
 * Dónde está el puntero del mouse, sin necesidad de un evento.
 *
 * <p>Es la forma de preguntar por la posición del mouse **fuera** del reparto de eventos: un
 * {@link java.awt.event.MouseEvent} dice dónde estaba el puntero cuando pasó algo, y esto dice dónde
 * está ahora.
 *
 * <p>Sin pantalla no hay puntero, así que los dos métodos tiran {@link HeadlessException}. No hay
 * respuesta razonable: ni un (0,0) —que sería una posición inventada— ni un cero botones, que haría
 * creer que hay un mouse sin botones en vez de que no hay mouse.
 */
public class MouseInfo {

    /** No se instancia: es todo estático. */
    private MouseInfo() {
    }

    /**
     * Dónde está el puntero.
     *
     * @throws HeadlessException siempre: sin pantalla no hay puntero
     */
    public static PointerInfo getPointerInfo() throws HeadlessException {
        throw new HeadlessException();
    }

    /**
     * Cuántos botones tiene el mouse.
     *
     * @throws HeadlessException siempre, por lo mismo
     */
    public static int getNumberOfButtons() throws HeadlessException {
        throw new HeadlessException();
    }
}
