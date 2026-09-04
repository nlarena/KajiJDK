import jdk.dynalink.linker.support.TypeUtilities;
public class TU6 {
    static Class<?>[] p() {
        return new Class<?>[] { void.class, boolean.class, byte.class, char.class,
            short.class, int.class, long.class, float.class, double.class };
    }
    // fila i: bit j = isSubtype(p[i], p[j]) | bit (j+9) = conv | bit (j+18) = loss
    public static int fila(int i) {
        Class<?>[] t = p(); int h = 0;
        for (int j = 0; j < 9; j++) {
            if (TypeUtilities.isSubtype(t[i], t[j])) h |= 1 << j;
            if (TypeUtilities.isMethodInvocationConvertible(t[i], t[j])) h |= 1 << (j + 9);
            if (TypeUtilities.isConvertibleWithoutLoss(t[i], t[j])) h |= 1 << (j + 18);
        }
        return h;
    }
    public static void main(String[] a) {
        for (int i = 0; i < 9; i++) System.out.println("fila " + i + " " + fila(i));
    }
}
