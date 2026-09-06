package jdk.swing.interop;

import java.awt.AWTEvent;
import java.awt.EventQueue;
import java.awt.Toolkit;
import java.awt.Window;

/**
 * Las cuatro cosas sueltas que el puente con otro juego de herramientas graficas necesita del
 * interior de AWT.
 *
 * <h2>Tomar el mouse</h2>
 *
 * <p>{@link #grab} es lo que hace un menu emergente al abrirse: pide que todos los eventos del
 * mouse le lleguen a el, incluso los que caen sobre otra ventana. Sin eso, hacer clic afuera no
 * cerraria el menu, porque el clic se lo llevaria la ventana de abajo y el menu nunca se enteraria.
 *
 * <p>{@link #isUngrabEvent} es la otra mitad: reconoce el evento con el que el sistema avisa que se
 * perdio esa toma, para que el menu se cierre solo.
 *
 * <h2>Estado en esta biblioteca</h2>
 *
 * <p>{@link #postEvent} funciona: encola el evento en la cola del sistema. {@link #grab} y
 * {@link #ungrab} no hacen nada, que es exactamente lo que hacen en el JDK cuando el juego de
 * herramientas no es el de Sun --tomar el mouse es una operacion del sistema de ventanas, y no hay
 * ninguno--. {@link #isUngrabEvent} da siempre falso por la misma razon: ese evento lo fabrica el
 * sistema, y aca no lo fabrica nadie.
 *
 * @since 9
 */
public class SwingInterOpUtils {

    /**
     * La marca del evento con que el sistema avisa que se perdio la toma del mouse.
     *
     * <p>Es el bit de arriba de todo del entero, el unico que quedaba libre: las mascaras de eventos
     * de AWT se fueron repartiendo los demas.
     */
    public static final int GRAB_EVENT_MASK = 0x80000000;

    /** Uno. */
    public SwingInterOpUtils() {
    }

    /**
     * Encola el evento.
     *
     * <p>El primer argumento es el contexto de aplicacion al que mandarlo. Esta implementacion tiene
     * una sola cola de eventos, asi que no hay a que otro contexto mandarlo y el argumento se
     * ignora.
     *
     * @param targetAppContext a que contexto de aplicacion, o {@code null}
     * @param event el evento, o {@code null} para no hacer nada
     */
    public static void postEvent(Object targetAppContext, AWTEvent event) {
        if (event == null) {
            return;
        }
        final EventQueue cola = Toolkit.getDefaultToolkit().getSystemEventQueue();
        if (cola != null) {
            cola.postEvent(event);
        }
    }

    /**
     * Hace que todos los eventos del mouse vayan a esa ventana.
     *
     * @param toolkit el juego de herramientas
     * @param w la ventana que se lleva los eventos
     */
    public static void grab(Toolkit toolkit, Window w) {
    }

    /**
     * Devuelve los eventos del mouse a quien corresponda.
     *
     * @param toolkit el juego de herramientas
     * @param w la ventana que los tenia
     */
    public static void ungrab(Toolkit toolkit, Window w) {
    }

    /**
     * Si ese es el evento con que el sistema avisa que se perdio la toma del mouse.
     *
     * @param ev el evento
     * @return falso siempre: ese evento lo fabrica el sistema de ventanas, y no hay ninguno
     */
    public static boolean isUngrabEvent(AWTEvent ev) {
        return false;
    }
}
