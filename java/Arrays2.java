// Arrays de long/double/float: newarray + laload/lastore/daload/dastore/faload/fastore.
class Arrays2 {
    static long longArr() {
        long[] a = new long[3];
        a[0] = 10_000_000_000L;
        a[1] = 7L;
        a[2] = a[0] + a[1];
        return a[2];                       // 10000000007
    }
    static double dblArr() {
        double[] d = new double[2];
        d[0] = 1.5;
        d[1] = 2.25;
        return d[0] + d[1];                // 3.75
    }
    static float fltArr() {
        float[] f = new float[2];
        f[0] = 1.25f;
        f[1] = 0.75f;
        return f[0] + f[1];                // 2.0
    }
}
