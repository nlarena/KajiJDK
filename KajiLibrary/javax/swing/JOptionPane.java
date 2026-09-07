package javax.swing;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.OptionPaneUI;

/**
 * El panel de los dialogos estandar: avisos, preguntas y pedidos de un dato.
 *
 * <h2>El panel y el dialogo son cosas distintas</h2>
 *
 * <p>{@code JOptionPane} es un {@link JComponent}: el mensaje, el icono y los botones. Los metodos
 * {@code showXxxDialog} son un atajo que arma el panel, lo mete en un {@link JDialog}, lo muestra y
 * devuelve la respuesta. Se puede saltear el atajo y poner el panel donde uno quiera, que es para
 * lo que existe {@link #createDialog}.
 *
 * <h2>Los cuatro ejes</h2>
 *
 * <p>El <em>mensaje</em> es lo que se dice. El <em>tipo de mensaje</em>
 * ({@link #ERROR_MESSAGE} y compania) elige el icono. El <em>tipo de opciones</em>
 * ({@link #YES_NO_OPTION} y compania) elige los botones. Las <em>opciones</em> los reemplazan por
 * unos propios. Son independientes: se puede tener un icono de error con botones de si y no.
 *
 * <h2>Lo que devuelven los atajos</h2>
 *
 * <p>Con botones estandar, la constante del boton apretado. Con opciones propias, el <em>indice</em>
 * en el arreglo. Y si el usuario cerro la ventana sin apretar nada, {@link #CLOSED_OPTION}, que vale
 * lo mismo que {@link #PLAIN_MESSAGE} y que {@link #DEFAULT_OPTION} pero no significa lo mismo:
 * confundirlos es el error clasico con esta clase. Siempre hay que comparar contra la constante que
 * corresponde al tipo de opciones que se pidio.
 *
 * <h2>Aca no bloquea nada</h2>
 *
 * <p>En el JDK real, {@code showConfirmDialog} no vuelve hasta que el usuario contesta, porque el
 * dialogo modal apila un bucle de eventos. Esta biblioteca no reparte eventos de ventana: como en
 * {@link java.awt.Dialog}, {@code setVisible} vuelve enseguida. Los atajos por lo tanto arman todo,
 * lo muestran y devuelven {@link #CLOSED_OPTION} (o nulo, los de entrada de texto), que es lo que
 * corresponde a un dialogo que se cerro sin que se eligiera nada.
 *
 * <p>Se dice aca y no se disimula: un metodo que devolviera {@link #YES_OPTION} inventado seria
 * mucho peor que uno que dice la verdad.
 */
public class JOptionPane extends JComponent implements Accessible {

    private static final String uiClassID = "OptionPaneUI";

    /**
     * El valor que tiene el panel antes de que el usuario elija.
     *
     * <p>Hace falta un centinela propio porque nulo es una respuesta legitima, y porque el panel
     * tiene que distinguir "todavia no contesto" de "contesto nulo".
     */
    public static final Object UNINITIALIZED_VALUE = "uninitializedValue";

    /** Un solo boton, el que proponga el aspecto. */
    public static final int DEFAULT_OPTION = -1;

    /** Si y No. */
    public static final int YES_NO_OPTION = 0;

    /** Si, No y Cancelar. */
    public static final int YES_NO_CANCEL_OPTION = 1;

    /** Aceptar y Cancelar. */
    public static final int OK_CANCEL_OPTION = 2;

    /** Apreto Si. */
    public static final int YES_OPTION = 0;

    /** Apreto No. */
    public static final int NO_OPTION = 1;

    /** Apreto Cancelar. */
    public static final int CANCEL_OPTION = 2;

    /** Apreto Aceptar. */
    public static final int OK_OPTION = 0;

    /** Cerro la ventana sin elegir; ver la nota de la clase. */
    public static final int CLOSED_OPTION = -1;

    /** Icono de error. */
    public static final int ERROR_MESSAGE = 0;

    /** Icono de informacion. */
    public static final int INFORMATION_MESSAGE = 1;

    /** Icono de advertencia. */
    public static final int WARNING_MESSAGE = 2;

    /** Icono de pregunta. */
    public static final int QUESTION_MESSAGE = 3;

