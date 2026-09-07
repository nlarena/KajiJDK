package javax.swing;

/**
 * Un {@link InputMap} atado a un componente.
 *
 * <h2>Por que necesita conocer al componente</h2>
 *
 * <p>Un atajo que vale en toda la ventana lo atiende el componente que lo declaro, aunque el foco
 * este en otro lado. Para eso Swing guarda una tabla de esos atajos por ventana, armada a partir de
 * los componentes que la contienen; cuando uno cambia su tabla, esa tabla de ventana hay que
 * rehacerla. Sin saber de que componente es, no habria a quien avisarle.
 *
 * <p>De ahi tambien la regla del padre: solo puede serlo otro {@code ComponentInputMap} del
 * <em>mismo</em> componente. Encadenar la de un componente con la de otro haria que un atajo se
 * atribuyera a quien no es.
 */
public class ComponentInputMap extends InputMap {

    private JComponent component;

    /**
     * Una tabla para ese componente.
     *
     * @throws IllegalArgumentException si el componente es nulo.
     */
    public ComponentInputMap(JComponent component) {
        this.component = component;
        if (component == null) {
            throw new IllegalArgumentException(
                    "ComponentInputMaps must be associated with a non-null JComponent");
        }
    }

    /**
     * El padre; ver la nota de la clase.
     *
     * @throws IllegalArgumentException si no es de este mismo componente.
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
        avisar();
    }

    /** El componente al que pertenece. */
    public JComponent getComponent() {
        return component;
    }

    public void put(KeyStroke key, Object actionMapKey) {
        super.put(key, actionMapKey);
        avisar();
    }

    public void remove(KeyStroke key) {
        super.remove(key);
        avisar();
    }

    public void clear() {
        int n = size();
        super.clear();
        if (n > 0) {
            avisar();
        }
    }

    /** Le avisa al componente que la tabla de la ventana hay que rehacerla. */
    private void avisar() {
        if (getComponent() != null) {
            getComponent().componentInputMapChanged(this);
        }
    }
}
