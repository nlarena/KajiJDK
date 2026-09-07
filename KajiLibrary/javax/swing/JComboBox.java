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
 * Una lista desplegable, con o sin campo para escribir.
 *
 * <h2>Dos avisos por cada eleccion</h2>
 *
 * <p>Elegir un elemento dispara un {@link ItemEvent} y ademas un {@link ActionEvent}. No es
 * redundante: el primero llega dos veces, una por lo que se dejo de elegir y otra por lo nuevo, y
 * sirve para saber que cambio; el segundo llega una vez y significa "el usuario eligio". Para casi
 * todo lo que se quiere es el segundo.
 *
 * <h2>Editable o no</h2>
 *
 * <p>Con {@link #setEditable} la lista lleva adelante un campo de texto y el usuario puede escribir
 * algo que no esta en la lista. Por eso {@link #getSelectedItem} devuelve {@code Object} y no el
 * tipo de los elementos; ver la nota de {@link ComboBoxModel}.
 *
 * <h2>Por que escucha a su propio modelo</h2>
 *
 * <p>La clase implementa {@link ListDataListener}. Es lo que hace que agregar un elemento al modelo
 * por afuera actualice la lista, y que sacar el elegido no la deje mostrando algo que ya no esta.
 *
 * @param <E> el tipo de los elementos.
 */
public class JComboBox<E> extends JComponent implements ItemSelectable, ListDataListener,
        ActionListener, Accessible {

    private static final String uiClassID = "ComboBoxUI";

    /** El modelo de datos. */
    protected ComboBoxModel<E> dataModel;

    /** Quien dibuja cada renglon del desplegable. */
    protected ListCellRenderer<? super E> renderer;

    /** El campo de texto, cuando la lista es editable. */
    protected ComboBoxEditor editor;

    /** Cuantos renglones se ven antes de que el desplegable se desplace. */
    protected int maximumRowCount = 8;

    /** Si se puede escribir. */
    protected boolean isEditable = false;

    /** Quien decide a que renglon saltar al escribir una letra. */
    protected KeySelectionManager keySelectionManager = null;

    /** El nombre que llevan los eventos de accion. */
    protected String actionCommand = "comboBoxChanged";

    /** Si el desplegable se dibuja adentro de la ventana o en una propia. */
    protected boolean lightWeightPopupEnabled = true;

    /**
     * Lo que estaba elegido la ultima vez que se aviso.
     *
     * <p>Se guarda para poder decir <em>que</em> se dejo de elegir: el modelo solo sabe lo que esta
     * elegido ahora.
     */
    protected Object selectedItemReminder = null;

    private E prototypeDisplayValue;
    private boolean firingActionEvent = false;
    private boolean selectingItem = false;
    private Action action;
    private AccessibleContext accessibleContext;

    /** Una lista sobre ese modelo. */
    public JComboBox(ComboBoxModel<E> aModel) {
        super();
        setModel(aModel);
        init();
    }

    /** Una lista con esos elementos. */
    public JComboBox(E[] items) {
        super();
        setModel(new DefaultComboBoxModel<E>(items));
        init();
    }

    /** Una lista con los elementos de ese vector. */
    public JComboBox(Vector<E> items) {
        super();
        setModel(new DefaultComboBoxModel<E>(items));
        init();
    }

    /** Una lista vacia, sobre un modelo que se puede modificar. */
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
     * Se engancha para cerrar el desplegable si la ventana se mueve.
     *
     * <p>Sin eso, mover la ventana con el desplegable abierto lo dejaria flotando en su lugar
     * viejo.
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
     * Cambia el modelo.
     *
     * <p>Se desengancha del viejo y se engancha al nuevo; ver la nota de la clase.
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

    /** Si el desplegable se dibuja adentro de la ventana. */
    public void setLightWeightPopupEnabled(boolean aFlag) {
        boolean oldFlag = lightWeightPopupEnabled;
        lightWeightPopupEnabled = aFlag;
        firePropertyChange("lightWeightPopupEnabled", oldFlag, lightWeightPopupEnabled);
    }

    public boolean isLightWeightPopupEnabled() {
        return lightWeightPopupEnabled;
    }

    /** Si el usuario puede escribir un valor que no este en la lista. */
    public void setEditable(boolean aFlag) {
        boolean oldFlag = isEditable;
        isEditable = aFlag;
        firePropertyChange("editable", oldFlag, isEditable);
    }

    public boolean isEditable() {
        return isEditable;
    }

    /**
     * Cuantos renglones se ven en el desplegable.
     *
     * @throws IllegalArgumentException si no es positivo.
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

    /** El campo de texto de la lista editable. */
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

    /** Elige ese elemento; puede no estar en la lista si es editable. */
    public void setSelectedItem(Object anObject) {
        Object oldSelection = selectedItemReminder;
        Object objectToSelect = anObject;
        if (oldSelection == null || !oldSelection.equals(anObject)) {
            if (anObject != null && !isEditable()) {
                // Sin campo de texto, solo se puede elegir algo que este en la lista.
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
     * Elige el renglon numero tal; con -1 no queda ninguno.
     *
     * @throws IllegalArgumentException si el indice no existe.
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

    /** En que renglon esta lo elegido, o -1 si lo elegido no esta en la lista. */
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

    /** Un valor de ejemplo del que se deduce el ancho, sin recorrer todos. */
    public E getPrototypeDisplayValue() {
        return prototypeDisplayValue;
    }

    public void setPrototypeDisplayValue(E prototypeDisplayValue) {
        Object oldValue = this.prototypeDisplayValue;
        this.prototypeDisplayValue = prototypeDisplayValue;
        firePropertyChange("prototypeDisplayValue", oldValue, prototypeDisplayValue);
    }

    /**
     * Agrega un elemento al final.
     *
     * @throws RuntimeException si el modelo no se puede modificar.
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

    /** Vacia la lista. */
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

    /** Abre o cierra el desplegable; lo hace el aspecto. */
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

    /** Avisa que el desplegable esta por abrirse; lo llama el aspecto. */
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

    /** Ata la lista a una accion, que se dispara al elegir. */
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
     * Avisa que el usuario eligio.
     *
     * <p>Se protege de volver a entrar: un oyente que cambie lo elegido dispararia otro aviso
     * desde adentro de este, y el par de avisos quedaria cruzado.
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

    /** Manda los dos avisos de elemento: lo que se dejo de elegir y lo nuevo. */
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

    /** Lo elegido, como arreglo de uno; es lo que pide {@link ItemSelectable}. */
    public Object[] getSelectedObjects() {
        Object selectedObject = getSelectedItem();
        if (selectedObject == null) {
            return new Object[0];
        }
        Object[] result = new Object[1];
        result[0] = selectedObject;
        return result;
    }

    /** Lo llama el campo de texto cuando el usuario aprieta Enter. */
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

    /** El modelo cambio lo elegido. */
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

    /** Salta al renglon que empieza con esa letra. */
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

    /** Prepara el campo de texto con ese valor. */
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
     * Decide a que renglon saltar cuando el usuario escribe una letra.
     *
     * <p>Es una interfaz y no una regla fija porque la respuesta depende del idioma y del contenido:
     * en una lista de apellidos conviene saltar por la primera letra, y en una de codigos tal vez
     * no.
     */
    public interface KeySelectionManager {

        /** El renglon al que saltar, o -1 si ninguno. */
        int selectionForKey(char aKey, ComboBoxModel<?> aModel);
    }

    /**
     * La regla de siempre: el proximo renglon que empiece con esa letra.
     *
     * <p>Arranca del que sigue al elegido y da la vuelta. Es lo que hace que apretar la misma letra
     * varias veces recorra todos los que empiezan con ella en lugar de quedarse en el primero.
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
