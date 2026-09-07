package javax.swing;

import java.awt.Component;
import java.awt.ComponentOrientation;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.SpinnerUI;
import javax.swing.text.DateFormatter;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;

/**
 * Un campo con dos flechas para recorrer una secuencia.
 *
 * <h2>Tres piezas, no una</h2>
 *
 * <p>El {@link SpinnerModel} sabe cual es el valor y cual viene despues. El <em>editor</em> lo
 * muestra y deja escribirlo. El aspecto dibuja las flechas. Cambiar el modelo cambia la secuencia
 * <em>y</em> el editor, salvo que se haya puesto uno a mano: ver {@link #setEditor}.
 *
 * <h2>Por que el editor es un panel y no un campo</h2>
 *
 * <p>{@link DefaultEditor} es un {@link JPanel} con un {@link JFormattedTextField} adentro, y es su
 * propio acomodador. Podria ser el campo directamente; no lo es porque asi un editor propio puede
 * tener varias piezas -- tres campos para una fecha, por ejemplo -- sin cambiar nada de arriba.
 *
 * <h2>El valor viaja en las dos direcciones</h2>
 *
 * <p>Cuando el modelo cambia, el editor escucha y actualiza el campo. Cuando el usuario escribe y
 * confirma, el campo avisa y el editor se lo pasa al modelo. Si el modelo lo rechaza, el editor
 * devuelve el campo al valor anterior: es lo que evita que quede en pantalla un valor que el modelo
 * nunca acepto.
 */
public class JSpinner extends JComponent implements Accessible {

    private static final String uiClassID = "SpinnerUI";

    /**
     * Una accion apagada, para tapar las del campo de texto.
     *
     * <p>Una atadura de teclas a una accion apagada cuenta como si no existiera, asi que poner esta
     * en el mapa del campo deja que las flechas del control ganen sobre las del campo.
     */
    private static final Action DISABLED_ACTION = new DisabledAction();

    private SpinnerModel model;
    private JComponent editor;
    private ChangeListener modelListener;
    private transient ChangeEvent changeEvent;
    private boolean editorExplicitlySet = false;

    /**
     * Con ese modelo.
     *
     * @throws NullPointerException si el modelo es nulo.
     */
    public JSpinner(SpinnerModel model) {
        if (model == null) {
            throw new NullPointerException("model cannot be null");
        }
        this.model = model;
        this.editor = createEditor(model);
        setOpaque(true);
        updateUI();
    }

    /** Con un {@link SpinnerNumberModel} recien hecho: enteros desde cero, de a uno. */
    public JSpinner() {
        this(new SpinnerNumberModel());
    }

    public SpinnerUI getUI() {
        return (SpinnerUI) ui;
    }

