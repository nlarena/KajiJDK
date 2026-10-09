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
 * The basic look and feel of a combo box.
 *
 * <h2>Three pieces that are put together and taken apart</h2>
 *
 * <p>A non-editable combo box has the value drawn and a little arrow; an editable one has on
 * top of that a text field over the value. Changing from one to the other on the fly
 * -- {@code setEditable} -- adds or removes the editor without rebuilding anything else, and
 * {@link #addEditor} and {@link #removeEditor} take care of that.
 *
 * <p>A non-editable combo box's value is not a component: it is drawn with the list's renderer
 * over a {@link CellRendererPane}, just like a cell. That is why {@link #paintCurrentValue}
 * exists and there is no label inside.
 *
 * <h2>The size is cached because measuring it is expensive</h2>
 *
 * <p>A combo box's width is that of the widest item, and knowing it forces the renderer to be
 * built with <em>each</em> item. For a combo box of a thousand items that is terribly expensive
 * and does not change between two paints, so it is kept in {@link #cachedMinimumSize} and is
 * recomputed only when {@link #isMinimumSizeDirty} asks for it -- on changing the model, the
 * typeface or the renderer --.
 *
 * <p>The total width is that of the widest item plus that of the little arrow. The preferred
 * one and the minimum are the same number: there is no reason for a combo box to be wider than
 * it needs.
 *
 * <h2>Which keys are navigation ones</h2>
 *
 * <p>{@link #isNavigationKey} answers yes only to the up and down arrows. Neither Page Up nor
 * Page Down nor Enter: it is measured, and it makes sense -- a combo box does not page, and
 * Enter belongs to the dialog --.
 *
 * <h2>What is left said</h2>
 *
 * <p>The two listeners that are left null are the item one and the popup window's key one.
 * Measured, and it is the same story as in the other looks and feels: there is a single object
 * that listens to everything, and hooking it twice would make it react twice.
 */
public class BasicComboBoxUI extends ComboBoxUI {

    protected JComboBox comboBox;

    /** Whether the combo box has the focus; how the value is drawn depends on that. */
    protected boolean hasFocus = false;

    protected JList listBox;

    /** Where a non-editable combo box's value is drawn; see the class note. */
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

    /** Whether the kept size went stale; see the class note. */
    protected boolean isMinimumSizeDirty = true;

    protected Dimension cachedMinimumSize = new Dimension(0, 0);

    /** Whether the little arrow has to be square. */
    protected boolean squareButton = true;

    /** What is added to the drawn value; null if the look and feel asks for none. */
    protected Insets padding;

    private static final ColorUIResource BACKGROUND = new ColorUIResource(238, 238, 238);
    private static final ColorUIResource FOREGROUND = new ColorUIResource(51, 51, 51);
    private static final FontUIResource FONT = new FontUIResource("Dialog", Font.BOLD, 12);

    public BasicComboBoxUI() {
    }

    /** A new one per combo box: it keeps the component, its list and its measured size. */
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

    /** Colours, typeface and padding; the values are those of {@code ComboBox.*} in Metal. */
    protected void installDefaults() {
        Color background = comboBox.getBackground();
        if (background == null || background instanceof UIResource) {
            comboBox.setBackground(BACKGROUND);
        }
        Color foreground = comboBox.getForeground();
        if (foreground == null || foreground instanceof UIResource) {
            comboBox.setForeground(FOREGROUND);
        }
        Font font = comboBox.getFont();
        if (font == null || font instanceof UIResource) {
            comboBox.setFont(FONT);
        }
        LookAndFeel.installProperty(comboBox, "opaque", Boolean.TRUE);
        squareButton = true;
        padding = null;
    }

    /** It removes nothing; see {@link BasicPanelUI#uninstallDefaults}. */
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

    /** With no shortcuts of its own: the arrows are handled by the key listener. */
    protected void installKeyboardActions() {
    }

    protected void uninstallKeyboardActions() {
    }

    /** It puts in the little arrow, and the editor if the combo box is editable. */
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

    /** None; see the class note. */
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

    /** The items' renderer: the same one for the list and for the value at the top. */
    protected ListCellRenderer createRenderer() {
        return new BasicComboBoxRenderer.UIResource();
    }

    protected ComboBoxEditor createEditor() {
        return new BasicComboBoxEditor.UIResource();
    }

    protected ComboPopup createPopup() {
        return new BasicComboPopup(comboBox);
    }

    /** A down arrow; with no focus, because the focus is held by the combo box. */
    protected JButton createArrowButton() {
        JButton b = new BasicArrowButton(SwingConstants.SOUTH);
        b.setName("ComboBox.arrowButton");
        return b;
    }

    /** It hooks the combo box's listeners to it, so that pressing it opens the list. */
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

    /** It adds an editable combo box's text field. */
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

    /** It passes the typeface, the colours and the value on to the editor. */
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

    /** An ordinary combo box yes; one with the little arrow switched off no. */
    public boolean isFocusTraversable(JComboBox c) {
        return !comboBox.isEditable();
    }

    /** It opens if it is closed and closes if it is open. */
    protected void toggleOpenClose() {
        setPopupVisible(comboBox, !isPopupVisible(comboBox));
    }

    /** It chooses the model's next value. */
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

    /** And the previous one. */
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

    /** Only the up and down arrows; see the class note. */
    protected boolean isNavigationKey(int keyCode) {
        return keyCode == KeyEvent.VK_UP || keyCode == KeyEvent.VK_DOWN;
    }

    protected Insets getInsets() {
        return comboBox.getInsets();
    }

    /** Where the value goes: everything left over to the left of the little arrow. */
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

    /** How much that component measures once built by the renderer. */
    protected Dimension getSizeForComponent(Component comp) {
        currentValuePane.add(comp);
        comp.setFont(comboBox.getFont());
        Dimension d = comp.getPreferredSize();
        currentValuePane.remove(comp);
        return d;
    }

    /** An empty line's size; the floor of any combo box. */
    protected Dimension getDefaultSize() {
        ListCellRenderer renderer = comboBox.getRenderer();
        if (renderer == null) {
            renderer = new javax.swing.DefaultListCellRenderer();
        }
        Component c = renderer.getListCellRendererComponent(listBox, " ", -1, false, false);
        return getSizeForComponent(c);
    }

    /** The widest item's; see the class note about why it is kept. */
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

    /** The widest item's plus the little arrow; see the class note. */
    public Dimension getMinimumSize(JComponent c) {
        if (!isMinimumSizeDirty) {
            return new Dimension(cachedMinimumSize);
        }
        Dimension size = getDisplaySize();
        Insets insets = getInsets();
        // The little arrow is square: its width is the line's height, not its preferred width.
                    // With `squareButton` off it is asked. It is measured.
        int buttonHeight = size.height;
        int buttonWidth = squareButton ? buttonHeight
                : ((arrowButton != null) ? arrowButton.getPreferredSize().width : buttonHeight);
        size.height += insets.top + insets.bottom;
        size.width += insets.left + insets.right + buttonWidth;
        cachedMinimumSize.setSize(size.width, size.height);
        isMinimumSizeDirty = false;
        return new Dimension(size);
    }

    /** The same as the minimum; see the class note. */
    public Dimension getPreferredSize(JComponent c) {
        return getMinimumSize(c);
    }

    /** No cap: a combo box stretches as far as it is given. */
    public Dimension getMaximumSize(JComponent c) {
        return new Dimension(Short.MAX_VALUE, Short.MAX_VALUE);
    }

    /**
     * Where the value's text rests.
     *
     * @throws NullPointerException if the component is null
     * @throws IllegalArgumentException if the width or the height are negative
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
     * {@code CENTER_OFFSET}: the value goes centred.
     *
     * @throws NullPointerException if the component is null
     */
    public Component.BaselineResizeBehavior getBaselineResizeBehavior(JComponent c) {
        super.getBaselineResizeBehavior(c);
        return Component.BaselineResizeBehavior.CENTER_OFFSET;
    }

    /** The value, if the combo box is not editable; if it is, it is drawn by the editor. */
    public void paint(Graphics g, JComponent c) {
        hasFocus = comboBox.hasFocus();
        if (!comboBox.isEditable()) {
            Rectangle r = rectangleForCurrentValue();
            paintCurrentValueBackground(g, r, hasFocus);
            paintCurrentValue(g, r, hasFocus);
        }
    }

    /** The value, drawn with the list's renderer; see the class note. */
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
            c.setForeground(FOREGROUND);
            c.setBackground(BACKGROUND);
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

    /** The background of where the value goes. */
    public void paintCurrentValueBackground(Graphics g, Rectangle bounds, boolean hasFocus) {
        Color t = g.getColor();
        g.setColor(comboBox.isEnabled() ? comboBox.getBackground() : BACKGROUND);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.setColor(t);
    }

    /** One: the drop-down list. */
    public int getAccessibleChildrenCount(JComponent c) {
        return 1;
    }

    /** The drop-down list, if it is accessible. */
    public javax.accessibility.Accessible getAccessibleChild(JComponent c, int i) {
        if (i == 0 && popup instanceof javax.accessibility.Accessible) {
            return (javax.accessibility.Accessible) popup;
        }
        return null;
    }

    /**
     * The one that listens to everything and also lays out.
     *
     * <p>The layout is part of the same object because the sharing out depends on whether the
     * combo box is editable, which is just what this listener follows.
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
                ComboBoxModel old = (ComboBoxModel) e.getOldValue();
                ComboBoxModel newValue = (ComboBoxModel) e.getNewValue();
                if (old != null && ui.listDataListener != null) {
                    old.removeListDataListener(ui.listDataListener);
                }
                if (newValue != null && ui.listDataListener != null) {
                    newValue.addListDataListener(ui.listDataListener);
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

        /** The little arrow on the right and the rest for the value or the editor. */
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
