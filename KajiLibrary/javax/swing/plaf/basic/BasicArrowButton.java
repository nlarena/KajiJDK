package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JButton;
import javax.swing.SwingConstants;

/**
 * Un boton chico con una flecha: el que va en las puntas de una barra de desplazamiento.
 *
 * <h2>Pinta el mismo, sin pasar por su aspecto</h2>
 *
 * <p>Redefine {@link #paint} entero, asi que el {@code ButtonUI} que tenga instalado no dibuja
 * nunca. Es deliberado: la flecha tiene que verse igual venga del aspecto que venga, porque su
 * tamano y sus cuatro colores se los da quien lo crea —la barra— y no una tabla de valores.
 *
 * <p>El relieve es el mismo del pulgar de la barra, y esta medido en el JDK 25: un marco de un
 * pixel en la sombra oscura del que solo quedan a la vista el lado derecho y el de abajo, el fondo
 * lleno, una linea de brillo por dentro arriba y a la izquierda, y una de sombra por dentro abajo y
 * a la derecha. Apretado, el marco pasa a ser de sombra clara y la flecha se corre un pixel: es
 * todo lo que hace falta para que se vea hundido.
 *
 * <p>La flecha es un triangulo de lineas, no una figura rellena: asi queda simetrica pixel a pixel
 * en los cuatro sentidos. Deshabilitada se dibuja dos veces, en sombra y en brillo corrida un
 * pixel, que es el mismo relieve que usa una etiqueta deshabilitada.
 */
public class BasicArrowButton extends JButton implements SwingConstants {

    /** Hacia donde apunta: {@code NORTH}, {@code SOUTH}, {@code EAST} u {@code WEST}. */
    protected int direction;

    private Color shadow;
    private Color darkShadow;
    private Color highlight;

    /** Un boton con esos cuatro colores; los da quien lo crea. */
    public BasicArrowButton(int direction, Color background, Color shadow, Color darkShadow,
            Color highlight) {
        super();
        setRequestFocusEnabled(false);
        setDirection(direction);
        setBackground(background);
        this.shadow = shadow;
        this.darkShadow = darkShadow;
        this.highlight = highlight;
    }

    /**
     * Un boton con los colores del aspecto: control, sombra, sombra oscura y brillo claro.
     *
     * <p>Los cuatro son los de Metal medidos en el JDK 25: (238, 238, 238), (184, 207, 229),
     * (122, 138, 153) y blanco. Sin {@code UIManager}, van escritos aca.
     */
    public BasicArrowButton(int direction) {
        this(direction, new Color(238, 238, 238), new Color(184, 207, 229),
                new Color(122, 138, 153), new Color(255, 255, 255));
    }

    public int getDirection() {
        return direction;
    }

    public void setDirection(int direction) {
        this.direction = direction;
        setFocusable(false);
    }

    /** Ver la nota de la clase. */
    public void paint(Graphics g) {
        int w = getSize().width;
        int h = getSize().height;
        Color origColor = g.getColor();
        boolean isPressed = getModel().isPressed();
        boolean isEnabled = isEnabled();

        if (isPressed) {
            g.setColor(getBackground());
            g.fillRect(1, 1, w - 2, h - 2);
            g.setColor(shadow);
            g.drawRect(0, 0, w - 1, h - 1);
        } else {
            g.setColor(darkShadow);
            g.drawRect(0, 0, w - 1, h - 1);
            g.setColor(getBackground());
            g.fillRect(0, 0, w - 1, h - 1);
            g.setColor(highlight);
            g.drawLine(1, 1, 1, h - 2);
            g.drawLine(2, 1, w - 3, 1);
            g.setColor(shadow);
            g.drawLine(1, h - 2, w - 2, h - 2);
            g.drawLine(w - 2, 1, w - 2, h - 2);
        }

        // Sin lugar para la flecha, queda el relieve solo.
        if (h < 5 || w < 5) {
            g.setColor(origColor);
            return;
        }

        if (isPressed) {
            g.translate(1, 1);
        }

        int size = Math.min((h - 4) / 3, (w - 4) / 3);
        size = Math.max(size, 2);
        paintTriangle(g, (w - size) / 2, (h - size) / 2, size, direction, isEnabled);

        if (isPressed) {
            g.translate(-1, -1);
        }
        g.setColor(origColor);
    }

    /** Dieciseis por dieciseis: el tamano del que la barra saca el largo de sus puntas. */
    public Dimension getPreferredSize() {
        return new Dimension(16, 16);
    }

    /** Cinco por cinco: menos que eso y no queda flecha, solo relieve. */
    public Dimension getMinimumSize() {
        return new Dimension(5, 5);
    }

    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /** No: el foco va a la barra, no a sus flechas. */
    public boolean isFocusTraversable() {
        return false;
    }

    /**
     * Dibuja el triangulo en esa caja y en ese sentido.
     *
     * <p>Es publico porque otros aspectos lo usan para dibujar la misma flecha en otro lado —el
     * boton de un desplegable, la punta de un menu—, y ninguno querria repetir la aritmetica.
     */
    public void paintTriangle(Graphics g, int x, int y, int size, int direction,
            boolean isEnabled) {
        Color oldColor = g.getColor();
        int mid;
        int i;
        int j;

        j = 0;
        size = Math.max(size, 2);
        mid = (size / 2) - 1;

        g.translate(x, y);
        if (isEnabled) {
            g.setColor(darkShadow);
        } else {
            g.setColor(shadow);
        }

        if (direction == NORTH) {
            for (i = 0; i < size; i++) {
                g.drawLine(mid - i, i, mid + i, i);
            }
            if (!isEnabled) {
                g.setColor(highlight);
                g.drawLine(mid - i + 2, i, mid + i, i);
            }
        } else if (direction == SOUTH) {
            if (!isEnabled) {
                g.translate(1, 1);
                g.setColor(highlight);
                for (i = size - 1; i >= 0; i--) {
                    g.drawLine(mid - i, j, mid + i, j);
                    j++;
                }
                g.translate(-1, -1);
                g.setColor(shadow);
            }
            j = 0;
            for (i = size - 1; i >= 0; i--) {
                g.drawLine(mid - i, j, mid + i, j);
                j++;
            }
        } else if (direction == WEST) {
            for (i = 0; i < size; i++) {
                g.drawLine(i, mid - i, i, mid + i);
            }
            if (!isEnabled) {
                g.setColor(highlight);
                g.drawLine(i, mid - i + 2, i, mid + i);
            }
        } else if (direction == EAST) {
            if (!isEnabled) {
                g.translate(1, 1);
                g.setColor(highlight);
                for (i = size - 1; i >= 0; i--) {
                    g.drawLine(j, mid - i, j, mid + i);
                    j++;
                }
                g.translate(-1, -1);
                g.setColor(shadow);
            }
            j = 0;
            for (i = size - 1; i >= 0; i--) {
                g.drawLine(j, mid - i, j, mid + i);
                j++;
            }
        }
        g.translate(-x, -y);
        g.setColor(oldColor);
    }
}
