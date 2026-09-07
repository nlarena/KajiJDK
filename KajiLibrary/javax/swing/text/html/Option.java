package javax.swing.text.html;

import java.io.Serializable;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;

/**
 * Una opcion de un {@code <select>}.
 *
 * <h2>Por que hay una clase para esto</h2>
 *
 * <p>El resto del HTML se muestra con vistas sobre el documento. Una lista desplegable no: se
 * muestra con un {@code JComboBox} o un {@code JList} de Swing de verdad, y esos componentes
 * necesitan objetos comunes en su modelo, no elementos del documento.
 *
 * <p>Esta clase es ese objeto. Guarda los atributos de la etiqueta y el texto que va entre
 * <code>&lt;option&gt;</code> y su cierre, y su {@link #toString} es lo que la lista dibuja.
 *
 * <h2>El valor no es la etiqueta</h2>
 *
 * <p>{@link #getValue} devuelve el atributo <code>value</code> y, si no esta, el texto visible.
 * Esa regla es del HTML, no una comodidad: un formulario que envia
 * <code>&lt;option value="ar"&gt;Argentina&lt;/option&gt;</code> tiene que mandar
 * <code>ar</code>, y si la etiqueta no trae <code>value</code>, entonces si manda el texto.
 */
public class Option implements Serializable {

    private boolean selected;
    private String label;
    private AttributeSet attr;

    /**
     * Una opcion con esos atributos.
     *
     * <p>Los atributos se copian: el documento puede cambiar los suyos, y la opcion que ya esta en
     * una lista no deberia cambiar sola.
     */
    public Option(AttributeSet attr) {
        this.attr = attr.copyAttributes();
        selected = (attr.getAttribute(HTML.Attribute.SELECTED) != null);
    }

    /** El texto que se ve. */
    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Los atributos de la etiqueta, ya copiados. */
    public AttributeSet getAttributes() {
        return attr;
    }

    /** Lo que dibuja la lista: el texto visible. */
    public String toString() {
        return label;
    }

    /** Lo pone el documento al leer el HTML; ver {@link #isSelected}. */
    protected void setSelection(boolean state) {
        selected = state;
    }

    /** Si la opcion viene marcada. */
    public boolean isSelected() {
        return selected;
    }

    /** El valor que se envia; ver la nota de la clase. */
    public String getValue() {
        String value = (String) attr.getAttribute(HTML.Attribute.VALUE);
        if (value == null) {
            value = label;
        }
        return value;
    }
}
