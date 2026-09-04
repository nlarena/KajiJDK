package javax.swing.plaf;

import java.awt.Component$BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.accessibility.Accessible;
import javax.swing.JComponent;

/**
 * La mitad "aspecto" de un componente: quien lo dibuja y lo mide, separado de quien lo modela.
 *
 * <h2>La idea que sostiene a todo Swing</h2>
 *
 * <p>Un {@link JComponent} sabe que es —un boton, una tabla— pero no como se ve. Eso lo sabe su
 * {@code ComponentUI}, que se puede cambiar en caliente: es lo que permite que la misma aplicacion se
 * vea como Windows, como Metal o como lo que un aspecto nuevo decida, sin tocar el modelo. De ahi
 * que todos los metodos reciban el componente como parametro: <strong>un UI no guarda al
 * componente</strong>, y un mismo UI puede atender a varios.
 *
 * <h2>{@link #update} contra {@link #paint}</h2>
 *
 * <p>Son dos metodos porque son dos responsabilidades. {@code update} borra el fondo si el
 * componente es opaco y despues llama a {@code paint}; {@code paint} dibuja el contenido y no
 * sabe nada del fondo. Un aspecto que redefine solo {@code paint} conserva el borrado; uno que
 * redefine {@code update} elige no borrar — que es lo que hace un componente translucido.
 *
 * <h2>Todo devuelve "no se"</h2>
 *
 * <p>Las medidas devuelven {@code null} y {@link #getBaseline} devuelve {@code -1}: es la senal de
 * que el UI no tiene opinion y el componente cae a su propio calculo. Un UI vacio es entonces valido
 * y no rompe nada, que es la razon de que esta clase sea concreta y no una interfaz.
 *
 * <p>{@link #createUI} tira: cada subclase la sombrea con un metodo estatico propio, y llamar a
 * esta directamente es un error de programa. Es lo que hace el JDK.
 */
public abstract class ComponentUI {

    /** Para las subclases. */
    public ComponentUI() {
    }

    /** Este UI pasa a atender a {@code c}: instala colores, fuente, borde, oyentes. */
    public void installUI(JComponent c) {
    }

    /** Deshace exactamente lo que hizo {@link #installUI}. */
    public void uninstallUI(JComponent c) {
    }

    /** Dibuja el contenido. Sin fondo: eso es de {@link #update}. */
    public void paint(Graphics g, JComponent c) {
    }

    /** Borra el fondo si el componente es opaco, y despues dibuja. */
    public void update(Graphics g, JComponent c) {
        if (c.isOpaque()) {
            g.setColor(c.getBackground());
            g.fillRect(0, 0, c.getWidth(), c.getHeight());
        }
        paint(g, c);
    }

    /** El tamano preferido, o {@code null} si este UI no tiene opinion. */
    public Dimension getPreferredSize(JComponent c) {
        return null;
    }

    /** El tamano minimo; por omision, el preferido. */
    public Dimension getMinimumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /** El tamano maximo; por omision, el preferido. */
    public Dimension getMaximumSize(JComponent c) {
        return getPreferredSize(c);
    }

    /**
     * Si el punto cae dentro del componente.
     *
     * <p>Existe para que un aspecto pueda darle a un componente una forma que no sea su rectangulo
     * —un boton redondo— y que los clics fuera de esa forma pasen de largo.
     */
    public boolean contains(JComponent c, int x, int y) {
        return c.inside(x, y);
    }

    /**
     * @throws Error siempre: cada subclase provee la suya, estatica y con el mismo nombre
     */
    public static ComponentUI createUI(JComponent c) {
        throw new Error("ComponentUI.createUI no esta implementado; la sombrea cada subclase");
    }

    /**
     * La linea de base del componente, o {@code -1} si no tiene.
     *
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        if (c == null) {
            throw new NullPointerException("El componente no puede ser null");
        }
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("El ancho y el alto no pueden ser negativos");
        }
        return -1;
    }

    /**
     * Como se mueve la linea de base al cambiar el tamano.
     *
     * <p>Con el nombre binario {@code Component$BaselineResizeBehavior}: un tipo anidado de otro
     * archivo no resuelve por su nombre Java en nuestro compilador (#101).
     */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        if (c == null) {
            throw new NullPointerException("El componente no puede ser null");
        }
        return Component$BaselineResizeBehavior.OTHER;
    }

    /** Cuantos hijos accesibles tiene; por omision, los hijos del contenedor. */
    public int getAccessibleChildrenCount(JComponent c) {
        return c.getComponentCount();
    }

    /** El hijo accesible numero {@code i}, o {@code null} si no es {@link Accessible}. */
    public Accessible getAccessibleChild(JComponent c, int i) {
        if (i < 0 || i >= c.getComponentCount()) {
            return null;
        }
        java.awt.Component hijo = c.getComponent(i);
        if (hijo instanceof Accessible) {
            return (Accessible) hijo;
        }
        return null;
    }
}
