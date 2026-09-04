package java.awt;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventListener;
import java.util.Vector;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.accessibility.AccessibleSelection;
import javax.accessibility.AccessibleState;
import javax.accessibility.AccessibleStateSet;

/**
 * Una lista de renglones donde se puede elegir uno o varios.
 *
 * <p>A diferencia de {@link Choice}, la lista muestra varios renglones a la vez, puede no tener nada
 * seleccionado, y en modo múltiple admite cualquier cantidad. Genera dos eventos distintos y hay que
 * no confundirlos: un {@link ItemEvent} cuando cambia la selección, y un {@link ActionEvent} sólo
 * cuando el usuario hace **doble clic** o aprieta Enter, o sea cuando confirma.
 *
 * <p><strong>Dos divergencias deliberadas con el JDK</strong>, las dos por lo mismo: allá el widget
 * nativo lleva la selección, y acá no hay widget nativo. El JDK, sin uno, deja la selección
 * apuntando a los renglones **viejos** después de insertar o sacar en el medio, y deja varios
 * seleccionados después de pasar a modo simple. Son dos estados que la propia clase dice que no
 * existen, y que allá nunca se ven porque el widget del sistema los arregla. Acá los arregla la
 * clase: {@link #add(String, int)} y {@link #delItems} corren la selección con los renglones, y
 * {@link #setMultipleMode setMultipleMode(false)} la recorta a uno.
 */
public class List extends Component implements ItemSelectable, Accessible {

    private static final long serialVersionUID = -3304312411574666869L;

    private static int listCounter = 0;

    /** Cuántos renglones muestra una lista que no dijo cuántos. */
    static final int DEFAULT_VISIBLE_ROWS = 4;

    /** Los renglones. */
    Vector<String> items = new Vector<String>();

    /** Cuántos renglones se ven de una. */
    int rows = 0;

    /** Si admite más de uno seleccionado. */
    boolean multipleMode = false;

    /** Las posiciones seleccionadas, ordenadas. */
    int[] selected = new int[0];

    /** El renglón que se pidió dejar a la vista, o -1. */
    int visibleIndex = -1;

    /** Los oyentes de acción, encadenados. */
    transient ActionListener actionListener;

    /** Los de selección. */
    transient ItemListener itemListener;

    /** Una lista de cuatro renglones, de selección simple. */
    public List() throws HeadlessException {
        this(0, false);
    }

    /** Una lista de esa cantidad de renglones, de selección simple. */
    public List(int rows) throws HeadlessException {
        this(rows, false);
    }

    /** Una lista de esa cantidad de renglones, del modo que se pida. */
    public List(int rows, boolean multipleMode) throws HeadlessException {
        this.rows = rows != 0 ? rows : DEFAULT_VISIBLE_ROWS;
        this.multipleMode = multipleMode;
    }

    String constructComponentName() {
        synchronized (List.class) {
            String n = "list" + listCounter;
            listCounter = listCounter + 1;
            return n;
        }
    }

    /** La declara mostrable. */
    public void addNotify() {
        super.addNotify();
    }

    /** La declara no mostrable. */
    public void removeNotify() {
        super.removeNotify();
    }

    /** Cuántos renglones tiene. */
    public int getItemCount() {
        return this.items.size();
    }

    /**
     * Cuántos renglones tiene.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getItemCount}.
     */
    @Deprecated
    public int countItems() {
        return this.getItemCount();
    }

    /**
     * El renglón de esa posición.
     *
     * @throws ArrayIndexOutOfBoundsException si la posición no existe
     */
    public String getItem(int index) {
        return this.getItemImpl(index);
    }

    final String getItemImpl(int index) {
        return this.items.elementAt(index);
    }

    /** Todos los renglones. */
    public synchronized String[] getItems() {
        String[] r = new String[this.items.size()];
        this.items.copyInto(r);
        return r;
    }

    /** Agrega un renglón al final. */
    public void add(String item) {
        this.add(item, -1);
    }

    /**
     * Agrega un renglón al final.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #add(String)}.
     */
    @Deprecated
    public void addItem(String item) {
        this.addItem(item, -1);
    }

    /**
     * Inserta un renglón en esa posición.
     *
     * @param index dónde meterlo; una posición negativa o pasada del final lo pone al final
     */
    public void add(String item, int index) {
        this.addItem(item, index);
    }

    /**
     * Inserta un renglón en esa posición.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #add(String, int)}.
     */
    @Deprecated
    public synchronized void addItem(String item, int index) {
        if (index < -1 || index >= this.items.size()) {
            index = -1;
        }
        if (item == null) {
            item = "";
        }
        if (index == -1) {
            this.items.addElement(item);
        } else {
            this.items.insertElementAt(item, index);
            this.correrSeleccion(index, 1);
        }
    }

