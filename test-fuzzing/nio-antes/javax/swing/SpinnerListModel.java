package javax.swing;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * A sequence made of a fixed list, for a {@link JSpinner}.
 *
 * <h2>The list cannot be empty</h2>
 *
 * <p>Every constructor and {@link #setList} reject null and empty. The reason is that the model
 * keeps a <em>position</em>, not a value, and {@link #getValue} reads the list at that position:
 * with the list empty there would be nothing to return and every query would blow up. It is
 * better that whoever empties it blows up.
 *
 * <h2>The value is looked up by equality</h2>
 *
 * <p>{@link #setValue} does not keep what it is given: it looks up where it is in the list and
 * keeps that position. A value that is not in the list is an error, not a new value. And if the
 * list has repeats, the first one wins.
 *
 * <p>Changing the list takes the position back to zero, even though the previous value is still
 * there: the old position does not mean the same thing in a new list.
 */
public class SpinnerListModel extends AbstractSpinnerModel implements Serializable {

    private List<?> list;
    private int index;

    /**
     * With that list.
     *
     * @throws IllegalArgumentException if it is null or empty.
     */
    public SpinnerListModel(List<?> values) {
        if (values == null || values.size() == 0) {
            throw new IllegalArgumentException(
                    "SpinnerListModel(List) expects non-null non-empty List");
        }
        this.list = values;
        this.index = 0;
    }

    /**
     * With that array.
     *
     * @throws IllegalArgumentException if it is null or empty.
     */
    public SpinnerListModel(Object[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(
                    "SpinnerListModel(Object[]) expects non-null non-empty Object[]");
        }
        this.list = Arrays.asList(values);
        this.index = 0;
    }

    /** With a single filler element, which is what the JDK sets. */
    public SpinnerListModel() {
        this(new Object[] {"empty"});
    }

    /** The list; it is not a copy. */
    public List<?> getList() {
        return list;
    }

    /**
     * It changes the list and goes back to the first.
     *
     * @throws IllegalArgumentException if it is null or empty.
     */
    public void setList(List<?> list) {
        if ((list == null) || (list.size() == 0)) {
            throw new IllegalArgumentException("invalid list");
        }
        if (!list.equals(this.list)) {
            this.list = list;
            index = 0;
            fireStateChanged();
        }
    }

    public Object getValue() {
        return list.get(index);
    }

    /**
     * It stands on that element.
     *
     * @throws IllegalArgumentException if it is not in the list.
     */
    public void setValue(Object elt) {
        int index = list.indexOf(elt);
        if (index == -1) {
            throw new IllegalArgumentException("invalid sequence element");
        } else if (index != this.index) {
            this.index = index;
            fireStateChanged();
        }
    }

    /** The next one, or null if it is already on the last. */
    public Object getNextValue() {
        return (index >= (list.size() - 1)) ? null : list.get(index + 1);
    }

    /** The previous one, or null if it is already on the first. */
    public Object getPreviousValue() {
        return (index <= 0) ? null : list.get(index - 1);
    }

    /**
     * The first element from the current one on whose text begins with that prefix.
     *
     * <p>It is what the editor uses in order to jump by typing. It wraps round on reaching the
     * end, so typing the same letter walks through all those that begin with it.
     */
    Object findNextMatch(String prefix) {
        int max = list.size();
        if (max == 0) {
            return null;
        }
        int counter = index;
        do {
            Object value = list.get(counter);
            String string = value.toString();
            if (string != null && string.startsWith(prefix)) {
                return value;
            }
            counter = (counter + 1) % max;
        } while (counter != index);
        return null;
    }
}
