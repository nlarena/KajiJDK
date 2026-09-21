package java.security;

// The parameters of a deterministic random bit generator (DRBG), according to NIST SP 800-90Ar1.
//
// ===============================================================================================
// WHAT A DRBG IS AND WHY THE PARAMETERS ARE THREE CLASSES
// ===============================================================================================
//
// A DRBG does not produce randomness: it **stretches** it. It is seeded once with real entropy and
// from there it generates bits deterministically. All of its security is in the seed and in its
// internal state not leaking.
//
// Out of that come the three moments each class describes:
//
//   - `Instantiation` is the creation: how much strength is asked for, whether it is going to be
//     possible to reseed, and a "personalisation string" that separates this generator from another
//     seeded with the same entropy. That string is what keeps two cloned machines —two VMs of the
//     same image— from producing the same sequence.
//   - `NextBytes` is each request for bits.
//   - `Reseed` is mixing fresh entropy in again.
//
// Prediction resistance is the property that is most misunderstood: it means that whoever sees the
// internal state **now** cannot predict the bits that come afterwards, because before generating
// them new entropy is mixed in. It is expensive —it asks for real entropy at every call— and that
// is why it is asked for per operation and is not left turned on.
//
// This class is a **descriptor** and nothing else: it does not generate a single bit. It is passed
// to `SecureRandom.getInstance("DRBG", params)`, which in this library does not exist (see
// `Signature` for why there is no `SecureRandom`). It is declared all the same because it is pure
// data and because describing correctly what is asked for is independent of there being somebody to
// fulfil it.
public final class DrbgParameters {

    // It is not instantiated: it is only the roof of the three nested classes and of their
    // factories.
    private DrbgParameters() {
    }

    // What a DRBG knows how to do beyond generating.
    public enum Capability {

        // Reseeding and prediction resistance.
        PR_AND_RESEED,

        // Reseeding only.
        RESEED_ONLY,

        // Neither of the two: once seeded, it generates until it runs out.
        NONE;

        // The name as the security property `securerandom.drbg.config` writes it: "pr_and_reseed",
        // "reseed_only", "none".
        @Override
        public String toString() {
            return this.name().toLowerCase(java.util.Locale.ROOT);
        }

        // PR implies reseed: entropy cannot be mixed per operation without being able to reseed.
        public boolean supportsReseeding() {
            return this != NONE;
        }

        public boolean supportsPredictionResistance() {
            return this == PR_AND_RESEED;
        }
    }

    // What is asked for when creating the DRBG.
    public static final class Instantiation implements SecureRandomParameters {

        private final int strength;
        private final Capability capability;
        private final byte[] personalizationString;

        private Instantiation(int strength, Capability capability, byte[] personalizationString) {
            this.strength = strength;
            this.capability = capability;
            this.personalizationString = personalizationString;
        }

        // The strength in bits, or -1 for "whichever the provider prefers". A DRBG can give
        // **more** than what was asked for, never less.
        public int getStrength() {
            return this.strength;
        }

        public Capability getCapability() {
            return this.capability;
        }

        // A copy of the personalisation string, or null. It is what separates this generator from
        // another seeded with the same entropy; it can be something as mundane as the name of the
        // machine.
        public byte[] getPersonalizationString() {
            return copyOf(this.personalizationString);
        }

        // It prints the whole personalisation string, not a summary. It is not an oversight of the
        // JDK: that string **is not secret** —its only job is to separate two generators, not to
        // contribute entropy— so seeing it in a log weakens nothing.
        //
        // The format is built by hand instead of with `java.util.Arrays.toString(byte[])` because
        // that overload is broken in this library: it prints each byte as a **character** instead
        // of as a number, so {1} comes out as the control character 0x01 and not as "1". See the
        // report; when it is fixed, this can be replaced by the direct call.
        @Override
        public String toString() {
            return this.strength + "," + this.capability + ","
                + listOf(this.personalizationString);
        }
    }

    // What is asked for at each generation of bits.
    public static final class NextBytes implements SecureRandomParameters {

        private final int strength;
        private final boolean predictionResistance;
        private final byte[] additionalInput;

        private NextBytes(int strength, boolean predictionResistance, byte[] additionalInput) {
            this.strength = strength;
            this.predictionResistance = predictionResistance;
            this.additionalInput = additionalInput;
        }

        public int getStrength() {
            return this.strength;
        }

        // Whether fresh entropy has to be mixed in before generating. Expensive, and it only makes
        // sense if the DRBG was created with `PR_AND_RESEED`.
        public boolean getPredictionResistance() {
            return this.predictionResistance;
        }

        // A copy of the additional input, or null. It is mixed with the state only for this call.
        public byte[] getAdditionalInput() {
            return copyOf(this.additionalInput);
        }
    }

    // What is asked for when reseeding.
    public static final class Reseed implements SecureRandomParameters {

        private final boolean predictionResistance;
        private final byte[] additionalInput;

        private Reseed(boolean predictionResistance, byte[] additionalInput) {
            this.predictionResistance = predictionResistance;
            this.additionalInput = additionalInput;
        }

        public boolean getPredictionResistance() {
            return this.predictionResistance;
        }

        public byte[] getAdditionalInput() {
            return copyOf(this.additionalInput);
        }
    }

    private static byte[] copyOf(byte[] b) {
        if (b == null) {
            return null;
        }
        byte[] c = new byte[b.length];
        System.arraycopy(b, 0, c, 0, b.length);
        return c;
    }

    // The same format as `java.util.Arrays.toString(byte[])`: "null", "[]", "[1, 2]".
    private static String listOf(byte[] b) {
        if (b == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        int i = 0;
        while (i < b.length) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append((int) b[i]);
            i = i + 1;
        }
        sb.append(']');
        return sb.toString();
    }

    // The parameters of creation. `strength` may be -1 to let the provider choose; any other
    // negative is an error, because asking for "strength -3" means nothing.
    public static Instantiation instantiation(int strength, Capability capability,
                                              byte[] personalizationString) {
        if (strength < -1) {
            throw new IllegalArgumentException("Invalid strength: " + strength);
        }
        if (capability == null) {
            throw new NullPointerException("Capability is null");
        }
        return new Instantiation(strength, capability, copyOf(personalizationString));
    }

    public static NextBytes nextBytes(int strength, boolean predictionResistance,
                                      byte[] additionalInput) {
        if (strength < -1) {
            throw new IllegalArgumentException("Invalid strength: " + strength);
        }
        return new NextBytes(strength, predictionResistance, copyOf(additionalInput));
    }

    public static Reseed reseed(boolean predictionResistance, byte[] additionalInput) {
        return new Reseed(predictionResistance, copyOf(additionalInput));
    }
}
