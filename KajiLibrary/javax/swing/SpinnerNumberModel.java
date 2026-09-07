package javax.swing;

import java.io.Serializable;

/**
 * Una secuencia de numeros para un {@link JSpinner}.
 *
 * <h2>El tipo lo manda el valor, no los limites</h2>
 *
 * <p>Si el valor es un {@link Integer}, sumar el paso da un {@code Integer}; si es un
 * {@link Double}, da un {@code Double}. Los limites y el paso pueden ser de otro tipo -- se los
 * consulta con {@code compareTo} y con {@code doubleValue}/{@code longValue} --, pero el que sale
 * por {@link #getNextValue} tiene el tipo del valor de ahora.
 *
 * <p>Eso importa mas de lo que parece: un modelo que arranca en {@code Integer.valueOf(0)} con paso
 * {@code Double.valueOf(0.5)} avanza de a cero, porque {@code longValue()} de 0.5 es 0. No es un
 * error del modelo; es que el tipo del valor decide la aritmetica.
 *
 * <h2>Los limites no recortan</h2>
 *
 * <p>{@link #setValue} acepta cualquier numero, incluso fuera de rango. Los limites solo se usan
 * para decidir si hay siguiente o anterior: al pasarse, la flecha devuelve nulo y el control la
 * apaga. Ver la nota de {@link SpinnerModel}.
 */
public class SpinnerNumberModel extends AbstractSpinnerModel implements Serializable {

    private Number stepSize;
    private Number value;
    private Comparable<?> minimum;
    private Comparable<?> maximum;

    /**
     * Con valor, limites y paso.
     *
     * <p>Los limites pueden ser nulos, que es como decir "sin tope".
     *
     * @throws IllegalArgumentException si el valor o el paso son nulos, o si no se cumple
     *     minimo &lt;= valor &lt;= maximo.
     */
    public SpinnerNumberModel(Number value, Comparable<?> minimum, Comparable<?> maximum,
            Number stepSize) {
        if (!(value != null && stepSize != null)) {
            throw new IllegalArgumentException("value and stepSize must be non-null");
        }
        if (!((minimum == null || comparar(minimum, value) <= 0)
                && (maximum == null || comparar(maximum, value) >= 0))) {
            throw new IllegalArgumentException("(minimum <= value <= maximum) is false");
        }
        this.value = value;
        this.minimum = minimum;
        this.maximum = maximum;
        this.stepSize = stepSize;
    }

    /** Con enteros; el valor sale como {@link Integer}. */
    public SpinnerNumberModel(int value, int minimum, int maximum, int stepSize) {
        this(Integer.valueOf(value), Integer.valueOf(minimum), Integer.valueOf(maximum),
                Integer.valueOf(stepSize));
    }

    /** Con dobles; el valor sale como {@link Double}. */
    public SpinnerNumberModel(double value, double minimum, double maximum, double stepSize) {
        this(Double.valueOf(value), Double.valueOf(minimum), Double.valueOf(maximum),
                Double.valueOf(stepSize));
    }

    /** Desde cero, de a uno, sin topes. */
    public SpinnerNumberModel() {
        this(Integer.valueOf(0), null, null, Integer.valueOf(1));
    }

    /**
     * Compara un limite con un valor.
     *
     * <p>Concentra el descarte de generico en un solo lugar: el limite se declara
     * {@code Comparable<?>} y solo se lo puede llamar tratandolo como crudo.
     */
    @SuppressWarnings("unchecked")
    private static int comparar(Comparable<?> limite, Object valor) {
        return ((Comparable<Object>) limite).compareTo(valor);
    }

    /**
     * El piso; nulo quita el tope.
     *
     * <p>No se comprueba contra el valor de ahora: se puede poner un piso por encima del valor, y
     * lo que pasa entonces es que la flecha de bajar se apaga.
     */
    public void setMinimum(Comparable<?> minimum) {
        if ((minimum == null) ? (this.minimum != null) : !minimum.equals(this.minimum)) {
            this.minimum = minimum;
            fireStateChanged();
        }
    }

    public Comparable<?> getMinimum() {
        return minimum;
    }

    /** El techo; nulo quita el tope. */
    public void setMaximum(Comparable<?> maximum) {
        if ((maximum == null) ? (this.maximum != null) : !maximum.equals(this.maximum)) {
            this.maximum = maximum;
            fireStateChanged();
        }
    }

    public Comparable<?> getMaximum() {
        return maximum;
    }

    /**
     * Cuanto avanza cada flecha.
     *
     * @throws IllegalArgumentException si es nulo.
     */
    public void setStepSize(Number stepSize) {
        if (stepSize == null) {
            throw new IllegalArgumentException("null stepSize");
        }
        if (!stepSize.equals(this.stepSize)) {
            this.stepSize = stepSize;
            fireStateChanged();
        }
    }

    public Number getStepSize() {
        return stepSize;
    }

    /**
     * Suma el paso en esa direccion.
     *
     * <p>El tipo del resultado es el del valor de ahora; ver la nota de la clase. Si el resultado
     * se sale de los limites devuelve nulo, que es como el modelo dice "no hay mas".
     */
    private Number correr(int dir) {
        Number nuevo;
        if ((value instanceof Float) || (value instanceof Double)) {
            double v = value.doubleValue() + (stepSize.doubleValue() * (double) dir);
            if (value instanceof Double) {
                nuevo = Double.valueOf(v);
            } else {
                nuevo = Float.valueOf((float) v);
            }
        } else {
            long v = value.longValue() + (stepSize.longValue() * (long) dir);
            if (value instanceof Long) {
                nuevo = Long.valueOf(v);
            } else if (value instanceof Integer) {
                nuevo = Integer.valueOf((int) v);
            } else if (value instanceof Short) {
                nuevo = Short.valueOf((short) v);
            } else {
                nuevo = Byte.valueOf((byte) v);
            }
        }
        if ((maximum != null) && comparar(maximum, nuevo) < 0) {
            return null;
        }
        if ((minimum != null) && comparar(minimum, nuevo) > 0) {
            return null;
        }
        return nuevo;
    }

    /** El siguiente, o nulo si pasa el techo. */
    public Object getNextValue() {
        return correr(1);
    }

    /** El anterior, o nulo si pasa el piso. */
    public Object getPreviousValue() {
        return correr(-1);
    }

    /** El valor, ya como numero. */
    public Number getNumber() {
        return value;
    }

    public Object getValue() {
        return value;
    }

    /**
     * Cambia el valor.
     *
     * <p>No recorta contra los limites; ver la nota de la clase.
     *
     * @throws IllegalArgumentException si no es un numero.
     */
    public void setValue(Object value) {
        if ((value == null) || !(value instanceof Number)) {
            throw new IllegalArgumentException("illegal value");
        }
        if (!value.equals(this.value)) {
            this.value = (Number) value;
            fireStateChanged();
        }
    }
}
