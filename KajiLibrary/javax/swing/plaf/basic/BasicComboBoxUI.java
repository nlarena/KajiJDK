package javax.swing.plaf.basic;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.CellRendererPane;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;

/**
 * El aspecto basico de un combo.
 *
 * <h2>Tres piezas que se arman y se desarman</h2>
 *
 * <p>Un combo no editable tiene el valor dibujado y una flechita; uno editable tiene ademas un campo
 * de texto encima del valor. Cambiar de uno a otro en caliente --{@code setEditable}-- agrega o saca
 * el editor sin rehacer nada mas, y de eso se ocupan {@link #addEditor} y {@link #removeEditor}.
 *
 * <p>El valor de un combo no editable no es un componente: se dibuja con el dibujante de la lista
 * sobre un {@link CellRendererPane}, igual que una celda. Por eso {@link #paintCurrentValue} existe
 * y no hay una etiqueta adentro.
 *
 * <h2>El tamano se cachea porque medirlo es caro</h2>
 *
 * <p>El ancho de un combo es el del item mas ancho, y saberlo obliga a armar el dibujante con
 * <em>cada</em> item. Para un combo de mil items eso es carisimo y no cambia entre dos dibujados,
 * asi que se guarda en {@link #cachedMinimumSize} y se recalcula solo cuando
 * {@link #isMinimumSizeDirty} lo pide -- al cambiar el modelo, la fuente o el dibujante --.
 *
 * <p>El ancho total es el del item mas ancho mas el de la flechita. El preferido y el minimo son el
 * mismo numero: un combo no tiene por que ser mas ancho de lo que necesita.
 *
 * <h2>Que teclas son de navegacion</h2>
 *
 * <p>{@link #isNavigationKey} contesta que si solo a las flechas de arriba y abajo. Ni Re Pag ni Av
 * Pag ni Enter: esta medido, y tiene sentido -- un combo no pagina, y Enter es del dialogo --.
 *
 * <h2>Lo que queda dicho</h2>
 *
 * <p>Los dos escuchas que quedan en nulo son el de items y el de teclas de la ventana emergente.
 * Medido, y es la misma historia que en los otros UI: hay un solo objeto que escucha todo, y
 * engancharlo dos veces lo haria reaccionar dos veces.
 */
public class BasicComboBoxUI extends ComboBoxUI {

    protected JComboBox comboBox;

    /** Si el combo tiene el foco; de eso depende como se dibuja el valor. */
    protected boolean hasFocus = false;

    protected JList listBox;

    /** Donde se dibuja el valor de un combo no editable; ver la nota de la clase. */
    protected CellRendererPane currentValuePane = new CellRendererPane();

    protected Component editor;
    protected JButton arrowButton;
    protected ComboPopup popup;

    protected KeyListener keyListener;
    protected FocusListener focusListener;
    protected PropertyChangeListener propertyChangeListener;
    protected ItemListener itemListener;
    protected MouseListener popupMouseListener;
    protected MouseMotionListener popupMouseMotionListener;
    protected KeyListener popupKeyListener;
    protected ListDataListener listDataListener;

    /** Si el tamano guardado quedo viejo; ver la nota de la clase. */
    protected boolean isMinimumSizeDirty = true;

    protected Dimension cachedMinimumSize = new Dimension(0, 0);

    /** Si la flechita tiene que ser cuadrada. */
    protected boolean squareButton = true;

    /** Lo que se le agrega al valor dibujado; nulo si el aspecto no pide ninguno. */
    protected Insets padding;

    private static final ColorUIResource FONDO = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FRENTE = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FUENTE = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicComboBoxUI() {
    }

    /** Uno nuevo por combo: guarda el componente, su lista y su tamano medido. */
    public static ComponentUI createUI(JComponent c) {
        return new BasicComboBoxUI();
    }

    public void installUI(JComponent c) {
        isMinimumSizeDirty = true;
        comboBox = (JComboBox) c;
        installDefaults();
        popup = createPopup();
        listBox = popup.getList();
        if (comboBox.getRenderer() == null || comboBox.getRenderer() instanceof UIResource) {
            ListCellRenderer r = createRenderer();
            comboBox.setRenderer(r);
        }
        ComboBoxEditor e = comboBox.getEditor();
        if (e == null || e instanceof UIResource) {
            comboBox.setEditor(createEditor());
        }
        installListeners();
        installComponents();
        comboBox.setLayout(createLayoutManager());
        comboBox.setRequestFocusEnabled(true);
        installKeyboardActions();
        comboBox.putClientProperty("doNotCancelPopup", null);
    }

