package javax.sound.midi;

import java.util.EventListener;

/**
 * KajiLibrary's javax.sound.midi.ControllerEventListener -- avisa cuando el secuenciador pasa por un
 * cambio de controlador.
 *
 * <p>Se registra con {@code Sequencer.addControllerEventListener}, que ademas recibe <b>que
 * controladores</b> interesan. Sin ese filtro un archivo con automatizacion genera cientos de eventos
 * por segundo.
 *
 * <p>Ese metodo devuelve el arreglo de los que <b>efectivamente</b> quedaron registrados, que puede
 * ser mas chico que el que se pidio. Hay que mirarlo: pedir un controlador que el secuenciador no
 * sigue no falla, simplemente no llega.
 *
 * <p>El aviso llega en el hilo del secuenciador. Bloquearlo desacomoda la reproduccion.
 */
public interface ControllerEventListener extends EventListener {

    /** Paso un cambio de controlador de los que se pidieron. */
    void controlChange(ShortMessage event);
}
