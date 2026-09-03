package java.awt.image;

import java.awt.BufferCapabilities;
import java.awt.Graphics;

/**
 * Cómo se manejan los buffers de dibujo de una superficie.
 *
 * <p>Resuelve el parpadeo. Dibujar directo sobre lo que se está mostrando deja ver la construcción
 * del cuadro; con varios buffers se dibuja en uno oculto y se lo muestra entero de una vez, con
 * {@link #show}.
 *
 * <p>Los dos métodos de "contenido" distinguen dos desgracias distintas. {@link #contentsLost} dice
 * que lo que se dibujó **no llegó** a mostrarse; {@link #contentsRestored} dice que el buffer se
 * recuperó pero quedó vacío y hay que rehacer el cuadro. Como en {@link VolatileImage}, el bucle
 * correcto los consulta después de mostrar y no antes.
 */
public abstract class BufferStrategy {

    /** Para las subclases. */
    protected BufferStrategy() {
    }

    /** Qué buffers hay y qué se puede hacer con ellos. */
    public abstract BufferCapabilities getCapabilities();

    /**
     * Un contexto para dibujar en el buffer oculto.
     *
     * <p>Hay que pedir uno nuevo por cuadro y soltarlo al terminar: el buffer que estaba oculto pasa
     * a estar a la vista en cada {@link #show}.
     */
    public abstract Graphics getDrawGraphics();

    /** Si lo que se dibujó desde la última llamada se perdió sin llegar a mostrarse. */
    public abstract boolean contentsLost();

    /** Si el buffer se recuperó vacío y hay que volver a dibujar el cuadro. */
    public abstract boolean contentsRestored();

    /** Muestra el buffer oculto. */
    public abstract void show();

    /**
     * Suelta los recursos.
     *
     * <p>No hace nada acá: una estrategia sin recursos propios no tiene qué soltar.
     */
    public void dispose() {
    }
}
