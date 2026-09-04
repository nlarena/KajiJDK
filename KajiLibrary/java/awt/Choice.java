package java.awt;

import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventListener;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;

/**
 * Una lista desplegable: muestra un renglón y despliega el resto al apretarla.
 *
 * <p>Siempre hay **exactamente uno** seleccionado mientras haya algo en la lista, y no se puede
 * volver a un estado sin selección. Agregar la primera entrada la selecciona sola.
 *
 * <p>Como en {@link Checkbox}, cambiar la selección por programa con {@link #select} no dispara
 * eventos: son de la interacción del usuario.
 */
public class Choice extends Component implements ItemSelectable, Accessible {

    private static final long serialVersionUID = -4075310674757313071L;

    private static int choiceCounter = 0;

    /** Las entradas. */
    Vector<String> pItems = new Vector<String>();

    /** Cuál está seleccionada, o -1 si la lista está vacía. */
    int selectedIndex = -1;

    /** Los oyentes, encadenados. */
    transient ItemListener itemListener;

    /** Una lista vacía. */
    public Choice() throws HeadlessException {
    }

    String constructComponentName() {
        synchronized (Choice.class) {
            String n = "choice" + choiceCounter;
            choiceCounter = choiceCounter + 1;
            return n;
        }
    }

    /** La declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /** Cuántas entradas tiene. */
    public int getItemCount() {
        return this.pItems.size();
    }

    /**
     * Cuántas entradas tiene.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getItemCount}.
     */
    @Deprecated
    public int countItems() {
        return this.getItemCount();
    }

    /**
     * La entrada de esa posición.
     *
     * @throws ArrayIndexOutOfBoundsException si la posición no existe
     */
    public String getItem(int index) {
        return this.getItemImpl(index);
    }

    final String getItemImpl(int index) {
        return this.pItems.elementAt(index);
    }

    /**
     * Agrega una entrada al final.
     *
     * <p>La primera queda seleccionada: una lista desplegable no puede estar sin selección.
     *
     * @throws NullPointerException si la entrada es `null`
     */
    public void add(String item) {
        synchronized (this) {
            this.agregar(item, this.pItems.size());
        }
    }

    /**
     * Agrega una entrada al final.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #add(String)}.
     */
    @Deprecated
    public void addItem(String item) {
        this.add(item);
    }

    /**
     * Inserta una entrada en esa posición.
     *
     * <p>Si la insertada cae en la posición de la seleccionada o antes, **la selección pasa a la
     * primera**. Suena arbitrario y lo es, pero es lo que hace AWT y cambiarlo sería mentir: el
     * llamador que insertó arriba de la selección se encuentra con la primera seleccionada, no con
     * la que tenía corrida un lugar.
     *
     * @throws IllegalArgumentException si la posición es negativa
     */
    public void insert(String item, int index) {
        synchronized (this) {
            if (index < 0) {
                throw new IllegalArgumentException("index less than zero.");
            }
            int i = Math.min(index, this.pItems.size());
            this.agregar(item, i);
            if (this.selectedIndex < 0 || this.selectedIndex >= i) {
                this.select(0);
            }
        }
    }

    /** Mete la entrada y selecciona la primera si era la única. */
    private void agregar(String item, int index) {
        if (item == null) {
            throw new NullPointerException("cannot add null item to Choice");
        }
        this.pItems.insertElementAt(item, index);
        if (this.selectedIndex < 0) {
            this.select(0);
        }
    }

    /**
     * Saca la primera entrada que diga eso.
     *
     * @throws IllegalArgumentException si no hay ninguna que diga eso
     */
    public void remove(String item) {
        synchronized (this) {
            int i = this.pItems.indexOf(item);
            if (i < 0) {
                throw new IllegalArgumentException("item " + item + " not found in choice");
            }
            this.remove(i);
        }
    }

    /**
     * Saca la entrada de esa posición.
     *
     * <p>Sacar la seleccionada pasa la selección a la primera que quede; si no queda ninguna, la
     * lista se queda sin selección, que es el único caso en que eso puede pasar.
     *
     * @throws IndexOutOfBoundsException si la posición no existe
     */
    public void remove(int position) {
        synchronized (this) {
            this.pItems.removeElementAt(position);
            if (this.pItems.isEmpty()) {
                this.selectedIndex = -1;
            } else if (this.selectedIndex == position) {
                this.select(0);
            } else if (this.selectedIndex > position) {
                this.select(this.selectedIndex - 1);
            }
        }
    }

