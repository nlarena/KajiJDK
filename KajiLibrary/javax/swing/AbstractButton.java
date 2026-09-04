package javax.swing;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.ItemSelectable;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Serializable;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.ButtonUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.UIResource;

/**
 * Lo que comparten todos los botones: modelo, texto, iconos por estado, alineaciones y accion.
 *
 * <h2>El boton es una vista de su modelo</h2>
 *
 * <p>El estado —armado, apretado, seleccionado, habilitado, con el cursor encima— no esta aca sino
 * en el {@link ButtonModel}. Esta clase escucha al modelo y se repinta, y reenvia sus eventos de
 * accion, de item y de cambio a los escuchas del boton, con el boton como origen. Por eso
 * {@link #setEnabled} escribe en los dos lados y {@link #isSelected} lee del modelo: el boton no
 * tiene copia propia.
 *
 * <h2>Iconos por estado</h2>
 *
 * <p>Siete ranuras: el icono, y los de apretado, seleccionado, rollover, rollover seleccionado,
 * deshabilitado y deshabilitado seleccionado. El aspecto elige cual pintar; una ranura vacia
 * vuelve al icono comun. El JDK fabrica el deshabilitado agrisando un {@code ImageIcon}; sin
 * {@code ImageIcon}, {@link #getDisabledIcon} devuelve lo que se puso y nada mas.
 *
 * <h2>Lo que puso el usuario y lo que puso el aspecto</h2>
 *
 * <p>Cuatro propiedades —borde pintado, rollover, separacion icono-texto, area rellena— las propone
 * el aspecto al instalarse y las puede fijar el usuario. Cada una recuerda quien la puso
 * ({@code *Set}), y {@link #setUIProperty} solo escribe las que el usuario no toco. El margen usa
 * {@link UIResource} para lo mismo.
 *
 * <p>No estan {@code getAccessibleContext} ni la clase {@code AccessibleAbstractButton}: no hay
 * tecnologia asistiva en esta VM. {@link #addImpl} no instala {@code OverlayLayout}, que no esta:
 * un hijo agregado a un boton queda con el layout del contenedor.
 */
public abstract class AbstractButton extends JComponent implements ItemSelectable, SwingConstants {

    public static final String MODEL_CHANGED_PROPERTY = "model";
    public static final String TEXT_CHANGED_PROPERTY = "text";
    public static final String MNEMONIC_CHANGED_PROPERTY = "mnemonic";
    public static final String MARGIN_CHANGED_PROPERTY = "margin";
    public static final String VERTICAL_ALIGNMENT_CHANGED_PROPERTY = "verticalAlignment";
    public static final String HORIZONTAL_ALIGNMENT_CHANGED_PROPERTY = "horizontalAlignment";
    public static final String VERTICAL_TEXT_POSITION_CHANGED_PROPERTY = "verticalTextPosition";
    public static final String HORIZONTAL_TEXT_POSITION_CHANGED_PROPERTY =
            "horizontalTextPosition";
    public static final String BORDER_PAINTED_CHANGED_PROPERTY = "borderPainted";
    public static final String FOCUS_PAINTED_CHANGED_PROPERTY = "focusPainted";
    public static final String ROLLOVER_ENABLED_CHANGED_PROPERTY = "rolloverEnabled";
    public static final String CONTENT_AREA_FILLED_CHANGED_PROPERTY = "contentAreaFilled";
    public static final String ICON_CHANGED_PROPERTY = "icon";
    public static final String PRESSED_ICON_CHANGED_PROPERTY = "pressedIcon";
    public static final String SELECTED_ICON_CHANGED_PROPERTY = "selectedIcon";
    public static final String ROLLOVER_ICON_CHANGED_PROPERTY = "rolloverIcon";
    public static final String ROLLOVER_SELECTED_ICON_CHANGED_PROPERTY = "rolloverSelectedIcon";
    public static final String DISABLED_ICON_CHANGED_PROPERTY = "disabledIcon";
    public static final String DISABLED_SELECTED_ICON_CHANGED_PROPERTY = "disabledSelectedIcon";

    /** El modelo; ver la nota de la clase. */
    protected ButtonModel model = null;

    private String text = "";
    private Insets margin = null;
    private Insets defaultMargin = null;

    private Icon defaultIcon = null;
    private Icon pressedIcon = null;
    private Icon disabledIcon = null;
    private Icon selectedIcon = null;
    private Icon disabledSelectedIcon = null;
    private Icon rolloverIcon = null;
    private Icon rolloverSelectedIcon = null;

    private boolean paintBorder = true;
    private boolean paintFocus = true;
    private boolean rolloverEnabled = false;
    private boolean contentAreaFilled = true;

    private int verticalAlignment = CENTER;
    private int horizontalAlignment = CENTER;
    private int verticalTextPosition = CENTER;
    private int horizontalTextPosition = TRAILING;
    private int iconTextGap = 4;

    private int mnemonic;
    private int mnemonicIndex = -1;

    private long multiClickThreshhold = 0;

