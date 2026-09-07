package javax.swing;

import java.io.Serializable;
import java.util.BitSet;
import java.util.EventListener;

import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

/**
 * Que renglones estan elegidos.
 *
 * <h2>Un conjunto de bits y dos indices</h2>
 *
 * <p>Lo elegido va en un {@link BitSet}: una lista de un millon de renglones con dos elegidos ocupa
 * lo mismo que una de mil. Ademas se llevan dos indices que no dicen que esta elegido sino
 * <em>como</em> se llego: el ancla es donde empezo la seleccion y el guia donde esta ahora. Con los
 * dos, arrastrar el mouse hacia atras puede desmarcar lo que marco hacia adelante.
 *
 * <h2>Los avisos se juntan</h2>
 *
 * <p>Mientras {@link #setValueIsAdjusting} esta prendido, quien escucha sabe que la seleccion esta
 * a medio hacer y puede no actualizar nada hasta el final. Es lo que evita que arrastrar el mouse
 * por cien renglones haga cien consultas a una base de datos.
 *
 * <p>Ademas cada aviso lleva el rango que cambio, no la seleccion entera. El rango se junta
 * mientras se hacen varios cambios seguidos y se manda uno solo.
 */
public class DefaultListSelectionModel implements ListSelectionModel, Cloneable, Serializable {

    private static final int MIN = -1;
    private static final int MAX = Integer.MAX_VALUE;

    private int value = MIN;
    private BitSet valor = new BitSet(32);
    private int minIndex = MAX;
    private int maxIndex = MIN;
    private int anchorIndex = -1;
    private int leadIndex = -1;
    private int firstAdjustedIndex = MAX;
    private int lastAdjustedIndex = MIN;
    private boolean isAdjusting = false;
    private int firstChangedIndex = MAX;
    private int lastChangedIndex = MIN;
    private int selectionMode = MULTIPLE_INTERVAL_SELECTION;

    /** Quienes escuchan. */
    protected EventListenerList listenerList = new EventListenerList();

    /** Si al cambiar el ancla o el guia hay que avisar. */
    protected boolean leadAnchorNotificationEnabled = true;

    /** Un modelo sin nada elegido. */
    public DefaultListSelectionModel() {
    }

    public int getMinSelectionIndex() {
        return isSelectionEmpty() ? -1 : minIndex;
    }

    public int getMaxSelectionIndex() {
        return maxIndex;
    }

    public boolean getValueIsAdjusting() {
        return isAdjusting;
    }

    public int getSelectionMode() {
        return selectionMode;
    }

    /**
     * Cuantos renglones se pueden elegir a la vez.
     *
     * @throws IllegalArgumentException si no es uno de los tres modos.
     */
    public void setSelectionMode(int selectionMode) {
        if (selectionMode != SINGLE_SELECTION && selectionMode != SINGLE_INTERVAL_SELECTION
                && selectionMode != MULTIPLE_INTERVAL_SELECTION) {
            throw new IllegalArgumentException("invalid selectionMode");
        }
        int oldMode = this.selectionMode;
        this.selectionMode = selectionMode;
        if (oldMode == selectionMode || isSelectionEmpty()) {
            // Con la seleccion vacia no hay nada que achicar, y no se puede tocar: los extremos
            // valen los centinelas, y usarlos como indices marcaria un renglon inexistente.
            return;
        }
        if (selectionMode == SINGLE_SELECTION) {
            setSelectionInterval(maxIndex, maxIndex);
        } else if (selectionMode == SINGLE_INTERVAL_SELECTION) {
            setSelectionInterval(minIndex, maxIndex);
        }
    }

    public boolean isSelectedIndex(int index) {
        return ((index < minIndex) || (index > maxIndex)) ? false : valor.get(index);
    }

    public boolean isSelectionEmpty() {
        return (minIndex > maxIndex);
    }

    public void addListSelectionListener(ListSelectionListener l) {
        listenerList.add(ListSelectionListener.class, l);
    }

    public void removeListSelectionListener(ListSelectionListener l) {
        listenerList.remove(ListSelectionListener.class, l);
    }

    public ListSelectionListener[] getListSelectionListeners() {
        return listenerList.getListeners(ListSelectionListener.class);
    }

    /** Avisa que cambio la seleccion entre esos dos indices. */
    protected void fireValueChanged(int firstIndex, int lastIndex) {
        fireValueChanged(firstIndex, lastIndex, getValueIsAdjusting());
    }

