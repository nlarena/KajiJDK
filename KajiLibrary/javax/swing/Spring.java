package javax.swing;

import java.awt.Component;

/**
 * Una distancia elastica: un minimo, un preferido, un maximo, y el valor de ahora.
 *
 * <h2>Tres numeros y uno</h2>
 *
 * <p>Los tres primeros son lo que el resorte <em>puede</em> medir; el cuarto es lo que mide en este
 * momento. Un acomodador reparte el espacio disponible ajustando los valores, y cada resorte cede o
 * se estira dentro de sus tres numeros.
 *
 * <h2>La tension, que es lo que hace que el reparto sea justo</h2>
 *
 * <p>La <em>tension</em> de un resorte es cuanto se aparto de su preferido, medido en fracciones de
 * lo que le queda para llegar a su tope. Vale cero en el preferido, uno en el maximo y menos uno en
 * el minimo. Repartir el espacio no es darle a cada uno lo mismo: es ponerlos a todos con la misma
 * tension, y por eso el que tiene mas margen se lleva mas.
 *
 * <p>Un resorte que no puede moverse -- minimo, preferido y maximo iguales -- tiene rango cero, y su
 * tension es una division por cero. Sale infinito o NaN, y esta bien que salga: la pregunta "cuanto
 * te estiraste, en fracciones de lo que podias" no tiene respuesta cuando no podia nada.
 *
 * <h2>Se combinan, y el resultado es otro resorte</h2>
 *
 * <p>{@link #sum}, {@link #max}, {@link #minus} y {@link #scale} devuelven resortes que <em>miran</em>
 * a los que se les dieron. No copian sus numeros: si el de adentro cambia, el de afuera cambia. Por
 * eso {@link #width} y {@link #height} son utiles -- son un resorte que siempre dice lo que el
 * componente mide ahora --, y por eso ponerle un valor a una suma se lo reparte a sus dos partes.
 *
 * <p>Ese reparto usa la tension: la suma le pone a la primera parte su misma tension y a la segunda
 * lo que sobra. Es lo que hace que estirar una fila de resortes distintos quede parejo.
 */
public abstract class Spring {

    /**
     * "Todavia no se sabe".
     *
     * <p>Es {@link Integer#MIN_VALUE} y no cero ni menos uno porque un resorte puede medir
     * legitimamente cualquiera de los dos -- y tambien negativo, que es lo que devuelve
     * {@link #minus}.
     */
    public static final int UNSET = Integer.MIN_VALUE;

    /** Para las subclases. */
    protected Spring() {
    }

    public abstract int getMinimumValue();

    public abstract int getPreferredValue();

    public abstract int getMaximumValue();

    /** Lo que mide ahora. */
    public abstract int getValue();

    /** Le pone ese valor; {@link #UNSET} lo devuelve a "todavia no se sabe". */
    public abstract void setValue(int value);

    /** Cuanto margen le queda de este lado del preferido. */
    private double range(boolean contract) {
        return contract ? getPreferredValue() - getMinimumValue()
                : getMaximumValue() - getPreferredValue();
    }

    /** Ver la nota de la clase. */
    double getStrain() {
        double delta = (getValue() - getPreferredValue());
        return delta / range(getValue() < getPreferredValue());
    }

    /** Lo pone en el valor que le corresponde a esa tension. */
    void setStrain(double strain) {
        setValue(getPreferredValue() + (int) (strain * range(strain < 0)));
    }

    /** Si depende de si mismo a traves de ese acomodador. */
    boolean isCyclic(SpringLayout l) {
        return false;
    }

    /** La parte comun de los resortes que guardan su valor. */
    abstract static class AbstractSpring extends Spring {

        protected int size = UNSET;

        public int getValue() {
            return size != UNSET ? size : getPreferredValue();
        }

        public final void setValue(int size) {
            if (this.size == size) {
                return;
            }
            if (size == UNSET) {
                clear();
            } else {
                setNonClearValue(size);
            }
        }

        protected void clear() {
            size = UNSET;
        }

        protected void setNonClearValue(int size) {
            this.size = size;
        }
    }

    /** Tres numeros fijos. */
    private static class StaticSpring extends AbstractSpring {

        protected int min;
        protected int pref;
        protected int max;

        StaticSpring(int pref) {
            this(pref, pref, pref);
        }

        StaticSpring(int min, int pref, int max) {
            this.min = min;
            this.pref = pref;
            this.max = max;
        }

        public String toString() {
            return "StaticSpring [" + min + ", " + pref + ", " + max + "]";
        }

