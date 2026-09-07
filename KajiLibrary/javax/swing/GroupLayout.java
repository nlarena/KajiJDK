package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Acomoda describiendo los dos ejes por separado.
 *
 * <h2>Cada componente se declara dos veces</h2>
 *
 * <p>Es lo que hay que entender antes que nada, y lo que sorprende al principio: se arma un grupo
 * para el eje horizontal y otro para el vertical, y <strong>cada componente tiene que aparecer en
 * los dos</strong>. Uno dice donde cae a lo ancho, el otro donde cae a lo alto.
 *
 * <p>Suena redundante y no lo es: es lo que permite que una etiqueta este alineada a la izquierda
 * con otras dos en horizontal, y al mismo tiempo en la misma fila que su campo en vertical. Con un
 * solo arbol de posiciones eso no se puede decir sin una grilla, y una grilla no sabe de filas de
 * distinto alto.
 *
 * <h2>Dos clases de grupo</h2>
 *
 * <p>Un {@link SequentialGroup} pone sus miembros <em>uno detras de otro</em> a lo largo del eje; un
 * {@link ParallelGroup} los pone <em>en el mismo lugar</em>, alineados entre si. Anidando los dos se
 * describe cualquier formulario.
 *
 * <h2>Los huecos, que es lo que casi nadie quiere calcular</h2>
 *
 * <p>{@link #setAutoCreateGaps} y {@link #setAutoCreateContainerGaps} ponen solos el espacio que
 * corresponde entre componentes y contra el borde, preguntandole a {@link LayoutStyle} -- que sabe
 * lo que dice la guia de estilo del sistema. Es la razon por la que un formulario armado con esta
 * clase se ve bien en Windows y en Linux sin tocar un numero.
 *
 * <h2>Los dos tamanos magicos</h2>
 *
 * <p>{@link #DEFAULT_SIZE} significa "el que el componente diga" y {@link #PREFERRED_SIZE} "el
 * preferido, y que no cambie". Se los usa en los tres huecos de {@code addComponent}, y la
 * combinacion tipica -- {@code addComponent(c, PREFERRED_SIZE, PREFERRED_SIZE, PREFERRED_SIZE)} --
 * es como se dice "este no se estira".
 */
public class GroupLayout implements LayoutManager2 {

    /** "El tamano que el componente diga." */
    public static final int DEFAULT_SIZE = -1;

    /** "El preferido del componente." */
    public static final int PREFERRED_SIZE = -2;

    private static final int MIN = 0;
    private static final int PREF = 1;
    private static final int MAX = 2;

    /** El eje horizontal. */
    static final int HORIZONTAL = 0;

    /** El eje vertical. */
    static final int VERTICAL = 1;

    private final Container host;
    private Group horizontalGroup;
    private Group verticalGroup;
    private boolean honorsVisibility = true;
    private boolean autocreatePadding;
    private boolean autocreateContainerPadding;
    private LayoutStyle layoutStyle;
    private final Map<Component, Boolean> componentHonorsVisibility =
            new HashMap<Component, Boolean>();
    private final List<Component[]> linkedH = new ArrayList<Component[]>();
    private final List<Component[]> linkedV = new ArrayList<Component[]>();

    /**
     * Para ese contenedor.
     *
     * @throws IllegalArgumentException si es nulo
     */
    public GroupLayout(Container host) {
        if (host == null) {
            throw new IllegalArgumentException("Container must be non-null");
        }
        this.host = host;
        // Los dos ejes arrancan descriptos con un grupo paralelo vacio. Es lo que hace que medir
        // un acomodador recien creado de cero en vez de reventar: no hay estado "sin describir".
        setHorizontalGroup(createParallelGroup(Alignment.LEADING));
        setVerticalGroup(createParallelGroup(Alignment.LEADING));
    }

    /**
     * Si un componente escondido deja de ocupar lugar.
     *
     * <p>Prendido -- que es lo de omision -- un componente invisible mide cero y los demas se
     * corren. Apagado, sigue ocupando su lugar. Las dos formas se usan: la primera para lo que
     * aparece y desaparece, la segunda para que la pantalla no salte.
     */
    public void setHonorsVisibility(boolean honorsVisibility) {
        if (this.honorsVisibility != honorsVisibility) {
            this.honorsVisibility = honorsVisibility;
            invalidateHost();
        }
    }

    public boolean getHonorsVisibility() {
        return honorsVisibility;
    }

    /**
     * Lo mismo, para un componente en particular.
     *
     * <p>Nulo lo devuelve a lo que diga el contenedor.
     *
     * @throws IllegalArgumentException si el componente es nulo
     */
    public void setHonorsVisibility(Component component, Boolean honorsVisibility) {
        if (component == null) {
            throw new IllegalArgumentException("Component must be non-null");
        }
        if (honorsVisibility == null) {
            componentHonorsVisibility.remove(component);
        } else {
            componentHonorsVisibility.put(component, honorsVisibility);
        }
        invalidateHost();
    }

    /** Si los huecos entre componentes se ponen solos; ver la nota de la clase. */
    public void setAutoCreateGaps(boolean autoCreatePadding) {
        if (this.autocreatePadding != autoCreatePadding) {
            this.autocreatePadding = autoCreatePadding;
            invalidateHost();
        }
    }

    public boolean getAutoCreateGaps() {
        return autocreatePadding;
    }

    /** Si el hueco contra el borde del contenedor se pone solo. */
    public void setAutoCreateContainerGaps(boolean autoCreateContainerPadding) {
        if (this.autocreateContainerPadding != autoCreateContainerPadding) {
            this.autocreateContainerPadding = autoCreateContainerPadding;
            invalidateHost();
        }
    }

    public boolean getAutoCreateContainerGaps() {
        return autocreateContainerPadding;
    }

    /**
     * El grupo que describe el eje horizontal.
     *
     * @throws IllegalArgumentException si es nulo
     */
    public void setHorizontalGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Group must be non-null");
        }
        horizontalGroup = group;
        invalidateHost();
    }

    /**
     * El grupo que describe el eje vertical.
     *
     * @throws IllegalArgumentException si es nulo
     */
    public void setVerticalGroup(Group group) {
        if (group == null) {
            throw new IllegalArgumentException("Group must be non-null");
        }
        verticalGroup = group;
        invalidateHost();
    }

    /** Un grupo que pone sus miembros uno detras de otro. */
    public SequentialGroup createSequentialGroup() {
        return new SequentialGroup(this);
    }

    /** Un grupo que los pone en el mismo lugar, alineados al principio. */
    public ParallelGroup createParallelGroup() {
        return createParallelGroup(Alignment.LEADING);
    }

    /**
     * Idem, con esa alineacion.
     *
     * @throws IllegalArgumentException si la alineacion es nula
     */
    public ParallelGroup createParallelGroup(Alignment alignment) {
        return createParallelGroup(alignment, true);
    }

    /**
     * Idem, pudiendo pedir que el grupo no se estire.
     *
     * @throws IllegalArgumentException si la alineacion es nula
     */
    public ParallelGroup createParallelGroup(Alignment alignment, boolean resizable) {
        if (alignment == null) {
            throw new IllegalArgumentException("alignment must be non null");
        }
        if (alignment == Alignment.BASELINE) {
            return new ParallelGroup(this, alignment, resizable);
        }
        return new ParallelGroup(this, alignment, resizable);
    }

    /** Un grupo alineado por la linea de base del texto. */
    public ParallelGroup createBaselineGroup(boolean resizable, boolean anchorBaselineToTop) {
        return new ParallelGroup(this, Alignment.BASELINE, resizable);
    }

    /**
     * Hace que esos componentes midan todos lo mismo, en los dos ejes.
     *
     * <p>Toman el tamano del mas grande. Es como se consigue que tres botones con textos de
     * distinto largo queden del mismo ancho, que es lo que se espera de una fila de botones.
     *
     * @throws IllegalArgumentException si alguno es nulo
     */
    public void linkSize(Component... components) {
        linkSize(SwingConstants.HORIZONTAL, components);
        linkSize(SwingConstants.VERTICAL, components);
    }

    /**
     * Lo mismo, en un solo eje.
     *
     * @throws IllegalArgumentException si alguno es nulo o el eje no es uno de los dos
     */
    public void linkSize(int axis, Component... components) {
        if (components == null) {
            throw new IllegalArgumentException("Components must be non-null");
        }
        for (int i = components.length - 1; i >= 0; i--) {
            if (components[i] == null) {
                throw new IllegalArgumentException("Components must be non-null");
            }
        }
        if (axis == SwingConstants.HORIZONTAL) {
            linkedH.add(components.clone());
        } else if (axis == SwingConstants.VERTICAL) {
            linkedV.add(components.clone());
        } else {
            throw new IllegalArgumentException("Axis must be one of "
                    + "SwingConstants.HORIZONTAL or SwingConstants.VERTICAL");
        }
        invalidateHost();
    }

    /**
     * Cambia un componente por otro sin rearmar los grupos.
     *
     * <p>Es lo que permite reemplazar un campo por otro en un formulario ya descrito.
     *
     * @throws IllegalArgumentException si alguno es nulo
     */
    public void replace(Component existingComponent, Component newComponent) {
        if (existingComponent == null || newComponent == null) {
            throw new IllegalArgumentException("Components must be non-null");
        }
        if (horizontalGroup != null) {
            horizontalGroup.reemplazar(existingComponent, newComponent);
        }
        if (verticalGroup != null) {
            verticalGroup.reemplazar(existingComponent, newComponent);
        }
        host.remove(existingComponent);
        host.add(newComponent);
        invalidateHost();
    }

    /** Quien sabe cuanto espacio va entre dos cosas; nulo usa el del aspecto. */
    public void setLayoutStyle(LayoutStyle layoutStyle) {
        this.layoutStyle = layoutStyle;
        invalidateHost();
    }

    public LayoutStyle getLayoutStyle() {
        return layoutStyle;
    }

    LayoutStyle estilo() {
        if (layoutStyle != null) {
            return layoutStyle;
        }
        return LayoutStyle.getInstance();
    }

    /** No hace nada: los componentes se declaran en los grupos, no aca. */
    public void addLayoutComponent(String name, Component component) {
    }

    /** No hace nada: sacar un componente de los grupos es cosa de {@link #replace}. */
    public void removeLayoutComponent(Component component) {
        componentHonorsVisibility.remove(component);
    }

    /**
     * @throws IllegalArgumentException si no es el contenedor de este acomodador
     */
    public Dimension preferredLayoutSize(Container parent) {
        checkParent(parent);
        prepararGrupos();
        return medir(PREF);
    }

    /**
     * @throws IllegalArgumentException si no es el contenedor de este acomodador
     */
    public Dimension minimumLayoutSize(Container parent) {
        checkParent(parent);
        prepararGrupos();
        return medir(MIN);
    }

    /**
     * @throws IllegalArgumentException si no es el contenedor de este acomodador
     */
    public Dimension maximumLayoutSize(Container parent) {
        checkParent(parent);
        prepararGrupos();
        return medir(MAX);
    }

    private Dimension medir(int cual) {
        Insets insets = host.getInsets();
        int w = horizontalGroup.tamano(HORIZONTAL, cual);
        int h = verticalGroup.tamano(VERTICAL, cual);
        long tw = (long) w + insets.left + insets.right;
        long th = (long) h + insets.top + insets.bottom;
        return new Dimension((int) Math.min(tw, Integer.MAX_VALUE),
                (int) Math.min(th, Integer.MAX_VALUE));
    }

    /**
     * Coloca a cada componente.
     *
     * <p>Es el unico de los metodos de {@link java.awt.LayoutManager2} que <em>no</em> exige que el
     * contenedor sea el suyo: acomoda el propio igual, mire quien mire. La asimetria es del JDK y
     * esta medida.
     */
    public void layoutContainer(Container parent) {
        prepararGrupos();
        Insets insets = host.getInsets();
        int width = host.getWidth() - insets.left - insets.right;
        int height = host.getHeight() - insets.top - insets.bottom;
        horizontalGroup.setSize(HORIZONTAL, 0, width);
        verticalGroup.setSize(VERTICAL, 0, height);
        for (int i = 0; i < host.getComponentCount(); i++) {
            Component c = host.getComponent(i);
            Rect r = new Rect();
            horizontalGroup.ubicar(HORIZONTAL, c, r);
            verticalGroup.ubicar(VERTICAL, c, r);
            if (r.puestoH && r.puestoV) {
                c.setBounds(insets.left + r.x, insets.top + r.y, r.w, r.h);
            }
        }
    }

    /** Donde va a quedar un componente; se llena de a un eje por vez. */
    static class Rect {
        int x;
        int y;
        int w;
        int h;
        boolean puestoH;
        boolean puestoV;
    }

    public void addLayoutComponent(Component component, Object constraints) {
    }

    public float getLayoutAlignmentX(Container parent) {
        checkParent(parent);
        return 0.5f;
    }

    public float getLayoutAlignmentY(Container parent) {
        checkParent(parent);
        return 0.5f;
    }

    public void invalidateLayout(Container parent) {
        checkParent(parent);
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("HORIZONTAL\n").append(horizontalGroup)
                .append("\nVERTICAL\n").append(verticalGroup);
        return b.toString();
    }

    /**
     * @throws IllegalArgumentException si no es el contenedor de este acomodador
     */
    private void checkParent(Container parent) {
        if (parent != host) {
            throw new IllegalArgumentException(
                    "GroupLayout can only be used with one Container at a time");
        }
    }

    /** Deja los dos grupos listos para medir o colocar. */
    private void prepararGrupos() {
        horizontalGroup.prepararHuecos(HORIZONTAL, this);
        verticalGroup.prepararHuecos(VERTICAL, this);
        aplicarEnlaces();
    }

    /** Le da a los componentes enlazados el tamano del mas grande; ver {@link #linkSize}. */
    private void aplicarEnlaces() {
        aplicarEnlaces(linkedH, HORIZONTAL);
        aplicarEnlaces(linkedV, VERTICAL);
    }

    private void aplicarEnlaces(List<Component[]> lista, int eje) {
        for (int i = 0; i < lista.size(); i++) {
            Component[] grupo = lista.get(i);
            int max = 0;
            for (int j = 0; j < grupo.length; j++) {
                Dimension d = grupo[j].getPreferredSize();
                int v = (eje == HORIZONTAL) ? d.width : d.height;
                if (v > max) {
                    max = v;
                }
            }
            for (int j = 0; j < grupo.length; j++) {
                Group g = (eje == HORIZONTAL) ? horizontalGroup : verticalGroup;
                g.fijarEnlace(grupo[j], max);
            }
        }
    }

    private void invalidateHost() {
        if (host instanceof JComponent) {
            ((JComponent) host).revalidate();
        } else {
            host.invalidate();
        }
        host.repaint();
    }

    /** Si ese componente cuenta como visible para las medidas. */
    boolean cuenta(Component c) {
        Boolean propio = componentHonorsVisibility.get(c);
        boolean honra = (propio != null) ? propio.booleanValue() : honorsVisibility;
        return !honra || c.isVisible();
    }

    /** Como se alinean los miembros de un {@link ParallelGroup}. */
    public enum Alignment {

        /** Al principio del eje: arriba o a la izquierda. */
        LEADING,

        /** Al final: abajo o a la derecha. */
        TRAILING,

        /** Al medio. */
        CENTER,

        /** Por la linea de base del texto; solo tiene sentido en vertical. */
        BASELINE;
    }

    /**
     * Una distancia con minimo, preferido y maximo.
     *
     * <p>No es publica -- en el JDK tampoco --: lo que se ve desde afuera son los grupos. Todo lo
     * que entra en un grupo -- un componente, un hueco, otro grupo -- es uno de estos.
     */
    abstract static class Spring {

        int origen;
        int tamano;

        abstract int calcular(int eje, int cual);

        /** Le da posicion y tamano; las subclases que contienen a otros lo reparten. */
        void setSize(int eje, int origen, int tamano) {
            this.origen = origen;
            this.tamano = tamano;
        }

        int tamano(int eje, int cual) {
            return calcular(eje, cual);
        }

        /** Busca ese componente y lo ubica; ver {@link GroupLayout#layoutContainer}. */
        void ubicar(int eje, Component c, Rect r) {
        }

        void reemplazar(Component viejo, Component nuevo) {
        }

        void fijarEnlace(Component c, int tam) {
        }

        void prepararHuecos(int eje, GroupLayout l) {
        }
    }

    /**
     * Un grupo: varios resortes tratados como uno.
     *
     * <p>Lo que cambia entre las dos subclases es una sola cosa -- si los tamanos se suman o se toma
     * el mayor -- y de ahi sale todo lo demas.
     */
    public abstract static class Group extends Spring {

        final List<Spring> springs = new ArrayList<Spring>();
        final GroupLayout duenio;

        Group(GroupLayout duenio) {
            this.duenio = duenio;
        }

        /**
         * Agrega otro grupo adentro.
         *
         * @throws IllegalArgumentException si es nulo
         */
        public Group addGroup(Group group) {
            return addSpring(group);
        }

        /**
         * Agrega un componente con su tamano natural.
         *
         * @throws IllegalArgumentException si es nulo
         */
        public Group addComponent(Component component) {
            return addComponent(component, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        /**
         * Agrega un componente con esos tres tamanos.
         *
         * <p>Ver la nota de {@link GroupLayout} sobre {@link GroupLayout#DEFAULT_SIZE} y
         * {@link GroupLayout#PREFERRED_SIZE}.
         *
         * @throws IllegalArgumentException si es nulo o los tamanos son incoherentes
         */
        public Group addComponent(Component component, int min, int pref, int max) {
            return addSpring(new ComponentSpring(duenio, component, min, pref, max));
        }

        /**
         * Agrega un hueco fijo.
         *
         * @throws IllegalArgumentException si es negativo
         */
        public Group addGap(int size) {
            return addGap(size, size, size);
        }

        /**
         * Agrega un hueco elastico.
         *
         * @throws IllegalArgumentException si los tamanos son incoherentes
         */
        public Group addGap(int min, int pref, int max) {
            return addSpring(new GapSpring(min, pref, max));
        }

        Spring getSpring(int index) {
            return springs.get(index);
        }

        int indexOf(Spring spring) {
            return springs.indexOf(spring);
        }

        /**
         * @throws IllegalArgumentException si es nulo
         */
        Group addSpring(Spring spring) {
            if (spring == null) {
                throw new IllegalArgumentException("Spring must be non-null");
            }
            springs.add(spring);
            return this;
        }

        abstract int operator(int a, int b);

        int calcular(int eje, int cual) {
            int resultado = 0;
            boolean primero = true;
            for (int i = 0; i < springs.size(); i++) {
                Spring s = springs.get(i);
                int v = s.tamano(eje, cual);
                if (primero) {
                    resultado = v;
                    primero = false;
                } else {
                    resultado = operator(resultado, v);
                }
            }
            return Math.max(0, resultado);
        }

        void ubicar(int eje, Component c, Rect r) {
            for (int i = 0; i < springs.size(); i++) {
                springs.get(i).ubicar(eje, c, r);
            }
        }

        void reemplazar(Component viejo, Component nuevo) {
            for (int i = 0; i < springs.size(); i++) {
                springs.get(i).reemplazar(viejo, nuevo);
            }
        }

        void fijarEnlace(Component c, int tam) {
            for (int i = 0; i < springs.size(); i++) {
                springs.get(i).fijarEnlace(c, tam);
            }
        }

        void prepararHuecos(int eje, GroupLayout l) {
            for (int i = 0; i < springs.size(); i++) {
                springs.get(i).prepararHuecos(eje, l);
            }
        }

        public String toString() {
            return getClass().getSimpleName() + springs;
        }
    }

    /**
     * Pone sus miembros uno detras de otro.
     *
     * <p>El tamano del grupo es la suma. Al repartir un tamano distinto del preferido, el sobrante
     * se distribuye entre los que pueden estirarse, en proporcion a cuanto pueden: es lo que hace
     * que un campo elastico se lleve todo el espacio y una etiqueta no se mueva.
     */
    public static final class SequentialGroup extends Group {

        SequentialGroup(GroupLayout duenio) {
            super(duenio);
        }

        public SequentialGroup addGroup(Group group) {
            return (SequentialGroup) super.addGroup(group);
        }

        /** Idem, pudiendo excluir al grupo del calculo de la linea de base. */
        public SequentialGroup addGroup(boolean useAsBaseline, Group group) {
            return addGroup(group);
        }

        public SequentialGroup addComponent(Component component) {
            return (SequentialGroup) super.addComponent(component);
        }

        /** Idem, pudiendo excluir al componente del calculo de la linea de base. */
        public SequentialGroup addComponent(boolean useAsBaseline, Component component) {
            return addComponent(component);
        }

        public SequentialGroup addComponent(Component component, int min, int pref, int max) {
            return (SequentialGroup) super.addComponent(component, min, pref, max);
        }

        /** Idem; ver {@link #addComponent(boolean, Component)}. */
        public SequentialGroup addComponent(boolean useAsBaseline, Component component, int min,
                int pref, int max) {
            return addComponent(component, min, pref, max);
        }

        public SequentialGroup addGap(int size) {
            return (SequentialGroup) super.addGap(size);
        }

        public SequentialGroup addGap(int min, int pref, int max) {
            return (SequentialGroup) super.addGap(min, pref, max);
        }

        /**
         * Un hueco del tamano que corresponda entre esos dos componentes.
         *
         * <p>Lo decide {@link LayoutStyle}; ver la nota de {@link GroupLayout}.
         *
         * @throws IllegalArgumentException si algo es nulo
         */
        public SequentialGroup addPreferredGap(JComponent comp1, JComponent comp2,
                LayoutStyle.ComponentPlacement type) {
            return addPreferredGap(comp1, comp2, type, DEFAULT_SIZE, PREFERRED_SIZE);
        }

        /**
         * Idem, pudiendo estirarse.
         *
         * @throws IllegalArgumentException si algo es nulo
         */
        public SequentialGroup addPreferredGap(JComponent comp1, JComponent comp2,
                LayoutStyle.ComponentPlacement type, int pref, int max) {
            if (comp1 == null || comp2 == null || type == null) {
                throw new IllegalArgumentException("Components and type must be non-null");
            }
            return (SequentialGroup) addSpring(
                    new AutoGapSpring(duenio, comp1, comp2, type, pref, max));
        }

        /**
         * Un hueco entre lo que venga antes y lo que venga despues.
         *
         * @throws IllegalArgumentException si el tipo es nulo o es {@code INDENT}
         */
        public SequentialGroup addPreferredGap(LayoutStyle.ComponentPlacement type) {
            return addPreferredGap(type, DEFAULT_SIZE, PREFERRED_SIZE);
        }

        /**
         * Idem, pudiendo estirarse.
         *
         * @throws IllegalArgumentException si el tipo es nulo o es {@code INDENT}
         */
        public SequentialGroup addPreferredGap(LayoutStyle.ComponentPlacement type, int pref,
                int max) {
            if (type == null) {
                throw new IllegalArgumentException("Type must be non-null");
            }
            if (type == LayoutStyle.ComponentPlacement.INDENT) {
                throw new IllegalArgumentException("Unsupported type");
            }
            return (SequentialGroup) addSpring(
                    new AutoGapSpring(duenio, null, null, type, pref, max));
        }

        /** El hueco que corresponde contra el borde del contenedor. */
        public SequentialGroup addContainerGap() {
            return addContainerGap(DEFAULT_SIZE, PREFERRED_SIZE);
        }

        /**
         * Idem, pudiendo estirarse.
         *
         * @throws IllegalArgumentException si los tamanos son incoherentes
         */
        public SequentialGroup addContainerGap(int pref, int max) {
            return (SequentialGroup) addSpring(new ContainerGapSpring(duenio, pref, max));
        }

        int operator(int a, int b) {
            long total = (long) a + (long) b;
            return (int) Math.min(total, Integer.MAX_VALUE);
        }

        /**
         * Reparte el tamano entre los miembros.
         *
         * <p>Cada uno arranca en su preferido; el sobrante -- o el faltante -- se reparte entre los
         * que pueden moverse, en proporcion a cuanto margen tiene cada uno. Ver la nota de la clase.
         */
        void setSize(int eje, int origen, int tamano) {
            super.setSize(eje, origen, tamano);
            int n = springs.size();
            if (n == 0) {
                return;
            }
            int[] pref = new int[n];
            int[] margen = new int[n];
            long totalPref = 0;
            long totalMargen = 0;
            boolean agrandar = true;
            for (int i = 0; i < n; i++) {
                pref[i] = springs.get(i).tamano(eje, PREF);
                totalPref = totalPref + pref[i];
            }
            long delta = tamano - totalPref;
            agrandar = (delta >= 0);
            for (int i = 0; i < n; i++) {
                Spring s = springs.get(i);
                if (agrandar) {
                    margen[i] = s.tamano(eje, MAX) - pref[i];
                } else {
                    margen[i] = pref[i] - s.tamano(eje, MIN);
                }
                if (margen[i] < 0) {
                    margen[i] = 0;
                }
                totalMargen = totalMargen + margen[i];
            }
            int pos = origen;
            long restante = (delta < 0) ? -delta : delta;
            long repartido = 0;
            for (int i = 0; i < n; i++) {
                int extra = 0;
                if (totalMargen > 0 && restante > 0) {
                    if (i == n - 1) {
                        extra = (int) Math.min(restante - repartido, margen[i]);
                    } else {
                        extra = (int) (restante * margen[i] / totalMargen);
                        if (extra > margen[i]) {
                            extra = margen[i];
                        }
                    }
                    repartido = repartido + extra;
                }
                int t = agrandar ? pref[i] + extra : pref[i] - extra;
                springs.get(i).setSize(eje, pos, t);
                pos = pos + t;
            }
        }
    }

    /**
     * Pone sus miembros en el mismo lugar, alineados entre si.
     *
     * <p>El tamano del grupo es el del mayor. Cada miembro recibe el tamano del grupo si puede
     * estirarse, y si no queda de su tamano preferido, colocado segun la alineacion.
     */
    public static class ParallelGroup extends Group {

        private final Alignment childAlignment;
        private final boolean resizable;
        private final Map<Spring, Alignment> alineaciones = new HashMap<Spring, Alignment>();

        ParallelGroup(GroupLayout duenio, Alignment childAlignment, boolean resizable) {
            super(duenio);
            this.childAlignment = childAlignment;
            this.resizable = resizable;
        }

        public ParallelGroup addGroup(Group group) {
            return (ParallelGroup) super.addGroup(group);
        }

        public ParallelGroup addComponent(Component component) {
            return (ParallelGroup) super.addComponent(component);
        }

        public ParallelGroup addComponent(Component component, int min, int pref, int max) {
            return (ParallelGroup) super.addComponent(component, min, pref, max);
        }

        public ParallelGroup addGap(int size) {
            return (ParallelGroup) super.addGap(size);
        }

        public ParallelGroup addGap(int min, int pref, int max) {
            return (ParallelGroup) super.addGap(min, pref, max);
        }

        /**
         * Agrega un grupo con su propia alineacion.
         *
         * @throws IllegalArgumentException si la alineacion es nula
         */
        public ParallelGroup addGroup(Alignment alignment, Group group) {
            if (alignment == null) {
                throw new IllegalArgumentException("Alignment must be non-null");
            }
            addSpring(group);
            alineaciones.put(group, alignment);
            return this;
        }

        /**
         * Agrega un componente con su propia alineacion.
         *
         * @throws IllegalArgumentException si la alineacion es nula
         */
        public ParallelGroup addComponent(Component component, Alignment alignment) {
            return addComponent(component, alignment, DEFAULT_SIZE, DEFAULT_SIZE, DEFAULT_SIZE);
        }

        /**
         * Idem, con los tres tamanos.
         *
         * @throws IllegalArgumentException si la alineacion es nula
         */
        public ParallelGroup addComponent(Component component, Alignment alignment, int min,
                int pref, int max) {
            if (alignment == null) {
                throw new IllegalArgumentException("Alignment must be non-null");
            }
            Spring s = new ComponentSpring(duenio, component, min, pref, max);
            addSpring(s);
            alineaciones.put(s, alignment);
            return this;
        }

        /** Si el grupo se estira; ver {@link GroupLayout#createParallelGroup(Alignment, boolean)}. */
        boolean isResizable() {
            return resizable;
        }

        int operator(int a, int b) {
            return Math.max(a, b);
        }

        int calcular(int eje, int cual) {
            if (!resizable && cual != PREF) {
                return calcular(eje, PREF);
            }
            return super.calcular(eje, cual);
        }

        /** A cada uno el tamano del grupo si puede; si no, el suyo, alineado. */
        void setSize(int eje, int origen, int tamano) {
            super.setSize(eje, origen, tamano);
            for (int i = 0; i < springs.size(); i++) {
                Spring s = springs.get(i);
                int max = s.tamano(eje, MAX);
                int min = s.tamano(eje, MIN);
                int t = Math.max(min, Math.min(tamano, max));
                Alignment a = alineaciones.get(s);
                if (a == null) {
                    a = childAlignment;
                }
                int off = 0;
                if (t < tamano) {
                    if (a == Alignment.TRAILING) {
                        off = tamano - t;
                    } else if (a == Alignment.CENTER) {
                        off = (tamano - t) / 2;
                    }
                }
                s.setSize(eje, origen + off, t);
            }
        }
    }

    /** Un componente adentro de un grupo. */
    private static class ComponentSpring extends Spring {

        private final GroupLayout duenio;
        private Component component;
        private final int min;
        private final int pref;
        private final int max;
        private int enlazado = -1;

        ComponentSpring(GroupLayout duenio, Component component, int min, int pref, int max) {
            if (component == null) {
                throw new IllegalArgumentException("Component must be non-null");
            }
            checkSize(min, pref, max, true);
            this.duenio = duenio;
            this.component = component;
            this.min = min;
            this.pref = pref;
            this.max = max;
        }

        int calcular(int eje, int cual) {
            if (!duenio.cuenta(component)) {
                return 0;
            }
            if (enlazado >= 0) {
                return enlazado;
            }
            int pedido = (cual == MIN) ? min : ((cual == PREF) ? pref : max);
            if (pedido >= 0) {
                return pedido;
            }
            if (pedido == PREFERRED_SIZE) {
                return natural(eje, PREF);
            }
            return natural(eje, cual);
        }

        private int natural(int eje, int cual) {
            Dimension d;
            if (cual == MIN) {
                d = component.getMinimumSize();
            } else if (cual == PREF) {
                d = component.getPreferredSize();
            } else {
                d = component.getMaximumSize();
            }
            return (eje == HORIZONTAL) ? d.width : d.height;
        }

        void ubicar(int eje, Component c, Rect r) {
            if (c != component) {
                return;
            }
            if (eje == HORIZONTAL) {
                r.x = origen;
                r.w = tamano;
                r.puestoH = true;
            } else {
                r.y = origen;
                r.h = tamano;
                r.puestoV = true;
            }
        }

        void reemplazar(Component viejo, Component nuevo) {
            if (component == viejo) {
                component = nuevo;
            }
        }

        void fijarEnlace(Component c, int tam) {
            if (component == c) {
                enlazado = tam;
            }
        }

        public String toString() {
            return "Component(" + component.getName() + ")";
        }
    }

    /** Un hueco de tamano dado. */
    private static class GapSpring extends Spring {

        private final int min;
        private final int pref;
        private final int max;

        GapSpring(int min, int pref, int max) {
            checkSize(min, pref, max, false);
            this.min = min;
            this.pref = pref;
            this.max = max;
        }

        int calcular(int eje, int cual) {
            int v = (cual == MIN) ? min : ((cual == PREF) ? pref : max);
            return (v < 0) ? 0 : v;
        }

        public String toString() {
            return "Gap(" + min + "," + pref + "," + max + ")";
        }
    }

    /** Un hueco cuyo tamano lo decide {@link LayoutStyle}. */
    private static class AutoGapSpring extends Spring {

        private final GroupLayout duenio;
        private JComponent c1;
        private JComponent c2;
        private final LayoutStyle.ComponentPlacement type;
        private final int pref;
        private final int max;

        AutoGapSpring(GroupLayout duenio, JComponent c1, JComponent c2,
                LayoutStyle.ComponentPlacement type, int pref, int max) {
            this.duenio = duenio;
            this.c1 = c1;
            this.c2 = c2;
            this.type = type;
            this.pref = pref;
            this.max = max;
        }

        private int base(int eje) {
            if (c1 == null || c2 == null) {
                // Sin componentes concretos no hay a quien medirle el vecino: se usa el hueco de
                // "relacionados" contra el mismo contenedor, que es lo que se ve en la practica.
                return (type == LayoutStyle.ComponentPlacement.UNRELATED) ? 12 : 6;
            }
            int position = (eje == HORIZONTAL) ? SwingConstants.EAST : SwingConstants.SOUTH;
            return duenio.estilo().getPreferredGap(c1, c2, type, position, null);
        }

        int calcular(int eje, int cual) {
            int b = base(eje);
            if (cual == MIN) {
                return b;
            }
            if (cual == PREF) {
                return (pref >= 0) ? pref : b;
            }
            if (max == PREFERRED_SIZE) {
                return (pref >= 0) ? pref : b;
            }
            return (max >= 0) ? max : Integer.MAX_VALUE;
        }

        void reemplazar(Component viejo, Component nuevo) {
            if (c1 == viejo && nuevo instanceof JComponent) {
                c1 = (JComponent) nuevo;
            }
            if (c2 == viejo && nuevo instanceof JComponent) {
                c2 = (JComponent) nuevo;
            }
        }

        public String toString() {
            return "AutoGap(" + type + ")";
        }
    }

    /** El hueco contra el borde del contenedor. */
    private static class ContainerGapSpring extends Spring {

        private final GroupLayout duenio;
        private final int pref;
        private final int max;

        ContainerGapSpring(GroupLayout duenio, int pref, int max) {
            this.duenio = duenio;
            this.pref = pref;
            this.max = max;
        }

        int calcular(int eje, int cual) {
            int b = 6;
            if (cual == MIN) {
                return b;
            }
            if (cual == PREF) {
                return (pref >= 0) ? pref : b;
            }
            if (max == PREFERRED_SIZE) {
                return (pref >= 0) ? pref : b;
            }
            return (max >= 0) ? max : Integer.MAX_VALUE;
        }

        public String toString() {
            return "ContainerGap";
        }
    }

    /**
     * Que los tres tamanos sean coherentes.
     *
     * <p>Un componente admite {@link #DEFAULT_SIZE} y {@link #PREFERRED_SIZE} en el minimo y en el
     * maximo -- "el que tenga" y "el preferido" son respuestas validas cuando hay a quien
     * preguntarle --; un hueco no tiene a quien preguntarle su tamano de omision, y por eso solo
     * admite {@link #PREFERRED_SIZE}, que ahi significa "no te estires".
     *
     * @throws IllegalArgumentException si los tres tamanos no son coherentes
     */
    private static void checkSize(int min, int pref, int max, boolean componente) {
        checkResizeType(min, componente);
        if (!componente && pref < 0) {
            throw new IllegalArgumentException("Pref must be positive, DEFAULT_SIZE "
                    + "or PREFERRED_SIZE");
        }
        checkResizeType(max, componente);
        if (min >= 0 && pref >= 0 && min > pref) {
            throw new IllegalArgumentException("Following is not met: min<=pref<=max");
        }
        if (pref >= 0 && max >= 0 && pref > max) {
            throw new IllegalArgumentException("Following is not met: min<=pref<=max");
        }
        if (min >= 0 && max >= 0 && min > max) {
            throw new IllegalArgumentException("Following is not met: min<=pref<=max");
        }
    }

    private static void checkResizeType(int type, boolean componente) {
        if (type < 0 && ((componente && type != DEFAULT_SIZE && type != PREFERRED_SIZE)
                || (!componente && type != PREFERRED_SIZE))) {
            throw new IllegalArgumentException("Invalid size");
        }
    }
}
