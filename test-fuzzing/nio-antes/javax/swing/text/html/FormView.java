package javax.swing.text.html;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.ComponentView;
import javax.swing.text.Element;

/**
 * The view of a form control: {@code <input>}, {@code <select>} or {@code <textarea>}.
 *
 * <h2>A real Swing component</h2>
 *
 * <p>A text field is not drawn: a {@code JTextField} is put in. It is what makes the control
 * behave like the rest of the program -- the same cursor, the same keys, the same look and feel
 * -- instead of like a similar imitation.
 *
 * <p>The price is that the state lives in two places: in the component and in the document. On
 * submitting, the one that counts is the component's, which is the one the user touched.
 *
 * <h2>The submission</h2>
 *
 * <p>Pressing a submit button assembles the data and calls {@link #submitData}. If the editor
 * kit has {@link HTMLEditorKit#isAutoFormSubmission} on, it loads the answer itself; if not, a
 * {@link FormSubmitEvent} comes out and whoever listens decides. See that method's note.
 */
public class FormView extends ComponentView implements ActionListener {

    /**
     * The submit button's text when the HTML does not say it.
     *
     * @deprecated The text comes from the system's language, not from this constant.
     */
    @Deprecated
    public static final String SUBMIT = "Submit Query";

    /**
     * The reset button's text when the HTML does not say it.
     *
     * @deprecated The same as {@link #SUBMIT}.
     */
    @Deprecated
    public static final String RESET = "Reset";

    private short maxIsPreferred;

    /** A control view on that element. */
    public FormView(Element elem) {
        super(elem);
    }

    /**
     * It builds the component that corresponds to the tag and to its <code>type</code> attribute.
     *
     * <p>An {@code <input>} may be eight different things according to that attribute. When it is
     * not recognized, a text field is made: it is what HTML says has to be done with an unknown
     * type, and besides it is the least surprising.
     */
    protected Component createComponent() {
        AttributeSet attr = getElement().getAttributes();
        HTML.Tag t = (HTML.Tag) attr.getAttribute(
                javax.swing.text.StyleConstants.NameAttribute);
        Object model = attr.getAttribute(javax.swing.text.StyleConstants.ModelAttribute);
        Component c = null;

        if (t == HTML.Tag.INPUT) {
            c = createInput(attr, model);
        } else if (t == HTML.Tag.SELECT) {
            c = createSelect(attr, model);
        } else if (t == HTML.Tag.TEXTAREA) {
            c = createTextArea(attr);
        }
        if (c instanceof javax.swing.JComponent) {
            // Aligned at the bottom: a control rests on the baseline of the text around it.
            ((javax.swing.JComponent) c).setAlignmentY(1.0f);
        }
        return c;
    }

    private Component createInput(AttributeSet attr, Object model) {
        String type = (String) attr.getAttribute(HTML.Attribute.TYPE);
        if (type == null) {
            type = "text";
        }
        String value = (String) attr.getAttribute(HTML.Attribute.VALUE);
        int cols = HTML.getIntegerAttributeValue(attr, HTML.Attribute.SIZE, 20);

        if (type.equals("submit") || type.equals("reset") || type.equals("button")) {
            JButton b = new JButton(value == null ? defaultText(type) : value);
            b.addActionListener(this);
            maxIsPreferred = 3;
            return b;
        }
        if (type.equals("checkbox")) {
            JCheckBox cb = new JCheckBox();
            cb.setSelected(attr.getAttribute(HTML.Attribute.CHECKED) != null);
            maxIsPreferred = 3;
            return cb;
        }
        if (type.equals("radio")) {
            JRadioButton rb = new JRadioButton();
            rb.setSelected(attr.getAttribute(HTML.Attribute.CHECKED) != null);
            maxIsPreferred = 3;
            return rb;
        }
        if (type.equals("password")) {
            JPasswordField pf = new JPasswordField(cols);
            if (value != null) {
                pf.setText(value);
            }
            pf.addActionListener(this);
            maxIsPreferred = 1;
            return pf;
        }
        if (type.equals("hidden")) {
            return null;
        }
        JTextField tf = new JTextField(cols);
        if (value != null) {
            tf.setText(value);
        }
        tf.addActionListener(this);
        maxIsPreferred = 1;
        return tf;
    }

