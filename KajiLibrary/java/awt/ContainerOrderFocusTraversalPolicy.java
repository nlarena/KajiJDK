package java.awt;

import java.io.Serializable;

/**
 * Recorre el foco en el **orden en que se agregaron** los hijos, entrando en cada contenedor.
 *
 * <p>Es la política más simple que sirve para algo: un recorrido en profundidad del árbol, de
 * izquierda a derecha, que es exactamente el orden de los `add`. Si ese orden coincide con el orden
 * visual —y suele coincidir, porque los componentes se agregan según se leen— el Tab hace lo que el
 * usuario espera sin que nadie configure nada.
 *
 * <p>Lo que la hace interesante es {@link #setImplicitDownCycleTraversal}: con `true`, que es lo de
 * fábrica, al llegar a una **raíz de ciclo** el recorrido **entra** en ella en vez de saltearla. Con
 * `false`, la raíz se visita como un componente más y hay que entrar a mano. Es la diferencia entre
 * un Tab que atraviesa todo el árbol y uno que se queda en el nivel actual.
 *
 * <p><strong>Los cinco métodos tienen precondiciones que conviene tener presentes</strong>, porque
 * fallan de dos maneras distintas y por motivos distintos. El contenedor que se pasa tiene que ser
 * una **raíz de ciclo** o un **proveedor de política** —si no, la pregunta no tiene sentido: no se
 * recorre un contenedor cualquiera sino un ciclo— y eso se rechaza con `IllegalArgumentException`.
 * En cambio, si el contenedor no está **visible y mostrable**, la respuesta es `null`: no es un
 * error preguntar por un ciclo que todavía no se ve, simplemente no hay a quién darle el foco. Sin
 * pantalla nada es mostrable, así que sin pantalla estos métodos devuelven `null` siempre.
 */