    public void uninstallUI(JComponent c) {
        setPopupVisible(comboBox, false);
        popup.uninstallingUI();
        uninstallKeyboardActions();
        uninstallComponents();
        uninstallListeners();
        uninstallDefaults();
        comboBox.setLayout(null);
        comboBox = null;
        listBox = null;
        popup = null;
    }

    /** Colores, fuente y relleno; los valores son los de {@code ComboBox.*} en Metal. */
    protected void installDefaults() {
        Color fondo = comboBox.getBackground();
        if (fondo == null || fondo instanceof UIResource) {
            comboBox.setBackground(FONDO);
        }
        Color frente = comboBox.getForeground();
        if (frente == null || frente instanceof UIResource) {
            comboBox.setForeground(FRENTE);
        }
        Font fuente = comboBox.getFont();
        if (fuente == null || fuente instanceof UIResource) {
            comboBox.setFont(FUENTE);
        }
        LookAndFeel.installProperty(comboBox, "opaque", Boolean.TRUE);
        squareButton = true;
        padding = null;
    }

    /** No saca nada; ver {@link BasicPanelUI#uninstallDefaults}. */
    protected void uninstallDefaults() {
    }

    protected void installListeners() {
        itemListener = createItemListener();
        if (itemListener != null) {
            comboBox.addItemListener(itemListener);
        }
        propertyChangeListener = createPropertyChangeListener();
        if (propertyChangeListener != null) {
            comboBox.addPropertyChangeListener(propertyChangeListener);
        }
        keyListener = createKeyListener();
        if (keyListener != null) {
            comboBox.addKeyListener(keyListener);
        }
        focusListener = createFocusListener();
        if (focusListener != null) {
            comboBox.addFocusListener(focusListener);
        }
        popupMouseListener = popup.getMouseListener();
        if (popupMouseListener != null) {
            comboBox.addMouseListener(popupMouseListener);
        }
        popupMouseMotionListener = popup.getMouseMotionListener();
        if (popupMouseMotionListener != null) {
            comboBox.addMouseMotionListener(popupMouseMotionListener);
        }
        popupKeyListener = popup.getKeyListener();
        if (popupKeyListener != null) {
            comboBox.addKeyListener(popupKeyListener);
        }
        listDataListener = createListDataListener();
        if (listDataListener != null && comboBox.getModel() != null) {
            comboBox.getModel().addListDataListener(listDataListener);
        }
    }

    protected void uninstallListeners() {
        if (keyListener != null) {
            comboBox.removeKeyListener(keyListener);
        }
        if (itemListener != null) {
            comboBox.removeItemListener(itemListener);
        }
        if (propertyChangeListener != null) {
            comboBox.removePropertyChangeListener(propertyChangeListener);
        }
        if (focusListener != null) {
            comboBox.removeFocusListener(focusListener);
        }
        if (popupMouseListener != null) {
            comboBox.removeMouseListener(popupMouseListener);
        }
        if (popupMouseMotionListener != null) {
            comboBox.removeMouseMotionListener(popupMouseMotionListener);
        }
        if (popupKeyListener != null) {
            comboBox.removeKeyListener(popupKeyListener);
        }
        if (listDataListener != null && comboBox.getModel() != null) {
            comboBox.getModel().removeListDataListener(listDataListener);
        }
        keyListener = null;
        itemListener = null;
        propertyChangeListener = null;
        focusListener = null;
        popupMouseListener = null;
        popupMouseMotionListener = null;
        popupKeyListener = null;
        listDataListener = null;
    }

    /** Sin atajos propios: las flechas las maneja el escucha de teclas. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    /** Pone la flechita, y el editor si el combo es editable. */
    protected void installComponents() {
        arrowButton = createArrowButton();
        if (arrowButton != null) {
            comboBox.add(arrowButton);
            configureArrowButton();
        }
        if (comboBox.isEditable()) {
            addEditor();
        }
        comboBox.add(currentValuePane);
    }

    protected void uninstallComponents() {
        if (arrowButton != null) {
            unconfigureArrowButton();
        }
        if (editor != null) {
            unconfigureEditor();
        }
        comboBox.removeAll();
        arrowButton = null;
    }