    /** Avisa que la seleccion dejo de estar a medio hacer. */
    protected void fireValueChanged(boolean isAdjusting) {
        if (lastChangedIndex == MIN) {
            return;
        }
        int oldFirstChangedIndex = firstChangedIndex;
        int oldLastChangedIndex = lastChangedIndex;
        firstChangedIndex = MAX;
        lastChangedIndex = MIN;
        fireValueChanged(oldFirstChangedIndex, oldLastChangedIndex, isAdjusting);
    }

    protected void fireValueChanged(int firstIndex, int lastIndex, boolean isAdjusting) {
        Object[] listeners = listenerList.getListenerList();
        ListSelectionEvent e = null;
        for (int i = listeners.length - 2; i >= 0; i = i - 2) {
            if (listeners[i] == ListSelectionListener.class) {
                if (e == null) {
                    e = new ListSelectionEvent(this, firstIndex, lastIndex, isAdjusting);
                }
                ((ListSelectionListener) listeners[i + 1]).valueChanged(e);
            }
        }
    }

    public <T extends EventListener> T[] getListeners(Class<T> listenerType) {
        return listenerList.getListeners(listenerType);
    }

    /** Si mover el ancla o el guia cuenta como un cambio que hay que avisar. */
    public void setLeadAnchorNotificationEnabled(boolean flag) {
        leadAnchorNotificationEnabled = flag;
    }

    public boolean isLeadAnchorNotificationEnabled() {
        return leadAnchorNotificationEnabled;
    }

    /**
     * Deja la seleccion vacia.
     *
     * <p>No mueve el ancla ni el guia. Parece una omision y no lo es: los dos dicen por donde
     * venia el usuario, y borrar lo elegido no borra ese recorrido. Es lo que permite que apretar
     * Escape y despues Shift+flecha siga extendiendo desde donde estaba.
     */
    public void clearSelection() {
        quitarTramo(minIndex, maxIndex, false);
    }

    /** Deja elegido solo ese tramo. */
    public void setSelectionInterval(int index0, int index1) {
        if (index0 == -1 || index1 == -1) {
            return;
        }
        if (getSelectionMode() == SINGLE_SELECTION) {
            index0 = index1;
        }
        updateLeadAnchorIndices(index0, index1);
        int clearMin = minIndex;
        int clearMax = maxIndex;
        int setMin = Math.min(index0, index1);
        int setMax = Math.max(index0, index1);
        changeSelection(clearMin, clearMax, setMin, setMax);
    }

    /** Agrega ese tramo a lo elegido. */
    public void addSelectionInterval(int index0, int index1) {
        if (index0 == -1 || index1 == -1) {
            return;
        }
        if (getSelectionMode() == SINGLE_SELECTION) {
            setSelectionInterval(index0, index1);
            return;
        }
        updateLeadAnchorIndices(index0, index1);
        int clearMin = MAX;
        int clearMax = MIN;
        int setMin = Math.min(index0, index1);
        int setMax = Math.max(index0, index1);
        changeSelection(clearMin, clearMax, setMin, setMax);
    }

    /** Saca ese tramo de lo elegido. */
    public void removeSelectionInterval(int index0, int index1) {
        quitarTramo(index0, index1, true);
    }

    /** Saca ese tramo; {@code moverGuia} dice si ademas se mueven el ancla y el guia. */
    private void quitarTramo(int index0, int index1, boolean moverGuia) {
        if (index0 == -1 || index1 == -1) {
            return;
        }
        if (moverGuia) {
            updateLeadAnchorIndices(index0, index1);
        }
        int clearMin = Math.min(index0, index1);
        int clearMax = Math.max(index0, index1);
        int setMin = MAX;
        int setMax = MIN;
        changeSelection(clearMin, clearMax, setMin, setMax);
    }