    /**
     * Cambia lo que dice el renglón de esa posición.
     *
     * <p>Sacar y volver a poner **pierde la selección** de ese renglón, y es lo que hace el JDK.
     *
     * @throws ArrayIndexOutOfBoundsException si la posición no existe
     */
    public synchronized void replaceItem(String newValue, int index) {
        this.remove(index);
        this.add(newValue, index);
    }

    /** Vacía la lista. */
    public void removeAll() {
        synchronized (this) {
            this.items.removeAllElements();
            this.selected = new int[0];
        }
    }

    /**
     * Vacía la lista.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #removeAll}.
     */
    @Deprecated
    public synchronized void clear() {
        this.removeAll();
    }

    /**
     * Saca el primer renglón que diga eso.
     *
     * @throws IllegalArgumentException si no hay ninguno que diga eso
     */
    public synchronized void remove(String item) {
        int i = this.items.indexOf(item);
        if (i < 0) {
            throw new IllegalArgumentException("item " + item + " not found in list");
        }
        this.remove(i);
    }

    /**
     * Saca el renglón de esa posición.
     *
     * @throws ArrayIndexOutOfBoundsException si la posición no existe
     */
    public void remove(int position) {
        this.delItem(position);
    }

    /**
     * Saca el renglón de esa posición.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #remove(int)}.
     */
    @Deprecated
    public void delItem(int position) {
        this.delItems(position, position);
    }

    /**
     * Qué renglón está seleccionado.
     *
     * @return la posición, o -1 si no hay ninguno o hay más de uno
     */
    public synchronized int getSelectedIndex() {
        if (this.selected.length != 1) {
            return -1;
        }
        return this.selected[0];
    }

    /** Qué renglones están seleccionados. */
    public synchronized int[] getSelectedIndexes() {
        int[] r = new int[this.selected.length];
        System.arraycopy(this.selected, 0, r, 0, this.selected.length);
        return r;
    }

    /**
     * Lo que dice el renglón seleccionado.
     *
     * @return el texto, o `null` si no hay ninguno o hay más de uno
     */
    public synchronized String getSelectedItem() {
        int i = this.getSelectedIndex();
        return i < 0 ? null : this.getItem(i);
    }

    /** Lo que dicen los renglones seleccionados. */
    public synchronized String[] getSelectedItems() {
        String[] r = new String[this.selected.length];
        for (int i = 0; i < this.selected.length; i++) {
            r[i] = this.getItem(this.selected[i]);
        }
        return r;
    }

    /** Lo mismo que {@link #getSelectedItems}, como pide {@link ItemSelectable}. */
    public Object[] getSelectedObjects() {
        return this.getSelectedItems();
    }

    /**
     * Selecciona ese renglón.
     *
     * <p>En modo simple **reemplaza** la selección; en múltiple la agrega. Una posición que no existe
     * se ignora: es lo que hace el JDK, porque la lista puede haber cambiado entre que se calculó la
     * posición y se la usó.
     */
    public void select(int index) {
        synchronized (this) {
            if (index < 0 || index >= this.items.size()) {
                return;
            }
            if (this.isIndexSelected(index)) {
                return;
            }
            if (!this.multipleMode) {
                this.selected = new int[1];
                this.selected[0] = index;
                return;
            }
            int[] nuevos = new int[this.selected.length + 1];
            int j = 0;
            boolean puesto = false;
            for (int i = 0; i < this.selected.length; i++) {
                if (!puesto && this.selected[i] > index) {
                    nuevos[j] = index;
                    j = j + 1;
                    puesto = true;
                }
                nuevos[j] = this.selected[i];
                j = j + 1;
            }
            if (!puesto) {
                nuevos[j] = index;
            }
            this.selected = nuevos;
        }
    }

    /** Deselecciona ese renglón; si no estaba seleccionado no pasa nada. */
    public synchronized void deselect(int index) {
        if (!this.isIndexSelected(index)) {
            return;
        }
        int[] nuevos = new int[this.selected.length - 1];
        int j = 0;
        for (int i = 0; i < this.selected.length; i++) {
            if (this.selected[i] != index) {
                nuevos[j] = this.selected[i];
                j = j + 1;
            }
        }
        this.selected = nuevos;
    }

    /** Si ese renglón está seleccionado. */
    public boolean isIndexSelected(int index) {
        int[] sel = this.selected;
        for (int i = 0; i < sel.length; i++) {
            if (sel[i] == index) {
                return true;
            }
        }
        return false;
    }

    /**
     * Si ese renglón está seleccionado.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #isIndexSelected}.
     */
    @Deprecated
    public boolean isSelected(int index) {
        return this.isIndexSelected(index);
    }

    /** Cuántos renglones muestra de una. */
    public int getRows() {
        return this.rows;
    }

    /** Si admite más de uno seleccionado. */
    public boolean isMultipleMode() {
        return this.multipleMode;
    }

