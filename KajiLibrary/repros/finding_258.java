/**
 * Repro de #258 - un ARRAY no se aceptaba donde el parametro esta declarado {@code Object}.
 *
 *   bin/javac.exe --emit -cp KajiLibrary KajiLibrary/repros/finding_258.java
 *   bin/run-headless.exe KajiLibrary/repros/finding_258.class copiaNativa   -> 9
 *
 * ANTES tenia dos caras, y el lugar donde estuviera declarado el metodo decidia cual:
 *
 *   arrayComoObject   el metodo esta en ESTE archivo, asi que la llamada era un ERROR:
 *                     `no se encontro un metodo `take(int[], int[])` aplicable`
 *   copiaNativa       `System.arraycopy` viene del classpath, asi que la llamada se DESCARTABA:
 *                     se empujaban los argumentos, seguia un `pop`, y el invokestatic no estaba.
 *                     Devolvia 0 en vez de 9, sin un solo diagnostico.
 *
 * Lo que emitia para `copiaNativa`:
 *
 *     15: aload_1      // dst
 *     16: iconst_0     // dstPos
 *     17: iconst_3     // length
 *     18: pop          &lt;- y esa era toda la llamada
 *     19: aload_1
 *
 * La cara muda era la peligrosa: once fuentes de la biblioteca se compilaban rotas sin decir nada.
 *
 * AHORA: **compila y `copiaNativa` devuelve 9**. `#258` figura cerrado en COMPILER_FINDINGS.md, y
 * con una aclaracion que vale la pena: era **el mismo bug que #261**, encontrado en paralelo por
 * dos caminos. La conversion de un array a `Object` por ampliacion de referencia no estaba.
 *
 * `arrayComoObject` sigue COMENTADO en el archivo: se dejo asi cuando era un error duro.
 * Descomentarlo es la otra mitad de la regresion.
 *
 * Los controles siguen porque son los que ubicaban el defecto en la conversion array-a-Object y
 * en ningun otro lado: `objetoComoObject`, `propioMismaAridad`, `nativoConRetorno` y
 * `nativoVoidSinArgs` siempre pasaron.
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