    /**
     * Corre la seleccion porque se insertaron renglones.
     *
     * <p>Lo llama la lista cuando el modelo de datos cambia. Sin esto, insertar un renglon arriba
     * dejaria elegido el de al lado del que estaba elegido.
     */
    public void insertIndexInterval(int index, int length, boolean before) {
        int insMinIndex = (before) ? index : index + 1;
        int insMaxIndex = (insMinIndex + length) - 1;
        for (int i = maxIndex; i >= insMinIndex; i--) {
            setState(i + length, valor.get(i));
        }
        boolean setInsertedValues = ((getSelectionMode() == SINGLE_SELECTION)
                ? false : valor.get(index));
        for (int i = insMinIndex; i <= insMaxIndex; i++) {
            setState(i, setInsertedValues);
        }
        int leadIndex = this.leadIndex;
        if (leadIndex > index || (before && leadIndex == index)) {
            leadIndex = this.leadIndex + length;
        }
        int anchorIndex = this.anchorIndex;
        if (anchorIndex > index || (before && anchorIndex == index)) {
            anchorIndex = this.anchorIndex + length;
        }
        if (leadIndex != this.leadIndex || anchorIndex != this.anchorIndex) {
            updateLeadAnchorIndices(anchorIndex, leadIndex);
        }
        fireValueChanged();
    }

    /** Corre la seleccion porque se sacaron renglones. */
    public void removeIndexInterval(int index0, int index1) {
        int rmMinIndex = Math.min(index0, index1);
        int rmMaxIndex = Math.max(index0, index1);
        int gapLength = (rmMaxIndex - rmMinIndex) + 1;
        for (int i = rmMinIndex; i <= maxIndex; i++) {
            setState(i, valor.get(i + gapLength));
        }
        int leadIndex = this.leadIndex;
        if (leadIndex == 0 && rmMinIndex == 0) {
            // No se mueve.
        } else if (leadIndex > rmMaxIndex) {
            leadIndex = this.leadIndex - gapLength;
        } else if (leadIndex >= rmMinIndex) {
            leadIndex = rmMinIndex - 1;
        }
        int anchorIndex = this.anchorIndex;
        if (anchorIndex == 0 && rmMinIndex == 0) {
            // Tampoco.
        } else if (anchorIndex > rmMaxIndex) {
            anchorIndex = this.anchorIndex - gapLength;
        } else if (anchorIndex >= rmMinIndex) {
            anchorIndex = rmMinIndex - 1;
        }
        if (leadIndex != this.leadIndex || anchorIndex != this.anchorIndex) {
            updateLeadAnchorIndices(anchorIndex, leadIndex);
        }
        fireValueChanged();
    }

    /** Marca que la seleccion esta a medio hacer; ver la nota de la clase. */
    public void setValueIsAdjusting(boolean isAdjusting) {
        if (isAdjusting != this.isAdjusting) {
            this.isAdjusting = isAdjusting;
            this.fireValueChanged(isAdjusting);
        }
    }

    public String toString() {
        String s = ((getValueIsAdjusting()) ? "~" : "") + valor.toString();
        return getClass().getName() + " " + Integer.toString(hashCode()) + " " + s;
    }

    /** Una copia con la misma seleccion y sin los que escuchan. */
    public Object clone() throws CloneNotSupportedException {
        DefaultListSelectionModel clone = (DefaultListSelectionModel) super.clone();
        clone.valor = (BitSet) valor.clone();
        clone.listenerList = new EventListenerList();
        return clone;
    }

    public int getAnchorSelectionIndex() {
        return anchorIndex;
    }

    public int getLeadSelectionIndex() {
        return leadIndex;
    }

    public void setAnchorSelectionIndex(int anchorIndex) {
        updateLeadAnchorIndices(anchorIndex, this.leadIndex);
        fireValueChanged();
    }

    /** Mueve el guia sin cambiar lo elegido. */
    public void moveLeadSelectionIndex(int leadIndex) {
        if (leadIndex == -1 && anchorIndex != -1) {
            return;
        }
        if (this.leadIndex == leadIndex) {
            return;
        }
        updateLeadAnchorIndices(anchorIndex, leadIndex);
        fireValueChanged();
    }