    protected KeyListener createKeyListener() {
        return new Handler(this);
    }

    protected FocusListener createFocusListener() {
        return new Handler(this);
    }

    /** Ninguno; ver la nota de la clase. */
    protected ItemListener createItemListener() {
        return null;
    }

    protected PropertyChangeListener createPropertyChangeListener() {
        return new Handler(this);
    }

    protected LayoutManager createLayoutManager() {
        return new Handler(this);
    }

    protected ListDataListener createListDataListener() {
        return new Handler(this);
    }

    /** El dibujante de los items: el mismo para la lista y para el valor de arriba. */
    protected ListCellRenderer createRenderer() {
        return new BasicComboBoxRenderer.UIResource();
    }

    protected ComboBoxEditor createEditor() {
        return new BasicComboBoxEditor.UIResource();
    }

    protected ComboPopup createPopup() {
        return new BasicComboPopup(comboBox);
    }

    /** Una flecha para abajo; sin foco, porque el foco lo tiene el combo. */
    protected JButton createArrowButton() {
        JButton b = new BasicArrowButton(SwingConstants.SOUTH);
        b.setName("ComboBox.arrowButton");
        return b;
    }

    /** Le engancha los escuchas del combo, para que apretarla abra la lista. */
    public void configureArrowButton() {
        if (arrowButton != null) {
            arrowButton.setEnabled(comboBox.isEnabled());
            arrowButton.setFocusable(comboBox.isFocusable());
            arrowButton.setRequestFocusEnabled(false);
            arrowButton.addMouseListener(popup.getMouseListener());
            arrowButton.addMouseMotionListener(popup.getMouseMotionListener());
            arrowButton.resetKeyboardActions();
            arrowButton.setInheritsPopupMenu(true);
        }
    }

    public void unconfigureArrowButton() {
        if (arrowButton != null) {
            arrowButton.removeMouseListener(popup.getMouseListener());
            arrowButton.removeMouseMotionListener(popup.getMouseMotionListener());
        }
    }

    /** Agrega el campo de texto de un combo editable. */
    public void addEditor() {
        removeEditor();
        editor = comboBox.getEditor().getEditorComponent();
        if (editor != null) {
            configureEditor();
            comboBox.add(editor);
            if (comboBox.isFocusOwner()) {
                editor.requestFocusInWindow();
            }
        }
    }

    public void removeEditor() {
        if (editor != null) {
            unconfigureEditor();
            comboBox.remove(editor);
            editor = null;
        }
    }

    /** Le pasa al editor la fuente, los colores y el valor. */
    protected void configureEditor() {
        editor.setFont(comboBox.getFont());
        editor.setForeground(comboBox.getForeground());
        editor.setBackground(comboBox.getBackground());
        editor.setEnabled(comboBox.isEnabled());
        editor.setFocusable(comboBox.isFocusable());
        comboBox.getEditor().setItem(comboBox.getSelectedItem());
        if (editor instanceof JComponent) {
            ((JComponent) editor).setInheritsPopupMenu(true);
        }
    }

    protected void unconfigureEditor() {
    }

    public boolean isPopupVisible(JComboBox c) {
        return popup.isVisible();
    }

    public void setPopupVisible(JComboBox c, boolean v) {
        if (v) {
            popup.show();
        } else {
            popup.hide();
        }
    }

    /** Un combo comun si; uno con la flechita apagada no. */
    public boolean isFocusTraversable(JComboBox c) {
        return !comboBox.isEditable();
    }

    /** Abre si esta cerrado y cierra si esta abierto. */
    protected void toggleOpenClose() {
        setPopupVisible(comboBox, !isPopupVisible(comboBox));
    }

    /** Elige el valor siguiente del modelo. */
    protected void selectNextPossibleValue() {
        int si = isPopupVisible(comboBox) ? listBox.getSelectedIndex()
                : comboBox.getSelectedIndex();
        if (si < comboBox.getModel().getSize() - 1) {
            if (isPopupVisible(comboBox)) {
                listBox.setSelectedIndex(si + 1);
                listBox.ensureIndexIsVisible(si + 1);
            } else {
                comboBox.setSelectedIndex(si + 1);
            }
        }
    }