    /**
     * Si admite más de uno seleccionado.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #isMultipleMode}.
     */
    @Deprecated
    public boolean allowsMultipleSelections() {
        return this.multipleMode;
    }

    /**
     * Cambia el modo de selección.
     *
     * <p>Al pasar a simple con varios seleccionados, se queda con el **último**, que es el que estaba
     * marcado como el actual en la interfaz.
     */
    public void setMultipleMode(boolean b) {
        this.setMultipleSelections(b);
    }

    /**
     * Cambia el modo de selección.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #setMultipleMode}.
     */
    @Deprecated
    public synchronized void setMultipleSelections(boolean b) {
        if (b == this.multipleMode) {
            return;
        }
        this.multipleMode = b;
        if (!b && this.selected.length > 1) {
            int ultimo = this.selected[this.selected.length - 1];
            this.selected = new int[1];
            this.selected[0] = ultimo;
        }
    }

    /**
     * Qué renglón se pidió dejar a la vista.
     *
     * @return la posición, o -1 si nadie lo pidió
     */
    public int getVisibleIndex() {
        return this.visibleIndex;
    }

    /**
     * Pide que ese renglón quede a la vista.
     *
     * <p>Sin pantalla no hay nada que desplazar, pero el pedido queda anotado y
     * {@link #getVisibleIndex} lo informa, que es lo único observable de este método.
     */
    public synchronized void makeVisible(int index) {
        this.visibleIndex = index;
    }

    /** Lo que necesitaría una lista de esa cantidad de renglones. */
    public Dimension getPreferredSize(int rows) {
        return this.medir(rows);
    }

    /**
     * Lo que necesitaría una lista de esa cantidad de renglones.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getPreferredSize(int)}.
     */
    @Deprecated
    public Dimension preferredSize(int rows) {
        return this.getPreferredSize(rows);
    }

    public Dimension getPreferredSize() {
        return this.rows > 0 ? this.getPreferredSize(this.rows) : super.getPreferredSize();
    }

    /**
     * Lo que necesita.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getPreferredSize()}.
     */
    @Deprecated
    public Dimension preferredSize() {
        return this.getPreferredSize();
    }

    /** Lo mínimo que necesitaría una lista de esa cantidad de renglones. */
    public Dimension getMinimumSize(int rows) {
        return this.medir(rows);
    }

    /**
     * Lo mínimo para esa cantidad de renglones.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getMinimumSize(int)}.
     */
    @Deprecated
    public Dimension minimumSize(int rows) {
        return this.getMinimumSize(rows);
    }

    public Dimension getMinimumSize() {
        return this.rows > 0 ? this.getMinimumSize(this.rows) : super.getMinimumSize();
    }

    /**
     * Lo mínimo que necesita.
     *
     * @deprecated es del nombrado de 1.0. Usar {@link #getMinimumSize()}.
     */
    @Deprecated
    public Dimension minimumSize() {
        return this.getMinimumSize();
    }

    /**
     * Cuánto ocupan esos renglones.
     *
     * <p>Sin pantalla no hay tipografía medida, así que la medida sale del tamaño ya fijado. Es lo
     * mismo que hace {@link Component#getPreferredSize} y por el mismo motivo: inventar un alto de
     * renglón sería inventar una métrica que no existe.
     */
    private Dimension medir(int rows) {
        return this.getSize();
    }

