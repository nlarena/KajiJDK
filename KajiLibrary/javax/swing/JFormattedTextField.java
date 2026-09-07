package javax.swing;

import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.InputMethodEvent;
import java.text.Format;
import java.text.ParseException;

import javax.swing.text.AttributeSet;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.NavigationFilter;
import javax.swing.text.NumberFormatter;
import javax.swing.text.TextAction;

/**
 * Un campo de texto que guarda un valor, no una cadena.
 *
 * <h2>El valor y el texto son dos cosas</h2>
 *
 * <p>Un campo de texto comun tiene texto. Este tiene un <em>valor</em> (una fecha, un numero, lo
 * que sea) y un formateador que lo convierte a texto para mostrarlo y de vuelta a valor cuando el
 * usuario termina de editar. El texto que se ve es una representacion; el valor es lo que importa.
 *
 * <p>Que sean dos cosas es lo que permite que el campo muestre <code>1.234,50</code> y el programa
 * lea un {@code double}. Y tambien lo que obliga a decidir que pasa cuando el texto no se puede
 * convertir: eso es {@link #setFocusLostBehavior}.
 *
 * <h2>Por que hay una fabrica y no un formateador</h2>
 *
 * <p>Muchas veces el texto que se muestra y el que se edita no son el mismo: un importe se muestra
 * con signo de moneda y se edita sin el. La fabrica devuelve un formateador distinto segun si el
 * campo tiene el foco o no. Con un solo formateador habria que elegir uno de los dos.
 */
public class JFormattedTextField extends JTextField {

    private static final String uiClassID = "FormattedTextFieldUI";
    private static final Action[] defaultActions = {new CommitAction(), new CancelAction()};

    /** Al perder el foco, pasar el texto al valor; si no se puede, dejarlo invalido. */
    public static final int COMMIT = 0;

    /** Igual, pero si no se puede volver al ultimo valor bueno. */
    public static final int COMMIT_OR_REVERT = 1;

    /** Al perder el foco, volver siempre al valor. */
    public static final int REVERT = 2;

    /** Al perder el foco, no hacer nada. */
    public static final int PERSIST = 3;

    private AbstractFormatterFactory factory;
    private AbstractFormatter format;
    private Object value;
    private boolean editValid;
    private int focusLostBehavior;
    private boolean edited;
    private boolean composedTextExists = false;

    /** Un campo vacio, sin formateador todavia. */
    public JFormattedTextField() {
        super();
        enableEvents(java.awt.AWTEvent.FOCUS_EVENT_MASK);
        setFocusLostBehavior(COMMIT_OR_REVERT);
    }

    /** Un campo con ese valor; el formateador sale del tipo del valor. */
    public JFormattedTextField(Object value) {
        this();
        setValue(value);
    }

    /** Un campo que usa ese formato de {@code java.text}. */
    public JFormattedTextField(Format format) {
        this();
        setFormatterFactory(getDefaultFormatterFactory(format));
    }

    /** Un campo con ese formateador, para editar y para mostrar. */
    public JFormattedTextField(AbstractFormatter formatter) {
        this(new DefaultFormatterFactory(formatter));
    }

    /** Un campo con esa fabrica de formateadores. */
    public JFormattedTextField(AbstractFormatterFactory factory) {
        this();
        setFormatterFactory(factory);
    }

    /** Un campo con esa fabrica y ese valor inicial. */
    public JFormattedTextField(AbstractFormatterFactory factory, Object currentValue) {
        this(currentValue);
        setFormatterFactory(factory);
    }

    /** Que hacer cuando el campo pierde el foco; ver la nota de la clase. */
    public void setFocusLostBehavior(int behavior) {
        if (behavior != COMMIT && behavior != COMMIT_OR_REVERT
                && behavior != PERSIST && behavior != REVERT) {
            throw new IllegalArgumentException("setFocusLostBehavior must be one of: "
                    + "JFormattedTextField.COMMIT, JFormattedTextField.COMMIT_OR_REVERT, "
                    + "JFormattedTextField.PERSIST or JFormattedTextField.REVERT");
        }
        focusLostBehavior = behavior;
    }

    public int getFocusLostBehavior() {
        return focusLostBehavior;
    }

    /** La fabrica de formateadores; cambiarla vuelve a formatear el valor. */
    public void setFormatterFactory(AbstractFormatterFactory tf) {
        AbstractFormatterFactory oldFactory = factory;
        factory = tf;
        firePropertyChange("formatterFactory", oldFactory, tf);
        setValue(getValue(), true, false);
    }

    public AbstractFormatterFactory getFormatterFactory() {
        return factory;
    }

    /**
     * El formateador que esta puesto ahora.
     *
     * <p>No lo pone quien usa el campo sino el campo mismo, pidiendoselo a la fabrica. Es
     * protegido justamente para eso.
     */
    protected void setFormatter(AbstractFormatter format) {
        AbstractFormatter oldFormat = this.format;
        if (oldFormat != null) {
            oldFormat.uninstall();
        }
        setEditValid(true);
        this.format = format;
        if (format != null) {
            format.install(this);
        }
        setEdited(false);
        firePropertyChange("textFormatter", oldFormat, format);
    }

