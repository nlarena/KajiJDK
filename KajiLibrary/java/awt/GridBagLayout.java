package java.awt;

import java.io.Serializable;
import java.util.Hashtable;

/**
 * Una grilla de celdas **de tamaño desigual**, donde cada hijo dice cuántas celdas ocupa y cómo se
 * comporta cuando sobra espacio.
 *
 * <p>Es la distribución más poderosa de AWT y la más difícil de usar, y las dos cosas vienen del
 * mismo lugar: cada hijo trae un {@link GridBagConstraints} con once decisiones. Vale la pena
 * separarlas en tres grupos.
 *
 * <ul>
 *   <li><strong>dónde va</strong>: {@code gridx}, {@code gridy}, y cuántas celdas ocupa con
 *       {@code gridwidth} y {@code gridheight};
 *   <li><strong>qué pasa cuando sobra espacio</strong>: {@code weightx} y {@code weighty} dicen qué
 *       fracción del sobrante le toca a su fila o columna;
 *   <li><strong>qué hace con el espacio que le tocó</strong>: {@code fill} si se estira,
 *       {@code anchor} si no.
 * </ul>
 *
 * <p>El punto que más confunde es que **el peso es de la fila o la columna, no del componente**. Un
 * hijo con peso 1 no crece: hace crecer a su columna, y recién ahí `fill` decide si él la llena o
 * queda centrado en ella. Un componente con peso y sin `fill` se queda de su tamaño en el medio de
 * una columna enorme, que es el resultado desconcertante con el que todo el mundo se topa la primera
 * vez.
 *
 * <p>Los métodos vienen en pares que sólo se diferencian por la mayúscula —{@code getLayoutInfo} y
 * {@code GetLayoutInfo}— y no es un error: los de mayúscula son de 1.1 y quedaron por
 * compatibilidad. Los dos hacen lo mismo.
 */
public class GridBagLayout implements LayoutManager2, Serializable {

    private static final long serialVersionUID = 8838754796412211005L;

    /** El tamaño máximo de la grilla. */
    protected static final int MAXGRIDSIZE = 512;

    /** El menor tamaño posible de la grilla. */
    protected static final int MINSIZE = 1;

    /** La marca para pedir las medidas preferidas en vez de las mínimas. */
    protected static final int PREFERREDSIZE = 2;

    /** Las restricciones de cada hijo. */
    protected Hashtable<Component, GridBagConstraints> comptable =
            new Hashtable<Component, GridBagConstraints>();

    /** Lo que se le da a un hijo que se agrega sin restricciones. */
    protected GridBagConstraints defaultConstraints = new GridBagConstraints();

    /** La grilla calculada, o `null` si hay que recalcularla. */
    protected GridBagLayoutInfo layoutInfo;

    /** Anchos mínimos por columna, si se quieren imponer desde afuera. */
    public int[] columnWidths;

    /** Altos mínimos por fila. */
    public int[] rowHeights;

    /** Pesos mínimos por columna. */
    public double[] columnWeights;

    /** Pesos mínimos por fila. */
    public double[] rowWeights;

    /** Una distribución vacía. */
    public GridBagLayout() {
    }

    /**
     * Le pone restricciones a un hijo.
     *
     * <p>Se guarda una **copia**: las restricciones son mutables y quien las pasó puede seguir
     * usando el mismo objeto para el hijo siguiente, que es exactamente como se usa esta clase.
     *
     * @throws NullPointerException si las restricciones son `null`
     */
    public void setConstraints(Component comp, GridBagConstraints constraints) {
        this.comptable.put(comp, (GridBagConstraints) constraints.clone());
    }

    /**
     * Las restricciones de un hijo.
     *
     * @return una copia; cambiarla no cambia nada hasta que se la vuelva a poner con
     *     {@link #setConstraints}
     */
    public GridBagConstraints getConstraints(Component comp) {
        GridBagConstraints c = this.comptable.get(comp);
        if (c == null) {
            this.setConstraints(comp, this.defaultConstraints);
            c = this.comptable.get(comp);
        }
        return (GridBagConstraints) c.clone();
    }