    /** Agrega un oyente de selección; `null` no hace nada. */
    public synchronized void addItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.add(this.itemListener, l);
        this.enableEvents(AWTEvent.ITEM_EVENT_MASK);
    }

    /** Saca un oyente de selección. */
    public synchronized void removeItemListener(ItemListener l) {
        if (l == null) {
            return;
        }
        this.itemListener = AWTEventMulticaster.remove(this.itemListener, l);
    }

    /** Los oyentes de selección. */
    public synchronized ItemListener[] getItemListeners() {
        return AWTEventMulticaster.getListeners(this.itemListener, ItemListener.class);
    }

    /** Agrega un oyente de acción; `null` no hace nada. */
    public synchronized void addActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.add(this.actionListener, l);
        this.enableEvents(AWTEvent.ACTION_EVENT_MASK);
    }

    /** Saca un oyente de acción. */
    public synchronized void removeActionListener(ActionListener l) {
        if (l == null) {
            return;
        }
        this.actionListener = AWTEventMulticaster.remove(this.actionListener, l);
    }

    /** Los oyentes de acción. */
    public synchronized ActionListener[] getActionListeners() {
        return AWTEventMulticaster.getListeners(this.actionListener, ActionListener.class);
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        if (listenerType == ActionListener.class) {
            return AWTEventMulticaster.getListeners(this.actionListener, listenerType);
        }
        if (listenerType == ItemListener.class) {
            return AWTEventMulticaster.getListeners(this.itemListener, listenerType);
        }
        return super.getListeners(listenerType);
    }

    protected void processEvent(AWTEvent e) {
        if (e instanceof ActionEvent) {
            this.processActionEvent((ActionEvent) e);
            return;
        }
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

    /** Les avisa a los oyentes de acción. */
    protected void processActionEvent(ActionEvent e) {
        ActionListener l = this.actionListener;
        if (l != null) {
            l.actionPerformed(e);
        }
    }

    protected String paramString() {
        return super.paramString() + ",selected=" + this.getSelectedItem();
    }

    /**
     * Saca un tramo de renglones, de punta a punta inclusive.
     *
     * @deprecated es de uso interno del sistema de ventanas. Usar {@link #remove(int)}.
     */
    @Deprecated
    public synchronized void delItems(int start, int end) {
        for (int i = end; i >= start; i--) {
            this.items.removeElementAt(i);
        }
        this.correrSeleccion(start, -(end - start + 1));
    }

    /**
     * Corre las posiciones seleccionadas cuando se mete o se saca en el medio.
     *
     * <p>Las que caen adentro del tramo sacado se pierden; las de después se corren. Sin esto la
     * selección quedaría apuntando a renglones distintos de los que el usuario eligió, que es peor
     * que perderla.
     */
    private void correrSeleccion(int desde, int cuanto) {
        int[] sel = this.selected;
        int[] tmp = new int[sel.length];
        int j = 0;
        for (int i = 0; i < sel.length; i++) {
            if (sel[i] < desde) {
                tmp[j] = sel[i];
                j = j + 1;
            } else if (cuanto > 0) {
                tmp[j] = sel[i] + cuanto;
                j = j + 1;
            } else if (sel[i] >= desde - cuanto) {
                tmp[j] = sel[i] + cuanto;
                j = j + 1;
            }
        }
        int[] nuevos = new int[j];
        System.arraycopy(tmp, 0, nuevos, 0, j);
        this.selected = nuevos;
    }

    /** La accesibilidad de la lista. */
    public AccessibleContext getAccessibleContext() {
        if (this.accessibleContext == null) {
            this.accessibleContext = new AccessibleAWTList();
        }
        return this.accessibleContext;
    }

    /**
     * La accesibilidad de una lista.
     *
     * <p>Informa `MULTISELECTABLE` cuando corresponde y sabe operar la selección. Los renglones no
     * son componentes, así que {@link #getAccessibleSelection(int)} devuelve `null`: mentir con un
     * objeto envolvente sería peor que decir que no hay.
     */
    protected class AccessibleAWTList extends AccessibleAWTComponent
            implements AccessibleSelection {

        /** Para las subclases. */
        protected AccessibleAWTList() {
        }

        public AccessibleSelection getAccessibleSelection() {
            return this;
        }

        public AccessibleRole getAccessibleRole() {
            return AccessibleRole.LIST;
        }

        public AccessibleStateSet getAccessibleStateSet() {
            AccessibleStateSet s = super.getAccessibleStateSet();
            if (List.this.isMultipleMode()) {
                s.add(AccessibleState.MULTISELECTABLE);
            }
            return s;
        }

        /** Cuántos renglones tiene. */
        public int getAccessibleChildrenCount() {
            return List.this.getItemCount();
        }

        /**
         * El renglón de esa posición.
         *
         * @return `null` siempre: los renglones son cadenas, no componentes
         */
        public Accessible getAccessibleChild(int i) {
            return null;
        }

        /** Cuántos están seleccionados. */
        public int getAccessibleSelectionCount() {
            return List.this.getSelectedIndexes().length;
        }

        /**
         * Lo seleccionado.
         *
         * @return `null` siempre, por lo mismo que {@link #getAccessibleChild}
         */
        public Accessible getAccessibleSelection(int i) {
            return null;
        }

        /** Si ese renglón está seleccionado. */
        public boolean isAccessibleChildSelected(int i) {
            return List.this.isIndexSelected(i);
        }

        /** Selecciona ese renglón. */
        public void addAccessibleSelection(int i) {
            List.this.select(i);
        }

        /** Lo deselecciona. */
        public void removeAccessibleSelection(int i) {
            List.this.deselect(i);
        }

        /** Deselecciona todo. */
        public void clearAccessibleSelection() {
            int[] sel = List.this.getSelectedIndexes();
            for (int i = 0; i < sel.length; i++) {
                List.this.deselect(sel[i]);
            }
        }

        /** Selecciona todo, si la lista lo admite; en modo simple no hace nada. */
        public void selectAllAccessibleSelection() {
            if (!List.this.isMultipleMode()) {
                return;
            }
            int n = List.this.getItemCount();
            for (int i = 0; i < n; i++) {
                List.this.select(i);
            }
        }
    }
}
