package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.InputEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

/**
 * Los trazos que comparten los aspectos basicos: biseles, surcos, texto con mnemonico.
 *
 * <p>Estatica y sin estado a proposito: son rutinas de dibujo puras, y ponerlas en un lugar es lo
 * que hace que un boton, un panel con borde y una barra se vean con el mismo relieve.
 *
 * <p>Las cuatro rutinas de relieve dibujan la misma ilusion que {@code BevelBorder}: la luz viene
 * de arriba a la izquierda, asi que ese lado va claro y el opuesto oscuro. Cambiar que color va a
 * cada lado es toda la diferencia entre levantado y hundido.
 */
public class BasicGraphicsUtils {

    private static final Insets INSETS_GRABADO = new Insets(2, 2, 2, 2);
    private static final Insets INSETS_SURCO = new Insets(2, 2, 2, 2);

    /** El JDK la deja instanciable, aunque no haya nada que instanciar. */
    public BasicGraphicsUtils() {
    }

    /** Un rectangulo grabado de dos pixeles: sombra afuera arriba-izquierda, brillo abajo-derecha. */
    public static void drawEtchedRect(Graphics g, int x, int y, int w, int h, Color shadow,
            Color darkShadow, Color highlight, Color lightHighlight) {
        Color viejo = g.getColor();
        g.translate(x, y);

        g.setColor(shadow);
        g.drawLine(0, 0, w - 1, 0);
        g.drawLine(0, 1, 0, h - 2);

        g.setColor(darkShadow);
        g.drawLine(1, 1, w - 3, 1);
        g.drawLine(1, 2, 1, h - 3);

        g.setColor(lightHighlight);
        g.drawLine(w - 1, 0, w - 1, h - 1);
        g.drawLine(0, h - 1, w - 1, h - 1);

        g.setColor(highlight);
        g.drawLine(w - 2, 1, w - 2, h - 3);
        g.drawLine(1, h - 2, w - 2, h - 2);

        g.translate(-x, -y);
        g.setColor(viejo);
    }

    /** Cuanto ocupa un rectangulo grabado: dos pixeles por lado. */
    public static Insets getEtchedInsets() {
        return INSETS_GRABADO;
    }

    /** Un surco: una linea de sombra y una de brillo, desplazada un pixel. */
    public static void drawGroove(Graphics g, int x, int y, int w, int h, Color shadow,
            Color highlight) {
        Color viejo = g.getColor();
        g.translate(x, y);

        g.setColor(shadow);
        g.drawRect(0, 0, w - 2, h - 2);

        g.setColor(highlight);
        g.drawLine(1, h - 3, 1, 1);
        g.drawLine(1, 1, w - 3, 1);
        g.drawLine(0, h - 1, w - 1, h - 1);
        g.drawLine(w - 1, h - 1, w - 1, 0);

        g.translate(-x, -y);
        g.setColor(viejo);
    }

    /** Cuanto ocupa un surco: dos pixeles por lado. */
    public static Insets getGrooveInsets() {
        return INSETS_SURCO;
    }

    /**
     * El bisel de un boton, en sus cuatro estados.
     *
     * <p>{@code isPressed} invierte los colores —el boton se hunde— y {@code isDefault} agrega el
     * marco oscuro exterior que marca al boton por omision de un dialogo. Los dos se combinan.
     */
    public static void drawBezel(Graphics g, int x, int y, int w, int h, boolean isPressed,
            boolean isDefault, Color shadow, Color darkShadow, Color highlight,
            Color lightHighlight) {
        Color viejo = g.getColor();
        g.translate(x, y);

        if (isPressed && isDefault) {
            g.setColor(darkShadow);
            g.drawRect(0, 0, w - 1, h - 1);
            g.setColor(shadow);
            g.drawRect(1, 1, w - 3, h - 3);
        } else if (isPressed) {
            drawLoweredBezel(g, 0, 0, w, h, shadow, darkShadow, highlight, lightHighlight);
        } else if (isDefault) {
            g.setColor(darkShadow);
            g.drawRect(0, 0, w - 1, h - 1);

            g.setColor(lightHighlight);
            g.drawLine(1, 1, 1, h - 3);
            g.drawLine(2, 1, w - 3, 1);

            g.setColor(highlight);
            g.drawLine(2, 2, 2, h - 4);
            g.drawLine(3, 2, w - 4, 2);

            g.setColor(shadow);
            g.drawLine(2, h - 3, w - 3, h - 3);
            g.drawLine(w - 3, 2, w - 3, h - 4);

            g.setColor(darkShadow);
            g.drawLine(1, h - 2, w - 2, h - 2);
            g.drawLine(w - 2, h - 2, w - 2, 1);
        } else {
            g.setColor(lightHighlight);
            g.drawLine(0, 0, 0, h - 1);
            g.drawLine(1, 0, w - 2, 0);

            g.setColor(highlight);
            g.drawLine(1, 1, 1, h - 3);
            g.drawLine(2, 1, w - 3, 1);

            g.setColor(shadow);
            g.drawLine(1, h - 2, w - 2, h - 2);
            g.drawLine(w - 2, 1, w - 2, h - 3);

            g.setColor(darkShadow);
            g.drawLine(0, h - 1, w - 1, h - 1);
            g.drawLine(w - 1, h - 1, w - 1, 0);
        }

        g.translate(-x, -y);
        g.setColor(viejo);
    }

