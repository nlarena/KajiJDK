package javax.accessibility;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Locale;

/**
 * All of an object's accessibility information, in a single place.
 *
 * <p>It is the heart of the package and its most debatable design at first sight: a class with
 * fifty members, half of which return `null`. The reason is that **not every object is
 * everything**. A button has no text to walk and no rows; a text field has no children to choose.
 * Instead of a hierarchy of interfaces that would multiply the combinations, one asks: {@code
 * getAccessibleText()} returns `null` if the object shows no text, and something if it does.
 *
 * <p>That `null` is not a void: it is the answer. "This object is not text" is information, and it
 * is what lets an assistive technology know which questions make sense.
 *
 * <p>The other half of the class is the **notifications**. An assistive technology cannot be
 * polling the state all the time, so the context announces when something changes, with the same
 * property mechanism as JavaBeans. The property names are the constants above.
 */
public abstract class AccessibleContext {

    /** The name of the <b>accessibleActionProperty</b> property. */
    public static final String ACCESSIBLE_ACTION_PROPERTY = "accessibleActionProperty";

    /** The name of the <b>AccessibleActiveDescendant</b> property. */
    public static final String ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY = "AccessibleActiveDescendant";

    /** The name of the <b>AccessibleCaret</b> property. */
    public static final String ACCESSIBLE_CARET_PROPERTY = "AccessibleCaret";

    /** The name of the <b>AccessibleChild</b> property. */
    public static final String ACCESSIBLE_CHILD_PROPERTY = "AccessibleChild";

    /** The name of the <b>accessibleComponentBoundsChanged</b> property. */
    public static final String ACCESSIBLE_COMPONENT_BOUNDS_CHANGED = "accessibleComponentBoundsChanged";

    /** The name of the <b>AccessibleDescription</b> property. */
    public static final String ACCESSIBLE_DESCRIPTION_PROPERTY = "AccessibleDescription";

    /** The name of the <b>AccessibleHypertextOffset</b> property. */
    public static final String ACCESSIBLE_HYPERTEXT_OFFSET = "AccessibleHypertextOffset";

    /** The name of the <b>accessibleInvalidateChildren</b> property. */
    public static final String ACCESSIBLE_INVALIDATE_CHILDREN = "accessibleInvalidateChildren";

    /** The name of the <b>AccessibleName</b> property. */
    public static final String ACCESSIBLE_NAME_PROPERTY = "AccessibleName";

    /** The name of the <b>AccessibleSelection</b> property. */
    public static final String ACCESSIBLE_SELECTION_PROPERTY = "AccessibleSelection";

    /** The name of the <b>AccessibleState</b> property. */
    public static final String ACCESSIBLE_STATE_PROPERTY = "AccessibleState";

    /** The name of the <b>accessibleTableCaptionChanged</b> property. */
    public static final String ACCESSIBLE_TABLE_CAPTION_CHANGED = "accessibleTableCaptionChanged";

    /** The name of the <b>accessibleTableColumnDescriptionChanged</b> property. */
    public static final String ACCESSIBLE_TABLE_COLUMN_DESCRIPTION_CHANGED = "accessibleTableColumnDescriptionChanged";

    /** The name of the <b>accessibleTableColumnHeaderChanged</b> property. */
    public static final String ACCESSIBLE_TABLE_COLUMN_HEADER_CHANGED = "accessibleTableColumnHeaderChanged";

    /** The name of the <b>accessibleTableModelChanged</b> property. */
    public static final String ACCESSIBLE_TABLE_MODEL_CHANGED = "accessibleTableModelChanged";

    /** The name of the <b>accessibleTableRowDescriptionChanged</b> property. */
    public static final String ACCESSIBLE_TABLE_ROW_DESCRIPTION_CHANGED = "accessibleTableRowDescriptionChanged";

    /** The name of the <b>accessibleTableRowHeaderChanged</b> property. */
    public static final String ACCESSIBLE_TABLE_ROW_HEADER_CHANGED = "accessibleTableRowHeaderChanged";

    /** The name of the <b>accessibleTableSummaryChanged</b> property. */
    public static final String ACCESSIBLE_TABLE_SUMMARY_CHANGED = "accessibleTableSummaryChanged";

    /** The name of the <b>accessibleTextAttributesChanged</b> property. */
    public static final String ACCESSIBLE_TEXT_ATTRIBUTES_CHANGED = "accessibleTextAttributesChanged";

    /** The name of the <b>AccessibleText</b> property. */
    public static final String ACCESSIBLE_TEXT_PROPERTY = "AccessibleText";

    /** The name of the <b>AccessibleValue</b> property. */
    public static final String ACCESSIBLE_VALUE_PROPERTY = "AccessibleValue";

    /** The name of the <b>AccessibleVisibleData</b> property. */
    public static final String ACCESSIBLE_VISIBLE_DATA_PROPERTY = "AccessibleVisibleData";