    /** Y el anterior. */
    protected void selectPreviousPossibleValue() {
        int si = isPopupVisible(comboBox) ? listBox.getSelectedIndex()
                : comboBox.getSelectedIndex();
        if (si > 0) {
            if (isPopupVisible(comboBox)) {
                listBox.setSelectedIndex(si - 1);
                listBox.ensureIndexIsVisible(si - 1);
            } else {
                comboBox.setSelectedIndex(si - 1);
            }
        }
    }

    /** Solo las flechas de arriba y abajo; ver la nota de la clase. */
    protected boolean isNavigationKey(int keyCode) {
        return keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN;
    }

    protected Insets getInsets() {
        return comboBox.getInsets();
    }

    /** Donde va el valor: todo lo que queda a la izquierda de la flechita. */
    protected Rectangle rectangleForCurrentValue() {
        int width = comboBox.getWidth();
        int height = comboBox.getHeight();
        Insets insets = getInsets();
        int buttonSize = height - (insets.top + insets.bottom);
        if (arrowButton != null) {
            buttonSize = arrowButton.getWidth();
        }
        if (comboBox.getComponentOrientation().isLeftToRight()) {
            return new Rectangle(insets.left, insets.top,
                    width - (insets.left + insets.right + buttonSize),
                    height - (insets.top + insets.bottom));
        }
        return new Rectangle(insets.left + buttonSize, insets.top,
                width - (insets.left + insets.right + buttonSize),
                height - (insets.top + insets.bottom));
    }

    /** Lo que mide ese componente ya armado por el dibujante. */
    protected Dimension getSizeForComponent(Component comp) {
        currentValuePane.add(comp);
        comp.setFont(comboBox.getFont());
        Dimension d = comp.getPreferredSize();
        currentValuePane.remove(comp);
        return d;
    }

    /** El tamano de un renglon vacio; el piso de cualquier combo. */
    protected Dimension getDefaultSize() {
        ListCellRenderer renderer = comboBox.getRenderer();
        if (renderer == null) {
            renderer = new javax.swing.DefaultListCellRenderer();
        }
        Component c = renderer.getListCellRendererComponent(listBox, " ", -1, false, false);
        return getSizeForComponent(c);
    }

    /** El del item mas ancho; ver la nota de la clase sobre por que se guarda. */
    protected Dimension getDisplaySize() {
        Dimension result = new Dimension();
        ListCellRenderer renderer = comboBox.getRenderer();
        if (renderer == null) {
            renderer = new javax.swing.DefaultListCellRenderer();
        }
        Object prototypeValue = comboBox.getPrototypeDisplayValue();
        if (prototypeValue != null) {
            Component c = renderer.getListCellRendererComponent(listBox, prototypeValue, -1,
                    false, false);
            return getSizeForComponent(c);
        }
        ComboBoxModel model = comboBox.getModel();
        int modelSize = model.getSize();
        if (modelSize > 0) {
            for (int i = 0; i < modelSize; i++) {
                Object value = model.getElementAt(i);
                Component c = renderer.getListCellRendererComponent(listBox, value, -1,
                        false, false);
                Dimension d = getSizeForComponent(c);
                result.width = Math.max(result.width, d.width);
                result.height = Math.max(result.height, d.height);
            }
        } else {
            result = getDefaultSize();
            if (comboBox.isEditable()) {
                result.width = 100;
            }
        }
        if (padding != null) {
            result.width += padding.left + padding.right;
            result.height += padding.top + padding.bottom;
        }
        return result;
    }

    /** El del item mas ancho mas la flechita; ver la nota de la clase. */
    public Dimension getMinimumSize(JComponent c) {
        if (!isMinimumSizeDirty) {
            return new Dimension(cachedMinimumSize);
        }
        Dimension size = getDisplaySize();
        Insets insets = getInsets();
        // La flechita es cuadrada: su ancho es el alto del renglon, no su ancho preferido. Con
        // `squareButton` apagado si se le pregunta a ella. Esta medido.
        int buttonHeight = size.height;
        int buttonWidth = squareButton ? buttonHeight
                : ((arrowButton != null) ? arrowButton.getPreferredSize().width : buttonHeight);
        size.height += insets.top + insets.bottom;
        size.width += insets.left + insets.right + buttonWidth;
        cachedMinimumSize.setSize(size.width, size.height);
        isMinimumSizeDirty = false;
        return new Dimension(size);
    }

