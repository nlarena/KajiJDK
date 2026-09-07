package javax.swing.text.html;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;

import javax.swing.SizeRequirements;
import javax.swing.event.DocumentEvent;
import javax.swing.text.AttributeSet;
import javax.swing.text.BoxView;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.StyleConstants;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;

/**
 * La vista de un elemento de HTML que arma bloque.
 *
 * <h2>Que agrega sobre una caja comun</h2>
 *
 * <p>Una {@link BoxView} apila a sus hijos. Esta ademas mira el CSS: toma los margenes y el fondo
 * de la hoja de estilos, y hace caso a un <code>width</code> o un <code>height</code> declarados.
 *
 * <p>Los atributos se leen en {@link #setPropertiesFromAttributes}, y eso ocurre al colgarse de un
 * padre y cada vez que el documento cambia los atributos. No se puede hacer en el constructor: sin
 * padre no hay documento, y sin documento no hay hoja de estilos que consultar.
 */
public class BlockView extends BoxView {

    private AttributeSet attr;
    private StyleSheet.BoxPainter painter;
    private float anchoPedido = -1;
    private float altoPedido = -1;

    /** Una vista de bloque sobre ese eje. */
    public BlockView(Element elem, int axis) {
        super(elem, axis);
    }

    public void setParent(View parent) {
        super.setParent(parent);
        if (parent != null) {
            setPropertiesFromAttributes();
        }
    }

    protected SizeRequirements calculateMajorAxisRequirements(int axis, SizeRequirements r) {
        SizeRequirements rr = super.calculateMajorAxisRequirements(axis, r);
        return ajustar(axis, rr);
    }

    protected SizeRequirements calculateMinorAxisRequirements(int axis, SizeRequirements r) {
        SizeRequirements rr = super.calculateMinorAxisRequirements(axis, r);
        return ajustar(axis, rr);
    }

    /**
     * Impone el tamano declarado en el CSS, si lo hay.
     *
     * <p>Un <code>width</code> declarado fija el minimo, el preferido y el maximo en el mismo
     * valor. Fijar solo el preferido no alcanzaria: el reparto de la caja de arriba lo estiraria
     * igual.
     */
    private SizeRequirements ajustar(int axis, SizeRequirements r) {
        float pedido = (axis == X_AXIS) ? anchoPedido : altoPedido;
        if (pedido > 0) {
            r.minimum = (int) pedido;
            r.preferred = (int) pedido;
            r.maximum = (int) pedido;
        }
        return r;
    }

    protected void layoutMinorAxis(int targetSpan, int axis, int[] offsets, int[] spans) {
        super.layoutMinorAxis(targetSpan, axis, offsets, spans);
    }

    /** Dibuja el fondo del bloque y despues los hijos. */
    public void paint(Graphics g, Shape allocation) {
        Rectangle a = (Rectangle) allocation;
        if (painter != null) {
            painter.paint(g, a.x, a.y, a.width, a.height, this);
        }
        super.paint(g, a);
    }

    /**
     * Los atributos de la vista: los del elemento mas los que aporte la hoja.
     *
     * <p>Se calculan una vez y se guardan. Volver a resolverlos en cada consulta seria correcto y
     * seria lento: se consultan a cada linea que se dibuja.
     */
    public AttributeSet getAttributes() {
        if (attr == null) {
            StyleSheet sheet = getStyleSheet();
            attr = (sheet == null) ? super.getAttributes()
                    : sheet.getViewAttributes(this);
        }
        return attr;
    }

    /** Un bloque se estira en el eje menor y no en el mayor. */
    public int getResizeWeight(int axis) {
        if (axis == X_AXIS) {
            return 1;
        }
        return 0;
    }

    public float getAlignment(int axis) {
        if (axis == X_AXIS) {
            return 0;
        }
        return super.getAlignment(axis);
    }

    public void changedUpdate(DocumentEvent changes, Shape a, ViewFactory f) {
        super.changedUpdate(changes, a, f);
        int pos = changes.getOffset();
        if (pos <= getStartOffset() && (pos + changes.getLength()) >= getEndOffset()) {
            setPropertiesFromAttributes();
        }
    }

    public float getPreferredSpan(int axis) {
        return super.getPreferredSpan(axis);
    }

    public float getMinimumSpan(int axis) {
        return super.getMinimumSpan(axis);
    }

    public float getMaximumSpan(int axis) {
        return super.getMaximumSpan(axis);
    }

    /** Lee margenes, fondo y tamano de la hoja de estilos. */
    protected void setPropertiesFromAttributes() {
        attr = null;
        StyleSheet sheet = getStyleSheet();
        if (sheet == null) {
            return;
        }
        AttributeSet a = getAttributes();
        painter = sheet.getBoxPainter(a);
        setInsets((short) painter.getInset(TOP, this), (short) painter.getInset(LEFT, this),
                (short) painter.getInset(BOTTOM, this), (short) painter.getInset(RIGHT, this));
        anchoPedido = medida(a, CSS.Attribute.WIDTH);
        altoPedido = medida(a, CSS.Attribute.HEIGHT);
    }

    /** Un largo declarado en pixeles, o -1 si no hay o no se entiende. */
    private static float medida(AttributeSet a, CSS.Attribute clave) {
        Object o = a.getAttribute(clave);
        if (o == null) {
            return -1;
        }
        String s = o.toString().trim();
        if (s.endsWith("px") || s.endsWith("pt")) {
            s = s.substring(0, s.length() - 2).trim();
        }
        try {
            return Float.parseFloat(s);
        } catch (NumberFormatException nfe) {
            // Un porcentaje o algo raro: se deja que la caja decida.
            return -1;
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
