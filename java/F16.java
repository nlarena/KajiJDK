import jdk.incubator.vector.Float16;

public class F16 {
    static int h(int a, String s) { return a * 31 + (s == null ? 0 : s.hashCode()); }
    static int h(int a, int v) { return a * 31 + v; }
    static int hf(int a, Float16 f) { return a * 31 + Float16.float16ToRawShortBits(f); }

    /** Recorre los 65536 valores y mezcla todo lo unario. */
    public static int unario() {
        int a = 17;
        for (int i = 0; i < 65536; i++) {
            Float16 f = Float16.shortBitsToFloat16((short) i);
            a = h(a, Float16.toString(f));
            a = h(a, Float16.toHexString(f));
            a = h(a, Float16.isNaN(f) ? 1 : 0);
            a = h(a, Float16.isInfinite(f) ? 1 : 0);
            a = h(a, Float16.isFinite(f) ? 1 : 0);
            a = h(a, Float16.getExponent(f));
            a = h(a, Float16.hashCode(f));
            a = h(a, Float16.float16ToShortBits(f));
            a = hf(a, Float16.abs(f));
            a = hf(a, Float16.negate(f));
            a = hf(a, Float16.signum(f));
            a = hf(a, Float16.ulp(f));
            a = hf(a, Float16.nextUp(f));
            a = hf(a, Float16.nextDown(f));
            a = hf(a, Float16.sqrt(f));
            a = h(a, f.byteValue());
            a = h(a, f.shortValue());
            a = h(a, f.intValue());
            a = h(a, (int) f.longValue());
            a = h(a, Float.floatToRawIntBits(f.floatValue()));
            a = h(a, (int) Double.doubleToRawLongBits(f.doubleValue()));
            for (int n = -30; n <= 30; n += 7) a = hf(a, Float16.scalb(f, n));
        }
        return a;
    }

    /** Pares de operandos: una muestra que cubre los bordes. */
    static short[] M = { 0x0000, (short)0x8000, 0x0001, (short)0x8001, 0x0200, 0x03FF, 0x0400,
        0x3C00, (short)0xBC00, 0x3555, 0x4900, 0x7BFF, (short)0xFBFF, 0x7C00, (short)0xFC00,
        0x7E00, 0x2E66, 0x1200, 0x63D0, 0x5640 };

    public static int binario() {
        int a = 17;
        for (int i = 0; i < M.length; i++) {
            for (int j = 0; j < M.length; j++) {
                Float16 x = Float16.shortBitsToFloat16(M[i]);
                Float16 y = Float16.shortBitsToFloat16(M[j]);
                a = hf(a, Float16.add(x, y));
                a = hf(a, Float16.subtract(x, y));
                a = hf(a, Float16.multiply(x, y));
                a = hf(a, Float16.divide(x, y));
                a = hf(a, Float16.max(x, y));
                a = hf(a, Float16.min(x, y));
                a = hf(a, Float16.copySign(x, y));
                a = h(a, Float16.compare(x, y));
                a = h(a, x.equals(y) ? 1 : 0);
                for (int k = 0; k < M.length; k += 5) {
                    a = hf(a, Float16.fma(x, y, Float16.shortBitsToFloat16(M[k])));
                }
            }
        }
        return a;
    }

    /** Las fabricas. */
    public static int fabricas() {
        int a = 17;
        for (int i = -70000; i <= 70000; i += 137) {
            a = hf(a, Float16.valueOf(i));
            a = hf(a, Float16.valueOf((long) i));
            a = hf(a, Float16.valueOf(i / 1000.0f));
            a = hf(a, Float16.valueOf(i / 1000.0));
        }
        String[] ss = { "0", "-0", "1", "-1", "0.1", "65504", "65505", "1e-8", "1e10", "NaN",
                        "Infinity", "-Infinity", "6.1035e-5", "0x1.554p-2" };
        for (int i = 0; i < ss.length; i++) a = hf(a, Float16.valueOf(ss[i]));
        return a;
    }

    public static void main(String[] args) {
        System.out.println(unario() + " " + binario() + " " + fabricas());
    }
}
