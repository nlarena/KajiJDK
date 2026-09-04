package javax.sound.sampled;

import java.util.EventListener;

/**
 * KajiLibrary's javax.sound.sampled.LineListener -- escucha los cambios de estado de una linea.
 *
 * <p>Un solo metodo para los cuatro eventos; el tipo se lee del {@link LineEvent}.
 *
 * <p>Es la unica forma de saber que un clip termino de sonar: {@code Clip.start()} vuelve enseguida y
 * la reproduccion sigue en otro hilo. Esperar con pausas es lo que hace casi todo el mundo y siempre
 * queda mal.
 *
 * <p>El aviso llega en un hilo del sistema de audio, no en el que pidio la operacion. Bloquearlo
 * retrasa el audio de todo el programa.
 */
public interface LineListener extends EventListener {

    /** Algo cambio en una linea. */
    void update(LineEvent event);
}
