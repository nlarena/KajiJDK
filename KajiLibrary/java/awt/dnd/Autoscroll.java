package java.awt.dnd;

import java.awt.Insets;
import java.awt.Point;

/**
 * Lo implementa un componente que quiere desplazarse solo mientras le arrastran algo encima.
 *
 * <p>Resuelve un problema real: para soltar algo al final de una lista larga hay que llegar hasta
 * ahí, y con el botón apretado no se puede usar la barra de desplazamiento. La solución es que el
 * componente se desplace solo cuando el puntero se acerca a su borde.
 *
 * <p>Los márgenes de {@link #getAutoscrollInsets} son **desde afuera hacia adentro**: dicen a qué
 * distancia del borde empieza la zona sensible. Márgenes grandes hacen que el desplazamiento arranque
 * enseguida; chicos, que haya que ir casi hasta el borde.
 */
public interface Autoscroll {

    /** A qué distancia de cada borde empieza la zona que dispara el desplazamiento. */
    Insets getAutoscrollInsets();

    /** Desplaza un paso, según dónde esté el puntero. */
    void autoscroll(Point cursorLocn);
}
