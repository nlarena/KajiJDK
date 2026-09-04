import jdk.internal.reflect.CallerSensitive;
import jdk.internal.reflect.Reflection;
import java.lang.reflect.Method;

/** Los cinco miembros nuevos de Reflection. */
public class Rf474 {
    public record Punto(int x, int y) { }

    static class Tiene {
        @CallerSensitive
        public static Class<?> quienMeLlamo() { return Reflection.getCallerClass(); }

        public static Class<?> sinMarca() { return Reflection.getCallerClass(); }
    }

    public static int run() throws Exception {
        // getCallerClass NO se compara entre las dos VMs: el JDK lanza InternalError si el llamador
        // no lleva SU CallerSensitive (el de java.base), y una anotacion propia no cuenta. Ver la
        // diferencia anotada en Reflection.getCallerClass. Se prueba aparte, en runKaji().
        int flags = Reflection.getClassAccessFlags(Rf474.class);
        System.out.println("getClassAccessFlags(Rf474) -> 0x" + Integer.toHexString(flags));
        if ((flags & 0x0001) == 0) { return 3; }

        System.out.println("isTrustedFinalField(record x) -> "
            + Reflection.isTrustedFinalField(Punto.class.getDeclaredField("x")));
        if (!Reflection.isTrustedFinalField(Punto.class.getDeclaredField("x"))) { return 4; }
        Reflection.ensureNativeAccess(Rf474.class, Rf474.class, "m", false);
        return -1;
    }

    /**
     * Lo que no se puede comparar entre las dos VMs, y por que.
     *
     * <p>Los dos casos son el mismo: el JDK solo reconoce SU CallerSensitive, la de java.base. Una
     * anotacion propia con el mismo nombre no cuenta -- getCallerClass lanza InternalError y
     * isCallerSensitive contesta false. Para una biblioteca que declara la suya, la respuesta
     * correcta es la que da esta VM.
     */
    public static int runKaji() throws Exception {
        Class<?> c = Tiene.quienMeLlamo();
        System.out.println("getCallerClass desde Rf474 -> " + (c == null ? "null" : c.getName()));
        if (c != Rf474.class) { return 0; }
        Method marcado = Tiene.class.getMethod("quienMeLlamo");
        Method pelado = Tiene.class.getMethod("sinMarca");
        System.out.println("isCallerSensitive(marcado) -> " + Reflection.isCallerSensitive(marcado));
        System.out.println("isCallerSensitive(pelado)  -> " + Reflection.isCallerSensitive(pelado));
        if (!Reflection.isCallerSensitive(marcado)) { return 1; }
        if (Reflection.isCallerSensitive(pelado)) { return 2; }
        return -1;
    }

    public static void main(String[] a) throws Exception { System.out.println(run()); }
}
