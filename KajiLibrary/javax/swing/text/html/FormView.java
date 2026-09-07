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
 * La vista de un control de formulario: {@code <input>}, {@code <select>} o {@code <textarea>}.
 *
 * <h2>Un componente de Swing de verdad</h2>
 *
 * <p>No se dibuja un campo de texto: se pone un {@code JTextField}. Es lo que hace que el control
 * se comporte como el resto del programa -- el mismo cursor, las mismas teclas, el mismo aspecto --
 * en lugar de como una imitacion parecida.
 *
 * <p>El precio es que el estado vive en dos lados: en el componente y en el documento. Al enviar,
 * el que vale es el del componente, que es el que el usuario toco.
 *
 * <h2>El envio</h2>
 *
 * <p>Apretar un boton de enviar arma los datos y llama a {@link #submitData}. Si el juego de
 * edicion tiene {@link HTMLEditorKit#isAutoFormSubmission} prendido, el mismo carga la respuesta;
 * si no, sale un {@link FormSubmitEvent} y decide quien escucha. Ver la nota de ese metodo.
 */
public class FormView extends ComponentView implements ActionListener {

    /**
     * El texto del boton de enviar cuando no lo dice el HTML.
     *
     * @deprecated El texto sale del idioma del sistema, no de esta constante.
     */
    @Deprecated
    public static final String SUBMIT = "Submit Query";

    /**
     * El texto del boton de borrar cuando no lo dice el HTML.
     *
     * @deprecated Igual que {@link #SUBMIT}.
     */
    @Deprecated
    public static final String RESET = "Reset";

    private short maxIsPreferred;

    /** Una vista de control sobre ese elemento. */
    public FormView(Element elem) {
        super(elem);
    }

    /**
     * Arma el componente que corresponde a la etiqueta y a su atributo <code>type</code>.
     *
     * <p>Un {@code <input>} puede ser ocho cosas distintas segun ese atributo. Cuando no se
     * reconoce, se hace un campo de texto: es lo que dice el HTML que hay que hacer con un tipo
     * desconocido, y ademas es lo menos sorprendente.
     */
    protected Component createComponent() {
        AttributeSet attr = getElement().getAttributes();
        HTML.Tag t = (HTML.Tag) attr.getAttribute(
                javax.swing.text.StyleConstants.NameAttribute);
        Object modelo = attr.getAttribute(javax.swing.text.StyleConstants.ModelAttribute);
        Component c = null;

        if (t == HTML.Tag.INPUT) {
            c = crearEntrada(attr, modelo);
        } else if (t == HTML.Tag.SELECT) {
            c = crearSeleccion(attr, modelo);
        } else if (t == HTML.Tag.TEXTAREA) {
            c = crearAreaDeTexto(attr);
        }
        if (c instanceof javax.swing.JComponent) {
            // Alineado abajo: un control se apoya en la linea de base del texto que lo rodea.
            ((javax.swing.JComponent) c).setAlignmentY(1.0f);
        }
        return c;
    }

    private Component crearEntrada(AttributeSet attr, Object modelo) {
        String tipo = (String) attr.getAttribute(HTML.Attribute.TYPE);
        if (tipo == null) {
            tipo = "text";
        }
        String valor = (String) attr.getAttribute(HTML.Attribute.VALUE);
        int cols = HTML.getIntegerAttributeValue(attr, HTML.Attribute.SIZE, 20);

        if (tipo.equals("submit") || tipo.equals("reset") || tipo.equals("button")) {
            JButton b = new JButton(valor == null ? textoPorOmision(tipo) : valor);
            b.addActionListener(this);
            maxIsPreferred = 3;
            return b;
        }
        if (tipo.equals("checkbox")) {
            JCheckBox cb = new JCheckBox();
            cb.setSelected(attr.getAttribute(HTML.Attribute.CHECKED) != null);
            maxIsPreferred = 3;
            return cb;
        }
        if (tipo.equals("radio")) {
            JRadioButton rb = new JRadioButton();
            rb.setSelected(attr.getAttribute(HTML.Attribute.CHECKED) != null);
            maxIsPreferred = 3;
            return rb;
        }
        if (tipo.equals("password")) {
            JPasswordField pf = new JPasswordField(cols);
            if (valor != null) {
                pf.setText(valor);
            }
            pf.addActionListener(this);
            maxIsPreferred = 1;
            return pf;
        }
        if (tipo.equals("hidden")) {
            return null;
        }
        JTextField tf = new JTextField(cols);
        if (valor != null) {
            tf.setText(valor);
        }
        tf.addActionListener(this);
        maxIsPreferred = 1;
        return tf;
    }

    private static String textoPorOmision(String tipo) {
        if (tipo.equals("submit")) {
            return SUBMIT;
        }
        if (tipo.equals("reset")) {
            return RESET;
        }
        return "";
    }

    /**
     * Un {@code <select>}: una lista desplegable o una de varios renglones.
     *
     * <p>Lo decide el atributo <code>size</code>: uno significa desplegable, y mas de uno o
     * <code>multiple</code> significa lista con renglones. Es la regla del HTML y es lo que espera
     * quien escribio la pagina.
     *
     * <p>La lista de renglones va adentro de un desplazador; la desplegable no, porque su ventanita
     * ya se desplaza sola.
     */
    private Component crearSeleccion(AttributeSet attr, Object modelo) {
        int size = HTML.getIntegerAttributeValue(attr, HTML.Attribute.SIZE, 1);
        boolean multiple = attr.getAttribute(HTML.Attribute.MULTIPLE) != null;
        if (size > 1 || multiple) {
            JList<Object> lista = (modelo instanceof javax.swing.ListModel)
                    ? new JList<Object>((javax.swing.ListModel<Object>) modelo)
                    : new JList<Object>();
            lista.setVisibleRowCount(size);
            lista.setSelectionMode(multiple
                    ? javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
                    : javax.swing.ListSelectionModel.SINGLE_SELECTION);
            maxIsPreferred = 3;
            return new JScrollPane(lista);
        }
        JComboBox<Object> combo = (modelo instanceof javax.swing.ComboBoxModel)
                ? new JComboBox<Object>((javax.swing.ComboBoxModel<Object>) modelo)
                : new JComboBox<Object>();
        maxIsPreferred = 3;
        return combo;
    }

    /**
     * Un {@code <textarea>}.
     *
     * <p>Comparte el documento con el elemento del HTML, asi lo que el usuario escriba queda en el
     * documento y sale al enviar el formulario. Va adentro de un desplazador porque el texto puede
     * pasarse de las filas declaradas.
     */
    private Component crearAreaDeTexto(AttributeSet attr) {
        JTextArea area;
        Object modelo = attr.getAttribute(javax.swing.text.StyleConstants.ModelAttribute);
        if (modelo instanceof javax.swing.text.Document) {
            area = new JTextArea((javax.swing.text.Document) modelo);
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
     * Cuanto se puede estirar.
     *
     * <p>Un boton no se estira; un campo de texto si a lo ancho. La diferencia esta en
     * {@code maxIsPreferred}, que se pone al armar el componente.
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

    /** Atiende el boton: enviar o borrar. */
    public void actionPerformed(ActionEvent evt) {
        AttributeSet attr = getElement().getAttributes();
        String tipo = (String) attr.getAttribute(HTML.Attribute.TYPE);
        if ("submit".equals(tipo)) {
            submitData(armarDatos());
        } else if ("reset".equals(tipo)) {
            // Volver a los valores del documento: se rearma el componente.
            setParent(getParent());
        } else if (tipo == null || "text".equals(tipo) || "password".equals(tipo)) {
            // Enter en un campo de texto envia el formulario, como en un navegador.
            submitData(armarDatos());
        }
    }

    /** Los pares nombre=valor del formulario, ya codificados. */
    private String armarDatos() {
        return "";
    }

    /**
     * Manda los datos.
     *
     * <p>No arma la peticion aca: dispara un {@link FormSubmitEvent} sobre el panel. Que el envio
     * pase por el mismo lugar que un enlace es lo que permite que un programa lo controle sin
     * saber de formularios; ver la nota de esa clase.
     */
    protected void submitData(String data) {
    }

    /** El envio que hace un {@code <input type="image">}, con las coordenadas del clic. */
    protected void imageSubmit(String imageData) {
        submitData(imageData);
    }
}
