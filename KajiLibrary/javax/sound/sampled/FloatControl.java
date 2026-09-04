package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.FloatControl -- una perilla continua.
 *
 * <p>Volumen, balance, panoramica. Lleva un rango, una precision, unidades, y tres etiquetas para los
 * extremos y el centro.
 *
 * <h2>La precision no es el paso</h2>
 *
 * <p>{@link #getPrecision} es la <b>menor diferencia que el dispositivo puede representar</b>. Fijar un
 * valor que no sea multiplo de ella no falla: se redondea al mas cercano que si lo sea. Por eso
 * {@link #getValue} despues de {@link #setValue} puede devolver otra cosa, y comparar por igualdad ahi
 * es un error.
 *
 * <h2>{@link #shift} es un cambio gradual, no inmediato</h2>
 *
 * <p>Pide ir de un valor a otro en cierto tiempo. Existe porque un cambio brusco de volumen produce un
 * chasquido audible, y hacerlo a mano desde Java --con un hilo y pausas-- suena peor todavia.
 *
 * <p>El dispositivo puede no soportarlo, y ahi salta directo al valor final. La documentacion lo
 * permite explicitamente, asi que un programa no puede confiar en el desvanecimiento.
 */
public abstract class FloatControl extends Control {

    /** El minimo. */
    private final float minimum;

    /** El maximo. */
    private final float maximum;

    /** La menor diferencia representable. Ver la nota de la clase. */
    private final float precision;

    /** Cada cuantos microsegundos cambia durante un {@link #shift}. */
    private final int updatePeriod;

    /** En que unidades esta el valor. */
    private final String units;

    /** Como mostrar el minimo. */
    private final String minLabel;

    /** Como mostrar el centro. */
    private final String midLabel;

    /** Como mostrar el maximo. */
    private final String maxLabel;

    /** El valor actual. */
    private float value;

    /** El constructor completo. */
    protected FloatControl(Type type, float minimum, float maximum, float precision,
                           int updatePeriod, float initialValue, String units, String minLabel,
                           String midLabel, String maxLabel) {
        super(type);
        this.minimum = minimum;
        this.maximum = maximum;
        this.precision = precision;
        this.updatePeriod = updatePeriod;
        this.value = initialValue;
        this.units = units;
        this.minLabel = minLabel;
        this.midLabel = midLabel;
        this.maxLabel = maxLabel;
    }

    /** Idem, con las tres etiquetas vacias. */
    protected FloatControl(Type type, float minimum, float maximum, float precision,
                           int updatePeriod, float initialValue, String units) {
        this(type, minimum, maximum, precision, updatePeriod, initialValue, units, "", "", "");
    }

    /**
     * Fija el valor.
     *
     * @throws IllegalArgumentException si esta fuera del rango
     */
    public void setValue(float newValue) {
        if (newValue > this.maximum || newValue < this.minimum) {
            if (newValue > this.maximum) {
                throw new IllegalArgumentException("Requested value " + newValue
                    + " exceeds allowable maximum value " + this.maximum + ".");
            }
            throw new IllegalArgumentException("Requested value " + newValue
                + " is smaller than allowable minimum value " + this.minimum + ".");
        }
        this.value = newValue;
    }

    /** El valor actual. Ver la nota de la clase: puede no ser el que se fijo. */
    public float getValue() {
        return this.value;
    }

    /** El maximo. */
    public float getMaximum() {
        return this.maximum;
    }

    /** El minimo. */
    public float getMinimum() {
        return this.minimum;
    }

    /** En que unidades esta. */
    public String getUnits() {
        return this.units;
    }

    /** Como mostrar el minimo. */
    public String getMinLabel() {
        return this.minLabel;
    }

    /** Como mostrar el centro. */
    public String getMidLabel() {
        return this.midLabel;
    }

    /** Como mostrar el maximo. */
    public String getMaxLabel() {
        return this.maxLabel;
    }

    /** La menor diferencia representable. Ver la nota de la clase. */
    public float getPrecision() {
        return this.precision;
    }

    /** Cada cuantos microsegundos cambia durante un {@link #shift}. */
    public int getUpdatePeriod() {
        return this.updatePeriod;
    }

    /**
     * Va de un valor a otro en ese tiempo.
     *
     * <p>Esta implementacion salta directo al valor final, que es lo que la documentacion permite
     * cuando el dispositivo no sabe hacer el cambio gradual. Ver la nota de la clase.
     *
     * @param microseconds cuanto deberia tardar
     * @throws IllegalArgumentException si alguno de los dos valores esta fuera del rango
     */
    public void shift(float from, float to, int microseconds) {
        setValue(from);
        setValue(to);
    }

    /** El del control, el valor con sus unidades, y el rango. */
    @Override
    public String toString() {
        return super.toString() + " with current value: " + getValue() + " " + getUnits()
            + " (range: " + getMinimum() + " - " + getMaximum() + ")";
    }

    /**
     * Los tipos de perilla continua que la plataforma nombra.
     *
     * <p>{@link #VOLUME} y {@link #MASTER_GAIN} se confunden: la primera es el volumen de una linea, la
     * segunda la ganancia global del mezclador. Cambiar la que no es afecta a otras lineas.
     *
     * <p>{@link #PAN} y {@link #BALANCE} tampoco son lo mismo. La panoramica ubica una fuente
     * <b>mono</b> entre los dos parlantes; el balance ajusta la proporcion entre los canales de algo
     * que <b>ya es estereo</b>. Aplicar panoramica a material estereo lo colapsa a mono.
     */
    public static class Type extends Control.Type {

        /** Ganancia global del mezclador, en decibeles. Ver la nota de la clase. */
        public static final Type MASTER_GAIN = new Type("Master Gain");

        /** Cuanto se manda al efecto auxiliar. */
        public static final Type AUX_SEND = new Type("AUX Send");

        /** Cuanto vuelve del efecto auxiliar. */
        public static final Type AUX_RETURN = new Type("AUX Return");

        /** Cuanto se manda a la reverberacion. */
        public static final Type REVERB_SEND = new Type("Reverb Send");

        /** Cuanto vuelve de la reverberacion. */
        public static final Type REVERB_RETURN = new Type("Reverb Return");

        /** Volumen de esta linea. Ver la nota de la clase. */
        public static final Type VOLUME = new Type("Volume");

        /** Ubicacion de una fuente mono entre los parlantes. Ver la nota de la clase. */
        public static final Type PAN = new Type("Pan");

        /** Proporcion entre los canales de una fuente estereo. */
        public static final Type BALANCE = new Type("Balance");

        /** Velocidad de reproduccion, que cambia el tono. */
        public static final Type SAMPLE_RATE = new Type("Sample Rate");

        /** Protegido: los tipos los define quien provee el mezclador. */
        protected Type(String name) {
            super(name);
        }
    }
}