    public AbstractFormatter getFormatter() {
        return format;
    }

    /** Cambia el valor y actualiza el texto. */
    public void setValue(Object value) {
        if (getFormatterFactory() == null) {
            setFormatterFactory(getDefaultFormatterFactory(value));
        }
        setValue(value, true, true);
    }

    public Object getValue() {
        return value;
    }

    /**
     * Pasa el texto que se ve al valor.
     *
     * @throws ParseException si el texto no se puede convertir.
     */
    public void commitEdit() throws ParseException {
        AbstractFormatter format = getFormatter();
        if (format != null) {
            setValue(format.stringToValue(getText()), false, true);
        }
    }

    /** Si lo que se escribio hasta ahora se puede convertir a un valor. */
    public boolean isEditValid() {
        return editValid;
    }

    /** Avisa que lo que se escribio no sirve; el aspecto suele hacer sonar un pitido. */
    protected void invalidEdit() {
        UIManagerBeep();
    }

    private void UIManagerBeep() {
        java.awt.Toolkit.getDefaultToolkit().beep();
    }

    protected void processInputMethodEvent(InputMethodEvent e) {
        java.text.AttributedCharacterIterator text = e.getText();
        int commitCount = e.getCommittedCharacterCount();
        // Un texto que se esta componiendo (por ejemplo en japones) no se valida hasta que
        // termina: validarlo a medias marcaria como invalido algo que todavia no es nada.
        composedTextExists = ((text != null)
                && (text.getEndIndex() - (text.getBeginIndex() + commitCount)) > 0);
        super.processInputMethodEvent(e);
    }

    /** Al ganar o perder el foco cambia el formateador, y a veces el valor. */
    protected void processFocusEvent(FocusEvent e) {
        super.processFocusEvent(e);
        if (composedTextExists) {
            return;
        }
        if (e.isTemporary()) {
            return;
        }
        if (isEdited() && e.getID() == FocusEvent.FOCUS_LOST) {
            int fb = getFocusLostBehavior();
            if (fb == JFormattedTextField.COMMIT
                    || fb == JFormattedTextField.COMMIT_OR_REVERT) {
                try {
                    commitEdit();
                    // Volver a formatear: el valor puede verse distinto ya guardado.
                    setValue(getValue(), true, true);
                } catch (ParseException pe) {
                    if (fb == JFormattedTextField.COMMIT_OR_REVERT) {
                        setValue(getValue(), true, true);
                    }
                }
            } else if (fb == JFormattedTextField.REVERT) {
                setValue(getValue(), true, true);
            }
        }
        setFormatter(getFormatterFactory() == null ? null
                : getFormatterFactory().getFormatter(this));
    }

