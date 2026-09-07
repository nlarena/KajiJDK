package javax.swing.text;

import java.io.Serializable;

/**
 * Las paradas de tabulacion de un parrafo, ordenadas y sin cambiar.
 *
 * <p>Inmutable como {@link TabStop}, y por la misma razon: un juego de paradas suele valer para
 * todo un documento. Las busquedas son binarias porque el arreglo esta ordenado por posicion, que
 * es como llega y como se lo usa: la pregunta tipica es "cual es la proxima parada despues de este
 * punto".
 */
public class TabSet implements Serializable {

    private TabStop[] tabs;

    private int hashCode = Integer.MAX_VALUE;

    /** Un juego con esas paradas; el arreglo se copia. */
    public TabSet(TabStop[] tabs) {
        if (tabs != null) {
            int tabCount = tabs.length;
            this.tabs = new TabStop[tabCount];
            System.arraycopy(tabs, 0, this.tabs, 0, tabCount);
        } else {
            this.tabs = new TabStop[0];
        }
    }

    public int getTabCount() {
        return tabs.length;
    }

    public TabStop getTab(int index) {
        int numTabs = tabs.length;
        if (index < 0 || index >= numTabs) {
            throw new IllegalArgumentException(index + " is outside the range of tabs");
        }
        return tabs[index];
    }

    /** La primera parada estrictamente despues de esa posicion, o {@code null} si no hay. */
    public TabStop getTabAfter(float location) {
        int index = getTabIndexAfter(location);
        return (index == -1) ? null : tabs[index];
    }

    public int getTabIndex(TabStop tab) {
        for (int counter = getTabCount() - 1; counter >= 0; counter--) {
            if (getTab(counter) == tab) {
                return counter;
            }
        }
        return -1;
    }

    /** Busqueda binaria: el indice de la primera parada despues de esa posicion, o {@code -1}. */
    public int getTabIndexAfter(float location) {
        int lower = 0;
        int upper = tabs.length - 1;
        int mid = 0;

        if (upper == -1 || location > tabs[upper].getPosition()) {
            return -1;
        }

        while (lower <= upper) {
            mid = lower + ((upper - lower) / 2);
            float tabPosition = tabs[mid].getPosition();
            if (location > tabPosition) {
                lower = mid + 1;
            } else if (location < tabPosition) {
                upper = mid - 1;
            } else {
                // Una parada justo en esa posicion cuenta como "la de despues": es lo que hace que
                // tabular estando en una parada no salte a la siguiente.
                return mid;
            }
        }
        return lower;
    }

    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof TabSet) {
            TabSet ts = (TabSet) o;
            int count = getTabCount();
            if (ts.getTabCount() != count) {
                return false;
            }
            for (int i = 0; i < count; i++) {
                TabStop ts1 = getTab(i);
                TabStop ts2 = ts.getTab(i);
                if ((ts1 == null && ts2 != null) || (ts1 != null && !getTab(i).equals(ts.getTab(i)))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /** Se calcula una vez y se guarda: el objeto no cambia. */
    public int hashCode() {
        if (hashCode == Integer.MAX_VALUE) {
            hashCode = 0;
            int len = getTabCount();
            for (int i = 0; i < len; i++) {
                TabStop ts = getTab(i);
                hashCode = hashCode ^ (ts != null ? getTab(i).hashCode() : 0);
            }
            if (hashCode == Integer.MAX_VALUE) {
                hashCode = hashCode - 1;
            }
        }
        return hashCode;
    }

    public String toString() {
        int tabCount = getTabCount();
        String buffer = "[ ";
        for (int counter = 0; counter < tabCount; counter++) {
            if (counter > 0) {
                buffer = buffer + " - ";
            }
            buffer = buffer + getTab(counter).toString();
        }
        buffer = buffer + " ]";
        return buffer;
    }
}