        public int getMinimumValue() {
            return min;
        }

        public int getPreferredValue() {
            return pref;
        }

        public int getMaximumValue() {
            return max;
        }
    }

    /**
     * El mismo resorte al reves.
     *
     * <p>El minimo pasa a ser menos el maximo, no menos el minimo: al dar vuelta el signo, el que
     * mas medía pasa a ser el que menos mide.
     */
    private static class NegativeSpring extends Spring {

        private final Spring s;

        NegativeSpring(Spring s) {
            this.s = s;
        }

        public int getMinimumValue() {
            return -s.getMaximumValue();
        }

        public int getPreferredValue() {
            return -s.getPreferredValue();
        }

        public int getMaximumValue() {
            return -s.getMinimumValue();
        }

        public int getValue() {
            return -s.getValue();
        }

        public void setValue(int size) {
            if (size == UNSET) {
                s.setValue(UNSET);
            } else {
                s.setValue(-size);
            }
        }

        boolean isCyclic(SpringLayout l) {
            return s.isCyclic(l);
        }
    }

    /**
     * El mismo resorte multiplicado.
     *
     * <p>Con factor negativo el minimo y el maximo se cruzan, por el mismo motivo que en
     * {@link NegativeSpring}.
     */
    private static class ScaleSpring extends Spring {

        private final float factor;
        private final Spring s;

        ScaleSpring(Spring s, float factor) {
            this.s = s;
            this.factor = factor;
        }

        public int getMinimumValue() {
            return Math.round((factor < 0 ? s.getMaximumValue() : s.getMinimumValue()) * factor);
        }

        public int getPreferredValue() {
            return Math.round(s.getPreferredValue() * factor);
        }

        public int getMaximumValue() {
            return Math.round((factor < 0 ? s.getMinimumValue() : s.getMaximumValue()) * factor);
        }

        public int getValue() {
            return Math.round(s.getValue() * factor);
        }

        public void setValue(int value) {
            if (value == UNSET) {
                s.setValue(UNSET);
            } else {
                s.setValue(Math.round(value / factor));
            }
        }

        boolean isCyclic(SpringLayout l) {
            return s.isCyclic(l);
        }
    }

    /**
     * El ancho de un componente, siempre al dia.
     *
     * <p>El maximo se recorta a {@link Short#MAX_VALUE}. Un componente sin tope devuelve
     * {@link Integer#MAX_VALUE}, y con eso no se puede hacer aritmetica -- una suma de dos se
     * desborda y da negativo --. Recortar es lo que hace el JDK y es lo que mantiene las cuentas
     * sanas.
     */
    private static class WidthSpring extends AbstractSpring {

        private final Component c;

        WidthSpring(Component c) {
            this.c = c;
        }

        public int getMinimumValue() {
            return c.getMinimumSize().width;
        }

        public int getPreferredValue() {
            return c.getPreferredSize().width;
        }

        public int getMaximumValue() {
            return Math.min(Short.MAX_VALUE, c.getMaximumSize().width);
        }
    }

    /** El alto de un componente; ver {@link WidthSpring}. */
    private static class HeightSpring extends AbstractSpring {

        private final Component c;

        HeightSpring(Component c) {
            this.c = c;
        }

        public int getMinimumValue() {
            return c.getMinimumSize().height;
        }

        public int getPreferredValue() {
            return c.getPreferredSize().height;
        }

        public int getMaximumValue() {
            return Math.min(Short.MAX_VALUE, c.getMaximumSize().height);
        }
    }

    /**
     * La parte comun de los que combinan dos.
     *
     * <p>Hereda de {@link StaticSpring} para reusar sus tres campos como <em>cache</em>: combinar es
     * caro cuando los de adentro son a su vez combinaciones, y el resultado no cambia hasta que
     * alguien limpie. De ahi que {@link #clear} tenga que limpiar tambien a los dos de adentro.
     */
    abstract static class CompoundSpring extends StaticSpring {

        protected Spring s1;
        protected Spring s2;

        /**
         * No llama a {@link #clear}: {@code super(UNSET)} ya deja los tres campos en UNSET, y
         * limpiar desreferenciaria los dos resortes -- que es justamente lo que hace que el JDK
         * acepte un nulo en {@link Spring#sum} sin quejarse hasta que se lo use.
         */
        CompoundSpring(Spring s1, Spring s2) {
            super(UNSET);
            this.s1 = s1;
            this.s2 = s2;
        }

        public String toString() {
            return "CompoundSpring of " + s1 + " and " + s2;
        }

