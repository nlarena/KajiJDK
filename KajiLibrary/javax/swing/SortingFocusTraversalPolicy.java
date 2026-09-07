package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Recorre el foco en el orden que diga un comparador, no en el orden en que se agregaron.
 *
 * <h2>Por que hace falta ordenar</h2>
 *
 * <p>La politica de AWT recorre los componentes en el orden en que estan en el contenedor, que es el
 * orden en que alguien los agrego. Eso casi nunca coincide con como se ven: un formulario armado con
 * una grilla puede tener los campos agregados por columnas y verse por filas, y entonces el
 * tabulador salta de arriba a abajo cuando el usuario espera izquierda a derecha.
 *
 * <p>Esta politica ordena antes de recorrer. Con que criterio lo decide el {@link Comparator}, y el
 * unico que usa Swing de verdad ordena por posicion en pantalla: es {@link LayoutFocusTraversalPolicy}.
 *
 * <h2>Bajar al ciclo de adentro</h2>
 *
 * <p>Un contenedor que es raiz de su propio ciclo -- una ventana interna, un panel marcado como tal
 * -- normalmente se saltea entero: el tabulador pasa de largo. Con
 * {@link #setImplicitDownCycleTraversal} prendido -- que es lo de omision -- el foco <em>entra</em> y
 * recorre lo de adentro antes de seguir.
 *
 * <h2>Que componentes entran</h2>
 *
 * <p>{@link #accept} decide. Uno visible, habilitado y que puede recibir el foco; los demas se
 * saltean. Una subclase la ajusta para saltear tambien los que estan tapados o de solo lectura.
 */
public class SortingFocusTraversalPolicy extends InternalFrameFocusTraversalPolicy {

    private Comparator<? super Component> comparator;
    private boolean implicitDownCycleTraversal = true;

    /** Sin comparador; una subclase tiene que ponerlo antes de usarla. */
    protected SortingFocusTraversalPolicy() {
    }

    /** Con ese criterio de orden. */
    public SortingFocusTraversalPolicy(Comparator<? super Component> comparator) {
        this.comparator = comparator;
    }

    /**
     * Los componentes del ciclo, ya ordenados y filtrados.
     *
     * <p>Se arma la lista entera y despues se busca: recorrer y ordenar a la vez seria mas rapido y
     * mucho mas dificil de entender, y esta lista tiene el tamano de una pantalla.
     */
    private List<Component> ciclo(Container aContainer) {
        List<Component> lista = new ArrayList<Component>();
        juntar(aContainer, lista);
        Comparator<? super Component> c = getComparator();
        if (c != null) {
            java.util.Collections.sort(lista, c);
        }
        return lista;
    }

    private void juntar(Container padre, List<Component> lista) {
        int n = padre.getComponentCount();
        for (int i = 0; i < n; i++) {
            Component comp = padre.getComponent(i);
            if (comp instanceof Container) {
                Container cont = (Container) comp;
                if (!cont.isFocusCycleRoot() && !cont.isFocusTraversalPolicyProvider()) {
                    // No es raiz de su propio ciclo: sus hijos son parte de este.
                    juntar(cont, lista);
                    continue;
                }
                if (getImplicitDownCycleTraversal() && accept(cont)) {
                    lista.add(cont);
                    continue;
                }
                if (getImplicitDownCycleTraversal()) {
                    juntar(cont, lista);
                    continue;
                }
            }
            if (accept(comp)) {
                lista.add(comp);
            }
        }
    }

    /** El contenedor mas alto que sea proveedor de politica, entre ese y el componente. */
    Container getTopmostProvider(Container focusCycleRoot, Component aComponent) {
        Container aCont = aComponent.getParent();
        Container ftp = null;
        while (aCont != focusCycleRoot && aCont != null) {
            if (aCont.isFocusTraversalPolicyProvider()) {
                ftp = aCont;
            }
            aCont = aCont.getParent();
        }
        if (aCont == null) {
            return null;
        }
        return ftp;
    }

    /**
     * El que sigue.
     *
     * <p>Al llegar al final vuelve al principio: un ciclo de foco es un ciclo.
     *
     * @throws IllegalArgumentException si alguno es nulo
     */
    public Component getComponentAfter(Container aContainer, Component aComponent) {
        exigir(aContainer, aComponent);
        exigirRaiz(aContainer);
        List<Component> lista = ciclo(aContainer);
        int i = lista.indexOf(aComponent);
        if (i < 0) {
            return getFirstComponent(aContainer);
        }
        if (i == lista.size() - 1) {
            return lista.isEmpty() ? null : lista.get(0);
        }
        return lista.get(i + 1);
    }

    /**
     * El anterior; al llegar al principio salta al final.
     *
     * @throws IllegalArgumentException si alguno es nulo
     */
    public Component getComponentBefore(Container aContainer, Component aComponent) {
        exigir(aContainer, aComponent);
        exigirRaiz(aContainer);
        List<Component> lista = ciclo(aContainer);
        int i = lista.indexOf(aComponent);
        if (i < 0) {
            return getLastComponent(aContainer);
        }
        if (i == 0) {
            return lista.isEmpty() ? null : lista.get(lista.size() - 1);
        }
        return lista.get(i - 1);
    }

    /**
     * @throws IllegalArgumentException si el contenedor es nulo
     */
    public Component getFirstComponent(Container aContainer) {
        exigirContenedor(aContainer);
        List<Component> lista = ciclo(aContainer);
        return lista.isEmpty() ? null : lista.get(0);
    }

    /**
     * @throws IllegalArgumentException si el contenedor es nulo
     */
    public Component getLastComponent(Container aContainer) {
        exigirContenedor(aContainer);
        List<Component> lista = ciclo(aContainer);
        return lista.isEmpty() ? null : lista.get(lista.size() - 1);
    }

    /**
     * Donde va el foco al entrar al ciclo: el primero.
     *
     * @throws IllegalArgumentException si el contenedor es nulo
     */
    public Component getDefaultComponent(Container aContainer) {
        return getFirstComponent(aContainer);
    }

    private static void exigir(Container aContainer, Component aComponent) {
        if (aContainer == null || aComponent == null) {
            throw new IllegalArgumentException("aContainer and aComponent cannot be null");
        }
    }

    /**
     * Exige que el contenedor sea raiz de un ciclo de foco.
     *
     * <p>Solo lo piden {@link #getComponentAfter} y {@link #getComponentBefore}: preguntar "que
     * sigue" solo tiene sentido dentro de un ciclo, mientras que "cual es el primero" se puede
     * contestar de cualquier contenedor. La asimetria es del JDK y esta medida.
     *
     * @throws IllegalArgumentException si no lo es
     */
    private static void exigirRaiz(Container aContainer) {
        if (!aContainer.isFocusCycleRoot() && !aContainer.isFocusTraversalPolicyProvider()) {
            throw new IllegalArgumentException("aContainer should be focus cycle root or "
                    + "focus traversal policy provider");
        }
    }

    private static void exigirContenedor(Container aContainer) {
        if (aContainer == null) {
            throw new IllegalArgumentException("aContainer cannot be null");
        }
    }

    /** Si el foco entra a los ciclos de adentro; ver la nota de la clase. */
    public void setImplicitDownCycleTraversal(boolean implicitDownCycleTraversal) {
        this.implicitDownCycleTraversal = implicitDownCycleTraversal;
    }

    public boolean getImplicitDownCycleTraversal() {
        return implicitDownCycleTraversal;
    }

    /** El criterio de orden; ver la nota de la clase. */
    protected void setComparator(Comparator<? super Component> comparator) {
        this.comparator = comparator;
    }

    protected Comparator<? super Component> getComparator() {
        return comparator;
    }

    /**
     * Si ese componente entra en el recorrido.
     *
     * <p>Visible, habilitado, y que acepte el foco. Los tres hacen falta: un componente escondido no
     * se puede enfocar aunque lo acepte, y uno apagado tampoco.
     */
    protected boolean accept(Component aComponent) {
        if (!aComponent.isVisible() || !aComponent.isDisplayable()
                || !aComponent.isEnabled() || !aComponent.isFocusable()) {
            return false;
        }
        return true;
    }
}
