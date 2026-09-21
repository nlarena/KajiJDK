package java.security;

// The generator `KajiProvider` registers: a direct pass to the operating system's.
//
// ===============================================================================================
// WHY A DIRECT PASS AND NOT A DRBG
// ===============================================================================================
//
// A DRBG --the `SHA1PRNG` or the `Hash_DRBG` of the JDK-- has internal state: it is seeded once and
// then expands that seed with a hash function. Writing one properly is not difficult, but
// **choosing any detail wrongly does not show**: the output looks just as random with or without
// the mistake, and nobody discovers it until somebody attacks it.
//
// A direct pass does not have that kind of possible mistake. Every byte that comes out is a byte
// `BCryptGenRandom` or `/dev/urandom` gave, and the security is exactly the system's -- which
// besides is the one the system reseeds by itself with the entropy sources of the hardware. It is
// the same thing the `SunMSCAPI` provider of the JDK does with its `Windows-PRNG`.
//
// The price is speed: each call goes down to the system. For what is needed in this library
// --seeds, nonces, identifiers-- it does not matter.
//
// ===============================================================================================
// WHAT IT DOES WITH THE SEED IT IS GIVEN, AND WHY
// ===============================================================================================
//
// `engineSetSeed` **ignores it**, and it has to be said because it sounds as if something were
// missing.
//
// The contract of `setSeed` is that the seed **adds**, it never replaces: calling it cannot leave
// the generator more predictable. Ignoring it fulfils that -- the output goes on being the
// system's, which does not depend on what the caller passes. The alternative would be mixing it,
// and there the mixing would have to be invented: deriving a stream from the seed and combining it
// with the bytes of the system. That is designing a cryptographic construction for no gain, because
// the bytes of the system are strong already.
//
// `engineReseed` is not there either: there is no internal state to reseed. Whoever calls it
// receives `UnsupportedOperationException`, which is the right answer and not a silent no-op.
final class OsPrngSpi extends SecureRandomSpi {

    private static final long serialVersionUID = 6812298296178204625L;

    /** See the note of the class: the seed is accepted and discarded. */
    @Override
    protected void engineSetSeed(byte[] seed) {
        if (seed == null) {
            throw new NullPointerException("seed is null");
        }
    }

    @Override
    protected void engineNextBytes(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        OsEntropy.fill(bytes);
    }

    /**
     * Entropy for seeding another generator.
     *
     * <p>It comes from the same source as {@code engineNextBytes}, and here that <b>is</b> right:
     * the distinction between output and entropy exists because a DRBG expands a seed, and
     * expanding adds no entropy. A direct pass expands nothing, so the two are the same thing.
     */
    @Override
    protected byte[] engineGenerateSeed(int numBytes) {
        if (numBytes < 0) {
            throw new IllegalArgumentException("numBytes cannot be negative");
        }
        return OsEntropy.bytes(numBytes);
    }

    @Override
    public String toString() {
        return "OS-PRNG";
    }
}