        protected void clear() {
            super.clear();
            min = UNSET;
            pref = UNSET;
            max = UNSET;
            s1.setValue(UNSET);
            s2.setValue(UNSET);
        }

        protected abstract int op(int x, int y);

        public int getMinimumValue() {
            if (min == UNSET) {
                min = op(s1.getMinimumValue(), s2.getMinimumValue());
            }
            return min;
        }

        public int getPreferredValue() {
            if (pref == UNSET) {
                pref = op(s1.getPreferredValue(), s2.getPreferredValue());
            }
            return pref;
        }

        public int getMaximumValue() {
            if (max == UNSET) {
                max = op(s1.getMaximumValue(), s2.getMaximumValue());
            }
            return max;
        }

        public int getValue() {
            if (size == UNSET) {
                size = op(s1.getValue(), s2.getValue());
            }
            return size;
        }

        boolean isCyclic(SpringLayout l) {
            return l.isCyclic(s1) || l.isCyclic(s2);
        }
    }

    /** Uno detras del otro. */
    private static class SumSpring extends CompoundSpring {

        SumSpring(Spring s1, Spring s2) {
            super(s1, s2);
        }

        /**
         * Suma lisa y llana.
         *
         * <p>No respeta el centinela {@link Spring#UNSET}: sumarle cinco a "no se sabe" da
         * "no se sabe mas cinco". Esta medido contra el JDK, que hace lo mismo. Tratarlo aparte
         * seria mas prolijo y daria otro numero.
         */
        protected int op(int x, int y) {
            return x + y;
        }

        /**
         * Reparte el valor entre los dos con la misma tension; ver la nota de la clase.
         *
         * <p>Al segundo se le da lo que sobra y no su tension: asi la suma de los dos es
         * exactamente lo pedido, sin errores de redondeo acumulados.
         */
        protected void setNonClearValue(int size) {
            super.setNonClearValue(size);
            s1.setStrain(this.getStrain());
            s2.setValue(size - s1.getValue());
        }
    }

    /** El mayor de los dos, en los tres numeros. */
    private static class MaxSpring extends CompoundSpring {

        MaxSpring(Spring s1, Spring s2) {
            super(s1, s2);
        }

        protected int op(int x, int y) {
            return Math.max(x, y);
        }

        /** A los dos el mismo valor: los dos tienen que llegar hasta ahi. */
        protected void setNonClearValue(int size) {
            super.setNonClearValue(size);
            s1.setValue(size);
            s2.setValue(size);
        }
    }

    /** Un resorte que no se mueve. */
    public static Spring constant(int pref) {
        return new StaticSpring(pref);
    }

    /** Un resorte con esos tres numeros. */
    public static Spring constant(int min, int pref, int max) {
        return new StaticSpring(min, pref, max);
    }

    /**
     * El mismo, al reves.
     *
     * <p><strong>No comprueba el nulo</strong>, y esta medido: el JDK lo acepta y arma el resorte
     * igual. Lo que pasa despues es que revienta al usarlo. Rechazarlo aca adelantaria el error, que
     * suena mejor y no es lo mismo.
     */
    public static Spring minus(Spring s) {
        return new NegativeSpring(s);
    }

    /** Uno detras del otro; tampoco comprueba nulos. Ver {@link #minus}. */
    public static Spring sum(Spring s1, Spring s2) {
        return new SumSpring(s1, s2);
    }

    /** El mayor de los dos; tampoco comprueba nulos. Ver {@link #minus}. */
    public static Spring max(Spring s1, Spring s2) {
        return new MaxSpring(s1, s2);
    }

    /** La distancia del segundo al primero; es la suma con el segundo dado vuelta. */
    static Spring difference(Spring s1, Spring s2) {
        return sum(s1, minus(s2));
    }

    /**
     * El mismo, multiplicado.
     *
     * @throws NullPointerException si es nulo
     */
    public static Spring scale(Spring s, float factor) {
        checkArg(s);
        return new ScaleSpring(s, factor);
    }

    /**
     * El ancho de ese componente, siempre al dia; ver la nota de la clase.
     *
     * @throws NullPointerException si es nulo
     */
    public static Spring width(Component c) {
        checkArg(c);
        return new WidthSpring(c);
    }

    /**
     * El alto de ese componente.
     *
     * @throws NullPointerException si es nulo
     */
    public static Spring height(Component c) {
        checkArg(c);
        return new HeightSpring(c);
    }

    private static void checkArg(Object arg) {
        if (arg == null) {
            throw new NullPointerException("Argument must not be null");
        }
    }
}
