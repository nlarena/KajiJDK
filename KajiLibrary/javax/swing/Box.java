package javax.swing;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;

import javax.accessibility.AccessibleContext;

/**
 * Un contenedor liviano que solo sabe usar {@link BoxLayout}, y las piezas de relleno que lo
 * acompanan.
 *
 * <h2>El contenedor</h2>
 *
 * <p>No pinta nada por omision —es transparente— y no deja cambiarle la distribucion: eso es todo
 * lo que agrega sobre {@link JComponent}. Su valor esta en los metodos de fabrica: un
 * {@code createHorizontalBox()} dice en una linea lo que con {@code new JPanel} y
 * {@code setLayout} lleva tres.
 *
 * <h2>Las piezas de relleno</h2>
 *
 * <p>Un {@link Filler} es un componente invisible que solo existe para ocupar lugar. Con los tres
 * tamanos iguales es un <em>separador</em> —un hueco de tamano fijo—; con maximo enorme es
 * <em>pegamento</em>, que absorbe todo el espacio sobrante y empuja al resto. Dos pegamentos, uno
 * a cada lado, centran; uno solo adelante alinea al final.
 *
 * <p>El pegamento es la respuesta de esta familia a "quiero que ese boton quede a la derecha": no
 * hay una restriccion que lo diga, hay algo que ocupa el medio.
 */
public class Box extends JComponent implements Accessible {

    /** Una caja sobre ese eje; ver las constantes de {@link BoxLayout}. */
    public Box(int axis) {
        super();
        super.setLayout(new BoxLayout(this, axis));
    }

    /** Una caja horizontal. */
    public static Box createHorizontalBox() {
        return new Box(BoxLayout.X_AXIS);
    }

    /** Una caja vertical. */
    public static Box createVerticalBox() {
        return new Box(BoxLayout.Y_AXIS);
    }

    /** Un hueco de tamano fijo en las dos direcciones. */
    public static Component createRigidArea(Dimension d) {
        return new Filler(d, d, d);
    }

    /** Un hueco de ancho fijo, que no ocupa alto y puede estirarse a lo alto. */
    public static Component createHorizontalStrut(int width) {
        return new Filler(new Dimension(width, 0), new Dimension(width, 0),
                new Dimension(width, Short.MAX_VALUE));
    }

    /** Un hueco de alto fijo, que no ocupa ancho y puede estirarse a lo ancho. */
    public static Component createVerticalStrut(int height) {
        return new Filler(new Dimension(0, height), new Dimension(0, height),
                new Dimension(Short.MAX_VALUE, height));
    }

    /** Pegamento en las dos direcciones; ver la nota de la clase. */
    public static Component createGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(Short.MAX_VALUE, Short.MAX_VALUE));
    }

    /** Pegamento horizontal. */
    public static Component createHorizontalGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(Short.MAX_VALUE, 0));
    }

    /** Pegamento vertical. */
    public static Component createVerticalGlue() {
        return new Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(0, Short.MAX_VALUE));
    }

    /** Un {@link AWTError}: una caja es su {@link BoxLayout}, sin el no es nada. */
    public void setLayout(LayoutManager l) {
        throw new AWTError("Illegal request");
    }

    /** Pinta el fondo si es opaca; por omision no lo es. */
    protected void paintComponent(Graphics g) {
        if (ui != null) {
            Graphics scratchGraphics = (g == null) ? null : g.create();
            try {
                ui.update(scratchGraphics, this);
            } finally {
                scratchGraphics.dispose();
            }
        } else if (isOpaque()) {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
        }
    }

    /** Sin contexto de accesibilidad: no hay tecnologia asistiva que lo lea en esta VM. */
    public AccessibleContext getAccessibleContext() {
        return null;
    }

    /**
     * Un componente invisible con los tres tamanos a pedido; ver la nota de {@link Box}.
     *
     * <p>Es publica y no anonima porque los tamanos se pueden cambiar despues, con
     * {@link #changeShape}: un separador que se agranda cuando la ventana lo hace.
     */
    public static class Filler extends JComponent implements Accessible {

        /** Un relleno con esos tres tamanos. */
        public Filler(Dimension min, Dimension pref, Dimension max) {
            setMinimumSize(min);
            setPreferredSize(pref);
            setMaximumSize(max);
        }

        /** Le cambia los tres tamanos de una vez y pide que lo reacomoden. */
        public void changeShape(Dimension min, Dimension pref, Dimension max) {
            setMinimumSize(min);
            setPreferredSize(pref);
            setMaximumSize(max);
            revalidate();
        }

        /** Pinta el fondo si es opaco; por omision no lo es, y por eso no se ve. */
        protected void paintComponent(Graphics g) {
            if (ui != null) {
                Graphics scratchGraphics = (g == null) ? null : g.create();
                try {
                    ui.update(scratchGraphics, this);
                } finally {
                    scratchGraphics.dispose();
                }
            } else if (isOpaque()) {
                g.setColor(getBackground());
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        }

        /** Sin contexto de accesibilidad: no hay tecnologia asistiva que lo lea en esta VM. */
        public AccessibleContext getAccessibleContext() {
            return null;
        }
    }
}