    public void setUI(SpinnerUI ui) {
        super.setUI(ui);
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public void updateUI() {
    }

    /**
     * Elige el editor que le va a ese modelo.
     *
     * <p>El orden importa: se pregunta por fecha y por lista antes que por numero, porque son los
     * casos con editor propio. Un modelo que no sea ninguno de los tres se queda con el editor de
     * base, que muestra el valor pero no deja escribirlo -- no habria como interpretar lo escrito.
     */
    protected JComponent createEditor(SpinnerModel model) {
        if (model instanceof SpinnerDateModel) {
            return new DateEditor(this);
        } else if (model instanceof SpinnerListModel) {
            return new ListEditor(this);
        } else if (model instanceof SpinnerNumberModel) {
            return new NumberEditor(this);
        } else {
            return new DefaultEditor(this);
        }
    }

    /**
     * Cambia el modelo, y con el el editor.
     *
     * <p>Salvo que el editor se haya puesto a mano: en ese caso se respeta, porque cambiarlo
     * borraria una decision del programa.
     *
     * @throws IllegalArgumentException si el modelo es nulo.
     */
    public void setModel(SpinnerModel model) {
        if (model == null) {
            throw new IllegalArgumentException("null model");
        }
        if (!model.equals(this.model)) {
            SpinnerModel oldModel = this.model;
            this.model = model;
            if (modelListener != null) {
                oldModel.removeChangeListener(modelListener);
                this.model.addChangeListener(modelListener);
            }
            firePropertyChange("model", oldModel, model);
            if (!editorExplicitlySet) {
                setEditor(createEditor(model));
                editorExplicitlySet = false;
            }
            repaint();
            revalidate();
        }
    }

    public SpinnerModel getModel() {
        return model;
    }

    /** El valor, preguntandoselo al modelo. */
    public Object getValue() {
        return getModel().getValue();
    }

    /**
     * Cambia el valor.
     *
     * @throws IllegalArgumentException si el modelo no lo acepta.
     */
    public void setValue(Object value) {
        getModel().setValue(value);
    }

    /** El siguiente de la secuencia, o nulo si no hay. */
    public Object getNextValue() {
        return getModel().getNextValue();
    }

    /** El anterior, o nulo si no hay. */
    public Object getPreviousValue() {
        return getModel().getPreviousValue();
    }

    /**
     * Escucha los cambios de valor.
     *
     * <p>El control se anota en el modelo recien cuando alguien se anota en el control, y no antes:
     * un control sin oyentes no tiene por que escuchar a su modelo.
     */
    public void addChangeListener(ChangeListener listener) {
        if (modelListener == null) {
            modelListener = new ModelListener(this);
            getModel().addChangeListener(modelListener);
        }
        listenerList.add(ChangeListener.class, listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        listenerList.remove(ChangeListener.class, listener);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /** Reparte un aviso de cambio; el evento se arma una vez y se reusa. */
    protected void fireStateChanged() {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) listeners[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    /**
     * Pone un editor propio.
     *
     * <p>Al editor que sale se le avisa con {@code dismiss} para que se desanote del modelo; si no,
     * seguiria reaccionando a cambios de un control que ya no muestra.
     *
     * @throws IllegalArgumentException si es nulo.
     */
    public void setEditor(JComponent editor) {
        if (editor == null) {
            throw new IllegalArgumentException("null editor");
        }
        if (!editor.equals(this.editor)) {
            JComponent oldEditor = this.editor;
            this.editor = editor;
            if (oldEditor instanceof DefaultEditor) {
                ((DefaultEditor) oldEditor).dismiss(this);
            }
            editorExplicitlySet = true;
            firePropertyChange("editor", oldEditor, editor);
            revalidate();
            repaint();
        }
    }

    public JComponent getEditor() {
        return editor;
    }

    /**
     * Le pide al editor que confirme lo escrito.
     *
     * @throws ParseException si lo escrito no se puede interpretar.
     */
    public void commitEdit() throws ParseException {
        JComponent editor = getEditor();
        if (editor instanceof DefaultEditor) {
            ((DefaultEditor) editor).commitEdit();
        }
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /** Pasa el aviso del modelo a los oyentes del control. */
    private static class ModelListener implements ChangeListener, java.io.Serializable {

        private final JSpinner control;

        ModelListener(JSpinner control) {
            this.control = control;
        }

        public void stateChanged(ChangeEvent e) {
            control.fireStateChanged();
        }
    }

    /** Ver {@link JSpinner#DISABLED_ACTION}. */
    private static class DisabledAction implements Action {

        public Object getValue(String key) {
            return null;
        }

        public void putValue(String key, Object value) {
        }

        public void setEnabled(boolean b) {
        }

        public boolean isEnabled() {
            return false;
        }

        public void addPropertyChangeListener(PropertyChangeListener l) {
        }

        public void removePropertyChangeListener(PropertyChangeListener l) {
        }

        public void actionPerformed(java.awt.event.ActionEvent ae) {
        }
    }

    /**
     * El editor de base: un campo de texto que muestra el valor.
     *
     * <p>Es tambien su propio acomodador -- implementa {@link LayoutManager} -- porque lo unico que
     * tiene que hacer es darle al campo todo el espacio menos los margenes. Un acomodador de los de
     * verdad seria mas codigo del que ahorra.
     *
     * <p>De base el campo no es editable: el editor generico no sabe interpretar lo que se escriba.
     * Los tres editores que siguen lo prenden, cada uno con su formateador.
     */
    public static class DefaultEditor extends JPanel
            implements ChangeListener, PropertyChangeListener, LayoutManager {

        /** Con el campo ya conectado a ese control. */
        public DefaultEditor(JSpinner spinner) {
            super(null);
            JFormattedTextField ftf = new JFormattedTextField();
            ftf.setName("Spinner.formattedTextField");
            ftf.setValue(spinner.getValue());
            ftf.addPropertyChangeListener(this);
            ftf.setEditable(false);
            ftf.setInheritsPopupMenu(true);
            String toolTipText = spinner.getToolTipText();
            if (toolTipText != null) {
                ftf.setToolTipText(toolTipText);
            }
            add(ftf);
            setLayout(this);
            spinner.addChangeListener(this);
            // Ver DISABLED_ACTION: las flechas del control tienen que ganarle a las del campo.
            ActionMap ftfMap = ftf.getActionMap();
            if (ftfMap != null) {
                ftfMap.put("increment", DISABLED_ACTION);
                ftfMap.put("decrement", DISABLED_ACTION);
            }
        }

        /** Lo desconecta del control; ver {@link JSpinner#setEditor}. */
        public void dismiss(JSpinner spinner) {
            spinner.removeChangeListener(this);
        }

        /** El control que lo contiene, buscando hacia arriba, o nulo si no esta puesto. */
        public JSpinner getSpinner() {
            for (Component c = this; c != null; c = c.getParent()) {
                if (c instanceof JSpinner) {
                    return (JSpinner) c;
                }
            }
            return null;
        }

        public JFormattedTextField getTextField() {
            return (JFormattedTextField) getComponent(0);
        }

        /** El modelo cambio: se lo pasa al campo. */
        public void stateChanged(ChangeEvent e) {
            JSpinner spinner = (JSpinner) (e.getSource());
            getTextField().setValue(spinner.getValue());
        }

        /**
         * El campo cambio: se lo pasa al modelo.
         *
         * <p>Si el modelo lo rechaza, el campo vuelve al valor de antes. Ver la nota de
         * {@link JSpinner}.
         */
        public void propertyChange(PropertyChangeEvent e) {
            JSpinner spinner = getSpinner();
            if (spinner == null) {
                // No esta puesto en ningun control: no hay a quien avisarle.
                return;
            }
            Object source = e.getSource();
            String name = e.getPropertyName();
            if ((source instanceof JFormattedTextField) && "value".equals(name)) {
                Object lastValue = spinner.getValue();
                try {
                    spinner.setValue(getTextField().getValue());
                } catch (IllegalArgumentException iae) {
                    try {
                        ((JFormattedTextField) source).setValue(lastValue);
                    } catch (IllegalArgumentException iae2) {
                        // Ni el valor viejo sirve: no queda nada por hacer y los dos quedan
                        // desacoplados, que es lo que hace el JDK.
                    }
                }
            }
        }

        public void addLayoutComponent(String name, Component child) {
        }

        public void removeLayoutComponent(Component child) {
        }

        /** Lo que ocupan los margenes. */
        private Dimension insetSize(Container parent) {
            Insets insets = parent.getInsets();
            int w = insets.left + insets.right;
            int h = insets.top + insets.bottom;
            return new Dimension(w, h);
        }

        public Dimension preferredLayoutSize(Container parent) {
            Dimension preferredSize = insetSize(parent);
            if (parent.getComponentCount() > 0) {
                Dimension childSize = getComponent(0).getPreferredSize();
                preferredSize.width += childSize.width;
                preferredSize.height += childSize.height;
            }
            return preferredSize;
        }

        public Dimension minimumLayoutSize(Container parent) {
            Dimension minimumSize = insetSize(parent);
            if (parent.getComponentCount() > 0) {
                Dimension childSize = getComponent(0).getMinimumSize();
                minimumSize.width += childSize.width;
                minimumSize.height += childSize.height;
            }
            return minimumSize;
        }

        /** Le da al campo todo menos los margenes. */
        public void layoutContainer(Container parent) {
            if (parent.getComponentCount() > 0) {
                Insets insets = parent.getInsets();
                int w = parent.getWidth() - (insets.left + insets.right);
                int h = parent.getHeight() - (insets.top + insets.bottom);
                getComponent(0).setBounds(insets.left, insets.top, w, h);
            }
        }

        /**
         * Confirma lo escrito.
         *
         * @throws ParseException si no se puede interpretar.
         */
        public void commitEdit() throws ParseException {
            getTextField().commitEdit();
        }

        /** La linea de base es la del campo, corrida por el margen de arriba. */
        public int getBaseline(int width, int height) {
            super.getBaseline(width, height);
            Insets insets = getInsets();
            width = width - insets.left - insets.right;
            height = height - insets.top - insets.bottom;
            int baseline = getComponent(0).getBaseline(width, height);
            if (baseline >= 0) {
                return baseline + insets.top;
            }
            return -1;
        }

        public Component.BaselineResizeBehavior getBaselineResizeBehavior() {
            return getComponent(0).getBaselineResizeBehavior();
        }
    }

    /**
     * El editor para un {@link SpinnerNumberModel}.
     *
     * <p>El formateador conoce los limites del modelo, asi que escribir algo fuera de rango se
     * marca como invalido antes de llegar al modelo.
     */
    public static class NumberEditor extends DefaultEditor {

        /** Con el formato de numeros del idioma del control. */
        public NumberEditor(JSpinner spinner) {
            this(spinner, patronPorOmision(spinner.getLocale()));
        }

        /**
         * Con ese patron de {@link DecimalFormat}.
         *
         * @throws IllegalArgumentException si el modelo no es un {@link SpinnerNumberModel}.
         */
        public NumberEditor(JSpinner spinner, String decimalFormatPattern) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerNumberModel)) {
                throw new IllegalArgumentException("model not a SpinnerNumberModel");
            }
            DecimalFormat format = new DecimalFormat(decimalFormatPattern);
            SpinnerNumberModel model = (SpinnerNumberModel) spinner.getModel();
            NumberFormatter formatter = new FormateadorDeNumero(model, format);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(formatter);
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(factory);
            ftf.setHorizontalAlignment(JTextField.RIGHT);
            // El ancho sale del limite mas largo: es lo unico que se sabe de antemano sobre
            // cuanto va a ocupar el numero.
            try {
                String maxString = formatter.valueToString(model.getMinimum());
                String minString = formatter.valueToString(model.getMaximum());
                ftf.setColumns(Math.max(maxString.length(), minString.length()));
            } catch (ParseException e) {
                // Sin limites no hay de donde sacar el ancho; se deja el que traiga el campo.
            }
        }

        /** El patron de numeros de ese idioma. */
        private static String patronPorOmision(Locale locale) {
            NumberFormat nf = (locale == null) ? NumberFormat.getInstance()
                    : NumberFormat.getInstance(locale);
            if (nf instanceof DecimalFormat) {
                return ((DecimalFormat) nf).toPattern();
            }
            return "#,##0.###";
        }

        public DecimalFormat getFormat() {
            NumberFormatter f = (NumberFormatter) getTextField().getFormatter();
            return (DecimalFormat) f.getFormat();
        }

        public SpinnerNumberModel getModel() {
            return (SpinnerNumberModel) (getSpinner().getModel());
        }

        public void setComponentOrientation(ComponentOrientation o) {
            super.setComponentOrientation(o);
        }
    }

    /** Ata el formateador a los limites del modelo; ver {@link NumberEditor}. */
    private static class FormateadorDeNumero extends NumberFormatter {

        private final SpinnerNumberModel model;

        FormateadorDeNumero(SpinnerNumberModel model, Format format) {
            super((NumberFormat) format);
            this.model = model;
            setValueClass(model.getValue().getClass());
        }

        public void setMinimum(Comparable<?> min) {
            model.setMinimum(min);
        }

        public Comparable<?> getMinimum() {
            return model.getMinimum();
        }

        public void setMaximum(Comparable<?> max) {
            model.setMaximum(max);
        }

        public Comparable<?> getMaximum() {
            return model.getMaximum();
        }
    }

    /**
     * El editor para un {@link SpinnerListModel}.
     *
     * <p>No hay nada que formatear: los elementos van y vienen por su {@code toString}.
     */
    public static class ListEditor extends DefaultEditor {

        /**
         * @throws IllegalArgumentException si el modelo no es un {@link SpinnerListModel}.
         */
        public ListEditor(JSpinner spinner) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerListModel)) {
                throw new IllegalArgumentException("model not a SpinnerListModel");
            }
            getTextField().setEditable(true);
            getTextField().setFormatterFactory(
                    new DefaultFormatterFactory(new FormateadorDeLista()));
        }

