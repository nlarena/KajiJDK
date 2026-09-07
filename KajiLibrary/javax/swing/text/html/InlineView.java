package javax.swing.text.html;

import java.awt.Shape;

import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.LabelView;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * La vista del texto que va en la linea.
 *
 * <h2>Que agrega sobre una etiqueta comun</h2>
 *
 * <p>Una {@link LabelView} dibuja texto con una tipografia. Esta saca esa tipografia y ese color de
 * la hoja de estilos en lugar de los atributos de Swing, y ademas hace caso a
 * <code>white-space: nowrap</code>.
 *
 * <h2>Cortar o no cortar</h2>
 *
 * <p>{@link #getBreakWeight} contesta cuanto le conviene a esta vista partirse para que el parrafo
 * entre en el ancho. Con <code>nowrap</code> contesta que no se parte de ninguna manera, y entonces
 * el parrafo se pasa de largo. Es lo que pide esa propiedad: mejor una linea larga que una palabra
 * cortada donde el autor dijo que no.
 */
public class InlineView extends LabelView {

    private AttributeSet attr;
    private boolean sinCorte;

    /** Una vista de texto en linea sobre ese elemento. */
    public InlineView(Element elem) {
        super(elem);
        StyleSheet sheet = getStyleSheet();
        attr = (sheet == null) ? null : sheet.getViewAttributes(this);
    }

    public void insertUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.insertUpdate(e, a, f);
    }

    public void removeUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.removeUpdate(e, a, f);
    }

    public void changedUpdate(DocumentEvent e, Shape a, ViewFactory f) {
        super.changedUpdate(e, a, f);
        StyleSheet sheet = getStyleSheet();
        attr = (sheet == null) ? null : sheet.getViewAttributes(this);
        preferenceChanged(null, true, true);
    }

    public AttributeSet getAttributes() {
        return (attr == null) ? super.getAttributes() : attr;
    }

    /** Cuanto conviene partir aca; cero si el CSS lo prohibe. */
    public int getBreakWeight(int axis, float pos, float len) {
        if (sinCorte) {
            return BadBreakWeight;
        }
        return super.getBreakWeight(axis, pos, len);
    }

    public View breakView(int axis, int offset, float pos, float len) {
        if (sinCorte) {
            return this;
        }
        return super.breakView(axis, offset, pos, len);
    }

    /** Lee del CSS lo que cambia como se dibuja el texto. */
    protected void setPropertiesFromAttributes() {
        super.setPropertiesFromAttributes();
        StyleSheet sheet = getStyleSheet();
        if (sheet == null) {
            return;
        }
        AttributeSet a = getAttributes();
        Object ws = a.getAttribute(CSS.Attribute.WHITE_SPACE);
        sinCorte = (ws != null && "nowrap".equals(ws.toString()));
        // El color y la tipografia no se ponen desde aca: los pide GlyphView a los atributos, y
        // los atributos ya son los que resolvio la hoja.
        Object dec = a.getAttribute(CSS.Attribute.TEXT_DECORATION);
        String d = (dec == null) ? "" : dec.toString();
        setUnderline(d.indexOf("underline") >= 0);
        setStrikeThrough(d.indexOf("line-through") >= 0);
        Object va = a.getAttribute(CSS.Attribute.VERTICAL_ALIGN);
        if (va != null) {
            String v = va.toString();
            setSuperscript(v.indexOf("sup") >= 0);
            setSubscript(v.indexOf("sub") >= 0);
        }
    }

    /** La hoja de estilos del documento, o nulo si el documento no es de HTML. */
    protected StyleSheet getStyleSheet() {
        Document d = getDocument();
        if (d instanceof HTMLDocument) {
            return ((HTMLDocument) d).getStyleSheet();
        }
        return null;
    }
}
