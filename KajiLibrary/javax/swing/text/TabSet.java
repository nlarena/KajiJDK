package javax.swing.text;

import java.io.Serializable;

/**
 * A paragraph's tab stops, ordered and unchanging.
 *
 * <p>Immutable like {@link TabStop}, and for the same reason: a set of stops usually holds for a
 * whole document. The searches are binary because the array is ordered by position, which is how
 * it arrives and how it is used: the typical question is "which is the next stop after this
 * point".
 */
public class TabSet implements Serializable {

    private TabStop[] tabs;

    private int hashCode = Integer.MAX_VALUE;

    /** A set with those stops; the array is copied. */
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

    /** The first stop strictly after that position, or {@code null} if there is none. */
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

    /** Binary search: the index of the first stop after that position, or {@code -1}. */
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
                // A stop exactly at that position counts as "the one after": it is what keeps
                                // tabbing while standing on a stop from jumping to the next one.
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

    /** It is computed once and kept: the object does not change. */
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