    /** El bisel hundido: los colores del levantado, intercambiados. */
    public static void drawLoweredBezel(Graphics g, int x, int y, int w, int h, Color shadow,
            Color darkShadow, Color highlight, Color lightHighlight) {
        Color viejo = g.getColor();
        g.translate(x, y);

        g.setColor(darkShadow);
        g.drawLine(0, 0, 0, h - 1);
        g.drawLine(1, 0, w - 2, 0);

        g.setColor(shadow);
        g.drawLine(1, 1, 1, h - 2);
        g.drawLine(1, 1, w - 3, 1);

        g.setColor(lightHighlight);
        g.drawLine(0, h - 1, w - 1, h - 1);
        g.drawLine(w - 1, h - 1, w - 1, 0);

        g.setColor(highlight);
        g.drawLine(1, h - 2, w - 2, h - 2);
        g.drawLine(w - 2, h - 2, w - 2, 1);

        g.translate(-x, -y);
        g.setColor(viejo);
    }

    /**
     * Dibuja texto subrayando la primera aparicion del mnemonico.
     *
     * <p>Primero busca la mayuscula y despues la minuscula, como {@code JLabel} al elegir que
     * subrayar. Un mnemonico que no esta en el texto no subraya nada, y eso no es un error: un
     * boton puede tener atajo sin que la letra aparezca.
     */
    public static void drawString(Graphics g, String text, int underlinedChar, int x, int y) {
        int indice = -1;
        if (underlinedChar != '\0') {
            char mayus = Character.toUpperCase((char) underlinedChar);
            char minus = Character.toLowerCase((char) underlinedChar);
            int i1 = text.indexOf(mayus);
            int i2 = text.indexOf(minus);
            if (i1 == -1) {
                indice = i2;
            } else if (i2 == -1) {
                indice = i1;
            } else {
                indice = Math.min(i1, i2);
            }
        }
        drawStringUnderlineCharAt(g, text, indice, x, y);
    }

    /**
     * Dibuja texto subrayando el caracter en esa posicion.
     *
     * <p>La raya va un pixel por encima del fondo del descenso, del ancho del caracter, y de un
     * pixel de alto: es donde el JDK la pone, y lo que hace que el subrayado no se pise con la base
     * de las letras ni se separe de ellas.
     */
    public static void drawStringUnderlineCharAt(Graphics g, String text, int underlinedIndex,
            int x, int y) {
        g.drawString(text, x, y);
        if (underlinedIndex >= 0 && underlinedIndex < text.length()) {
            FontMetrics fm = g.getFontMetrics();
            int rayaX = x + fm.stringWidth(text.substring(0, underlinedIndex));
            int rayaY = y;
            int rayaAncho = fm.charWidth(text.charAt(underlinedIndex));
            int rayaAlto = 1;
            g.fillRect(rayaX, rayaY + fm.getDescent() - 1, rayaAncho, rayaAlto);
        }
    }

    /**
     * Un rectangulo punteado, de a un pixel si y uno no.
     *
     * <p>Es el marco de foco de los aspectos basicos. Se dibuja pixel por pixel y no con un trazo
     * discontinuo porque tiene que quedar igual en las esquinas, donde un trazo se desfasaria.
     */
    public static void drawDashedRect(Graphics g, int x, int y, int width, int height) {
        int vx;
        int vy;
        for (vx = x; vx < (x + width); vx = vx + 2) {
            g.fillRect(vx, y, 1, 1);
            g.fillRect(vx, y + height - 1, 1, 1);
        }
        for (vy = y; vy < (y + height); vy = vy + 2) {
            g.fillRect(x, vy, 1, 1);
            g.fillRect(x + width - 1, vy, 1, 1);
        }
    }

