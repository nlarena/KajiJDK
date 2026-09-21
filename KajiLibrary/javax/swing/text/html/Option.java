package javax.swing.text.html;

import java.io.Serializable;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;

/**
 * An option of a {@code <select>}.
 *
 * <h2>Why there is a class for this</h2>
 *
 * <p>The rest of the HTML is shown with views over the document. A drop-down list is not: it is
 * shown with a real Swing {@code JComboBox} or {@code JList}, and those components need ordinary
 * objects in their model, not document elements.
 *
 * <p>This class is that object. It keeps the tag's attributes and the text that goes between
 * <code>&lt;option&gt;</code> and its closing, and its {@link #toString} is what the list draws.
 *
 * <h2>The value is not the label</h2>
 *
 * <p>{@link #getValue} returns the <code>value</code> attribute and, if it is not there, the
 * visible text. That rule is HTML's, not a convenience: a form that submits
 * <code>&lt;option value="ar"&gt;Argentina&lt;/option&gt;</code> has to send <code>ar</code>,
 * and if the tag does not carry <code>value</code>, then it does send the text.
 */
public class Option implements Serializable {

    private boolean selected;
    private String label;
    private AttributeSet attr;

    /**
     * An option with those attributes.
     *
     * <p>The attributes are copied: the document may change its own, and the option that is already
     * in a list should not change by itself.
     */
    public Option(AttributeSet attr) {
        this.attr = attr.copyAttributes();
        selected = (attr.getAttribute(HTML.Attribute.SELECTED) != null);
    }

    /** The text that is seen. */
    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** The tag's attributes, already copied. */
    public AttributeSet getAttributes() {
        return attr;
    }

    /** What the list draws: the visible text. */
    public String toString() {
        return label;
    }

    /** The document sets it when reading the HTML; see {@link #isSelected}. */
    protected void setSelection(boolean state) {
        selected = state;
    }

    /** Whether the option comes marked. */
    public boolean isSelected() {
        return selected;
    }

    /** The value that is submitted; see the class note. */
    public String getValue() {
        String value = (String) attr.getAttribute(HTML.Attribute.VALUE);
        if (value == null) {
            value = label;
        }
        return value;
    }
}
