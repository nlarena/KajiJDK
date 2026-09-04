package javax.swing.event;

import java.awt.event.MouseAdapter;

/**
 * Un {@link MouseInputListener} con todos los metodos vacios.
 *
 * <p>Sirve para escribir uno solo: sin esto, atender nada mas que el clic obliga a escribir siete
 * metodos vacios. Es el patron adaptador de AWT aplicado a la union de los dos oyentes.
 *
 * <p>Hereda los cuerpos de {@link MouseAdapter} —que ya los tiene todos— y solo agrega la interfaz.
 * De ahi que el cuerpo de esta clase este vacio: no hay nada que escribir, y eso es el punto.
 */
public abstract class MouseInputAdapter extends MouseAdapter implements MouseInputListener {

    /** Para las subclases. */
    protected MouseInputAdapter() {
    }
}
