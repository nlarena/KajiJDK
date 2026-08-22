// A7 (JVMS §6.3): OutOfMemoryError must be catchable. Each recursion frame allocates
// and ROOTS one `new long[65536]` (512 KiB + header) in a local — frame locals are GC
// roots, so while the recursion deepens nothing is reclaimable and the 16 MiB max heap
// (JVM_GC_MAX_HEAP) truly fills after ~31 frames, far fewer than the 128 requested.
// The failing `newarray` must then throw a catchable OutOfMemoryError, which unwinds
// the recursion into run()'s handler (before the fix: VM panic "heap exhausted").
// Retention is via frame locals rather than an array holder so the test doesn't touch
// `aastore` of array references. Single-threaded and allocation-order deterministic →
// green ≡ os-gil ≡ os = 42. No autoboxing anywhere: only primitive arrays and ints.
public class OmTest {
    static long fill(int depth) {
        long[] chunk = new long[65536]; // 512 KiB, retained by this live frame
        chunk[0] = depth;
        if (depth == 0) {
            return chunk[0];
        }
        return chunk[0] + fill(depth - 1); // `chunk` stays rooted across the call
    }

    static int run() {
        try {
            return (int) fill(128); // unreachable: 128 * 512 KiB = 64 MiB >> 16 MiB
        } catch (OutOfMemoryError e) {
            return 42;
        }
    }
}
