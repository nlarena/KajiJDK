package javax.swing;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager2;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Acomoda diciendo donde va cada borde en relacion con otro borde.
 *
 * <h2>Bordes, no posiciones</h2>
 *
 * <p>En vez de decir "este componente va en (10, 20)", se dice "su borde oeste esta a diez del borde
 * oeste del contenedor" y "su borde norte esta a cinco del borde sur del de arriba". Cada relacion es
 * un {@link Spring}, asi que las distancias pueden ser elasticas y el conjunto se reacomoda solo
 * cuando la ventana cambia de tamano.
 *
 * <h2>Dos restricciones por eje, y la tercera pisa a la primera</h2>
 *
 * <p>En horizontal hay cuatro bordes -- oeste, ancho, este y centro -- pero solo dos son
 * independientes: con dos cualesquiera los otros dos quedan determinados. Poner una tercera no es un
 * error: <strong>se descarta la mas vieja</strong> y se recalculan las derivadas. Lo mismo en
 * vertical, donde ademas esta la linea de base.
 *
 * <p>Es la parte que sorprende y la que hay que tener presente: el orden en que se ponen las
 * restricciones cambia el resultado. De ahi que {@link Constraints} lleve una historia.
 *
 * <h2>El contenedor tambien tiene bordes</h2>
 *
 * <p>Se le ponen restricciones igual que a un hijo, y es como se le dice que tan grande quiere ser.
 * Un contenedor sin restricciones propias mide lo que haga falta para que entren todos.
 *
 * <h2>Los ciclos se detectan, no se cuelgan</h2>
 *
 * <p>Decir que A esta a la derecha de B y B a la derecha de A no tiene solucion. En vez de girar para
 * siempre, un resorte que se encuentra a si mismo devuelve {@link Spring#UNSET}.
 */
public class SpringLayout implements LayoutManager2 {

    private final Map<Component, Constraints> componentConstraints =
            new HashMap<Component, Constraints>();

    private final Spring cyclicReference = Spring.constant(Spring.UNSET);
    private Set<Spring> cyclicSprings;
    private Set<Spring> acyclicSprings;

    /** El borde de arriba. */
    public static final String NORTH = "North";

    /** El borde de abajo. */
    public static final String SOUTH = "South";

    /** El borde derecho. */
    public static final String EAST = "East";

    /** El borde izquierdo. */
    public static final String WEST = "West";

    /** El centro horizontal. */
    public static final String HORIZONTAL_CENTER = "HorizontalCenter";

    /** El centro vertical. */
    public static final String VERTICAL_CENTER = "VerticalCenter";

    /** La linea de base del texto. */
    public static final String BASELINE = "Baseline";

    /** El ancho. */
    public static final String WIDTH = "Width";

    /** El alto. */
    public static final String HEIGHT = "Height";

    private static final String[] ALL_HORIZONTAL = {WEST, WIDTH, EAST, HORIZONTAL_CENTER};

    private static final String[] ALL_VERTICAL = {NORTH, HEIGHT, SOUTH, VERTICAL_CENTER,
        BASELINE};

    /** Un acomodador sin ninguna restriccion puesta. */
    public SpringLayout() {
    }

    /**
     * Las restricciones de un componente.
     *
     * <p>Solo dos por eje son independientes; ver la nota de {@link SpringLayout}. Los bordes que no
     * se pusieron se derivan de los que si: el este es el oeste mas el ancho, el centro es el oeste
     * mas la mitad del ancho, y asi. Se derivan al pedirlos y se guardan.
     */
    public static class Constraints {

        private Spring x;
        private Spring y;
        private Spring width;
        private Spring height;
        private Spring east;
        private Spring south;
        private Spring horizontalCenter;
        private Spring verticalCenter;
        private Spring baseline;

        private final List<String> horizontalHistory = new ArrayList<String>(2);
        private final List<String> verticalHistory = new ArrayList<String>(2);

        /** Para la linea de base, que depende del componente. */
        private Component c;

        /** Sin ninguna restriccion. */
        public Constraints() {
        }

        /** Con la esquina noroeste puesta. */
        public Constraints(Spring x, Spring y) {
            setX(x);
            setY(y);
        }

        /** Con la esquina y las medidas. */
        public Constraints(Spring x, Spring y, Spring width, Spring height) {
            setX(x);
            setY(y);
            setWidth(width);
            setHeight(height);
        }

        /**
         * Tomadas de ese componente.
         *
         * <p>La posicion queda fija -- la que el componente tiene ahora -- y las medidas quedan
         * atadas a el, asi que si el componente cambia de tamano preferido las restricciones lo
         * siguen.
         */
        public Constraints(Component c) {
            this.c = c;
            setX(Spring.constant(c.getX()));
            setY(Spring.constant(c.getY()));
            setWidth(Spring.width(c));
            setHeight(Spring.height(c));
        }

        private Spring sum(Spring s1, Spring s2) {
            return (s1 == null || s2 == null) ? null : Spring.sum(s1, s2);
        }

        private Spring difference(Spring s1, Spring s2) {
            return (s1 == null || s2 == null) ? null : Spring.difference(s1, s2);
        }

        private Spring scale(Spring s, float factor) {
            return (s == null) ? null : Spring.scale(s, factor);
        }

        /**
         * Anota que se puso esta restriccion, y descarta la mas vieja si ya habia dos.
         *
         * <p>Al descartar, las derivadas que quedaron sin sustento se borran: si no, seguirian
         * contestando con una cuenta hecha a partir de algo que ya no vale.
         */
        private void pushConstraint(String name, Spring value, boolean horizontal) {
            boolean valid = true;
            List<String> history = horizontal ? horizontalHistory : verticalHistory;
            if (history.contains(name)) {
                history.remove(name);
                valid = false;
            } else if (history.size() == 2 && value != null) {
                history.remove(0);
                valid = false;
            }
            if (value != null) {
                history.add(name);
            }
            if (!valid) {
                String[] all = horizontal ? ALL_HORIZONTAL : ALL_VERTICAL;
                for (int i = 0; i < all.length; i++) {
                    if (!history.contains(all[i])) {
                        setConstraint(all[i], null);
                    }
                }
            }
        }

        /** El borde oeste. */
        public void setX(Spring x) {
            this.x = x;
            pushConstraint(WEST, x, true);
        }

        /** El borde oeste, derivandolo de los otros dos si no se puso; ver la nota de la clase. */
        public Spring getX() {
            return derivarX();
        }

        /** El borde norte. */
        public void setY(Spring y) {
            this.y = y;
            pushConstraint(NORTH, y, false);
        }

        /** El borde norte, derivandolo si hace falta. */
        public Spring getY() {
            return derivarY();
        }

        public void setWidth(Spring width) {
            this.width = width;
            pushConstraint(WIDTH, width, true);
        }

        /** El ancho, derivandolo si hace falta. */
        public Spring getWidth() {
            return derivarAncho();
        }

        public void setHeight(Spring height) {
            this.height = height;
            pushConstraint(HEIGHT, height, false);
        }

        /** El alto, derivandolo si hace falta. */
        public Spring getHeight() {
            return derivarAlto();
        }

        private void setEast(Spring east) {
            this.east = east;
            pushConstraint(EAST, east, true);
        }

        private Spring getEast() {
            return derivarEste();
        }

        private void setSouth(Spring south) {
            this.south = south;
            pushConstraint(SOUTH, south, false);
        }

        private Spring getSouth() {
            return derivarSur();
        }

        private void setHorizontalCenter(Spring horizontalCenter) {
            this.horizontalCenter = horizontalCenter;
            pushConstraint(HORIZONTAL_CENTER, horizontalCenter, true);
        }

        private Spring getHorizontalCenter() {
            return derivarCentroH();
        }

        private void setVerticalCenter(Spring verticalCenter) {
            this.verticalCenter = verticalCenter;
            pushConstraint(VERTICAL_CENTER, verticalCenter, false);
        }

        private Spring getVerticalCenter() {
            return derivarCentroV();
        }

        private void setBaseline(Spring baseline) {
            this.baseline = baseline;
            pushConstraint(BASELINE, baseline, false);
        }

        private Spring getBaseline() {
            return baseline;
        }

        /**
         * Pone la restriccion de ese borde.
         *
         * <p>Un nombre que no es ninguno de los nueve se ignora en silencio, que es lo que hace el
         * JDK: la lista de nombres es de cadenas y no de constantes de enumeracion, asi que no hay
         * como distinguir un nombre nuevo de uno mal escrito.
         */
        public void setConstraint(String edgeName, Spring s) {
            if (WEST.equals(edgeName)) {
                setX(s);
            } else if (NORTH.equals(edgeName)) {
                setY(s);
            } else if (EAST.equals(edgeName)) {
                setEast(s);
            } else if (SOUTH.equals(edgeName)) {
                setSouth(s);
            } else if (HORIZONTAL_CENTER.equals(edgeName)) {
                setHorizontalCenter(s);
            } else if (WIDTH.equals(edgeName)) {
                setWidth(s);
            } else if (HEIGHT.equals(edgeName)) {
                setHeight(s);
            } else if (VERTICAL_CENTER.equals(edgeName)) {
                setVerticalCenter(s);
            } else if (BASELINE.equals(edgeName)) {
                setBaseline(s);
            }
        }

        /**
         * La restriccion de ese borde, derivandola si hace falta.
         *
         * <p>Nulo si no hay con que derivarla; ver la nota de la clase.
         */
        public Spring getConstraint(String edgeName) {
            if (WEST.equals(edgeName)) {
                return getX();
            }
            if (NORTH.equals(edgeName)) {
                return getY();
            }
            if (EAST.equals(edgeName)) {
                return getEast();
            }
            if (SOUTH.equals(edgeName)) {
                return getSouth();
            }
            if (WIDTH.equals(edgeName)) {
                return getWidth();
            }
            if (HEIGHT.equals(edgeName)) {
                return getHeight();
            }
            if (HORIZONTAL_CENTER.equals(edgeName)) {
                return getHorizontalCenter();
            }
            if (VERTICAL_CENTER.equals(edgeName)) {
                return getVerticalCenter();
            }
            if (BASELINE.equals(edgeName)) {
                return getBaseline();
            }
            return null;
        }

        private Spring derivarX() {
            if (x == null) {
                if (east != null && width != null) {
                    x = difference(east, width);
                } else if (horizontalCenter != null && width != null) {
                    x = difference(horizontalCenter, scale(width, 0.5f));
                }
            }
            return x;
        }

        private Spring derivarAncho() {
            if (width == null) {
                if (east != null && x != null) {
                    width = difference(east, x);
                } else if (horizontalCenter != null && x != null) {
                    width = scale(difference(horizontalCenter, x), 2.0f);
                }
            }
            return width;
        }

        private Spring derivarEste() {
            if (east == null) {
                if (x != null && width != null) {
                    east = sum(x, width);
                } else if (horizontalCenter != null && x != null) {
                    east = difference(scale(horizontalCenter, 2.0f), x);
                }
            }
            return east;
        }

        private Spring derivarCentroH() {
            if (horizontalCenter == null) {
                if (x != null && width != null) {
                    horizontalCenter = sum(x, scale(width, 0.5f));
                } else if (east != null && x != null) {
                    horizontalCenter = scale(sum(x, east), 0.5f);
                }
            }
            return horizontalCenter;
        }

        private Spring derivarY() {
            if (y == null) {
                if (south != null && height != null) {
                    y = difference(south, height);
                } else if (verticalCenter != null && height != null) {
                    y = difference(verticalCenter, scale(height, 0.5f));
                }
            }
            return y;
        }

        private Spring derivarAlto() {
            if (height == null) {
                if (south != null && y != null) {
                    height = difference(south, y);
                } else if (verticalCenter != null && y != null) {
                    height = scale(difference(verticalCenter, y), 2.0f);
                }
            }
            return height;
        }

        private Spring derivarSur() {
            if (south == null) {
                if (y != null && height != null) {
                    south = sum(y, height);
                } else if (verticalCenter != null && y != null) {
                    south = difference(scale(verticalCenter, 2.0f), y);
                }
            }
            return south;
        }

        private Spring derivarCentroV() {
            if (verticalCenter == null) {
                if (y != null && height != null) {
                    verticalCenter = sum(y, scale(height, 0.5f));
                } else if (south != null && y != null) {
                    verticalCenter = scale(sum(y, south), 0.5f);
                }
            }
            return verticalCenter;
        }

        /** Devuelve todos los resortes a "todavia no se sabe". */
        void reset() {
            Spring[] todos = {x, y, width, height, east, south, horizontalCenter,
                verticalCenter, baseline};
            for (int i = 0; i < todos.length; i++) {
                if (todos[i] != null) {
                    todos[i].setValue(Spring.UNSET);
                }
            }
        }
    }

    /**
     * Si ese resorte se encuentra a si mismo.
     *
     * <p>Se marca mientras se lo recorre: si al preguntarle a sus partes se vuelve a llegar a el, es
     * ciclico. Los resultados se guardan en dos conjuntos, uno de los que si y otro de los que no,
     * porque la pregunta se hace muchas veces por acomodada.
     */
    boolean isCyclic(Spring s) {
        if (s == null) {
            return false;
        }
        if (cyclicSprings == null) {
            cyclicSprings = new HashSet<Spring>();
            acyclicSprings = new HashSet<Spring>();
        }
        if (cyclicSprings.contains(s)) {
            return true;
        }
        if (acyclicSprings.contains(s)) {
            return false;
        }
        cyclicSprings.add(s);
        boolean result = s.isCyclic(this);
        if (!result) {
            acyclicSprings.add(s);
            cyclicSprings.remove(s);
        }
        return result;
    }

    /** El resorte que se devuelve en lugar de uno ciclico. */
    private Spring abandonCycles(Spring s) {
        return isCyclic(s) ? cyclicReference : s;
    }

    public void addLayoutComponent(String name, Component c) {
    }

    public void removeLayoutComponent(Component c) {
        componentConstraints.remove(c);
    }

    private static Dimension sumar(Dimension size, Insets insets) {
        return new Dimension(size.width + insets.left + insets.right,
                size.height + insets.top + insets.bottom);
    }

    /**
     * Mide el contenedor por su ancho y su alto, no por sus bordes este y sur.
     *
     * <p>Parece lo mismo -- el este es el oeste mas el ancho, y el oeste del contenedor es cero --
     * y no lo es: pedirle un numero al este obliga a resolver una suma, y resolver una suma pide el
     * preferido de sus partes, y el preferido del contenedor vuelve a este metodo. Preguntarle
     * directamente al ancho corta esa vuelta.
     */
    private Dimension medir(Container parent, int cual) {
        setParent(parent);
        Constraints pc = getConstraints(parent);
        int w = valor(abandonCycles(pc.getWidth()), cual);
        int h = valor(abandonCycles(pc.getHeight()), cual);
        return sumar(new Dimension(w, h), parent.getInsets());
    }

    private static int valor(Spring s, int cual) {
        if (s == null) {
            return 0;
        }
        if (cual == 0) {
            return s.getMinimumValue();
        }
        if (cual == 1) {
            return s.getPreferredValue();
        }
        return s.getMaximumValue();
    }

    public Dimension minimumLayoutSize(Container parent) {
        return medir(parent, 0);
    }

    public Dimension preferredLayoutSize(Container parent) {
        return medir(parent, 1);
    }

    public Dimension maximumLayoutSize(Container parent) {
        return medir(parent, 2);
    }

    public void addLayoutComponent(Component component, Object constraints) {
        if (constraints instanceof Constraints) {
            putConstraints(component, (Constraints) constraints);
        }
    }

    public float getLayoutAlignmentX(Container p) {
        return 0.5f;
    }

    public float getLayoutAlignmentY(Container p) {
        return 0.5f;
    }

    /** Tira lo que se sabia de los valores; los tres numeros de cada resorte no cambian. */
    public void invalidateLayout(Container p) {
        cyclicSprings = null;
        acyclicSprings = null;
    }

    /**
     * Le da al contenedor sus restricciones de origen.
     *
     * <p>El noroeste del contenedor es siempre cero: es el origen de todo lo demas. El ancho y el
     * alto ya se los puso {@link #getConstraints}.
     */
    private void setParent(Container p) {
        Constraints pc = getConstraints(p);
        pc.setX(Spring.constant(0));
        pc.setY(Spring.constant(0));
    }

    private void putConstraints(Component component, Constraints constraints) {
        componentConstraints.put(component, constraints);
    }

    /**
     * Ata un borde de un componente a un borde de otro, a esa distancia fija.
     *
     * @throws NullPointerException si algun componente es nulo
     */
    public void putConstraint(String e1, Component c1, int pad, String e2, Component c2) {
        putConstraint(e1, c1, Spring.constant(pad), e2, c2);
    }

    /**
     * Lo mismo con una distancia elastica.
     *
     * <p><strong>El borde del otro componente se guarda como una referencia viva</strong>, no como
     * una foto. Es lo que hace que atar A a B y B a A sea un ciclo de verdad -- si se guardara el
     * resorte que B tenia en ese momento, el segundo lazo no cerraria y las dos posiciones saldrian
     * de una cuenta que parece razonable y no lo es. Ver {@link ReferenciaAlBorde}.
     *
     * @throws NullPointerException si algun componente es nulo
     */
    public void putConstraint(String e1, Component c1, Spring s, String e2, Component c2) {
        Constraints cs = getConstraints(c1);
        cs.setConstraint(e1, Spring.sum(s, new ReferenciaAlBorde(e2, c2, this)));
    }

    /**
     * Un resorte que es "el borde tal de aquel componente", resuelto cada vez que se lo consulta.
     *
     * <p>Sin esto, atar un componente a otro congelaria el estado del otro en ese instante; con
     * esto, mover al otro mueve a este, que es lo que uno espera de un acomodador por restricciones.
     * Y es tambien lo que permite que un ciclo se note: la vuelta se cierra de verdad.
     */
    private static class ReferenciaAlBorde extends Spring {

        private final String edgeName;
        private final Component c;
        private final SpringLayout l;

        ReferenciaAlBorde(String edgeName, Component c, SpringLayout l) {
            this.edgeName = edgeName;
            this.c = c;
            this.l = l;
        }

        private Spring apuntado() {
            return l.getConstraints(c).getConstraint(edgeName);
        }

        public int getMinimumValue() {
            return apuntado().getMinimumValue();
        }

        public int getPreferredValue() {
            return apuntado().getPreferredValue();
        }

        public int getMaximumValue() {
            return apuntado().getMaximumValue();
        }

        public int getValue() {
            return apuntado().getValue();
        }

        public void setValue(int size) {
            apuntado().setValue(size);
        }

        boolean isCyclic(SpringLayout l) {
            return l.isCyclic(apuntado());
        }

        public String toString() {
            return "SpringProxy for " + edgeName + " edge of " + c.getName();
        }
    }

    /**
     * Las restricciones de ese componente, creandolas si no tenia.
     *
     * <p>Nunca devuelve nulo: un componente sin restricciones puestas igual tiene las que salen de
     * su tamano.
     */
    public Constraints getConstraints(Component c) {
        Constraints result = componentConstraints.get(c);
        if (result == null) {
            result = new Constraints();
            componentConstraints.put(c, result);
        }
        return applyDefaults(c, result);
    }

    /**
     * Completa lo que falte con lo de siempre: pegado al origen y del tamano del componente.
     *
     * <p>Solo mientras queden menos de dos restricciones en el eje. Con dos ya esta determinado y
     * agregar una tercera descartaria una de las que el llamador puso.
     */
    private Constraints applyDefaults(Component c, Constraints cc) {
        if (cc.c == null) {
            cc.c = c;
        }
        if (cc.horizontalHistory.size() < 2) {
            porOmision(cc, WEST, Spring.constant(0), WIDTH, Spring.width(c),
                    cc.horizontalHistory);
        }
        if (cc.verticalHistory.size() < 2) {
            porOmision(cc, NORTH, Spring.constant(0), HEIGHT, Spring.height(c),
                    cc.verticalHistory);
        }
        return cc;
    }

    private static void porOmision(Constraints cc, String n1, Spring s1, String n2, Spring s2,
            List<String> history) {
        if (history.size() < 2 && !history.contains(n1)) {
            cc.setConstraint(n1, s1);
        }
        if (history.size() < 2 && !history.contains(n2)) {
            cc.setConstraint(n2, s2);
        }
    }

    /** El resorte de ese borde de ese componente, o nulo si no se puede derivar. */
    public Spring getConstraint(String edgeName, Component c) {
        return abandonCycles(getConstraints(c).getConstraint(edgeName));
    }

    /** Resuelve todos los resortes y coloca a cada hijo. */
    public void layoutContainer(Container parent) {
        setParent(parent);
        int n = parent.getComponentCount();
        getConstraints(parent).reset();
        for (int i = 0; i < n; i++) {
            getConstraints(parent.getComponent(i)).reset();
        }
        Insets insets = parent.getInsets();
        Constraints pc = getConstraints(parent);
        // Se le pone valor al ancho y al alto, no al este y al sur: ver la nota de `medir`.
        ponerValor(abandonCycles(pc.getX()), 0);
        ponerValor(abandonCycles(pc.getY()), 0);
        ponerValor(abandonCycles(pc.getWidth()),
                parent.getWidth() - insets.left - insets.right);
        ponerValor(abandonCycles(pc.getHeight()),
                parent.getHeight() - insets.top - insets.bottom);
        for (int i = 0; i < n; i++) {
            Component c = parent.getComponent(i);
            Constraints cc = getConstraints(c);
            int x = valorDe(abandonCycles(cc.getX()));
            int y = valorDe(abandonCycles(cc.getY()));
            int width = valorDe(abandonCycles(cc.getWidth()));
            int height = valorDe(abandonCycles(cc.getHeight()));
            c.setBounds(insets.left + x, insets.top + y, width, height);
        }
    }

    private static void ponerValor(Spring s, int valor) {
        if (s != null) {
            s.setValue(valor);
        }
    }

    /**
     * El valor de un resorte, tal cual.
     *
     * <p>{@link Spring#UNSET} no se traduce a cero: un componente atrapado en un ciclo queda con
     * una posicion absurda, y eso es visible y se investiga. Un cero se confunde con "esta arriba a
     * la izquierda" y no se investiga nunca.
     */
    private static int valorDe(Spring s) {
        if (s == null) {
            return 0;
        }
        return s.getValue();
    }
}
