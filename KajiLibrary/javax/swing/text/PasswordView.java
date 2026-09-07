package javax.swing.text;

import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;

/**
 * La vista de un campo de contrasena: dibuja un caracter de eco en lugar de cada letra.
 *
 * <h2>El texto sigue estando</h2>
 *
 * <p>Lo que cambia es el dibujado, no el documento: el texto real esta entero en el modelo y se
 * puede leer con {@code getText}. Esta vista no es una medida de seguridad contra el programa, es
 * una contra quien mira la pantalla.
 *
 * <p>Todas las cuentas de posicion se hacen con el ancho del caracter de eco, no con el de la
 * letra real: por eso el cursor cae donde debe aunque las letras midan distinto.
 */
public class PasswordView extends FieldView {

    static char[] ONE = new char[1];

    public PasswordView(Element elem) {
        super(elem);
    }

    /** Dibuja el tramo sin seleccionar como ecos. */
    protected int drawUnselectedText(Graphics g, int x, int y, int p0, int p1)
            throws BadLocationException {
        Container c = getContainer();
        if (c instanceof javax.swing.JPasswordField) {
            javax.swing.JPasswordField f = (javax.swing.JPasswordField) c;
            if (!f.echoCharIsSet()) {
                return super.drawUnselectedText(g, x, y, p0, p1);
            }
            if (f.isEnabled()) {
                g.setColor(f.getForeground());
            } else {
                g.setColor(f.getDisabledTextColor());
            }
            char echoChar = f.getEchoChar();
            int n = p1 - p0;
            for (int i = 0; i < n; i++) {
                x = drawEchoCharacter(g, x, y, echoChar);
            }
        }
        return x;
    }

    protected float drawUnselectedText(Graphics2D g, float x, float y, int p0, int p1)
            throws BadLocationException {
        return drawUnselectedText((Graphics) g, (int) x, (int) y, p0, p1);
    }

    protected int drawSelectedText(Graphics g, int x, int y, int p0, int p1)
            throws BadLocationException {
        g.setColor(selected);
        Container c = getContainer();
        if (c instanceof javax.swing.JPasswordField) {
            javax.swing.JPasswordField f = (javax.swing.JPasswordField) c;
            if (!f.echoCharIsSet()) {
                return super.drawSelectedText(g, x, y, p0, p1);
            }
            char echoChar = f.getEchoChar();
            int n = p1 - p0;
            for (int i = 0; i < n; i++) {
                x = drawEchoCharacter(g, x, y, echoChar);
            }
        }
        return x;
    }

    protected float drawSelectedText(Graphics2D g, float x, float y, int p0, int p1)
            throws BadLocationException {
        return drawSelectedText((Graphics) g, (int) x, (int) y, p0, p1);
    }

    /** Dibuja un eco y devuelve donde termino. */
    protected int drawEchoCharacter(Graphics g, int x, int y, char c) {
        ONE[0] = c;
        g.drawChars(ONE, 0, 1, x, y);
        return x + g.getFontMetrics().charWidth(c);
    }

    protected float drawEchoCharacter(Graphics2D g, float x, float y, char c) {
        return drawEchoCharacter((Graphics) g, (int) x, (int) y, c);
    }

    /** La posicion se cuenta en anchos de eco; ver la nota de la clase. */
    public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
        Container c = getContainer();
        if (c instanceof javax.swing.JPasswordField) {
            javax.swing.JPasswordField f = (javax.swing.JPasswordField) c;
            if (!f.echoCharIsSet()) {
                return super.modelToView(pos, a, b);
            }
            char echoChar = f.getEchoChar();
            FontMetrics m = f.getFontMetrics(f.getFont());

            Rectangle alloc = adjustAllocation(a).getBounds();
            int dx = (pos - getStartOffset()) * m.charWidth(echoChar);
            alloc.x = alloc.x + dx;
            alloc.width = 1;
            return alloc;
        }
        return null;
    }

    public int viewToModel(float fx, float fy, Shape a, Position.Bias[] bias) {
        bias[0] = Position.Bias.Forward;
        int n = 0;
        Container c = getContainer();
        if (c instanceof javax.swing.JPasswordField) {
            javax.swing.JPasswordField f = (javax.swing.JPasswordField) c;
            if (!f.echoCharIsSet()) {
                return super.viewToModel(fx, fy, a, bias);
            }
            char echoChar = f.getEchoChar();
            FontMetrics m = f.getFontMetrics(f.getFont());
            a = adjustAllocation(a);
            Rectangle alloc = (a instanceof Rectangle) ? (Rectangle) a : a.getBounds();
            n = ((int) fx - alloc.x) / m.charWidth(echoChar);
            if (n < 0) {
                n = 0;
            } else {
                Element e = getElement();
                int max = e.getEndOffset() - 1 - e.getStartOffset();
                if (n > max) {
                    n = max;
                }
            }
        }
        return getStartOffset() + n;
    }

    /** El ancho es el de los ecos, no el del texto. */
    public float getPreferredSpan(int axis) {
        Container c = getContainer();
        if (c instanceof javax.swing.JPasswordField) {
            javax.swing.JPasswordField f = (javax.swing.JPasswordField) c;
            if (f.echoCharIsSet() && axis == View.X_AXIS) {
                Element e = getElement();
                int n = e.getEndOffset() - e.getStartOffset() - 1;
                char echoChar = f.getEchoChar();
                FontMetrics m = f.getFontMetrics(f.getFont());
                return n * m.charWidth(echoChar);
            }
        }
        return super.getPreferredSpan(axis);
    }
}
