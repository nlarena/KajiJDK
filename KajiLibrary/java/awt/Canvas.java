package java.awt;

import java.awt.image.BufferStrategy;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * Un rectángulo en blanco para dibujar encima.
 *
 * <p>Es el componente que se usa cuando ninguno de los demás sirve: se hereda de él y se redefine
 * {@link #paint} con lo que sea que haya que mostrar. Un juego, un gráfico, un visor de imágenes.
 *
 * <p><strong>Sin pantalla no dibuja nada</strong>, y no puede: {@link #paint} recibe un
 * {@link Graphics} y esta implementación no tiene rasterizador. Lo que sí funciona es todo lo demás
 * —el tamaño, la posición, los eventos, el foco— así que un lienzo sirve perfectamente como hoja del
 * árbol de componentes aunque nunca llegue a mostrarse.
 */
public class Canvas extends Component implements Accessible {

    private static final long serialVersionUID = -2284879212465893870L;

    private static int canvasCounter = 0;

    /** Un lienzo. */
    public Canvas() {
    }

    /**
     * Un lienzo sobre esa configuración gráfica.
     *
     * @throws NullPointerException si la configuración es `null`
     */
    public Canvas(GraphicsConfiguration config) {
        this();
        this.setGraphicsConfiguration(config);
    }

    void setGraphicsConfiguration(GraphicsConfiguration gc) {
        if (gc == null) {
            throw new NullPointerException("config");
        }
    }

    String constructComponentName() {
        synchronized (Canvas.class) {
            String n = "canvas" + canvasCounter;
            canvasCounter = canvasCounter + 1;
            return n;
        }
    }

    /** Lo declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /**
     * Dibuja el lienzo.
     *
     * <p>El de base lo pinta del color de fondo. Acá no hace nada, porque no hay con qué: sin
     * rasterizador no existe un {@link Graphics} que pueda rellenar un rectángulo. Una subclase que
     * redefina esto en un entorno con pantalla sigue funcionando igual.
     */
    public void paint(Graphics g) {
    }

    /** Repinta; como {@link #paint} no dibuja, esto tampoco. */
    public void update(Graphics g) {
        this.paint(g);
    }

    boolean postsOldMouseEvents() {
        return true;
    }

    /**
     * Arma un mecanismo de doble buffer.
     *
     * @throws IllegalStateException siempre: el doble buffer es una cadena de superficies de dibujo,
     *     y sin pantalla no hay ninguna que encadenar. Inventar una que no dibuje sería peor que no
     *     tenerla.
     */
    public void createBufferStrategy(int numBuffers) {
        throw new IllegalStateException(
                "sin pantalla no hay superficies de dibujo que encadenar");
    }

    /**
     * Arma un mecanismo de doble buffer con esas capacidades.
     *
     * @throws IllegalStateException siempre, por el mismo motivo que
     *     {@link #createBufferStrategy(int)}
     */
    public void createBufferStrategy(int numBuffers, BufferCapabilities caps)
            throws AWTException {
        throw new IllegalStateException(
                "sin pantalla no hay superficies de dibujo que encadenar");
    }

    /**
     * El mecanismo de doble buffer.
     *
     * @return `null` siempre: nunca se pudo crear ninguno
     */
    public BufferStrategy getBufferStrategy() {
        return null;
    }

    /** La accesibilidad del lienzo. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTCanvas();
        }
        return this.accessibleContext;
    }

    /** Un lienzo, para la accesibilidad, es un lienzo. */
    protected class AccessibleAWTCanvas extends AccessibleAWTComponent {

        /** Para las subclases. */
        protected AccessibleAWTCanvas() {
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.CANVAS;
        }
    }
}
