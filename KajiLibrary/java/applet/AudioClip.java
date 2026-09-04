package java.applet;

/**
 * Un sonido que se puede reproducir, repetir y parar.
 *
 * <p>Es la interfaz de sonido más vieja de Java —anterior a `javax.sound`— y por eso no dice nada
 * del formato ni del volumen: sólo tres verbos. {@link #loop} no es "reproducir varias veces" sino
 * "reproducir hasta que alguien pare", que es la diferencia entre una música de fondo y un aviso.
 *
 * @deprecated el modelo de applets está en desuso desde Java 9 y marcado para borrarse desde 17; el
 *     sonido se maneja con `javax.sound.sampled`.
 */
@Deprecated(since = "9", forRemoval = true)
public interface AudioClip {

    /** Lo reproduce una vez desde el principio; si ya sonaba, arranca de nuevo. */
    void play();

    /** Lo reproduce en bucle hasta que alguien llame a {@link #stop}. */
    void loop();

    /** Lo para, esté sonando una vez o en bucle. */
    void stop();
}
