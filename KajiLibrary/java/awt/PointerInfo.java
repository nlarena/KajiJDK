package java.awt;

/**
 * Dónde está el puntero del mouse y en qué pantalla.
 *
 * <p>Es una **foto**, no un seguimiento: los datos son los del instante en que
 * {@link MouseInfo#getPointerInfo} la sacó, y no se actualizan solos. Por eso no tiene forma de
 * construirse desde afuera.
 */
public final class PointerInfo {

    /** En qué pantalla estaba. */
    private final GraphicsDevice device;

    /** Dónde estaba, en coordenadas de esa pantalla. */
    private final Point location;

    /** La arma {@link MouseInfo}; nadie más. */
    PointerInfo(GraphicsDevice device, Point location) {
        this.device = device;
        this.location = location;
    }

    /** La pantalla donde estaba el puntero. */
    public GraphicsDevice getDevice() {
        return this.device;
    }

    /**
     * Dónde estaba.
     *
     * @return una copia; mover el punto devuelto no mueve nada
     */
    public Point getLocation() {
        return new Point(this.location);
    }
}