    public Action[] getActions() {
        return TextAction.augmentList(super.getActions(), defaultActions);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /** Cambiar el documento vuelve a poner el texto del valor. */
    public void setDocument(Document doc) {
        super.setDocument(doc);
    }

    void setEditValid(boolean isValid) {
        if (isValid != editValid) {
            editValid = isValid;
            firePropertyChange("editValid", Boolean.valueOf(!isValid), Boolean.valueOf(isValid));
        }
    }

    void setEdited(boolean edited) {
        this.edited = edited;
    }

    boolean isEdited() {
        return edited;
    }

    private void setValue(Object value, boolean createFormat, boolean firePC) {
        Object oldValue = this.value;
        this.value = value;

        if (createFormat) {
            AbstractFormatterFactory factory = getFormatterFactory();
            AbstractFormatter atf = (factory != null) ? factory.getFormatter(this) : null;
            setFormatter(atf);
        } else {
            setEdited(false);
        }
        if (firePC) {
            firePropertyChange("value", oldValue, value);
        }
    }

    /**
     * Elige una fabrica mirando el tipo del valor.
     *
     * <p>Es lo que hace que un campo construido con una fecha ya sepa formatear fechas sin que
     * nadie diga nada.
     */
    private AbstractFormatterFactory getDefaultFormatterFactory(Object type) {
        if (type instanceof java.text.DateFormat) {
            return new DefaultFormatterFactory(
                    new DateFormatter((java.text.DateFormat) type));
        }
        if (type instanceof java.text.NumberFormat) {
            return new DefaultFormatterFactory(
                    new NumberFormatter((java.text.NumberFormat) type));
        }
        if (type instanceof Format) {
            return new DefaultFormatterFactory(
                    new javax.swing.text.InternationalFormatter((Format) type));
        }
        if (type instanceof java.util.Date) {
            return new DefaultFormatterFactory(new DateFormatter());
        }
        if (type instanceof Number) {
            AbstractFormatter displayFormatter = new NumberFormatter();
            ((javax.swing.text.NumberFormatter) displayFormatter).setValueClass(type.getClass());
            AbstractFormatter editFormatter = new NumberFormatter(
                    new java.text.DecimalFormat("#.#"));
            ((javax.swing.text.NumberFormatter) editFormatter).setValueClass(type.getClass());
            return new DefaultFormatterFactory(displayFormatter, displayFormatter,
                    editFormatter);
        }
        return new DefaultFormatterFactory(new javax.swing.text.DefaultFormatter());
    }

    /**
     * Convierte entre el valor y el texto que se ve.
     *
     * <p>Ademas de convertir puede controlar la edicion: {@link #getDocumentFilter} deja filtrar
     * lo que se escribe y {@link #getNavigationFilter} donde se puede parar el cursor. Con eso se
     * arma una mascara donde ciertas posiciones no se pueden tocar.
     */
    public abstract static class AbstractFormatter implements java.io.Serializable {

        private JFormattedTextField ftf;

        protected AbstractFormatter() {
        }

        /**
         * Se engancha al campo: pone el texto del valor y los filtros.
         *
         * <p>Si el valor no se puede formatear, el campo queda vacio e invalido; dejar el texto
         * anterior mostraria algo que ya no corresponde al valor.
         */
        public void install(JFormattedTextField ftf) {
            if (this.ftf != null) {
                uninstall();
            }
            this.ftf = ftf;
            if (ftf != null) {
                try {
                    ftf.setText(valueToString(ftf.getValue()));
                } catch (ParseException pe) {
                    ftf.setText("");
                    setEditValid(false);
                }
                installDocumentFilter(getDocumentFilter());
                ftf.setNavigationFilter(getNavigationFilter());
                ftf.setFormatterActions(getActions());
            }
        }

        public void uninstall() {
            if (this.ftf != null) {
                installDocumentFilter(null);
                this.ftf.setNavigationFilter(null);
                this.ftf.setFormatterActions(null);
            }
            this.ftf = null;
        }

        /** El valor que representa esa cadena. */
        public abstract Object stringToValue(String text) throws ParseException;

        /** El texto que representa ese valor. */
        public abstract String valueToString(Object value) throws ParseException;

        protected JFormattedTextField getFormattedTextField() {
            return ftf;
        }

        protected void invalidEdit() {
            JFormattedTextField ftf = getFormattedTextField();
            if (ftf != null) {
                ftf.invalidEdit();
            }
        }

        protected void setEditValid(boolean valid) {
            JFormattedTextField ftf = getFormattedTextField();
            if (ftf != null) {
                ftf.setEditValid(valid);
            }
        }

        /** Acciones propias del formateador; ninguna, salvo que la subclase agregue. */
        protected Action[] getActions() {
            return null;
        }

        /** El filtro que decide que se puede escribir. */
        protected DocumentFilter getDocumentFilter() {
            return null;
        }

        /** El filtro que decide donde se puede parar el cursor. */
        protected NavigationFilter getNavigationFilter() {
            return null;
        }

        protected Object clone() throws CloneNotSupportedException {
            AbstractFormatter formatter = (AbstractFormatter) super.clone();
            formatter.ftf = null;
            return formatter;
        }

        private void installDocumentFilter(DocumentFilter filter) {
            JFormattedTextField ftf = getFormattedTextField();
            if (ftf != null) {
                Document doc = ftf.getDocument();
                if (doc instanceof javax.swing.text.AbstractDocument) {
                    ((javax.swing.text.AbstractDocument) doc).setDocumentFilter(filter);
                }
                doc.putProperty(DocumentFilter.class, null);
            }
        }
    }

    /** Devuelve el formateador que corresponde al estado del campo. */
    public abstract static class AbstractFormatterFactory {

        protected AbstractFormatterFactory() {
        }

        /** El formateador para ese campo, ahora. */
        public abstract AbstractFormatter getFormatter(JFormattedTextField tf);
    }

    private Action[] formatterActions;

    void setFormatterActions(Action[] actions) {
        formatterActions = actions;
    }

    /** Pasa el texto al valor; la accion del Enter. */
    static class CommitAction extends TextAction {

        CommitAction() {
            super("notify-field-accept");
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getFocusedComponent();
            if (target instanceof JFormattedTextField) {
                JFormattedTextField ftf = (JFormattedTextField) target;
                try {
                    ftf.commitEdit();
                    ftf.setValue(ftf.getValue(), true, true);
                } catch (ParseException pe) {
                    ftf.invalidEdit();
                    return;
                }
            }
            if (target instanceof JTextField) {
                ((JTextField) target).postActionEvent();
            }
        }

        public boolean isEnabled() {
            return true;
        }
    }

    /** Vuelve al ultimo valor bueno; la accion del Escape. */
    static class CancelAction extends TextAction {

        CancelAction() {
            super("reset-field-edit");
        }

        public void actionPerformed(ActionEvent e) {
            JTextComponent target = getFocusedComponent();
            if (target instanceof JFormattedTextField) {
                JFormattedTextField ftf = (JFormattedTextField) target;
                ftf.setValue(ftf.getValue(), true, true);
            }
        }

        public boolean isEnabled() {
            return true;
        }
    }
}