    /** Vacía la lista. */
    public void removeAll() {
        synchronized (this) {
            this.pItems.removeAllElements();
            this.selectedIndex = -1;
        }
    }

    /**
     * Lo que dice la entrada seleccionada.
     *
     * @return el texto, o `null` si la lista está vacía
     */
    public synchronized String getSelectedItem() {
        if (this.selectedIndex < 0) {
            return null;
        }
        return this.getItem(this.selectedIndex);
    }

    /**
     * Lo que está seleccionado.
     *
     * @return un arreglo de un elemento, o `null` si la lista está vacía
     */
    public synchronized Object[] getSelectedObjects() {
        if (this.selectedIndex < 0) {
            return null;
        }
        Object[] items = new Object[1];
        items[0] = this.getItem(this.selectedIndex);
        return items;
    }

    /**
     * Qué posición está seleccionada.
     *
     * @return la posición, o -1 si la lista está vacía
     */
    public int getSelectedIndex() {
        return this.selectedIndex;
    }

    /**
     * Selecciona esa posición.
     *
     * @throws IllegalArgumentException si la posición no existe
     */
    public synchronized void select(int pos) {
        if (pos >= this.pItems.size() || pos < 0) {
            throw new IllegalArgumentException("illegal Choice item position: " + pos);
        }
        if (!this.pItems.isEmpty()) {
            this.selectedIndex = pos;
        }
    }

    /**
     * Selecciona la primera entrada que diga eso.
     *
     * <p>Si no hay ninguna no pasa nada, y es lo correcto: la selección anterior sigue siendo válida.
     */
    public synchronized void select(String str) {
        int i = this.pItems.indexOf(str);
        if (i >= 0) {
            this.select(i);
        }
    }

    /** Agrega un oyente; `null` no hace nada. */
    public synchronized void addItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.add(this.itemListener, l);
        this.enableEvents(AWTEvent.ITEM_EVENT_MASK);
    }

    /** Saca un oyente. */
    public synchronized void removeItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.remove(this.itemListener, l);
    }

    /** Los oyentes puestos. */
    public synchronized ItemListener[] getItemListeners() {
        return AWTEventMulticaster.getListeners(this.itemListener, ItemListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == ItemListener.class) {
            return AWTEventMulticaster.getListeners(this.itemListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof ItemEvent) {
            this.processItemEvent((ItemEvent) e);
            return;
        }
        super.processEvent(e);
    }

    /** Les avisa a los oyentes de selección. */
    protected void processItemEvent(ItemEvent e) {
        ItemListener l = this.itemListener;
        if (l != null) {
            l.itemStateChanged(e);
        }
    }

    protected String paramString() {
        return super.paramString() + ",current=" + this.getSelectedItem();
    }

    /** La accesibilidad de la lista. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTChoice();
        }
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de una lista desplegable.
     *
     * <p>Implementa {@link AccessibleAction} pero informa **cero** acciones, que es lo que hace el
     * JDK: la acción sería desplegar la lista, y eso lo hace el widget del sistema. Declarar una que
     * no se puede ejecutar sería peor que no declarar ninguna.
     *
     * <p><strong>No</strong> implementa {@link javax.accessibility.AccessibleSelection}, y no es un
     * olvido: tampoco lo hace el JDK, así que {@code getAccessibleSelection()} devuelve `null`.
     * Agregarlo sería más útil y sería divergir.
     */
    protected class AccessibleAWTChoice extends AccessibleAWTComponent
            implements AccessibleAction {

        /** Para las subclases. */
        protected AccessibleAWTChoice() {
        }

        public AccessibleAction getAccessibleAction() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.COMBO_BOX;
        }

        /** Ninguna. */
        public int getAccessibleActionCount() {
            return 0;
        }

        /**
         * Cómo se llama esa acción.
         *
         * @return `null` siempre: no hay ninguna
         */
        public String getAccessibleActionDescription(int i) {
            return null;
        }

        /**
         * Ejecuta esa acción.
         *
         * @return `false` siempre, por lo mismo
         */
        public boolean doAccessibleAction(int i) {
            return false;
        }

    }
}