    /** Sin icono. */
    public static final int PLAIN_MESSAGE = -1;

    public static final String ICON_PROPERTY = "icon";
    public static final String MESSAGE_PROPERTY = "message";
    public static final String VALUE_PROPERTY = "value";
    public static final String OPTIONS_PROPERTY = "options";
    public static final String INITIAL_VALUE_PROPERTY = "initialValue";
    public static final String MESSAGE_TYPE_PROPERTY = "messageType";
    public static final String OPTION_TYPE_PROPERTY = "optionType";
    public static final String SELECTION_VALUES_PROPERTY = "selectionValues";
    public static final String INITIAL_SELECTION_VALUE_PROPERTY = "initialSelectionValue";
    public static final String INPUT_VALUE_PROPERTY = "inputValue";
    public static final String WANTS_INPUT_PROPERTY = "wantsInput";

    /** El icono; si es nulo lo elige el aspecto segun el tipo de mensaje. */
    protected transient Icon icon;

    /** El mensaje. */
    protected transient Object message;

    /** Los botones propios, o nulo para los estandar. */
    protected transient Object[] options;

    /** Cual empieza con el foco. */
    protected transient Object initialValue;

    /** Que icono va. */
    protected int messageType;

    /** Que botones van. */
    protected int optionType;

    /** Lo que el usuario eligio; ver {@link #UNINITIALIZED_VALUE}. */
    protected transient Object value;

    /** Las opciones de la lista, cuando se pide un dato de un conjunto. */
    protected transient Object[] selectionValues;

    /** Lo que el usuario escribio o eligio de la lista. */
    protected transient Object inputValue;

    /** Cual viene elegida de entrada en la lista. */
    protected transient Object initialSelectionValue;

    /** Si ademas del mensaje se pide un dato. */
    protected boolean wantsInput;

    private static Frame rootFrame = null;

    /** Un panel con un mensaje de prueba, que es lo que muestra el JDK. */
    public JOptionPane() {
        this("JOptionPane message");
    }

    /** Un panel con ese mensaje. */
    public JOptionPane(Object message) {
        this(message, PLAIN_MESSAGE);
    }

    /** Con ese mensaje y ese icono. */
    public JOptionPane(Object message, int messageType) {
        this(message, messageType, DEFAULT_OPTION);
    }

    /** Con ese mensaje, ese icono y esos botones. */
    public JOptionPane(Object message, int messageType, int optionType) {
        this(message, messageType, optionType, null);
    }

    /** Con un icono propio. */
    public JOptionPane(Object message, int messageType, int optionType, Icon icon) {
        this(message, messageType, optionType, icon, null);
    }

    /** Con botones propios. */
    public JOptionPane(Object message, int messageType, int optionType, Icon icon,
            Object[] options) {
        this(message, messageType, optionType, icon, options, null);
    }

    /**
     * El constructor completo.
     *
     * @throws RuntimeException si el tipo de mensaje o el de opciones no son validos.
     */
    public JOptionPane(Object message, int messageType, int optionType, Icon icon,
            Object[] options, Object initialValue) {
        this.message = message;
        this.options = options;
        this.initialValue = initialValue;
        this.icon = icon;
        setMessageType(messageType);
        setOptionType(optionType);
        value = UNINITIALIZED_VALUE;
        inputValue = UNINITIALIZED_VALUE;
        updateUI();
    }

    /**
     * Muestra un pedido de texto sobre la ventana de siempre.
     *
     * @return lo que escribio, o nulo; ver la nota de la clase.
     * @throws HeadlessException si no hay pantalla.
     */
    public static String showInputDialog(Object message) throws HeadlessException {
        return showInputDialog(null, message);
    }

    /** Con ese texto ya puesto. */
    public static String showInputDialog(Object message, Object initialSelectionValue) {
        return showInputDialog(null, message, initialSelectionValue);
    }

    /**
     * Sobre esa ventana.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public static String showInputDialog(Component parentComponent, Object message)
            throws HeadlessException {
        return showInputDialog(parentComponent, message, "Input", QUESTION_MESSAGE);
    }

    /** Sobre esa ventana y con ese texto ya puesto. */
    public static String showInputDialog(Component parentComponent, Object message,
            Object initialSelectionValue) {
        return (String) showInputDialog(parentComponent, message, "Input", QUESTION_MESSAGE, null,
                null, initialSelectionValue);
    }

