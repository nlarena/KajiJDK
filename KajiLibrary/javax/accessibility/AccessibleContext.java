package javax.accessibility;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Locale;

/**
 * Toda la información de accesibilidad de un objeto, en un solo lugar.
 *
 * <p>Es el corazón del paquete y su diseño más discutible a primera vista: una clase con cincuenta
 * miembros, la mitad de los cuales devuelven `null`. La razón es que **no todo objeto es todo**. Un
 * botón no tiene texto que recorrer ni filas; un campo de texto no tiene hijos que elegir. En vez de
 * una jerarquía de interfaces que multiplicara las combinaciones, se pregunta:
 * {@code getAccessibleText()} devuelve `null` si el objeto no muestra texto, y algo si lo muestra.
 *
 * <p>Ese `null` no es un vacío: es la respuesta. "Este objeto no es texto" es información, y es la
 * que le permite a una ayuda técnica saber qué preguntas tienen sentido.
 *
 * <p>La otra mitad de la clase son las **notificaciones**. Una ayuda técnica no puede estar
 * consultando el estado todo el tiempo, así que el contexto avisa cuando algo cambia, con el mismo
 * mecanismo de propiedades de JavaBeans. Los nombres de propiedad son las constantes de arriba.
 */
public abstract class AccessibleContext {

    /** El nombre de la propiedad <b>accessibleActionProperty</b>. */
    public static final String ACCESSIBLE_ACTION_PROPERTY = "accessibleActionProperty";

    /** El nombre de la propiedad <b>AccessibleActiveDescendant</b>. */
    public static final String ACCESSIBLE_ACTIVE_DESCENDANT_PROPERTY = "AccessibleActiveDescendant";

    /** El nombre de la propiedad <b>AccessibleCaret</b>. */
    public static final String ACCESSIBLE_CARET_PROPERTY = "AccessibleCaret";

    /** El nombre de la propiedad <b>AccessibleChild</b>. */
    public static final String ACCESSIBLE_CHILD_PROPERTY = "AccessibleChild";

    /** El nombre de la propiedad <b>accessibleComponentBoundsChanged</b>. */
    public static final String ACCESSIBLE_COMPONENT_BOUNDS_CHANGED = "accessibleComponentBoundsChanged";

    /** El nombre de la propiedad <b>AccessibleDescription</b>. */
    public static final String ACCESSIBLE_DESCRIPTION_PROPERTY = "AccessibleDescription";

    /** El nombre de la propiedad <b>AccessibleHypertextOffset</b>. */
    public static final String ACCESSIBLE_HYPERTEXT_OFFSET = "AccessibleHypertextOffset";

    /** El nombre de la propiedad <b>accessibleInvalidateChildren</b>. */
    public static final String ACCESSIBLE_INVALIDATE_CHILDREN = "accessibleInvalidateChildren";

    /** El nombre de la propiedad <b>AccessibleName</b>. */
    public static final String ACCESSIBLE_NAME_PROPERTY = "AccessibleName";

    /** El nombre de la propiedad <b>AccessibleSelection</b>. */
    public static final String ACCESSIBLE_SELECTION_PROPERTY = "AccessibleSelection";

    /** El nombre de la propiedad <b>AccessibleState</b>. */
    public static final String ACCESSIBLE_STATE_PROPERTY = "AccessibleState";

    /** El nombre de la propiedad <b>accessibleTableCaptionChanged</b>. */
    public static final String ACCESSIBLE_TABLE_CAPTION_CHANGED = "accessibleTableCaptionChanged";

    /** El nombre de la propiedad <b>accessibleTableColumnDescriptionChanged</b>. */
    public static final String ACCESSIBLE_TABLE_COLUMN_DESCRIPTION_CHANGED = "accessibleTableColumnDescriptionChanged";

    /** El nombre de la propiedad <b>accessibleTableColumnHeaderChanged</b>. */
    public static final String ACCESSIBLE_TABLE_COLUMN_HEADER_CHANGED = "accessibleTableColumnHeaderChanged";

    /** El nombre de la propiedad <b>accessibleTableModelChanged</b>. */
    public static final String ACCESSIBLE_TABLE_MODEL_CHANGED = "accessibleTableModelChanged";

    /** El nombre de la propiedad <b>accessibleTableRowDescriptionChanged</b>. */
    public static final String ACCESSIBLE_TABLE_ROW_DESCRIPTION_CHANGED = "accessibleTableRowDescriptionChanged";

    /** El nombre de la propiedad <b>accessibleTableRowHeaderChanged</b>. */
    public static final String ACCESSIBLE_TABLE_ROW_HEADER_CHANGED = "accessibleTableRowHeaderChanged";

    /** El nombre de la propiedad <b>accessibleTableSummaryChanged</b>. */
    public static final String ACCESSIBLE_TABLE_SUMMARY_CHANGED = "accessibleTableSummaryChanged";

    /** El nombre de la propiedad <b>accessibleTextAttributesChanged</b>. */
    public static final String ACCESSIBLE_TEXT_ATTRIBUTES_CHANGED = "accessibleTextAttributesChanged";

    /** El nombre de la propiedad <b>AccessibleText</b>. */
    public static final String ACCESSIBLE_TEXT_PROPERTY = "AccessibleText";

    /** El nombre de la propiedad <b>AccessibleValue</b>. */
    public static final String ACCESSIBLE_VALUE_PROPERTY = "AccessibleValue";

