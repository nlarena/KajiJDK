package javax.swing.event;

import java.util.EventObject;

/**
 * Algo cambio, y no dice que.
 *
 * <p>Es el evento mas usado de Swing y el mas pobre a proposito: solo lleva su origen. La idea
 * es que quien escucha ya tiene el objeto que cambio y puede preguntarle lo que necesite — el
 * evento no tiene por que adivinar cual de sus propiedades le interesa.
 *
 * <p>Esa pobreza tiene una consecuencia practica: como no lleva datos, una misma instancia sirve
 * para todos los avisos de un objeto. Casi todo Swing crea uno y lo reusa para siempre.
 */
public class ChangeEvent extends EventObject {

    private static final long serialVersionUID = 1L;

    /** @param source el objeto que cambio */
    public ChangeEvent(Object source) {
        super(source);
    }
}