    /**
     * Mueve el guia arrastrando la seleccion desde el ancla.
     *
     * <p>Es lo que pasa al arrastrar el mouse: lo que quedo entre el ancla y el guia nuevo toma el
     * estado del ancla, y lo que quedo fuera vuelve a como estaba. Asi, arrastrar hacia atras
     * desmarca.
     */
    public void setLeadSelectionIndex(int leadIndex) {
        int anchorIndex = this.anchorIndex;
        if (getSelectionMode() == SINGLE_SELECTION) {
            setSelectionInterval(leadIndex, leadIndex);
            return;
        }
        if (anchorIndex == -1 || leadIndex == -1) {
            return;
        }
        if (this.leadIndex == -1) {
            this.leadIndex = leadIndex;
        }
        int oldMin = Math.min(this.anchorIndex, this.leadIndex);
        int oldMax = Math.max(this.anchorIndex, this.leadIndex);
        int newMin = Math.min(anchorIndex, leadIndex);
        int newMax = Math.max(anchorIndex, leadIndex);
        updateLeadAnchorIndices(anchorIndex, leadIndex);
        // Los dos casos no son simetricos. Arrastrando desde un ancla elegida se marca lo nuevo y
        // se desmarca lo que quedo afuera; desde un ancla no elegida es al reves, y ademas el
        // tramo que esta en los dos rangos tiene que quedar DESmarcado. De ahi el `false`: dice
        // cual de los dos gana en la parte que se pisa.
        if (valor.get(this.anchorIndex)) {
            changeSelection(oldMin, oldMax, newMin, newMax);
        } else {
            changeSelection(newMin, newMax, oldMin, oldMax, false);
        }
    }

    // ---- lo de adentro ----

    private void updateLeadAnchorIndices(int anchorIndex, int leadIndex) {
        if (leadAnchorNotificationEnabled) {
            if (this.anchorIndex != anchorIndex) {
                markAsDirty(this.anchorIndex);
                markAsDirty(anchorIndex);
            }
            if (this.leadIndex != leadIndex) {
                markAsDirty(this.leadIndex);
                markAsDirty(leadIndex);
            }
        }
        this.anchorIndex = anchorIndex;
        this.leadIndex = leadIndex;
    }

    private void markAsDirty(int r) {
        if (r == -1) {
            return;
        }
        firstAdjustedIndex = Math.min(firstAdjustedIndex, r);
        lastAdjustedIndex = Math.max(lastAdjustedIndex, r);
    }

    private void setState(int r, boolean state) {
        if (state) {
            set(r);
        } else {
            clear(r);
        }
    }

    private void set(int r) {
        if (valor.get(r)) {
            return;
        }
        valor.set(r);
        markAsDirty(r);
        minIndex = Math.min(minIndex, r);
        maxIndex = Math.max(maxIndex, r);
    }

    private void clear(int r) {
        if (!valor.get(r)) {
            return;
        }
        valor.clear(r);
        markAsDirty(r);
        // Si se saco un extremo, hay que buscar el nuevo: el conjunto no lo lleva.
        if (r == minIndex) {
            for (minIndex = minIndex + 1; minIndex <= maxIndex; minIndex++) {
                if (valor.get(minIndex)) {
                    break;
                }
            }
        }
        if (r == maxIndex) {
            for (maxIndex = maxIndex - 1; minIndex <= maxIndex; maxIndex--) {
                if (valor.get(maxIndex)) {
                    break;
                }
            }
        }
        if (isSelectionEmpty()) {
            minIndex = MAX;
            maxIndex = MIN;
        }
    }

    private void changeSelection(int clearMin, int clearMax, int setMin, int setMax) {
        changeSelection(clearMin, clearMax, setMin, setMax, true);
    }

    private void changeSelection(int clearMin, int clearMax, int setMin, int setMax,
            boolean clearFirst) {
        for (int i = Math.min(setMin, clearMin); i <= Math.max(setMax, clearMax); i++) {
            boolean shouldClear = contains(clearMin, clearMax, i);
            boolean shouldSet = contains(setMin, setMax, i);
            if (shouldSet && shouldClear) {
                if (clearFirst) {
                    shouldClear = false;
                } else {
                    shouldSet = false;
                }
            }
            if (shouldSet) {
                set(i);
            }
            if (shouldClear) {
                clear(i);
            }
        }
        fireValueChanged();
    }

    private static boolean contains(int a, int b, int i) {
        return (i >= a) && (i <= b);
    }

    /** Manda el aviso con el rango que se junto, si hay alguno. */
    private void fireValueChanged() {
        if (lastAdjustedIndex == MIN) {
            return;
        }
        if (getValueIsAdjusting()) {
            firstChangedIndex = Math.min(firstChangedIndex, firstAdjustedIndex);
            lastChangedIndex = Math.max(lastChangedIndex, lastAdjustedIndex);
        }
        int oldFirstAdjustedIndex = firstAdjustedIndex;
        int oldLastAdjustedIndex = lastAdjustedIndex;
        firstAdjustedIndex = MAX;
        lastAdjustedIndex = MIN;
        fireValueChanged(oldFirstAdjustedIndex, oldLastAdjustedIndex);
    }
}
