package java.awt;

/**
 * Un modo de video: ancho, alto, bits por pixel y frecuencia de refresco.
 *
 * <p>Cuatro enteros inmutables. Dos de ellos tienen un valor especial que significa "no aplica" y
 * no son intercambiables: {@code BIT_DEPTH_MULTI} vale -1 y dice que el dispositivo acepta varias
 * profundidades a la vez, mientras que {@code REFRESH_RATE_UNKNOWN} vale 0 y dice que la
 * frecuencia no se pudo averiguar. Un 0 en la profundidad seria una profundidad de cero bits, no un
 * "no se".
 */
public final class DisplayMode {

    public static final int BIT_DEPTH_MULTI = -1;

    public static final int REFRESH_RATE_UNKNOWN = 0;

    private Dimension size;

    private int bitDepth;

    private int refreshRate;

    public DisplayMode(int width, int height, int bitDepth, int refreshRate) {
        this.size = new Dimension(width, height);
        this.bitDepth = bitDepth;
        this.refreshRate = refreshRate;
    }

    public int getHeight() {
        return size.height;
    }

    public int getWidth() {
        return size.width;
    }

    public int getBitDepth() {
        return bitDepth;
    }

    public int getRefreshRate() {
        return refreshRate;
    }

    /** Sobrecarga tipada: evita el casteo cuando ya se sabe que el otro es un DisplayMode. */
    public boolean equals(DisplayMode dm) {
        if (dm == null) {
            return false;
        }
        return (getHeight() == dm.getHeight()
                && getWidth() == dm.getWidth()
                && getBitDepth() == dm.getBitDepth()
                && getRefreshRate() == dm.getRefreshRate());
    }

    public boolean equals(Object dm) {
        if (dm instanceof DisplayMode) {
            return equals((DisplayMode) dm);
        } else {
            return false;
        }
    }

    /**
     * Los pesos 7 y 13 son primos y distintos a proposito: sin ellos, 800x600 y 600x800 tendrian el
     * mismo hash, y lo mismo un modo con profundidad y frecuencia intercambiadas.
     */
    public int hashCode() {
        return getWidth() + getHeight() + getBitDepth() * 7 + getRefreshRate() * 13;
    }

    public String toString() {
        return getWidth() + "x" + getHeight() + "x"
                + (getBitDepth() == BIT_DEPTH_MULTI ? "[Multi depth]" : getBitDepth() + "bpp")
                + "@"
                + (getRefreshRate() == REFRESH_RATE_UNKNOWN
                        ? "[Unknown refresh rate]" : getRefreshRate() + "Hz");
    }
}
