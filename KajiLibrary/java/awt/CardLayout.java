package java.awt;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Apila a los hijos como cartas y muestra **uno solo** por vez.
 *
 * <p>Es la distribución de un asistente paso a paso o de un panel de pestañas sin las pestañas: los
 * hijos están todos agregados y ocupan el mismo lugar, y sólo uno es visible.
 *
 * <p>Cada carta se agrega con un **nombre**, y ése es el motivo de que
 * {@link LayoutManager#addLayoutComponent(String, Component)} exista con esa firma rara en la
 * interfaz vieja: esta distribución es prácticamente la única que la usa para algo.
 *
 * <p>Las medidas son las de la carta **más grande**, no las de la que se está mostrando. Es lo
 * correcto: si el contenedor se ajustara a la carta visible, cambiar de carta lo haría saltar de
 * tamaño.
 */
public class CardLayout implements LayoutManager2, Serializable {

    private static final long serialVersionUID = -4328196481005934313L;

    // `java.util.List` va con el nombre entero: en este paquete `List` es el widget de AWT, y un
    // `import java.util.List` --que el JLS admite y tapa al homonimo del paquete-- nuestro javac
    // todavia no lo resuelve al derecho (hallazgo #493).
    private final java.util.List<Component> componentes = new ArrayList<Component>();
    private final java.util.List<String> nombres = new ArrayList<String>();
    private int hgap;
    private int vgap;
    private int actual;

    /** Sin margen alrededor de las cartas. */
    public CardLayout() {
        this(0, 0);
    }

    /** Con los márgenes dados. */
    public CardLayout(int hgap, int vgap) {
        this.hgap = hgap;
        this.vgap = vgap;
    }

    /** El margen horizontal. */
    public int getHgap() {
        return this.hgap;
    }

    /** Cambia el margen horizontal. */
    public void setHgap(int hgap) {
        this.hgap = hgap;
    }

    /** El margen vertical. */
    public int getVgap() {
        return this.vgap;
    }

    /** Cambia el margen vertical. */
    public void setVgap(int vgap) {
        this.vgap = vgap;
    }

    /**
     * Agrega una carta con ese nombre.
     *
     * <p>La primera que se agrega es la que se muestra; las demás nacen ocultas.
     *
     * @throws IllegalArgumentException si el nombre no es una cadena
     */
    public void addLayoutComponent(Component comp, Object constraints) {
        synchronized (comp.getTreeLock()) {
            if (constraints == null) {
                this.agregar(comp, "");
                return;
            }
            if (!(constraints instanceof String)) {
                throw new IllegalArgumentException(
                        "cannot add to layout: constraint must be a string");
            }
            this.agregar(comp, (String) constraints);
        }
    }

    /** Guarda la carta y esconde todas menos la que toca. */
    private void agregar(Component comp, String name) {
        if (!this.componentes.isEmpty()) {
            comp.setVisible(false);
        }
        for (int i = 0; i < this.nombres.size(); i++) {
            if (this.nombres.get(i).equals(name)) {
                this.componentes.get(i).setVisible(false);
            }
        }
        this.componentes.add(comp);
        this.nombres.add(name);
        if (this.componentes.size() == 1) {
            comp.setVisible(true);
            this.actual = 0;
        }
    }

    /**
     * Agrega una carta con ese nombre.
     *
     * @deprecated es del modelo de 1.0. Usar {@link #addLayoutComponent(Component, Object)}.
     */
    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
        synchronized (comp.getTreeLock()) {
            this.agregar(comp, name == null ? "" : name);
        }
    }

    /**
     * Saca esa carta.
     *
     * <p>Si era la que se estaba mostrando, se muestra la primera que quede: dejar el contenedor sin
     * ninguna carta visible sería un panel en blanco sin motivo.
     */
    public void removeLayoutComponent(Component comp) {
        synchronized (comp.getTreeLock()) {
            int i = this.componentes.indexOf(comp);
            if (i < 0) {
                return;
            }
            boolean eraLaVisible = comp.isVisible();
            this.componentes.remove(i);
            this.nombres.remove(i);
            if (eraLaVisible && !this.componentes.isEmpty()) {
                this.actual = 0;
                this.componentes.get(0).setVisible(true);
            }
        }
    }

    /** La medida de la carta más grande, más los márgenes. */
    public Dimension preferredLayoutSize(Container parent) {
        return this.medir(parent, true);
    }

    /** Lo mismo, con las medidas mínimas. */
    public Dimension minimumLayoutSize(Container parent) {
        return this.medir(parent, false);
    }

    /** El máximo de todas las cartas, no el de la visible. */
    private Dimension medir(Container parent, boolean preferida) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            int w = 0;
            int h = 0;
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                Dimension d = preferida ? comp.getPreferredSize() : comp.getMinimumSize();
                w = Math.max(w, d.width);
                h = Math.max(h, d.height);
            }
            return new Dimension(insets.left + insets.right + w + this.hgap * 2,
                    insets.top + insets.bottom + h + this.vgap * 2);
        }
    }

    /** Sin tope: la carta visible aprovecha todo lo que le den. */
    public Dimension maximumLayoutSize(Container target) {
        return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    /** Centrado. */
    public float getLayoutAlignmentX(Container parent) {
        return 0.5f;
    }

    /** Centrado. */
    public float getLayoutAlignmentY(Container parent) {
        return 0.5f;
    }

    /** No guarda cuentas entre llamadas. */
    public void invalidateLayout(Container target) {
    }

    /** Le da a la carta visible todo el espacio disponible menos los márgenes. */
    public void layoutContainer(Container parent) {
        synchronized (parent.getTreeLock()) {
            Insets insets = parent.getInsets();
            int ncomponents = parent.getComponentCount();
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                if (comp.isVisible()) {
                    comp.setBounds(this.hgap + insets.left, this.vgap + insets.top,
                            parent.getWidth() - (this.hgap * 2 + insets.left + insets.right),
                            parent.getHeight() - (this.vgap * 2 + insets.top + insets.bottom));
                }
            }
        }
    }

    /** Muestra la carta de esa posición y esconde la anterior. */
    private void mostrar(Container parent, int indice) {
        synchronized (parent.getTreeLock()) {
            if (this.componentes.isEmpty()) {
                return;
            }
            int n = this.componentes.size();
            int i = ((indice % n) + n) % n;
            for (int j = 0; j < n; j++) {
                this.componentes.get(j).setVisible(j == i);
            }
            this.actual = i;
            parent.validate();
        }
    }

    /** Muestra la primera carta. */
    public void first(Container parent) {
        this.mostrar(parent, 0);
    }

    /**
     * Muestra la siguiente.
     *
     * <p>Después de la última vuelve a la primera: es un ciclo, no una lista con final.
     */
    public void next(Container parent) {
        this.mostrar(parent, this.actual + 1);
    }

    /** Muestra la anterior; antes de la primera va a la última. */
    public void previous(Container parent) {
        this.mostrar(parent, this.actual - 1);
    }

    /** Muestra la última. */
    public void last(Container parent) {
        this.mostrar(parent, this.componentes.size() - 1);
    }

    /**
     * Muestra la carta que se agregó con ese nombre.
     *
     * <p>Si no hay ninguna con ese nombre no pasa nada: es lo que hace el JDK, y tiene sentido —
     * pedir una carta que no está no debería romper la navegación.
     */
    public void show(Container parent, String name) {
        synchronized (parent.getTreeLock()) {
            for (int i = 0; i < this.nombres.size(); i++) {
                if (this.nombres.get(i).equals(name)) {
                    this.mostrar(parent, i);
                    return;
                }
            }
        }
    }

    public String toString() {
        return this.getClass().getName() + "[hgap=" + this.hgap + ",vgap=" + this.vgap + "]";
    }
}
