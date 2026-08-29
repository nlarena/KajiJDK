/**
 * An ARRAY is not accepted where a parameter is declared {@code Object}. Depending on where the
 * method is declared the compiler either refuses it or drops the call in silence.
 *
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_258.java
 *   bin/run-headless.exe KajiLibrary/repros/finding_258.class copiaNativa
 *
 * Two faces, one cause:
 *
 *   arrayComoObject   -- the method is in THIS file, so the call is an ERROR:
 *                        `no se encontro un metodo `take(int[], int[])` aplicable`
 *   copiaNativa       -- System.arraycopy is on the classpath, so the call is DROPPED:
 *                        the arguments are pushed, one `pop` follows, and the invokestatic
 *                        is simply not there. Returns 0 instead of 9, with no diagnostic.
 *
 * Emitted for `copiaNativa`:
 *
 *     15: aload_1      // dst
 *     16: iconst_0     // dstPos
 *     17: iconst_3     // length
 *     18: pop          <- and that is the whole call
 *     19: aload_1
 *
 * Every control passes, which is what places the defect on the array-to-Object conversion and
 * nowhere else:
 *
 *   propioMismaAridad   a static void of this class, five arguments, arrays typed as arrays -> 9
 *   nativoConRetorno    System.identityHashCode: native, another class, returns int          -> 1
 *   nativoVoidSinArgs   System.gc: native, another class, void, no arguments                 -> 1
 *   ajenoNoNativo       a non-native static void of another class in this file               -> 5
 *   objetoComoObject    the same Object-typed helper called with real Objects                -> 7
 *
 * BLAST RADIUS: `System.arraycopy` is exactly this shape (`Object src, int, Object dest, int,
 * int`), so all 31 of its call sites across 12 KajiLibrary files silently do nothing --
 * `ArrayList`, `StringBuilder`, `StringBuffer` and eight `java.io` classes among them. They
 * work only for as long as they never have to copy.
 */
public class finding_258 {

    static int[] sink = new int[3];

    /** The classpath face: silently dropped. */
    public static int copiaNativa() {
        int[] src = new int[3];
        src[2] = 9;
        int[] dst = new int[3];
        System.arraycopy(src, 0, dst, 0, 3);
        return dst[2];
    }

    /** The same-file face: a hard error. Uncomment to see it. */
    // public static int arrayComoObject() {
    //     int[] a = new int[2];
    //     finding_258.sink[2] = 0;
    //     Helper258.take(a, a);
    //     return finding_258.sink[2];
    // }

    /** Control: the same helper called with real Objects. */
    public static int objetoComoObject() {
        Object o = new Object();
        finding_258.sink[2] = 0;
        Helper258.take(o, o);
        return finding_258.sink[2];
    }

    /** Control: a static void of this class, five arguments, arrays typed as arrays. */
    public static int propioMismaAridad() {
        int[] src = new int[3];
        src[2] = 9;
        int[] dst = new int[3];
        finding_258.copy(src, 0, dst, 0, 3);
        return dst[2];
    }

    static void copy(int[] s, int sp, int[] d, int dp, int n) {
        int i = 0;
        while (i < n) {
            d[dp + i] = s[sp + i];
            i = i + 1;
        }
    }

    /** Control: a native static of another class that returns a value. */
    public static int nativoConRetorno() {
        Object o = new Object();
        int h = System.identityHashCode(o);
        if (h == 0) {
            return -1;
        }
        return 1;
    }

    /** Control: a native static void of another class, no arguments. */
    public static int nativoVoidSinArgs() {
        finding_258.sink[0] = 1;
        System.gc();
        return finding_258.sink[0];
    }

    /** Control: a non-native static void of another class. */
    public static int ajenoNoNativo() {
        finding_258.sink[1] = 0;
        Helper258.bump();
        return finding_258.sink[1];
    }
}


final class Helper258 {

    static void bump() {
        finding_258.sink[1] = 5;
    }

    static void take(Object a, Object b) {
        finding_258.sink[2] = 7;
    }
}
