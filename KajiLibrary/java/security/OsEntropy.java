package java.security;

// The seam towards the generator of the operating system.
//
// ===============================================================================================
// WHY A NATIVE IS NEEDED
// ===============================================================================================
//
// Everything else of this library is deterministic: given the same state it does the same. A
// cryptographic generator needs just the opposite, and it cannot compute it -- the seed has to come
// from outside, from something an attacker cannot reproduce. That is why this is the only place of
// `java.security` that goes down to the VM.
//
// What is on the other side is the generator **of the system**, not one of our own:
//
//   - On Windows, `BCryptGenRandom` with the preferred algorithm of the system.
//   - On the rest, `/dev/urandom`.
//
// They are the same two the JDK and the standard library of Rust use. The choice matters: a
// generator of one's own seeded with the time and the identifier of the process looks the same from
// outside and is guessable, which is exactly the way this kind of code fails.
//
// ===============================================================================================
// WHY A PARTIAL FAILURE IS TREATED AS A TOTAL FAILURE
// ===============================================================================================
//
// The native returns a boolean and not a number of bytes. If it could not fill the whole array, it
// returns false and the array is left as it was. The alternative --returning how many bytes it
// filled-- would force every caller to remember to look at it, and whoever forgets is left with a
// seed half of zeroes that looks complete. Here that cannot happen: either there is entropy or
// there is an exception.
final class OsEntropy {

    private OsEntropy() {
    }

    /** It fills `out` with bytes of the system. It returns whether it could. */
    private static native boolean fill0(byte[] out);

    /**
     * It returns `count` bytes of the generator of the system.
     *
     * @throws ProviderException if the system could not give them. It is an error of the platform,
     *     not of the caller, and there is no reasonable way of going on: anything that was returned
     *     in its place would be guessable.
     */
    static byte[] bytes(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("negative count: " + count);
        }
        byte[] out = new byte[count];
        fill(out);
        return out;
    }

    /**
     * It fills `out` with bytes of the generator of the system.
     *
     * @throws ProviderException if the system could not give them
     */
    static void fill(byte[] out) {
        if (out.length == 0) {
            return;
        }
        if (!fill0(out)) {
            throw new ProviderException("the operating system's random generator is unavailable");
        }
    }

    /** Whether the system can give entropy. The provider uses it so as not to register what it
     * cannot do. */
    static boolean available() {
        try {
            byte[] probe = new byte[1];
            return fill0(probe);
        } catch (Throwable e) {
            return false;
        }
    }
}
