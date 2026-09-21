package javax.swing;

import java.awt.ItemSelectable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComboBoxUI;
import javax.swing.plaf.ComponentUI;

/**
 * A combo box, with or without a field for typing.
 *
 * <h2>Two notices for each choice</h2>
 *
 * <p>Choosing an element fires an {@link ItemEvent} and also an {@link ActionEvent}. It is not
 * redundant: the first arrives twice, once for what stopped being chosen and another for the
 * new one, and it serves in order to know what changed; the second arrives once and means "the
 * user chose". For almost everything one wants it is the second.
 *
 * <h2>Editable or not</h2>
 *
 * <p>With {@link #setEditable} the combo box carries a text field in front and the user may
 * type something that is not in the list. That is why {@link #getSelectedItem} returns
 * {@code Object} and not the elements' type; see {@link ComboBoxModel}'s note.
 *
 * <h2>Why it listens to its own model</h2>
 *
 * <p>The class implements {@link ListDataListener}. It is what makes adding an element to the
 * model from outside update the combo box, and removing the chosen one not leave it showing
 * something that is no longer there.
 *
 * @param <E> the elements' type.
 */
public class JComboBox<E> extends JComponent implements ItemSelectable, ListDataListener,
        ActionListener, Accessible {

    private static final String uiClassID = "ComboBoxUI";

    /** The data model. */
    protected ComboBoxModel<E> dataModel;

    /** Who draws each line of the drop-down. */
    protected ListCellRenderer<? super E> renderer;

    /** The text field, when the combo box is editable. */
    protected ComboBoxEditor editor;

    /** How many lines are seen before the drop-down scrolls. */
    protected int maximumRowCount = 8;

    /** Whether typing is possible. */
    protected boolean isEditable = false;

    /** Who decides which line to jump to on typing a letter. */
    protected KeySelectionManager keySelectionManager = null;

    /** The name the action events carry. */
    protected String actionCommand = "comboBoxChanged";

    /** Whether the drop-down is drawn inside the window or in one of its own. */
    protected boolean lightWeightPopupEnabled = true;

    /**
     * What was chosen the last time notice was given.
     *
     * <p>It is kept in order to be able to say <em>what</em> stopped being chosen: the model only
     * knows what is chosen now.
     */
    protected Object selectedItemReminder = null;

    private E prototypeDisplayValue;
    private boolean firingActionEvent = false;
    private boolean selectingItem = false;
    private Action action;
    private AccessibleContext accessibleContext;

    /** A combo box over that model. */
    public JComboBox(ComboBoxModel<E> aModel) {
        super();
        setModel(aModel);
        init();
    }

    /** A combo box with those elements. */
    public JComboBox(E[] items) {
        super();
        setModel(new DefaultComboBoxModel<E>(items));
        init();
    }

    /** A combo box with that vector's elements. */
    public JComboBox(Vector<E> items) {
        super();
        setModel(new DefaultComboBoxModel<E>(items));
        init();
    }

    /** An empty combo box, over a model that can be modified. */
    public JComboBox() {
        super();
        setModel(new DefaultComboBoxModel<E>());
        init();
    }

    private void init() {
        installAncestorListener();
        setOpaque(true);
        updateUI();
    }

    /**
     * It hooks itself up in order to close the drop-down if the window moves.
     *
     * <p>Without that, moving the window with the drop-down open would leave it floating in its
     * old place.
     */
    protected void installAncestorListener() {
    }

    public void setUI(ComboBoxUI ui) {
        super.setUI(ui);
    }

    public void updateUI() {
    }

    public String getUIClassID() {
        return uiClassID;
    }

    public ComboBoxUI getUI() {
        return (ComboBoxUI) ui;
    }

    /**
     * It changes the model.
     *
     * <p>It unhooks itself from the old one and hooks itself to the new one; see the class note.
     */
    public void setModel(ComboBoxModel<E> aModel) {
        ComboBoxModel<E> oldModel = dataModel;
        if (oldModel != null) {
            oldModel.removeListDataListener(this);
        }
        dataModel = aModel;
        if (dataModel != null) {
            dataModel.addListDataListener(this);
        }
        selectedItemReminder = (dataModel == null) ? null : dataModel.getSelectedItem();
        firePropertyChange("model", oldModel, dataModel);
    }

    public ComboBoxModel<E> getModel() {
        return dataModel;
    }

    /** Whether the drop-down is drawn inside the window. */
    public void setLightWeightPopupEnabled(boolean aFlag) {
        boolean oldFlag = lightWeightPopupEnabled;
        lightWeightPopupEnabled = aFlag;
        firePropertyChange("lightWeightPopupEnabled", oldFlag, lightWeightPopupEnabled);
    }

    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }

    /** Whether the user may type a value that is not in the list. */
    public void setEditable(boolean aFlag) {
        boolean oldFlag = isEditable;
        isEditable = aFlag;
        firePropertyChange("editable", oldFlag, isEditable);
    }

    public boolean isEditable() {
        return isEditable;
    }

    /**
     * How many lines are seen in the drop-down.
     *
     * @throws IllegalArgumentException if it is not positive.
     */
    public void setMaximumRowCount(int count) {
        int oldCount = maximumRowCount;
        maximumRowCount = count;
        firePropertyChange("maximumRowCount", oldCount, maximumRowCount);
    }

    public int getMaximumRowCount() {
        return maximumRowCount;
    }

    public void setRenderer(ListCellRenderer<? super E> aRenderer) {
        ListCellRenderer<? super E> oldRenderer = renderer;
        renderer = aRenderer;
        firePropertyChange("renderer", oldRenderer, renderer);
        invalidate();
    }

    public ListCellRenderer<? super E> getRenderer() {
        return renderer;
    }

    /** The editable combo box's text field. */
    public void setEditor(ComboBoxEditor anEditor) {
        ComboBoxEditor oldEditor = editor;
        if (editor != null) {
            editor.removeActionListener(this);
        }
        editor = anEditor;
        if (editor != null) {
            editor.addActionListener(this);
        }
        firePropertyChange("editor", oldEditor, editor);
    }

    public ComboBoxEditor getEditor() {
        return editor;
    }

    /** It chooses that element; it may not be in the list if it is editable. */
    public void setSelectedItem(Object anObject) {
        Object oldSelection = selectedItemReminder;
        Object objectToSelect = anObject;
        if (oldSelection == null || !oldSelection.equals(anObject)) {
            if (anObject != null && !isEditable()) {
                // With no text field, only something that is in the list can be chosen.
                boolean found = false;
                for (int i = 0; i < dataModel.getSize(); i++) {
                    E element = dataModel.getElementAt(i);
                    if (anObject.equals(element)) {
                        found = true;
                        objectToSelect = element;
                        break;
                    }
                }
                if (!found) {
                    return;
                }
            }
            selectingItem = true;
            dataModel.setSelectedItem(objectToSelect);
            selectingItem = false;
            if (selectedItemReminder != dataModel.getSelectedItem()) {
                selectedItemChanged();
            }
        }
        fireActionEvent();
    }

    public Object getSelectedItem() {
        return dataModel.getSelectedItem();
    }

    /**
     * It chooses line number such-and-such; with -1 none is left.
     *
     * @throws IllegalArgumentException if the index does not exist.
     */
    public void setSelectedIndex(int anIndex) {
        int size = dataModel.getSize();
        if (anIndex == -1) {
            setSelectedItem(null);
        } else if (anIndex < -1 || anIndex >= size) {
            throw new IllegalArgumentException("setSelectedIndex: " + anIndex
                    + " out of bounds");
        } else {
            setSelectedItem(dataModel.getElementAt(anIndex));
        }
    }

    /** Which line what is chosen is in, or -1 if what is chosen is not in the list. */
    public int getSelectedIndex() {
        Object sObject = dataModel.getSelectedItem();
        if (sObject != null) {
            for (int i = 0; i < dataModel.getSize(); i++) {
                E obj = dataModel.getElementAt(i);
                if (obj != null && obj.equals(sObject)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /** A sample value the width is deduced from, without walking through them all. */
    public E getPrototypeDisplayValue() {
        return prototypeDisplayValue;
    }

    public void setPrototypeDisplayValue(E prototypeDisplayValue) {
        Object oldValue = this.prototypeDisplayValue;
        this.prototypeDisplayValue = prototypeDisplayValue;
        firePropertyChange("prototypeDisplayValue", oldValue, prototypeDisplayValue);
    }

    /**
     * It adds an element at the end.
     *
     * @throws RuntimeException if the model cannot be modified.
     */
    public void addItem(E item) {
        checkMutableComboBoxModel();
        ((MutableComboBoxModel<E>) dataModel).addElement(item);
    }

    public void insertItemAt(E item, int index) {
        checkMutableComboBoxModel();
        ((MutableComboBoxModel<E>) dataModel).insertElementAt(item, index);
    }

    public void removeItem(Object anObject) {
        checkMutableComboBoxModel();
        ((MutableComboBoxModel<E>) dataModel).removeElement(anObject);
    }

    public void removeItemAt(int anIndex) {
        checkMutableComboBoxModel();
        ((MutableComboBoxModel<E>) dataModel).removeElementAt(anIndex);
    }

    /** It empties the list. */
    public void removeAllItems() {
        checkMutableComboBoxModel();
        MutableComboBoxModel<E> model = (MutableComboBoxModel<E>) dataModel;
        int size = model.getSize();
        if (model instanceof DefaultComboBoxModel) {
            ((DefaultComboBoxModel<E>) model).removeAllElements();
        } else {
            for (int i = 0; i < size; i++) {
                model.removeElementAt(0);
            }
        }
        selectedItemReminder = null;
    }

    private void checkMutableComboBoxModel() {
        if (!(dataModel instanceof MutableComboBoxModel)) {
            throw new RuntimeException("Cannot use this method with a non-Mutable data model.");
        }
    }

    public void showPopup() {
        setPopupVisible(true);
    }

    public void hidePopup() {
        setPopupVisible(false);
    }

    /** It opens or closes the drop-down; the look and feel does it. */
    public void setPopupVisible(boolean v) {
        getUI().setPopupVisible(this, v);
    }

    public boolean isPopupVisible() {
        return getUI().isPopupVisible(this);
    }

    public void addItemListener(ItemListener aListener) {
        listenerList.add(ItemListener.class, aListener);
    }

    public void removeItemListener(ItemListener aListener) {
        listenerList.remove(ItemListener.class, aListener);
    }

    public ItemListener[] getItemListeners() {
        return listenerList.getListeners(ItemListener.class);
    }

    public void addActionListener(ActionListener l) {
        listenerList.add(ActionListener.class, l);
    }

    public void removeActionListener(ActionListener l) {
        listenerList.remove(ActionListener.class, l);
    }

    public ActionListener[] getActionListeners() {
        return listenerList.getListeners(ActionListener.class);
    }

    public void addPopupMenuListener(PopupMenuListener l) {
        listenerList.add(PopupMenuListener.class, l);
    }

    public void removePopupMenuListener(PopupMenuListener l) {
        listenerList.remove(PopupMenuListener.class, l);
    }

    public PopupMenuListener[] getPopupMenuListeners() {
        return listenerList.getListeners(PopupMenuListener.class);
    }

    /** It gives notice that the drop-down is about to open; the look and feel calls it. */
    public void firePopupMenuWillBecomeVisible() {
        Object[] listeners = listenerList.getListenerList();
        javax.swing.event.PopupMenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == PopupMenuListener.class) {
                if (e == null) {
                    e = new javax.swing.event.PopupMenuEvent(this);
                }
                ((PopupMenuListener) listeners[i + 1]).popupMenuWillBecomeVisible(e);
            }
        }
    }

    public void firePopupMenuWillBecomeInvisible() {
        Object[] listeners = listenerList.getListenerList();
        javax.swing.event.PopupMenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == PopupMenuListener.class) {
                if (e == null) {
                    e = new javax.swing.event.PopupMenuEvent(this);
                }
                ((PopupMenuListener) listeners[i + 1]).popupMenuWillBecomeInvisible(e);
            }
        }
    }

    public void firePopupMenuCanceled() {
        Object[] listeners = listenerList.getListenerList();
        javax.swing.event.PopupMenuEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == PopupMenuListener.class) {
                if (e == null) {
                    e = new javax.swing.event.PopupMenuEvent(this);
                }
                ((PopupMenuListener) listeners[i + 1]).popupMenuCanceled(e);
            }
        }
    }

    public void setActionCommand(String aCommand) {
        actionCommand = aCommand;
    }

    public String getActionCommand() {
        return actionCommand;
    }

    /** It ties the combo box to an action, which is fired on choosing. */
    public void setAction(Action a) {
        Action oldValue = getAction();
        if (action == null || !action.equals(a)) {
            action = a;
            if (oldValue != null) {
                removeActionListener(oldValue);
            }
            configurePropertiesFromAction(action);
            if (action != null) {
                addActionListener(action);
            }
            firePropertyChange("action", oldValue, action);
        }
    }

    public Action getAction() {
        return action;
    }

    protected void configurePropertiesFromAction(Action a) {
        if (a != null) {
            setEnabled(a.isEnabled());
        }
    }

    protected PropertyChangeListener createActionPropertyChangeListener(Action a) {
        return null;
    }

    protected void actionPropertyChanged(Action action, String propertyName) {
    }

    protected void fireItemStateChanged(ItemEvent e) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ItemListener.class) {
                ((ItemListener) listeners[i + 1]).itemStateChanged(e);
            }
        }
    }

    /**
     * It gives notice that the user chose.
     *
     * <p>It guards itself against coming back in: a listener that changed what is chosen would
     * fire another notice from inside this one, and the pair of notices would end up crossed.
     */
    protected void fireActionEvent() {
        if (!firingActionEvent) {
            firingActionEvent = true;
            ActionEvent e = null;
            Object[] listeners = listenerList.getListenerList();
            try {
                for (int i = listeners.length - 2; i >= 0; i = i - 2) {
                    if (listeners[i] == ActionListener.class) {
                        if (e == null) {
                            e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED,
                                    getActionCommand());
                        }
                        ((ActionListener) listeners[i + 1]).actionPerformed(e);
                    }
                }
            } finally {
                firingActionEvent = false;
            }
        }
    }

    /** It sends the two item notices: what stopped being chosen and the new one. */
    protected void selectedItemChanged() {
        if (selectedItemReminder != null) {
            fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,
                    selectedItemReminder, ItemEvent.DESELECTED));
        }
        selectedItemReminder = dataModel.getSelectedItem();
        if (selectedItemReminder != null) {
            fireItemStateChanged(new ItemEvent(this, ItemEvent.ITEM_STATE_CHANGED,
                    selectedItemReminder, ItemEvent.SELECTED));
        }
    }

    /** What is chosen, as an array of one; it is what {@link ItemSelectable} asks for. */
    public Object[] getSelectedObjects() {
        Object selectedObject = getSelectedItem();
        if (selectedObject == null) {
            return new Object[0];
        }
        Object[] result = new Object[1];
        result[0] = selectedObject;
        return result;
    }

    /** The text field calls it when the user presses Enter. */
    public void actionPerformed(ActionEvent e) {
        ComboBoxEditor editor = getEditor();
        if (editor != null) {
            Object newItem = editor.getItem();
            setPopupVisible(false);
            getModel().setSelectedItem(newItem);
            ActionEvent newEvent = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "comboBoxEdited");
            fireActionEvent();
        }
    }

    /** The model changed what is chosen. */
    public void contentsChanged(ListDataEvent e) {
        Object oldSelection = selectedItemReminder;
        Object newSelection = dataModel.getSelectedItem();
        if (oldSelection == null || !oldSelection.equals(newSelection)) {
            selectedItemChanged();
            if (!selectingItem) {
                fireActionEvent();
            }
        }
    }

    public void intervalAdded(ListDataEvent e) {
        if (selectedItemReminder != dataModel.getSelectedItem()) {
            selectedItemChanged();
        }
    }

    public void intervalRemoved(ListDataEvent e) {
        contentsChanged(e);
    }

    /** It jumps to the line that starts with that letter. */
    public boolean selectWithKeyChar(char keyChar) {
        int index;
        if (keySelectionManager == null) {
            keySelectionManager = createDefaultKeySelectionManager();
        }
        index = keySelectionManager.selectionForKey(keyChar, getModel());
        if (index != -1) {
            setSelectedIndex(index);
            return true;
        }
        return false;
    }

    public void setEnabled(boolean b) {
        super.setEnabled(b);
        firePropertyChange("enabled", !isEnabled(), isEnabled());
    }

    /** It gets the text field ready with that value. */
    public void configureEditor(ComboBoxEditor anEditor, Object anItem) {
        anEditor.setItem(anItem);
    }

    public void processKeyEvent(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            hidePopup();
        }
        super.processKeyEvent(e);
    }

    protected boolean processKeyBinding(KeyStroke ks, KeyEvent e, int condition,
            boolean pressed) {
        return super.processKeyBinding(ks, e, condition, pressed);
    }

    public void setKeySelectionManager(KeySelectionManager aManager) {
        keySelectionManager = aManager;
    }

    public KeySelectionManager getKeySelectionManager() {
        return keySelectionManager;
    }

    public int getItemCount() {
        return dataModel.getSize();
    }

    public E getItemAt(int index) {
        return dataModel.getElementAt(index);
    }

    protected KeySelectionManager createDefaultKeySelectionManager() {
        return new DefaultKeySelectionManager();
    }

    protected String paramString() {
        return super.paramString();
    }

    public AccessibleContext getAccessibleContext() {
        return accessibleContext;
    }

    /**
     * It decides which line to jump to when the user types a letter.
     *
     * <p>It is an interface and not a fixed rule because the answer depends on the language and on
     * the content: in a list of surnames it is best to jump by the first letter, and in one of
     * codes perhaps not.
     */
    public interface KeySelectionManager {

        /** The line to jump to, or -1 if none. */
        int selectionForKey(char aKey, ComboBoxModel<?> aModel);
    }

    /**
     * The usual rule: the next line that begins with that letter.
     *
     * <p>It starts from the one after the chosen one and wraps round. It is what makes pressing
     * the same letter several times walk through all those that begin with it instead of staying
     * at the first.
     */
    class DefaultKeySelectionManager implements KeySelectionManager, java.io.Serializable {

        public int selectionForKey(char aKey, ComboBoxModel<?> aModel) {
            int i;
            int c;
            int currentSelection = -1;
            Object selectedItem = aModel.getSelectedItem();
            String v;
            String pattern;

            if (selectedItem != null) {
                int n = aModel.getSize();
                for (i = 0; i < n; i++) {
                    if (selectedItem == aModel.getElementAt(i)) {
                        currentSelection = i;
                        break;
                    }
                }
            }

            pattern = ("" + aKey).toLowerCase(java.util.Locale.ROOT);
            aKey = pattern.charAt(0);

            int n = aModel.getSize();
            for (i = currentSelection + 1; i < n; i++) {
                Object elem = aModel.getElementAt(i);
                if (elem != null && elem.toString() != null) {
                    v = elem.toString().toLowerCase(java.util.Locale.ROOT);
                    if (v.length() > 0 && v.charAt(0) == aKey) {
                        return i;
                    }
                }
            }
            for (i = 0; i < currentSelection; i++) {
                Object elem = aModel.getElementAt(i);
                if (elem != null && elem.toString() != null) {
                    v = elem.toString().toLowerCase(java.util.Locale.ROOT);
                    if (v.length() > 0 && v.charAt(0) == aKey) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }
}