    private boolean borderPaintedSet = false;
    private boolean rolloverEnabledSet = false;
    private boolean iconTextGapSet = false;
    private boolean contentAreaFilledSet = false;
    private boolean setLayout = false;

    private boolean hideActionText = false;

    private Action action;
    private PropertyChangeListener actionPropertyChangeListener;

    /** Si puede ser el boton por omision de un dialogo; lo usa {@code JButton}. */
    boolean defaultCapable = true;

    /** El escucha de cambios del modelo; lo crea {@link #createChangeListener}. */
    protected ChangeListener changeListener = null;

    /** El escucha de acciones del modelo; lo crea {@link #createActionListener}. */
    protected ActionListener actionListener = null;

    /** El escucha de items del modelo; lo crea {@link #createItemListener}. */
    protected ItemListener itemListener = null;

    /** El evento de cambio que se reenvia, creado una vez. */
    protected transient ChangeEvent changeEvent;

    private Manejador manejador;

    protected AbstractButton() {
    }

    // -- accion ----------------------------------------------------------------------------------

    /**
     * Si el texto de la accion se oculta: un boton de barra de herramientas muestra solo el icono.
     *
     * <p>Solo afecta al texto que viene de la {@link Action}; uno puesto con {@link #setText} no se
     * oculta.
     */
    public void setHideActionText(boolean hideActionText) {
        if (hideActionText != this.hideActionText) {
            this.hideActionText = hideActionText;
            if (getAction() != null) {
                textoDesdeAccion(getAction(), false);
            }
            firePropertyChange("hideActionText", !hideActionText, hideActionText);
        }
    }

    public boolean getHideActionText() {
        return hideActionText;
    }

    // -- texto y seleccion -----------------------------------------------------------------------

    public String getText() {
        return text;
    }

    /** Pone el texto y recalcula que caracter subraya el mnemonico. */
    public void setText(String text) {
        String viejo = this.text;
        this.text = text;
        firePropertyChange(TEXT_CHANGED_PROPERTY, viejo, text);
        actualizarIndiceDeMnemonico(text, getMnemonic());
        if (text == null || viejo == null || !text.equals(viejo)) {
            revalidate();
            repaint();
        }
    }

    public boolean isSelected() {
        return model.isSelected();
    }

    public void setSelected(boolean b) {
        model.setSelected(b);
    }

    /** Un click como el del mouse: arma, aprieta, y suelta, disparando la accion. */
    public void doClick() {
        doClick(68);
    }

    /**
     * Un click que se ve: el boton queda apretado {@code pressTime} milisegundos.
     *
     * <p>Es lo que hace el mnemonico: el usuario ve el boton hundirse, como si lo hubiera
     * apretado. La accion se dispara al soltar, como siempre.
     */
    public void doClick(int pressTime) {
        Dimension tamano = getSize();
        model.setArmed(true);
        model.setPressed(true);
        paintImmediately(0, 0, tamano.width, tamano.height);
        try {
            Thread.sleep(pressTime);
        } catch (InterruptedException ie) {
        }
        model.setPressed(false);
        model.setArmed(false);
    }

    // -- margen ----------------------------------------------------------------------------------

    /**
     * El margen entre el borde y el contenido.
     *
     * <p>{@code null} vuelve al margen del aspecto: el ultimo {@link UIResource} que se puso, que
     * esta clase recuerda para eso. Que el margen cuente en los insets depende del borde: el del
     * aspecto lleva un {@code MarginBorder} adentro; uno del usuario, no.
     */
    public void setMargin(Insets m) {
        if (m instanceof UIResource) {
            defaultMargin = m;
        } else if (margin instanceof UIResource) {
            defaultMargin = margin;
        }
        if (m == null) {
            m = defaultMargin;
        }
        Insets viejo = margin;
        margin = m;
        firePropertyChange(MARGIN_CHANGED_PROPERTY, viejo, m);
        if (viejo == null || !viejo.equals(m)) {
            revalidate();
            repaint();
        }
    }

    /** Una copia del margen, o {@code null}; la copia conserva si es del aspecto o del usuario. */
    public Insets getMargin() {
        if (margin == null) {
            return null;
        }
        return (Insets) margin.clone();
    }

    // -- iconos ----------------------------------------------------------------------------------

    public Icon getIcon() {
        return defaultIcon;
    }

    /**
     * Pone el icono; si cambia de tamano, el boton se reacomoda.
     *
     * <p>Un icono deshabilitado que puso el aspecto se descarta: era una version de este.
     */
    public void setIcon(Icon defaultIcon) {
        Icon viejo = this.defaultIcon;
        this.defaultIcon = defaultIcon;
        if (defaultIcon != viejo && (disabledIcon instanceof UIResource)) {
            disabledIcon = null;
        }
        firePropertyChange(ICON_CHANGED_PROPERTY, viejo, defaultIcon);
        if (defaultIcon != viejo) {
            if (defaultIcon == null || viejo == null
                    || defaultIcon.getIconWidth() != viejo.getIconWidth()
                    || defaultIcon.getIconHeight() != viejo.getIconHeight()) {
                revalidate();
            }
            repaint();
        }
    }

