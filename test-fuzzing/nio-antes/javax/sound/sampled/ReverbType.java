package javax.sound.sampled;

/**
 * KajiLibrary's javax.sound.sampled.ReverbType -- a reverb environment.
 *
 * <p>It describes how a space sounds, with five numbers: when the first reflections arrive, how
 * strong, when the late ones arrive, how strong, and how long everything takes to die away.
 *
 * <p>The two groups of reflections are what tells one environment from another. The <b>early</b>
 * ones are the few reflections that arrive separately and tell the ear the size of the room; the
 * <b>late</b> ones are the cloud of reflections no longer distinguishable, and they give the sense
 * of material and of spaciousness.
 *
 * <p>The constructor is protected: the environments are defined by whoever provides the mixer, and
 * they are obtained through an {@link EnumControl} of type {@link EnumControl.Type#REVERB}.
 *
 * <p>Equality is by identity --{@code this == obj}--, not by the five numbers. Two environments
 * with the same values but from different mixers are different, which is right: they are not
 * interchangeable.
 */
public class ReverbType {

    /** What it is called. */
    private final String name;

    /** Microseconds until the first reflections. */
    private final int earlyReflectionDelay;

    /** Their strength, in decibels. */
    private final float earlyReflectionIntensity;

    /** Microseconds until the late reflections. */
    private final int lateReflectionDelay;

    /** Their strength, in decibels. */
    private final float lateReflectionIntensity;

    /** Microseconds until it dies away. */
    private final int decayTime;

    /** Protected: the environments are defined by the mixer's provider. */
    protected ReverbType(String name, int earlyReflectionDelay, float earlyReflectionIntensity,
                         int lateReflectionDelay, float lateReflectionIntensity, int decayTime) {
        this.name = name;
        this.earlyReflectionDelay = earlyReflectionDelay;
        this.earlyReflectionIntensity = earlyReflectionIntensity;
        this.lateReflectionDelay = lateReflectionDelay;
        this.lateReflectionIntensity = lateReflectionIntensity;
        this.decayTime = decayTime;
    }

    /** What it is called. */
    public String getName() {
        return this.name;
    }

    /** Microseconds until the first reflections. */
    public final int getEarlyReflectionDelay() {
        return this.earlyReflectionDelay;
    }

    /** Their strength, in decibels. */
    public final float getEarlyReflectionIntensity() {
        return this.earlyReflectionIntensity;
    }

    /** Microseconds until the late reflections. */
    public final int getLateReflectionDelay() {
        return this.lateReflectionDelay;
    }

    /** Their strength, in decibels. */
    public final float getLateReflectionIntensity() {
        return this.lateReflectionIntensity;
    }

    /** Microseconds until it dies away. */
    public final int getDecayTime() {
        return this.decayTime;
    }

    /** By identity. See the class note. */
    @Override
    public final boolean equals(Object obj) {
        return super.equals(obj);
    }

    /** The identity one. */
    @Override
    public final int hashCode() {
        return super.hashCode();
    }

    /**
     * The name and the five numbers.
     *
     * <p>It says {@code "late deflection delay"} where it should say {@code "reflection"}. It is a
     * JDK typo that has been there since 1999 and is kept: there are tests that compare this text.
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