    /**
     * Con titulo e icono propios.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public static String showInputDialog(Component parentComponent, Object message, String title,
            int messageType) throws HeadlessException {
        return (String) showInputDialog(parentComponent, message, title, messageType, null, null,
                null);
    }

    /**
     * El pedido completo: se puede elegir de una lista en vez de escribir.
     *
     * <p>Si {@code selectionValues} no es nulo, el aspecto pone una lista y no un campo de texto.
     *
     * @return lo elegido, o nulo; ver la nota de la clase.
     * @throws HeadlessException si no hay pantalla.
     */
    public static Object showInputDialog(Component parentComponent, Object message, String title,
            int messageType, Icon icon, Object[] selectionValues, Object initialSelectionValue)
            throws HeadlessException {
        JOptionPane pane = new JOptionPane(message, messageType, OK_CANCEL_OPTION, icon, null,
                null);
        pane.setWantsInput(true);
        pane.setSelectionValues(selectionValues);
        pane.setInitialSelectionValue(initialSelectionValue);
        JDialog dialog = pane.createDialog(parentComponent, title);
        pane.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();
        Object valor = pane.getInputValue();
        if (valor == UNINITIALIZED_VALUE) {
            return null;
        }
        return valor;
    }

    /**
     * Muestra un aviso.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public static void showMessageDialog(Component parentComponent, Object message)
            throws HeadlessException {
        showMessageDialog(parentComponent, message, "Message", INFORMATION_MESSAGE);
    }

    /**
     * Con titulo y tipo de icono.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public static void showMessageDialog(Component parentComponent, Object message, String title,
            int messageType) throws HeadlessException {
        showMessageDialog(parentComponent, message, title, messageType, null);
    }

    /**
     * Con un icono propio.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public static void showMessageDialog(Component parentComponent, Object message, String title,
            int messageType, Icon icon) throws HeadlessException {
        showOptionDialog(parentComponent, message, title, DEFAULT_OPTION, messageType, icon, null,
                null);
    }

    /**
     * Pregunta Si / No / Cancelar.
     *
     * @return la constante del boton, o {@link #CLOSED_OPTION}; ver la nota de la clase.
     * @throws HeadlessException si no hay pantalla.
     */
    public static int showConfirmDialog(Component parentComponent, Object message)
            throws HeadlessException {
        return showConfirmDialog(parentComponent, message, "Select an Option",
                YES_NO_CANCEL_OPTION);
    }

