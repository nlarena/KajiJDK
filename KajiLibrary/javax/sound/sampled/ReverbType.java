package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.ReverbType -- un ambiente de reverberacion.
 *
 * <p>Describe como suena un espacio, con cinco numeros: cuando llegan los primeros rebotes, con que
 * fuerza, cuando llegan los tardios, con que fuerza, y cuanto tarda todo en apagarse.
 *
 * <p>Los dos grupos de rebotes son lo que distingue un ambiente de otro. Los <b>tempranos</b> son los
 * pocos rebotes que llegan por separado y le dicen al oido el tamano del cuarto; los <b>tardios</b>
 * son la nube de rebotes ya indistinguibles, y dan la sensacion de material y de amplitud.
 *
 * <p>El constructor es protegido: los ambientes los define quien provee el mezclador, y se consiguen
 * por un {@link EnumControl} de tipo {@link EnumControl.Type#REVERB}.
 *
 * <p>La igualdad es por identidad --{@code this == obj}--, no por los cinco numeros. Dos ambientes con
 * los mismos valores pero de mezcladores distintos son distintos, que es lo que corresponde: no son
 * intercambiables.
 */
public class ReverbType {

    /** Como se llama. */
    private final String name;

    /** Microsegundos hasta los primeros rebotes. */
    private final int earlyReflectionDelay;

    /** Su fuerza, en decibeles. */
    private final float earlyReflectionIntensity;

    /** Microsegundos hasta los rebotes tardios. */
    private final int lateReflectionDelay;

    /** Su fuerza, en decibeles. */
    private final float lateReflectionIntensity;

    /** Microsegundos hasta que se apaga. */
    private final int decayTime;

    /** Protegido: los ambientes los define el proveedor del mezclador. */
    protected ReverbType(String name, int earlyReflectionDelay, float earlyReflectionIntensity,
                         int lateReflectionDelay, float lateReflectionIntensity, int decayTime) {
        this.name = name;
        this.earlyReflectionDelay = earlyReflectionDelay;
        this.earlyReflectionIntensity = earlyReflectionIntensity;
        this.lateReflectionDelay = lateReflectionDelay;
        this.lateReflectionIntensity = lateReflectionIntensity;
        this.decayTime = decayTime;
    }

    /** Como se llama. */
    public String getName() {
        return this.name;
    }

    /** Microsegundos hasta los primeros rebotes. */
    public final int getEarlyReflectionDelay() {
        return this.earlyReflectionDelay;
    }

    /** Su fuerza, en decibeles. */
    public final float getEarlyReflectionIntensity() {
        return this.earlyReflectionIntensity;
    }

    /** Microsegundos hasta los rebotes tardios. */
    public final int getLateReflectionDelay() {
        return this.lateReflectionDelay;
    }

    /** Su fuerza, en decibeles. */
    public final float getLateReflectionIntensity() {
        return this.lateReflectionIntensity;
    }

    /** Microsegundos hasta que se apaga. */
    public final int getDecayTime() {
        return this.decayTime;
    }

    /** Por identidad. Ver la nota de la clase. */
    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    /** El de identidad. */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * El nombre y los cinco numeros.
     *
     * <p>Dice {@code "late deflection delay"} donde deberia decir {@code "reflection"}. Es una errata
     * del JDK que esta ahi desde 1999 y se conserva: hay pruebas que comparan este texto.
     */
    @Override
    public final String toString() {
        return this.name
            + ", early reflection delay " + this.earlyReflectionDelay + " ns"
            + ", early reflection intensity " + this.earlyReflectionIntensity + " dB"
            + ", late deflection delay " + this.lateReflectionDelay + " ns"
            + ", late reflection intensity " + this.lateReflectionIntensity + " dB"
            + ", decay time " + this.decayTime;
    }
}
