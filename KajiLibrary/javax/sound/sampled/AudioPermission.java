package javax.sound.sampled;

import java.security.BasicPermission;

/**
 * KajiLibrary's javax.sound.sampled.AudioPermission -- permiso para usar el audio.
 *
 * <p>Dos nombres: {@code "play"} para reproducir y {@code "record"} para capturar. El segundo era el
 * que importaba -- un applet que pudiera abrir el microfono sin permiso es un problema evidente.
 *
 * <p>Marcada para eliminacion junto con todo el mecanismo de {@code SecurityManager}, que ya no
 * controla nada. Se mantiene para que el codigo viejo compile.
 */
@Deprecated(since = "24", forRemoval = true)
public class AudioPermission extends BasicPermission {

    private static final long serialVersionUID = -5518053473477801126L;

    /**
     * @param name {@code "play"}, {@code "record"} o {@code "*"}
     * @throws NullPointerException si es null
     * @throws IllegalArgumentException si esta vacio
     */
    public AudioPermission(String name) {
        super(name);
    }

    /**
     * Idem; las acciones no se usan.
     *
     * @param actions se ignora
     */
    public AudioPermission(String name, String actions) {
        super(name, actions);
    }
}