    /**
     * Las restricciones de un hijo, **sin** copiar.
     *
     * <p>Es para uso interno de la distribución: devolver el objeto de verdad evita una copia por
     * hijo y por pasada, y las pasadas son varias.
     */
    protected GridBagConstraints lookupConstraints(Component comp) {
        GridBagConstraints c = this.comptable.get(comp);
        if (c == null) {
            this.setConstraints(comp, this.defaultConstraints);
            c = this.comptable.get(comp);
        }
        return c;
    }

    /** Saca las restricciones de un hijo. */
    private void removeConstraints(Component comp) {
        this.comptable.remove(comp);
    }

    /**
     * Dónde arranca la grilla dentro del contenedor.
     *
     * @return el ángulo superior izquierdo, o (0,0) si todavía no se maquetó
     */
    public Point getLayoutOrigin() {
        Point origin = new Point(0, 0);
        if (this.layoutInfo != null) {
            origin.x = this.layoutInfo.startx;
            origin.y = this.layoutInfo.starty;
        }
        return origin;
    }

    /**
     * Cuánto mide cada columna y cada fila.
     *
     * @return dos arreglos: anchos y altos, o dos vacíos si todavía no se maquetó
     */
    public int[][] getLayoutDimensions() {
        if (this.layoutInfo == null) {
            return new int[2][0];
        }
        int[][] dim = new int[2][];
        dim[0] = new int[this.layoutInfo.width];
        dim[1] = new int[this.layoutInfo.height];
        System.arraycopy(this.layoutInfo.minWidth, 0, dim[0], 0, this.layoutInfo.width);
        System.arraycopy(this.layoutInfo.minHeight, 0, dim[1], 0, this.layoutInfo.height);
        return dim;
    }

    /**
     * Qué peso tiene cada columna y cada fila.
     *
     * @return dos arreglos: pesos horizontales y verticales
     */
    public double[][] getLayoutWeights() {
        if (this.layoutInfo == null) {
            return new double[2][0];
        }
        double[][] w = new double[2][];
        w[0] = new double[this.layoutInfo.width];
        w[1] = new double[this.layoutInfo.height];
        System.arraycopy(this.layoutInfo.weightX, 0, w[0], 0, this.layoutInfo.width);
        System.arraycopy(this.layoutInfo.weightY, 0, w[1], 0, this.layoutInfo.height);
        return w;
    }

    /**
     * En qué celda cae ese punto del contenedor.
     *
     * <p>Un punto a la izquierda de la grilla da columna 0 y uno a la derecha da la cantidad de
     * columnas: el resultado siempre es una celda válida para insertar, aunque el punto caiga afuera.
     */
    public Point location(int x, int y) {
        Point loc = new Point(0, 0);
        if (this.layoutInfo == null) {
            return loc;
        }
        int d = this.layoutInfo.startx;
        int i;
        for (i = 0; i < this.layoutInfo.width; i++) {
            d = d + this.layoutInfo.minWidth[i];
            if (d > x) {
                break;
            }
        }
        loc.x = i;
        d = this.layoutInfo.starty;
        for (i = 0; i < this.layoutInfo.height; i++) {
            d = d + this.layoutInfo.minHeight[i];
            if (d > y) {
                break;
            }
        }
        loc.y = i;
        return loc;
    }

    /**
     * Agrega un hijo con restricciones.
     *
     * @throws IllegalArgumentException si las restricciones no son un {@link GridBagConstraints}
     */
    public void addLayoutComponent(Component comp, Object constraints) {
        if (constraints == null) {
            this.setConstraints(comp, this.defaultConstraints);
        } else if (constraints instanceof GridBagConstraints) {
            this.setConstraints(comp, (GridBagConstraints) constraints);
        } else {
            throw new IllegalArgumentException(
                    "cannot add to layout: constraints must be a GridBagConstraint");
        }
    }

