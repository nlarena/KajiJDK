package javax.swing.border;

import java.awt.Component;
import java.awt.Component$BaselineResizeBehavior;
import java.awt.ComponentOrientation;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * La base de todos los bordes: implementa los tres metodos de {@link Border} sin dibujar nada.
 *
 * <h2>Por que un borde que no hace nada es util</h2>
 *
 * <p>Porque casi ningun borde concreto necesita los tres metodos. Uno que solo reserva espacio
 * ({@link EmptyBorder}) no pinta; uno que solo pinta una linea no necesita reescribir el calculo de
 * la caja interior. Extender esta clase deja escribir unicamente lo que el borde realmente hace.
 *
 * <p>Los valores por omision son los <em>neutros</em>: no pinta, no ocupa espacio, no es opaco. Los
 * tres son seguros — un borde a medio escribir se ve como si no estuviera, en vez de romper el
 * layout o dejar basura en pantalla.
 *
 * <h2>Las dos formas de {@link #getBorderInsets}</h2>
 *
 * <p>La de un argumento crea un {@link Insets} nuevo; la de dos <strong>reusa</strong> el que se le
 * pasa. La segunda existe porque el layout de Swing pregunta los insets muchas veces por segundo y
 * alocar un objeto en cada consulta se nota. Las subclases sobrescriben la de dos, y la de uno la
 * llama con un {@code Insets} fresco: asi hay un solo lugar con la logica.
 */
public abstract class AbstractBorder implements Border, java.io.Serializable {

    private static final long serialVersionUID = -511181274974418195L;

    /** Para las subclases. */
    protected AbstractBorder() {
    }

    /** No dibuja nada. */
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
    }

    /** No reserva espacio. */
    public Insets getBorderInsets(Component c) {
        return getBorderInsets(c, new Insets(0, 0, 0, 0));
    }

    /**
     * No reserva espacio, reusando {@code insets}.
     *
     * <p>Es la forma que las subclases sobrescriben; ver la nota de la clase.
     */
    public Insets getBorderInsets(Component c, Insets insets) {
        insets.left = 0;
        insets.top = 0;
        insets.right = 0;
        insets.bottom = 0;
        return insets;
    }

    /** No es opaco. */
    public boolean isBorderOpaque() {
        return false;
    }

    /** El rectangulo que queda adentro del borde, o sea el dado menos los insets. */
    public Rectangle getInteriorRectangle(Component c, int x, int y, int width, int height) {
        return getInteriorRectangle(c, this, x, y, width, height);
    }

    /**
     * Lo mismo, para un borde cualquiera.
     *
     * <p>Estatica porque {@link CompoundBorder} la necesita sobre su borde <em>externo</em>, que no
     * es {@code this}. Un borde {@code null} no reserva nada, que es lo que permite escribir
     * "el borde de este componente, si tiene" sin un {@code if}.
     */
    public static Rectangle getInteriorRectangle(Component c, Border b, int x, int y, int width,
            int height) {
        Insets insets;
        if (b != null) {
            insets = b.getBorderInsets(c);
        } else {
            insets = new Insets(0, 0, 0, 0);
        }
        return new Rectangle(x + insets.left, y + insets.top,
                width - insets.right - insets.left, height - insets.top - insets.bottom);
    }

    /**
     * La linea de base del texto de este borde, o {@code -1} si no tiene.
     *
     * <p>Existe para que un borde con texto —{@link TitledBorder}— pueda alinearse con el de al
     * lado. Los demas no tienen nada que alinear.
     *
     * @throws IllegalArgumentException si {@code width} o {@code height} son negativos
     */
    public int getBaseline(Component c, int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("El ancho y el alto no pueden ser negativos");
        }
        return -1;
    }

    /**
     * Como se mueve la linea de base cuando el componente cambia de tamano.
     *
     * <p>El tipo va con el nombre <strong>binario</strong> {@code Component$BaselineResizeBehavior}:
     * el nombre Java de un tipo anidado de otro archivo no resuelve en nuestro compilador (#101), y
     * el rodeo por {@code import} emite un descriptor de una clase que no existe (#208).
     */
    public Component$BaselineResizeBehavior getBaselineResizeBehavior(Component c) {
        if (c == null) {
            throw new NullPointerException("El componente no puede ser null");
        }
        return Component$BaselineResizeBehavior.OTHER;
    }

    /**
     * Si {@code c} se lee de izquierda a derecha.
     *
     * <p>De paquete y no publica: la usan los bordes que dibujan algo asimetrico —el titulo de un
     * {@link TitledBorder}— y no es parte del contrato de nadie mas.
     */
    static boolean isLeftToRight(Component c) {
        ComponentOrientation o = c.getComponentOrientation();
        return o.isLeftToRight();
    }
}