    /**
     * El tamano preferido de un boton: icono y texto ubicados en una vista infinita, mas los
     * insets.
     *
     * <p>{@code null} si el boton tiene hijos: entonces el tamano lo decide su layout, no su
     * texto. Sin texto la separacion icono-texto es cero, como al pintar, para que un boton de
     * solo icono mida lo que mide el icono.
     */
    public static Dimension getPreferredButtonSize(AbstractButton b, int textIconGap) {
        if (b.getComponentCount() > 0) {
            return null;
        }
        Icon icono = b.getIcon();
        String texto = b.getText();
        Font fuente = b.getFont();
        FontMetrics fm = b.getFontMetrics(fuente);
        Rectangle iconoR = new Rectangle();
        Rectangle textoR = new Rectangle();
        Rectangle vistaR = new Rectangle(Short.MAX_VALUE, Short.MAX_VALUE);
        SwingUtilities.layoutCompoundLabel(b, fm, texto, icono, b.getVerticalAlignment(),
                b.getHorizontalAlignment(), b.getVerticalTextPosition(),
                b.getHorizontalTextPosition(), vistaR, iconoR, textoR,
                texto == null ? 0 : textIconGap);
        Rectangle r = iconoR.union(textoR);
        Insets insets = b.getInsets();
        r.width = r.width + insets.left + insets.right;
        r.height = r.height + insets.top + insets.bottom;
        return new Dimension(r.width, r.height);
    }

    /** Si el componente se lee de izquierda a derecha. */
    static boolean isLeftToRight(Component c) {
        return c.getComponentOrientation().isLeftToRight();
    }

    /**
     * Si el modificador de atajos del sistema esta apretado.
     *
     * <p>Control, en esta VM: no hay {@code Toolkit.getMenuShortcutKeyMaskEx} que consultar, y
     * Control es lo que devuelve en todas las plataformas que no son macOS.
     */
    static boolean isMenuShortcutKeyDown(InputEvent event) {
        return (event.getModifiersEx() & InputEvent.CTRL_DOWN_MASK) != 0;
    }

    /** Dibuja texto con las sugerencias de renderizado del componente; ver la nota de {@link #getStringWidth}. */
    public static void drawString(JComponent c, Graphics2D g, String string, float x, float y) {
        if (string == null || string.isEmpty()) {
            return;
        }
        g.drawString(string, x, y);
    }

    /** Dibuja texto subrayando una posicion, con coordenadas fraccionarias. */
    public static void drawStringUnderlineCharAt(JComponent c, Graphics2D g, String string,
            int underlinedIndex, float x, float y) {
        if (string == null || string.isEmpty()) {
            return;
        }
        drawStringUnderlineCharAt(g, string, underlinedIndex, Math.round(x), Math.round(y));
    }

    /**
     * Recorta la cadena con puntos suspensivos para que entre en ese ancho.
     *
     * <p>Devuelve la cadena entera si entra, y solo los puntos si ni ellos entran: nunca
     * {@code null}. El bucle suma de a un caracter y para en el primero que se pasa, igual que
     * {@code SwingUtilities.layoutCompoundLabel}, para que una etiqueta y un texto recortado a mano
     * corten en el mismo lugar.
     */
    public static String getClippedString(JComponent c, FontMetrics fm, String string,
            int availTextWidth) {
        if (string == null || string.isEmpty()) {
            return "";
        }
        int ancho = fm.stringWidth(string);
        if (ancho <= availTextWidth) {
            return string;
        }
        String puntos = "...";
        int disponible = availTextWidth - fm.stringWidth(puntos);
        if (disponible <= 0) {
            return puntos;
        }
        int total = 0;
        int n;
        for (n = 0; n < string.length(); n++) {
            total = total + fm.charWidth(string.charAt(n));
            if (total > disponible) {
                break;
            }
        }
        return string.substring(0, n) + puntos;
    }

    /**
     * El ancho de una cadena.
     *
     * <p>Entero, aunque la firma diga {@code float}: esta VM no tiene metricas fraccionarias, asi
     * que el ancho es el de {@link FontMetrics#stringWidth}. La firma es la del JDK, que si las tiene
     * cuando el componente las pide.
     */
    public static float getStringWidth(JComponent c, FontMetrics fm, String string) {
        if (string == null || string.isEmpty()) {
            return 0.0f;
        }
        return fm.stringWidth(string);
    }
}
