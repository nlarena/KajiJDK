package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.BooleanControl -- una perilla de dos posiciones.
 *
 * <p>Silencio, reverberacion prendida o apagada. Ademas del valor lleva las <b>etiquetas</b> de cada
 * estado, para que una interfaz pueda mostrar "Silencio"/"Sonido" en lugar de "true"/"false" -- y
 * traducidas, si el proveedor las trae asi.
 *
 * <p>El constructor corto usa {@code "true"} y {@code "false"} como etiquetas, que es lo que se ve
 * cuando el proveedor no se tomo el trabajo.
 */
public abstract class BooleanControl extends Control {

    /** Como se llama el estado verdadero. */
    private final String trueStateLabel;

    /** Como se llama el falso. */
    private final String falseStateLabel;

    /** En que esta. */
    private boolean value;

    /**
     * @param initialValue en que arranca
     * @param trueStateLabel como mostrar el estado verdadero
     * @param falseStateLabel como mostrar el falso
     */
    protected BooleanControl(Type type, boolean initialValue, String trueStateLabel,
                             String falseStateLabel) {
        super(type);
        this.value = initialValue;
        this.trueStateLabel = trueStateLabel;
        this.falseStateLabel = falseStateLabel;
    }

    /** Idem, con las etiquetas {@code "true"} y {@code "false"}. */
    protected BooleanControl(Type type, boolean initialValue) {
        this(type, initialValue, "true", "false");
    }

    /** Cambia el estado. */
    public void setValue(boolean value) {
        this.value = value;
    }

    /** En que esta. */
    public boolean getValue() {
        return this.value;
    }

    /** Como se muestra ese estado. */
    public String getStateLabel(boolean state) {
        if (state) {
            return this.trueStateLabel;
        }
        return this.falseStateLabel;
    }

    /** El del control, mas el valor actual con su etiqueta. */
    @Override
    public String toString() {
        return super.toString() + " with current value: " + getStateLabel(getValue());
    }

    /**
     * Los tipos de perilla booleana que la plataforma nombra.
     *
     * <p>Son dos y hay muchas mas en la practica: un proveedor puede definir las suyas, y por eso el
     * constructor es protegido en lugar de que esto sea un enum.
     */
    public static class Type extends Control.Type {

        /** Silenciar. */
        public static final Type MUTE = new Type("Mute");

        /** Aplicar reverberacion. */
        public static final Type APPLY_REVERB = new Type("Apply Reverb");

        /** Protegido: los tipos los define quien provee el mezclador. */
        protected Type(String name) {
            super(name);
        }
    }
}