    /** El mismo que el minimo; ver la nota de la clase. */
    public Dimension getPreferredSize(JComponent c) {
        return getMinimumSize(c);
    }

    /** Sin tope: un combo se estira todo lo que le den. */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /**
     * Donde apoya el texto del valor.
     *
     * @throws NullPointerException si el componente es nulo
     * @throws IllegalArgumentException si el ancho o el alto son negativos
     */
    public int getBaseline(JComponent c, int width, int height) {
        super.getBaseline(c, width, height);
        int baseline = -1;
        Insets insets = getInsets();
        height = height - insets.top - insets.bottom;
        if (height <= 0) {
            return -1;
        }
        if (comboBox.isEditable() && editor != null) {
            baseline = editor.getBaseline(width, height);
        } else {
            ListCellRenderer renderer = comboBox.getRenderer();
            if (renderer == null) {
                renderer = new javax.swing.DefaultListCellRenderer();
            }
            Object value = comboBox.getSelectedItem();
            if (value == null) {
                value = " ";
            }
            Component comp = renderer.getListCellRendererComponent(listBox, value, -1,
                    false, false);
            currentValuePane.add(comp);
            comp.setFont(comboBox.getFont());
            baseline = comp.getBaseline(width, height);
            currentValuePane.remove(comp);
        }
        return (baseline < 0) ? -1 : baseline + insets.top;
    }

    /**
     * {@code CENTER_OFFSET}: el valor va centrado.
     *
     * @throws NullPointerException si el componente es nulo
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CENTER_OFFSET;
    }

    /** El valor, si el combo no es editable; si lo es, lo dibuja el editor. */
    public void paint(Graphics g, JComponent c) {
        hasFocus = comboBox.hasFocus();
        if (!comboBox.isEditable()) {
            Rectangle r = rectangleForCurrentValue();
            paintCurrentValueBackground(g, r, hasFocus);
            paintCurrentValue(g, r, hasFocus);
        }
    }

    /** El valor, dibujado con el dibujante de la lista; ver la nota de la clase. */
    public void paintCurrentValue(Graphics g, Rectangle bounds, boolean hasFocus) {
        ListCellRenderer renderer = comboBox.getRenderer();
        if (renderer == null) {
            return;
        }
        Component c = renderer.getListCellRendererComponent(listBox,
                comboBox.getSelectedItem(), -1, false, false);
        c.setFont(comboBox.getFont());
        if (hasFocus && !isPopupVisible(comboBox)) {
            c.setForeground(listBox.getSelectionForeground());
            c.setBackground(listBox.getSelectionBackground());
        } else if (comboBox.isEnabled()) {
            c.setForeground(comboBox.getForeground());
            c.setBackground(comboBox.getBackground());
        } else {
            c.setForeground(FRENTE);
            c.setBackground(FONDO);
        }
        boolean shouldValidate = (c instanceof Container);
        int x = bounds.x;
        int y = bounds.y;
        int w = bounds.width;
        int h = bounds.height;
        if (padding != null) {
            x += padding.left;
            y += padding.top;
            w -= padding.left + padding.right;
            h -= padding.top + padding.bottom;
        }
        currentValuePane.paintComponent(g, c, comboBox, x, y, w, h, shouldValidate);
    }

