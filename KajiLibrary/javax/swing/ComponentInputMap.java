package javax.swing;

/**
 * An {@link InputMap} tied to a component.
 *
 * <h2>Why it needs to know the component</h2>
 *
 * <p>A shortcut that holds in the whole window is attended by the component that declared it,
 * even though the focus is somewhere else. For that Swing keeps a table of those shortcuts per
 * window, built from the components it contains; when one changes its table, that window table
 * has to be rebuilt. Without knowing which component it belongs to, there would be nobody to
 * tell.
 *
 * <p>Hence also the parent's rule: only another {@code ComponentInputMap} of the <em>same</em>
 * component can be one. Chaining one component's with another's would make a shortcut be
 * attributed to the wrong one.
 */
public class ComponentInputMap extends InputMap {

    private JComponent component;

    /**
     * A table for that component.
     *
     * @throws IllegalArgumentException if the component is null.
     */
    public ComponentInputMap(JComponent component) {
        this.component = component;
        if (component == null) {
            throw new IllegalArgumentException(
                    "ComponentInputMaps must be associated with a non-null JComponent");
        }
    }

    /**
     * The parent; see the class note.
     *
     * @throws IllegalArgumentException if it is not of this same component.
     */
    public void setParent(InputMap map) {
        if (getParent() == map) {
            return;
        }
        if (map != null && (!(map instanceof ComponentInputMap)
                || ((ComponentInputMap) map).getComponent() != getComponent())) {
            throw new IllegalArgumentException(
                    "ComponentInputMaps must have a parent ComponentInputMap");
        }
        super.setParent(map);
        notifyChange();
    }

    /** The component it belongs to. */
    public JComponent getComponent() {
        return component;
    }

    public void put(KeyStroke key, Object actionMapKey) {
        super.put(key, actionMapKey);
        notifyChange();
    }

    public void remove(KeyStroke key) {
        super.remove(key);
        notifyChange();
    }

    public void clear() {
        int n = size();
        super.clear();
        if (n > 0) {
            notifyChange();
        }
    }

    /** It tells the component that the window's table has to be rebuilt. */
    private void notifyChange() {
        if (getComponent() != null) {
            getComponent().componentInputMapChanged(this);
        }
    }
}
