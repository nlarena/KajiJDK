package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.EnumControl -- una perilla de opciones discretas.
 *
 * <p>Un valor elegido de una lista cerrada. El caso tipico es el ambiente de reverberacion, donde las
 * opciones son objetos {@link ReverbType}.
 *
 * <p>Los valores son {@link Object} y no un tipo mas preciso: una opcion puede ser un texto, un numero
 * o un objeto entero, segun lo que la perilla represente.
 *
 * <p>{@link #setValue} solo acepta uno de los valores de {@link #getValues}, comparando por
 * {@code equals}. Cualquier otro lanza {@link IllegalArgumentException}: es una lista cerrada, no una
 * sugerencia.
 */
public abstract class EnumControl extends Control {

    /** Las opciones. */
    private final Object[] values;

    /** La elegida. */
    private Object value;

    /**
     * @param values las opciones posibles
     * @param value la inicial, que tiene que estar entre ellas
     */
    protected EnumControl(Type type, Object[] values, Object value) {
        super(type);
        this.values = values;
        this.value = value;
    }

    /**
     * Elige una opcion.
     *
     * @throws IllegalArgumentException si no esta entre las posibles
     */
    public void setValue(Object value) {
        if (!isValueSupported(value)) {
            throw new IllegalArgumentException("Requested value " + value + " is not supported.");
        }
        this.value = value;
    }

    /** La elegida. */
    public Object getValue() {
        return this.value;
    }

    /** Las opciones; una copia del arreglo. */
    public Object[] getValues() {
        Object[] copy = new Object[this.values.length];
        int i = 0;
        while (i < this.values.length) {
            copy[i] = this.values[i];
            i = i + 1;
        }
        return copy;
    }

    /** El del control, mas la opcion elegida. */
    @Override
    public String toString() {
        return super.toString() + " with current value: " + getValue();
    }

    /** Si ese valor esta entre los posibles. */
    private boolean isValueSupported(Object value) {
        int i = 0;
        while (i < this.values.length) {
            if (value == null) {
                if (this.values[i] == null) {
                    return true;
                }
            } else if (value.equals(this.values[i])) {
                return true;
            }
            i = i + 1;
        }
        return false;
    }

    /**
     * Los tipos de perilla de opciones.
     *
     * <p>Solo uno predefinido: la reverberacion, cuyas opciones son {@link ReverbType}.
     */
    public static class Type extends Control.Type {

        /** El ambiente de reverberacion; sus valores son {@link ReverbType}. */
        public static final Type REVERB = new Type("Reverb");

        /** Protegido: los tipos los define quien provee el mezclador. */
        protected Type(String name) {
            super(name);
        }
    }
}
