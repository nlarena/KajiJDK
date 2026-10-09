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
 * What every button shares: model, text, icons by state, alignments and action.
 *
 * <h2>The button is a view of its model</h2>
 *
 * <p>The state -- armed, pressed, selected, enabled, with the cursor over it -- is not here but
 * in the {@link ButtonModel}. This class listens to the model and repaints itself, and forwards
 * its action, item and change events to the button's listeners, with the button as the source.
 * That is why {@link #setEnabled} writes on both sides and {@link #isSelected} reads from the
 * model: the button has no copy of its own.
 *
 * <h2>Icons by state</h2>
 *
 * <p>Seven slots: the icon, and those for pressed, selected, rollover, rollover selected,
 * disabled and disabled selected. The look and feel chooses which to paint; an empty slot falls
 * back to the ordinary icon. The JDK makes the disabled one by greying an {@code ImageIcon};
 * with no {@code ImageIcon}, {@link #getDisabledIcon} returns what was set and nothing more.
 *
 * <h2>What the user set and what the look and feel set</h2>
 *
 * <p>Four properties -- border painted, rollover, icon-text gap, content area filled -- are
 * proposed by the look and feel on installing itself and may be fixed by the user. Each one
 * remembers who set it ({@code *Set}), and {@link #setUIProperty} only writes those the user
 * did not touch. The margin uses {@link UIResource} for the same.
 *
 * <p>{@code getAccessibleContext} and the class {@code AccessibleAbstractButton} are not there:
 * there is no assistive technology on this VM. {@link #addImpl} does not install
 * {@code OverlayLayout}, which is not there: a child added to a button is left with the
 * container's layout.
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

    /** The model; see the class note. */
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

    /** Whether it can be a dialog's default button; {@code JButton} uses it. */
    boolean defaultCapable = true;

    /** The model's change listener; {@link #createChangeListener} creates it. */
    protected ChangeListener changeListener = null;

    /** The model's action listener; {@link #createActionListener} creates it. */
    protected ActionListener actionListener = null;

    /** The model's item listener; {@link #createItemListener} creates it. */
    protected ItemListener itemListener = null;

    /** The change event that is forwarded, created once. */
    protected transient ChangeEvent changeEvent;

    private Handler handler;

    protected AbstractButton() {
    }

    // -- action ----------------------------------------------------------------------------------

    /**
     * Whether the action's text is hidden: a tool bar button shows only the icon.
     *
     * <p>It only affects the text that comes from the {@link Action}; one set with
     * {@link #setText} is not hidden.
     */
    public void setHideActionText(boolean hideActionText) {
        if (hideActionText != this.hideActionText) {
            this.hideActionText = hideActionText;
            if (getAction() != null) {
                textFromAction(getAction(), false);
            }
            firePropertyChange("hideActionText", !hideActionText, hideActionText);
        }
    }

    public boolean getHideActionText() {
        return hideActionText;
    }

    // -- text and selection ----------------------------------------------------------------------

    public String getText() {
        return text;
    }

    /** It sets the text and recomputes which character the mnemonic underlines. */
    public void setText(String text) {
        String old = this.text;
        this.text = text;
        firePropertyChange(TEXT_CHANGED_PROPERTY, old, text);
        updateMnemonicIndex(text, getMnemonic());
        if (text == null || old == null || !text.equals(old)) {
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

    /** A click like the mouse's: it arms, presses, and releases, firing the action. */
    public void doClick() {
        doClick(68);
    }

    /**
     * A click that is seen: the button stays pressed for {@code pressTime} milliseconds.
     *
     * <p>It is what the mnemonic does: the user sees the button sink, as though they had pressed
     * it. The action is fired on releasing, as always.
     */
    public void doClick(int pressTime) {
        Dimension size = getSize();
        model.setArmed(true);
        model.setPressed(true);
        paintImmediately(0, 0, size.width, size.height);
        try {
            Thread.sleep(pressTime);
        } catch (InterruptedException ie) {
        }
        model.setPressed(false);
        model.setArmed(false);
    }

    // -- margin ----------------------------------------------------------------------------------

    /**
     * The margin between the border and the content.
     *
     * <p>{@code null} goes back to the look and feel's margin: the last {@link UIResource} that
     * was set, which this class remembers for that. Whether the margin counts in the insets
     * depends on the border: the look and feel's carries a {@code MarginBorder} inside; one of the
     * user's does not.
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
        Insets old = margin;
        margin = m;
        firePropertyChange(MARGIN_CHANGED_PROPERTY, old, m);
        if (old == null || !old.equals(m)) {
            revalidate();
            repaint();
        }
    }

    /**
     * A copy of the margin, or {@code null}; the copy keeps whether it is the look and feel's or
     * the user's.
     */
    public Insets getMargin() {
        if (margin == null) {
            return null;
        }
        return (Insets) margin.clone();
    }

    // -- icons -----------------------------------------------------------------------------------

    public Icon getIcon() {
        return defaultIcon;
    }

    /**
     * It sets the icon; if it changes size, the button is laid out again.
     *
     * <p>A disabled icon the look and feel set is discarded: it was a version of this one.
     */
    public void setIcon(Icon defaultIcon) {
        Icon old = this.defaultIcon;
        this.defaultIcon = defaultIcon;
        if (defaultIcon != old && (disabledIcon instanceof UIResource)) {
            disabledIcon = null;
        }
        firePropertyChange(ICON_CHANGED_PROPERTY, old, defaultIcon);
        if (defaultIcon != old) {
            if (defaultIcon == null || old == null
                    || defaultIcon.getIconWidth() != old.getIconWidth()
                    || defaultIcon.getIconHeight() != old.getIconHeight()) {
                revalidate();
            }
            repaint();
        }
    }

    public Icon getPressedIcon() {
        return pressedIcon;
    }

    public void setPressedIcon(Icon pressedIcon) {
        Icon old = this.pressedIcon;
        this.pressedIcon = pressedIcon;
        firePropertyChange(PRESSED_ICON_CHANGED_PROPERTY, old, pressedIcon);
        if (pressedIcon != old) {
            if (getModel().isPressed() && getModel().isArmed()) {
                repaint();
            }
        }
    }

    public Icon getSelectedIcon() {
        return selectedIcon;
    }

    public void setSelectedIcon(Icon selectedIcon) {
        Icon old = this.selectedIcon;
        this.selectedIcon = selectedIcon;
        if (selectedIcon != old && disabledSelectedIcon instanceof UIResource) {
            disabledSelectedIcon = null;
        }
        firePropertyChange(SELECTED_ICON_CHANGED_PROPERTY, old, selectedIcon);
        if (selectedIcon != old) {
            if (isSelected()) {
                repaint();
            }
        }
    }

    public Icon getRolloverIcon() {
        return rolloverIcon;
    }

    /** Setting a rollover icon enables rollover: without that, it would never be seen. */
    public void setRolloverIcon(Icon rolloverIcon) {
        Icon old = this.rolloverIcon;
        this.rolloverIcon = rolloverIcon;
        firePropertyChange(ROLLOVER_ICON_CHANGED_PROPERTY, old, rolloverIcon);
        setRolloverEnabled(true);
        if (rolloverIcon != old) {
            repaint();
        }
    }

    public Icon getRolloverSelectedIcon() {
        return rolloverSelectedIcon;
    }

    public void setRolloverSelectedIcon(Icon rolloverSelectedIcon) {
        Icon old = this.rolloverSelectedIcon;
        this.rolloverSelectedIcon = rolloverSelectedIcon;
        firePropertyChange(ROLLOVER_SELECTED_ICON_CHANGED_PROPERTY, old, rolloverSelectedIcon);
        setRolloverEnabled(true);
        if (rolloverSelectedIcon != old) {
            if (isSelected()) {
                repaint();
            }
        }
    }

    /** The disabled icon that was set, or {@code null}; see the class note. */
    public Icon getDisabledIcon() {
        return disabledIcon;
    }

    public void setDisabledIcon(Icon disabledIcon) {
        Icon old = this.disabledIcon;
        this.disabledIcon = disabledIcon;
        firePropertyChange(DISABLED_ICON_CHANGED_PROPERTY, old, disabledIcon);
        if (disabledIcon != old) {
            if (!isEnabled()) {
                repaint();
            }
        }
    }

    /** The disabled and selected icon that was set, or {@code null}. */
    public Icon getDisabledSelectedIcon() {
        return disabledSelectedIcon;
    }

    public void setDisabledSelectedIcon(Icon disabledSelectedIcon) {
        Icon old = this.disabledSelectedIcon;
        this.disabledSelectedIcon = disabledSelectedIcon;
        firePropertyChange(DISABLED_SELECTED_ICON_CHANGED_PROPERTY, old, disabledSelectedIcon);
        if (disabledSelectedIcon != old) {
            if (disabledSelectedIcon == null || old == null
                    || disabledSelectedIcon.getIconWidth() != old.getIconWidth()
                    || disabledSelectedIcon.getIconHeight() != old.getIconHeight()) {
                revalidate();
            }
            if (!isEnabled() && isSelected()) {
                repaint();
            }
        }
    }

    // -- alignments ------------------------------------------------------------------------------

    public int getVerticalAlignment() {
        return verticalAlignment;
    }

    public void setVerticalAlignment(int alignment) {
        if (alignment == verticalAlignment) {
            return;
        }
        int old = verticalAlignment;
        verticalAlignment = checkVerticalKey(alignment, "verticalAlignment");
        firePropertyChange(VERTICAL_ALIGNMENT_CHANGED_PROPERTY, old, verticalAlignment);
        repaint();
    }

    public int getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public void setHorizontalAlignment(int alignment) {
        if (alignment == horizontalAlignment) {
            return;
        }
        int old = horizontalAlignment;
        horizontalAlignment = checkHorizontalKey(alignment, "horizontalAlignment");
        firePropertyChange(HORIZONTAL_ALIGNMENT_CHANGED_PROPERTY, old, horizontalAlignment);
        repaint();
    }

    public int getVerticalTextPosition() {
        return verticalTextPosition;
    }

    public void setVerticalTextPosition(int textPosition) {
        if (textPosition == verticalTextPosition) {
            return;
        }
        int old = verticalTextPosition;
        verticalTextPosition = checkVerticalKey(textPosition, "verticalTextPosition");
        firePropertyChange(VERTICAL_TEXT_POSITION_CHANGED_PROPERTY, old, verticalTextPosition);
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
        int old = horizontalTextPosition;
        horizontalTextPosition = checkHorizontalKey(textPosition, "horizontalTextPosition");
        firePropertyChange(HORIZONTAL_TEXT_POSITION_CHANGED_PROPERTY, old,
                horizontalTextPosition);
        revalidate();
        repaint();
    }

    public int getIconTextGap() {
        return iconTextGap;
    }

    public void setIconTextGap(int iconTextGap) {
        int old = this.iconTextGap;
        this.iconTextGap = iconTextGap;
        iconTextGapSet = true;
        firePropertyChange("iconTextGap", old, iconTextGap);
        if (iconTextGap != old) {
            revalidate();
            repaint();
        }
    }

    /** It validates a horizontal key; {@code exception} is the name that goes in the error. */
    protected int checkHorizontalKey(int key, String exception) {
        if (key == LEFT || key == CENTER || key == RIGHT || key == LEADING || key == TRAILING) {
            return key;
        }
        throw new IllegalArgumentException(exception);
    }

    /** It validates a vertical key. */
    protected int checkVerticalKey(int key, String exception) {
        if (key == TOP || key == CENTER || key == BOTTOM) {
            return key;
        }
        throw new IllegalArgumentException(exception);
    }

    /** It leaves the hierarchy: if it was showing rollover, it stops showing it. */
    public void removeNotify() {
        super.removeNotify();
        if (isRolloverEnabled()) {
            getModel().setRollover(false);
        }
    }

    // -- command and action ----------------------------------------------------------------------

    public void setActionCommand(String actionCommand) {
        getModel().setActionCommand(actionCommand);
    }

    /** The model's command, or the text if the model does not have one. */
    public String getActionCommand() {
        String ac = getModel().getActionCommand();
        if (ac == null) {
            ac = getText();
        }
        return ac;
    }

    /**
     * It ties the button to an action: it takes text, icon, mnemonic, command, tip and state from
     * it, fires it on pressing, and follows it when it changes.
     *
     * <p>Tying another one lets the previous go: it stops listening to it and firing it.
     */
    public void setAction(Action a) {
        Action old = getAction();
        if (action == null || !action.equals(a)) {
            action = a;
            if (old != null) {
                removeActionListener(old);
                old.removePropertyChangeListener(actionPropertyChangeListener);
                actionPropertyChangeListener = null;
            }
            configurePropertiesFromAction(action);
            if (action != null) {
                if (!isListener(ActionListener.class, action)) {
                    addActionListener(action);
                }
                actionPropertyChangeListener = createActionPropertyChangeListener(action);
                action.addPropertyChangeListener(actionPropertyChangeListener);
            }
            firePropertyChange("action", old, action);
        }
    }

    private boolean isListener(Class<?> clazz, ActionListener a) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == clazz && listeners[i + 1] == a) {
                return true;
            }
        }
        return false;
    }

    public Action getAction() {
        return action;
    }

    /**
     * It takes everything from the action; {@code null} leaves the button as though it had never
     * had one.
     */
    protected void configurePropertiesFromAction(Action a) {
        mnemonicFromAction(a);
        textFromAction(a, false);
        setToolTipText(a != null ? (String) a.getValue(Action.SHORT_DESCRIPTION) : null);
        setIconFromAction(a);
        commandFromAction(a);
        setEnabled(a != null ? a.isEnabled() : true);
        if (hasSelectedKey(a) && shouldUpdateSelectedStateFromAction()) {
            selectedFromAction(a);
        }
        mnemonicIndexFromAction(a, false);
    }

    /**
     * Whether the selection state follows the action: no in an ordinary button, yes in one with
     * state. {@code JToggleButton} redefines it.
     */
    boolean shouldUpdateSelectedStateFromAction() {
        return false;
    }

    /**
     * A property of the action changed: the one that changed is copied.
     *
     * <p>{@link #createActionPropertyChangeListener}'s listener calls it; redefining it is the way
     * of following properties of an action's own.
     */
    protected void actionPropertyChanged(Action action, String propertyName) {
        if (Action.NAME.equals(propertyName)) {
            textFromAction(action, true);
        } else if ("enabled".equals(propertyName)) {
            setEnabled(action != null ? action.isEnabled() : true);
        } else if (Action.SHORT_DESCRIPTION.equals(propertyName)) {
            setToolTipText(action != null ? (String) action.getValue(Action.SHORT_DESCRIPTION)
                    : null);
        } else if (Action.SMALL_ICON.equals(propertyName)) {
            smallIconChanged(action);
        } else if (Action.MNEMONIC_KEY.equals(propertyName)) {
            mnemonicFromAction(action);
        } else if (Action.ACTION_COMMAND_KEY.equals(propertyName)) {
            commandFromAction(action);
        } else if (Action.SELECTED_KEY.equals(propertyName)
                && hasSelectedKey(action) && shouldUpdateSelectedStateFromAction()) {
            selectedFromAction(action);
        } else if (Action.DISPLAYED_MNEMONIC_INDEX_KEY.equals(propertyName)) {
            mnemonicIndexFromAction(action, true);
        } else if (Action.LARGE_ICON_KEY.equals(propertyName)) {
            largeIconChanged(action);
        }
    }

    private void textFromAction(Action a, boolean propertyChange) {
        boolean hide = getHideActionText();
        if (!propertyChange) {
            setText((a != null && !hide) ? (String) a.getValue(Action.NAME) : null);
        } else if (!hide) {
            setText((String) a.getValue(Action.NAME));
        }
    }

    /** The large icon if it is there, otherwise the small one, otherwise none. */
    void setIconFromAction(Action a) {
        Icon icon = null;
        if (a != null) {
            icon = (Icon) a.getValue(Action.LARGE_ICON_KEY);
            if (icon == null) {
                icon = (Icon) a.getValue(Action.SMALL_ICON);
            }
        }
        setIcon(icon);
    }

    /** The small icon changed: it only matters if there is no large one. */
    void smallIconChanged(Action a) {
        if (a.getValue(Action.LARGE_ICON_KEY) == null) {
            setIconFromAction(a);
        }
    }

    void largeIconChanged(Action a) {
        setIconFromAction(a);
    }

    private void commandFromAction(Action a) {
        setActionCommand(a != null ? (String) a.getValue(Action.ACTION_COMMAND_KEY) : null);
    }

    private void mnemonicFromAction(Action a) {
        Integer n = (a == null) ? null : (Integer) a.getValue(Action.MNEMONIC_KEY);
        setMnemonic(n == null ? '\0' : n.intValue());
    }

    private static boolean hasSelectedKey(Action a) {
        return a != null && a.getValue(Action.SELECTED_KEY) != null;
    }

    private void selectedFromAction(Action a) {
        boolean selected = false;
        if (a != null) {
            selected = Boolean.TRUE.equals(a.getValue(Action.SELECTED_KEY));
        }
        if (selected != isSelected()) {
            setSelected(selected);
        }
    }

    private void mnemonicIndexFromAction(Action a, boolean fromPropertyChange) {
        Integer value = (a == null) ? null
                : (Integer) a.getValue(Action.DISPLAYED_MNEMONIC_INDEX_KEY);
        if (fromPropertyChange || value != null) {
            int index = (value == null) ? -1 : value.intValue();
            if (index == -1) {
                updateMnemonicIndex(getText(), getMnemonic());
            } else {
                try {
                    setDisplayedMnemonicIndex(index);
                } catch (IllegalArgumentException iae) {
                }
            }
        }
    }

    /** The listener that follows the action; it tells {@link #actionPropertyChanged}. */
    protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
        return new ActionListenerImpl(this, a);
    }

    /** It follows the action on behalf of a button; named and not anonymous (#499). */
    private static class ActionListenerImpl implements PropertyChangeListener {
        private final AbstractButton button;
        private final Action action;

        ActionListenerImpl(AbstractButton button, Action action) {
            this.button = button;
            this.action = action;
        }

        public void propertyChange(PropertyChangeEvent e) {
            button.actionPropertyChanged(action, e.getPropertyName());
        }
    }

    // -- border, focus, fill, rollover -----------------------------------------------------------

    public boolean isBorderPainted() {
        return paintBorder;
    }

    public void setBorderPainted(boolean b) {
        boolean old = paintBorder;
        paintBorder = b;
        borderPaintedSet = true;
        firePropertyChange(BORDER_PAINTED_CHANGED_PROPERTY, old, paintBorder);
        if (b != old) {
            revalidate();
            repaint();
        }
    }

    /** It paints the border only if {@link #isBorderPainted}; the insets count all the same. */
    protected void paintBorder(Graphics g) {
        if (isBorderPainted()) {
            super.paintBorder(g);
        }
    }

    public boolean isFocusPainted() {
        return paintFocus;
    }

    public void setFocusPainted(boolean b) {
        boolean old = paintFocus;
        paintFocus = b;
        firePropertyChange(FOCUS_PAINTED_CHANGED_PROPERTY, old, paintFocus);
        if (b != old && isFocusOwner()) {
            revalidate();
            repaint();
        }
    }

    public boolean isContentAreaFilled() {
        return contentAreaFilled;
    }

    /**
     * Whether the look and feel fills the background; switching it off is how a transparent button
     * is made.
     */
    public void setContentAreaFilled(boolean b) {
        boolean old = contentAreaFilled;
        contentAreaFilled = b;
        contentAreaFilledSet = true;
        firePropertyChange(CONTENT_AREA_FILLED_CHANGED_PROPERTY, old, contentAreaFilled);
        if (b != old) {
            repaint();
        }
    }

    public boolean isRolloverEnabled() {
        return rolloverEnabled;
    }

    public void setRolloverEnabled(boolean b) {
        boolean old = rolloverEnabled;
        rolloverEnabled = b;
        rolloverEnabledSet = true;
        firePropertyChange(ROLLOVER_ENABLED_CHANGED_PROPERTY, old, rolloverEnabled);
        if (b != old) {
            repaint();
        }
    }

    // -- mnemonic --------------------------------------------------------------------------------

    public int getMnemonic() {
        return mnemonic;
    }

    /**
     * The mnemonic as a {@code KeyEvent} virtual key; it goes to the model and comes back from
     * there.
     */
    public void setMnemonic(int mnemonic) {
        model.setMnemonic(mnemonic);
        updateMnemonic();
    }

    /**
     * The mnemonic as a character; a lower-case one is turned into upper case, which is the key.
     */
    public void setMnemonic(char mnemonic) {
        int vk = (int) mnemonic;
        if (vk >= 'a' && vk <= 'z') {
            vk = vk - ('a' - 'A');
        }
        setMnemonic(vk);
    }

    /**
     * Which character of the text to underline; {@code -1}, none.
     *
     * <p>By default it is the mnemonic's first appearance; setting it by hand serves when the
     * letter appears several times and the one that is underlined must be another.
     */
    public void setDisplayedMnemonicIndex(int index) throws IllegalArgumentException {
        int old = mnemonicIndex;
        if (index == -1) {
            mnemonicIndex = -1;
        } else {
            String t = getText();
            int length = (t == null) ? 0 : t.length();
            if (index < -1 || index >= length) {
                throw new IllegalArgumentException("index == " + index);
            }
        }
        mnemonicIndex = index;
        firePropertyChange("displayedMnemonicIndex", old, index);
        if (index != old) {
            revalidate();
            repaint();
        }
    }

    public int getDisplayedMnemonicIndex() {
        return mnemonicIndex;
    }

    private void updateMnemonicIndex(String text, int mnemonic) {
        setDisplayedMnemonicIndex(SwingUtilities.findDisplayedMnemonicIndex(text, mnemonic));
    }

    /** The model changed mnemonic: the button copies it and gives notice. */
    private void updateMnemonic() {
        int newValue = model.getMnemonic();
        if (mnemonic != newValue) {
            int old = mnemonic;
            mnemonic = newValue;
            firePropertyChange(MNEMONIC_CHANGED_PROPERTY, old, mnemonic);
            updateMnemonicIndex(getText(), mnemonic);
            revalidate();
            repaint();
        }
    }

    /**
     * The milliseconds between two presses for the second to count separately; zero, they all
     * count.
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

    // -- model -----------------------------------------------------------------------------------

    public ButtonModel getModel() {
        return model;
    }

    /**
     * It changes the model: it stops listening to the old one, listens to the new one and copies
     * its state.
     *
     * <p>Enabled and mnemonic are copied from the model to the button, because the model is the
     * truth.
     */
    public void setModel(ButtonModel newModel) {
        ButtonModel old = getModel();
        if (old != null) {
            old.removeChangeListener(changeListener);
            old.removeActionListener(actionListener);
            old.removeItemListener(itemListener);
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
            updateMnemonic();
            setEnabled(newModel.isEnabled());
        } else {
            mnemonic = '\0';
        }
        updateMnemonicIndex(getText(), mnemonic);
        firePropertyChange(MODEL_CHANGED_PROPERTY, old, newModel);
        if (newModel != old) {
            revalidate();
            repaint();
        }
    }

    // -- look and feel ---------------------------------------------------------------------------

    public ButtonUI getUI() {
        return (ButtonUI) ui;
    }

    public void setUI(ButtonUI ui) {
        super.setUI(ui);
    }

    /** Nothing: each concrete button knows which look and feel to install. */
    public void updateUI() {
    }

    /** See the class note about {@code OverlayLayout}. */
    protected void addImpl(Component comp, Object constraints, int index) {
        super.addImpl(comp, constraints, index);
    }

    public void setLayout(LayoutManager mgr) {
        setLayout = true;
        super.setLayout(mgr);
    }

    // -- the button's listeners ------------------------------------------------------------------

    public void addChangeListener(ChangeListener l) {
        listenerList.add(ChangeListener.class, l);
    }

    public void removeChangeListener(ChangeListener l) {
        listenerList.remove(ChangeListener.class, l);
    }

    public ChangeListener[] getChangeListeners() {
        return listenerList.getListeners(ChangeListener.class);
    }

    /** It forwards the model's change with the button as the source. */
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
        return handler();
    }

    /**
     * It forwards the model's action with the button as the source and the button's command.
     *
     * <p>The new event is built once only, and only if there is somebody to listen to it.
     */
    protected void fireActionPerformed(ActionEvent event) {
        Object[] listeners = listenerList.getListenerList();
        ActionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ActionListener.class) {
                if (e == null) {
                    String command = event.getActionCommand();
                    if (command == null) {
                        command = getActionCommand();
                    }
                    e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, command,
                            event.getWhen(), event.getModifiers());
                }
                ((ActionListener) listeners[i + 1]).actionPerformed(e);
            }
        }
    }

    protected void fireItemStateChanged(ItemEvent event) {
        Object[] listeners = listenerList.getListenerList();
        ItemEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ItemListener.class) {
                if (e == null) {
                    e = new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED, this,
                            event.getStateChange());
                }
                ((ItemListener) listeners[i + 1]).itemStateChanged(e);
            }
        }
    }

    protected ActionListener createActionListener() {
        return handler();
    }

    protected ItemListener createItemListener() {
        return handler();
    }

    private Handler handler() {
        if (handler == null) {
            handler = new Handler(this);
        }
        return handler;
    }

    /** A change of the model: the button copies mnemonic and enabled, and gives notice. */
    void modelChange() {
        updateMnemonic();
        if (isEnabled() != model.isEnabled()) {
            setEnabled(model.isEnabled());
        }
        fireStateChanged();
        repaint();
    }

    /**
     * An item change of the model: it is forwarded, and if the action has state, it is copied to
     * it.
     */
    void modelItem(ItemEvent event) {
        fireItemStateChanged(event);
        if (shouldUpdateSelectedStateFromAction()) {
            Action a = getAction();
            if (a != null && hasSelectedKey(a)) {
                boolean selected = isSelected();
                boolean inAction = Boolean.TRUE.equals(a.getValue(Action.SELECTED_KEY));
                if (inAction != selected) {
                    a.putValue(Action.SELECTED_KEY, Boolean.valueOf(selected));
                }
            }
        }
    }

    /** The model's three listeners in one object; named and not anonymous (#499). */
    private static class Handler implements ActionListener, ChangeListener, ItemListener,
            Serializable {
        private final AbstractButton button;

        Handler(AbstractButton button) {
            this.button = button;
        }

        public void stateChanged(ChangeEvent e) {
            button.modelChange();
        }

        public void actionPerformed(ActionEvent e) {
            button.fireActionPerformed(e);
        }

        public void itemStateChanged(ItemEvent e) {
            button.modelItem(e);
        }
    }

    /**
     * The model's change listener, as a class; the JDK keeps it for compatibility and
     * {@link #createChangeListener} no longer uses it.
     */
    protected class ButtonChangeListener implements ChangeListener, Serializable {
        ButtonChangeListener() {
        }

        public void stateChanged(ChangeEvent e) {
            modelChange();
        }
    }

    /** It enables or disables button and model; disabling switches the rollover off. */
    public void setEnabled(boolean b) {
        if (!b && model.isRollover()) {
            model.setRollover(false);
        }
        super.setEnabled(b);
        model.setEnabled(b);
    }

    /** @deprecated it is {@link #getText}. */
    @Deprecated
    public String getLabel() {
        return getText();
    }

    /** @deprecated it is {@link #setText}. */
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

    /** The text, in an array of one, if it is selected; {@code null} if not. */
    public Object[] getSelectedObjects() {
        if (!isSelected()) {
            return null;
        }
        Object[] selected = new Object[1];
        selected[0] = getText();
        return selected;
    }

    /**
     * What each constructor does: text, icon, look and feel, and aligned to the left and to the
     * centre.
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
     * More than one image arrived: it repaints if there is an icon in sight.
     *
     * <p>The JDK also checks that the image is that of the icon that is shown, which it can only
     * know with {@code ImageIcon}. Without it, any image that arrives while there is an icon
     * repaints; too much, never too little.
     */
    public boolean imageUpdate(Image img, int infoflags, int x, int y, int w, int h) {
        Icon visible = null;
        if (!model.isEnabled()) {
            if (model.isSelected()) {
                visible = getDisabledSelectedIcon();
            } else {
                visible = getDisabledIcon();
            }
        } else if (model.isPressed() && model.isArmed()) {
            visible = getPressedIcon();
        } else if (isRolloverEnabled() && model.isRollover()) {
            if (model.isSelected()) {
                visible = getRolloverSelectedIcon();
            } else {
                visible = getRolloverIcon();
            }
        } else if (model.isSelected()) {
            visible = getSelectedIcon();
        }
        if (visible == null) {
            visible = getIcon();
        }
        if (visible == null) {
            return false;
        }
        return super.imageUpdate(img, infoflags, x, y, w, h);
    }

    /** See the class note; the four of its own, and the rest to {@code JComponent}. */
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