    /** The parent in the accessibility tree. */
    protected Accessible accessibleParent = null;

    /** The object's name, if one of its own was set. */
    protected String accessibleName = null;

    /** The object's description, if one of its own was set. */
    protected String accessibleDescription = null;

    /** Whom to notify of the changes. */
    private PropertyChangeSupport accessibleChangeSupport = null;

    /** For subclasses. */
    public AccessibleContext() {
    }

    /**
     * The object's name, short and meant to be read aloud.
     *
     * @return the name, or `null` if it has none
     */
    public String getAccessibleName() {
        return this.accessibleName;
    }

    /** Changes the name and notifies. */
    public void setAccessibleName(String s) {
        String old = this.accessibleName;
        this.accessibleName = s;
        this.firePropertyChange(ACCESSIBLE_NAME_PROPERTY, old, this.accessibleName);
    }

    /**
     * A description longer than the name.
     *
     * @return the description, or `null` if it has none
     */
    public String getAccessibleDescription() {
        return this.accessibleDescription;
    }

    /** Changes the description and notifies. */
    public void setAccessibleDescription(String s) {
        String old = this.accessibleDescription;
        this.accessibleDescription = s;
        this.firePropertyChange(ACCESSIBLE_DESCRIPTION_PROPERTY, old,
                this.accessibleDescription);
    }

    /** What the object is. */
    public abstract AccessibleRole getAccessibleRole();

    /** What condition it is in, now. */
    public abstract AccessibleStateSet getAccessibleStateSet();

    /**
     * The parent in the accessibility tree.
     *
     * @return the parent, or `null` if it is the root
     */
    public Accessible getAccessibleParent() {
        return this.accessibleParent;
    }

    /**
     * Changes the parent.
     *
     * <p>It is only needed when the accessibility tree does **not** match the component tree, which
     * is exactly the case this property exists to solve.
     */
    public void setAccessibleParent(Accessible a) {
        this.accessibleParent = a;
    }

    /** Which child number it is within its parent. */
    public abstract int getAccessibleIndexInParent();

    /** How many accessible children it has. */
    public abstract int getAccessibleChildrenCount();

    /**
     * The `i`-th child.
     *
     * @return the child, or `null` if there are not that many
     */
    public abstract Accessible getAccessibleChild(int i);

    /** What language it is in. */
    public abstract Locale getLocale();

    /**
     * The graphical part, if it has one.
     *
     * @return the component, or `null` if the object is not drawn
     */
    public AccessibleComponent getAccessibleComponent() {
        return null;
    }

    /**
     * The selection, if it has one.
     *
     * @return the selection, or `null` if the object has no children to choose
     */
    public AccessibleSelection getAccessibleSelection() {
        return null;
    }

    /**
     * The text, if it has any.
     *
     * @return the text, or `null` if the object shows no walkable text
     */
    public AccessibleText getAccessibleText() {
        return null;
    }

    /**
     * The editable text, if it has any.
     *
     * @return the text, or `null` if the object cannot be edited
     */
    public AccessibleEditableText getAccessibleEditableText() {
        return null;
    }

    /**
     * The value, if it has one.
     *
     * @return the value, or `null` if the object does not represent a number in a range
     */
    public AccessibleValue getAccessibleValue() {
        return null;
    }

    /**
     * The icons, if it has any.
     *
     * @return the icons, or `null` if the object shows none
     */
    public AccessibleIcon[] getAccessibleIcon() {
        return null;
    }

    /**
     * The actions, if it has any.
     *
     * @return the actions, or `null` if the object does nothing
     */
    public AccessibleAction getAccessibleAction() {
        return null;
    }

    /**
     * The table, if it is one.
     *
     * @return the table, or `null` if the object does not show rows and columns
     */
    public AccessibleTable getAccessibleTable() {
        return null;
    }

    /** The relations with other objects; empty if there are none. */
    public AccessibleRelationSet getAccessibleRelationSet() {
        return new AccessibleRelationSet();
    }

    /** Adds somebody to notify of the changes. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (this.accessibleChangeSupport == null) {
            this.accessibleChangeSupport = new PropertyChangeSupport(this);
        }
        this.accessibleChangeSupport.addPropertyChangeListener(listener);
    }

    /** Removes that listener. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (this.accessibleChangeSupport != null) {
            this.accessibleChangeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
     * Announces that a property changed.
     *
     * <p>It does not announce if the value did not really change: an assistive technology that
     * reacts to every announcement should not have to filter out the ones that say nothing.
     */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
        if (this.accessibleChangeSupport != null) {
            if (oldValue == null && newValue == null) {
                return;
            }
            if (oldValue != null && oldValue.equals(newValue)) {
                return;
            }
            this.accessibleChangeSupport.firePropertyChange(propertyName, oldValue, newValue);
        }
    }
}
