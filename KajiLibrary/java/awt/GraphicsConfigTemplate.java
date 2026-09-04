package java.awt;

import java.io.Serializable;

/**
 * Lo que se le pide a una configuración gráfica, para que el sistema elija la que mejor cumpla.
 *
 * <p>En vez de enumerar las configuraciones y comparar a mano, se declara qué hace falta —doble
 * buffer sí, estéreo no— y el dispositivo devuelve la más adecuada. Cada requisito puede ser
 * obligatorio, deseable o indeseable, y esa gradación es la que permite ordenar candidatas en vez de
 * sólo aceptarlas o rechazarlas.
 */
public abstract class GraphicsConfigTemplate implements Serializable {

    private static final long serialVersionUID = -8061369279557787079L;

    /** El requisito tiene que cumplirse. */
    public static final int REQUIRED = 1;

    /** Mejor si se cumple. */
    public static final int PREFERRED = 2;

    /** Mejor si no se cumple. */
    public static final int UNNECESSARY = 3;

    /** Para las subclases. */
    public GraphicsConfigTemplate() {
    }

    /**
     * La mejor de esas configuraciones, o `null` si ninguna sirve.
     *
     * @throws NullPointerException si el arreglo es `null`
     */
    public abstract GraphicsConfiguration getBestConfiguration(GraphicsConfiguration[] gc);

    /**
     * Si esa configuración cumple los requisitos obligatorios.
     *
     * @throws NullPointerException si la configuración es `null`
     */
    public abstract boolean isGraphicsConfigSupported(GraphicsConfiguration gc);
}
