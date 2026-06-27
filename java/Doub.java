// double (categoría-2, f64): ldc2_w + dstore + dload + dadd + dreturn.
class Doub {
    static double run() {
        double a = 2.5;
        double b = 1.5;
        return a + b;   // 4.0
    }
}