    /** El fondo de donde va el valor. */
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        Color t = g.getColor();
        g.setColor(comboBox.isEnabled() ? comboBox.getBackground() : FONDO);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setColor(t);
    }

    /** Uno: la lista desplegable. */
    public int getAccessibleChildrenCount(JComponent c) {
        return 1;
    }

    /** La lista desplegable, si es accesible. */
    public javax.accessibility.Accessible getAccessibleChild(JComponent c, int i) {
        if (i == 0 && popup instanceof javax.accessibility.Accessible) {
            return (javax.accessibility.Accessible) popup;
        }
        return null;
    }

    /**
     * El que escucha todo y ademas acomoda.
     *
     * <p>El acomodador es parte del mismo objeto porque el reparto depende de si el combo es
     * editable, que es justo lo que este escucha sigue.
     */
    private static class Handler implements KeyListener, FocusListener, PropertyChangeListener,
            LayoutManager, ListDataListener, ItemListener, ActionListener {

        private final BasicComboBoxUI ui;

        Handler(BasicComboBoxUI ui) {
            this.ui = ui;
        }

        public void keyPressed(KeyEvent e) {
            if (ui.isNavigationKey(e.getKeyCode())) {
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    ui.selectNextPossibleValue();
                } else {
                    ui.selectPreviousPossibleValue();
                }
                e.consume();
            }
        }

        public void keyReleased(KeyEvent e) {
        }

        public void keyTyped(KeyEvent e) {
        }

        public void focusGained(FocusEvent e) {
            ui.hasFocus = true;
            ui.comboBox.repaint();
        }

        public void focusLost(FocusEvent e) {
            ui.hasFocus = false;
            ui.setPopupVisible(ui.comboBox, false);
            ui.comboBox.repaint();
        }

        public void propertyChange(PropertyChangeEvent e) {
            String propertyName = e.getPropertyName();
            JComboBox comboBox = ui.comboBox;
            if ("model".equals(propertyName)) {
                ComboBoxModel viejo = (ComboBoxModel) e.getOldValue();
                ComboBoxModel nuevo = (ComboBoxModel) e.getNewValue();
                if (viejo != null && ui.listDataListener != null) {
                    viejo.removeListDataListener(ui.listDataListener);
                }
                if (nuevo != null && ui.listDataListener != null) {
                    nuevo.addListDataListener(ui.listDataListener);
                }
                ui.isMinimumSizeDirty = true;
                comboBox.revalidate();
                comboBox.repaint();
            } else if ("editable".equals(propertyName)) {
                if (comboBox.isEditable()) {
                    ui.addEditor();
                } else {
                    ui.removeEditor();
                }
                ui.isMinimumSizeDirty = true;
                comboBox.revalidate();
            } else if ("enabled".equals(propertyName)) {
                if (ui.arrowButton != null) {
                    ui.arrowButton.setEnabled(comboBox.isEnabled());
                }
                if (ui.editor != null) {
                    ui.editor.setEnabled(comboBox.isEnabled());
                }
                comboBox.repaint();
            } else if ("font".equals(propertyName) || "renderer".equals(propertyName)
                    || "prototypeDisplayValue".equals(propertyName)) {
                ui.isMinimumSizeDirty = true;
                comboBox.revalidate();
                comboBox.repaint();
            } else if ("editor".equals(propertyName) && comboBox.isEditable()) {
                ui.addEditor();
                comboBox.revalidate();
            }
        }

        public void addLayoutComponent(String name, Component comp) {
        }

        public void removeLayoutComponent(Component comp) {
        }

        public Dimension preferredLayoutSize(Container parent) {
            return ui.getPreferredSize((JComponent) parent);
        }

        public Dimension minimumLayoutSize(Container parent) {
            return ui.getMinimumSize((JComponent) parent);
        }

        /** La flechita a la derecha y el resto para el valor o el editor. */
        public void layoutContainer(Container parent) {
            JComboBox cb = (JComboBox) parent;
            int width = cb.getWidth();
            int height = cb.getHeight();
            Insets insets = ui.getInsets();
            int buttonHeight = height - (insets.top + insets.bottom);
            int buttonWidth = buttonHeight;
            if (ui.arrowButton != null) {
                Insets ai = ui.arrowButton.getInsets();
                buttonWidth = ui.squareButton ? buttonHeight
                        : (ui.arrowButton.getPreferredSize().width + ai.left + ai.right);
                if (cb.getComponentOrientation().isLeftToRight()) {
                    ui.arrowButton.setBounds(width - (insets.right + buttonWidth),
                            insets.top, buttonWidth, buttonHeight);
                } else {
                    ui.arrowButton.setBounds(insets.left, insets.top, buttonWidth, buttonHeight);
                }
            }
            if (ui.editor != null) {
                ui.editor.setBounds(ui.rectangleForCurrentValue());
            }
        }

        public void intervalAdded(ListDataEvent e) {
            contentsChanged(e);
        }

        public void intervalRemoved(ListDataEvent e) {
            contentsChanged(e);
        }

        public void contentsChanged(ListDataEvent e) {
            ui.isMinimumSizeDirty = true;
            if (ui.comboBox != null) {
                ui.comboBox.revalidate();
                ui.comboBox.repaint();
            }
        }

        public void itemStateChanged(ItemEvent e) {
        }

        public void actionPerformed(ActionEvent e) {
        }
    }
}