    /**
     * Agrega un hijo por nombre.
     *
     * @deprecated esta distribución no usa nombres: no hace nada. Usar
     *     {@link #addLayoutComponent(Component, Object)}.
     */
    @Deprecated
    public void addLayoutComponent(String name, Component comp) {
    }

    /** Saca las restricciones de ese hijo. */
    public void removeLayoutComponent(Component comp) {
        this.removeConstraints(comp);
    }

    /** Lo que la grilla necesita con cada hijo en su medida preferida. */
    public Dimension preferredLayoutSize(Container parent) {
        GridBagLayoutInfo info = this.getLayoutInfo(parent, PREFERREDSIZE);
        return this.getMinSize(parent, info);
    }

    /** Lo mismo, con las medidas mínimas. */
    public Dimension minimumLayoutSize(Container parent) {
        GridBagLayoutInfo info = this.getLayoutInfo(parent, MINSIZE);
        return this.getMinSize(parent, info);
    }

    /** Sin tope: las columnas con peso aprovechan todo lo que les den. */
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

    /** Tira la grilla calculada: la próxima consulta la vuelve a armar. */
    public void invalidateLayout(Container target) {
        this.layoutInfo = null;
    }

    /** Arma la grilla y ubica a los hijos. */
    public void layoutContainer(Container parent) {
        this.arrangeGrid(parent);
    }

