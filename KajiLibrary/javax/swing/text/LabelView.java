package javax.swing.text;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Shape;

import javax.swing.event.DocumentEvent;

/**
 * Un tramo de texto con estilo: {@link GlyphView} con los atributos ya resueltos y guardados.
 *
 * <h2>Por que guarda lo que ya sabe preguntar</h2>
 *
 * <p>{@code GlyphView} pregunta la fuente y el color al documento cada vez. Esta los resuelve una
 * sola vez ({@link #setPropertiesFromAttributes}) y los guarda en campos. Dibujar una linea
 * consulta el color por cada tramo y por cada cuadro; con el estilo resuelto, esa consulta es leer
 * un campo.
 *
 * <p>Lo guardado se rehace cuando los atributos cambian, y de ahi que {@link #changedUpdate} sea
 * el unico metodo que hace algo mas que reenviar.
 */
public class LabelView extends GlyphView implements TabableView {

    private Font font;
    private Color fg;
    private Color bg;
    private boolean underline;
    private boolean strike;
    private boolean superscript;
    private boolean subscript;

    /** Si lo guardado sigue valiendo. */
    private boolean valido;

    public LabelView(Element elem) {
        super(elem);
    }

    /** Rehace lo guardado si hizo falta. */
    final void sync() {
        if (!valido) {
            setPropertiesFromAttributes();
        }
    }

    protected void setUnderline(boolean u) {
        underline = u;
    }

    protected void setStrikeThrough(boolean s) {
        strike = s;
    }

    protected void setSuperscript(boolean s) {
        superscript = s;
    }

    protected void setSubscript(boolean s) {
        subscript = s;
    }

    protected void setBackground(Color bg) {
        this.bg = bg;
    }

    /** Resuelve fuente, colores y decoraciones de los atributos, una sola vez. */
    protected void setPropertiesFromAttributes() {
        AttributeSet attr = getAttributes();
        if (attr != null) {
            Document d = getDocument();
            if (d instanceof StyledDocument) {
                StyledDocument doc = (StyledDocument) d;
                font = doc.getFont(attr);
                fg = doc.getForeground(attr);
                if (attr.isDefined(StyleConstants.Background)) {
                    bg = doc.getBackground(attr);
                } else {
                    bg = null;
                }
            }
            setUnderline(StyleConstants.isUnderline(attr));
            setStrikeThrough(StyleConstants.isStrikeThrough(attr));
            setSuperscript(StyleConstants.isSuperscript(attr));
            setSubscript(StyleConstants.isSubscript(attr));
            valido = true;
        }
    }

    /** Las metricas de la fuente resuelta. */
    protected FontMetrics getFontMetrics() {
        sync();
        java.awt.Container c = getContainer();
        if (c != null && font != null) {
            return c.getFontMetrics(font);
        }
        if (font != null) {
            return java.awt.Toolkit.getDefaultToolkit().getFontMetrics(font);
        }
        return null;
    }

    public Color getBackground() {
        sync();
        return bg;
    }

    public Color getForeground() {
        sync();
        return fg;
    }

    public Font getFont() {
        sync();
        return font;
    }

    public boolean isUnderline() {
        sync();
        return underline;
    }

    public boolean isStrikeThrough() {
        sync();
        return strike;
    }

    public boolean isSubscript() {
        sync();
        return subscript;
    }

    public boolean isSuperscript() {
        sync();
        return superscript;
    }

    /** Cambiaron los atributos: lo guardado dejo de valer. */
    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        valido = false;
        super.changedUpdate(e, a, f);
    }
}