    public Icon getPressedIcon() {
        return pressedIcon;
    }

    public void setPressedIcon(Icon pressedIcon) {
        Icon viejo = this.pressedIcon;
        this.pressedIcon = pressedIcon;
        firePropertyChange(PRESSED_ICON_CHANGED_PROPERTY, viejo, pressedIcon);
        if (pressedIcon != viejo) {
            if (getModel().isPressed() && getModel().isArmed()) {
                repaint();
            }
        }
    }

    public Icon getSelectedIcon() {
        return selectedIcon;
    }

    public void setSelectedIcon(Icon selectedIcon) {
        Icon viejo = this.selectedIcon;
        this.selectedIcon = selectedIcon;
        if (selectedIcon != viejo && disabledSelectedIcon instanceof UIResource) {
            disabledSelectedIcon = null;
        }
        firePropertyChange(SELECTED_ICON_CHANGED_PROPERTY, viejo, selectedIcon);
        if (selectedIcon != viejo) {
            if (isSelected()) {
                repaint();
            }
        }
    }

    public Icon getRolloverIcon() {
        return rolloverIcon;
    }

    /** Poner un icono de rollover habilita el rollover: sin eso, nunca se veria. */
    public void setRolloverIcon(Icon rolloverIcon) {
        Icon viejo = this.rolloverIcon;
        this.rolloverIcon = rolloverIcon;
        firePropertyChange(ROLLOVER_ICON_CHANGED_PROPERTY, viejo, rolloverIcon);
        setRolloverEnabled(true);
        if (rolloverIcon != viejo) {
            repaint();
        }
    }

    public Icon getRolloverSelectedIcon() {
        return rolloverSelectedIcon;
    }

    public void setRolloverSelectedIcon(Icon rolloverSelectedIcon) {
        Icon viejo = this.rolloverSelectedIcon;
        this.rolloverSelectedIcon = rolloverSelectedIcon;
        firePropertyChange(ROLLOVER_SELECTED_ICON_CHANGED_PROPERTY, viejo, rolloverSelectedIcon);
        setRolloverEnabled(true);
        if (rolloverSelectedIcon != viejo) {
            if (isSelected()) {
                repaint();
            }
        }
    }

    /** El icono deshabilitado que se puso, o {@code null}; ver la nota de la clase. */
    public Icon getDisabledIcon() {
        return disabledIcon;
    }

    public void setDisabledIcon(Icon disabledIcon) {
        Icon viejo = this.disabledIcon;
        this.disabledIcon = disabledIcon;
        firePropertyChange(DISABLED_ICON_CHANGED_PROPERTY, viejo, disabledIcon);
        if (disabledIcon != viejo) {
            if (!isEnabled()) {
                repaint();
            }
        }
    }

    /** El icono deshabilitado y seleccionado que se puso, o {@code null}. */
    public Icon getDisabledSelectedIcon() {
        return disabledSelectedIcon;
    }

    public void setDisabledSelectedIcon(Icon disabledSelectedIcon) {
        Icon viejo = this.disabledSelectedIcon;
        this.disabledSelectedIcon = disabledSelectedIcon;
        firePropertyChange(DISABLED_SELECTED_ICON_CHANGED_PROPERTY, viejo, disabledSelectedIcon);
        if (disabledSelectedIcon != viejo) {
            if (disabledSelectedIcon == null || viejo == null
                    || disabledSelectedIcon.getIconWidth() != viejo.getIconWidth()
                    || disabledSelectedIcon.getIconHeight() != viejo.getIconHeight()) {
                revalidate();
            }
            if (!isEnabled() && isSelected()) {
                repaint();
            }
        }
    }

    // -- alineaciones ----------------------------------------------------------------------------

