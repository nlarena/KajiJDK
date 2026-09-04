package javax.swing.border;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Insets;

import javax.swing.Icon;

/**
 * Un borde macizo, de un color o embaldosado con un icono.
 *
 * <h2>Las dos cosas que lo distinguen de {@link LineBorder}</h2>
 *
 * <p>La primera es que los cuatro lados pueden tener <strong>grosores distintos</strong> — de ahi
 * que extienda {@link EmptyBorder}, que ya sabe llevar cuatro margenes. Una linea de tres pixeles
 * solo arriba es un separador, y con {@code LineBorder} no se puede escribir.
 *
 * <p>La segunda es el icono: en vez de un color, el borde se rellena repitiendo una imagen. Ahi el
 * dibujo se recorta a cada lado del marco antes de embaldosar, porque si no las baldosas se saldrian
 * por encima del contenido.
 *
 * <p>Con el constructor de un solo {@link Icon} los grosores se toman <strong>del tamano del
 * icono</strong>, que es la unica medida razonable disponible.
 */
public class MatteBorder extends EmptyBorder {

    private static final long serialVersionUID = 4422248989617298224L;

    protected Color color;
    protected Icon tileIcon;

    /** Con los cuatro grosores y un color. */
    public MatteBorder(int top, int left, int bottom, int right, Color matteColor) {
        super(top, left, bottom, right);
        this.color = matteColor;
    }

    /** Con los grosores de un {@link Insets} y un color. */
    public MatteBorder(Insets borderInsets, Color matteColor) {
        super(borderInsets);
        this.color = matteColor;
    }

    /** Con los cuatro grosores, embaldosando con un icono. */
    public MatteBorder(int top, int left, int bottom, int right, Icon tileIcon) {
        super(top, left, bottom, right);
        this.tileIcon = tileIcon;
    }

    /** Con los grosores de un {@link Insets}, embaldosando con un icono. */
    public MatteBorder(Insets borderInsets, Icon tileIcon) {
        super(borderInsets);
        this.tileIcon = tileIcon;
    }

    /** Solo con el icono: los grosores salen de su tamano. */
    public MatteBorder(Icon tileIcon) {
        this(-1, -1, -1, -1, tileIcon);
    }

    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Insets i = getBorderInsets(c, new Insets(0, 0, 0, 0));
        Color viejo = g.getColor();
        g.translate(x, y);

        if (this.color != null) {
            g.setColor(this.color);
            g.fillRect(0, 0, width - i.right, i.top);
            g.fillRect(0, i.top, i.left, height - i.top);
            g.fillRect(i.left, height - i.bottom, width - i.left, i.bottom);
            g.fillRect(width - i.right, 0, i.right, height - i.bottom);
        } else if (this.tileIcon != null) {
            int anchoBaldosa = this.tileIcon.getIconWidth();
            int altoBaldosa = this.tileIcon.getIconHeight();
            // Los cuatro lados del marco, embaldosados. Se recorta antes de dibujar porque una
            // baldosa casi nunca entra un numero entero de veces, y la ultima se saldria.
            embaldosar(c, g, 0, 0, width - i.right, i.top, anchoBaldosa, altoBaldosa);
            embaldosar(c, g, 0, i.top, i.left, height - i.top, anchoBaldosa, altoBaldosa);
            embaldosar(c, g, i.left, height - i.bottom, width - i.left, i.bottom,
                    anchoBaldosa, altoBaldosa);
            embaldosar(c, g, width - i.right, 0, i.right, height - i.bottom,
                    anchoBaldosa, altoBaldosa);
        }

        g.translate(-x, -y);
        g.setColor(viejo);
    }

    private void embaldosar(Component c, Graphics g, int x, int y, int width, int height,
            int anchoBaldosa, int altoBaldosa) {
        if (width <= 0 || height <= 0 || anchoBaldosa <= 0 || altoBaldosa <= 0) {
            return;
        }
        java.awt.Shape recorteViejo = g.getClip();
        g.clipRect(x, y, width, height);
        for (int fy = y; fy < y + height; fy = fy + altoBaldosa) {
            for (int fx = x; fx < x + width; fx = fx + anchoBaldosa) {
                this.tileIcon.paintIcon(c, g, fx, fy);
            }
        }
        g.setClip(recorteViejo);
    }

    public Insets getBorderInsets(Component c, Insets insets) {
        return calcularInsets(insets);
    }

    /** Los grosores, en un {@link Insets} nuevo. */
    public Insets getBorderInsets() {
        return calcularInsets(new Insets(0, 0, 0, 0));
    }

    /**
     * Rellena {@code insets}, resolviendo los negativos con el tamano del icono.
     *
     * <p>Un grosor negativo es la marca de "no me lo dijeron": lo pone el constructor de un solo
     * icono. Resolverlo aca y no ahi es lo que permite que el icono se pueda cambiar despues.
     */
    private Insets calcularInsets(Insets insets) {
        if (this.tileIcon != null) {
            if (this.top == -1 && this.bottom == -1 && this.left == -1 && this.right == -1) {
                int ancho = this.tileIcon.getIconWidth();
                int alto = this.tileIcon.getIconHeight();
                insets.top = alto;
                insets.bottom = alto;
                insets.left = ancho;
                insets.right = ancho;
                return insets;
            }
        }
        insets.top = Math.max(this.top, 0);
        insets.left = Math.max(this.left, 0);
        insets.bottom = Math.max(this.bottom, 0);
        insets.right = Math.max(this.right, 0);
        return insets;
    }

    /** El color de relleno, o {@code null} si embaldosa con un icono. */
    public Color getMatteColor() {
        return this.color;
    }

    /** El icono con el que embaldosa, o {@code null} si es de color. */
    public Icon getTileIcon() {
        return this.tileIcon;
    }

    /**
     * Opaco solo si es de color.
     *
     * <p>Un icono puede tener transparencias, y esta clase no tiene forma de saberlo: prometer
     * opacidad ahi seria una apuesta sobre una imagen ajena.
     */
    public boolean isBorderOpaque() {
        return this.color != null;
    }
}
