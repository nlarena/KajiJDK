package javax.sound.midi;

import java.util.EventListener;

/**
 * KajiLibrary's javax.sound.midi.MetaEventListener -- avisa cuando el secuenciador pasa por un meta
 * evento.
 *
 * <p>A diferencia de {@link ControllerEventListener}, este no lleva filtro: llegan todos.
 *
 * <p>Su uso mas comun es detectar el <b>fin de la obra</b>: el meta evento de tipo 0x2F. Es la unica
 * forma limpia de saber que un secuenciador termino, porque {@code start()} vuelve enseguida.
 *
 * <p>El aviso llega en el hilo del secuenciador. Bloquearlo desacomoda la reproduccion.
 */
public interface MetaEventListener extends EventListener {

    /** Paso un meta evento. */
    void meta(MetaMessage meta);
}