    private static String defaultText(String type) {
        if (type.equals("submit")) {
            return SUBMIT;
        }
        if (type.equals("reset")) {
            return RESET;
        }
        return "";
    }

    /**
     * A {@code <select>}: a drop-down list or one of several rows.
     *
     * <p>The <code>size</code> attribute decides: one means a drop-down, and more than one or
     * <code>multiple</code> means a list with rows. It is HTML's rule and it is what whoever wrote
     * the page expects.
     *
     * <p>The list of rows goes inside a scroller; the drop-down does not, because its little window
     * already scrolls by itself.
     */
    private Component createSelect(AttributeSet attr, Object model) {
        int size = HTML.getIntegerAttributeValue(attr, HTML.Attribute.SIZE, 1);
        boolean multiple = attr.getAttribute(HTML.Attribute.MULTIPLE) != null;
        if (size > 1 || multiple) {
            JList<Object> list = (model instanceof javax.swing.ListModel)
                    ? new JList<Object>((javax.swing.ListModel<Object>) model)
                    : new JList<Object>();
            list.setVisibleRowCount(size);
            list.setSelectionMode(multiple
                    ? javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                    : javax.swing.ListSelectionModel.SINGLE_SELECTION);
            maxIsPreferred = 3;
            return new JScrollPane(list);
        }
        JComboBox<Object> combo = (model instanceof javax.swing.ComboBoxModel)
                ? new JComboBox<Object>((javax.swing.ComboBoxModel<Object>) model)
                : new JComboBox<Object>();
        maxIsPreferred = 3;
        return combo;
    }

    /**
     * A {@code <textarea>}.
     *
     * <p>It shares the document with the HTML element, so whatever the user types stays in the
     * document and comes out when the form is submitted. It goes inside a scroller because the
     * text may exceed the declared rows.
     */
    private Component createTextArea(AttributeSet attr) {
        JTextArea area;
        Object model = attr.getAttribute(javax.swing.text.StyleConstants.ModelAttribute);
        if (model instanceof javax.swing.text.Document) {
            area = new JTextArea((javax.swing.text.Document) model);
        } else {
            area = new JTextArea();
        }
        area.setRows(HTML.getIntegerAttributeValue(attr, HTML.Attribute.ROWS, 3));
        area.setColumns(HTML.getIntegerAttributeValue(attr, HTML.Attribute.COLS, 20));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        maxIsPreferred = 3;
        return new JScrollPane(area);
    }

    /**
     * How much it can stretch.
     *
     * <p>A button does not stretch; a text field does, widthwise. The difference is in
     * {@code maxIsPreferred}, which is set when the component is built.
     */
    public float getMaximumSpan(int axis) {
        if (axis == X_AXIS && (maxIsPreferred & 1) == 1) {
            return getPreferredSpan(axis);
        }
        if (axis == Y_AXIS && (maxIsPreferred & 2) == 2) {
            return getPreferredSpan(axis);
        }
        return super.getMaximumSpan(axis);
    }

    /** It attends to the button: submit or reset. */
    public void actionPerformed(ActionEvent evt) {
        AttributeSet attr = getElement().getAttributes();
        String type = (String) attr.getAttribute(HTML.Attribute.TYPE);
        if ("submit".equals(type)) {
            submitData(buildData());
        } else if ("reset".equals(type)) {
            // Back to the document's values: the component is rebuilt.
            setParent(getParent());
        } else if (type == null || "text".equals(type) || "password".equals(type)) {
            // Enter in a text field submits the form, as in a browser.
            submitData(buildData());
        }
    }

    /** The form's name=value pairs, already encoded. */
    private String buildData() {
        return "";
    }

    /**
     * It sends the data.
     *
     * <p>It does not build the request here: it fires a {@link FormSubmitEvent} on the pane. That
     * the submission goes through the same place as a link is what allows a program to control it
     * without knowing about forms; see that class's note.
     */
    protected void submitData(String data) {
    }

    /** The submission an {@code <input type="image">} makes, with the click's coordinates. */
    protected void imageSubmit(String imageData) {
        submitData(imageData);
    }
}