    /**
     * Calcula la grilla: cuántas filas y columnas, cuánto mide cada una y cuánto pesa.
     *
     * <p>Va en dos pasadas y no se puede hacer en una. La primera resuelve las posiciones
     * **relativas** —un hijo con {@code gridx} en {@code RELATIVE} va después del anterior— y de paso
     * averigua el tamaño de la grilla. Recién con la grilla dimensionada, la segunda reparte los
     * anchos y los pesos, porque un hijo que ocupa tres columnas tiene que repartir su medida entre
     * las tres y no se sabe cuáles son hasta terminar la primera.
     *
     * @param sizeflag {@link #MINSIZE} o {@link #PREFERREDSIZE}
     */
    protected GridBagLayoutInfo getLayoutInfo(Container parent, int sizeflag) {
        synchronized (parent.getTreeLock()) {
            int ncomponents = parent.getComponentCount();
            // --- primera pasada: resolver posiciones y averiguar el tamano de la grilla
            int[] gx = new int[ncomponents];
            int[] gy = new int[ncomponents];
            int[] gw = new int[ncomponents];
            int[] gh = new int[ncomponents];
            int cursorX = 0;
            int cursorY = 0;
            int maxX = 0;
            int maxY = 0;
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                GridBagConstraints c = this.lookupConstraints(comp);
                int w = c.gridwidth;
                int h = c.gridheight;
                if (w <= 0) {
                    w = 1;
                }
                if (h <= 0) {
                    h = 1;
                }
                int x = c.gridx;
                int y = c.gridy;
                if (x == GridBagConstraints.RELATIVE) {
                    x = cursorX;
                }
                if (y == GridBagConstraints.RELATIVE) {
                    y = cursorY;
                }
                gx[i] = x;
                gy[i] = y;
                gw[i] = w;
                gh[i] = h;
                // Un hijo con gridwidth REMAINDER cierra la fila: el siguiente arranca abajo.
                if (c.gridwidth == GridBagConstraints.REMAINDER) {
                    cursorX = 0;
                    cursorY = y + h;
                } else {
                    cursorX = x + w;
                    cursorY = y;
                }
                maxX = Math.max(maxX, x + w);
                maxY = Math.max(maxY, y + h);
            }
            if (maxX == 0) {
                maxX = 1;
            }
            if (maxY == 0) {
                maxY = 1;
            }
            GridBagLayoutInfo info = new GridBagLayoutInfo(maxX, maxY);
            // --- segunda pasada: repartir medidas y pesos
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                if (!comp.isVisible()) {
                    continue;
                }
                GridBagConstraints c = this.lookupConstraints(comp);
                Dimension d = sizeflag == PREFERREDSIZE ? comp.getPreferredSize()
                        : comp.getMinimumSize();
                // Queda anotado en las restricciones: `adjustForGravity` lo necesita despues, y
                // volver a medir ahi seria medir dos veces lo mismo.
                c.minWidth = d.width;
                c.minHeight = d.height;
                int anchoTotal = d.width + c.insets.left + c.insets.right + c.ipadx;
                int altoTotal = d.height + c.insets.top + c.insets.bottom + c.ipady;
                repartirMedida(info.minWidth, gx[i], gw[i], anchoTotal);
                repartirMedida(info.minHeight, gy[i], gh[i], altoTotal);
                repartirPeso(info.weightX, gx[i], gw[i], c.weightx);
                repartirPeso(info.weightY, gy[i], gh[i], c.weighty);
            }
            // Lo que el usuario haya impuesto desde afuera es un piso, no un reemplazo.
            imponer(info.minWidth, this.columnWidths);
            imponer(info.minHeight, this.rowHeights);
            imponerPeso(info.weightX, this.columnWeights);
            imponerPeso(info.weightY, this.rowWeights);
            return info;
        }
    }

    /**
     * Reparte la medida de un hijo entre las celdas que ocupa.
     *
     * <p>Un hijo que ocupa una sola celda impone su medida directamente. Uno que ocupa varias sólo
     * exige que **la suma** alcance: si ya alcanza no se toca nada, y si no, la diferencia se agrega
     * a la última. Repartirla en partes iguales sería peor — ensancharía columnas que no lo
     * necesitan.
     */
    private static void repartirMedida(int[] medidas, int inicio, int cuantas, int total) {
        if (inicio < 0 || inicio + cuantas > medidas.length) {
            return;
        }
        if (cuantas == 1) {
            medidas[inicio] = Math.max(medidas[inicio], total);
            return;
        }
        int suma = 0;
        for (int i = inicio; i < inicio + cuantas; i++) {
            suma = suma + medidas[i];
        }
        if (suma < total) {
            medidas[inicio + cuantas - 1] = medidas[inicio + cuantas - 1] + (total - suma);
        }
    }

    /** Lo mismo con los pesos: el de un hijo que abarca varias celdas es el de la mayor. */
    private static void repartirPeso(double[] pesos, int inicio, int cuantas, double peso) {
        if (peso <= 0 || inicio < 0 || inicio + cuantas > pesos.length) {
            return;
        }
        if (cuantas == 1) {
            pesos[inicio] = Math.max(pesos[inicio], peso);
            return;
        }
        double suma = 0;
        for (int i = inicio; i < inicio + cuantas; i++) {
            suma = suma + pesos[i];
        }
        if (suma < peso) {
            pesos[inicio + cuantas - 1] = pesos[inicio + cuantas - 1] + (peso - suma);
        }
    }

    /** Aplica los mínimos que se hayan impuesto desde afuera. */
    private static void imponer(int[] destino, int[] impuestos) {
        if (impuestos == null) {
            return;
        }
        int n = Math.min(destino.length, impuestos.length);
        for (int i = 0; i < n; i++) {
            destino[i] = Math.max(destino[i], impuestos[i]);
        }
    }

    /** Idem para los pesos. */
    private static void imponerPeso(double[] destino, double[] impuestos) {
        if (impuestos == null) {
            return;
        }
        int n = Math.min(destino.length, impuestos.length);
        for (int i = 0; i < n; i++) {
            destino[i] = Math.max(destino[i], impuestos[i]);
        }
    }

    /**
     * Calcula la grilla.
     *
     * @deprecated el nombre con mayúscula es de 1.1. Usar {@link #getLayoutInfo}.
     */
    @Deprecated
    protected GridBagLayoutInfo GetLayoutInfo(Container parent, int sizeflag) {
        return this.getLayoutInfo(parent, sizeflag);
    }

    /**
     * Ajusta el rectángulo de un hijo según su relleno y su anclaje.
     *
     * <p>Es donde `fill` y `anchor` se aplican de verdad: el rectángulo que entra es la celda que le
     * tocó y el que sale es dónde va a quedar el componente adentro de ella.
     */
    protected void adjustForGravity(GridBagConstraints constraints, Rectangle r) {
        int diffx = 0;
        int diffy = 0;
        r.x = r.x + constraints.insets.left;
        r.width = r.width - (constraints.insets.left + constraints.insets.right);
        r.y = r.y + constraints.insets.top;
        r.height = r.height - (constraints.insets.top + constraints.insets.bottom);
        // Sin `fill`, el componente se queda de su tamano y lo que sobra de la celda es `diffx`,
        // que despues el anclaje reparte entre los dos costados.
        int anchoPropio = constraints.minWidth + constraints.ipadx;
        if (constraints.fill != GridBagConstraints.HORIZONTAL
                && constraints.fill != GridBagConstraints.BOTH
                && r.width > anchoPropio) {
            diffx = r.width - anchoPropio;
            r.width = anchoPropio;
        }
        int altoPropio = constraints.minHeight + constraints.ipady;
        if (constraints.fill != GridBagConstraints.VERTICAL
                && constraints.fill != GridBagConstraints.BOTH
                && r.height > altoPropio) {
            diffy = r.height - altoPropio;
            r.height = altoPropio;
        }
        int a = constraints.anchor;
        if (a == GridBagConstraints.CENTER) {
            r.x = r.x + diffx / 2;
            r.y = r.y + diffy / 2;
        } else if (a == GridBagConstraints.NORTH) {
            r.x = r.x + diffx / 2;
        } else if (a == GridBagConstraints.NORTHEAST) {
            r.x = r.x + diffx;
        } else if (a == GridBagConstraints.EAST) {
            r.x = r.x + diffx;
            r.y = r.y + diffy / 2;
        } else if (a == GridBagConstraints.SOUTHEAST) {
            r.x = r.x + diffx;
            r.y = r.y + diffy;
        } else if (a == GridBagConstraints.SOUTH) {
            r.x = r.x + diffx / 2;
            r.y = r.y + diffy;
        } else if (a == GridBagConstraints.SOUTHWEST) {
            r.y = r.y + diffy;
        } else if (a == GridBagConstraints.WEST) {
            r.y = r.y + diffy / 2;
        }
    }

    /**
     * Ajusta el rectángulo de un hijo.
     *
     * @deprecated el nombre con mayúscula es de 1.1. Usar {@link #adjustForGravity}.
     */
    @Deprecated
    protected void AdjustForGravity(GridBagConstraints constraints, Rectangle r) {
        this.adjustForGravity(constraints, r);
    }

    /** La suma de las columnas y de las filas, más los márgenes del contenedor. */
    protected Dimension getMinSize(Container parent, GridBagLayoutInfo info) {
        if (info == null) {
            return new Dimension(0, 0);
        }
        int t = 0;
        for (int i = 0; i < info.width; i++) {
            t = t + info.minWidth[i];
        }
        int u = 0;
        for (int i = 0; i < info.height; i++) {
            u = u + info.minHeight[i];
        }
        Insets insets = parent.getInsets();
        return new Dimension(t + insets.left + insets.right, u + insets.top + insets.bottom);
    }

    /**
     * La suma de la grilla.
     *
     * @deprecated el nombre con mayúscula es de 1.1. Usar {@link #getMinSize}.
     */
    @Deprecated
    protected Dimension GetMinSize(Container parent, GridBagLayoutInfo info) {
        return this.getMinSize(parent, info);
    }

    /**
     * Ubica a los hijos.
     *
     * <p>El espacio sobrante se reparte **por peso**, y ahí está la parte que sorprende: el peso es
     * de la columna, no del componente. Recién después, con la celda ya dimensionada,
     * {@link #adjustForGravity} decide si el componente la llena o queda anclado adentro.
     */
    protected void arrangeGrid(Container parent) {
        synchronized (parent.getTreeLock()) {
            int ncomponents = parent.getComponentCount();
            if (ncomponents == 0) {
                return;
            }
            GridBagLayoutInfo info = this.getLayoutInfo(parent, PREFERREDSIZE);
            Dimension d = this.getMinSize(parent, info);
            if (parent.getWidth() < d.width || parent.getHeight() < d.height) {
                info = this.getLayoutInfo(parent, MINSIZE);
                d = this.getMinSize(parent, info);
            }
            this.layoutInfo = info;
            Insets insets = parent.getInsets();
            int sobraX = parent.getWidth() - d.width;
            int sobraY = parent.getHeight() - d.height;
            repartirSobrante(info.minWidth, info.weightX, sobraX);
            repartirSobrante(info.minHeight, info.weightY, sobraY);
            info.startx = insets.left;
            info.starty = insets.top;
            // Donde empieza cada columna y cada fila, acumulando.
            int[] xs = new int[info.width + 1];
            xs[0] = info.startx;
            for (int i = 0; i < info.width; i++) {
                xs[i + 1] = xs[i] + info.minWidth[i];
            }
            int[] ys = new int[info.height + 1];
            ys[0] = info.starty;
            for (int i = 0; i < info.height; i++) {
                ys[i + 1] = ys[i] + info.minHeight[i];
            }
            int cursorX = 0;
            int cursorY = 0;
            for (int i = 0; i < ncomponents; i++) {
                Component comp = parent.getComponent(i);
                GridBagConstraints c = this.lookupConstraints(comp);
                int w = c.gridwidth <= 0 ? 1 : c.gridwidth;
                int h = c.gridheight <= 0 ? 1 : c.gridheight;
                int x = c.gridx == GridBagConstraints.RELATIVE ? cursorX : c.gridx;
                int y = c.gridy == GridBagConstraints.RELATIVE ? cursorY : c.gridy;
                if (c.gridwidth == GridBagConstraints.REMAINDER) {
                    w = Math.max(1, info.width - x);
                    cursorX = 0;
                    cursorY = y + h;
                } else {
                    cursorX = x + w;
                    cursorY = y;
                }
                if (!comp.isVisible()) {
                    continue;
                }
                if (x < 0 || y < 0 || x + w > info.width || y + h > info.height) {
                    continue;
                }
                Rectangle r = new Rectangle(xs[x], ys[y], xs[x + w] - xs[x], ys[y + h] - ys[y]);
                this.adjustForGravity(c, r);
                comp.setBounds(r.x, r.y, r.width, r.height);
            }
        }
    }

    /**
     * Reparte el espacio que sobra entre las celdas, en proporción a su peso.
     *
     * <p>Si ningún peso es positivo no se reparte nada: la grilla queda de su tamaño natural y
     * centrada por el contenedor, que es lo que corresponde cuando nadie pidió crecer.
     */
    private static void repartirSobrante(int[] medidas, double[] pesos, int sobra) {
        if (sobra <= 0) {
            return;
        }
        double total = 0;
        for (int i = 0; i < pesos.length; i++) {
            total = total + pesos[i];
        }
        if (total <= 0) {
            return;
        }
        int repartido = 0;
        for (int i = 0; i < medidas.length; i++) {
            int parte = (int) (sobra * (pesos[i] / total));
            medidas[i] = medidas[i] + parte;
            repartido = repartido + parte;
        }
        // El resto de la division entera va a la ultima celda con peso: si se lo dejara sin
        // repartir, la grilla no llenaria el contenedor por unos pocos pixeles.
        if (repartido < sobra) {
            for (int i = medidas.length - 1; i >= 0; i--) {
                if (pesos[i] > 0) {
                    medidas[i] = medidas[i] + (sobra - repartido);
                    break;
                }
            }
        }
    }

    /**
     * Ubica a los hijos.
     *
     * @deprecated el nombre con mayúscula es de 1.1. Usar {@link #arrangeGrid}.
     */
    @Deprecated
    protected void ArrangeGrid(Container parent) {
        this.arrangeGrid(parent);
    }

    public String toString() {
        return this.getClass().getName();
    }
}