public class ContainerOrderFocusTraversalPolicy extends FocusTraversalPolicy
        implements Serializable {

    private static final long serialVersionUID = 486933713763926351L;

    /** Si al llegar a una raíz de ciclo se entra en ella. */
    private boolean implicitDownCycleTraversal = true;

    /** Una política de orden de contenedor, que entra en los ciclos. */
    public ContainerOrderFocusTraversalPolicy() {
    }

    /**
     * El siguiente de `aComponent` dentro de `aContainer`.
     *
     * @return el siguiente, o el primero si `aComponent` era el último —el recorrido es un ciclo—, o
     *     `null` si no hay ninguno que acepte el foco
     * @throws IllegalArgumentException si alguno es `null` o si `aComponent` no está en `aContainer`
     */
    public Component getComponentAfter(Container aContainer, Component aComponent) {
        this.comprobar(aContainer, aComponent);
        if (!this.recorrible(aContainer)) {
            return null;
        }
        java.util.ArrayList<Component> orden = new java.util.ArrayList<Component>();
        this.recorrer(aContainer, orden, aComponent);
        int i = orden.indexOf(aComponent);
        if (i < 0) {
            return this.getFirstComponent(aContainer);
        }
        for (int j = i + 1; j < orden.size(); j++) {
            Component c = orden.get(j);
            if (this.accept(c)) {
                return this.bajarSiCorresponde(c);
            }
        }
        return this.getFirstComponent(aContainer);
    }

    /**
     * El anterior de `aComponent` dentro de `aContainer`.
     *
     * @return el anterior, o el último si `aComponent` era el primero, o `null` si no hay ninguno
     * @throws IllegalArgumentException si alguno es `null` o si `aComponent` no está en `aContainer`
     */
    public Component getComponentBefore(Container aContainer, Component aComponent) {
        this.comprobar(aContainer, aComponent);
        if (!this.recorrible(aContainer)) {
            return null;
        }
        java.util.ArrayList<Component> orden = new java.util.ArrayList<Component>();
        this.recorrer(aContainer, orden, aComponent);
        int i = orden.indexOf(aComponent);
        if (i < 0) {
            return this.getLastComponent(aContainer);
        }
        for (int j = i - 1; j >= 0; j--) {
            Component c = orden.get(j);
            if (this.accept(c)) {
                return c;
            }
        }
        return this.getLastComponent(aContainer);
    }

    /**
     * El primero del recorrido.
     *
     * @return el primero, o `null` si ninguno acepta el foco
     * @throws IllegalArgumentException si el contenedor es `null`
     */
    public Component getFirstComponent(Container aContainer) {
        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }
        if (!this.recorrible(aContainer)) {
            return null;
        }
        java.util.ArrayList<Component> orden = new java.util.ArrayList<Component>();
        this.recorrer(aContainer, orden, null);
        for (int i = 0; i < orden.size(); i++) {
            if (this.accept(orden.get(i))) {
                return this.bajarSiCorresponde(orden.get(i));
            }
        }
        return null;
    }

    /**
     * El último del recorrido.
     *
     * @return el último, o `null` si ninguno acepta el foco
     * @throws IllegalArgumentException si el contenedor es `null`
     */
    public Component getLastComponent(Container aContainer) {
        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }
        if (!this.recorrible(aContainer)) {
            return null;
        }
        java.util.ArrayList<Component> orden = new java.util.ArrayList<Component>();
        this.recorrer(aContainer, orden, null);
        for (int i = orden.size() - 1; i >= 0; i--) {
            if (this.accept(orden.get(i))) {
                return orden.get(i);
            }
        }
        return null;
    }

    /**
     * A quién le toca el foco cuando el ciclo se hace visible.
     *
     * <p>Es el primero: esta política no distingue "el primero" de "el de arranque", que es
     * justamente lo que una política más elaborada sí hace.
     */
    public Component getDefaultComponent(Container aContainer) {
        return this.getFirstComponent(aContainer);
    }

    /**
     * Junta a los hijos en orden, entrando en los contenedores.
     *
     * <p>Un contenedor invisible o deshabilitado no se recorre: sus hijos tampoco podrían recibir el
     * foco, y bajar a mirarlos uno por uno sería recorrer un subárbol entero para descartarlo.
     *
     * @param parar si aparece, se deja de bajar en él —es la raíz de ciclo desde la que se pregunta—
     */
    private void recorrer(Container cont, java.util.ArrayList<Component> out, Component parar) {
        Component[] hijos = cont.getComponents();
        for (int i = 0; i < hijos.length; i++) {
            Component c = hijos[i];
            out.add(c);
            if (!(c instanceof Container)) {
                continue;
            }
            Container k = (Container) c;
            if (!k.isVisible() || !k.isEnabled()) {
                continue;
            }
            // Una raíz de ciclo no se abre acá: sus hijos son de **su** recorrido, no de éste. Con
            // el descenso implícito prendido, entrar en ella es cosa de `bajarSiCorresponde`.
            if (k.isFocusCycleRoot() && k != parar) {
                continue;
            }
            this.recorrer(k, out, parar);
        }
    }

    /**
     * Si el componente es una raíz de ciclo y el descenso implícito está prendido, devuelve a **quién
     * le toca adentro** en vez de a la raíz misma.
     */
    private Component bajarSiCorresponde(Component c) {
        if (!this.implicitDownCycleTraversal || !(c instanceof Container)) {
            return c;
        }
        Container k = (Container) c;
        if (!k.isFocusCycleRoot()) {
            return c;
        }
        FocusTraversalPolicy p = k.getFocusTraversalPolicy();
        Component dentro = p == null ? null : p.getDefaultComponent(k);
        return dentro != null ? dentro : c;
    }

    /**
     * Comprueba los argumentos de los métodos que toman un componente.
     *
     * <p>Las dos condiciones que exige son las del JDK: que el contenedor sea una raíz de ciclo o un
     * proveedor de política, y —si es raíz— que el componente pertenezca a **ese** ciclo. Preguntar
     * "quién va después de éste" sobre un ciclo del que el componente no forma parte no tiene
     * respuesta, y contestar algo sería peor que rechazar la pregunta.
     */
    private void comprobar(Container aContainer, Component aComponent) {
        if (aContainer == null || aComponent == null) {
            throw new IllegalArgumentException("aContainer and aComponent cannot be null");
        }
        if (!aContainer.isFocusTraversalPolicyProvider() && !aContainer.isFocusCycleRoot()) {
            throw new IllegalArgumentException(
                    "aContainer should be focus cycle root or focus traversal policy provider");
        }
        if (aContainer.isFocusCycleRoot() && !aComponent.isFocusCycleRoot(aContainer)) {
            throw new IllegalArgumentException("aContainer is not a focus cycle root of aComponent");
        }
    }

    /**
     * Si el contenedor se puede recorrer ahora.
     *
     * <p>Un ciclo que no se ve no tiene a quién darle el foco. Es una respuesta, no un error, y por
     * eso los cinco métodos devuelven `null` en vez de tirar.
     */
    private boolean recorrible(Container aContainer) {
        synchronized (aContainer.getTreeLock()) {
            return aContainer.isVisible() && aContainer.isDisplayable();
        }
    }

    /**
     * Si al llegar a una raíz de ciclo el recorrido entra en ella.
     *
     * <p>Con `false`, la raíz se devuelve como un componente más y quien recorre decide si entra.
     */
    public void setImplicitDownCycleTraversal(boolean implicitDownCycleTraversal) {
        this.implicitDownCycleTraversal = implicitDownCycleTraversal;
    }

    /** Si entra en las raíces de ciclo; de fábrica, `true`. */
    public boolean getImplicitDownCycleTraversal() {
        return this.implicitDownCycleTraversal;
    }

    /**
     * Si ese componente entra en el recorrido.
     *
     * <p>Tiene que estar visible, mostrable, habilitado y admitir el foco. Una subclase que quiera
     * saltear componentes redefine esto y nada más.
     */
    protected boolean accept(Component aComponent) {
        if (!aComponent.isVisible() || !aComponent.isDisplayable() || !aComponent.isEnabled()
                || !aComponent.isFocusable()) {
            return false;
        }
        return true;
    }
}