    /**
     * Con titulo y juego de botones.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title,
            int optionType) throws HeadlessException {
        return showConfirmDialog(parentComponent, message, title, optionType, QUESTION_MESSAGE);
    }

    /**
     * Con tipo de icono.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title,
            int optionType, int messageType) throws HeadlessException {
        return showConfirmDialog(parentComponent, message, title, optionType, messageType, null);
    }

    /**
     * Con un icono propio.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public static int showConfirmDialog(Component parentComponent, Object message, String title,
            int optionType, int messageType, Icon icon) throws HeadlessException {
        return showOptionDialog(parentComponent, message, title, optionType, messageType, icon,
                null, null);
    }

    /**
     * El dialogo completo, con botones propios.
     *
     * @return el indice en {@code options} del boton apretado, o su constante si
     *     {@code options} es nulo, o {@link #CLOSED_OPTION}.
     * @throws HeadlessException si no hay pantalla.
     */
    public static int showOptionDialog(Component parentComponent, Object message, String title,
            int optionType, int messageType, Icon icon, Object[] options, Object initialValue)
            throws HeadlessException {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon, options,
                initialValue);
        pane.setInitialValue(initialValue);
        JDialog dialog = pane.createDialog(parentComponent, title);
        pane.selectInitialValue();
        dialog.setVisible(true);
        dialog.dispose();
        return traducir(pane.getValue(), options);
    }

    /**
     * Convierte el valor que quedo en el panel en el entero que devuelven los atajos.
     *
     * <p>Con botones propios el resultado es el indice; con los estandar, el entero que el aspecto
     * puso como valor. Cualquier otra cosa -- incluido el centinela de "no contesto" -- es
     * {@link #CLOSED_OPTION}.
     */
    private static int traducir(Object elegido, Object[] options) {
        if (elegido == null || elegido == UNINITIALIZED_VALUE) {
            return CLOSED_OPTION;
        }
        if (options == null) {
            if (elegido instanceof Integer) {
                return ((Integer) elegido).intValue();
            }
            return CLOSED_OPTION;
        }
        for (int i = 0; i < options.length; i++) {
            if (options[i] != null && options[i].equals(elegido)) {
                return i;
            }
        }
        return CLOSED_OPTION;
    }

    /**
     * Arma el dialogo que contiene a este panel.
     *
     * <p>El dialogo se esconde solo cuando el panel cambia de valor: es lo que hace que apretar un
     * boton cierre la ventana sin que el aspecto tenga que conocerla.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public JDialog createDialog(Component parentComponent, String title) throws HeadlessException {
        Window duena = null;
        if (parentComponent != null) {
            duena = SwingUtilities.getWindowAncestor(parentComponent);
        }
        JDialog dialog;
        if (duena instanceof java.awt.Dialog) {
            dialog = new JDialog((java.awt.Dialog) duena, title, true);
        } else if (duena instanceof Frame) {
            dialog = new JDialog((Frame) duena, title, true);
        } else {
            dialog = new JDialog((Frame) null, title, true);
        }
        armarDialogo(dialog);
        return dialog;
    }

    /**
     * Arma el dialogo sobre la ventana de siempre.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public JDialog createDialog(String title) throws HeadlessException {
        JDialog dialog = new JDialog((Frame) null, title, true);
        armarDialogo(dialog);
        return dialog;
    }

    /** Le pone el panel adentro y lo conecta al valor; ver {@link #createDialog}. */
    private void armarDialogo(JDialog dialog) {
        Container contenido = dialog.getContentPane();
        contenido.setLayout(new BorderLayout());
        contenido.add(this, BorderLayout.CENTER);
        dialog.setResizable(false);
        setValue(UNINITIALIZED_VALUE);
        dialog.addPropertyChangeListener(new EscuchaValor(this, dialog));
        addPropertyChangeListener(new EscuchaValor(this, dialog));
        dialog.pack();
    }

    /**
     * Esconde el dialogo apenas el panel tiene un valor.
     *
     * <p>Es una clase con nombre y no una anonima porque el panel tambien la registra sobre si
     * mismo, y hace falta poder distinguir el origen del evento.
     */
    private static class EscuchaValor implements PropertyChangeListener {

        private final JOptionPane panel;
        private final JDialog dialogo;

        EscuchaValor(JOptionPane panel, JDialog dialogo) {
            this.panel = panel;
            this.dialogo = dialogo;
        }

        public void propertyChange(PropertyChangeEvent event) {
            if (event.getSource() != panel) {
                return;
            }
            if (!dialogo.isVisible()) {
                return;
            }
            String nombre = event.getPropertyName();
            boolean esValor = VALUE_PROPERTY.equals(nombre)
                    || INPUT_VALUE_PROPERTY.equals(nombre);
            if (esValor && event.getNewValue() != null
                    && event.getNewValue() != UNINITIALIZED_VALUE) {
                dialogo.setVisible(false);
            }
        }
    }

    /** Como {@link #showMessageDialog} pero con una ventana interna. */
    public static void showInternalMessageDialog(Component parentComponent, Object message) {
        showInternalMessageDialog(parentComponent, message, "Message", INFORMATION_MESSAGE);
    }

    /** Con titulo y tipo de icono. */
    public static void showInternalMessageDialog(Component parentComponent, Object message,
            String title, int messageType) {
        showInternalMessageDialog(parentComponent, message, title, messageType, null);
    }

    /** Con un icono propio. */
    public static void showInternalMessageDialog(Component parentComponent, Object message,
            String title, int messageType, Icon icon) {
        showInternalOptionDialog(parentComponent, message, title, DEFAULT_OPTION, messageType,
                icon, null, null);
    }

    /** Como {@link #showConfirmDialog} pero con una ventana interna. */
    public static int showInternalConfirmDialog(Component parentComponent, Object message) {
        return showInternalConfirmDialog(parentComponent, message, "Select an Option",
                YES_NO_CANCEL_OPTION);
    }

    /** Con titulo y juego de botones. */
    public static int showInternalConfirmDialog(Component parentComponent, Object message,
            String title, int optionType) {
        return showInternalConfirmDialog(parentComponent, message, title, optionType,
                QUESTION_MESSAGE);
    }

    /** Con tipo de icono. */
    public static int showInternalConfirmDialog(Component parentComponent, Object message,
            String title, int optionType, int messageType) {
        return showInternalConfirmDialog(parentComponent, message, title, optionType, messageType,
                null);
    }

    /** Con un icono propio. */
    public static int showInternalConfirmDialog(Component parentComponent, Object message,
            String title, int optionType, int messageType, Icon icon) {
        return showInternalOptionDialog(parentComponent, message, title, optionType, messageType,
                icon, null, null);
    }

    /**
     * El dialogo interno completo.
     *
     * @return el indice del boton apretado, o {@link #CLOSED_OPTION}; ver la nota de la clase.
     * @throws RuntimeException si el componente no esta en un escritorio ni tiene padre.
     */
    public static int showInternalOptionDialog(Component parentComponent, Object message,
            String title, int optionType, int messageType, Icon icon, Object[] options,
            Object initialValue) {
        JOptionPane pane = new JOptionPane(message, messageType, optionType, icon, options,
                initialValue);
        pane.setInitialValue(initialValue);
        JInternalFrame marco = pane.createInternalFrame(parentComponent, title);
        pane.selectInitialValue();
        marco.setVisible(true);
        return traducir(pane.getValue(), options);
    }

    /** Como {@link #showInputDialog} pero con una ventana interna. */
    public static String showInternalInputDialog(Component parentComponent, Object message) {
        return showInternalInputDialog(parentComponent, message, "Input", QUESTION_MESSAGE);
    }

    /** Con titulo y tipo de icono. */
    public static String showInternalInputDialog(Component parentComponent, Object message,
            String title, int messageType) {
        return (String) showInternalInputDialog(parentComponent, message, title, messageType, null,
                null, null);
    }

    /**
     * El pedido interno completo.
     *
     * @throws RuntimeException si el componente no esta en un escritorio ni tiene padre.
     */
    public static Object showInternalInputDialog(Component parentComponent, Object message,
            String title, int messageType, Icon icon, Object[] selectionValues,
            Object initialSelectionValue) {
        JOptionPane pane = new JOptionPane(message, messageType, OK_CANCEL_OPTION, icon, null,
                null);
        pane.setWantsInput(true);
        pane.setSelectionValues(selectionValues);
        pane.setInitialSelectionValue(initialSelectionValue);
        JInternalFrame marco = pane.createInternalFrame(parentComponent, title);
        pane.selectInitialValue();
        marco.setVisible(true);
        Object valor = pane.getInputValue();
        if (valor == UNINITIALIZED_VALUE) {
            return null;
        }
        return valor;
    }

    /**
     * Arma la ventana interna que contiene a este panel.
     *
     * <p>Se pone en la capa modal del escritorio para que quede arriba de las demas ventanas, que
     * es lo mas parecido a la modalidad que hay adentro de un escritorio.
     *
     * @throws RuntimeException si el componente no esta en un escritorio ni tiene padre.
     */
    public JInternalFrame createInternalFrame(Component parentComponent, String title) {
        Container padre = getDesktopPaneForComponent(parentComponent);
        if (padre == null) {
            if (parentComponent == null) {
                throw new RuntimeException(
                        "JOptionPane: parentComponent does not have a valid parent");
            }
            padre = parentComponent.getParent();
            if (padre == null) {
                throw new RuntimeException(
                        "JOptionPane: parentComponent does not have a valid parent");
            }
        }
        // Un dialogo se cierra, no se agranda ni se achica ni se redimensiona.
        JInternalFrame marco = new JInternalFrame(title, false, true, false, false);
        marco.putClientProperty("JInternalFrame.frameType", "optionDialog");
        marco.putClientProperty("JInternalFrame.messageType", Integer.valueOf(getMessageType()));
        marco.getContentPane().add(this, BorderLayout.CENTER);
        if (padre instanceof JDesktopPane) {
            padre.add(marco, JLayeredPane.MODAL_LAYER);
        } else {
            padre.add(marco, BorderLayout.CENTER);
        }
        Dimension medida = marco.getPreferredSize();
        marco.setBounds(0, 0, medida.width, medida.height);
        padre.validate();
        try {
            marco.setSelected(true);
        } catch (PropertyVetoException e) {
            // Que no se pueda activar no impide mostrarla.
        }
        return marco;
    }

    /**
     * La ventana del sistema que contiene a ese componente.
     *
     * <p>Si no hay ninguna, la ventana de siempre: un dialogo tiene que colgar de algo.
     *
     * @throws HeadlessException si no hay pantalla y hace falta la ventana de siempre.
     */
    public static Frame getFrameForComponent(Component parentComponent) throws HeadlessException {
        if (parentComponent == null) {
            return getRootFrame();
        }
        if (parentComponent instanceof Frame) {
            return (Frame) parentComponent;
        }
        return getFrameForComponent(parentComponent.getParent());
    }

    /** El escritorio que contiene a ese componente, o nulo. */
    public static JDesktopPane getDesktopPaneForComponent(Component parentComponent) {
        if (parentComponent == null) {
            return null;
        }
        if (parentComponent instanceof JDesktopPane) {
            return (JDesktopPane) parentComponent;
        }
        return getDesktopPaneForComponent(parentComponent.getParent());
    }

    /** Fija la ventana de la que cuelgan los dialogos sin padre. */
    public static void setRootFrame(Frame newRootFrame) {
        rootFrame = newRootFrame;
    }

    /**
     * La ventana de la que cuelgan los dialogos sin padre.
     *
     * @throws HeadlessException si no hay pantalla.
     */
    public static Frame getRootFrame() throws HeadlessException {
        if (rootFrame == null) {
            rootFrame = new Frame();
        }
        return rootFrame;
    }

    public void setUI(OptionPaneUI ui) {
        super.setUI(ui);
    }

    public OptionPaneUI getUI() {
        return (OptionPaneUI) ui;
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    /**
     * El mensaje.
     *
     * <p>No tiene por que ser texto: un {@link Component} se muestra tal cual, y un arreglo se
     * apila renglon por renglon. Es lo que permite meter un campo de contrasena adentro de un
     * aviso sin escribir un dialogo entero.
     */
    public void setMessage(Object newMessage) {
        Object oldMessage = message;
        message = newMessage;
        firePropertyChange(MESSAGE_PROPERTY, oldMessage, message);
    }

    public Object getMessage() {
        return message;
    }

    /** El icono; nulo deja que lo elija el aspecto segun el tipo de mensaje. */
    public void setIcon(Icon newIcon) {
        Object oldIcon = icon;
        icon = newIcon;
        firePropertyChange(ICON_PROPERTY, oldIcon, icon);
    }

    public Icon getIcon() {
        return icon;
    }

    /**
     * Lo que el usuario eligio.
     *
     * <p>Ponerlo es lo que cierra el dialogo: quien arma el dialogo escucha esta propiedad.
     */
    public void setValue(Object newValue) {
        Object oldValue = value;
        value = newValue;
        firePropertyChange(VALUE_PROPERTY, oldValue, value);
    }

    /** Lo elegido, o {@link #UNINITIALIZED_VALUE} si todavia no contesto. */
    public Object getValue() {
        return value;
    }

    /** Los botones propios; nulo deja los estandar. */
    public void setOptions(Object[] newOptions) {
        Object[] oldOptions = options;
        options = newOptions;
        firePropertyChange(OPTIONS_PROPERTY, oldOptions, options);
    }

    /** Una copia de los botones propios, o nulo. */
    public Object[] getOptions() {
        if (options != null) {
            int optionCount = options.length;
            Object[] retOptions = new Object[optionCount];
            System.arraycopy(options, 0, retOptions, 0, optionCount);
            return retOptions;
        }
        return options;
    }

    /** Cual boton empieza con el foco. */
    public void setInitialValue(Object newInitialValue) {
        Object oldIV = initialValue;
        initialValue = newInitialValue;
        firePropertyChange(INITIAL_VALUE_PROPERTY, oldIV, initialValue);
    }

    public Object getInitialValue() {
        return initialValue;
    }

    /**
     * Que icono va.
     *
     * @throws RuntimeException si no es uno de los cinco tipos.
     */
    public void setMessageType(int newType) {
        if (newType != ERROR_MESSAGE && newType != INFORMATION_MESSAGE
                && newType != WARNING_MESSAGE && newType != QUESTION_MESSAGE
                && newType != PLAIN_MESSAGE) {
            throw new RuntimeException("JOptionPane: type must be one of JOptionPane.ERROR_MESSAGE,"
                    + " JOptionPane.INFORMATION_MESSAGE, JOptionPane.WARNING_MESSAGE,"
                    + " JOptionPane.QUESTION_MESSAGE or JOptionPane.PLAIN_MESSAGE");
        }
        int oldType = messageType;
        messageType = newType;
        firePropertyChange(MESSAGE_TYPE_PROPERTY, oldType, messageType);
    }

    public int getMessageType() {
        return messageType;
    }

    /**
     * Que botones van.
     *
     * @throws RuntimeException si no es uno de los cuatro juegos.
     */
    public void setOptionType(int newType) {
        if (newType != DEFAULT_OPTION && newType != YES_NO_OPTION
                && newType != YES_NO_CANCEL_OPTION && newType != OK_CANCEL_OPTION) {
            throw new RuntimeException("JOptionPane: option type must be one of"
                    + " JOptionPane.DEFAULT_OPTION, JOptionPane.YES_NO_OPTION,"
                    + " JOptionPane.YES_NO_CANCEL_OPTION or JOptionPane.OK_CANCEL_OPTION");
        }
        int oldType = optionType;
        optionType = newType;
        firePropertyChange(OPTION_TYPE_PROPERTY, oldType, optionType);
    }

    public int getOptionType() {
        return optionType;
    }

    /**
     * Las opciones de la lista.
     *
     * <p>Ponerlas prende {@link #setWantsInput}: una lista de donde elegir no tiene sentido si no
     * se esta pidiendo un dato.
     */
    public void setSelectionValues(Object[] newValues) {
        Object[] oldValues = selectionValues;
        selectionValues = newValues;
        firePropertyChange(SELECTION_VALUES_PROPERTY, oldValues, newValues);
        if (selectionValues != null) {
            setWantsInput(true);
        }
    }

    public Object[] getSelectionValues() {
        return selectionValues;
    }

    public void setInitialSelectionValue(Object newValue) {
        Object oldValue = initialSelectionValue;
        initialSelectionValue = newValue;
        firePropertyChange(INITIAL_SELECTION_VALUE_PROPERTY, oldValue, newValue);
    }

    public Object getInitialSelectionValue() {
        return initialSelectionValue;
    }

    /** Lo que el usuario escribio o eligio; lo pone el aspecto. */
    public void setInputValue(Object newValue) {
        Object oldValue = inputValue;
        inputValue = newValue;
        firePropertyChange(INPUT_VALUE_PROPERTY, oldValue, newValue);
    }

    public Object getInputValue() {
        return inputValue;
    }

    /**
     * Cuantos caracteres entran en un renglon del mensaje.
     *
     * <p>Con {@link Integer#MAX_VALUE} no se corta nunca, que es lo que devuelve el aspecto de
     * base: cortar es una decision de presentacion y cada aspecto la toma por su cuenta.
     */
    public int getMaxCharactersPerLineCount() {
        return Integer.MAX_VALUE;
    }

    /** Si ademas del mensaje se pide un dato. */
    public void setWantsInput(boolean newValue) {
        boolean oldValue = wantsInput;
        wantsInput = newValue;
        firePropertyChange(WANTS_INPUT_PROPERTY, oldValue, newValue);
    }

    public boolean getWantsInput() {
        return wantsInput;
    }

    /** Le pide al aspecto que le de el foco al valor inicial. */
    public void selectInitialValue() {
        OptionPaneUI ui = getUI();
        if (ui != null) {
            ui.selectInitialValue(this);
        }
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }
}
