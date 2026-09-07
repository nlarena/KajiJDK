package javax.swing;

import java.awt.Graphics2D;

/**
 * Algo que sabe dibujarse en un rectangulo.
 *
 * <h2>Por que no es simplemente un {@code Icon}</h2>
 *
 * <p>Un icono tiene tamano propio y se dibuja en una posicion. Un pintor no: recibe el ancho y el
 * alto en cada llamada y se adapta. Esa es toda la diferencia, y es la que hace falta para pintar el
 * fondo de un componente que cambia de tamano.
 *
 * <p>El objeto que se le pasa es lo que se esta pintando --normalmente el componente-- para que el
 * pintor pueda mirarle el estado: si esta apretado, si tiene el foco, si esta deshabilitado.
 *
 * @param <T> lo que se pinta
 * @since 1.7
 */
public interface Painter<T> {

    /**
     * Dibuja en ese rectangulo.
     *
     * @param g donde dibujar; el pintor lo puede modificar sin restaurarlo
     * @param object lo que se esta pintando, o {@code null}
     * @param width el ancho
     * @param height el alto
     */
    void paint(Graphics2D g, T object, int width, int height);
}