    public int getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(int alignment) {
        if (alignment == verticalAlignment) {
            return;
        }
        int viejo = verticalAlignment;
        verticalAlignment = checkVerticalKey(alignment, "verticalAlignment");
        firePropertyChange(VERTICAL_ALIGNMENT_CHANGED_PROPERTY, viejo, verticalAlignment);
        repaint();
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(int alignment) {
        if (alignment == horizontalAlignment) {
            return;
        }
        int viejo = horizontalAlignment;
        horizontalAlignment = checkHorizontalKey(alignment, "horizontalAlignment");
        firePropertyChange(HORIZONTAL_ALIGNMENT_CHANGED_PROPERTY, viejo, horizontalAlignment);
        repaint();
    }

    public int getVerticalTextPosition() {
        return verticalTextPosition;
    }

    public void setVerticalTextPosition(int textPosition) {
        if (textPosition == verticalTextPosition) {
            return;
        }
        int viejo = verticalTextPosition;
        verticalTextPosition = checkVerticalKey(textPosition, "verticalTextPosition");
        firePropertyChange(VERTICAL_TEXT_POSITION_CHANGED_PROPERTY, viejo, verticalTextPosition);
        revalidate();
        repaint();
    }

    public int getHorizontalTextPosition() {
        return horizontalTextPosition;
    }

    public void setHorizontalTextPosition(int textPosition) {
        if (textPosition == horizontalTextPosition) {
            return;
        }
        int viejo = horizontalTextPosition;
        horizontalTextPosition = checkHorizontalKey(textPosition, "horizontalTextPosition");
        firePropertyChange(HORIZONTAL_TEXT_POSITION_CHANGED_PROPERTY, viejo,
                horizontalTextPosition);
        revalidate();
        repaint();
    }

    public int getIconTextGap() {
        return iconTextGap;
    }

    public void setIconTextGap(int iconTextGap) {
        int viejo = this.iconTextGap;
        this.iconTextGap = iconTextGap;
        iconTextGapSet = true;
        firePropertyChange("iconTextGap", viejo, iconTextGap);
        if (iconTextGap != viejo) {
            revalidate();
            repaint();
        }
    }

    /** Valida una clave horizontal; {@code exception} es el nombre que va en el error. */
    protected int checkHorizontalKey(int key, String exception) {
        if (key == LEFT || key == CENTER || key == RIGHT || key == LEADING || key == TRAILING) {
            return key;
        }
        throw new IllegalArgumentException(exception);
    }

    /** Valida una clave vertical. */
    protected int checkVerticalKey(int key, String exception) {
        if (key == TOP || key == CENTER || key == BOTTOM) {
            return key;
        }
        throw new IllegalArgumentException(exception);
    }

    /** Se va de la jerarquia: si mostraba rollover, lo deja de mostrar. */
    public void removeNotify() {
        super.removeNotify();
        if (isRolloverEnabled()) {
            getModel().setRollover(false);
        }
    }

    // -- comando y accion ------------------------------------------------------------------------

    public void setActionCommand(String actionCommand) {
        getModel().setActionCommand(actionCommand);
    }

    /** El comando del modelo, o el texto si el modelo no tiene. */
    public String getActionCommand() {
        String ac = getModel().getActionCommand();
        if (ac == null) {
            ac = getText();
        }
        return ac;
    }

    /**
     * Ata el boton a una accion: toma de ella texto, icono, mnemonico, comando, ayuda y estado, la
     * dispara al apretar, y la sigue cuando cambia.
     *
     * <p>Atar otra suelta la anterior: deja de escucharla y de dispararla.
     */
    public void setAction(Action a) {
        Action viejo = getAction();
        if (action == null || !action.equals(a)) {
            action = a;
            if (viejo != null) {
                removeActionListener(viejo);
                viejo.removePropertyChangeListener(actionPropertyChangeListener);
                actionPropertyChangeListener = null;
            }
            configurePropertiesFromAction(action);
            if (action != null) {
                if (!esEscucha(ActionListener.class, action)) {
                    addActionListener(action);
                }
                actionPropertyChangeListener = createActionPropertyChangeListener(action);
                action.addPropertyChangeListener(actionPropertyChangeListener);
            }
            firePropertyChange("action", viejo, action);
        }
    }

    private boolean esEscucha(Class<?> clase, ActionListener a) {
        Object[] escuchas = listenerList.getListenerList();
        for (int i = escuchas.length - 2; i >= 0; i = i - 2) {
            if (escuchas[i] == clase && escuchas[i + 1] == a) {
                return true;
            }
        }
        return false;
    }

    public Action getAction() {
        return action;
    }

    /** Toma todo de la accion; {@code null} deja el boton como si nunca hubiera tenido una. */
    protected void configurePropertiesFromAction(Action a) {
        mnemonicoDesdeAccion(a);
        textoDesdeAccion(a, false);
        setToolTipText(a != null ? (String) a.getValue(Action.SHORT_DESCRIPTION) : null);
        setIconFromAction(a);
        comandoDesdeAccion(a);
        setEnabled(a != null ? a.isEnabled() : true);
        if (tieneClaveDeSeleccion(a) && shouldUpdateSelectedStateFromAction()) {
            seleccionDesdeAccion(a);
        }
        indiceDeMnemonicoDesdeAccion(a, false);
    }

    /**
     * Si el estado de seleccion sigue a la accion: no en un boton comun, si en uno con estado.
     * Lo redefine {@code JToggleButton}.
     */
    boolean shouldUpdateSelectedStateFromAction() {
        return false;
    }

    /**
     * Cambio una propiedad de la accion: se copia la que cambio.
     *
     * <p>Lo llama el escucha de {@link #createActionPropertyChangeListener}; redefinirlo es la
     * manera de seguir propiedades propias de una accion.
     */
    protected void actionPropertyChanged(Action action, String propertyName) {
        if (Action.NAME.equals(propertyName)) {
            textoDesdeAccion(action, true);
        } else if ("enabled".equals(propertyName)) {
            setEnabled(action != null ? action.isEnabled() : true);
        } else if (Action.SHORT_DESCRIPTION.equals(propertyName)) {
            setToolTipText(action != null ? (String) action.getValue(Action.SHORT_DESCRIPTION)
                    : null);
        } else if (Action.SMALL_ICON.equals(propertyName)) {
            smallIconChanged(action);
        } else if (Action.MNEMONIC_KEY.equals(propertyName)) {
            mnemonicoDesdeAccion(action);
        } else if (Action.ACTION_COMMAND_KEY.equals(propertyName)) {
            comandoDesdeAccion(action);
        } else if (Action.SELECTED_KEY.equals(propertyName)
                && tieneClaveDeSeleccion(action) && shouldUpdateSelectedStateFromAction()) {
            seleccionDesdeAccion(action);
        } else if (Action.DISPLAYED_MNEMONIC_INDEX_KEY.equals(propertyName)) {
            indiceDeMnemonicoDesdeAccion(action, true);
        } else if (Action.LARGE_ICON_KEY.equals(propertyName)) {
            largeIconChanged(action);
        }
    }

    private void textoDesdeAccion(Action a, boolean propertyChange) {
        boolean ocultar = getHideActionText();
        if (!propertyChange) {
            setText((a != null && !ocultar) ? (String) a.getValue(Action.NAME) : null);
        } else if (!ocultar) {
            setText((String) a.getValue(Action.NAME));
        }
    }

    /** El icono grande si esta, si no el chico, si no ninguno. */
    void setIconFromAction(Action a) {
        Icon icono = null;
        if (a != null) {
            icono = (Icon) a.getValue(Action.LARGE_ICON_KEY);
            if (icono == null) {
                icono = (Icon) a.getValue(Action.SMALL_ICON);
            }
        }
        setIcon(icono);
    }

    /** Cambio el icono chico: importa solo si no hay grande. */
    void smallIconChanged(Action a) {
        if (a.getValue(Action.LARGE_ICON_KEY) == null) {
            setIconFromAction(a);
        }
    }

    void largeIconChanged(Action a) {
        setIconFromAction(a);
    }

    private void comandoDesdeAccion(Action a) {
        setActionCommand(a != null ? (String) a.getValue(Action.ACTION_COMMAND_KEY) : null);
    }

    private void mnemonicoDesdeAccion(Action a) {
        Integer n = (a == null) ? null : (Integer) a.getValue(Action.MNEMONIC_KEY);
        setMnemonic(n == null ? '\0' : n.intValue());
    }

    private static boolean tieneClaveDeSeleccion(Action a) {
        return a != null && a.getValue(Action.SELECTED_KEY) != null;
    }

    private void seleccionDesdeAccion(Action a) {
        boolean seleccionado = false;
        if (a != null) {
            seleccionado = Boolean.TRUE.equals(a.getValue(Action.SELECTED_KEY));
        }
        if (seleccionado != isSelected()) {
            setSelected(seleccionado);
        }
    }

    private void indiceDeMnemonicoDesdeAccion(Action a, boolean fromPropertyChange) {
        Integer valor = (a == null) ? null : (Integer) a.getValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY);
        if (fromPropertyChange || valor != null) {
            int indice = (valor == null) ? -1 : valor.intValue();
            if (indice == -1) {
                actualizarIndiceDeMnemonico(getText(), getMnemonic());
            } else {
                try {
                    setDisplayedMnemonicIndex(indice);
                } catch (IllegalArgumentException iae) {
                }
            }
        }
    }

