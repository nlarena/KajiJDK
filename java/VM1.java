import jdk.incubator.vector.VectorMath;

public class VM1 {
    static long[] LS = { 0, 1, -1, 2, -2, 127, -128, 255, 32767, -32768, 65535,
        Integer.MAX_VALUE, Integer.MIN_VALUE, Long.MAX_VALUE, Long.MIN_VALUE,
        Long.MAX_VALUE - 1, Long.MIN_VALUE + 1, 0x8000000000000000L, 0x7FFFFFFFFFFFFFFFL };

    static int h(int a, long v) { return a * 31 + (int) (v ^ (v >>> 32)); }

    public static int check() {
        int a = 17;
        for (int i = 0; i < LS.length; i++) {
            for (int j = 0; j < LS.length; j++) {
                long x = LS[i], y = LS[j];
                a = h(a, VectorMath.minUnsigned(x, y));
                a = h(a, VectorMath.maxUnsigned(x, y));
                a = h(a, VectorMath.addSaturating(x, y));
                a = h(a, VectorMath.subSaturating(x, y));
                a = h(a, VectorMath.addSaturatingUnsigned(x, y));
                a = h(a, VectorMath.subSaturatingUnsigned(x, y));
                int xi = (int) x, yi = (int) y;
                a = h(a, VectorMath.minUnsigned(xi, yi));
                a = h(a, VectorMath.maxUnsigned(xi, yi));
                a = h(a, VectorMath.addSaturating(xi, yi));
                a = h(a, VectorMath.subSaturating(xi, yi));
                a = h(a, VectorMath.addSaturatingUnsigned(xi, yi));
                a = h(a, VectorMath.subSaturatingUnsigned(xi, yi));
                short xs = (short) x, ys = (short) y;
                a = h(a, VectorMath.minUnsigned(xs, ys));
                a = h(a, VectorMath.maxUnsigned(xs, ys));
                a = h(a, VectorMath.addSaturating(xs, ys));
                a = h(a, VectorMath.subSaturating(xs, ys));
                a = h(a, VectorMath.addSaturatingUnsigned(xs, ys));
                a = h(a, VectorMath.subSaturatingUnsigned(xs, ys));
                byte xb = (byte) x, yb = (byte) y;
                a = h(a, VectorMath.minUnsigned(xb, yb));
                a = h(a, VectorMath.maxUnsigned(xb, yb));
                a = h(a, VectorMath.addSaturating(xb, yb));
                a = h(a, VectorMath.subSaturating(xb, yb));
                a = h(a, VectorMath.addSaturatingUnsigned(xb, yb));
                a = h(a, VectorMath.subSaturatingUnsigned(xb, yb));
            }
        }
        // ademas, todos los pares de byte: 65536 combinaciones, exhaustivo
        for (int i = -128; i < 128; i++) {
            for (int j = -128; j < 128; j++) {
                byte x = (byte) i, y = (byte) j;
                a = h(a, VectorMath.addSaturating(x, y));
                a = h(a, VectorMath.subSaturating(x, y));
                a = h(a, VectorMath.addSaturatingUnsigned(x, y));
                a = h(a, VectorMath.subSaturatingUnsigned(x, y));
                a = h(a, VectorMath.minUnsigned(x, y));
                a = h(a, VectorMath.maxUnsigned(x, y));
            }
        }
        return a;
    }

    public static void main(String[] args) { System.out.println(check()); }
}