    /** El nombre de la propiedad <b>AccessibleVisibleData</b>. */
    public static final String ACCESSIBLE_VISIBLE_DATA_PROPERTY = "AccessibleVisibleData";

    /** El padre en el árbol de accesibilidad. */
    protected Accessible accessibleParent = null;

    /** El nombre del objeto, si se le puso uno propio. */
    protected String accessibleName = null;

    /** La descripción del objeto, si se le puso una propia. */
    protected String accessibleDescription = null;

    /** A quién avisarle de los cambios. */
    private PropertyChangeSupport accessibleChangeSupport = null;

    /** Para las subclases. */
    public AccessibleContext() {
    }

    /**
     * El nombre del objeto, corto y para leer en voz alta.
     *
     * @return el nombre, o `null` si no tiene
     */
    public String getAccessibleName() {
        return this.accessibleName;
    }

    /** Cambia el nombre y avisa. */
    public void setAccessibleName(String s) {
        String viejo = this.accessibleName;
        this.accessibleName = s;
        this.firePropertyChange(ACCESSIBLE_NAME_PROPERTY, viejo, this.accessibleName);
    }

    /**
     * Una descripción más larga que el nombre.
     *
     * @return la descripción, o `null` si no tiene
     */
    public String getAccessibleDescription() {
        return this.accessibleDescription;
    }

    /** Cambia la descripción y avisa. */
    public void setAccessibleDescription(String s) {
        String viejo = this.accessibleDescription;
        this.accessibleDescription = s;
        this.firePropertyChange(ACCESSIBLE_DESCRIPTION_PROPERTY, viejo,
                this.accessibleDescription);
    }

    /** Qué es el objeto. */
    public abstract AccessibleRole getAccessibleRole();

    /** En qué condición está, ahora. */
    public abstract AccessibleStateSet getAccessibleStateSet();

    /**
     * El padre en el árbol de accesibilidad.
     *
     * @return el padre, o `null` si es la raíz
     */
    public Accessible getAccessibleParent() {
        return this.accessibleParent;
    }

    /**
     * Cambia el padre.
     *
     * <p>Sólo hace falta cuando el árbol de accesibilidad **no** coincide con el de componentes, que
     * es justamente el caso que esta propiedad existe para resolver.
     */
    public void setAccessibleParent(Accessible a) {
        this.accessibleParent = a;
    }

    /** Qué número de hijo es dentro de su padre. */
    public abstract int getAccessibleIndexInParent();

    /** Cuántos hijos accesibles tiene. */
    public abstract int getAccessibleChildrenCount();

    /**
     * El `i`-ésimo hijo.
     *
     * @return el hijo, o `null` si no hay tantos
     */
    public abstract Accessible getAccessibleChild(int i);

    /** En qué idioma está. */
    public abstract Locale getLocale();

    /**
     * La parte gráfica, si la tiene.
     *
     * @return el componente, o `null` si el objeto no se dibuja
     */
    public AccessibleComponent getAccessibleComponent() {
        return null;
    }

    /**
     * La selección, si la tiene.
     *
     * @return la selección, o `null` si el objeto no tiene hijos que elegir
     */
    public AccessibleSelection getAccessibleSelection() {
        return null;
    }

    /**
     * El texto, si lo tiene.
     *
     * @return el texto, o `null` si el objeto no muestra texto recorrible
     */
    public AccessibleText getAccessibleText() {
        return null;
    }

    /**
     * El texto editable, si lo tiene.
     *
     * @return el texto, o `null` si el objeto no se puede editar
     */
    public AccessibleEditableText getAccessibleEditableText() {
        return null;
    }

    /**
     * El valor, si lo tiene.
     *
     * @return el valor, o `null` si el objeto no representa un número en un rango
     */
    public AccessibleValue getAccessibleValue() {
        return null;
    }

    /**
     * Los íconos, si los tiene.
     *
     * @return los íconos, o `null` si el objeto no muestra ninguno
     */
    public AccessibleIcon[] getAccessibleIcon() {
        return null;
    }

    /**
     * Las acciones, si las tiene.
     *
     * @return las acciones, o `null` si el objeto no hace nada
     */
    public AccessibleAction getAccessibleAction() {
        return null;
    }

    /**
     * La tabla, si lo es.
     *
     * @return la tabla, o `null` si el objeto no muestra filas y columnas
     */
    public AccessibleTable getAccessibleTable() {
        return null;
    }

    /** Las relaciones con otros objetos; vacío si no hay ninguna. */
    public AccessibleRelationSet getAccessibleRelationSet() {
        return new AccessibleRelationSet();
    }

    /** Suma alguien a quien avisarle de los cambios. */
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        if (this.accessibleChangeSupport == null) {
            this.accessibleChangeSupport = new PropertyChangeSupport(this);
        }
        this.accessibleChangeSupport.addPropertyChangeListener(listener);
    }

    /** Saca a ese oyente. */
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        if (this.accessibleChangeSupport != null) {
            this.accessibleChangeSupport.removePropertyChangeListener(listener);
        }
    }

    /**
     * Avisa que cambió una propiedad.
     *
     * <p>No avisa si el valor no cambió de verdad: una ayuda técnica que reaccione a cada aviso no
     * debería tener que filtrar los que no dicen nada.
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
