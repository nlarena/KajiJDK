// Conversiones numéricas: cobertura amplia de i2l..i2s.
class Conv {
    static long widen() {            // i2l, d2l
        int i = 5;
        double d = 7.9;
        return (long) i + (long) d;  // 5 + 7 = 12
    }
    static int narrow() {            // f2i, i2b
        float f = 300.75f;
        int n = (int) f;             // 300
        byte b = (byte) n;           // 44
        return b;
    }
    static double mixed() {          // i2d (promoción) + dmul
        int i = 3;
        double d = 2.5;
        return i * d;                // 7.5
    }
    static int roundtrip() {         // i2l, l2f, f2d, d2l, l2i — vuelve al original
        int start = 42;
        long l = (long) start;
        float f = (float) l;
        double d = (double) f;
        long l2 = (long) d;
        return (int) l2;             // 42
    }
    static int chars() {             // i2c (zero-extend), i2s (sign-extend)
        int big = 70000;             // > char range
        char c = (char) big;         // 4464
        short s = (short) big;       // 4464
        return c + s;                // 8928
    }
    static int rest() {              // l2d, d2i, d2f, f2l, i2f
        double d = (double) 9L;      // l2d → 9.0
        int a = (int) d;             // d2i → 9
        float f = (float) 5.0;       // d2f → 5.0f
        long b = (long) f;           // f2l → 5
        float g = (float) a;         // i2f
        return a + (int) b + (int) g; // 9 + 5 + 9 = 23
    }
}
