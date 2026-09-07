package javax.crypto.spec;

import java.security.spec.AlgorithmParameterSpec;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.crypto.SecretKey;

/**
 * HKDF's parameters (RFC 5869), the two-step key derivation.
 *
 * <p>The two steps are what explains the shape of this interface, which otherwise looks arbitrary:
 *
 * <ul>
 * <li><strong>Extract</strong> takes input material --which may not be uniform: a Diffie-Hellman
 *     secret, a stretched password-- and a salt, and produces a pseudorandom key (`PRK`) that is.</li>
 * <li><strong>Expand</strong> takes that `PRK`, a context string and a length, and produces the
 *     final key. The same `PRK` can be expanded several times with different contexts to obtain
 *     independent keys.</li>
 * </ul>
 *
 * <p>Hence the three implementations: {@link Extract} does only the first step, {@link Expand} only
 * the second --when one already has the `PRK`-- and {@link ExtractThenExpand} both at once. They are
 * the three legitimate ways of using HKDF and there is no fourth, which is why they are `final` and
 * the interface has no public constructor.
 *
 * <p><strong>The material accumulates.</strong> {@link Builder#addIKM} and {@link Builder#addSalt}
 * can be called several times, and what is passed is **concatenated** in order instead of replacing.
 * It is not a convenience: it allows the input to be assembled out of pieces that arrive separately
 * without having to join them into an array first, which is exactly what one wants to avoid with
 * secret material.
 */
public interface HKDFParameterSpec extends AlgorithmParameterSpec {

    /** A builder for the forms that start by extracting. */
    public static Builder ofExtract() {
        return new Builder();
    }

    /**
     * The form that **only expands**, from a `PRK` one already has.
     *
     * @param prk the pseudorandom key; it cannot be null
     * @param info the context, or null for none
     * @param length how many bytes are wanted, greater than zero
     * @throws NullPointerException if `prk` is null
     * @throws IllegalArgumentException if `length` is not positive
     */
    public static Expand expandOnly(SecretKey prk, byte[] info, int length) {
        if (prk == null) {
            throw new NullPointerException("the PRK cannot be null");
        }
        if (length <= 0) {
            throw new IllegalArgumentException("the length has to be positive");
        }
        return new Expand(prk, info, length);
    }

    /**
     * The builder of the forms that extract.
     *
     * <p>It is mutable and used once: inputs are added to it and it is finished with
     * {@link #extractOnly} or {@link #thenExpand}. Both return an already frozen object, so going on
     * using the builder afterwards does not change what was returned.
     */
    public static final class Builder {

        private final List<SecretKey> ikms = new ArrayList<SecretKey>();
        private final List<SecretKey> salts = new ArrayList<SecretKey>();

        Builder() {
        }

        /** Only the first step: it produces the `PRK` and stops there. */
        public Extract extractOnly() {
            return new Extract(this.ikms, this.salts);
        }

        /**
         * Both steps: it extracts and then expands with that context to that length.
         *
         * @throws IllegalArgumentException if `length` is not positive
         */
        public ExtractThenExpand thenExpand(byte[] info, int length) {
            if (length <= 0) {
                throw new IllegalArgumentException("the length has to be positive");
            }
            return new ExtractThenExpand(this.ikms, this.salts, info, length);
        }

        /**
         * Adds input material. See the interface's note about concatenation.
         *
         * @throws NullPointerException if the key is null
         */
        public Builder addIKM(SecretKey ikm) {
            if (ikm == null) {
                throw new NullPointerException("the input material cannot be null");
            }
            this.ikms.add(ikm);
            return this;
        }

        /**
         * Adds raw input material.
         *
         * <p>It is wrapped in a {@link SecretKeySpec} with algorithm `"Generic"`, which is the name
         * the JDK uses for material belonging to no algorithm in particular. An **empty** array is
         * ignored instead of added: concatenating zero bytes changes nothing, and keeping it would
         * only give the list an element that contributes nothing.
         *
         * @throws NullPointerException if the array is null
         */
        public Builder addIKM(byte[] ikm) {
            if (ikm == null) {
                throw new NullPointerException("the input material cannot be null");
            }
            if (ikm.length != 0) {
                this.ikms.add(new SecretKeySpec(ikm, "Generic"));
            }
            return this;
        }

        /**
         * Adds salt.
         *
         * @throws NullPointerException if the key is null
         */
        public Builder addSalt(SecretKey salt) {
            if (salt == null) {
                throw new NullPointerException("the salt cannot be null");
            }
            this.salts.add(salt);
            return this;
        }

        /**
         * Adds raw salt. The same note as {@link #addIKM(byte[])} applies.
         *
         * @throws NullPointerException if the array is null
         */
        public Builder addSalt(byte[] salt) {
            if (salt == null) {
                throw new NullPointerException("the salt cannot be null");
            }
            if (salt.length != 0) {
                this.salts.add(new SecretKeySpec(salt, "Generic"));
            }
            return this;
        }
    }

    /** Only the first step: from input material and salt to a `PRK`. */
    public static final class Extract implements HKDFParameterSpec {

        private final List<SecretKey> ikms;
        private final List<SecretKey> salts;

        Extract(List<SecretKey> ikms, List<SecretKey> salts) {
            this.ikms = Collections.unmodifiableList(new ArrayList<SecretKey>(ikms));
            this.salts = Collections.unmodifiableList(new ArrayList<SecretKey>(salts));
        }

        /** The input material, in order and read-only. */
        public List<SecretKey> ikms() {
            return this.ikms;
        }

        /** The salt, in order and read-only. */
        public List<SecretKey> salts() {
            return this.salts;
        }
    }

    /** Only the second step: from a `PRK` and a context to the final key. */
    public static final class Expand implements HKDFParameterSpec {

        private final SecretKey prk;
        private final byte[] info;
        private final int length;

        Expand(SecretKey prk, byte[] info, int length) {
            this.prk = prk;
            this.info = info == null ? null : IvParameterSpec.copy(info, 0, info.length);
            this.length = length;
        }

        /** The pseudorandom key that is expanded from. */
        public SecretKey prk() {
            return this.prk;
        }

        /** A copy of the context, or null if there is none. */
        public byte[] info() {
            return this.info == null ? null
                    : IvParameterSpec.copy(this.info, 0, this.info.length);
        }

        /** How many bytes are wanted. */
        public int length() {
            return this.length;
        }
    }

    /** Both steps at once. */
    public static final class ExtractThenExpand implements HKDFParameterSpec {

        private final List<SecretKey> ikms;
        private final List<SecretKey> salts;
        private final byte[] info;
        private final int length;

        ExtractThenExpand(List<SecretKey> ikms, List<SecretKey> salts, byte[] info, int length) {
            this.ikms = Collections.unmodifiableList(new ArrayList<SecretKey>(ikms));
            this.salts = Collections.unmodifiableList(new ArrayList<SecretKey>(salts));
            this.info = info == null ? null : IvParameterSpec.copy(info, 0, info.length);
            this.length = length;
        }

        /** The input material, in order and read-only. */
        public List<SecretKey> ikms() {
            return this.ikms;
        }

        /** The salt, in order and read-only. */
        public List<SecretKey> salts() {
            return this.salts;
        }

        /** A copy of the context, or null if there is none. */
        public byte[] info() {
            return this.info == null ? null
                    : IvParameterSpec.copy(this.info, 0, this.info.length);
        }

        /** How many bytes are wanted. */
        public int length() {
            return this.length;
        }
    }
}