        public SpinnerListModel getModel() {
            return (SpinnerListModel) (getSpinner().getModel());
        }
    }

    /** El texto es el valor y el valor es el texto; ver {@link ListEditor}. */
    private static class FormateadorDeLista extends JFormattedTextField.AbstractFormatter {

        public String valueToString(Object value) throws ParseException {
            return (value == null) ? "" : value.toString();
        }

        public Object stringToValue(String string) throws ParseException {
            return string;
        }
    }

    /**
     * El editor para un {@link SpinnerDateModel}.
     *
     * <p>Como en {@link NumberEditor}, el formateador conoce los limites del modelo.
     */
    public static class DateEditor extends DefaultEditor {

        /** Con el formato de fecha y hora del idioma del control. */
        public DateEditor(JSpinner spinner) {
            this(spinner, patronPorOmision(spinner.getLocale()));
        }

        /**
         * Con ese patron de {@link SimpleDateFormat}.
         *
         * @throws IllegalArgumentException si el modelo no es un {@link SpinnerDateModel}.
         */
        public DateEditor(JSpinner spinner, String dateFormatPattern) {
            super(spinner);
            if (!(spinner.getModel() instanceof SpinnerDateModel)) {
                throw new IllegalArgumentException("model not a SpinnerDateModel");
            }
            Locale loc = spinner.getLocale();
            SimpleDateFormat format = (loc == null) ? new SimpleDateFormat(dateFormatPattern)
                    : new SimpleDateFormat(dateFormatPattern, loc);
            SpinnerDateModel model = (SpinnerDateModel) spinner.getModel();
            DateFormatter formatter = new FormateadorDeFecha(model, format);
            DefaultFormatterFactory factory = new DefaultFormatterFactory(formatter);
            JFormattedTextField ftf = getTextField();
            ftf.setEditable(true);
            ftf.setFormatterFactory(factory);
        }

        /** El patron de fecha y hora de ese idioma. */
        private static String patronPorOmision(Locale locale) {
            DateFormat df = DateFormat.getDateTimeInstance();
            if (df instanceof SimpleDateFormat) {
                return ((SimpleDateFormat) df).toPattern();
            }
            return "d/MM/yy H:mm:ss";
        }

        public SimpleDateFormat getFormat() {
            DateFormatter f = (DateFormatter) getTextField().getFormatter();
            return (SimpleDateFormat) f.getFormat();
        }

        public SpinnerDateModel getModel() {
            return (SpinnerDateModel) (getSpinner().getModel());
        }
    }

    /** Ata el formateador a los limites del modelo; ver {@link DateEditor}. */
    private static class FormateadorDeFecha extends DateFormatter {

        private final SpinnerDateModel model;

        FormateadorDeFecha(SpinnerDateModel model, DateFormat format) {
            super(format);
            this.model = model;
        }

        public void setMinimum(Comparable<?> min) {
            model.setStart(comoFecha(min));
        }

        public Comparable<?> getMinimum() {
            return model.getStart();
        }

        public void setMaximum(Comparable<?> max) {
            model.setEnd(comoFecha(max));
        }

        public Comparable<?> getMaximum() {
            return model.getEnd();
        }

        /** El descarte de generico, en un solo lugar. */
        @SuppressWarnings("unchecked")
        private static Comparable<java.util.Date> comoFecha(Comparable<?> c) {
            return (Comparable<java.util.Date>) c;
        }
    }
}
