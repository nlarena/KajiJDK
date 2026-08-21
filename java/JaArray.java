// Differential workload for the F3 JIT — dimension: **array allocation** (`newarray`/`anewarray`).
//
// A compiled `new` knows its object's size at compile time; an array's size is
// `header + count * width` with the count in a register, and every question below exists because
// of that one difference:
//
//   * does a small array allocated **entirely in native code** hold the right bytes? `fill`
//     allocates, writes and reads back one array per call, and folds its `length` into the answer —
//     so a length word off by a rounding, or a payload that started as anything but zero, moves the
//     number.
//   * is the zeroing **rounded up**? `char[3]` is 18 logical bytes and the arena reserves 24. The
//     three elements sit at 12, 14 and 16, so a zeroing loop that covered `size & ~7` = 16 bytes
//     would leave the *last element* holding whatever the previous occupant of those bytes left.
//     `chars`/`bytes` allocate odd-length arrays (natively — that is all they do) and `scanChars`/
//     `scanBytes` sum them interpreted, because `caload`/`baload` are outside the subset. The sum
//     must be 0 every time.
//   * does `anewarray` leave its slots **null**? `refs` allocates and `countNulls` counts, the same
//     way round: `aaload` is outside the subset, so the counting is the interpreter's.
//   * is a **big** array handed back? `big` asks for more than the tier will zero inline, so the
//     compiled code leaves through `Status::ALLOC` and the interpreter allocates it — and the array
//     that comes back has to be indistinguishable from the one native code would have made.
//   * does a **negative** count throw the right exception? `neg` is compiled by the time it is
//     asked for `new int[-1]`; native code cannot throw, so it deopts and the *interpreter*
//     re-executes the instruction and raises `NegativeArraySizeException`. The `catch` proves the
//     exception is the right one, and calling `neg` again afterwards proves the deopt left the
//     method usable.
//
// Nothing here needs `multianewarray`, which is deliberately outside the subset: it is a recursion
// over allocations rather than an allocation.
public class JaArray {

    // ---- A whole array's life inside one compiled method ---------------------------------------
    static int fill(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i * 3 + 1;
        }
        int acc = 0;
        for (int i = 0; i < n; i++) {
            acc = acc + a[i];
        }
        return acc + a.length;
    }

    // ---- Odd payloads: the zeroing has to cover the 8-rounded stride ---------------------------
    // These do nothing but allocate, so they are `iload_0; newarray …; areturn` and compile whole.
    static char[] chars(int n) {
        return new char[n];
    }

    static byte[] bytes(int n) {
        return new byte[n];
    }

    // ...and these read them back, interpreted, because `caload`/`baload` are outside the subset.
    static int scanChars(char[] a) {
        int acc = 0;
        for (int i = 0; i < a.length; i++) {
            acc = acc + a[i];
        }
        return acc;
    }

    static int scanBytes(byte[] a) {
        int acc = 0;
        for (int i = 0; i < a.length; i++) {
            acc = acc + a[i];
        }
        return acc;
    }

    // ---- anewarray: every slot null ------------------------------------------------------------
    static String[] refs(int n) {
        return new String[n];
    }

    static int countNulls(String[] a) {
        int n = 0;
        for (int i = 0; i < a.length; i++) {
            if (a[i] == null) {
                n = n + 1;
            }
        }
        return n;
    }

    // ---- Over the inline cap: the interpreter does this one ------------------------------------
    static int big(int n) {
        int[] a = new int[n];
        a[0] = 11;
        a[n - 1] = 22;
        return a.length + a[0] + a[n - 1];
    }

    // ---- A count native code refuses to believe -------------------------------------------------
    static int neg(int n) {
        int[] a = new int[n];
        return a.length;
    }

    public static int run() {
        int acc = 0;
        for (int round = 0; round < 400; round++) {
            acc = (acc + fill(round % 17)) & 0xFFFFFF;
            acc = (acc + scanChars(chars(round % 7))) & 0xFFFFFF;
            acc = (acc + scanBytes(bytes(round % 5))) & 0xFFFFFF;
            acc = (acc + countNulls(refs(round % 6))) & 0xFFFFFF;
            acc = (acc + big(1200 + (round % 3))) & 0xFFFFFF;
            acc = (acc + neg(1 + round % 5)) & 0xFFFFFF;
        }
        // Every one of the six above is compiled by now, so the negative count below is met by
        // *native* code, which is the only way this observation is about the JIT at all.
        try {
            acc = (acc + neg(-1)) & 0xFFFFFF;
        } catch (NegativeArraySizeException e) {
            acc = (acc + 7) & 0xFFFFFF;
        }
        // ...and the method still works afterwards: a deopt is a pause, not a retirement.
        acc = (acc + neg(9)) & 0xFFFFFF;
        return acc;
    }

    public static void main(String[] args) {
        System.out.println(run());
    }
}