    /** El escucha que sigue a la accion; avisa a {@link #actionPropertyChanged}. */
    protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
        return new EscuchaDeAccion(this, a);
    }

    /** Sigue a la accion en nombre de un boton; nombrada y no anonima (#499). */
    private static class EscuchaDeAccion implements PropertyChangeListener {
        private final AbstractButton boton;
        private final Action accion;

        EscuchaDeAccion(AbstractButton boton, Action accion) {
            this.boton = boton;
            this.accion = accion;
        }

        public void propertyChange(PropertyChangeEvent e) {
            boton.actionPropertyChanged(accion, e.getPropertyName());
        }
    }

    // -- borde, foco, relleno, rollover ----------------------------------------------------------

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        boolean viejo = paintBorder;
        paintBorder = b;
        borderPaintedSet = true;
        firePropertyChange(BORDER_PAINTED_CHANGED_PROPERTY, viejo, paintBorder);
        if (b != viejo) {
            revalidate();
            repaint();
        }
    }

    /** Pinta el borde solo si {@link #isBorderPainted}; los insets cuentan igual. */
    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    public boolean isFocusPainted() {
        return paintFocus;
    }

    public void setFocusPainted(boolean b) {
        boolean viejo = paintFocus;
        paintFocus = b;
        firePropertyChange(FOCUS_PAINTED_CHANGED_PROPERTY, viejo, paintFocus);
        if (b != viejo && isFocusOwner()) {
            revalidate();
            repaint();
        }
    }

    public boolean isContentAreaFilled() {
        return contentAreaFilled;
    }

    /** Si el aspecto rellena el fondo; apagarlo es como se hace un boton transparente. */
    public void setContentAreaFilled(boolean b) {
        boolean viejo = contentAreaFilled;
        contentAreaFilled = b;
        contentAreaFilledSet = true;
        firePropertyChange(CONTENT_AREA_FILLED_CHANGED_PROPERTY, viejo, contentAreaFilled);
        if (b != viejo) {
            repaint();
        }
    }

    public boolean isRolloverEnabled() {
        return rolloverEnabled;
    }

    public void setRolloverEnabled(boolean b) {
        boolean viejo = rolloverEnabled;
        rolloverEnabled = b;
        rolloverEnabledSet = true;
        firePropertyChange(ROLLOVER_ENABLED_CHANGED_PROPERTY, viejo, rolloverEnabled);
        if (b != viejo) {
            repaint();
        }
    }

    // -- mnemonico -------------------------------------------------------------------------------

    public int getMnemonic() {
        return mnemonic;
    }

    /** El mnemonico como tecla virtual de {@code KeyEvent}; va al modelo y de ahi vuelve. */
    public void setMnemonic(int mnemonic) {
        model.setMnemonic(mnemonic);
        actualizarMnemonico();
    }

    /** El mnemonico como caracter; una minuscula se pasa a mayuscula, que es la tecla. */
    public void setMnemonic(char mnemonic) {
        int vk = (int) mnemonic;
        if (vk >= 'a' && vk <= 'z') {
            vk = vk - ('a' - 'A');
        }
        setMnemonic(vk);
    }

    /**
     * Que caracter del texto subrayar; {@code -1}, ninguno.
     *
     * <p>Por omision es la primera aparicion del mnemonico; ponerlo a mano sirve cuando la letra
     * aparece varias veces y la que se subraya debe ser otra.
     */
    public void setDisplayedMnemonicIndex(int index) throws IllegalArgumentException {
        int viejo = mnemonicIndex;
        if (index == -1) {
            mnemonicIndex = -1;
        } else {
            String t = getText();
            int largo = (t == null) ? 0 : t.length();
            if (index < -1 || index >= largo) {
                throw new IllegalArgumentException("index == " + index);
            }
        }
        mnemonicIndex = index;
        firePropertyChange("displayedMnemonicIndex", viejo, index);
        if (index != viejo) {
            revalidate();
            repaint();
        }
    }

    public int getDisplayedMnemonicIndex() {
        return mnemonicIndex;
    }

    private void actualizarIndiceDeMnemonico(String texto, int mnemonico) {
        setDisplayedMnemonicIndex(SwingUtilities.findDisplayedMnemonicIndex(texto, mnemonico));
    }

    /** El modelo cambio de mnemonico: el boton lo copia y avisa. */
    private void actualizarMnemonico() {
        int nuevo = model.getMnemonic();
        if (mnemonic != nuevo) {
            int viejo = mnemonic;
            mnemonic = nuevo;
            firePropertyChange(MNEMONIC_CHANGED_PROPERTY, viejo, mnemonic);
            actualizarIndiceDeMnemonico(getText(), mnemonic);
            revalidate();
            repaint();
        }
    }

    /**
     * Los milisegundos entre dos presiones para que la segunda cuente aparte; cero, todas
     * cuentan.
     */
    public void setMultiClickThreshhold(long threshhold) {
        if (threshhold < 0) {
            throw new IllegalArgumentException("threshhold must be >= 0");
        }
        this.multiClickThreshhold = threshhold;
    }

    public long getMultiClickThreshhold() {
        return multiClickThreshhold;
    }

    // -- modelo ----------------------------------------------------------------------------------

    public ButtonModel getModel() {
        return model;
    }

    /**
     * Cambia el modelo: deja de escuchar al viejo, escucha al nuevo y copia su estado.
     *
     * <p>Habilitado y mnemonico se copian del modelo al boton, porque el modelo es la verdad.
     */
    public void setModel(ButtonModel newModel) {
        ButtonModel viejo = getModel();
        if (viejo != null) {
            viejo.removeChangeListener(changeListener);
            viejo.removeActionListener(actionListener);
            viejo.removeItemListener(itemListener);
            changeListener = null;
            actionListener = null;
            itemListener = null;
        }
        model = newModel;
        if (newModel != null) {
            changeListener = createChangeListener();
            actionListener = createActionListener();
            itemListener = createItemListener();
            newModel.addChangeListener(changeListener);
            newModel.addActionListener(actionListener);
            newModel.addItemListener(itemListener);
            actualizarMnemonico();
            setEnabled(newModel.isEnabled());
        } else {
            mnemonic = '\0';
        }
        actualizarIndiceDeMnemonico(getText(), mnemonic);
        firePropertyChange(MODEL_CHANGED_PROPERTY, viejo, newModel);
        if (newModel != viejo) {
            revalidate();
            repaint();
        }
    }

    // -- aspecto ---------------------------------------------------------------------------------

    public ButtonUI getUI() {
        return (ButtonUI) ui;
    }

    public void setUI(ButtonUI ui) {
        super.setUI(ui);
    }

    /** Nada: cada boton concreto sabe que aspecto instalar. */
    public void updateUI() {
    }

    /** Ver la nota de la clase sobre {@code OverlayLayout}. */
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
    }

    public void setLayout(LayoutManager mgr) {
        setLayout = true;
        super.setLayout(mgr);
    }

    // -- escuchas del boton ----------------------------------------------------------------------

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /** Reenvia el cambio del modelo con el boton como origen. */
    protected void fireStateChanged() {
        Object[] escuchas = listenerList.getListenerList();
        for (int i = escuchas.length - 2; i >= 0; i = i - 2) {
            if (escuchas[i] == ChangeListener.class) {
                if (changeEvent == null) {
                    changeEvent = new ChangeEvent(this);
                }
                ((ChangeListener) escuchas[i + 1]).stateChanged(changeEvent);
            }
        }
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        if (l != null && getAction() == l) {
            setAction(null);
        } else {
            listenerList.remove(ActionListener.class, l);
        }
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    protected ChangeListener createChangeListener() {
        return manejador();
    }

    /**
     * Reenvia la accion del modelo con el boton como origen y el comando del boton.
     *
     * <p>El evento nuevo se arma una sola vez, y solo si hay quien lo escuche.
     */
    protected void fireActionPerformed(ActionEvent event) {
        Object[] escuchas = listenerList.getListenerList();
        ActionEvent e = null;
        for (int i = escuchas.length - 2; i >= 0; i = i - 2) {
            if (escuchas[i] == ActionListener.class) {
                if (e == null) {
                    String comando = event.getActionCommand();
                    if (comando == null) {
                        comando = getActionCommand();
                    }
                    e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, comando,
                            event.getWhen(), event.getModifiers());
                }
                ((ActionListener) escuchas[i + 1]).actionPerformed(e);
            }
        }
    }

    protected void fireItemStateChanged(ItemEvent event) {
        Object[] escuchas = listenerList.getListenerList();
        ItemEvent e = null;
        for (int i = escuchas.length - 2; i >= 0; i = i - 2) {
            if (escuchas[i] == ItemListener.class) {
                if (e == null) {
                    e = new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                            event.getStateChange());
                }
                ((ItemListener) escuchas[i + 1]).itemStateChanged(e);
            }
        }
    }

    protected ActionListener createActionListener() {
        return manejador();
    }

    protected ItemListener createItemListener() {
        return manejador();
    }

    private Manejador manejador() {
        if (manejador == null) {
            manejador = new Manejador(this);
        }
        return manejador;
    }

    /** Un cambio del modelo: el boton copia mnemonico y habilitado, y avisa. */
    void cambioDelModelo() {
        actualizarMnemonico();
        if (isEnabled() != model.isEnabled()) {
            setEnabled(model.isEnabled());
        }
        fireStateChanged();
        repaint();
    }

    /** Un cambio de item del modelo: se reenvia, y si la accion tiene estado, se le copia. */
    void itemDelModelo(ItemEvent event) {
        fireItemStateChanged(event);
        if (shouldUpdateSelectedStateFromAction()) {
            Action a = getAction();
            if (a != null && tieneClaveDeSeleccion(a)) {
                boolean seleccionado = isSelected();
                boolean enAccion = Boolean.TRUE.equals(a.getValue(Action.SELECTED_KEY));
                if (enAccion != seleccionado) {
                    a.putValue(Action.SELECTED_KEY, Boolean.valueOf(seleccionado));
                }
            }
        }
    }

    /** Los tres escuchas del modelo en un objeto; nombrado y no anonimo (#499). */
    private static class Manejador implements ActionListener, ChangeListener, ItemListener,
            Serializable {
        private final AbstractButton boton;

        Manejador(AbstractButton boton) {
            this.boton = boton;
        }

        public void stateChanged(ChangeEvent e) {
            boton.cambioDelModelo();
        }

        public void actionPerformed(ActionEvent e) {
            boton.fireActionPerformed(e);
        }

        public void itemStateChanged(ItemEvent e) {
            boton.itemDelModelo(e);
        }
    }

    /**
     * El escucha de cambios del modelo, como clase; el JDK la conserva por compatibilidad y
     * {@link #createChangeListener} ya no la usa.
     */
    protected class ButtonChangeListener implements ChangeListener, Serializable {
        ButtonChangeListener() {
        }

        public void stateChanged(ChangeEvent e) {
            cambioDelModelo();
        }
    }

    /** Habilita o deshabilita boton y modelo; deshabilitar apaga el rollover. */
    public void setEnabled(boolean b) {
        if (!b && model.isRollover()) {
            model.setRollover(false);
        }
        super.setEnabled(b);
        model.setEnabled(b);
    }

    /** @deprecated es {@link #getText}. */
    @Deprecated
    public String getLabel() {
        return getText();
    }

    /** @deprecated es {@link #setText}. */
    @Deprecated
    public void setLabel(String label) {
        setText(label);
    }

    public void addItemListener(ItemListener l) {
        listenerList.add(ItemListener.class, l);
    }

    public void removeItemListener(ItemListener l) {
        listenerList.remove(ItemListener.class, l);
    }

    public ItemListener[] getItemListeners() {
        return listenerList.getListeners(ItemListener.class);
    }

    /** El texto, en un arreglo de uno, si esta seleccionado; {@code null} si no. */
    public Object[] getSelectedObjects() {
        if (!isSelected()) {
            return null;
        }
        Object[] seleccionados = new Object[1];
        seleccionados[0] = getText();
        return seleccionados;
    }

    /**
     * Lo que hace cada constructor: texto, icono, aspecto, y alineado a la izquierda y al centro.
     */
    protected void init(String text, Icon icon) {
        if (text != null) {
            setText(text);
        }
        if (icon != null) {
            setIcon(icon);
        }
        updateUI();
        setAlignmentX(LEFT_ALIGNMENT);
        setAlignmentY(CENTER_ALIGNMENT);
    }

    /**
     * Llego mas de una imagen: repinta si hay un icono a la vista.
     *
     * <p>El JDK comprueba ademas que la imagen sea la del icono que se muestra, cosa que solo
     * puede saber con {@code ImageIcon}. Sin el, cualquier imagen que llegue mientras hay icono
     * repinta; de mas, nunca de menos.
     */
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        Icon aLaVista = null;
        if (!model.isEnabled()) {
            if (model.isSelected()) {
                aLaVista = getDisabledSelectedIcon();
            } else {
                aLaVista = getDisabledIcon();
            }
        } else if (model.isPressed() && model.isArmed()) {
            aLaVista = getPressedIcon();
        } else if (isRolloverEnabled() && model.isRollover()) {
            if (model.isSelected()) {
                aLaVista = getRolloverSelectedIcon();
            } else {
                aLaVista = getRolloverIcon();
            }
        } else if (model.isSelected()) {
            aLaVista = getSelectedIcon();
        }
        if (aLaVista == null) {
            aLaVista = getIcon();
        }
        if (aLaVista == null) {
            return false;
        }
        return super.imageUpdate(img, infoflags, x, y, w, h);
    }

    /** Ver la nota de la clase; las cuatro propias, y el resto a {@code JComponent}. */
    void setUIProperty(String propertyName, Object value) {
        if ("borderPainted".equals(propertyName)) {
            if (!borderPaintedSet) {
                setBorderPainted(((Boolean) value).booleanValue());
                borderPaintedSet = false;
            }
        } else if ("rolloverEnabled".equals(propertyName)) {
            if (!rolloverEnabledSet) {
                setRolloverEnabled(((Boolean) value).booleanValue());
                rolloverEnabledSet = false;
            }
        } else if ("iconTextGap".equals(propertyName)) {
            if (!iconTextGapSet) {
                setIconTextGap(((Number) value).intValue());
                iconTextGapSet = false;
            }
        } else if ("contentAreaFilled".equals(propertyName)) {
            if (!contentAreaFilledSet) {
                setContentAreaFilled(((Boolean) value).booleanValue());
                contentAreaFilledSet = false;
            }
        } else {
            super.setUIProperty(propertyName, value);
        }
    }

    protected String paramString() {
        String defaultIconString = (defaultIcon != null && defaultIcon != this)
                ? defaultIcon.toString() : "";
        String pressedIconString = (pressedIcon != null && pressedIcon != this)
                ? pressedIcon.toString() : "";
        String disabledIconString = (disabledIcon != null && disabledIcon != this)
                ? disabledIcon.toString() : "";
        String selectedIconString = (selectedIcon != null && selectedIcon != this)
                ? selectedIcon.toString() : "";
        String disabledSelectedIconString =
                (disabledSelectedIcon != null && disabledSelectedIcon != this)
                ? disabledSelectedIcon.toString() : "";
        String rolloverIconString = (rolloverIcon != null && rolloverIcon != this)
                ? rolloverIcon.toString() : "";
        String rolloverSelectedIconString =
                (rolloverSelectedIcon != null && rolloverSelectedIcon != this)
                ? rolloverSelectedIcon.toString() : "";
        String paintBorderString = paintBorder ? "true" : "false";
        String paintFocusString = paintFocus ? "true" : "false";
        String rolloverEnabledString = rolloverEnabled ? "true" : "false";

        return super.paramString() + ",defaultIcon=" + defaultIconString + ",disabledIcon="
                + disabledIconString + ",disabledSelectedIcon=" + disabledSelectedIconString
                + ",margin=" + margin + ",paintBorder=" + paintBorderString + ",paintFocus="
                + paintFocusString + ",pressedIcon=" + pressedIconString + ",rolloverEnabled="
                + rolloverEnabledString + ",rolloverIcon=" + rolloverIconString
                + ",rolloverSelectedIcon=" + rolloverSelectedIconString + ",selectedIcon="
                + selectedIconString + ",text=" + text;
    }
}
